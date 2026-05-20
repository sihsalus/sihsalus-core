package org.sihsalus.core.boot.openmrs;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.util.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class OpenmrsAdminUserBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OpenmrsAdminUserBootstrapper.class);

    private final JdbcTemplate jdbcTemplate;

    private final String adminUsername;

    private final String adminPassword;

    OpenmrsAdminUserBootstrapper(
            JdbcTemplate jdbcTemplate,
            @Value("${sihsalus.admin.username:admin}") String adminUsername,
            @Value("${sihsalus.admin.password:}") String adminPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<AdminUserRecord> records =
                jdbcTemplate.query(
                        "select user_id, username, salt from users where system_id = 'admin' and retired = false",
                        this::mapAdminUser);
        if (records.isEmpty()) {
            log.warn("Cannot normalize OpenMRS admin user because no active system_id=admin user exists");
            return;
        }

        AdminUserRecord adminUser = records.get(0);
        if (!adminUsername.equals(adminUser.username())) {
            jdbcTemplate.update("update users set username = ? where user_id = ?", adminUsername, adminUser.userId());
        }

        if (StringUtils.isNotBlank(adminPassword)) {
            String salt = StringUtils.defaultIfBlank(adminUser.salt(), Security.getRandomToken());
            String hashedPassword = Security.encodeString(adminPassword + salt);
            jdbcTemplate.update(
                    "update users set password = ?, salt = ?, changed_by = ?, date_changed = current_timestamp "
                            + "where user_id = ?",
                    hashedPassword,
                    salt,
                    adminUser.userId(),
                    adminUser.userId());
            upsertGlobalProperty("scheduler.username", adminUsername);
            upsertGlobalProperty("scheduler.password", adminPassword);
        }
    }

    private AdminUserRecord mapAdminUser(ResultSet rs, int rowNumber) throws SQLException {
        return new AdminUserRecord(rs.getInt("user_id"), rs.getString("username"), rs.getString("salt"));
    }

    private void upsertGlobalProperty(String property, String value) {
        int updated =
                jdbcTemplate.update(
                        "update global_property set property_value = ? where property = ?",
                        value,
                        property);
        if (updated == 0) {
            jdbcTemplate.update(
                    "insert into global_property (property, property_value, description, uuid) values (?, ?, ?, ?)",
                    property,
                    value,
                    "Configured by SIH Salus static runtime",
                    java.util.UUID.nameUUIDFromBytes(property.getBytes(StandardCharsets.UTF_8)).toString());
        }
    }

    private record AdminUserRecord(Integer userId, String username, String salt) {}
}
