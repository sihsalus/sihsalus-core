package org.openmrs.module.initializer.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.initializer.InitializerConfig;
import org.openmrs.module.initializer.api.loaders.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializerServiceImpl extends BaseOpenmrsService implements InitializerService {

  private static final Logger log = LoggerFactory.getLogger(InitializerServiceImpl.class);

  private final List<Loader> loaders;

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
    return StringUtils.isBlank(value) ? null : Boolean.valueOf(value);
  }

  private Set<String> supportedDomains() {
    Set<String> domainNames = new LinkedHashSet<>();
    for (Loader loader : loaders.stream().sorted(Comparator.naturalOrder()).toList()) {
      domainNames.add(loader.getDomainName());
    }
    return domainNames;
  }
}
