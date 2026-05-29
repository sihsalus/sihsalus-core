package org.sihsalus.initializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class StaticSihsalusContentLoaderTest {

  @TempDir private Path tempDir;

  private String previousSourceRoot;

  private JdbcTemplate jdbcTemplate;

  private StaticSihsalusContentLoader loader;

  @BeforeEach
  void setUp() {
    previousSourceRoot = System.getProperty("sihsalus.initializer.sourceRoot");
    System.setProperty("sihsalus.initializer.sourceRoot", tempDir.toString());

    JdbcDataSource dataSource = new JdbcDataSource();
    String databaseName = "initializer_" + UUID.randomUUID().toString().replace("-", "");
    dataSource.setURL(
        "jdbc:h2:mem:"
            + databaseName
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=ROLE,USER");
    jdbcTemplate = new JdbcTemplate(dataSource);
    createDataFilterSchema();
    createGlobalPropertySchema();
    loader = new StaticSihsalusContentLoader(jdbcTemplate);
  }

  @AfterEach
  void restoreEnvironment() {
    if (previousSourceRoot == null) {
      System.clearProperty("sihsalus.initializer.sourceRoot");
    } else {
      System.setProperty("sihsalus.initializer.sourceRoot", previousSourceRoot);
    }
  }

  @Test
  void globalPropertiesOverrideLiquibaseSeededValues() throws Exception {
    jdbcTemplate.update(
        "insert into global_property (property, property_value, description, uuid) values (?, ?, ?, ?)",
        "emrapi.conceptCode.disposition",
        "Disposition",
        "Liquibase default",
        "11111111-1111-1111-1111-111111111111");

    writeGlobalProperties(
        """
        <config>
          <globalProperties>
            <globalProperty>
              <property>emrapi.conceptCode.disposition</property>
              <value>sihsalus-disposition</value>
              <description>Initializer value</description>
            </globalProperty>
          </globalProperties>
        </config>
        """);

    loader.loadDomain("globalproperties", List.of());

    assertEquals(
        "sihsalus-disposition",
        jdbcTemplate.queryForObject(
            "select property_value from global_property where property = ?",
            String.class,
            "emrapi.conceptCode.disposition"));
    assertEquals(
        "Initializer value",
        jdbcTemplate.queryForObject(
            "select description from global_property where property = ?",
            String.class,
            "emrapi.conceptCode.disposition"));
  }

  @Test
  void loadPostConceptDomainsLoadsAppointmentServices() throws Exception {
    createAppointmentDomainSchema();

    writeAppointmentSpecialities(
        """
        Uuid,Void/Retire,Name,Description
        11111111-1111-1111-1111-111111111111,,Consulta ambulatoria,
        """);

    writeAppointmentServiceDefinitions(
        """
        Uuid,Void/Retire,Name,Description,Duration,Start Time,End Time,Max Load,Speciality,Location,Label Colour
        22222222-2222-2222-2222-222222222222,,Consulta de prueba,Atencion inicial,,,,,,,
        """
            .trim());

    loader.loadPostConceptDomains();

    Integer specialityCount =
        jdbcTemplate.queryForObject(
            "select count(*) from appointment_speciality where uuid = ?",
            Integer.class,
            "11111111-1111-1111-1111-111111111111");
    assertEquals(1, specialityCount);

    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from appointment_service where uuid = ?",
            Integer.class,
            "22222222-2222-2222-2222-222222222222");
    assertEquals(1, count);
  }

  @Test
  void loadPostConceptDomainsLoadsOrderFrequencies() throws Exception {
    createConceptTableSchema();
    createOrderFrequencySchema();

    jdbcTemplate.update(
        "insert into concept (concept_id, uuid) values (?, ?)",
        1,
        "11111111-1111-1111-1111-111111111111");

    writeOrderFrequencies(
        """
        Uuid,Void/Retire,Concept frequency,Frequency per day
        22222222-2222-2222-2222-222222222222,,11111111-1111-1111-1111-111111111111,3
        """);

    loader.loadPostConceptDomains();

    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from order_frequency where uuid = ?",
            Integer.class,
            "22222222-2222-2222-2222-222222222222");
    assertEquals(1, count);
  }

  @Test
  void loadDomainUnknownDomainIsIgnored() throws Exception {
    loader.loadDomain("unknown-domain", List.of());
  }

  @Test
  void dataFilterMappingsUseRuntimeIdentifiersAndRemainIdempotent() throws Exception {
    String roleUuid = "11111111-1111-1111-1111-111111111111";
    String locationUuid = "22222222-2222-2222-2222-222222222222";
    jdbcTemplate.update(
        "insert into role (role, description, uuid) values (?, ?, ?)",
        "Clinical Role",
        "Clinical role",
        roleUuid);
    jdbcTemplate.update(
        "insert into location (location_id, name, uuid) values (?, ?, ?)",
        42,
        "Clinic",
        locationUuid);

    writeDataFilterMappings(
        """
        Void/Retire,Entity UUID,Entity class,Basis UUID,Basis class
        ,11111111-1111-1111-1111-111111111111,org.openmrs.Role,22222222-2222-2222-2222-222222222222,org.openmrs.Location
        ,11111111-1111-1111-1111-111111111111,org.openmrs.Role,22222222-2222-2222-2222-222222222222,org.openmrs.Location
        """);

    loader.loadDomain("datafiltermappings", List.of());
    loader.loadDomain("datafiltermappings", List.of());

    assertEquals(1, mapCount());
    assertEquals(
        "Clinical Role",
        jdbcTemplate.queryForObject(
            "select entity_identifier from datafilter_entity_basis_map", String.class));
    assertEquals(
        "42",
        jdbcTemplate.queryForObject(
            "select basis_identifier from datafilter_entity_basis_map", String.class));

    writeDataFilterMappings(
        """
        Void/Retire,Entity UUID,Entity class,Basis UUID,Basis class
        true,11111111-1111-1111-1111-111111111111,org.openmrs.Role,22222222-2222-2222-2222-222222222222,org.openmrs.Location
        """);

    loader.loadDomain("datafiltermappings", List.of());

    assertEquals(0, mapCount());
  }

  private void createDataFilterSchema() {
    jdbcTemplate.execute("create table users (user_id int primary key, uuid varchar(38) not null)");
    jdbcTemplate.execute(
        "insert into users (user_id, uuid) values (1, '00000000-0000-0000-0000-000000000001')");
    jdbcTemplate.execute(
        "create table role (role varchar(50) primary key, description varchar(255), uuid varchar(38) not null unique)");
    jdbcTemplate.execute(
        "create table location (location_id int primary key, name varchar(255) not null, uuid varchar(38) not null unique)");
    jdbcTemplate.execute(
        "create table datafilter_entity_basis_map ("
            + "entity_basis_map_id int generated by default as identity primary key, "
            + "entity_identifier varchar(127) not null, "
            + "entity_type varchar(255) not null, "
            + "basis_identifier varchar(127) not null, "
            + "basis_type varchar(255) not null, "
            + "creator int not null, "
            + "date_created timestamp not null, "
            + "uuid varchar(38) not null unique)");
    jdbcTemplate.execute(
        "create unique index entity_basis_UK on datafilter_entity_basis_map "
            + "(entity_identifier, entity_type, basis_identifier, basis_type)");
  }

  private void createGlobalPropertySchema() {
    jdbcTemplate.execute(
        "create table global_property ("
            + "property varchar(255) primary key, "
            + "property_value varchar(4000), "
            + "description varchar(1024), "
            + "changed_by int, "
            + "date_changed timestamp, "
            + "uuid varchar(38) not null unique)");
  }

  private int mapCount() {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from datafilter_entity_basis_map", Integer.class);
    return count == null ? 0 : count;
  }

  private void writeDataFilterMappings(String content) throws Exception {
    Path directory =
        tempDir
            .resolve("configuration")
            .resolve("backend_configuration")
            .resolve("datafiltermappings");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("mappings.csv"), content);
  }

  private void createAppointmentDomainSchema() {
    jdbcTemplate.execute(
        "create table if not exists appointment_speciality ("
            + "speciality_id int generated by default as identity primary key,"
            + "name varchar(50),"
            + "creator int,"
            + "date_created timestamp,"
            + "changed_by int,"
            + "date_changed timestamp,"
            + "uuid varchar(38) not null unique,"
            + "voided boolean)");

    jdbcTemplate.execute(
        "create table if not exists appointment_service ("
            + "appointment_service_id int generated by default as identity primary key,"
            + "name varchar(255),"
            + "description varchar(1024),"
            + "start_time varchar(8),"
            + "end_time varchar(8),"
            + "location_id int,"
            + "speciality_id int,"
            + "max_appointments_limit int,"
            + "duration_mins int,"
            + "date_created timestamp,"
            + "creator int,"
            + "date_changed timestamp,"
            + "changed_by int,"
            + "voided boolean,"
            + "voided_by int,"
            + "date_voided timestamp,"
            + "void_reason varchar(1024),"
            + "uuid varchar(38) not null unique,"
            + "color varchar(8),"
            + "initial_appointment_status varchar(20))");
  }

  private void createConceptTableSchema() {
    jdbcTemplate.execute(
        "create table if not exists concept (concept_id int primary key, uuid varchar(38) not null)");
  }

  private void createOrderFrequencySchema() {
    jdbcTemplate.execute(
        "create table if not exists order_frequency ("
            + "order_frequency_id int generated by default as identity primary key,"
            + "concept_id int,"
            + "frequency_per_day double,"
            + "creator int,"
            + "date_created timestamp,"
            + "changed_by int,"
            + "date_changed timestamp,"
            + "retired boolean default false,"
            + "retired_by int,"
            + "date_retired timestamp,"
            + "retire_reason varchar(255),"
            + "uuid varchar(38) not null unique)");
  }

  private void writeAppointmentSpecialities(String content) throws Exception {
    Path directory =
        tempDir
            .resolve("configuration")
            .resolve("backend_configuration")
            .resolve("appointmentspecialities");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("specialities.csv"), content);
  }

  private void writeAppointmentServiceDefinitions(String content) throws Exception {
    Path directory =
        tempDir
            .resolve("configuration")
            .resolve("backend_configuration")
            .resolve("appointmentservicedefinitions");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("servicedefinitions.csv"), content);
  }

  private void writeOrderFrequencies(String content) throws Exception {
    Path directory =
        tempDir
            .resolve("configuration")
            .resolve("backend_configuration")
            .resolve("orderfrequencies");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("orderfrequencies.csv"), content);
  }

  private void writeGlobalProperties(String content) throws Exception {
    Path directory =
        tempDir
            .resolve("configuration")
            .resolve("backend_configuration")
            .resolve("globalproperties");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("globalproperties.xml"), content);
  }
}
