package org.openmrs.module.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openmrs.api.APIException;
import org.openmrs.module.attachments.AttachmentsActivator.ConceptComplexDefinition;
import org.openmrs.module.attachments.obs.DefaultAttachmentHandler;
import org.openmrs.module.attachments.obs.ImageAttachmentHandler;

class AttachmentsActivatorTest {

  @Test
  void defaultAttachmentConceptDefinitionUsesConfiguredProperties() {
    TestAttachmentsActivator activator = new TestAttachmentsActivator();
    activator.setGlobalProperty(
        AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_UUID,
        "11111111-1111-4111-8111-111111111111");
    activator.setGlobalProperty(
        AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_NAME, "Configured default");
    activator.setGlobalProperty(
        AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_DESCRIPTION,
        "Configured default description");

    ConceptComplexDefinition definition = activator.getDefaultAttachmentConceptDefinition();

    assertEquals("11111111-1111-4111-8111-111111111111", definition.getUuid());
    assertEquals("Configured default", definition.getName());
    assertEquals("Configured default description", definition.getDescription());
    assertEquals(DefaultAttachmentHandler.class.getSimpleName(), definition.getHandler());
  }

  @Test
  void imageAttachmentConceptDefinitionUsesConfiguredImageMapping() {
    TestAttachmentsActivator activator = new TestAttachmentsActivator();
    activator.setGlobalProperty(
        AttachmentsConstants.GP_CONCEPT_COMPLEX_UUID_MAP,
        "{\"IMAGE\":\"22222222-2222-4222-8222-222222222222\",\"OTHER\":\"33333333-3333-4333-8333-333333333333\"}");
    activator.setGlobalProperty(
        AttachmentsConstants.GP_IMAGE_CONCEPT_COMPLEX_NAME, "Configured image");
    activator.setGlobalProperty(
        AttachmentsConstants.GP_IMAGE_CONCEPT_COMPLEX_DESCRIPTION, "Configured image description");

    ConceptComplexDefinition definition = activator.getImageAttachmentConceptDefinition();

    assertEquals("22222222-2222-4222-8222-222222222222", definition.getUuid());
    assertEquals("Configured image", definition.getName());
    assertEquals("Configured image description", definition.getDescription());
    assertEquals(ImageAttachmentHandler.class.getSimpleName(), definition.getHandler());
  }

  @Test
  void imageAttachmentConceptDefinitionFallsBackToBuiltInUuidWhenMappingIsMissing() {
    TestAttachmentsActivator activator = new TestAttachmentsActivator();

    ConceptComplexDefinition definition = activator.getImageAttachmentConceptDefinition();

    assertEquals(AttachmentsConstants.CONCEPT_IMAGE_UUID, definition.getUuid());
  }

  @Test
  void defaultAttachmentConceptDefinitionRejectsInvalidConfiguredUuid() {
    TestAttachmentsActivator activator = new TestAttachmentsActivator();
    activator.setGlobalProperty(AttachmentsConstants.GP_DEFAULT_CONCEPT_COMPLEX_UUID, "not-a-uuid");

    assertThrows(APIException.class, activator::getDefaultAttachmentConceptDefinition);
  }

  private static class TestAttachmentsActivator extends AttachmentsActivator {

    private final Map<String, String> globalProperties = new HashMap<>();

    void setGlobalProperty(String propertyName, String value) {
      globalProperties.put(propertyName, value);
    }

    @Override
    protected String getGlobalProperty(String propertyName) {
      return globalProperties.get(propertyName);
    }
  }
}
