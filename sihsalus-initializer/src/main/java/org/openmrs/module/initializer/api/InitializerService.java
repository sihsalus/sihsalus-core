package org.openmrs.module.initializer.api;

import org.openmrs.api.OpenmrsService;

public interface InitializerService extends OpenmrsService {

    String getValueFromKey(String key);

    Boolean getBooleanFromKey(String key);
}
