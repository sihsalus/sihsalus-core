package org.sihsalus.initializer;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Time;
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
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class StaticSihsalusContentLoader {

  private static final Logger log = LoggerFactory.getLogger(StaticSihsalusContentLoader.class);

  private static final int SYSTEM_USER_ID = 1;

  private static final String AMPATH_FORMS_UUID = "794c4598-ab82-47ca-8d18-483a8abe6f4f";

  private static final String JSON_SCHEMA_RESOURCE_NAME = "JSON schema";

  private static final String AMPATH_JSON_SCHEMA_DATATYPE = "AmpathJsonSchema";

  private static final String LONG_FREE_TEXT_DATATYPE =
      "org.openmrs.customdatatype.datatype.LongFreeTextDatatype";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final JdbcTemplate jdbcTemplate;

  private final Map<String, Boolean> tableCache = new HashMap<>();

  private final Map<String, Set<String>> columnCache = new HashMap<>();

  private final Set<String> missingOptionalConceptWarnings = new LinkedHashSet<>();

  StaticSihsalusContentLoader(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  void load() {
    loadDomains(
        List.of(
            "conceptclasses",
            "conceptsources",
            "metadatasharing",
            "visittypes",
            "patientidentifiertypes",
            "relationshiptypes",
            "privileges",
            "encountertypes",
            "encounterroles",
            "locationtags",
            "roles",
            "globalproperties",
            "addresshierarchy",
            "attributetypes",
            "locations",
            "personattributetypes",
            "billableservices",
            "paymentmodes",
            "cashpoints",
            "ordertypes",
            "appointmentspecialities",
            "appointmentservicedefinitions",
            "cohorttypes",
            "cohortattributetypes",
            "fhirconceptsources",
            "fhirpatientidentifiersystems",
            "idgen",
            "autogenerationoptions",
            "metadatasets",
            "metadatatermmappings"));
  }

  public void loadPostConceptDomains() {
    loadDomains(
        List.of(
            "personattributetypes",
            "billableservices",
            "conceptsets",
            "orderfrequencies",
            "drugs",
            "programs",
            "programworkflows",
            "programworkflowstates",
            "queues",
            "datafiltermappings",
            "conceptreferencerange",
            "ampathforms",
            "ampathformstranslations"));
  }

  private void loadDomains(List<String> domainNames) {
    try {
      for (String domainName : domainNames) {
        loadDomain(domainName, List.of());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load static SIH Salus content package.", e);
    }
  }

  public void loadDomain(String domainName, List<String> wildcardExclusions) throws Exception {
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
      case "metadatasharing":
        loadMetadataSharing(configRoot, wildcardExclusions);
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
      case "addresshierarchy":
        loadAddressHierarchy(configRoot, wildcardExclusions);
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
      case "paymentmodes":
        loadPaymentModes(configRoot, wildcardExclusions);
        break;
      case "cashpoints":
        loadCashPoints(configRoot, wildcardExclusions);
        break;
      case "appointmentspecialities":
        loadAppointmentSpecialities(configRoot, wildcardExclusions);
        break;
      case "appointmentservicedefinitions":
        loadAppointmentServiceDefinitions(configRoot, wildcardExclusions);
        break;
      case "cohorttypes":
        loadCohortTypes(configRoot, wildcardExclusions);
        break;
      case "cohortattributetypes":
        loadCohortAttributeTypes(configRoot, wildcardExclusions);
        break;
      case "fhirconceptsources":
        loadFhirConceptSources(configRoot, wildcardExclusions);
        break;
      case "fhirpatientidentifiersystems":
        loadFhirPatientIdentifierSystems(configRoot, wildcardExclusions);
        break;
      case "idgen":
        loadIdentifierSources(configRoot, wildcardExclusions);
        break;
      case "autogenerationoptions":
        loadAutoGenerationOptions(configRoot, wildcardExclusions);
        break;
      case "conceptsets":
        loadConceptSets(configRoot, wildcardExclusions);
        break;
      case "orderfrequencies":
        loadOrderFrequencies(configRoot, wildcardExclusions);
        break;
      case "drugs":
        loadDrugs(configRoot, wildcardExclusions);
        break;
      case "programs":
        loadPrograms(configRoot, wildcardExclusions);
        break;
      case "programworkflows":
        loadProgramWorkflows(configRoot, wildcardExclusions);
        break;
      case "programworkflowstates":
        loadProgramWorkflowStates(configRoot, wildcardExclusions);
        break;
      case "queues":
        loadQueues(configRoot, wildcardExclusions);
        break;
      case "datafiltermappings":
        loadDataFilterMappings(configRoot, wildcardExclusions);
        break;
      case "conceptreferencerange":
        loadConceptReferenceRanges(configRoot, wildcardExclusions);
        break;
      case "ampathforms":
        loadAmpathForms(configRoot, wildcardExclusions);
        break;
      case "ampathformstranslations":
        loadAmpathFormTranslations(configRoot, wildcardExclusions);
        break;
      case "metadatasets":
        loadMetadataSets(configRoot, wildcardExclusions);
        break;
      case "metadatatermmappings":
        loadMetadataTermMappings(configRoot, wildcardExclusions);
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

  private void loadMetadataSharing(Path configRoot, List<String> wildcardExclusions)
      throws IOException, ParserConfigurationException, SAXException {
    if (!tableExists("metadatamapping_metadata_source")) {
      return;
    }

    for (Path zipFile : zipFiles(configRoot, "metadatasharing", wildcardExclusions)) {
      for (MetadataSourceRecord record : readMetadataSourcePackage(zipFile)) {
        if (!isBlank(record.name())) {
          upsertMetadataSource(record);
        }
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

  private void loadMetadataSets(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("metadatamapping_metadata_set")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "metadatasets", wildcardExclusions)) {
      if (!isBlank(record.value("Uuid"))) {
        upsertMetadataSet(record);
      }
    }
  }

  private void loadMetadataTermMappings(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("metadatamapping_metadata_term_mapping")
        || !tableExists("metadatamapping_metadata_source")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "metadatatermmappings", wildcardExclusions)) {
      if (!isBlank(record.value("Mapping source")) && !isBlank(record.value("Mapping code"))) {
        upsertMetadataTermMapping(record);
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
    Path configRoot = SihsalusContentPaths.resolveConfigRoot();
    if (configRoot == null) {
      log.info(
          "SIH Salus content configuration was not found under {}, skipping static content load.",
          SihsalusContentPaths.resolveSourceRoot());
      return null;
    }
    return configRoot;
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

  private void loadAddressHierarchy(Path configRoot, List<String> wildcardExclusions)
      throws IOException, ParserConfigurationException, SAXException {
    if (!tableExists("address_hierarchy_level")
        || !tableExists("address_hierarchy_entry")
        || !tableExists("global_property")) {
      return;
    }

    Path configFile = null;
    for (Path xmlFile : xmlFiles(configRoot, "addresshierarchy", wildcardExclusions)) {
      if ("addressconfiguration.xml".equals(xmlFile.getFileName().toString().toLowerCase(Locale.ROOT))) {
        configFile = xmlFile;
        break;
      }
    }
    if (configFile == null) {
      log.warn("Address hierarchy configuration file was not found; skipping address hierarchy load.");
      return;
    }

    AddressHierarchyConfig config = readAddressHierarchyConfiguration(configFile);
    upsertGlobalProperty(
        new GlobalPropertyRecord(
            "layout.address.format",
            buildAddressTemplateXml(config),
            "XML description of address formats"));

    List<Integer> levelIds = upsertAddressHierarchyLevels(config.components());
    loadAddressHierarchyEntries(configFile.getParent().resolve(config.filename()), config, levelIds);
  }

  private List<Integer> upsertAddressHierarchyLevels(List<AddressHierarchyComponent> components) {
    List<Integer> levelIds = new ArrayList<>();
    Integer parentId = null;
    int nextLevelId = nextManualId("address_hierarchy_level", "address_hierarchy_level_id");

    for (AddressHierarchyComponent component : components) {
      Integer id = findAddressHierarchyLevelId(component.field(), component.nameMapping());
      String uuid =
          valueOrStableUuid(null, "address-hierarchy-level", component.field() + ":" + component.nameMapping());
      if (id == null) {
        id = nextLevelId++;
        jdbcTemplate.update(
            "insert into address_hierarchy_level "
                + "(address_hierarchy_level_id, name, parent_level_id, address_field, uuid, required) "
                + "values (?, ?, ?, ?, ?, ?)",
            id,
            limit(component.nameMapping(), 160),
            parentId,
            component.field(),
            uuid,
            component.required());
      } else {
        jdbcTemplate.update(
            "update address_hierarchy_level set name = ?, parent_level_id = ?, address_field = ?, "
                + "uuid = ?, required = ? where address_hierarchy_level_id = ?",
            limit(component.nameMapping(), 160),
            parentId,
            component.field(),
            uuid,
            component.required(),
            id);
      }
      levelIds.add(id);
      parentId = id;
    }

    restartSequence("address_hierarchy_level_address_hierarchy_level_id_seq", nextLevelId);
    return levelIds;
  }

  private void loadAddressHierarchyEntries(
      Path csvPath, AddressHierarchyConfig config, List<Integer> levelIds) throws IOException {
    if (!Files.isRegularFile(csvPath, LinkOption.NOFOLLOW_LINKS)) {
      log.warn("Address hierarchy entries file {} was not found; skipping entries load.", csvPath);
      return;
    }

    Map<AddressHierarchyEntryKey, Integer> entryIds = loadExistingAddressHierarchyEntryIds();
    Set<AddressHierarchyEntryKey> touchedExistingEntries = new LinkedHashSet<>();
    int nextEntryId = nextManualId("address_hierarchy_entry", "address_hierarchy_entry_id");
    int inserted = 0;

    try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (isBlank(line)) {
          continue;
        }

        Integer parentId = null;
        String parentPath = "";
        String[] values = line.split(Pattern.quote(config.entryDelimiter()), -1);
        int length = Math.min(values.length, levelIds.size());
        for (int index = 0; index < length; index++) {
          AddressHierarchyEntryValue value =
              parseAddressHierarchyEntryValue(values[index], config.identifierDelimiter());
          if (isBlank(value.name())) {
            continue;
          }

          Integer levelId = levelIds.get(index);
          String name = limit(value.name(), 160);
          String userGeneratedId = limit(value.userGeneratedId(), 11);
          AddressHierarchyEntryKey key =
              new AddressHierarchyEntryKey(parentId, levelId, normalizeAddressHierarchyName(name));
          String pathKey = parentPath + "/" + index + ":" + normalizeAddressHierarchyName(name);
          String uuid = stableUuid("address-hierarchy-entry", pathKey);
          Integer id = entryIds.get(key);
          if (id == null) {
            id = nextEntryId++;
            jdbcTemplate.update(
                "insert into address_hierarchy_entry "
                    + "(address_hierarchy_entry_id, name, level_id, parent_id, user_generated_id, uuid) "
                    + "values (?, ?, ?, ?, ?, ?)",
                id,
                name,
                levelId,
                parentId,
                userGeneratedId,
                uuid);
            entryIds.put(key, id);
            inserted++;
          } else if (touchedExistingEntries.add(key)) {
            jdbcTemplate.update(
                "update address_hierarchy_entry set name = ?, level_id = ?, parent_id = ?, "
                    + "user_generated_id = ?, uuid = ? where address_hierarchy_entry_id = ?",
                name,
                levelId,
                parentId,
                userGeneratedId,
                uuid,
                id);
          }

          parentId = id;
          parentPath = pathKey;
        }
      }
    }

    restartSequence("address_hierarchy_entry_address_hierarchy_entry_id_seq", nextEntryId);
    log.info(
        "Loaded static address hierarchy entries from {}; inserted {} new entries.",
        csvPath.getFileName(),
        inserted);
  }

  private Map<AddressHierarchyEntryKey, Integer> loadExistingAddressHierarchyEntryIds() {
    Map<AddressHierarchyEntryKey, Integer> entries = new HashMap<>();
    jdbcTemplate.query(
        "select address_hierarchy_entry_id, name, parent_id, level_id from address_hierarchy_entry",
        rs -> {
          Number parentId = (Number) rs.getObject("parent_id");
          AddressHierarchyEntryKey key =
              new AddressHierarchyEntryKey(
                  parentId == null ? null : parentId.intValue(),
                  rs.getInt("level_id"),
                  normalizeAddressHierarchyName(rs.getString("name")));
          entries.putIfAbsent(key, rs.getInt("address_hierarchy_entry_id"));
        });
    return entries;
  }

  private AddressHierarchyConfig readAddressHierarchyConfiguration(Path xmlFile)
      throws IOException, ParserConfigurationException, SAXException {
    Document document;
    try (InputStream inputStream = Files.newInputStream(xmlFile)) {
      document = secureDocumentBuilderFactory().newDocumentBuilder().parse(inputStream);
    }

    List<AddressHierarchyComponent> components = new ArrayList<>();
    NodeList componentNodes = document.getElementsByTagName("addressComponent");
    for (int index = 0; index < componentNodes.getLength(); index++) {
      Element element = (Element) componentNodes.item(index);
      String field = requiredElementText(element, "field", xmlFile);
      components.add(
          new AddressHierarchyComponent(
              field.trim().toUpperCase(Locale.ROOT),
              addressFieldToken(field),
              requiredElementText(element, "nameMapping", xmlFile),
              toInteger(elementText(element, "sizeMapping"), 40),
              elementText(element, "elementDefault"),
              toBoolean(elementText(element, "requiredInHierarchy"))));
    }

    Element hierarchyFile =
        firstElement(document.getElementsByTagName("addressHierarchyFile"), xmlFile, "addressHierarchyFile");
    String filename = requiredElementText(hierarchyFile, "filename", xmlFile);
    String entryDelimiter = firstNonBlank(elementText(hierarchyFile, "entryDelimiter"), "|");
    String identifierDelimiter = firstNonBlank(elementText(hierarchyFile, "identifierDelimiter"), "^");

    List<String> lineByLineFormat = new ArrayList<>();
    NodeList formatNodes = document.getElementsByTagName("lineByLineFormat");
    if (formatNodes.getLength() > 0) {
      NodeList lines = ((Element) formatNodes.item(0)).getElementsByTagName("string");
      for (int index = 0; index < lines.getLength(); index++) {
        String line = lines.item(index).getTextContent();
        if (!isBlank(line)) {
          lineByLineFormat.add(line.trim());
        }
      }
    }

    return new AddressHierarchyConfig(
        components, filename.trim(), entryDelimiter, identifierDelimiter, lineByLineFormat);
  }

  private String buildAddressTemplateXml(AddressHierarchyConfig config) {
    StringBuilder xml = new StringBuilder();
    xml.append("<org.openmrs.layout.address.AddressTemplate>\n");
    appendProperties(xml, "nameMappings", config.components(), AddressHierarchyComponent::nameMapping);
    appendProperties(
        xml, "sizeMappings", config.components(), component -> Integer.toString(component.sizeMapping()));
    appendProperties(xml, "elementDefaults", config.components(), AddressHierarchyComponent::elementDefault);
    xml.append("  <lineByLineFormat>\n");
    for (String line : config.lineByLineFormat()) {
      xml.append("    <string>").append(xmlEscape(line)).append("</string>\n");
    }
    xml.append("  </lineByLineFormat>\n");
    xml.append("</org.openmrs.layout.address.AddressTemplate>");
    return xml.toString();
  }

  private void appendProperties(
      StringBuilder xml,
      String name,
      List<AddressHierarchyComponent> components,
      java.util.function.Function<AddressHierarchyComponent, String> valueFunction) {
    xml.append("  <").append(name).append(" class=\"properties\">\n");
    for (AddressHierarchyComponent component : components) {
      String value = valueFunction.apply(component);
      if (!isBlank(value)) {
        xml.append("    <property name=\"")
            .append(xmlEscape(component.token()))
            .append("\" value=\"")
            .append(xmlEscape(value))
            .append("\"/>\n");
      }
    }
    xml.append("  </").append(name).append(">\n");
  }

  private AddressHierarchyEntryValue parseAddressHierarchyEntryValue(
      String rawValue, String identifierDelimiter) {
    String value = rawValue == null ? null : rawValue.replace("\uFEFF", "").trim();
    if (isBlank(value)) {
      return new AddressHierarchyEntryValue(null, null);
    }
    if (isBlank(identifierDelimiter)) {
      return new AddressHierarchyEntryValue(value, null);
    }
    String[] parts = value.split(Pattern.quote(identifierDelimiter), 2);
    String id = parts.length > 1 ? parts[1].trim() : null;
    return new AddressHierarchyEntryValue(parts[0].trim(), id);
  }

  private Integer findAddressHierarchyLevelId(String field, String name) {
    Integer id =
        queryInteger(
            "select address_hierarchy_level_id from address_hierarchy_level where address_field = ?",
            field);
    if (id == null) {
      id =
          queryInteger(
              "select address_hierarchy_level_id from address_hierarchy_level where name = ?",
              name);
    }
    return id;
  }

  private String requiredElementText(Element element, String tagName, Path source) {
    String value = elementText(element, tagName);
    if (isBlank(value)) {
      throw new IllegalStateException(tagName + " is required in " + source + ".");
    }
    return value;
  }

  private Element firstElement(NodeList nodes, Path source, String name) {
    if (nodes.getLength() == 0) {
      throw new IllegalStateException(name + " is required in " + source + ".");
    }
    return (Element) nodes.item(0);
  }

  private String normalizeAddressHierarchyName(String name) {
    return isBlank(name) ? "" : name.trim().toLowerCase(Locale.ROOT);
  }

  private String addressFieldToken(String field) {
    return switch (field.trim().toUpperCase(Locale.ROOT)) {
      case "ADDRESS_1" -> "address1";
      case "ADDRESS_2" -> "address2";
      case "ADDRESS_3" -> "address3";
      case "NEIGHBORHOOD_CELL" -> "neighborhoodCell";
      case "ADDRESS_4" -> "address4";
      case "TOWNSHIP_DIVISION" -> "townshipDivision";
      case "ADDRESS_5" -> "address5";
      case "SUBREGION" -> "subregion";
      case "ADDRESS_6" -> "address6";
      case "ADDRESS_7" -> "address7";
      case "ADDRESS_8" -> "address8";
      case "ADDRESS_9" -> "address9";
      case "ADDRESS_10" -> "address10";
      case "ADDRESS_11" -> "address11";
      case "ADDRESS_12" -> "address12";
      case "ADDRESS_13" -> "address13";
      case "ADDRESS_14" -> "address14";
      case "ADDRESS_15" -> "address15";
      case "REGION" -> "region";
      case "CITY_VILLAGE" -> "cityVillage";
      case "COUNTY_DISTRICT" -> "countyDistrict";
      case "STATE_PROVINCE" -> "stateProvince";
      case "COUNTRY" -> "country";
      case "POSTAL_CODE" -> "postalCode";
      case "LONGITUDE" -> "longitude";
      case "LATITUDE" -> "latitude";
      default -> throw new IllegalStateException("Unsupported address hierarchy field " + field + ".");
    };
  }

  private String xmlEscape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
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

  private void loadPaymentModes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("cashier_payment_mode")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "paymentmodes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertPaymentMode(record);
      }
    }
  }

  private void loadCashPoints(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("cashier_cash_point")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "cashpoints", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertCashPoint(record);
      }
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

  private void loadAppointmentSpecialities(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("appointment_speciality")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "appointmentspecialities", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertAppointmentSpeciality(record);
      }
    }
  }

  private void loadAppointmentServiceDefinitions(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("appointment_service")) {
      return;
    }

    for (CsvRecord record :
        readDomain(configRoot, "appointmentservicedefinitions", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertAppointmentServiceDefinition(record);
      }
    }
  }

  private void loadCohortTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("cohort_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "cohorttypes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertCohortType(record);
      }
    }
  }

  private void loadCohortAttributeTypes(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("cohort_attribute_type")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "cohortattributetypes", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertCohortAttributeType(record);
      }
    }
  }

  private void loadFhirConceptSources(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("fhir_concept_source")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "fhirconceptsources", wildcardExclusions)) {
      if (!isBlank(record.value("Concept source"))) {
        upsertFhirConceptSource(record);
      }
    }
  }

  private void loadFhirPatientIdentifierSystems(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("fhir_patient_identifier_system")) {
      return;
    }

    for (CsvRecord record :
        readDomain(configRoot, "fhirpatientidentifiersystems", wildcardExclusions)) {
      if (!isBlank(record.value("Patient identifier type"))) {
        upsertFhirPatientIdentifierSystem(record);
      }
    }
  }

  private void loadIdentifierSources(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("idgen_identifier_source") || !tableExists("idgen_seq_id_gen")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "idgen", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertSequentialIdentifierSource(record);
      }
    }
  }

  private void loadAutoGenerationOptions(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("idgen_auto_generation_option")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "autogenerationoptions", wildcardExclusions)) {
      if (!isBlank(record.value("Identifier Type"))) {
        upsertAutoGenerationOption(record);
      }
    }
  }

  private void loadConceptSets(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("concept_set")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "conceptsets", wildcardExclusions)) {
      if (!isBlank(record.value("Concept")) && !isBlank(record.value("Member"))) {
        upsertConceptSetMember(record);
      }
    }
  }

  private void loadOrderFrequencies(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("order_frequency")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "orderfrequencies", wildcardExclusions)) {
      if (!isBlank(record.value("Concept frequency"))) {
        upsertOrderFrequency(record);
      }
    }
  }

  private void loadDrugs(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("drug")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "drugs", wildcardExclusions)) {
      if (!isBlank(record.value("Name")) && !isBlank(record.value("Concept Drug"))) {
        upsertDrug(record);
      }
    }
  }

  private void loadPrograms(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("program")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "programs", wildcardExclusions)) {
      if (!isBlank(record.value("Name")) && !isBlank(record.value("Program concept"))) {
        upsertProgram(record);
      }
    }
  }

  private void loadProgramWorkflows(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("program_workflow")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "programworkflows", wildcardExclusions)) {
      if (!isBlank(record.value("Program")) && !isBlank(record.value("Workflow concept"))) {
        upsertProgramWorkflow(record);
      }
    }
  }

  private void loadProgramWorkflowStates(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("program_workflow_state")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "programworkflowstates", wildcardExclusions)) {
      if (!isBlank(record.value("Workflow")) && !isBlank(record.value("State concept"))) {
        upsertProgramWorkflowState(record);
      }
    }
  }

  private void loadQueues(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("queue")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "queues", wildcardExclusions)) {
      if (!isBlank(record.value("Name"))) {
        upsertQueue(record);
      }
    }
  }

  private void loadConceptReferenceRanges(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("concept_reference_range")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "conceptreferencerange", wildcardExclusions)) {
      if (!isBlank(record.value("Concept Numeric uuid")) && !isBlank(record.value("Criteria"))) {
        upsertConceptReferenceRange(record);
      }
    }
  }

  private void loadDataFilterMappings(Path configRoot, List<String> wildcardExclusions)
      throws IOException, CsvException {
    if (!tableExists("datafilter_entity_basis_map")) {
      return;
    }

    for (CsvRecord record : readDomain(configRoot, "datafiltermappings", wildcardExclusions)) {
      DataFilterObject entity =
          resolveDataFilterObject(
              requiredValue(record, "Entity UUID"), requiredValue(record, "Entity class"), record);
      DataFilterObject basis =
          resolveDataFilterObject(
              requiredValue(record, "Basis UUID"), requiredValue(record, "Basis class"), record);

      if (toBoolean(record.value("Void/Retire"))) {
        revokeDataFilterAccess(entity, basis);
      } else {
        grantDataFilterAccess(entity, basis);
      }
    }
  }

  private DataFilterObject resolveDataFilterObject(String uuid, String className, CsvRecord record) {
    DataFilterObjectType type = dataFilterObjectType(className);
    if (type == null) {
      throw new IllegalStateException(
          "Unsupported datafilter mapping class '" + className + "' in " + record.source + ".");
    }
    if (!tableExists(type.table())) {
      throw new IllegalStateException(
          "Datafilter mapping table '" + type.table() + "' is not available for " + record.source + ".");
    }

    String identifier;
    if (type.identifierColumn() != null) {
      identifier =
          queryString(
              "select "
                  + type.identifierColumn()
                  + " from "
                  + type.table()
                  + " where uuid = ?",
              uuid);
    } else {
      Integer id =
          queryInteger(
              "select " + type.idColumn() + " from " + type.table() + " where uuid = ?", uuid);
      identifier = id == null ? null : id.toString();
    }
    if (isBlank(identifier)) {
      throw new IllegalStateException(
          "Datafilter mapping object "
              + className
              + "/"
              + uuid
              + " was not loaded before "
              + record.source
              + ".");
    }
    return new DataFilterObject(identifier, type.className());
  }

  private DataFilterObjectType dataFilterObjectType(String className) {
    if (isBlank(className)) {
      return null;
    }
    return switch (className.trim()) {
      case "org.openmrs.Role" ->
          new DataFilterObjectType("org.openmrs.Role", "role", null, "role");
      case "org.openmrs.Privilege" ->
          new DataFilterObjectType("org.openmrs.Privilege", "privilege", null, "privilege");
      case "org.openmrs.Location" ->
          new DataFilterObjectType("org.openmrs.Location", "location", "location_id", null);
      case "org.openmrs.Program" ->
          new DataFilterObjectType("org.openmrs.Program", "program", "program_id", null);
      case "org.openmrs.User" ->
          new DataFilterObjectType("org.openmrs.User", "users", "user_id", null);
      default -> null;
    };
  }

  private void grantDataFilterAccess(DataFilterObject entity, DataFilterObject basis) {
    if (hasDataFilterAccess(entity, basis)) {
      return;
    }

    jdbcTemplate.update(
        "insert into datafilter_entity_basis_map "
            + "(entity_identifier, entity_type, basis_identifier, basis_type, creator, date_created, uuid) "
            + "values (?, ?, ?, ?, ?, ?, ?)",
        entity.identifier(),
        entity.className(),
        basis.identifier(),
        basis.className(),
        SYSTEM_USER_ID,
        now(),
        stableUuid(
            "datafilter-entity-basis-map",
            entity.className()
                + ":"
                + entity.identifier()
                + "->"
                + basis.className()
                + ":"
                + basis.identifier()));
  }

  private void revokeDataFilterAccess(DataFilterObject entity, DataFilterObject basis) {
    jdbcTemplate.update(
        "delete from datafilter_entity_basis_map where "
            + dataFilterAccessPredicate(),
        entity.identifier(),
        entity.className(),
        basis.identifier(),
        basis.className());
  }

  private boolean hasDataFilterAccess(DataFilterObject entity, DataFilterObject basis) {
    return countRows(
            "select count(*) from datafilter_entity_basis_map where " + dataFilterAccessPredicate(),
            entity.identifier(),
            entity.className(),
            basis.identifier(),
            basis.className())
        > 0;
  }

  private String dataFilterAccessPredicate() {
    return "lower(entity_identifier) = lower(?) and lower(entity_type) = lower(?) "
        + "and lower(basis_identifier) = lower(?) and lower(basis_type) = lower(?)";
  }

  private void loadAmpathForms(Path configRoot, List<String> wildcardExclusions)
      throws IOException {
    if (!tableExists("form")
        || !tableExists("form_resource")
        || !tableExists("clob_datatype_storage")) {
      return;
    }

    for (Path jsonFile : domainFiles(configRoot, "ampathforms", ".json", wildcardExclusions)) {
      upsertAmpathForm(jsonFile, Files.readString(jsonFile, StandardCharsets.UTF_8));
    }
  }

  private void loadAmpathFormTranslations(Path configRoot, List<String> wildcardExclusions)
      throws IOException {
    if (!tableExists("form")
        || !tableExists("form_resource")
        || !tableExists("clob_datatype_storage")) {
      return;
    }

    for (Path jsonFile :
        domainFiles(configRoot, "ampathformstranslations", ".json", wildcardExclusions)) {
      upsertAmpathFormTranslation(jsonFile, Files.readString(jsonFile, StandardCharsets.UTF_8));
    }
  }

  private void upsertAmpathForm(Path jsonFile, String jsonString) throws IOException {
    JsonNode formJson = OBJECT_MAPPER.readTree(jsonString);
    String formName = requiredJsonText(formJson, "name", jsonFile);
    String formVersion = requiredJsonText(formJson, "version", jsonFile);
    String formDescription = jsonText(formJson, "description");
    boolean formPublished = jsonBoolean(formJson, "published", false);
    boolean formRetired = jsonBoolean(formJson, "retired", false);
    String formProcessor = jsonText(formJson, "processor");
    boolean encounterForm =
        isBlank(formProcessor) || "EncounterFormProcessor".equalsIgnoreCase(formProcessor);
    Integer encounterTypeId =
        findEncounterTypeId(jsonText(formJson, "encounterType"), jsonText(formJson, "encounter"));

    if (encounterForm && encounterTypeId == null) {
      throw new IllegalStateException(
          "No encounter type was found for AMPATH form " + formName + " in " + jsonFile + ".");
    }

    String formUuid = uuidFromObjects(AMPATH_FORMS_UUID, formName, formVersion);
    Integer formId = queryInteger("select form_id from form where uuid = ?", formUuid);
    if (formId != null) {
      updateAmpathForm(
          formId,
          formName,
          formVersion,
          formDescription,
          formPublished,
          formRetired,
          encounterTypeId);
      upsertFormResource(
          formId,
          formUuid,
          JSON_SCHEMA_RESOURCE_NAME,
          AMPATH_JSON_SCHEMA_DATATYPE,
          jsonString);
      return;
    }

    retireActiveFormsWithName(formName, formUuid);
    formId =
        createAmpathForm(
            formUuid,
            formName,
            formVersion,
            formDescription,
            formPublished,
            formRetired,
            encounterTypeId);
    if (formId == null) {
      throw new IllegalStateException("Failed to create AMPATH form " + formName + ".");
    }
    upsertFormResource(
        formId, formUuid, JSON_SCHEMA_RESOURCE_NAME, AMPATH_JSON_SCHEMA_DATATYPE, jsonString);
  }

  private void upsertAmpathFormTranslation(Path jsonFile, String jsonString) throws IOException {
    JsonNode translationJson = OBJECT_MAPPER.readTree(jsonString);
    String formName = requiredJsonText(translationJson, "form", jsonFile);
    String language = requiredJsonText(translationJson, "language", jsonFile);
    Integer formId = findCurrentFormIdByName(formName);
    if (formId == null) {
      throw new IllegalStateException(
          "Could not find AMPATH form " + formName + " for translation " + jsonFile + ".");
    }

    String resourceName = formName + "_translations_" + language;
    String resourceUuid = stableUuid("ampath-form-translation", resourceName);
    upsertFormResource(formId, resourceUuid, resourceName, LONG_FREE_TEXT_DATATYPE, jsonString);
  }

  private void updateAmpathForm(
      Integer formId,
      String formName,
      String formVersion,
      String formDescription,
      boolean formPublished,
      boolean formRetired,
      Integer encounterTypeId) {
    String retireReason = formRetired ? "Retired by SIH Salus content package" : null;
    Timestamp retiredAt = formRetired ? now() : null;
    Integer retiredBy = formRetired ? SYSTEM_USER_ID : null;
    jdbcTemplate.update(
        "update form set name = ?, version = ?, description = ?, published = ?, retired = ?, "
            + "encounter_type = ?, changed_by = ?, date_changed = ?, retired_by = ?, "
            + "date_retired = ?, retired_reason = ? where form_id = ?",
        formName,
        formVersion,
        formDescription,
        formPublished,
        formRetired,
        encounterTypeId,
        SYSTEM_USER_ID,
        now(),
        retiredBy,
        retiredAt,
        retireReason,
        formId);
  }

  private Integer createAmpathForm(
      String formUuid,
      String formName,
      String formVersion,
      String formDescription,
      boolean formPublished,
      boolean formRetired,
      Integer encounterTypeId) {
    String retireReason = formRetired ? "Retired by SIH Salus content package" : null;
    Timestamp retiredAt = formRetired ? now() : null;
    Integer retiredBy = formRetired ? SYSTEM_USER_ID : null;
    jdbcTemplate.update(
        "insert into form "
            + "(name, version, build, published, description, encounter_type, creator, "
            + "date_created, retired, retired_by, date_retired, retired_reason, uuid) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        formName,
        formVersion,
        null,
        formPublished,
        formDescription,
        encounterTypeId,
        SYSTEM_USER_ID,
        now(),
        formRetired,
        retiredBy,
        retiredAt,
        retireReason,
        formUuid);
    return queryInteger("select form_id from form where uuid = ?", formUuid);
  }

  private void retireActiveFormsWithName(String formName, String replacementUuid) {
    Timestamp now = now();
    jdbcTemplate.update(
        "update form set retired = ?, retired_by = ?, date_retired = ?, retired_reason = ?, "
            + "changed_by = ?, date_changed = ? where name = ? and uuid <> ? and retired = ?",
        true,
        SYSTEM_USER_ID,
        now,
        "Replaced with new version by SIH Salus content package",
        SYSTEM_USER_ID,
        now,
        formName,
        replacementUuid,
        false);
  }

  private void upsertFormResource(
      Integer formId, String resourceUuidSeed, String resourceName, String datatype, String value) {
    Integer resourceId =
        queryInteger(
            "select form_resource_id from form_resource where form_id = ? and name = ?",
            formId,
            resourceName);
    String valueReference =
        resourceId == null
            ? null
            : queryString(
                "select value_reference from form_resource where form_resource_id = ?", resourceId);
    String clobUuid =
        isBlank(valueReference) ? stableUuid("form-resource-clob", resourceUuidSeed) : valueReference;
    upsertClob(clobUuid, value);

    if (resourceId == null) {
      jdbcTemplate.update(
          "insert into form_resource "
              + "(form_id, name, value_reference, datatype, datatype_config, preferred_handler, "
              + "handler_config, uuid, changed_by, date_changed) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          formId,
          resourceName,
          clobUuid,
          datatype,
          null,
          null,
          null,
          stableUuid("form-resource", resourceUuidSeed + ":" + resourceName),
          SYSTEM_USER_ID,
          now());
    } else {
      jdbcTemplate.update(
          "update form_resource set value_reference = ?, datatype = ?, changed_by = ?, "
              + "date_changed = ? where form_resource_id = ?",
          clobUuid,
          datatype,
          SYSTEM_USER_ID,
          now(),
          resourceId);
    }
  }

  private void upsertClob(String uuid, String value) {
    Integer clobId = queryInteger("select id from clob_datatype_storage where uuid = ?", uuid);
    if (clobId == null) {
      jdbcTemplate.update(
          "insert into clob_datatype_storage (uuid, value) values (?, ?)", uuid, value);
    } else {
      jdbcTemplate.update("update clob_datatype_storage set value = ? where id = ?", value, clobId);
    }
  }

  private Integer findCurrentFormIdByName(String formName) {
    List<Integer> formIds =
        jdbcTemplate.queryForList(
            "select form_id from form where name = ? and retired = ? order by form_id desc",
            Integer.class,
            formName,
            false);
    if (formIds.isEmpty()) {
      formIds =
          jdbcTemplate.queryForList(
              "select form_id from form where name = ? order by form_id desc",
              Integer.class,
              formName);
    }
    return formIds.isEmpty() ? null : formIds.get(0);
  }

  private Integer findEncounterTypeId(String uuid, String name) {
    Integer id = null;
    if (!isBlank(uuid)) {
      id =
          queryInteger(
              "select encounter_type_id from encounter_type where uuid = ? and retired = ?",
              uuid,
              false);
    }
    if (id == null && !isBlank(name)) {
      id =
          queryInteger(
              "select encounter_type_id from encounter_type where name = ? and retired = ?",
              name,
              false);
    }
    return id;
  }

  private String requiredJsonText(JsonNode node, String field, Path source) {
    String value = jsonText(node, field);
    if (isBlank(value)) {
      throw new IllegalStateException(field + " is required in " + source + ".");
    }
    return value;
  }

  private String jsonText(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return isBlank(text) ? null : text.trim();
  }

  private boolean jsonBoolean(JsonNode node, String field, boolean defaultValue) {
    JsonNode value = node == null ? null : node.get(field);
    if (value == null || value.isNull()) {
      return defaultValue;
    }
    if (value.isBoolean()) {
      return value.asBoolean();
    }
    return toBoolean(value.asText());
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

  private void upsertMetadataSource(MetadataSourceRecord record) {
    String uuid = valueOrStableUuid(record.uuid(), "metadata-source", record.name());
    Integer id =
        findIdByUuidOrName(
            "metadatamapping_metadata_source", "metadata_source_id", uuid, record.name());
    Timestamp now = now();
    Object retiredBy = record.retired() ? SYSTEM_USER_ID : null;
    Object retiredAt = record.retired() ? now : null;
    String retireReason = record.retired() ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into metadatamapping_metadata_source "
              + "(metadata_source_id, name, description, creator, date_created, retired, retired_by, "
              + "date_retired, retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          nextMetadataMappingId(
              "metadatamapping_metadata_source",
              "metadata_source_id",
              "metadatamapping_metadata_source_metadata_source_id_seq"),
          limit(record.name(), 255),
          limit(record.description(), 1024),
          SYSTEM_USER_ID,
          now,
          record.retired(),
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update metadatamapping_metadata_source set name = ?, description = ?, changed_by = ?, "
            + "date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? "
            + "where metadata_source_id = ?",
        limit(record.name(), 255),
        limit(record.description(), 1024),
        SYSTEM_USER_ID,
        now,
        record.retired(),
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
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

  private void upsertPaymentMode(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "payment-mode", name);
    Integer id = findIdByUuidOrName("cashier_payment_mode", "payment_mode_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into cashier_payment_mode "
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
      id = queryInteger("select payment_mode_id from cashier_payment_mode where uuid = ?", uuid);
    } else {
      jdbcTemplate.update(
          "update cashier_payment_mode set name = ?, description = ?, changed_by = ?, date_changed = ?, "
              + "retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? "
              + "where payment_mode_id = ?",
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

    if (columnExists("cashier_payment_mode", "sort_order")) {
      jdbcTemplate.update(
          "update cashier_payment_mode set sort_order = ? where payment_mode_id = ?",
          toInteger(record.value("Sort order")),
          id);
    }
    applyPaymentModeAttributes(id, uuid, record.value("Attributes"));
  }

  private void applyPaymentModeAttributes(
      Integer paymentModeId, String paymentModeUuid, String attributes) {
    if (paymentModeId == null
        || isBlank(attributes)
        || !tableExists("cashier_payment_mode_attribute_type")) {
      return;
    }

    List<String> activeAttributeUuids = new ArrayList<>();
    int attributeOrder = 0;
    for (String attribute : splitList(attributes)) {
      PaymentModeAttributeRecord attributeRecord =
          paymentModeAttributeRecord(paymentModeUuid, attribute, attributeOrder);
      activeAttributeUuids.add(attributeRecord.uuid());
      upsertPaymentModeAttribute(paymentModeId, attributeRecord);
      attributeOrder++;
    }

    if (!activeAttributeUuids.isEmpty()) {
      jdbcTemplate.update(
          "update cashier_payment_mode_attribute_type set retired = true, retired_by = ?, "
              + "date_retired = ?, retire_reason = ? where payment_mode_id = ? and uuid not in ("
              + placeholders(activeAttributeUuids.size())
              + ")",
          parameters(
              List.of(SYSTEM_USER_ID, now(), "Retired by SIH Salus content package", paymentModeId),
              activeAttributeUuids));
    }
  }

  private PaymentModeAttributeRecord paymentModeAttributeRecord(
      String paymentModeUuid, String attribute, int attributeOrder) {
    String[] parts = attribute.trim().split("::", -1);
    String name = parts.length > 0 ? blankToNull(parts[0]) : null;
    if (isBlank(name)) {
      throw new IllegalStateException("Payment mode attribute name is required.");
    }
    String format = parts.length > 1 ? blankToNull(parts[1]) : null;
    String regExp = parts.length > 2 ? blankToNull(parts[2]) : null;
    boolean required = parts.length > 3 && toBoolean(parts[3]);
    String uuid = stableUuid("payment-mode-attribute-type", paymentModeUuid + ":" + name);
    return new PaymentModeAttributeRecord(uuid, name, format, regExp, required, attributeOrder);
  }

  private void upsertPaymentModeAttribute(
      Integer paymentModeId, PaymentModeAttributeRecord attribute) {
    String foreignKeyColumn = paymentModeAttributeForeignKeyColumn();
    Integer id =
        queryInteger(
            "select payment_mode_attribute_type_id from cashier_payment_mode_attribute_type where uuid = ?",
            attribute.uuid());
    if (id == null) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select payment_mode_attribute_type_id from cashier_payment_mode_attribute_type "
                  + "where payment_mode_id = ? and name = ?",
              Integer.class,
              paymentModeId,
              attribute.name());
      id = ids.isEmpty() ? null : ids.get(0);
    }

    Timestamp now = now();
    if (id == null) {
      jdbcTemplate.update(
          "insert into cashier_payment_mode_attribute_type "
              + "(payment_mode_id, attribute_order, name, description, "
              + foreignKeyColumn
              + ", format, reg_exp, required, "
              + "creator, date_created, retired, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          paymentModeId,
          attribute.attributeOrder(),
          limit(attribute.name(), 255),
          null,
          null,
          limit(attribute.format(), 255),
          limit(attribute.regExp(), 255),
          attribute.required(),
          SYSTEM_USER_ID,
          now,
          false,
          attribute.uuid());
      return;
    }

    jdbcTemplate.update(
        "update cashier_payment_mode_attribute_type set payment_mode_id = ?, attribute_order = ?, name = ?, "
            + "description = ?, "
            + foreignKeyColumn
            + " = ?, format = ?, reg_exp = ?, required = ?, changed_by = ?, "
            + "date_changed = ?, retired = false, retired_by = null, date_retired = null, retire_reason = null, "
            + "uuid = ? where payment_mode_attribute_type_id = ?",
        paymentModeId,
        attribute.attributeOrder(),
        limit(attribute.name(), 255),
        null,
        null,
        limit(attribute.format(), 255),
        limit(attribute.regExp(), 255),
        attribute.required(),
        SYSTEM_USER_ID,
        now,
        attribute.uuid(),
        id);
  }

  private String paymentModeAttributeForeignKeyColumn() {
    if (!columnExists("cashier_payment_mode_attribute_type", "foreignKey")) {
      throw new IllegalStateException(
          "No foreignKey column found on cashier_payment_mode_attribute_type.");
    }
    return isPostgres() ? "\"foreignKey\"" : "foreignKey";
  }

  private void upsertCashPoint(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "cash-point", name);
    Integer id = findIdByUuidOrName("cashier_cash_point", "cash_point_id", uuid, name);
    Integer locationId = findLocationId(record.value("Location"), record.value("Location"));
    if (locationId == null) {
      throw new IllegalStateException(
          "Cash point location '" + record.value("Location") + "' was not loaded.");
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into cashier_cash_point "
              + "(name, description, creator, date_created, retired, retired_by, date_retired, retire_reason, "
              + "uuid, location_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 1024),
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid,
          locationId);
      return;
    }

    jdbcTemplate.update(
        "update cashier_cash_point set name = ?, description = ?, changed_by = ?, date_changed = ?, retired = ?, "
            + "retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ?, location_id = ? "
            + "where cash_point_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 1024),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        locationId,
        id);
  }

  private void upsertAppointmentSpeciality(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "appointment-speciality", name);
    Integer id = findIdByUuidOrName("appointment_speciality", "speciality_id", uuid, name);
    Timestamp now = now();
    boolean voided = toBoolean(record.value("Void/Retire"));

    if (id == null) {
      jdbcTemplate.update(
          "insert into appointment_speciality "
              + "(name, creator, date_created, changed_by, date_changed, uuid, voided) "
              + "values (?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          SYSTEM_USER_ID,
          now,
          null,
          null,
          uuid,
          voided);
      return;
    }

    jdbcTemplate.update(
        "update appointment_speciality set name = ?, changed_by = ?, date_changed = ?, uuid = ?, voided = ? "
            + "where speciality_id = ?",
        limit(name, 50),
        SYSTEM_USER_ID,
        now,
        uuid,
        voided,
        id);
  }

  private void upsertAppointmentServiceDefinition(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "appointment-service", name);
    Integer id = findIdByUuidOrName("appointment_service", "appointment_service_id", uuid, name);
    Integer specialityId = findSpecialityId(record.value("Speciality"));
    if (!isBlank(record.value("Speciality")) && specialityId == null) {
      throw new IllegalStateException(
          "Appointment speciality '" + record.value("Speciality") + "' was not loaded.");
    }

    Integer locationId = findLocationId(record.value("Location"), record.value("Location"));
    if (!isBlank(record.value("Location")) && locationId == null) {
      throw new IllegalStateException(
          "Appointment service location '" + record.value("Location") + "' was not loaded.");
    }

    Timestamp now = now();
    boolean voided = toBoolean(record.value("Void/Retire"));
    Object voidedBy = voided ? SYSTEM_USER_ID : null;
    Object voidedAt = voided ? now : null;
    String voidReason = voided ? "Voided by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into appointment_service "
              + "(name, description, start_time, end_time, location_id, speciality_id, max_appointments_limit, "
              + "duration_mins, date_created, creator, date_changed, changed_by, voided, voided_by, date_voided, "
              + "void_reason, uuid, color, initial_appointment_status) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 50),
          record.value("Description"),
          toSqlTime(record.value("Start Time")),
          toSqlTime(record.value("End Time")),
          locationId,
          specialityId,
          toInteger(record.value("Max Load")),
          toInteger(record.value("Duration")),
          now,
          SYSTEM_USER_ID,
          null,
          null,
          voided,
          voidedBy,
          voidedAt,
          voidReason,
          uuid,
          limit(record.value("Label Colour"), 8),
          null);
      return;
    }

    jdbcTemplate.update(
        "update appointment_service set name = ?, description = ?, start_time = ?, end_time = ?, "
            + "location_id = ?, speciality_id = ?, max_appointments_limit = ?, duration_mins = ?, "
            + "date_changed = ?, changed_by = ?, voided = ?, voided_by = ?, date_voided = ?, void_reason = ?, "
            + "uuid = ?, color = ? where appointment_service_id = ?",
        limit(name, 50),
        record.value("Description"),
        toSqlTime(record.value("Start Time")),
        toSqlTime(record.value("End Time")),
        locationId,
        specialityId,
        toInteger(record.value("Max Load")),
        toInteger(record.value("Duration")),
        now,
        SYSTEM_USER_ID,
        voided,
        voidedBy,
        voidedAt,
        voidReason,
        uuid,
        limit(record.value("Label Colour"), 8),
        id);
  }

  private void upsertCohortType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "cohort-type", name);
    Integer id = findIdByUuidOrName("cohort_type", "cohort_type_id", uuid, name);
    Timestamp now = now();
    boolean voided = toBoolean(record.value("Void/Retire"));
    Object voidedBy = voided ? SYSTEM_USER_ID : null;
    Object voidedAt = voided ? now : null;
    String voidReason = voided ? "Voided by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into cohort_type "
              + "(name, description, date_created, creator, changed_by, date_changed, voided, voided_by, "
              + "date_voided, void_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 255),
          now,
          SYSTEM_USER_ID,
          null,
          null,
          voided,
          voidedBy,
          voidedAt,
          voidReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update cohort_type set name = ?, description = ?, changed_by = ?, date_changed = ?, voided = ?, "
            + "voided_by = ?, date_voided = ?, void_reason = ?, uuid = ? where cohort_type_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 255),
        SYSTEM_USER_ID,
        now,
        voided,
        voidedBy,
        voidedAt,
        voidReason,
        uuid,
        id);
  }

  private void upsertCohortAttributeType(CsvRecord record) {
    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "cohort-attribute-type", name);
    Integer id =
        findIdByUuidOrName("cohort_attribute_type", "cohort_attribute_type_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into cohort_attribute_type "
              + "(name, description, datatype, datatype_config, preferred_handler, handler_config, min_occurs, "
              + "max_occurs, date_created, creator, changed_by, date_changed, retired, retired_by, date_retired, "
              + "retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          limit(name, 255),
          limit(record.value("Description"), 255),
          limit(record.value("Datatype classname"), 255),
          record.value("Datatype config"),
          limit(record.value("Preferred handler classname"), 255),
          record.value("Handler config"),
          toInteger(record.value("Min occurs"), 0),
          toInteger(record.value("Max occurs")),
          now,
          SYSTEM_USER_ID,
          null,
          null,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update cohort_attribute_type set name = ?, description = ?, datatype = ?, datatype_config = ?, "
            + "preferred_handler = ?, handler_config = ?, min_occurs = ?, max_occurs = ?, changed_by = ?, "
            + "date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? "
            + "where cohort_attribute_type_id = ?",
        limit(name, 255),
        limit(record.value("Description"), 255),
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

  private void upsertFhirConceptSource(CsvRecord record) {
    Integer conceptSourceId = findConceptSourceId(record.value("Concept source"));
    if (conceptSourceId == null) {
      throw new IllegalStateException(
          "FHIR concept source reference '" + record.value("Concept source") + "' was not loaded.");
    }

    String conceptSourceName =
        jdbcTemplate.queryForObject(
            "select name from concept_reference_source where concept_source_id = ?",
            String.class,
            conceptSourceId);
    String uuid = valueOrStableUuid(record.value("Uuid"), "fhir-concept-source", conceptSourceName);
    Integer id =
        queryInteger(
            "select fhir_concept_source_id from fhir_concept_source where concept_source_id = ?",
            conceptSourceId);
    if (id == null) {
      id =
          queryInteger(
              "select fhir_concept_source_id from fhir_concept_source where uuid = ?", uuid);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    String url = record.value("Url");
    if (isBlank(url) && (id == null || !retired)) {
      throw new IllegalStateException(
          "FHIR concept source URL is required for " + conceptSourceName + ".");
    }

    if (id == null) {
      jdbcTemplate.update(
          "insert into fhir_concept_source "
              + "(concept_source_id, url, name, description, creator, date_created, retired, retired_by, "
              + "date_retired, retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          conceptSourceId,
          limit(url, 255),
          limit(conceptSourceName, 255),
          null,
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
        "update fhir_concept_source set concept_source_id = ?, url = ?, name = ?, changed_by = ?, "
            + "date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? "
            + "where fhir_concept_source_id = ?",
        conceptSourceId,
        limit(url, 255),
        limit(conceptSourceName, 255),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private void upsertFhirPatientIdentifierSystem(CsvRecord record) {
    Integer identifierTypeId = findPatientIdentifierTypeId(record.value("Patient identifier type"));
    if (identifierTypeId == null) {
      throw new IllegalStateException(
          "FHIR patient identifier type reference '"
              + record.value("Patient identifier type")
              + "' was not loaded.");
    }

    String identifierTypeName =
        jdbcTemplate.queryForObject(
            "select name from patient_identifier_type where patient_identifier_type_id = ?",
            String.class,
            identifierTypeId);
    String uuid =
        valueOrStableUuid(
            record.value("Uuid"), "fhir-patient-identifier-system", identifierTypeName);
    Integer id =
        queryInteger(
            "select fhir_patient_identifier_system_id from fhir_patient_identifier_system "
                + "where patient_identifier_type_id = ?",
            identifierTypeId);
    if (id == null) {
      id =
          queryInteger(
              "select fhir_patient_identifier_system_id from fhir_patient_identifier_system where uuid = ?",
              uuid);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    String url = record.value("Url");
    if (isBlank(url) && (id == null || !retired)) {
      throw new IllegalStateException(
          "FHIR patient identifier system URL is required for " + identifierTypeName + ".");
    }

    if (id == null) {
      jdbcTemplate.update(
          "insert into fhir_patient_identifier_system "
              + "(patient_identifier_type_id, url, name, description, creator, date_created, retired, retired_by, "
              + "date_retired, retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          identifierTypeId,
          limit(url, 255),
          limit(identifierTypeName, 255),
          null,
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
        "update fhir_patient_identifier_system set patient_identifier_type_id = ?, url = ?, name = ?, "
            + "changed_by = ?, date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, "
            + "retire_reason = ?, uuid = ? where fhir_patient_identifier_system_id = ?",
        identifierTypeId,
        limit(url, 255),
        limit(identifierTypeName, 255),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private void upsertSequentialIdentifierSource(CsvRecord record) {
    Integer identifierTypeId = findPatientIdentifierTypeId(record.value("Identifier type"));
    if (identifierTypeId == null) {
      throw new IllegalStateException(
          "Identifier source type reference '"
              + record.value("Identifier type")
              + "' was not loaded.");
    }

    String name = record.value("Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "identifier-source", name);
    Integer id = queryInteger("select id from idgen_identifier_source where uuid = ?", uuid);
    if (id == null) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select id from idgen_identifier_source where name = ?", Integer.class, name);
      id = ids.isEmpty() ? null : ids.get(0);
    }

    if (id != null
        && ((tableExists("idgen_remote_source")
                && countRows("select count(*) from idgen_remote_source where id = ?", id) > 0)
            || (tableExists("idgen_id_pool")
                && countRows("select count(*) from idgen_id_pool where id = ?", id) > 0))) {
      throw new IllegalStateException(
          "Identifier source '" + name + "' already exists as a non-sequential source.");
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into idgen_identifier_source "
              + "(uuid, name, description, identifier_type, creator, date_created, retired, retired_by, "
              + "date_retired, retire_reason) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          uuid,
          limit(name, 255),
          limit(record.value("Description"), 1000),
          identifierTypeId,
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason);
      id = queryInteger("select id from idgen_identifier_source where uuid = ?", uuid);
    } else {
      jdbcTemplate.update(
          "update idgen_identifier_source set uuid = ?, name = ?, description = ?, identifier_type = ?, "
              + "changed_by = ?, date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, "
              + "retire_reason = ? where id = ?",
          uuid,
          limit(name, 255),
          limit(record.value("Description"), 1000),
          identifierTypeId,
          SYSTEM_USER_ID,
          now,
          retired,
          retiredBy,
          retiredAt,
          retireReason,
          id);
    }

    upsertSequentialIdentifierGenerator(id, record);
  }

  private void upsertSequentialIdentifierGenerator(Integer sourceId, CsvRecord record) {
    boolean hasMaxLength = columnExists("idgen_seq_id_gen", "max_length");
    boolean exists = countRows("select count(*) from idgen_seq_id_gen where id = ?", sourceId) > 0;

    if (!exists && hasMaxLength) {
      jdbcTemplate.update(
          "insert into idgen_seq_id_gen "
              + "(id, base_character_set, first_identifier_base, prefix, suffix, min_length, max_length) "
              + "values (?, ?, ?, ?, ?, ?, ?)",
          sourceId,
          requiredValue(record, "Base character set"),
          limit(requiredValue(record, "First identifier base"), 50),
          stringOrEmpty(record.value("Prefix")),
          stringOrEmpty(record.value("Suffix")),
          requiredInteger(record, "Min length"),
          requiredInteger(record, "Max length"));
      return;
    }

    if (!exists) {
      jdbcTemplate.update(
          "insert into idgen_seq_id_gen "
              + "(id, base_character_set, first_identifier_base, prefix, suffix, min_length) "
              + "values (?, ?, ?, ?, ?, ?)",
          sourceId,
          requiredValue(record, "Base character set"),
          limit(requiredValue(record, "First identifier base"), 50),
          stringOrEmpty(record.value("Prefix")),
          stringOrEmpty(record.value("Suffix")),
          requiredInteger(record, "Min length"));
      return;
    }

    if (hasMaxLength) {
      jdbcTemplate.update(
          "update idgen_seq_id_gen set base_character_set = ?, first_identifier_base = ?, prefix = ?, "
              + "suffix = ?, min_length = ?, max_length = ? where id = ?",
          requiredValue(record, "Base character set"),
          limit(requiredValue(record, "First identifier base"), 50),
          stringOrEmpty(record.value("Prefix")),
          stringOrEmpty(record.value("Suffix")),
          requiredInteger(record, "Min length"),
          requiredInteger(record, "Max length"),
          sourceId);
      return;
    }

    jdbcTemplate.update(
        "update idgen_seq_id_gen set base_character_set = ?, first_identifier_base = ?, prefix = ?, "
            + "suffix = ?, min_length = ? where id = ?",
        requiredValue(record, "Base character set"),
        limit(requiredValue(record, "First identifier base"), 50),
        stringOrEmpty(record.value("Prefix")),
        stringOrEmpty(record.value("Suffix")),
        requiredInteger(record, "Min length"),
        sourceId);
  }

  private void upsertAutoGenerationOption(CsvRecord record) {
    Integer identifierTypeId = findPatientIdentifierTypeId(record.value("Identifier Type"));
    if (identifierTypeId == null) {
      throw new IllegalStateException(
          "Auto-generation identifier type reference '"
              + record.value("Identifier Type")
              + "' was not loaded.");
    }

    Integer locationId = findLocationId(record.value("Location"), record.value("Location"));
    if (!isBlank(record.value("Location")) && locationId == null) {
      throw new IllegalStateException(
          "Auto-generation option location '" + record.value("Location") + "' was not loaded.");
    }

    Integer sourceId =
        queryInteger(
            "select id from idgen_identifier_source where uuid = ?",
            requiredValue(record, "Identifier Source"));
    if (sourceId == null) {
      throw new IllegalStateException(
          "Auto-generation identifier source '"
              + record.value("Identifier Source")
              + "' was not loaded.");
    }

    String uuid =
        valueOrStableUuid(
            record.value("Uuid"),
            "auto-generation-option",
            identifierTypeId + ":" + Objects.toString(locationId, ""));
    Integer id = queryInteger("select id from idgen_auto_generation_option where uuid = ?", uuid);
    if (id == null) {
      id = findAutoGenerationOptionId(identifierTypeId, locationId);
    }

    boolean hasLocation = columnExists("idgen_auto_generation_option", "location");
    if (id == null && hasLocation) {
      jdbcTemplate.update(
          "insert into idgen_auto_generation_option "
              + "(uuid, identifier_type, location, source, manual_entry_enabled, automatic_generation_enabled) "
              + "values (?, ?, ?, ?, ?, ?)",
          uuid,
          identifierTypeId,
          locationId,
          sourceId,
          toBoolean(record.value("Manual Entry Enabled")),
          toBoolean(record.value("Auto Generation Enabled")));
      return;
    }

    if (id == null) {
      jdbcTemplate.update(
          "insert into idgen_auto_generation_option "
              + "(uuid, identifier_type, source, manual_entry_enabled, automatic_generation_enabled) "
              + "values (?, ?, ?, ?, ?)",
          uuid,
          identifierTypeId,
          sourceId,
          toBoolean(record.value("Manual Entry Enabled")),
          toBoolean(record.value("Auto Generation Enabled")));
      return;
    }

    if (hasLocation) {
      jdbcTemplate.update(
          "update idgen_auto_generation_option set uuid = ?, identifier_type = ?, location = ?, source = ?, "
              + "manual_entry_enabled = ?, automatic_generation_enabled = ? where id = ?",
          uuid,
          identifierTypeId,
          locationId,
          sourceId,
          toBoolean(record.value("Manual Entry Enabled")),
          toBoolean(record.value("Auto Generation Enabled")),
          id);
      return;
    }

    jdbcTemplate.update(
        "update idgen_auto_generation_option set uuid = ?, identifier_type = ?, source = ?, "
            + "manual_entry_enabled = ?, automatic_generation_enabled = ? where id = ?",
        uuid,
        identifierTypeId,
        sourceId,
        toBoolean(record.value("Manual Entry Enabled")),
        toBoolean(record.value("Auto Generation Enabled")),
        id);
  }

  private Integer findAutoGenerationOptionId(Integer identifierTypeId, Integer locationId) {
    if (!columnExists("idgen_auto_generation_option", "location")) {
      return queryInteger(
          "select id from idgen_auto_generation_option where identifier_type = ?",
          identifierTypeId);
    }
    if (locationId == null) {
      return queryInteger(
          "select id from idgen_auto_generation_option where identifier_type = ? and location is null",
          identifierTypeId);
    }
    return queryInteger(
        "select id from idgen_auto_generation_option where identifier_type = ? and location = ?",
        identifierTypeId,
        locationId);
  }

  private void upsertConceptSetMember(CsvRecord record) {
    Integer setConceptId = requiredConceptId(record, "Concept");
    Integer memberConceptId = requiredConceptId(record, "Member");
    String uuid = stableUuid("concept-set", setConceptId + ":" + memberConceptId);
    Integer id = queryInteger("select concept_set_id from concept_set where uuid = ?", uuid);
    if (id == null) {
      id =
          queryInteger(
              "select concept_set_id from concept_set where concept_set = ? and concept_id = ?",
              setConceptId,
              memberConceptId);
    }

    if (id == null) {
      jdbcTemplate.update(
          "insert into concept_set (concept_id, concept_set, sort_weight, creator, date_created, uuid) "
              + "values (?, ?, ?, ?, ?, ?)",
          memberConceptId,
          setConceptId,
          toDouble(record.value("Sort Weight")),
          SYSTEM_USER_ID,
          now(),
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update concept_set set concept_id = ?, concept_set = ?, sort_weight = ?, uuid = ? "
            + "where concept_set_id = ?",
        memberConceptId,
        setConceptId,
        toDouble(record.value("Sort Weight")),
        uuid,
        id);
  }

  private void upsertOrderFrequency(CsvRecord record) {
    Integer conceptId = requiredConceptId(record, "Concept frequency");
    String uuid = valueOrStableUuid(record.value("Uuid"), "order-frequency", conceptId.toString());
    Integer id = queryInteger("select order_frequency_id from order_frequency where uuid = ?", uuid);
    if (id == null) {
      id =
          queryInteger(
              "select order_frequency_id from order_frequency where concept_id = ?", conceptId);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into order_frequency "
              + "(concept_id, frequency_per_day, creator, date_created, retired, retired_by, date_retired, "
              + "retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          conceptId,
          toDouble(record.value("Frequency per day")),
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
        "update order_frequency set concept_id = ?, frequency_per_day = ?, changed_by = ?, date_changed = ?, "
            + "retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? "
            + "where order_frequency_id = ?",
        conceptId,
        toDouble(record.value("Frequency per day")),
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
        id);
  }

  private void upsertDrug(CsvRecord record) {
    Integer conceptId = requiredConceptId(record, "Concept Drug");
    Integer dosageFormId = findConceptId(record.value("Concept Dosage Form"));
    if (!isBlank(record.value("Concept Dosage Form")) && dosageFormId == null) {
      warnMissingOptionalConcept("Drug dosage form", record.value("Concept Dosage Form"));
    }

    String name = requiredValue(record, "Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "drug", name);
    Integer id = queryInteger("select drug_id from drug where uuid = ?", uuid);
    if (id == null) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select drug_id from drug where name = ? and concept_id = ?",
              Integer.class,
              name,
              conceptId);
      id = ids.isEmpty() ? null : ids.get(0);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    boolean hasStrength = columnExists("drug", "strength");

    if (id == null) {
      List<String> columns =
          new ArrayList<>(
              List.of(
                  "concept_id",
                  "name",
                  "combination",
                  "dosage_form",
                  "creator",
                  "date_created",
                  "retired",
                  "retired_by",
                  "date_retired",
                  "retire_reason",
                  "uuid"));
      List<Object> values = new ArrayList<>();
      values.add(conceptId);
      values.add(limit(name, 255));
      values.add(false);
      values.add(dosageFormId);
      values.add(SYSTEM_USER_ID);
      values.add(now);
      values.add(retired);
      values.add(retiredBy);
      values.add(retiredAt);
      values.add(retireReason);
      values.add(uuid);
      if (hasStrength) {
        columns.add("strength");
        values.add(limit(record.value("Strength"), 255));
      }
      jdbcTemplate.update(
          "insert into drug ("
              + String.join(", ", columns)
              + ") values ("
              + placeholders(columns.size())
              + ")",
          values.toArray());
      return;
    }

    List<String> assignments =
        new ArrayList<>(
            List.of(
                "concept_id = ?",
                "name = ?",
                "combination = ?",
                "dosage_form = ?",
                "changed_by = ?",
                "date_changed = ?",
                "retired = ?",
                "retired_by = ?",
                "date_retired = ?",
                "retire_reason = ?",
                "uuid = ?"));
    List<Object> values = new ArrayList<>();
    values.add(conceptId);
    values.add(limit(name, 255));
    values.add(false);
    values.add(dosageFormId);
    values.add(SYSTEM_USER_ID);
    values.add(now);
    values.add(retired);
    values.add(retiredBy);
    values.add(retiredAt);
    values.add(retireReason);
    values.add(uuid);
    if (hasStrength) {
      assignments.add("strength = ?");
      values.add(limit(record.value("Strength"), 255));
    }
    values.add(id);
    jdbcTemplate.update(
        "update drug set " + String.join(", ", assignments) + " where drug_id = ?",
        values.toArray());
  }

  private void upsertProgram(CsvRecord record) {
    String name = requiredValue(record, "Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "program", name);
    Integer conceptId = requiredConceptId(record, "Program concept");
    Integer outcomesConceptId = findConceptId(record.value("Outcomes concept"));
    if (!isBlank(record.value("Outcomes concept")) && outcomesConceptId == null) {
      throw new IllegalStateException(
          "Program outcomes concept '" + record.value("Outcomes concept") + "' was not loaded.");
    }

    Integer id = findProgramId(uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));

    if (id == null) {
      jdbcTemplate.update(
          "insert into program "
              + "(concept_id, outcomes_concept_id, creator, date_created, retired, name, description, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?)",
          conceptId,
          outcomesConceptId,
          SYSTEM_USER_ID,
          now,
          retired,
          limit(name, 50),
          record.value("Description"),
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update program set concept_id = ?, outcomes_concept_id = ?, changed_by = ?, date_changed = ?, "
            + "retired = ?, name = ?, description = ?, uuid = ? where program_id = ?",
        conceptId,
        outcomesConceptId,
        SYSTEM_USER_ID,
        now,
        retired,
        limit(name, 50),
        record.value("Description"),
        uuid,
        id);
  }

  private void upsertProgramWorkflow(CsvRecord record) {
    Integer programId = findProgramId(record.value("Program"), record.value("Program"));
    if (programId == null) {
      throw new IllegalStateException(
          "Program '" + record.value("Program") + "' was not loaded.");
    }

    Integer conceptId = requiredConceptId(record, "Workflow concept");
    String uuid =
        valueOrStableUuid(
            record.value("Uuid"), "program-workflow", programId + ":" + conceptId);
    Integer id = findProgramWorkflowId(uuid);
    if (id == null) {
      id =
          queryInteger(
              "select program_workflow_id from program_workflow where program_id = ? and concept_id = ?",
              programId,
              conceptId);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    if (id == null) {
      jdbcTemplate.update(
          "insert into program_workflow "
              + "(program_id, concept_id, creator, date_created, retired, uuid) values (?, ?, ?, ?, ?, ?)",
          programId,
          conceptId,
          SYSTEM_USER_ID,
          now,
          retired,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update program_workflow set program_id = ?, concept_id = ?, changed_by = ?, date_changed = ?, "
            + "retired = ?, uuid = ? where program_workflow_id = ?",
        programId,
        conceptId,
        SYSTEM_USER_ID,
        now,
        retired,
        uuid,
        id);
  }

  private void upsertProgramWorkflowState(CsvRecord record) {
    Integer workflowId = findProgramWorkflowId(record.value("Workflow"));
    if (workflowId == null) {
      throw new IllegalStateException(
          "Program workflow '" + record.value("Workflow") + "' was not loaded.");
    }

    Integer conceptId = requiredConceptId(record, "State concept");
    String uuid =
        valueOrStableUuid(
            record.value("Uuid"), "program-workflow-state", workflowId + ":" + conceptId);
    Integer id =
        queryInteger(
            "select program_workflow_state_id from program_workflow_state where uuid = ?", uuid);
    if (id == null) {
      id =
          queryInteger(
              "select program_workflow_state_id from program_workflow_state "
                  + "where program_workflow_id = ? and concept_id = ?",
              workflowId,
              conceptId);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    if (id == null) {
      jdbcTemplate.update(
          "insert into program_workflow_state "
              + "(program_workflow_id, concept_id, initial, terminal, creator, date_created, retired, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?)",
          workflowId,
          conceptId,
          toBoolean(record.value("Initial")),
          toBoolean(record.value("Terminal")),
          SYSTEM_USER_ID,
          now,
          retired,
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update program_workflow_state set program_workflow_id = ?, concept_id = ?, initial = ?, "
            + "terminal = ?, changed_by = ?, date_changed = ?, retired = ?, uuid = ? "
            + "where program_workflow_state_id = ?",
        workflowId,
        conceptId,
        toBoolean(record.value("Initial")),
        toBoolean(record.value("Terminal")),
        SYSTEM_USER_ID,
        now,
        retired,
        uuid,
        id);
  }

  private void upsertQueue(CsvRecord record) {
    String name = requiredValue(record, "Name");
    String uuid = valueOrStableUuid(record.value("Uuid"), "queue", name);
    Integer locationId = findLocationId(record.value("Location"), record.value("Location"));
    if (locationId == null) {
      throw new IllegalStateException(
          "Queue location '" + record.value("Location") + "' was not loaded.");
    }

    Integer serviceConceptId = requiredConceptId(record, "Service");
    Integer statusConceptSetId = findConceptId(record.value("Status Concept Set"));
    if (!isBlank(record.value("Status Concept Set")) && statusConceptSetId == null) {
      throw new IllegalStateException(
          "Queue status concept set '" + record.value("Status Concept Set") + "' was not loaded.");
    }
    Integer priorityConceptSetId = findConceptId(record.value("Priority Concept Set"));
    if (!isBlank(record.value("Priority Concept Set")) && priorityConceptSetId == null) {
      throw new IllegalStateException(
          "Queue priority concept set '"
              + record.value("Priority Concept Set")
              + "' was not loaded.");
    }

    Integer id = findIdByUuidOrName("queue", "queue_id", uuid, name);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;
    boolean hasStatusConceptSet = columnExists("queue", "status_concept_set");
    boolean hasPriorityConceptSet = columnExists("queue", "priority_concept_set");

    if (id == null) {
      List<String> columns =
          new ArrayList<>(
              List.of(
                  "name",
                  "description",
                  "location_id",
                  "service",
                  "creator",
                  "date_created",
                  "retired",
                  "retired_by",
                  "date_retired",
                  "retire_reason",
                  "uuid"));
      List<Object> values = new ArrayList<>();
      values.add(limit(name, 255));
      values.add(limit(record.value("Description"), 255));
      values.add(locationId);
      values.add(serviceConceptId);
      values.add(SYSTEM_USER_ID);
      values.add(now);
      values.add(retired);
      values.add(retiredBy);
      values.add(retiredAt);
      values.add(retireReason);
      values.add(uuid);
      if (hasStatusConceptSet) {
        columns.add("status_concept_set");
        values.add(statusConceptSetId);
      }
      if (hasPriorityConceptSet) {
        columns.add("priority_concept_set");
        values.add(priorityConceptSetId);
      }
      jdbcTemplate.update(
          "insert into queue ("
              + String.join(", ", columns)
              + ") values ("
              + placeholders(columns.size())
              + ")",
          values.toArray());
      return;
    }

    List<String> assignments =
        new ArrayList<>(
            List.of(
                "name = ?",
                "description = ?",
                "location_id = ?",
                "service = ?",
                "changed_by = ?",
                "date_changed = ?",
                "retired = ?",
                "retired_by = ?",
                "date_retired = ?",
                "retire_reason = ?",
                "uuid = ?"));
    List<Object> values = new ArrayList<>();
    values.add(limit(name, 255));
    values.add(limit(record.value("Description"), 255));
    values.add(locationId);
    values.add(serviceConceptId);
    values.add(SYSTEM_USER_ID);
    values.add(now);
    values.add(retired);
    values.add(retiredBy);
    values.add(retiredAt);
    values.add(retireReason);
    values.add(uuid);
    if (hasStatusConceptSet) {
      assignments.add("status_concept_set = ?");
      values.add(statusConceptSetId);
    }
    if (hasPriorityConceptSet) {
      assignments.add("priority_concept_set = ?");
      values.add(priorityConceptSetId);
    }
    values.add(id);
    jdbcTemplate.update(
        "update queue set " + String.join(", ", assignments) + " where queue_id = ?",
        values.toArray());
  }

  private void upsertConceptReferenceRange(CsvRecord record) {
    String conceptIdentifier = requiredValue(record, "Concept Numeric uuid");
    Integer conceptId = findConceptId(conceptIdentifier);
    if (conceptId == null) {
      warnMissingOptionalConcept("Concept reference range", conceptIdentifier);
      return;
    }
    String uuid = requiredValue(record, "Uuid");
    Integer id =
        queryInteger(
            "select concept_reference_range_id from concept_reference_range where uuid = ?", uuid);

    if (id == null) {
      jdbcTemplate.update(
          "insert into concept_reference_range "
              + "(concept_id, criteria, hi_absolute, hi_critical, hi_normal, low_absolute, "
              + "low_critical, low_normal, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          conceptId,
          requiredValue(record, "Criteria"),
          toDouble(record.value("Absolute high")),
          toDouble(record.value("Critical high")),
          toDouble(record.value("Normal high")),
          toDouble(record.value("Absolute low")),
          toDouble(record.value("Critical low")),
          toDouble(record.value("Normal low")),
          uuid);
      return;
    }

    jdbcTemplate.update(
        "update concept_reference_range set concept_id = ?, criteria = ?, hi_absolute = ?, "
            + "hi_critical = ?, hi_normal = ?, low_absolute = ?, low_critical = ?, low_normal = ? "
            + "where concept_reference_range_id = ?",
        conceptId,
        requiredValue(record, "Criteria"),
        toDouble(record.value("Absolute high")),
        toDouble(record.value("Critical high")),
        toDouble(record.value("Normal high")),
        toDouble(record.value("Absolute low")),
        toDouble(record.value("Critical low")),
        toDouble(record.value("Normal low")),
        id);
  }

  private void upsertMetadataSet(CsvRecord record) {
    String uuid = requiredValue(record, "Uuid");
    Integer id =
        queryInteger(
            "select metadata_set_id from metadatamapping_metadata_set where uuid = ?", uuid);
    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into metadatamapping_metadata_set "
              + "(metadata_set_id, name, description, creator, date_created, retired, retired_by, "
              + "date_retired, retire_reason, uuid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          nextMetadataMappingId(
              "metadatamapping_metadata_set",
              "metadata_set_id",
              "metadatamapping_metadata_set_metadata_set_id_seq"),
          limit(record.value("Name"), 255),
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
        "update metadatamapping_metadata_set set name = ?, description = ?, changed_by = ?, "
            + "date_changed = ?, retired = ?, retired_by = ?, date_retired = ?, retire_reason = ?, uuid = ? "
            + "where metadata_set_id = ?",
        limit(record.value("Name"), 255),
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

  private void upsertMetadataTermMapping(CsvRecord record) {
    String mappingSource = requiredValue(record, "Mapping source");
    String mappingCode = requiredValue(record, "Mapping code");
    Integer sourceId = findMetadataSourceId(mappingSource);
    if (sourceId == null) {
      throw new IllegalStateException(
          "Metadata mapping source '" + mappingSource + "' was not loaded.");
    }

    String metadataClass = requiredValue(record, "Metadata class name");
    String metadataUuid = requiredValue(record, "Metadata Uuid");
    if (!metadataReferenceExists(metadataClass, metadataUuid)) {
      throw new IllegalStateException(
          "Metadata mapping reference "
              + metadataClass
              + " / "
              + metadataUuid
              + " was not loaded.");
    }

    String uuid =
        valueOrStableUuid(
            record.value("Uuid"), "metadata-term-mapping", mappingSource + ":" + mappingCode);
    Integer id =
        queryInteger(
            "select metadata_term_mapping_id from metadatamapping_metadata_term_mapping where uuid = ?",
            uuid);
    if (id == null) {
      id =
          queryInteger(
              "select metadata_term_mapping_id from metadatamapping_metadata_term_mapping "
                  + "where metadata_source_id = ? and code = ?",
              sourceId,
              mappingCode);
    }

    Timestamp now = now();
    boolean retired = toBoolean(record.value("Void/Retire"));
    Object retiredBy = retired ? SYSTEM_USER_ID : null;
    Object retiredAt = retired ? now : null;
    String retireReason = retired ? "Retired by SIH Salus content package" : null;

    if (id == null) {
      jdbcTemplate.update(
          "insert into metadatamapping_metadata_term_mapping "
              + "(metadata_term_mapping_id, metadata_source_id, code, metadata_class, metadata_uuid, creator, "
              + "date_created, retired, retired_by, date_retired, retire_reason, uuid) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          nextMetadataMappingId(
              "metadatamapping_metadata_term_mapping",
              "metadata_term_mapping_id",
              "metadatamapping_metadata_term_mapp_metadata_term_mapping_id_seq"),
          sourceId,
          limit(mappingCode, 255),
          limit(metadataClass, 1024),
          metadataUuid,
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
        "update metadatamapping_metadata_term_mapping set metadata_source_id = ?, code = ?, metadata_class = ?, "
            + "metadata_uuid = ?, changed_by = ?, date_changed = ?, retired = ?, retired_by = ?, "
            + "date_retired = ?, retire_reason = ?, uuid = ? where metadata_term_mapping_id = ?",
        sourceId,
        limit(mappingCode, 255),
        limit(metadataClass, 1024),
        metadataUuid,
        SYSTEM_USER_ID,
        now,
        retired,
        retiredBy,
        retiredAt,
        retireReason,
        uuid,
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

  private Integer findSpecialityId(String identifier) {
    if (isBlank(identifier)) {
      return null;
    }
    return findIdByUuidOrName("appointment_speciality", "speciality_id", identifier, identifier);
  }

  private Integer findConceptSourceId(String identifier) {
    if (isBlank(identifier)) {
      return null;
    }

    Integer id =
        findIdByUuidOrName("concept_reference_source", "concept_source_id", identifier, identifier);
    if (id == null && columnExists("concept_reference_source", "hl7_code")) {
      id =
          queryInteger(
              "select concept_source_id from concept_reference_source where hl7_code = ?",
              identifier);
    }
    if (id == null && columnExists("concept_reference_source", "unique_id")) {
      id =
          queryInteger(
              "select concept_source_id from concept_reference_source where unique_id = ?",
              identifier);
    }
    return id;
  }

  private Integer findPatientIdentifierTypeId(String identifier) {
    if (isBlank(identifier)) {
      return null;
    }
    return findIdByUuidOrName(
        "patient_identifier_type", "patient_identifier_type_id", identifier, identifier);
  }

  private Integer findMetadataSourceId(String name) {
    if (isBlank(name)) {
      return null;
    }
    return queryInteger(
        "select metadata_source_id from metadatamapping_metadata_source where name = ?", name);
  }

  private boolean metadataReferenceExists(String metadataClass, String metadataUuid) {
    if (isBlank(metadataClass) || isBlank(metadataUuid)) {
      return false;
    }
    return switch (metadataClass) {
      case "org.openmrs.PatientIdentifierType" ->
          countRows("select count(*) from patient_identifier_type where uuid = ?", metadataUuid)
              > 0;
      case "org.openmrs.EncounterType" ->
          countRows("select count(*) from encounter_type where uuid = ?", metadataUuid) > 0;
      case "org.openmrs.EncounterRole" ->
          countRows("select count(*) from encounter_role where uuid = ?", metadataUuid) > 0;
      case "org.openmrs.module.metadatamapping.MetadataSet" ->
          countRows(
                  "select count(*) from metadatamapping_metadata_set where uuid = ?", metadataUuid)
              > 0;
      default -> true;
    };
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
    if (id != null) {
      return id;
    }

    if (tableExists("concept_name")) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select concept_id from concept_name where name = ? and voided = false",
              Integer.class,
              identifier);
      if (!ids.isEmpty()) {
        return ids.get(0);
      }
    }

    return findConceptIdByReferenceTermCode(identifier);
  }

  private Integer requiredConceptId(CsvRecord record, String header) {
    String identifier = requiredValue(record, header);
    Integer conceptId = findConceptId(identifier);
    if (conceptId == null) {
      throw new IllegalStateException(
          "Concept reference '" + identifier + "' in " + record.source + " was not loaded.");
    }
    return conceptId;
  }

  private void warnMissingOptionalConcept(String context, String identifier) {
    String warningKey = context + ":" + identifier;
    if (missingOptionalConceptWarnings.add(warningKey)) {
      log.warn("{} concept '{}' was not loaded; storing a null reference.", context, identifier);
    }
  }

  private Integer findConceptIdByReferenceTermCode(String code) {
    if (isBlank(code)
        || !tableExists("concept_reference_term")
        || !tableExists("concept_reference_map")) {
      return null;
    }

    List<Integer> conceptIds =
        jdbcTemplate.queryForList(
            "select distinct map.concept_id from concept_reference_map map "
                + "join concept_reference_term term "
                + "on term.concept_reference_term_id = map.concept_reference_term_id "
                + "where term.code = ?",
            Integer.class,
            code);
    if (conceptIds.size() == 1) {
      return conceptIds.get(0);
    }
    if (conceptIds.size() > 1) {
      log.warn("Concept reference term code '{}' resolves to multiple concepts.", code);
    }
    return null;
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

  private Integer findProgramId(String uuid, String name) {
    return findIdByUuidOrName("program", "program_id", uuid, name);
  }

  private Integer findProgramWorkflowId(String uuid) {
    if (isBlank(uuid)) {
      return null;
    }
    return queryInteger("select program_workflow_id from program_workflow where uuid = ?", uuid);
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

  private List<Path> zipFiles(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException {
    return domainFiles(configRoot, domain, ".zip", wildcardExclusions);
  }

  private List<Path> domainFiles(
      Path configRoot, String domain, String extension, List<String> wildcardExclusions)
      throws IOException {
    Path directory = SihsalusContentPaths.resolveDomainDirectory(configRoot, domain);
    if (directory == null) {
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
    DocumentBuilderFactory factory = secureDocumentBuilderFactory();
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

  private List<MetadataSourceRecord> readMetadataSourcePackage(Path zipPackage)
      throws IOException, ParserConfigurationException, SAXException {
    try (ZipFile zipFile = new ZipFile(zipPackage.toFile())) {
      ZipEntry metadataEntry = zipFile.getEntry("metadata.xml");
      if (metadataEntry == null || metadataEntry.isDirectory()) {
        return List.of();
      }

      Document document;
      try (InputStream inputStream = zipFile.getInputStream(metadataEntry)) {
        document = secureDocumentBuilderFactory().newDocumentBuilder().parse(inputStream);
      }

      List<MetadataSourceRecord> records = new ArrayList<>();
      NodeList sources =
          document.getElementsByTagName("org.openmrs.module.metadatamapping.MetadataSource");
      for (int index = 0; index < sources.getLength(); index++) {
        Element element = (Element) sources.item(index);
        records.add(
            new MetadataSourceRecord(
                element.getAttribute("uuid"),
                elementText(element, "name"),
                elementText(element, "description"),
                toBoolean(elementText(element, "retired"))));
      }
      return records;
    }
  }

  private DocumentBuilderFactory secureDocumentBuilderFactory()
      throws ParserConfigurationException {
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
    return factory;
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

  private boolean isPostgres() {
    return jdbcTemplate.execute(
        (ConnectionCallback<Boolean>)
            connection ->
                connection
                    .getMetaData()
                    .getDatabaseProductName()
                    .toLowerCase(Locale.ROOT)
                    .contains("postgres"));
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

  private String queryString(String sql, Object... args) {
    List<String> values = jdbcTemplate.queryForList(sql, String.class, args);
    return values.isEmpty() ? null : values.get(0);
  }

  private Integer queryIntegerIfSupported(String sql) {
    try {
      return jdbcTemplate.queryForObject(sql, Integer.class);
    } catch (DataAccessException e) {
      log.debug("Integer query is not supported by the current database: {}", sql);
      return null;
    }
  }

  private Integer nextMetadataMappingId(String table, String idColumn, String sequenceName) {
    Integer id = nextSequenceValue(sequenceName);
    int attempts = 0;
    while (id != null
        && countRows("select count(*) from " + table + " where " + idColumn + " = ?", id) > 0
        && attempts < 50) {
      id = nextSequenceValue(sequenceName);
      attempts++;
    }
    if (id != null
        && countRows("select count(*) from " + table + " where " + idColumn + " = ?", id) == 0) {
      return id;
    }
    return jdbcTemplate.queryForObject(
        "select coalesce(max(" + idColumn + "), 0) + 1 from " + table, Integer.class);
  }

  private int nextManualId(String table, String idColumn) {
    Integer id =
        jdbcTemplate.queryForObject(
            "select coalesce(max(" + idColumn + "), 0) + 1 from " + table, Integer.class);
    return id == null ? 1 : id;
  }

  private void restartSequence(String sequenceName, int nextValue) {
    try {
      jdbcTemplate.execute("alter sequence " + sequenceName + " restart with " + nextValue);
    } catch (DataAccessException e) {
      log.debug("Sequence restart is not supported by the current database: {}", sequenceName);
    }
  }

  private Integer nextSequenceValue(String sequenceName) {
    Integer value = queryIntegerIfSupported("select next value for " + sequenceName);
    if (value != null) {
      return value;
    }
    return queryIntegerIfSupported("select nextval('" + sequenceName + "')");
  }

  private int countRows(String sql, Object... args) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  private String placeholders(int count) {
    return String.join(", ", java.util.Collections.nCopies(count, "?"));
  }

  private Object[] parameters(List<?> fixedParameters, List<?> dynamicParameters) {
    List<Object> parameters = new ArrayList<>(fixedParameters);
    parameters.addAll(dynamicParameters);
    return parameters.toArray();
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

  private String uuidFromObjects(Object... values) {
    List<String> seedParts = new ArrayList<>();
    for (Object value : values) {
      seedParts.add(value == null ? "null" : value.toString());
    }
    return UUID.nameUUIDFromBytes(String.join("_", seedParts).getBytes(StandardCharsets.UTF_8))
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

  private Double toDouble(String value) {
    if (isBlank(value)) {
      return null;
    }
    return Double.valueOf(value.trim());
  }

  private Integer requiredInteger(CsvRecord record, String header) {
    return Integer.valueOf(requiredValue(record, header));
  }

  private String requiredValue(CsvRecord record, String header) {
    String value = record.value(header);
    if (isBlank(value)) {
      throw new IllegalStateException(header + " is required in " + record.source + ".");
    }
    return value;
  }

  private int toInteger(String value, int defaultValue) {
    Integer parsed = toInteger(value);
    return parsed == null ? defaultValue : parsed;
  }

  private Time toSqlTime(String value) {
    if (isBlank(value)) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() == 5) {
      trimmed = trimmed + ":00";
    }
    return Time.valueOf(trimmed);
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

  private String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  private String stringOrEmpty(String value) {
    return isBlank(value) ? "" : value.trim();
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

  private record MetadataSourceRecord(
      String uuid, String name, String description, boolean retired) {}

  private record DataFilterObject(String identifier, String className) {}

  private record DataFilterObjectType(
      String className, String table, String idColumn, String identifierColumn) {}

  private record AddressHierarchyConfig(
      List<AddressHierarchyComponent> components,
      String filename,
      String entryDelimiter,
      String identifierDelimiter,
      List<String> lineByLineFormat) {}

  private record AddressHierarchyComponent(
      String field,
      String token,
      String nameMapping,
      int sizeMapping,
      String elementDefault,
      boolean required) {}

  private record AddressHierarchyEntryValue(String name, String userGeneratedId) {}

  private record AddressHierarchyEntryKey(Integer parentId, Integer levelId, String name) {}

  private record AttributeTypeTable(String table, String idColumn) {}

  private record PaymentModeAttributeRecord(
      String uuid,
      String name,
      String format,
      String regExp,
      boolean required,
      int attributeOrder) {}
}
