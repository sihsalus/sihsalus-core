package org.openmrs.module.webservices.rest.web.v1_0.resource.sihsalus;

import java.util.Date;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@Resource(
    name = RestConstants.VERSION_1 + "/patient",
    supportedClass = Patient.class,
    supportedOpenmrsVersions = {"1.9.* - 9.*"})
public class PatientResource extends DataDelegatingCrudResource<Patient> {

  @Override
  public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
    if (representation instanceof RefRepresentation) {
      DelegatingResourceDescription description = new DelegatingResourceDescription();
      description.addProperty("uuid");
      description.addProperty("display");
      description.addSelfLink();
      return description;
    }

    if (representation instanceof DefaultRepresentation
        || representation instanceof FullRepresentation) {
      DelegatingResourceDescription description = new DelegatingResourceDescription();
      description.addProperty("uuid");
      description.addProperty("display");
      description.addProperty("identifier");
      description.addProperty("givenName");
      description.addProperty("familyName");
      description.addProperty("gender");
      description.addProperty("birthdate");
      description.addProperty("voided");
      description.addSelfLink();
      if (representation instanceof DefaultRepresentation) {
        description.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
      } else {
        description.addProperty("auditInfo");
      }
      return description;
    }

    return null;
  }

  @Override
  public Patient newDelegate() {
    return new Patient();
  }

  @Override
  public Patient save(Patient patient) {
    return Context.getPatientService().savePatient(patient);
  }

  @Override
  public Patient getByUniqueId(String uuid) {
    return Context.getPatientService().getPatientByUuid(trimUuid(uuid));
  }

  @Override
  protected String getUniqueId(Patient patient) {
    return trimUuid(patient.getUuid());
  }

  @Override
  protected void delete(Patient patient, String reason, RequestContext context)
      throws ResponseException {
    if (!patient.isVoided()) {
      Context.getPatientService().voidPatient(patient, reason);
    }
  }

  @Override
  protected Patient undelete(Patient patient, RequestContext context) throws ResponseException {
    if (patient.isVoided()) {
      return Context.getPatientService().unvoidPatient(patient);
    }
    return patient;
  }

  @Override
  public void purge(Patient patient, RequestContext context) throws ResponseException {
    Context.getPatientService().purgePatient(patient);
  }

  @PropertyGetter("display")
  public String getDisplayString(Patient patient) {
    PatientIdentifier identifier = patient.getPatientIdentifier();
    PersonName name = patient.getPersonName();
    if (identifier != null && name != null) {
      return identifier.getIdentifier() + " - " + name.getFullName();
    }
    if (name != null) {
      return name.getFullName();
    }
    return trimUuid(patient.getUuid());
  }

  @PropertyGetter("uuid")
  public String getUuid(Patient patient) {
    return trimUuid(patient.getUuid());
  }

  @PropertyGetter("identifier")
  public String getIdentifier(Patient patient) {
    PatientIdentifier identifier = patient.getPatientIdentifier();
    return identifier == null ? null : identifier.getIdentifier();
  }

  @PropertyGetter("givenName")
  public String getGivenName(Patient patient) {
    PersonName name = patient.getPersonName();
    return name == null ? null : name.getGivenName();
  }

  @PropertyGetter("familyName")
  public String getFamilyName(Patient patient) {
    PersonName name = patient.getPersonName();
    return name == null ? null : name.getFamilyName();
  }

  @PropertyGetter("birthdate")
  public Date getBirthdate(Patient patient) {
    return patient.getBirthdate();
  }

  private String trimUuid(String uuid) {
    return uuid == null ? null : uuid.trim();
  }
}
