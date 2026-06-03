package org.openmrs.module.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.attachments.AttachmentsConstants.ContentFamily;

class AttachmentsContextTest {

  private final AttachmentsContext context = new AttachmentsContext();

  private final AdministrationService adminService = mock(AdministrationService.class);

  @BeforeEach
  void setUp() {
    context.administrationService = adminService;
  }

  @Test
  void getMapByGlobalPropertyReturnsSimpleMap() {
    String imageFamily = ContentFamily.IMAGE.toString();
    String imageUuid = "7cac8397-53cd-4f00-a6fe-028e8d743f8e";
    String otherFamily = ContentFamily.OTHER.toString();
    String otherUuid = "42ed45fd-f3f6-44b6-bfc2-8bde1bb41e00";

    String jsonMap =
        "{\""
            + imageFamily
            + "\":\""
            + imageUuid
            + "\",\""
            + otherFamily
            + "\":\""
            + otherUuid
            + "\"}";
    String globalPropertyName = AttachmentsConstants.GP_CONCEPT_COMPLEX_UUID_MAP;
    when(adminService.getGlobalProperty(eq(globalPropertyName))).thenReturn(jsonMap);

    Map<String, String> map = context.getMapByGlobalProperty(globalPropertyName);

    assertEquals(2, map.size());
    assertEquals(imageUuid, map.get(imageFamily));
    assertEquals(otherUuid, map.get(otherFamily));
  }

  @Test
  void getConceptComplexListReturnsConfiguredList() {
    String imageUuid = "7cac8397-53cd-4f00-a6fe-028e8d743f8e";
    String otherUuid = "42ed45fd-f3f6-44b6-bfc2-8bde1bb41e00";

    String jsonList = "[\"" + imageUuid + "\",\"" + otherUuid + "\"]";
    when(adminService.getGlobalProperty(eq(AttachmentsConstants.GP_CONCEPT_COMPLEX_UUID_LIST)))
        .thenReturn(jsonList);

    List<String> list = context.getConceptComplexList();

    assertEquals(2, list.size());
    assertEquals(imageUuid, list.get(0));
    assertEquals(otherUuid, list.get(1));
  }
}
