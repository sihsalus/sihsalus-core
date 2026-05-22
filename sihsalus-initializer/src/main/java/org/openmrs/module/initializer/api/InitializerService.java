package org.openmrs.module.initializer.api;

import java.util.List;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.initializer.InitializerConfig;
import org.openmrs.module.initializer.api.loaders.Loader;

public interface InitializerService extends OpenmrsService {

  List<Loader> getLoaders();

  void loadUnsafe(boolean applyFilters, boolean doThrow) throws Exception;

  void load();

  InitializerConfig getInitializerConfig();

  String getValueFromKey(String key);

  Boolean getBooleanFromKey(String key);
}
