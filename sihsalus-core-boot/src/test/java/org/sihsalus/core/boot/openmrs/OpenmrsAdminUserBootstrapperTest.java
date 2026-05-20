package org.sihsalus.core.boot.openmrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.util.Security;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

final class OpenmrsAdminUserBootstrapperTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:mem:admin-bootstrap-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                "create table users ("
                        + "user_id int primary key, "
                        + "system_id varchar(64), "
                        + "username varchar(64), "
                        + "password varchar(128), "
                        + "salt varchar(128), "
                        + "changed_by int, "
                        + "date_changed timestamp, "
                        + "retired boolean)");
        jdbcTemplate.execute(
                "create table global_property ("
                        + "property varchar(255) primary key, "
                        + "property_value varchar(255), "
                        + "description varchar(255), "
                        + "uuid varchar(64))");
    }

    @Test
    void refusesToKeepDefaultOpenmrsAdminPassword() {
        insertAdminUser("", "4a1750c8607dfa237de36c6305715c223415189", "c788c6ad82a157b712392ca695dfcf2eed193d7f");

        OpenmrsAdminUserBootstrapper bootstrapper = new OpenmrsAdminUserBootstrapper(jdbcTemplate, "admin", "");

        assertThrows(IllegalStateException.class, () -> bootstrapper.run(null));
    }

    @Test
    void refusesToKeepDefaultAdminPasswordWithCustomSalt() {
        String salt = "custom-salt";
        insertAdminUser("", Security.encodeString("Admin123" + salt), salt);

        OpenmrsAdminUserBootstrapper bootstrapper = new OpenmrsAdminUserBootstrapper(jdbcTemplate, "admin", "");

        assertThrows(IllegalStateException.class, () -> bootstrapper.run(null));
    }

    @Test
    void updatesAdminPasswordAndSchedulerCredentialsWhenPasswordIsConfigured() {
        insertAdminUser("", "4a1750c8607dfa237de36c6305715c223415189", "c788c6ad82a157b712392ca695dfcf2eed193d7f");

        OpenmrsAdminUserBootstrapper bootstrapper =
                new OpenmrsAdminUserBootstrapper(jdbcTemplate, "admin", "configured-password");

        bootstrapper.run(null);

        assertEquals("admin", queryString("select username from users where user_id = 1"));
        assertNotEquals(
                "4a1750c8607dfa237de36c6305715c223415189",
                queryString("select password from users where user_id = 1"));
        assertEquals("admin", queryString("select property_value from global_property where property = 'scheduler.username'"));
        assertEquals(
                "configured-password",
                queryString("select property_value from global_property where property = 'scheduler.password'"));
    }

    @Test
    void leavesNonDefaultAdminPasswordUnchangedWhenNoPasswordIsConfigured() {
        insertAdminUser("admin", "already-changed", "custom-salt");

        OpenmrsAdminUserBootstrapper bootstrapper = new OpenmrsAdminUserBootstrapper(jdbcTemplate, "admin", "");

        bootstrapper.run(null);

        assertEquals("already-changed", queryString("select password from users where user_id = 1"));
    }

    private void insertAdminUser(String username, String password, String salt) {
        jdbcTemplate.update(
                "insert into users (user_id, system_id, username, password, salt, retired) values (?, ?, ?, ?, ?, ?)",
                1,
                "admin",
                username,
                password,
                salt,
                false);
    }

    private String queryString(String sql) {
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
