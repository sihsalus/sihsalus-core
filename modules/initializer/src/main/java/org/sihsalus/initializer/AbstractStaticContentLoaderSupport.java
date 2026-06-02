package org.sihsalus.initializer;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
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
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Generic, domain-agnostic plumbing shared by the static content loader: database introspection,
 * query primitives, sequence/id helpers, value parsing/conversion, UUID/timestamp generation and
 * the CSV row abstraction.
 *
 * <p>This base class deliberately knows nothing about individual content domains; it exists so that
 * {@link StaticSihsalusContentLoader} can focus on per-domain load/upsert logic.
 */
abstract class AbstractStaticContentLoaderSupport {

  private static final Logger log =
      LoggerFactory.getLogger(AbstractStaticContentLoaderSupport.class);

  final JdbcTemplate jdbcTemplate;

  private final Map<String, Boolean> tableCache = new HashMap<>();

  private final Map<String, Set<String>> columnCache = new HashMap<>();

  AbstractStaticContentLoaderSupport(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  String retiredColumn(String table) {
    if (columnExists(table, "retired")) {
      return "retired";
    }
    if (columnExists(table, "voided")) {
      return "voided";
    }
    throw new IllegalStateException("No retired/voided column found on " + table + ".");
  }

  boolean tableExists(String table) {
    return tableCache.computeIfAbsent(
        table.toLowerCase(Locale.ROOT), ignored -> !columns(table).isEmpty());
  }

  boolean columnExists(String table, String column) {
    return columns(table).contains(column.toLowerCase(Locale.ROOT));
  }

  boolean isPostgres() {
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

  Integer queryInteger(String sql, Object... args) {
    List<Integer> values = jdbcTemplate.queryForList(sql, Integer.class, args);
    return values.isEmpty() ? null : values.get(0);
  }

  String queryString(String sql, Object... args) {
    List<String> values = jdbcTemplate.queryForList(sql, String.class, args);
    return values.isEmpty() ? null : values.get(0);
  }

  Integer queryIntegerIfSupported(String sql) {
    try {
      return jdbcTemplate.queryForObject(sql, Integer.class);
    } catch (DataAccessException e) {
      log.debug("Integer query is not supported by the current database: {}", sql);
      return null;
    }
  }

  Integer nextMetadataMappingId(String table, String idColumn, String sequenceName) {
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

  int nextManualId(String table, String idColumn) {
    Integer id =
        jdbcTemplate.queryForObject(
            "select coalesce(max(" + idColumn + "), 0) + 1 from " + table, Integer.class);
    return id == null ? 1 : id;
  }

  void restartSequence(String sequenceName, int nextValue) {
    try {
      jdbcTemplate.execute("alter sequence " + sequenceName + " restart with " + nextValue);
    } catch (DataAccessException e) {
      log.debug("Sequence restart is not supported by the current database: {}", sequenceName);
    }
  }

  Integer nextSequenceValue(String sequenceName) {
    Integer value = queryIntegerIfSupported("select next value for " + sequenceName);
    if (value != null) {
      return value;
    }
    return queryIntegerIfSupported("select nextval('" + sequenceName + "')");
  }

  int countRows(String sql, Object... args) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  String placeholders(int count) {
    return String.join(", ", java.util.Collections.nCopies(count, "?"));
  }

  Object[] parameters(List<?> fixedParameters, List<?> dynamicParameters) {
    List<Object> parameters = new ArrayList<>(fixedParameters);
    parameters.addAll(dynamicParameters);
    return parameters.toArray();
  }

  Timestamp now() {
    return Timestamp.from(Instant.now());
  }

  String valueOrStableUuid(String value, String namespace, String key) {
    return isBlank(value) ? stableUuid(namespace, key) : value.trim();
  }

  String stableUuid(String namespace, String value) {
    return UUID.nameUUIDFromBytes((namespace + ":" + value).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  String uuidFromObjects(Object... values) {
    List<String> seedParts = new ArrayList<>();
    for (Object value : values) {
      seedParts.add(value == null ? "null" : value.toString());
    }
    return UUID.nameUUIDFromBytes(String.join("_", seedParts).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  List<String> splitList(String value) {
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

  boolean toBoolean(String value) {
    if (isBlank(value)) {
      return false;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("true")
        || normalized.equals("yes")
        || normalized.equals("y")
        || normalized.equals("1");
  }

  Integer toInteger(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid integer value: " + value, e);
    }
  }

  Double toDouble(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Invalid decimal value: " + value, e);
    }
  }

  Integer requiredInteger(CsvRecord record, String header) {
    try {
      return Integer.valueOf(requiredValue(record, header));
    } catch (NumberFormatException e) {
      throw new IllegalStateException(header + " must be an integer in " + record.source + ".", e);
    }
  }

  String requiredValue(CsvRecord record, String header) {
    String value = record.value(header);
    if (isBlank(value)) {
      throw new IllegalStateException(header + " is required in " + record.source + ".");
    }
    return value;
  }

  int toInteger(String value, int defaultValue) {
    Integer parsed = toInteger(value);
    return parsed == null ? defaultValue : parsed;
  }

  Time toSqlTime(String value) {
    if (isBlank(value)) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() == 5) {
      trimmed = trimmed + ":00";
    }
    return Time.valueOf(trimmed);
  }

  String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  String limit(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  String blankToNull(String value) {
    return isBlank(value) ? null : value.trim();
  }

  String stringOrEmpty(String value) {
    return isBlank(value) ? "" : value.trim();
  }

  boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  boolean allBlank(String[] row) {
    for (String value : row) {
      if (!isBlank(value)) {
        return false;
      }
    }
    return true;
  }

  private final java.util.Set<String> missingOptionalConceptWarnings =
      new java.util.LinkedHashSet<>();

  record AttributeTypeTable(String table, String idColumn) {}

  Integer findPatientIdentifierTypeId(String identifier) {
    if (isBlank(identifier)) {
      return null;
    }
    return findIdByUuidOrName(
        "patient_identifier_type", "patient_identifier_type_id", identifier, identifier);
  }

  Integer findMetadataSourceId(String name) {
    if (isBlank(name)) {
      return null;
    }
    return queryInteger(
        "select metadata_source_id from metadatamapping_metadata_source where name = ?", name);
  }

  boolean metadataReferenceExists(String metadataClass, String metadataUuid) {
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

  Integer findRelationshipTypeId(String uuid, String aIsToB, String bIsToA) {
    Integer id = null;
    if (!isBlank(uuid)) {
      id = queryInteger("select relationship_type_id from relationship_type where uuid = ?", uuid);
    }
    if (id == null && !isBlank(aIsToB) && !isBlank(bIsToA)) {
      List<Integer> ids =
          jdbcTemplate.queryForList(
              "select relationship_type_id from relationship_type where a_is_to_b = ? and b_is_to_a"
                  + " = ?",
              Integer.class,
              aIsToB,
              bIsToA);
      id = ids.isEmpty() ? null : ids.get(0);
    }
    return id;
  }

  Integer findConceptId(String identifier) {
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

  Integer requiredConceptId(CsvRecord record, String header) {
    String identifier = requiredValue(record, header);
    Integer conceptId = findConceptId(identifier);
    if (conceptId == null) {
      throw new IllegalStateException(
          "Concept reference '" + identifier + "' in " + record.source + " was not loaded.");
    }
    return conceptId;
  }

  void warnMissingOptionalConcept(String context, String identifier) {
    String warningKey = context + ":" + identifier;
    if (missingOptionalConceptWarnings.add(warningKey)) {
      log.warn("{} concept '{}' was not loaded; storing a null reference.", context, identifier);
    }
  }

  Integer findConceptIdByReferenceTermCode(String code) {
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

  Integer resolvePersonAttributeForeignKey(String value) {
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

  AttributeTypeTable attributeTypeTable(String entityName) {
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

  Integer findConceptClassId(String identifier) {
    return findIdByUuidOrName("concept_class", "concept_class_id", identifier, identifier);
  }

  Integer findOrderTypeId(String uuid, String name) {
    return findIdByUuidOrName("order_type", "order_type_id", uuid, name);
  }

  Integer findProgramId(String uuid, String name) {
    return findIdByUuidOrName("program", "program_id", uuid, name);
  }

  Integer findProgramWorkflowId(String uuid) {
    if (isBlank(uuid)) {
      return null;
    }
    return queryInteger("select program_workflow_id from program_workflow where uuid = ?", uuid);
  }

  Integer findIdByUuidOrName(String table, String idColumn, String uuid, String name) {
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

  List<CsvRecord> readDomain(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException, CsvException {
    List<CsvRecord> records = new ArrayList<>();
    for (Path csvFile : csvFiles(configRoot, domain, wildcardExclusions)) {
      records.addAll(readCsv(csvFile));
    }
    return records;
  }

  List<Path> csvFiles(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException {
    return domainFiles(configRoot, domain, ".csv", wildcardExclusions);
  }

  List<Path> xmlFiles(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException {
    return domainFiles(configRoot, domain, ".xml", wildcardExclusions);
  }

  List<Path> zipFiles(Path configRoot, String domain, List<String> wildcardExclusions)
      throws IOException {
    return domainFiles(configRoot, domain, ".zip", wildcardExclusions);
  }

  List<Path> domainFiles(
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

  boolean excludedByWildcard(Path path, Path directory, List<String> wildcardExclusions) {
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

  List<CsvRecord> readCsv(Path csvFile) throws IOException, CsvException {
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

  DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
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

  String elementText(Element element, String tagName) {
    NodeList children = element.getElementsByTagName(tagName);
    if (children.getLength() == 0) {
      return null;
    }
    String text = children.item(0).getTextContent();
    return isBlank(text) ? null : text.trim();
  }

  void setXmlAttributeIfSupported(
      DocumentBuilderFactory factory, String attributeName, String value) {
    try {
      factory.setAttribute(attributeName, value);
    } catch (IllegalArgumentException e) {
      log.debug("XML parser does not support secure attribute {}.", attributeName);
    }
  }

  static final class CsvRecord {

    final Path source;

    private final Map<String, Integer> headerIndexes;

    private final Map<String, String> normalizedHeaders;

    private final String[] row;

    CsvRecord(Path source, String[] headers, String[] row) {
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

    String value(String header) {
      Integer index = headerIndexes.get(normalizeHeader(header).toLowerCase(Locale.ROOT));
      if (index == null || index >= row.length) {
        return null;
      }
      String value = row[index];
      return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    Set<String> headers() {
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
}
