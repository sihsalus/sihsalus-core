package org.openmrs.module.attachments.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openmrs.Concept;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.attachments.AttachmentsConstants;
import org.openmrs.module.attachments.AttachmentsContext;
import org.openmrs.module.attachments.AttachmentsService;
import org.openmrs.module.attachments.obs.Attachment;
import org.openmrs.module.attachments.obs.ComplexDataHelperImpl;
import org.openmrs.obs.ComplexData;
import org.sihsalus.core.api.authorization.PatientObjectAccessDeniedException;
import org.sihsalus.core.api.authorization.PatientObjectAuthorizationService;

class AttachmentResourceTest {

  private static final String ATTACHMENT_UUID = "1234";
  private static final String PATIENT_UUID = "patient-uuid";

  private MockedStatic<Context> mockedContext;
  private PatientObjectAuthorizationService patientAuthorization;

  @BeforeEach
  void setUp() {
    mockedContext = mockStatic(Context.class);
    AttachmentsContext attachmentsContext = mock(AttachmentsContext.class);
    patientAuthorization = mock(PatientObjectAuthorizationService.class);

    when(attachmentsContext.getComplexDataHelper()).thenReturn(new ComplexDataHelperImpl());
    mockedContext
        .when(
            () ->
                Context.getRegisteredComponent(
                    AttachmentsConstants.COMPONENT_ATT_CONTEXT, AttachmentsContext.class))
        .thenReturn(attachmentsContext);
    mockedContext
        .when(
            () ->
                Context.getRegisteredComponent(
                    PatientObjectAuthorizationService.BEAN_NAME,
                    PatientObjectAuthorizationService.class))
        .thenReturn(patientAuthorization);
  }

  @AfterEach
  void tearDown() {
    mockedContext.close();
  }

  @Test
  void getByUniqueIdReturnsFilenameProperty() {
    AttachmentResource resource = new AttachmentResource();
    ObsService obsService = mock(ObsService.class);
    mockedContext.when(Context::getObsService).thenReturn(obsService);
    Obs attachmentObs = attachmentObs("filename.png", patient());
    when(obsService.getObsByUuid(ATTACHMENT_UUID)).thenReturn(attachmentObs);

    Attachment attachment = resource.getByUniqueId(ATTACHMENT_UUID);

    assertEquals("filename.png", attachment.getFilename());
  }

  @Test
  void getByUniqueIdReturnsFilenamePropertyIfNewCore28Format() {
    AttachmentResource resource = new AttachmentResource();
    ObsService obsService = mock(ObsService.class);
    mockedContext.when(Context::getObsService).thenReturn(obsService);
    Obs attachmentObs =
        attachmentObs(
            "filename.png image |complex_obs/some-path/some-uuid_filename.png", patient());
    when(obsService.getObsByUuid(ATTACHMENT_UUID)).thenReturn(attachmentObs);

    Attachment attachment = resource.getByUniqueId(ATTACHMENT_UUID);

    assertEquals("filename.png", attachment.getFilename());
  }

  @Test
  void getByUniqueIdRequiresPatientReadAccess() {
    AttachmentResource resource = new AttachmentResource();
    ObsService obsService = mock(ObsService.class);
    Patient patient = patient();
    mockedContext.when(Context::getObsService).thenReturn(obsService);
    when(obsService.getObsByUuid(ATTACHMENT_UUID))
        .thenReturn(attachmentObs("filename.png", patient));

    resource.getByUniqueId(ATTACHMENT_UUID);

    verify(patientAuthorization).requireCanReadPatient(PATIENT_UUID);
  }

  @Test
  void getByUniqueIdStopsWhenPatientReadAccessIsDenied() {
    AttachmentResource resource = new AttachmentResource();
    ObsService obsService = mock(ObsService.class);
    Patient patient = patient();
    mockedContext.when(Context::getObsService).thenReturn(obsService);
    when(obsService.getObsByUuid(ATTACHMENT_UUID))
        .thenReturn(attachmentObs("filename.png", patient));
    doThrow(new PatientObjectAccessDeniedException(PATIENT_UUID))
        .when(patientAuthorization)
        .requireCanReadPatient(PATIENT_UUID);

    assertThrows(
        PatientObjectAccessDeniedException.class, () -> resource.getByUniqueId(ATTACHMENT_UUID));
  }

