package org.openmrs.module.appointments.web.contract;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentDefaultResponse {
  private String uuid;
  private String appointmentNumber;
  private Date dateCreated;
  private Date dateAppointmentScheduled;
  private Map<String, ?> patient;
  private AppointmentServiceDefaultResponse service;
  private Map<String, ?> serviceType;
  private Map<String, ?> provider;
  private Map<String, ?> location;
  private Date startDateTime;
  private Date endDateTime;
  private String appointmentKind;
  private String status;
  private String comments;
  private Map<String, ?> additionalInfo;
  private Boolean teleconsultation;
  private List<AppointmentProviderDetail> providers;
  private Boolean isRecurring;
  private Boolean voided;
  private HashMap<Object, Object> extensions;
  private String teleconsultationLink;
  private String priority;
  private List<AppointmentReasonResponse> reasons;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getAppointmentNumber() {
    return appointmentNumber;
  }

  public void setAppointmentNumber(String appointmentNumber) {
    this.appointmentNumber = appointmentNumber;
  }

  public Map<String, ?> getPatient() {
    return patient;
  }

  public void setPatient(Map<String, ?> patient) {
    this.patient = patient;
  }

  public AppointmentServiceDefaultResponse getService() {
    return service;
  }

  public void setService(AppointmentServiceDefaultResponse service) {
    this.service = service;
  }

  public Map<String, ?> getServiceType() {
    return serviceType;
  }

  public void setServiceType(Map<String, ?> serviceType) {
    this.serviceType = serviceType;
  }

  public Map<String, ?> getProvider() {
    return provider;
  }

  public void setProvider(Map<String, ?> provider) {
    this.provider = provider;
  }

  public Map<String, ?> getLocation() {
    return location;
  }

  public void setLocation(Map<String, ?> location) {
    this.location = location;
  }

  public Date getStartDateTime() {
    return startDateTime;
  }

  public void setStartDateTime(Date startDateTime) {
    this.startDateTime = startDateTime;
  }

  public Date getEndDateTime() {
    return endDateTime;
  }

  public void setEndDateTime(Date endDateTime) {
    this.endDateTime = endDateTime;
  }

  public String getAppointmentKind() {
    return appointmentKind;
  }

  public void setAppointmentKind(String appointmentKind) {
    this.appointmentKind = appointmentKind;
  }

  public Boolean isTeleconsultation() {
    return teleconsultation;
  }

  public void setTeleconsultation(Boolean teleconsultation) {
    this.teleconsultation = teleconsultation;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public Map<String, ?> getAdditionalInfo() {
    return additionalInfo;
  }

  public void setAdditionalInfo(Map<String, ?> additionalInfo) {
    this.additionalInfo = additionalInfo;
  }

  public void setProviders(List<AppointmentProviderDetail> providers) {
    this.providers = providers;
  }

  public List<AppointmentProviderDetail> getProviders() {
    return providers;
  }

  public Boolean getRecurring() {
    return isRecurring;
  }

  public void setRecurring(Boolean recurring) {
    isRecurring = recurring;
  }

  public Boolean getVoided() {
    return voided;
  }

  public void setVoided(Boolean voided) {
    this.voided = voided;
  }

  public void setExtensions(HashMap<Object, Object> extensions) {
    this.extensions = extensions == null ? null : new HashMap<>(extensions);
  }

  public HashMap<Object, Object> getExtensions() {
    return extensions == null ? null : new HashMap<>(extensions);
  }

  public void putExtension(Object key, Object value) {
    if (extensions == null) {
      extensions = new HashMap<>();
    }
    extensions.put(key, value);
  }

  public void setTeleconsultationLink(String teleconsultationLink) {
    this.teleconsultationLink = teleconsultationLink;
  }

  public String getTeleconsultationLink() {
    return teleconsultationLink;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getPriority() {
    return priority;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public Date getDateAppointmentScheduled() {
    return dateAppointmentScheduled;
  }

  public void setDateAppointmentScheduled(Date dateAppointmentScheduled) {
    this.dateAppointmentScheduled = dateAppointmentScheduled;
  }

  public List<AppointmentReasonResponse> getReasons() {
    return reasons;
  }

  public void setReasons(List<AppointmentReasonResponse> reasons) {
    this.reasons = reasons;
  }
}
