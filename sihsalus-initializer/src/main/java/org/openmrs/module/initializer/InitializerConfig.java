package org.openmrs.module.initializer;

import static org.openmrs.module.initializer.InitializerConstants.PROPS_DOMAINS;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_EXCLUDE;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD_CONTINUE_ON_ERROR;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD_DISABLED;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public final class InitializerConfig {

  private final Function<String, String> propertyResolver;

  private boolean inclusionList = true;

  private Set<String> filteredDomains = Set.of();

  private Map<String, List<String>> wildcardExclusions = Map.of();

  private String startupLoadingMode;

  public InitializerConfig(Function<String, String> propertyResolver) {
    this.propertyResolver = Objects.requireNonNull(propertyResolver);
  }

  public void init(Set<String> supportedDomains) {
    startupLoadingMode =
        StringUtils.defaultIfBlank(
            propertyResolver.apply(PROPS_STARTUP_LOAD), PROPS_STARTUP_LOAD_CONTINUE_ON_ERROR);
    if (PROPS_STARTUP_LOAD_DISABLED.equalsIgnoreCase(startupLoadingMode)) {
      filteredDomains = Set.of();
      wildcardExclusions = Map.of();
      return;
    }

    String domainsCsv = StringUtils.defaultString(propertyResolver.apply(PROPS_DOMAINS)).trim();
    if (domainsCsv.startsWith("!")) {
      inclusionList = false;
      domainsCsv = domainsCsv.substring(1);
    } else {
      inclusionList = true;
    }
    filteredDomains = parseCsv(domainsCsv);

    Map<String, List<String>> exclusions = new LinkedHashMap<>();
    for (String domain : supportedDomains) {
      String exclusionsCsv = propertyResolver.apply(PROPS_EXCLUDE + "." + domain);
      List<String> domainExclusions = parseCsvList(exclusionsCsv);
      if (!domainExclusions.isEmpty()) {
        exclusions.put(domain, domainExclusions);
      }
    }
    wildcardExclusions = Collections.unmodifiableMap(exclusions);
  }

  public Set<String> getFilteredDomains() {
    return filteredDomains;
  }

  public boolean isInclusionList() {
    return inclusionList;
  }

  public List<String> getWildcardExclusions(String domainName) {
    return wildcardExclusions.getOrDefault(domainName, List.of());
  }

  public String getStartupLoadingMode() {
    return startupLoadingMode;
  }

  private Set<String> parseCsv(String value) {
    return new LinkedHashSet<>(parseCsvList(value));
  }

  private List<String> parseCsvList(String value) {
    if (StringUtils.isBlank(value)) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
  }
}
