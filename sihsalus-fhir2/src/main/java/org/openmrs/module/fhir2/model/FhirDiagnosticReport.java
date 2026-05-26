/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "fhir_diagnostic_report")
public class FhirDiagnosticReport extends BaseOpenmrsData {

  private static final long serialVersionUID = 1L;

  @EqualsAndHashCode.Include
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "diagnostic_report_id")
  private Integer id;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private DiagnosticReportStatus status;

  @OneToOne
  @JoinColumn(name = "concept_id", referencedColumnName = "concept_id", nullable = false)
  private Concept code;

  @OneToOne
  @JoinColumn(name = "subject_id", referencedColumnName = "patient_id")
  private Patient subject;

  @OneToOne
  @JoinColumn(name = "encounter_id", referencedColumnName = "encounter_id")
  private Encounter encounter;

  @Column(name = "issued")
  private Date issued;

  @OneToMany
  @JoinTable(
      name = "fhir_diagnostic_report_performers",
      joinColumns = @JoinColumn(name = "diagnostic_report_id"),
      inverseJoinColumns = @JoinColumn(name = "provider_id"))
  private Set<Provider> performers = new HashSet<>();

  @OneToMany
  @JoinTable(
      name = "fhir_diagnostic_report_results",
      joinColumns = @JoinColumn(name = "diagnostic_report_id"),
      inverseJoinColumns = @JoinColumn(name = "obs_id"))
  private Set<Obs> results = new HashSet<>();

  /**
   * @Since 2.8.1
   *
   * Conclusion of results.
   */
  @Column(name = "conclusion", length = 1024)
  private String conclusion;

  /**
   * @Since 2.8.1
   *
   * References to service requests the report is based on.
   */
  @OneToMany
  @JoinTable(
      name = "fhir_diagnostic_report_service_request",
      joinColumns = @JoinColumn(name = "diagnostic_report_id"),
      inverseJoinColumns = @JoinColumn(name = "order_id"))
  private Set<Order> orders = new HashSet<>();

  public enum DiagnosticReportStatus {
    REGISTERED,
    PARTIAL,
    PRELIMINARY,
    AMENDED,
    CANCELLED,
    FINAL,
    UNKNOWN
  }
}
