package org.openmrs.api.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Diagnosis;
import org.openmrs.DiagnosisAttribute;
import org.openmrs.DiagnosisAttributeType;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.DiagnosisDAO;

class DiagnosisServiceImplTest {

  private final User authenticatedUser = new User(501);

  private DiagnosisServiceImpl service;

  private RecordingDiagnosisDAO diagnosisDAO;

  @BeforeEach
  void setUp() throws Exception {
    diagnosisDAO = new RecordingDiagnosisDAO();
    service = new DiagnosisServiceImpl();
    service.setDiagnosisDAO(diagnosisDAO);
    setAuthenticatedUser(authenticatedUser);
  }

  @AfterEach
  void tearDown() {
    Context.clearUserContext();
  }

  @Test
  void voidDiagnosisSetsAuditFields() {
    Diagnosis diagnosis = new Diagnosis();

    Diagnosis result = service.voidDiagnosis(diagnosis, "entered in error");

    assertSame(diagnosis, result);
    assertSame(diagnosis, diagnosisDAO.savedDiagnosis);
    assertTrue(diagnosis.getVoided());
    assertSame(authenticatedUser, diagnosis.getVoidedBy());
    assertNotNull(diagnosis.getDateVoided());
    assertSame("entered in error", diagnosis.getVoidReason());
  }

  @Test
  void unvoidDiagnosisClearsAuditFields() {
    Diagnosis diagnosis = new Diagnosis();
    diagnosis.setVoided(true);
    diagnosis.setVoidedBy(authenticatedUser);
    diagnosis.setDateVoided(new Date());
    diagnosis.setVoidReason("entered in error");

    Diagnosis result = service.unvoidDiagnosis(diagnosis);

    assertSame(diagnosis, result);
    assertSame(diagnosis, diagnosisDAO.savedDiagnosis);
    assertFalse(diagnosis.getVoided());
    assertNull(diagnosis.getVoidedBy());
    assertNull(diagnosis.getDateVoided());
    assertNull(diagnosis.getVoidReason());
  }

  @Test
  void retireDiagnosisAttributeTypeSetsAuditFields() {
    DiagnosisAttributeType attributeType = new DiagnosisAttributeType();

    DiagnosisAttributeType result = service.retireDiagnosisAttributeType(attributeType, "obsolete");

    assertSame(attributeType, result);
    assertSame(attributeType, diagnosisDAO.savedAttributeType);
    assertTrue(attributeType.getRetired());
    assertSame(authenticatedUser, attributeType.getRetiredBy());
    assertNotNull(attributeType.getDateRetired());
    assertSame("obsolete", attributeType.getRetireReason());
  }

  @Test
  void unretireDiagnosisAttributeTypeClearsAuditFields() {
    DiagnosisAttributeType attributeType = new DiagnosisAttributeType();
    attributeType.setRetired(true);
    attributeType.setRetiredBy(authenticatedUser);
    attributeType.setDateRetired(new Date());
    attributeType.setRetireReason("obsolete");

    DiagnosisAttributeType result = service.unretireDiagnosisAttributeType(attributeType);

    assertSame(attributeType, result);
    assertSame(attributeType, diagnosisDAO.savedAttributeType);
    assertFalse(attributeType.getRetired());
    assertNull(attributeType.getRetiredBy());
    assertNull(attributeType.getDateRetired());
    assertNull(attributeType.getRetireReason());
  }

  private static void setAuthenticatedUser(User user) throws Exception {
    UserContext userContext = new UserContext(credentials -> null);
    Field userField = UserContext.class.getDeclaredField("user");
    userField.setAccessible(true);
    userField.set(userContext, user);
    Context.setUserContext(userContext);
  }

  private static final class RecordingDiagnosisDAO implements DiagnosisDAO {

    private Diagnosis savedDiagnosis;

    private DiagnosisAttributeType savedAttributeType;

    @Override
    public Diagnosis saveDiagnosis(Diagnosis diagnosis) throws DAOException {
      savedDiagnosis = diagnosis;
      return diagnosis;
    }

    @Override
    public Diagnosis getDiagnosisById(Integer diagnosisId) throws DAOException {
      return null;
    }

    @Override
    public Diagnosis getDiagnosisByUuid(String uuid) {
      return null;
    }

    @Override
    public void deleteDiagnosis(Diagnosis diagnosis) throws DAOException {}

    @Override
    public List<Diagnosis> getDiagnosesByEncounter(
        Encounter encounter, boolean primaryOnly, boolean confirmedOnly) {
      return List.of();
    }

    @Override
    public List<Diagnosis> getDiagnosesByVisit(
        Visit visit, boolean primaryOnly, boolean confirmedOnly) {
      return List.of();
    }

    @Override
    public List<Diagnosis> getActiveDiagnoses(Patient patient, Date fromDate) {
      return List.of();
    }

    @Override
    public List<DiagnosisAttributeType> getAllDiagnosisAttributeTypes() throws DAOException {
      return List.of();
    }

    @Override
    public DiagnosisAttributeType getDiagnosisAttributeTypeById(Integer id) throws DAOException {
      return null;
    }

    @Override
    public DiagnosisAttributeType getDiagnosisAttributeTypeByUuid(String uuid) throws DAOException {
      return null;
    }

    @Override
    public DiagnosisAttributeType saveDiagnosisAttributeType(
        DiagnosisAttributeType diagnosisAttributeType) throws DAOException {
      savedAttributeType = diagnosisAttributeType;
      return diagnosisAttributeType;
    }

    @Override
    public void deleteDiagnosisAttributeType(DiagnosisAttributeType diagnosisAttributeType)
        throws DAOException {}

    @Override
    public DiagnosisAttribute getDiagnosisAttributeByUuid(String uuid) throws DAOException {
      return null;
    }
  }
}