  @Test
  void searchInvokesApiForEncounterAttachments() {
    AttachmentResource resource = new AttachmentResource();
    AttachmentsService attachmentsService = mock(AttachmentsService.class);
    Patient patient = patient();
    Encounter encounter = new Encounter();

    resource.search(attachmentsService, patient, null, encounter, null, true);

    verify(patientAuthorization).requireCanReadPatient(PATIENT_UUID);
    verify(attachmentsService, times(1)).getAttachments(patient, encounter, true);
    verifyNoMoreInteractions(attachmentsService);
  }

  @Test
  void searchInvokesApiForVisitAttachments() {
    AttachmentResource resource = new AttachmentResource();
    AttachmentsService attachmentsService = mock(AttachmentsService.class);
    Patient patient = patient();
    Visit visit = new Visit();

    resource.search(attachmentsService, patient, visit, null, null, true);

    verify(attachmentsService, times(1)).getAttachments(patient, visit, true);
    verifyNoMoreInteractions(attachmentsService);
  }

  @Test
  void searchInvokesApiForAllAttachments() {
    AttachmentResource resource = new AttachmentResource();
    AttachmentsService attachmentsService = mock(AttachmentsService.class);
    Patient patient = patient();

    resource.search(attachmentsService, patient, null, null, null, true);

    verify(attachmentsService, times(1)).getAttachments(patient, true);
    verifyNoMoreInteractions(attachmentsService);
  }

  @Test
  void searchInvokesApiForEncounterlessAttachments() {
    AttachmentResource resource = new AttachmentResource();
    AttachmentsService attachmentsService = mock(AttachmentsService.class);
    Patient patient = patient();

    resource.search(attachmentsService, patient, null, null, "only", true);

    verify(attachmentsService, times(1)).getEncounterlessAttachments(patient, true);
    verifyNoMoreInteractions(attachmentsService);
  }

  @Test
  void searchInvokesApiForAllAttachmentsButEncounterless() {
    AttachmentResource resource = new AttachmentResource();
    AttachmentsService attachmentsService = mock(AttachmentsService.class);
    Patient patient = patient();

    resource.search(attachmentsService, patient, null, null, "false", true);

    verify(attachmentsService, times(1)).getAttachments(patient, false, true);
    verifyNoMoreInteractions(attachmentsService);
  }

  @Test
  void searchStopsBeforeApiWhenPatientReadAccessIsDenied() {
    AttachmentResource resource = new AttachmentResource();
    AttachmentsService attachmentsService = mock(AttachmentsService.class);
    Patient patient = patient();
    doThrow(new PatientObjectAccessDeniedException(PATIENT_UUID))
        .when(patientAuthorization)
        .requireCanReadPatient(PATIENT_UUID);

    assertThrows(
        PatientObjectAccessDeniedException.class,
        () -> resource.search(attachmentsService, patient, null, null, null, true));

    verifyNoInteractions(attachmentsService);
  }

  private Patient patient() {
    Patient patient = new Patient();
    patient.setUuid(PATIENT_UUID);
    return patient;
  }

  private Obs attachmentObs(String fileName, Patient patient) {
    Obs attachmentObs = new Obs();
    attachmentObs.setUuid(ATTACHMENT_UUID);
    attachmentObs.setId(1);
    attachmentObs.setPerson(patient);
    attachmentObs.setConcept(attachmentConcept());
    attachmentObs.setValueComplex("m3ks | instructions.default | text/plain | " + fileName);
    attachmentObs.setComplexData(new ComplexData(fileName, new byte[] {1, 2, 3}));
    return attachmentObs;
  }

  private Concept attachmentConcept() {
    Concept attachmentConcept = new ConceptComplex();
    ConceptDatatype datatype = new ConceptDatatype();
    datatype.setHl7Abbreviation("ED");
    attachmentConcept.setDatatype(datatype);
    return attachmentConcept;
  }
}
