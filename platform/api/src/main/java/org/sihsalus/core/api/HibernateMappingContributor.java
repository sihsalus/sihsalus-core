package org.sihsalus.core.api;

import java.util.List;

/** Static replacement for OpenMRS module-discovered Hibernate mappings. */
public interface HibernateMappingContributor {

  List<String> mappingResources();
}
