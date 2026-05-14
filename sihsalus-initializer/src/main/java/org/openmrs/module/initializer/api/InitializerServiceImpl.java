package org.openmrs.module.initializer.api;

import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;

public class InitializerServiceImpl extends BaseOpenmrsService implements InitializerService {

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
}
