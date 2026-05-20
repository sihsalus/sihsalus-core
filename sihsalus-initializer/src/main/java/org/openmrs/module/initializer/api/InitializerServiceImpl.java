package org.openmrs.module.initializer.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.initializer.InitializerConfig;
import org.openmrs.module.initializer.api.loaders.Loader;
import org.sihsalus.initializer.SihsalusContentPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializerServiceImpl extends BaseOpenmrsService implements InitializerService {

  private static final Logger log = LoggerFactory.getLogger(InitializerServiceImpl.class);

  private static final String JSON_KEY_VALUES_DOMAIN = "jsonkeyvalues";

  private final List<Loader> loaders;

  private final Map<String, Object> keyValueCache = new ConcurrentHashMap<>();

  private final ObjectMapper objectMapper = new ObjectMapper();

  private volatile boolean keyValueCacheLoaded;

  public InitializerServiceImpl() {
    this(List.of());
  }

  public InitializerServiceImpl(List<Loader> loaders) {
    this.loaders = new ArrayList<>(loaders);
  }

  @Override
  public List<Loader> getLoaders() {
    return loaders.stream()
        .filter(loader -> !loader.isPreLoader())
        .sorted()
        .collect(java.util.stream.Collectors.toList());
  }

  @Override
  public void loadUnsafe(boolean applyFilters, boolean doThrow) throws Exception {
    InitializerConfig config = getInitializerConfig();
    Set<String> specifiedDomains =
        applyFilters ? config.getFilteredDomains() : Collections.emptySet();
    boolean includeSpecifiedDomains = !applyFilters || config.isInclusionList();

    for (Loader loader : getLoaders()) {
      boolean domainSpecified = specifiedDomains.contains(loader.getDomainName());
      if (!specifiedDomains.isEmpty()
          && !((includeSpecifiedDomains && domainSpecified)
              || (!includeSpecifiedDomains && !domainSpecified))) {
        continue;
      }

      try {
        loader.loadUnsafe(
            applyFilters
                ? config.getWildcardExclusions(loader.getDomainName())
                : Collections.emptyList(),
            doThrow);
      } catch (Exception e) {
        if (doThrow) {
          throw e;
        }
        log.error("Failed to load initializer domain {}.", loader.getDomainName(), e);
      }
    }
  }

  @Override
  public void load() {
    try {
      loadUnsafe(true, false);
    } catch (Exception e) {
      log.error("Failed to load initializer domains.", e);
    }
  }

  @Override
  public InitializerConfig getInitializerConfig() {
    InitializerConfig config = new InitializerConfig(this::getValueFromKey);
    config.init(supportedDomains());
    return config;
  }

  @Override
  public String getValueFromKey(String key) {
    if (StringUtils.isBlank(key)) {
      return null;
    }

    loadJsonKeyValuesIfNecessary();
    if (keyValueCache.containsKey(key)) {
      return stringValue(keyValueCache.get(key));
    }

    String value = null;
    try {
      if (Context.isSessionOpen()) {
        value = Context.getAdministrationService().getGlobalProperty(key);
      }
    } catch (APIException ignored) {
      // Fall back to runtime properties below.
    }

    if (StringUtils.isNotBlank(value)) {
      return value;
    }

    Properties runtimeProperties = Context.getRuntimeProperties();
    return runtimeProperties == null ? null : runtimeProperties.getProperty(key);
  }

  @Override
  public Boolean getBooleanFromKey(String key) {
    String value = getValueFromKey(key);
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      return BooleanUtils.toBoolean(value, "1", "0");
    } catch (IllegalArgumentException e) {
      return BooleanUtils.toBooleanObject(value);
    }
  }

  private Set<String> supportedDomains() {
    Set<String> domainNames = new LinkedHashSet<>();
    for (Loader loader : loaders.stream().sorted(Comparator.naturalOrder()).toList()) {
      domainNames.add(loader.getDomainName());
    }
    return domainNames;
  }

  private void loadJsonKeyValuesIfNecessary() {
    if (keyValueCacheLoaded) {
      return;
    }

    synchronized (keyValueCache) {
      if (keyValueCacheLoaded) {
        return;
      }
      try {
        Path configRoot = SihsalusContentPaths.resolveConfigRoot();
        if (configRoot != null) {
          loadJsonKeyValues(configRoot);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to resolve SIH Salus content configuration.", e);
      }
      keyValueCacheLoaded = true;
    }
  }

  private void loadJsonKeyValues(Path configRoot) {
    try {
      Path directory =
          SihsalusContentPaths.resolveDomainDirectory(configRoot, JSON_KEY_VALUES_DOMAIN);
      if (directory == null) {
        return;
      }
      try (Stream<Path> stream = Files.list(directory)) {
        for (Path jsonFile :
            stream
                .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .filter(
                    path ->
                        path.getFileName()
                            .toString()
                            .toLowerCase(java.util.Locale.ROOT)
                            .endsWith(".json"))
                .sorted()
                .toList()) {
          addKeyValues(jsonFile);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load initializer JSON key-values.", e);
    }
  }

  private void addKeyValues(Path jsonFile) throws Exception {
    try (InputStream inputStream = Files.newInputStream(jsonFile)) {
      keyValueCache.putAll(
          objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {}));
    }
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String string) {
      return string;
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to stringify initializer key-value.", e);
    }
  }
}
