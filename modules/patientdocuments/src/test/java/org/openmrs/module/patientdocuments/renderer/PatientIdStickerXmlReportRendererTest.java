/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.patientdocuments.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openmrs.module.patientdocuments.reports.PatientIdStickerReportManager.DATASET_KEY_STICKER_FIELDS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.initializer.api.InitializerService;
import org.openmrs.module.patientdocuments.library.PatientIdStickerDataSetDefinition;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.openmrs.module.reporting.dataset.SimpleDataSet;
import org.openmrs.module.reporting.report.ReportData;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.Document;

class PatientIdStickerXmlReportRendererTest {

  @Test
  void renderUsesA4WidthDefaultWhenStickerWidthIsMissing() throws Exception {
    PatientIdStickerXmlReportRenderer renderer = rendererWithMocks();

    Document xml = renderXml(renderer, new ReportData());

    assertEquals("297mm", xml.getDocumentElement().getAttribute("sticker-height"));
    assertEquals("210mm", xml.getDocumentElement().getAttribute("sticker-width"));
  }

  @Test
  void renderAppendsOneBarcodeToTemplateWhenDataSetHasMultipleRows() throws Exception {
    InitializerService initializerService = mock(InitializerService.class);
    when(initializerService.getBooleanFromKey(anyString())).thenReturn(false);
    when(initializerService.getBooleanFromKey("report.patientIdSticker.fields.identifier"))
        .thenReturn(true);
    when(initializerService.getBooleanFromKey("report.patientIdSticker.barcode")).thenReturn(true);

    PatientIdStickerXmlReportRenderer renderer = rendererWithMocks(initializerService);

    Document xml =
        renderXml(
            renderer,
            reportDataWithPatientRows(
                """
                {
                  "identifiers": [
                    {
                      "preferred": true,
                      "identifier": "ABC-123",
                      "identifierTypeUuid": "primary-id"
                    }
                  ]
                }
                """,
                """
                {
                  "identifiers": [
                    {
                      "preferred": true,
                      "identifier": "XYZ-789",
                      "identifierTypeUuid": "primary-id"
                    }
                  ]
                }
                """));

    assertEquals(1, xml.getElementsByTagName("barcode").getLength());
    assertEquals(
        "ABC-123",
        xml.getElementsByTagName("barcode")
            .item(0)
            .getAttributes()
            .getNamedItem("barcodeValue")
            .getNodeValue());
  }

  @Test
  void resolveSecureLogoPathReturnsFileWithinAppDataDirectory() throws Exception {
    PatientIdStickerXmlReportRenderer renderer = new PatientIdStickerXmlReportRenderer();
    Path logosDirectory =
        Files.createDirectories(
            OpenmrsUtil.getApplicationDataDirectoryAsFile().toPath().resolve("logos"));
    Path logoFile = logosDirectory.resolve("custom-logo.png");
    Files.writeString(logoFile, "image-data");

    File resolvedLogoFile = renderer.resolveSecureLogoPath("logos/custom-logo.png");

    assertNotNull(resolvedLogoFile, "Expected logo file within app data directory to be resolved");
    assertEquals(logoFile.toRealPath(), resolvedLogoFile.toPath().toRealPath());
  }

  @Test
  void resolveSecureLogoPathRejectsPathTraversalAttempts() {
    PatientIdStickerXmlReportRenderer renderer = new PatientIdStickerXmlReportRenderer();

    File resolvedLogoFile = renderer.resolveSecureLogoPath("../malicious-logo.png");

    assertNull(resolvedLogoFile, "Path traversal attempts must be rejected");
  }

  @Test
  void resolveSecureLogoPathRejectsAbsolutePaths() throws Exception {
    PatientIdStickerXmlReportRenderer renderer = new PatientIdStickerXmlReportRenderer();
    Path outsideLogo = Files.createTempFile("absolute-path-logo", ".png");

    File resolvedLogoFile = renderer.resolveSecureLogoPath(outsideLogo.toString());

    assertNull(resolvedLogoFile, "Absolute paths must be rejected");
    Files.deleteIfExists(outsideLogo);
  }

  private PatientIdStickerXmlReportRenderer rendererWithMocks() throws Exception {
    InitializerService initializerService = mock(InitializerService.class);
    when(initializerService.getBooleanFromKey(anyString())).thenReturn(false);
    return rendererWithMocks(initializerService);
  }

  private PatientIdStickerXmlReportRenderer rendererWithMocks(InitializerService initializerService)
      throws Exception {
    PatientIdStickerXmlReportRenderer renderer = new PatientIdStickerXmlReportRenderer();

    MessageSourceService messageSourceService = mock(MessageSourceService.class);
    when(messageSourceService.getMessage(anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    injectField(renderer, "initializerService", initializerService);
    injectField(renderer, "mss", messageSourceService);
    return renderer;
  }

  private void injectField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private Document renderXml(PatientIdStickerXmlReportRenderer renderer, ReportData reportData)
      throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    renderer.render(reportData, null, output);
    return DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(output.toByteArray()));
  }

  private ReportData reportDataWithPatientRows(String... patientJsonRows) {
    SimpleDataSet dataSet = new SimpleDataSet(new PatientIdStickerDataSetDefinition(), null);
    DataSetColumn patientDataColumn =
        new DataSetColumn("patientData", "Patient Data", String.class);

    for (String patientJson : List.of(patientJsonRows)) {
      DataSetRow row = new DataSetRow();
      row.addColumnValue(patientDataColumn, patientJson);
      dataSet.addRow(row);
    }

    ReportData reportData = new ReportData();
    reportData.addDataSet(DATASET_KEY_STICKER_FIELDS, dataSet);
    return reportData;
  }
}
