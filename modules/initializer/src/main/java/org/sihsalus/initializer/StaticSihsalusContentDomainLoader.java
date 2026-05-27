package org.sihsalus.initializer;

import java.util.List;
import org.openmrs.module.initializer.api.loaders.Loader;

final class StaticSihsalusContentDomainLoader implements Loader {

  private final String domainName;

  private final Integer order;

  private final StaticSihsalusContentLoader contentLoader;

  StaticSihsalusContentDomainLoader(
      String domainName, Integer order, StaticSihsalusContentLoader contentLoader) {
    this.domainName = domainName;
    this.order = order;
    this.contentLoader = contentLoader;
  }

  @Override
  public String getDomainName() {
    return domainName;
  }

  @Override
  public Integer getOrder() {
    return order;
  }

  @Override
  public void loadUnsafe(List<String> wildcardExclusions, boolean doThrow) throws Exception {
    contentLoader.loadDomain(domainName, wildcardExclusions);
  }
}
