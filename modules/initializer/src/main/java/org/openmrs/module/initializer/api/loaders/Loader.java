package org.openmrs.module.initializer.api.loaders;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public interface Loader extends Comparable<Loader> {

  default boolean isPreLoader() {
    return false;
  }

  String getDomainName();

  Integer getOrder();

  void loadUnsafe(List<String> wildcardExclusions, boolean doThrow) throws Exception;

  default void load(List<String> wildcardExclusions) {
    try {
      loadUnsafe(wildcardExclusions, false);
    } catch (Exception e) {
      Logger log = LoggerFactory.getLogger(getClass());
      log.error("Failed to load initializer domain {}.", getDomainName(), e);
    }
  }

  default void load() {
    load(Collections.emptyList());
  }

  @Override
  default int compareTo(Loader that) {
    int order = getOrder().compareTo(that.getOrder());
    if (order != 0) {
      return order;
    }
    return getDomainName().compareTo(that.getDomainName());
  }
}
