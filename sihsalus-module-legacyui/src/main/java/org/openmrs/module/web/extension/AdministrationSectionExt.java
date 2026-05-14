package org.openmrs.module.web.extension;

import java.util.Map;
import org.openmrs.module.Extension;

public abstract class AdministrationSectionExt extends Extension {

    @Override
    public Extension.MEDIA_TYPE getMediaType() {
        return Extension.MEDIA_TYPE.html;
    }

    public abstract String getTitle();

    public String getRequiredPrivilege() {
        return "";
    }

    public abstract Map<String, String> getLinks();
}
