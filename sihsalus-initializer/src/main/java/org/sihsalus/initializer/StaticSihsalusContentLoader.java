package org.sihsalus.initializer;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class StaticSihsalusContentLoader {

  private static final Logger log = LoggerFactory.getLogger(StaticSihsalusContentLoader.class);

  private static final int SYSTEM_USER_ID = 1;

  private static final String CONFIGURATION_ROOT = "configuration/backend_configuration";

  private final JdbcTemplate jdbcTemplate;

  private final Map<String, Boolean> tableCache = new HashMap<>();

  private final Map<String, Set<String>> columnCache = new HashMap<>();

  StaticSihsalusContentLoader(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  void load() {
    try {
      loadDomain("conceptclasses", List.of());
      loadDomain("conceptsources", List.of());
      loadDomain("visittypes", List.of());
      loadDomain("patientidentifiertypes", List.of());
      loadDomain("relationshiptypes", List.of());
      loadDomain("privileges", List.of());
      loadDomain("encountertypes", List.of());
      loadDomain("encounterroles", List.of());
      loadDomain("locationtags", List.of());
      loadDomain("roles", List.of());
      loadDomain("globalproperties", List.of());
      loadDomain("attributetypes", List.of());
      loadDomain("locations", List.of());
      loadDomain("personattributetypes", List.of());
      loadDomain("ordertypes", List.of());
      loadDomain("billableservices", List.of());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load static SIH Salus content package.", e);
    }
  }

  void loadDomain(String domainName, List<String> wildcardExclusions) throws Exception {
    Path configRoot = resolveConfigRoot();
    if (configRoot == null) {
      return;
    }

    switch (domainName) {
      case "conceptclasses":
        loadConceptClasses(configRoot, wildcardExclusions);
        break;
      case "conceptsources":
        loadConceptSources(configRoot, wildcardExclusions);
        break;
      case "visittypes":
        loadVisitTypes(configRoot, wildcardExclusions);
        break;
      case "patientidentifiertypes":
        loadPatientIdentifierTypes(configRoot, wildcardExclusions);
        break;
      case "relationshiptypes":
        loadRelationshipTypes(configRoot, wildcardExclusions);
        break;
      case "privileges":
        loadPrivileges(configRoot, wildcardExclusions);
        break;
      case "encountertypes":
        loadEncounterTypes(configRoot, wildcardExclusions);
        break;
      case "encounterroles":
        loadEncounterRoles(configRoot, wildcardExclusions);
        break;
      case "roles":
        loadRoles(configRoot, wildcardExclusions);
        break;
      case "globalproperties":
        loadGlobalProperties(configRoot, wildcardExclusions);
        break;
      case "attributetypes":
        loadAttributeTypes(configRoot, wildcardExclusions);
        break;
      case "locationtags":
        loadLocationTags(configRoot, wildcardExclusions);
        break;
      case "locations":
        loadLocations(configRoot, wildcardExclusions);
        break;
      case "personattributetypes":
        loadPersonAttributeTypes(configRoot, wildcardExclusions);
        break;
      case "ordertypes":
        loadOrderTypes(configRoot, wildcardExclusions);
        break;
      case "billableservices":
        loadBillableServices(configRoot, wildcardExclusions);
        break;
      default:
        log.debug("No static SIH Salus content loader is registered for domain {}.", domainName);
    }
  }

  private void loadConceptClasses(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("concept_class")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "conceptclasses", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertConceptClass(record);
      }
    }
  }

  private void loadConceptSources(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("concept_reference_source")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "conceptsources", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertConceptSource(record);
      }
    }
  }

  private void loadVisitTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("visit_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "visittypes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertVisitType(record);
      }
    }
  }

  private void loadPatientIdentifierTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("patient_identifier_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "patientidentifiertypes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertPatientIdentifierType(record);
      }
    }
  }

  private void loadRelationshipTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("relationship_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "relationshiptypes", wildcardExclusions)) {
      if (!isBlank(record.value("A is to B")) && !isBlank(record.value("B is to A"))) {
        upsertRelationshipType(record);
      }
    }
  }

  private Path resolveConfigRoot() throws IOException {
    Path sourceRoot = resolveSourceRoot();
    if (!Files.isDirectory(sourceRoot)) {
      log.info(
          "SIH Salus content source directory was not found at {}, skipping static content load.",
          sourceRoot);
      return null;
    }

    Path realSourceRoot = sourceRoot.toRealPath();
    Path configRoot = realSourceRoot.resolve(CONFIGURATION_ROOT).normalize();
    if (!Files.isDirectory(configRoot)) {
      log.info(
          "SIH Salus content configuration directory was not found at {}, skipping static content load.",
          configRoot);
      return null;
    }

    Path realConfigRoot = configRoot.toRealPath();
    if (!realConfigRoot.startsWith(realSourceRoot)) {
      throw new IllegalStateException(
          "SIH Salus content configuration escapes the configured source directory.");
    }
    return realConfigRoot;
  }

  private Path resolveSourceRoot() {
    Path sourceLayout = Paths.get(InitializerBoundary.sourceLayout());
    if (sourceLayout.isAbsolute()) {
      return sourceLayout.normalize();
    }

    Path current = Paths.get("").toAbsolutePath().normalize();
    for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
      Path resolved = candidate.resolve(sourceLayout).normalize();
      if (Files.isDirectory(resolved)) {
        return resolved;
      }
    }
    return current.resolve(sourceLayout).normalize();
  }

  private void loadPrivileges(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("privilege")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "privileges", wildcardExclusions)) {
      String name = record.value("Privilege name");
      if (isBlank(name)) {
        continue;
      }
      upsertPrivilege(
          name,
          record.value("Description"),
          valueOrStableUuid(record.value("Uuid"), "privilege", name));
    }
  }

  private void loadRoles(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("role")) {
      return;
    }

    List<CsvRecord> records = readDomain(configRoot, "roles", wildcardExclusions);
    for (CsvRecord record : records) {
      String role = record.value("Role name");
      if (isBlank(role)) {
        continue;
      }
      upsertRole(
          role, record.value("Description"), valueOrStableUuid(record.value("Uuid"), "role", role));
    }

    if (tableExists("role_role")) {
      for (CsvRecord record : records) {
        String childRole = record.value("Role name");
        if (isBlank(childRole)) {
          continue;
        }
        for (String parentRole : splitList(record.value("Inherited roles"))) {
          ensureRole(parentRole);
          if (countRows(
                  "select count(*) from role_role where parent_role = ? and child_role = ?",
                  parentRole,
                  childRole)
              == 0) {
            jdbcTemplate.update(
                "insert into role_role (parent_role, child_role) values (?, ?)",
                parentRole,
                childRole);
          }
        }
      }
    }

    if (tableExists("role_privilege")) {
      for (CsvRecord record : records) {
        String role = record.value("Role name");
        if (isBlank(role)) {
          continue;
        }
        for (String privilege : splitList(record.value("Privileges"))) {
          ensurePrivilege(privilege);
          if (countRows(
                  "select count(*) from role_privilege where role = ? and privilege = ?",
                  role,
                  privilege)
              == 0) {
            jdbcTemplate.update(
                "insert into role_privilege (role, privilege) values (?, ?)", role, privilege);
          }
        }
      }
    }
  }

  private void loadEncounterTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("encounter_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "encountertypes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertEncounterType(record);
      }
    }
  }

  private void loadEncounterRoles(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("encounter_role")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "encounterroles", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertEncounterRole(record);
      }
    }
  }

  private void loadGlobalProperties(Path configRoot, List<String> wildcardExclusions)
      throws IOException, ParserConfigurationException, SAXException {
    if (!tableExists("global_property")) {
      return;
    }

    for (Path xmlFile : xmlFiles(configRoot, "globalproperties", wildcardExclusions)) {
      for (GlobalPropertyRecord record : readGlobalProperties(xmlFile)) {
        if (!isBlank(record.property())) {
          upsertGlobalProperty(record);
        }
      }
    }
  }

  private void loadAttributeTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    for (CsvRecord record : readDomain(configRoot, "attributetypes", wildcardExclusions)) {
      if (!isBlank(record.value("Entity name")) && !isBlank(record.value("Name"))) {
        upsertAttributeType(record);
      }
    }
  }

  private void loadLocationTags(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("location_tag")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "locationtags", wildcardExclusions)) {
      String name = record.value("Name");
      if (isBlank(name)) {
        continue;
      }
      upsertLocationTag(
          name,
          record.value("Description"),
          valueOrStableUuid(record.value("Uuid"), "location-tag", name),
          toBoolean(record.value("Void/Retire")));
    }
  }

  private void loadLocations(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("location")) {
      return;
    }

    List<CsvRecord> records = readDomain(configRoot, "locations", wildcardExclusions);
    for (CsvRecord record : records) {
      if (!isBlank(record.value("Name"))) {
        upsertLocation(record);
      }
    }

    for (CsvRecord record : records) {
      String name = record.value("Name");
      if (isBlank(name)) {
        continue;
      }
      Integer locationId =
          findLocationId(valueOrStableUuid(record.value("Uuid"), "location", name), name);
      applyLocationParent(locationId, record.value("Parent"));
      applyLocationTags(locationId, record);
    }
  }

  private void loadPersonAttributeTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("person_attribute_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "personattributetypes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertPersonAttributeType(record);
      }
    }
  }

  private void loadBillableServices(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("cashier_billable_service")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "billableservices", wildcardExclusions)) {
      String name = record.value("service name");
      if (isBlank(name)) {
        continue;
      }
      upsertBillableService(record);
    }
  }

  private void loadOrderTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("order_type")) {
      return;
    }

    List<CsvRecord> records = readDomain(configRoot, "ordertypes", wildcardExclusions);
    for (CsvRecord record : records) {
      if (!isBlank(record.value("Name"))) {
        upsertOrderType(record);
      }
    }

    for (CsvRecord record : records) {
      if (!isBlank(record.value("Name"))) {
        applyOrderTypeParent(record);
        applyOrderTypeConceptClasses(record);
      }
    }
  }

  private void upsertConceptClass(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "concept-class", name);
    Integer id = findIdByUuidOrName("concept_class", "concept_class_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into concept_class "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 255),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update concept_class set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? where concept_class_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 255),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private void upsertConceptSource(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "concept-source", name);
    Integer id = findIdByUuidOrName("concept_reference_source", "concept_source_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    String description = firstNonBlank(record.value("Description"), name);

    if (id == null) {
      jdbcTemplate.update(
          "insert into concept_reference_source "
              + "(name, description, hl7_code, creator, date_created, retired, retired_by, date_retired, "
              + "retire_reason, uuid, unique_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          description,
          limit(record.value("HL7 Code"), 50),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid,
          limit(record.value("Unique ID"), 250));
      return;
    }

    jdbcTemplate.update(
        "update concept_reference_source set name = ?, description = ?, hl7_code = ?, changed_by = ?, "
            + "date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ?, "
            + "unique_id = ? where concept_source_id = ?",
        limit(name, 50),
        description,
        limit(record.value("HL7 Code"), 50),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        limit(record.value("Unique ID"), 250),
        id);
  }

  private void upsertVisitType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "visit-type", name);
    Integer id = findIdByUuidOrName("visit_type", "visit_type_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into visit_type "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 1024),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update visit_type set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? where visit_type_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 1024),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private void upsertPatientIdentifierType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "patient-identifier-type", name);
    Integer id =
        findIdByUuidOrName("patient_identifier_type", "patient_identifier_type_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into patient_identifier_type "
              + "(name, description, format, check_digit, creator, date_created, required, format_description, "
              + "validator, location_behavior, retired, retired_by, date_retired, retire_reason, uuid, "
              + "uniqueness_behavior) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          record.value("Description"),
          limit(record.value("Format"), 255),
          false,
          SYSTEM_USER_ID,
          now,
          toBoolean(record.value("Required")),
          limit(record.value("Format description"), 255),
          limit(record.value("Validator"), 200),
          limit(record.value("Location behavior"), 50),
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid,
          limit(record.value("Uniqueness behavior"), 50));
      return;
    }

    jdbcTemplate.update(
        "update patient_identifier_type set name = ?, description = ?, format = ?, required = ?, "
            + "format_description = ?, validator = ?, location_behavior = ?, changed_by = ?, date_changed = ?, "
            + "retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ?, "
            + "uniqueness_behavior = ? where patient_identifier_type_id = ?",
        limit(name, 50),
        record.value("Description"),
        limit(record.value("Format"), 255),
        toBoolean(record.value("Required")),
        limit(record.value("Format description"), 255),
        limit(record.value("Validator"), 200),
        limit(record.value("Location behavior"), 50),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        limit(record.value("Uniqueness behavior"), 50),
        id);
  }

  private void upsertRelationshipType(CsvRecord record) {
    String aIsToB = record.value("A is to B");
    String bIsToA = record.value("B is to A");
    String uuid =
        valueOrStableUuid(record.value("Uuid"), "relationship-type", aIsToB + ":" + bIsToA);
    Integer id = findRelationshipTypeId(uuid, aIsToB, bIsToA);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into relationship_type "
              + "(a_is_to_b, b_is_to_a, preferred, weight, description, creator, date_created, retired, "
              + "retired_by, date_retired, retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(aIsToB, 50),
          limit(bIsToA, 50),
          toBoolean(record.value("Preferred")),
          toInteger(record.value("Weight"), 0),
          limit(record.value("Description"), 255),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
    } else {
      jdbcTemplate.update(
          "update relationship_type set a_is_to_b = ?, b_is_to_a = ?, preferred = ?, weight = ?, "
              + "description = ?, changed_by = ?, date_changed = ?, retired = ?, retired_by = ?, "
              + "date_retired = ?, retire_reason = ?, uuid = ? where relationship_type_id = ?",
          limit(aIsToB, 50),
          limit(bIsToA, 50),
          toBoolean(record.value("Preferred")),
          toInteger(record.value("Weight"), 0),
          limit(record.value("Description"), 255),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid,
          id);
    }

    if (columnExists("relationship_type", "name")) {
      jdbcTemplate.update(
          "update relationship_type set name = ? where relationship_type_id = ?",
          limit(record.value("Name"), 255),
          findRelationshipTypeId(uuid, aIsToB, bIsToA));
    }
  }

  private void upsertPrivilege(String name, String description, String uuid) {
    if (countRows("select count(*) from privilege where privilege = ?", name) == 0) {
      jdbcTemplate.update(
          "insert into privilege (privilege, description, uuid) values (?, ?, ?)",
          name,
          description,
          uuid);
    } else {
      jdbcTemplate.update(
          "update privilege set description = ?, uuid = ? where privilege = ?",
          description,
          uuid,
          name);
    }
  }

  private void ensurePrivilege(String name) {
    if (!isBlank(name)
        && countRows("select count(*) from privilege where privilege = ?", name) == 0) {
      upsertPrivilege(name, null, stableUuid("privilege", name));
    }
  }

  private void upsertRole(String role, String description, String uuid) {
    String boundedDescription = limit(description, 255);
    if (countRows("select count(*) from role where role = ?", role) == 0) {
      jdbcTemplate.update(
          "insert into role (role, description, uuid) values (?, ?, ?)",
          role,
          boundedDescription,
          uuid);
    } else {
      jdbcTemplate.update(
          "update role set description = ?, uuid = ? where role = ?",
          boundedDescription,
          uuid,
          role);
    }
  }

  private void ensureRole(String role) {
    if (!isBlank(role) && countRows("select count(*) from role where role = ?", role) == 0) {
      upsertRole(role, null, stableUuid("role", role));
    }
  }

  private void upsertEncounterType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "encounter-type", name);
    Integer id = findIdByUuidOrName("encounter_type", "encounter_type_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    String viewPrivilege = record.value("View privilege");
    String editPrivilege = record.value("Edit privilege");
    ensurePrivilege(viewPrivilege);
    ensurePrivilege(editPrivilege);

    if (id == null) {
      jdbcTemplate.update(
          "insert into encounter_type "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, uuid, "
              + "view_privilege, edit_privilege) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          record.value("Description"),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid,
          viewPrivilege,
          editPrivilege);
      return;
    }

    jdbcTemplate.update(
        "update encounter_type set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ?, view_privilege = ?, "
            + "edit_privilege = ? where encounter_type_id = ?",
        limit(name, 50),
        record.value("Description"),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        viewPrivilege,
        editPrivilege,
        id);
  }

  private void upsertEncounterRole(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "encounter-role", name);
    Integer id = findIdByUuidOrName("encounter_role", "encounter_role_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into encounter_role "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 1024),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update encounter_role set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? where encounter_role_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 1024),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private void upsertGlobalProperty(GlobalPropertyRecord record) {
    String uuid = stableUuid("global-property", record.property());
    if (countRows("select count(*) from global_property where property = ?", record.property())
        == 0) {
      jdbcTemplate.update(
          "insert into global_property (property, property_value, description, uuid) values (?, ?, ?, ?)",
          record.property(),
          record.value(),
          record.description(),
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update global_property set property_value = ?, description = ?, changed_by = ?, date_changed = ?, uuid = ? "
            + "where property = ?",
        record.value(),
        record.description(),
        SYSTEM_USER_ID,
        now(),
        uuid,
        record.property());
  }

  private void upsertAttributeType(CsvRecord record) {
    AttributeTypeTable attributeTypeTable = attributeTypeTable(record.value("Entity name"));
    if (attributeTypeTable == null || !tableExists(attributeTypeTable.table())) {
      log.warn(
          "Skipping unsupported SIH Salus attribute type entity '{}'.",
          record.value("Entity name"));
      return;
    }

    String name = record.value("Name");
    String uuid =
        valueOrStableUuid(
            record.value("Uuid"), attributeTypeTable.table() + "-attribute-type", name);
    Integer id =
        findIdByUuidOrName(attributeTypeTable.table(), attributeTypeTable.idColumn(), uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into "
              + attributeTypeTable.table()
              + " (name, description, datatype, datatype_config, preferred_handler, handler_config, "
              + "min_occurs, max_occurs, creator, date_created, retired, retired_by, date_retired, "
              + "retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 1024),
          limit(record.value("Datatype classname"), 255),
          record.value("Datatype config"),
          limit(record.value("Preferred handler classname"), 255),
          record.value("Handler config"),
          toInteger(record.value("Min occurs"), 0),
          toInteger(record.value("Max occurs")),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update "
            + attributeTypeTable.table()
            + " set name = ?, description = ?, datatype = ?, datatype_config = ?, preferred_handler = ?, "
            + "handler_config = ?, min_occurs = ?, max_occurs = ?, changed_by = ?, date_changed = ?, "
            + "retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? where "
            + attributeTypeTable.idColumn()
            + " = ?",
        limit(name, 255),
        limit(record.value("Description"), 1024),
        limit(record.value("Datatype classname"), 255),
        record.value("Datatype config"),
        limit(record.value("Preferred handler classname"), 255),
        record.value("Handler config"),
        toInteger(record.value("Min occurs"), 0),
        toInteger(record.value("Max occurs")),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private Integer upsertLocationTag(String name, String description, String uuid, boolean retired) {
    Integer id = queryInteger("select location_tag_id from location_tag where uuid = ?", uuid);
    if (id == null) {
      id = queryInteger("select location_tag_id from location_tag where name = ?", name);
    }

    Timestamp now = now();
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    if (id == null) {
      jdbcTemplate.update(
          "insert into location_tag "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          limit(description, 255),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return queryInteger("select location_tag_id from location_tag where uuid = ?", uuid);
    }

    jdbcTemplate.update(
        "update location_tag set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? where location_tag_id = ?",
        limit(name, 50),
        limit(description, 255),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
    return id;
  }

  private Integer ensureLocationTag(String name) {
    Integer id = queryInteger("select location_tag_id from location_tag where name = ?", name);
    if (id != null) {
      return id;
    }
    return upsertLocationTag(name, null, stableUuid("location-tag", name), false);
  }

  private Integer upsertLocation(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "location", name);
    Integer id = findLocationId(uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into location "
              + "(name, description, address1, address2, address3, address4, address5, address6, "
              + "city_village, county_district, state_province, postal_code, country, creator, date_created, "
              + "retired, retired_by, date_retired, retire_reason, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 255),
          limit(record.value("Address 1"), 255),
          limit(record.value("Address 2"), 255),
          limit(record.value("Address 3"), 255),
          limit(record.value("Address 4"), 255),
          limit(record.value("Address 5"), 255),
          limit(record.value("Address 6"), 255),
          limit(record.value("City/Village"), 255),
          limit(record.value("County/District"), 255),
          limit(record.value("State/Province"), 255),
          limit(record.value("Postal Code"), 50),
          limit(record.value("Country"), 50),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return queryInteger("select location_id from location where uuid = ?", uuid);
    }

    jdbcTemplate.update(
        "update location set name = ?, description = ?, address1 = ?, address2 = ?, address3 = ?, address4 = ?, "
            + "address5 = ?, address6 = ?, city_village = ?, county_district = ?, state_province = ?, "
            + "postal_code = ?, country = ?, changed_by = ?, date_changed = ?, retired = ?, retired_by = ?, "
            + "date_retired = ?, retire_reason = ?, uuid = ? where location_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 255),
        limit(record.value("Address 1"), 255),
        limit(record.value("Address 2"), 255),
        limit(record.value("Address 3"), 255),
        limit(record.value("Address 4"), 255),
        limit(record.value("Address 5"), 255),
        limit(record.value("Address 6"), 255),
        limit(record.value("City/Village"), 255),
        limit(record.value("County/District"), 255),
        limit(record.value("State/Province"), 255),
        limit(record.value("Postal Code"), 50),
        limit(record.value("Country"), 50),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
    return id;
  }

  private void applyLocationParent(Integer locationId, String parentName) {
    if (locationId == null) {
      return;
    }

    Integer parentId = null;
    if (!isBlank(parentName)) {
      parentId = findLocationId(parentName, parentName);
      if (parentId == null) {
        throw new IllegalStateException("Location parent '" + parentName + "' was not loaded.");
      }
    }

    jdbcTemplate.update(
        "update location set parent_location = ?, changed_by = ?, date_changed = ? where location_id = ?",
        parentId,
        SYSTEM_USER_ID,
        now(),
        locationId);
  }

  private void applyLocationTags(Integer locationId, CsvRecord record) {
    if (locationId == null || !tableExists("location_tag") || !tableExists("location_tag_map")) {
      return;
    }

    for (String header : record.headers()) {
      if (!header.toLowerCase(Locale.ROOT).startsWith("tag|")) {
        continue;
      }
      String tagName = header.substring("Tag|".length()).trim();
      if (isBlank(tagName)) {
        continue;
      }

      boolean enabled = toBoolean(record.value(header));
      Integer tagId =
          enabled
              ? ensureLocationTag(tagName)
              : queryInteger("select location_tag_id from location_tag where name = ?", tagName);
      if (tagId == null) {
        continue;
      }

      if (enabled) {
        if (countRows(
                "select count(*) from location_tag_map where location_id = ? and location_tag_id = ?",
                locationId,
                tagId)
            == 0) {
          jdbcTemplate.update(
              "insert into location_tag_map (location_id, location_tag_id) values (?, ?)",
              locationId,
              tagId);
        }
      } else {
        jdbcTemplate.update(
            "delete from location_tag_map where location_id = ? and location_tag_id = ?",
            locationId,
            tagId);
      }
    }
  }

  private void upsertPersonAttributeType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "person-attribute-type", name);
    Integer id =
        findIdByUuidOrName("person_attribute_type", "person_attribute_type_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    Integer foreignKey = resolvePersonAttributeForeignKey(record.value("Foreign"));
    String editPrivilege = record.value("Edit privilege");
    ensurePrivilege(editPrivilege);

    if (id == null) {
      jdbcTemplate.update(
          "insert into person_attribute_type "
              + "(name, description, format, foreign_key, searchable, creator, date_created, retired, retired_by, "
              + "date_retired, retire_reason, edit_privilege, sort_weight, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          record.value("Description"),
          limit(record.value("Format"), 50),
          foreignKey,
          toBoolean(record.value("Searchable")),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          editPrivilege,
          toInteger(record.value("Sort weight")),
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update person_attribute_type set name = ?, description = ?, format = ?, foreign_key = ?, searchable = ?, "
            + "changed_by = ?, date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, "
            + "retire_reason = ?, edit_privilege = ?, sort_weight = ?, uuid = ? where person_attribute_type_id = ?",
        limit(name, 50),
        record.value("Description"),
        limit(record.value("Format"), 50),
        foreignKey,
        toBoolean(record.value("Searchable")),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        editPrivilege,
        toInteger(record.value("Sort weight")),
        uuid,
        id);
  }

  private void upsertOrderType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "order-type", name);
    Integer id = findOrderTypeId(uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    String javaClassName = firstNonBlank(record.value("Java class name"), "org.openmrs.Order");

    if (id == null) {
      jdbcTemplate.update(
          "insert into order_type "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, uuid, "
              + "java_class_name) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          record.value("Description"),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid,
          limit(javaClassName, 255));
      return;
    }

    jdbcTemplate.update(
        "update order_type set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ?, java_class_name = ? "
            + "where order_type_id = ?",
        limit(name, 255),
        record.value("Description"),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        limit(javaClassName, 255),
        id);
  }

  private void applyOrderTypeParent(CsvRecord record) {
    String parent = record.value("Parent");
    if (isBlank(parent)) {
      return;
    }

    Integer orderTypeId =
        findOrderTypeId(
            valueOrStableUuid(record.value("Uuid"), "order-type", record.value("Name")),
            record.value("Name"));
    Integer parentId = findOrderTypeId(parent, parent);
    if (parentId == null) {
      throw new IllegalStateException("Order type parent '" + parent + "' was not loaded.");
    }

    jdbcTemplate.update(
        "update order_type set parent = ?, changed_by = ?, date_changed = ? where order_type_id = ?",
        parentId,
        SYSTEM_USER_ID,
        now(),
        orderTypeId);
  }

  private void applyOrderTypeConceptClasses(CsvRecord record) {
    if (!tableExists("order_type_class_map") || isBlank(record.value("Concept classes"))) {
      return;
    }

    Integer orderTypeId =
        findOrderTypeId(
            valueOrStableUuid(record.value("Uuid"), "order-type", record.value("Name")),
            record.value("Name"));
    if (orderTypeId == null) {
      return;
    }

    jdbcTemplate.update("delete from order_type_class_map where order_type_id = ?", orderTypeId);
    for (String conceptClass : splitList(record.value("Concept classes"))) {
      Integer conceptClassId = findConceptClassId(conceptClass);
      if (conceptClassId == null) {
        throw new IllegalStateException(
            "Order type concept class '" + conceptClass + "' was not loaded.");
      }
      if (countRows(
              "select count(*) from order_type_class_map where order_type_id = ? and concept_class_id = ?",
              orderTypeId,
              conceptClassId)
          == 0) {
        jdbcTemplate.update(
            "insert into order_type_class_map (order_type_id, concept_class_id) values (?, ?)",
            orderTypeId,
            conceptClassId);
      }
    }
  }

  private void upsertBillableService(CsvRecord record) {
    String name = record.value("service name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "billable-service", name);
    Integer id =
        queryInteger("select service_id from cashier_billable_service where uuid = ?", uuid);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    String retiredColumn = retiredColumn("cashier_billable_service");
    String retiredByColumn = retiredColumn.equals("retired") ? "retired_by" : "voided_by";
    String dateRetiredColumn = retiredColumn.equals("retired") ? "date_retired" : "date_voided";
    String retireReasonColumn = retiredColumn.equals("retired") ? "retire_reason" : "void_reason";
    Integer conceptId = findConceptId(record.value("Concept"));
    Integer serviceTypeId = findConceptId(record.value("Service Type"));
    Integer serviceCategoryId = findConceptId(record.value("Service Category"));
    String shortName = firstNonBlank(record.value("short name"), record.value("Description"));
    String status =
        firstNonBlank(record.value("Service Status"), "ENABLED").toUpperCase(Locale.ROOT);

    if (id == null) {
      jdbcTemplate.update(
          "insert into cashier_billable_service "
              + "(name, short_name, concept_id, service_type, service_category, service_status, creator, "
              + "date_created, "
              + retiredColumn
              + ", "
              + retiredByColumn
              + ", "
              + dateRetiredColumn
              + ", "
              + retireReasonColumn
              + ", uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(shortName, 255),
          conceptId,
          serviceTypeId,
          serviceCategoryId,
          limit(status, 255),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update cashier_billable_service set name = ?, short_name = ?, concept_id = ?, service_type = ?, "
            + "service_category = ?, service_status = ?, changed_by = ?, date_changed = ?, "
            + retiredColumn
            + " = ?, "
            + retiredByColumn
            + " = ?, "
            + dateRetiredColumn
            + " = ?, "
            + retireReasonColumn
            + " = ? where service_id = ?",
        limit(name, 255),
        limit(shortName, 255),
        conceptId,
        serviceTypeId,
        serviceCategoryId,
        limit(status, 255),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        id);
  }

  private Integer findLocationId(String uuid, String name) {
    Integer id = null;
    if (!isBlank(uuid)) {
      id = queryInteger("select location_id from location where uuid = ?", uuid);
    }
    if (id == null && !isBlank(name)) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select location_id from location where name = ?", Integer.class, name);
      id = ids.isEmpty() ? null : ids.get(0);
    }
    return id;
  }

  private Integer findRelationshipTypeId(String uuid, String aIsToB, String bIsToA) {
    Integer id = null;
    if (!isBlank(uuid)) {
      id = queryInteger("select relationship_type_id from relationship_type where uuid = ?", uuid);
    }
    if (id == null && !isBlank(aIsToB) && !isBlank(bIsToA)) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select relationship_type_id from relationship_type where a_is_to_b = ? and b_is_to_a = ?",
              Integer.class,
              aIsToB,
              bIsToA);
      id = ids.isEmpty() ? null : ids.get(0);
    }
    return id;
  }

  private Integer findConceptId(String identifier) {
    if (isBlank(identifier) || !tableExists("concept")) {
      return null;
    }

    Integer id = queryInteger("select concept_id from concept where uuid = ?", identifier);
    if (id != null || !tableExists("concept_name")) {
      return id;
    }

    List<Integer> ids =
        jdbcTemplate.queryForList(
            "select concept_id from concept_name where name = ? and voided = false",
            Integer.class,
            identifier);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private Integer resolvePersonAttributeForeignKey(String value) {
    if (isBlank(value)) {
      return null;
    }
    Integer conceptId = findConceptId(value);
    if (conceptId != null) {
      return conceptId;
    }
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      log.debug("Person attribute foreign key '{}' was not resolved to a loaded concept.", value);
      return null;
    }
  }

  private AttributeTypeTable attributeTypeTable(String entityName) {
    if (isBlank(entityName)) {
      return null;
    }
    return switch (entityName.trim().toLowerCase(Locale.ROOT)) {
      case "provider" ->
          new AttributeTypeTable("provider_attribute_type", "provider_attribute_type_id");
      case "visit" -> new AttributeTypeTable("visit_attribute_type", "visit_attribute_type_id");
      default -> null;
    };
  }

  private Integer findConceptClassId(String identifier) {
    return findIdByUuidOrName("concept_class", "concept_class_id", identifier, identifier);
  }

  private Integer findOrderTypeId(String uuid, String name) {
    return findIdByUuidOrName("order_type", "order_type_id", uuid, name);
  }

  private Integer findIdByUuidOrName(String table, String idColumn, String uuid, String name) {
    Integer id = null;
    if (!isBlank(uuid) && columnExists(table, "uuid")) {
      id = queryInteger("select " + idColumn + " from " + table + " where uuid = ?", uuid);
    }
    if (id == null && !isBlank(name) && columnExists(table, "name")) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select " + idColumn + " from " + table + " where name = ?", Integer.class, name);
      id = ids.isEmpty() ? null : ids.get(0);
    }
    return id;
  }

  private List<CsvRecord> readDomain(
      Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException, CsvException {
    List<CsvRecord> records = new ArrayList<>();
    for (Path csvFile : csvFiles(configRoot, domain, wildcardExclusions)) {
      records.addAll(readCsv(csvFile));
    }
    return records;
  }

  private List<Path> csvFiles(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException {
    return domainFiles(configRoot, domain, ".csv", wildcardExclusions);
  }

  private List<Path> xmlFiles(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException {
    return domainFiles(configRoot, domain, ".xml", wildcardExclusions);
  }

  private List<Path> domainFiles(
      Path configRoot, String domain, String extension, List<String> wildcardExclusions)
      throws IOException {
    Path directory = configRoot.resolve(domain).normalize();
    if (!directory.startsWith(configRoot) || !Files.isDirectory(directory)) {
      return List.of();
    }

    try (Stream<Path> stream = Files.list(directory)) {
      return stream
          .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
          .filter(
              path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
          .filter(path -> !excludedByWildcard(path, directory, wildcardExclusions))
          .sorted()
          .toList();
    }
  }

  private boolean excludedByWildcard(Path path, Path directory, List<String> wildcardExclusions) {
    if (wildcardExclusions == null || wildcardExclusions.isEmpty()) {
      return false;
    }

    Path fileName = path.getFileName();
    Path relativePath = directory.relativize(path);
    for (String wildcardExclusion : wildcardExclusions) {
      if (isBlank(wildcardExclusion)) {
        continue;
      }
      try {
        PathMatcher matcher =
            FileSystems.getDefault().getPathMatcher("glob:" + wildcardExclusion.trim());
        if (matcher.matches(fileName) || matcher.matches(relativePath)) {
          return true;
        }
      } catch (PatternSyntaxException e) {
        log.warn("Ignoring invalid initializer wildcard exclusion '{}'.", wildcardExclusion);
      }
    }
    return false;
  }

  private List<CsvRecord> readCsv(Path csvFile) throws IOException, CsvException {
    try (CSVReader reader =
        new CSVReader(
            new InputStreamReader(Files.newInputStream(csvFile), StandardCharsets.UTF_8))) {
      String[] headers = reader.readNext();
      if (headers == null) {
        return List.of();
      }

      List<CsvRecord> records = new ArrayList<>();
      for (String[] row : reader.readAll()) {
        if (!allBlank(row)) {
          records.add(new CsvRecord(csvFile, headers, row));
        }
      }
      return records;
    }
  }

  private List<GlobalPropertyRecord> readGlobalProperties(Path xmlFile)
      throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    setXmlAttributeIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
    setXmlAttributeIfSupported(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setExpandEntityReferences(false);
    factory.setXIncludeAware(false);

    Document document;
    try (InputStream inputStream = Files.newInputStream(xmlFile)) {
      document = factory.newDocumentBuilder().parse(inputStream);
    }

    List<GlobalPropertyRecord> records = new ArrayList<>();
    NodeList globalProperties = document.getElementsByTagName("globalProperty");
    for (int index = 0; index < globalProperties.getLength(); index++) {
      Element element = (Element) globalProperties.item(index);
      records.add(
          new GlobalPropertyRecord(
              elementText(element, "property"),
              elementText(element, "value"),
              elementText(element, "description")));
    }
    return records;
  }

  private String elementText(Element element, String tagName) {
    NodeList children = element.getElementsByTagName(tagName);
    if (children.getLength() == 0) {
      return null;
    }
    String text = children.item(0).getTextContent();
    return isBlank(text) ? null : text.trim();
  }

  private void setXmlAttributeIfSupported(
      DocumentBuilderFactory factory, String attributeName, String value) {
    try {
      factory.setAttribute(attributeName, value);
    } catch (IllegalArgumentException e) {
      log.debug("XML parser does not support secure attribute {}.", attributeName);
    }
  }

  private String retiredColumn(String table) {
    if (columnExists(table, "retired")) {
      return "retired";
    }
    if (columnExists(table, "voided")) {
      return "voided";
    }
    throw new IllegalStateException("No retired/voided column found on " + table + ".");
  }

  private boolean tableExists(String table) {
    return tableCache.computeIfAbsent(
        table.toLowerCase(Locale.ROOT), ignored -> !columns(table).isEmpty());
  }

  private boolean columnExists(String table, String column) {
    return columns(table).contains(column.toLowerCase(Locale.ROOT));
  }

  private Set<String> columns(String table) {
    return columnCache.computeIfAbsent(
        table.toLowerCase(Locale.ROOT),
        ignored ->
            jdbcTemplate.execute(
                (ConnectionCallback<Set<String>>)
                    connection -> {
                      DatabaseMetaData metaData = connection.getMetaData();
                      Set<String> columns = new LinkedHashSet<>();
                      Set<String> tableNames = new LinkedHashSet<>();
                      tableNames.add(table);
                      tableNames.add(table.toUpperCase(Locale.ROOT));
                      tableNames.add(table.toLowerCase(Locale.ROOT));
                      for (String tableName : tableNames) {
                        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                          while (rs.next()) {
                            columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                          }
                        }
                      }
                      return columns;
                    }));
  }

  private Integer queryInteger(String sql, Object... args) {
    List<Integer> values = jdbcTemplate.queryForList(sql, Integer.class, args);
    return values.isEmpty() ? null : values.get(0);
  }

  private int countRows(String sql, Object... args) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  private Timestamp now() {
    return Timestamp.from(Instant.now());
  }

  private String valueOrStableUuid(String value, String namespace, String key) {
    return isBlank(value) ? stableUuid(namespace, key) : value.trim();
  }

  private String stableUuid(String namespace, String value) {
    return UUID.nameUUIDFromBytes((namespace + ":" + value).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private List<String> splitList(String value) {
    if (isBlank(value)) {
      return List.of();
    }

    List<String> values = new ArrayList<>();
    for (String item : value.split(";")) {
      if (!isBlank(item)) {
        values.add(item.trim());
      }
    }
    return values;
  }

  private boolean toBoolean(String value) {
    if (isBlank(value)) {
      return false;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("true")
        || normalized.equals("yes")
        || normalized.equals("y")
        || normalized.equals("1");
  }

  private Integer toInteger(String value) {
    if (isBlank(value)) {
      return null;
    }
    return Integer.valueOf(value.trim());
  }

  private int toInteger(String value, int defaultValue) {
    Integer parsed = toInteger(value);
    return parsed == null ? defaultValue : parsed;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private String limit(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private boolean allBlank(String[] row) {
    for (String value : row) {
      if (!isBlank(value)) {
        return false;
      }
    }
    return true;
  }

  private static final class CsvRecord {

    private final Path source;

    private final Map<String, Integer> headerIndexes;

    private final Map<String, String> normalizedHeaders;

    private final String[] row;

    private CsvRecord(Path source, String[] headers, String[] row) {
      this.source = source;
      this.row = row;
      this.headerIndexes = new LinkedHashMap<>();
      this.normalizedHeaders = new LinkedHashMap<>();
      for (int index = 0; index < headers.length; index++) {
        String normalized = normalizeHeader(headers[index]);
        String key = normalized.toLowerCase(Locale.ROOT);
        headerIndexes.put(key, index);
        normalizedHeaders.put(key, normalized);
      }
    }

    private String value(String header) {
      Integer index = headerIndexes.get(normalizeHeader(header).toLowerCase(Locale.ROOT));
      if (index == null || index >= row.length) {
        return null;
      }
      String value = row[index];
      return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Set<String> headers() {
      return new LinkedHashSet<>(normalizedHeaders.values());
    }

    @Override
    public String toString() {
      return source + ":" + Objects.toString(value("Uuid"), "<no uuid>");
    }

    private static String normalizeHeader(String header) {
      if (header == null) {
        return "";
      }
      return header.replace("\uFEFF", "").trim();
    }
  }

  private record GlobalPropertyRecord(String property, String value, String description) {}

  private record AttributeTypeTable(String table, String idColumn) {}
}
