package org.sihsalus.core.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillLineItemService;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.CashPointService;
import org.openmrs.module.billing.api.PaymentModeService;
import org.openmrs.module.billing.api.model.Bill;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.CashPoint;
import org.openmrs.module.billing.api.model.PaymentMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "sihsalus.ocl.static-import.enabled=false")
@ActiveProfiles("test")
class SihsalusBillingAuditIntegrationTest {

    private static final String TEST_ADMIN_USERNAME = "admin";

    private static final String TEST_ADMIN_PASSWORD = "Admin123";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void billingVoidAndRetireServicesSetAuditFields() {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        boolean authenticatedBeforeTest = Context.isAuthenticated();
        if (!authenticatedBeforeTest) {
            Context.authenticate(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        }

        try {
            Integer adminUserId = jdbcTemplate.queryForObject(
                    "select user_id from users where username = ?",
                    Integer.class,
                    TEST_ADMIN_USERNAME);
            assertNotNull(adminUserId);

            Integer patientId = createPatient(adminUserId);
            Integer providerId = createProvider(adminUserId);
            BillingFixtures fixtures = createBillingFixtures(adminUserId, patientId, providerId);

            BillService billService = Context.getService(BillService.class);
            BillLineItemService billLineItemService = Context.getService(BillLineItemService.class);
            CashPointService cashPointService = Context.getService(CashPointService.class);
            PaymentModeService paymentModeService = Context.getService(PaymentModeService.class);
            BillableServiceService billableServiceService = Context.getService(BillableServiceService.class);

            Bill bill = billService.getBillByUuid(fixtures.billUuid());
            billService.voidBill(bill, "audit void bill");
            assertTrue(booleanColumn("select voided from cashier_bill where uuid = ?", fixtures.billUuid()));
            assertEquals(adminUserId, intColumn("select voided_by from cashier_bill where uuid = ?", fixtures.billUuid()));
            assertEquals(
                    "audit void bill",
                    stringColumn("select void_reason from cashier_bill where uuid = ?", fixtures.billUuid()));
            assertNotNull(dateColumn("select date_voided from cashier_bill where uuid = ?", fixtures.billUuid()));

            billService.unvoidBill(billService.getBillByUuid(fixtures.billUuid()));
            assertFalse(booleanColumn("select voided from cashier_bill where uuid = ?", fixtures.billUuid()));
            assertNull(intColumn("select voided_by from cashier_bill where uuid = ?", fixtures.billUuid()));
            assertNull(stringColumn("select void_reason from cashier_bill where uuid = ?", fixtures.billUuid()));
            assertNull(dateColumn("select date_voided from cashier_bill where uuid = ?", fixtures.billUuid()));

            BillLineItem lineItem = billLineItemService.getBillLineItemByUuid(fixtures.lineItemUuid());
            billLineItemService.voidBillLineItem(lineItem, "audit void line item");
            assertTrue(booleanColumn("select voided from cashier_bill_line_item where uuid = ?", fixtures.lineItemUuid()));
            assertEquals(
                    adminUserId,
                    intColumn("select voided_by from cashier_bill_line_item where uuid = ?", fixtures.lineItemUuid()));
            assertEquals(
                    "audit void line item",
                    stringColumn("select void_reason from cashier_bill_line_item where uuid = ?", fixtures.lineItemUuid()));
            assertNotNull(dateColumn(
                    "select date_voided from cashier_bill_line_item where uuid = ?",
                    fixtures.lineItemUuid()));

            CashPoint cashPoint = cashPointService.getCashPointByUuid(fixtures.cashPointUuid());
            cashPointService.retireCashPoint(cashPoint, "audit retire cash point");
            assertTrue(booleanColumn("select retired from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));
            assertEquals(
                    adminUserId,
                    intColumn("select retired_by from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));
            assertEquals(
                    "audit retire cash point",
                    stringColumn("select retire_reason from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));
            assertNotNull(dateColumn(
                    "select date_retired from cashier_cash_point where uuid = ?",
                    fixtures.cashPointUuid()));
            cashPointService.unretireCashPoint(cashPointService.getCashPointByUuid(fixtures.cashPointUuid()));
            assertFalse(booleanColumn("select retired from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));
            assertNull(intColumn("select retired_by from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));
            assertNull(stringColumn("select retire_reason from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));
            assertNull(dateColumn("select date_retired from cashier_cash_point where uuid = ?", fixtures.cashPointUuid()));

            PaymentMode paymentMode = paymentModeService.getPaymentModeByUuid(fixtures.paymentModeUuid());
            paymentModeService.retirePaymentMode(paymentMode, "audit retire payment mode");
            assertTrue(booleanColumn("select retired from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));
            assertEquals(
                    adminUserId,
                    intColumn("select retired_by from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));
            assertEquals(
                    "audit retire payment mode",
                    stringColumn("select retire_reason from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));
            assertNotNull(dateColumn(
                    "select date_retired from cashier_payment_mode where uuid = ?",
                    fixtures.paymentModeUuid()));
            paymentModeService.unretirePaymentMode(paymentModeService.getPaymentModeByUuid(fixtures.paymentModeUuid()));
            assertFalse(booleanColumn("select retired from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));
            assertNull(intColumn("select retired_by from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));
            assertNull(stringColumn("select retire_reason from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));
            assertNull(dateColumn("select date_retired from cashier_payment_mode where uuid = ?", fixtures.paymentModeUuid()));

            BillableService billableService =
                    billableServiceService.getBillableServiceByUuid(fixtures.billableServiceUuid());
            billableServiceService.retireBillableService(billableService, "audit retire billable service");
            assertTrue(booleanColumn(
                    "select retired from cashier_billable_service where uuid = ?",
                    fixtures.billableServiceUuid()));
            assertEquals(
                    adminUserId,
                    intColumn(
                            "select retired_by from cashier_billable_service where uuid = ?",
                            fixtures.billableServiceUuid()));
            assertEquals(
                    "audit retire billable service",
                    stringColumn(
                            "select retire_reason from cashier_billable_service where uuid = ?",
                            fixtures.billableServiceUuid()));
            assertNotNull(dateColumn(
                    "select date_retired from cashier_billable_service where uuid = ?",
                    fixtures.billableServiceUuid()));
            billableServiceService.unretireBillableService(
                    billableServiceService.getBillableServiceByUuid(fixtures.billableServiceUuid()));
            assertFalse(booleanColumn(
                    "select retired from cashier_billable_service where uuid = ?",
                    fixtures.billableServiceUuid()));
            assertNull(intColumn(
                    "select retired_by from cashier_billable_service where uuid = ?",
                    fixtures.billableServiceUuid()));
            assertNull(stringColumn(
                    "select retire_reason from cashier_billable_service where uuid = ?",
                    fixtures.billableServiceUuid()));
            assertNull(dateColumn(
                    "select date_retired from cashier_billable_service where uuid = ?",
                    fixtures.billableServiceUuid()));
        } finally {
            if (!authenticatedBeforeTest && Context.isSessionOpen()) {
                Context.logout();
            }
            if (openedSession) {
                Context.closeSession();
            }
        }
    }

    private BillingFixtures createBillingFixtures(Integer adminUserId, Integer patientId, Integer providerId) {
        String cashPointUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into cashier_cash_point "
                        + "(name, description, creator, date_created, retired, uuid) "
                        + "values (?, ?, ?, current_timestamp, false, ?)",
                "Audit cash point " + cashPointUuid,
                "test",
                adminUserId,
                cashPointUuid);
        Integer cashPointId = intColumn("select cash_point_id from cashier_cash_point where uuid = ?", cashPointUuid);

        String paymentModeUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into cashier_payment_mode "
                        + "(name, description, sort_order, creator, date_created, retired, uuid) "
                        + "values (?, ?, 1, ?, current_timestamp, false, ?)",
                "Audit payment mode " + paymentModeUuid,
                "test",
                adminUserId,
                paymentModeUuid);

        String billableServiceUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into cashier_billable_service "
                        + "(name, short_name, service_status, creator, date_created, retired, uuid) "
                        + "values (?, ?, 'ENABLED', ?, current_timestamp, false, ?)",
                "Audit billable service " + billableServiceUuid,
                "audit",
                adminUserId,
                billableServiceUuid);
        Integer billableServiceId =
                intColumn("select service_id from cashier_billable_service where uuid = ?", billableServiceUuid);

        Integer stockItemId = createBillingItem(adminUserId);

        String billUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into cashier_bill "
                        + "(receipt_number, provider_id, patient_id, cash_point_id, status, receipt_printed, "
                        + "creator, date_created, voided, uuid) "
                        + "values (?, ?, ?, ?, 'PENDING', false, ?, current_timestamp, false, ?)",
                "AUD-" + billUuid.substring(0, 8),
                providerId,
                patientId,
                cashPointId,
                adminUserId,
                billUuid);
        Integer billId = intColumn("select bill_id from cashier_bill where uuid = ?", billUuid);

        String lineItemUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into cashier_bill_line_item "
                        + "(bill_id, item_id, service_id, price, price_name, quantity, line_item_order, "
                        + "payment_status, creator, date_created, voided, uuid) "
                        + "values (?, ?, ?, 10.00, ?, 1, 0, 'PENDING', ?, current_timestamp, false, ?)",
                billId,
                stockItemId,
                billableServiceId,
                "audit",
                adminUserId,
                lineItemUuid);

        return new BillingFixtures(cashPointUuid, paymentModeUuid, billableServiceUuid, billUuid, lineItemUuid);
    }

    private Integer createBillingItem(Integer adminUserId) {
        Integer itemId = Math.max(
                nextId("stockmgmt_stock_item", "stock_item_id"),
                nextId("cashier_item", "item_id"));

        String departmentUuid = UUID.randomUUID().toString();
        Integer departmentId = nextId("cashier_department", "department_id");
        jdbcTemplate.update(
                "insert into cashier_department "
                        + "(department_id, name, description, creator, date_created, retired, uuid) "
                        + "values (?, ?, ?, ?, current_timestamp, false, ?)",
                departmentId,
                "Audit department " + departmentUuid,
                "test",
                adminUserId,
                departmentUuid);

        String cashierItemUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into cashier_item "
                        + "(item_id, name, description, department_id, creator, date_created, retired, uuid) "
                        + "values (?, ?, ?, ?, ?, current_timestamp, false, ?)",
                itemId,
                "Audit cashier item " + cashierItemUuid,
                "test",
                departmentId,
                adminUserId,
                cashierItemUuid);

        String stockItemUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into stockmgmt_stock_item "
                        + "(stock_item_id, has_expiration, common_name, is_drug, creator, date_created, voided, uuid) "
                        + "values (?, false, ?, false, ?, current_timestamp, false, ?)",
                itemId,
                "Audit stock item " + stockItemUuid,
                adminUserId,
                stockItemUuid);
        return itemId;
    }

    private Integer createPatient(Integer adminUserId) {
        Integer patientId = nextId("person", "person_id");
        String personUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into person "
                        + "(person_id, gender, birthdate, birthdate_estimated, dead, creator, date_created, voided, uuid) "
                        + "values (?, 'M', current_date, false, false, ?, current_timestamp, false, ?)",
                patientId,
                adminUserId,
                personUuid);
        jdbcTemplate.update(
                "insert into patient (patient_id, creator, date_created, voided, allergy_status) "
                        + "values (?, ?, current_timestamp, false, 'Unknown')",
                patientId,
                adminUserId);
        jdbcTemplate.update(
                "insert into person_name "
                        + "(person_id, preferred, given_name, family_name, creator, date_created, voided, uuid) "
                        + "values (?, true, ?, ?, ?, current_timestamp, false, ?)",
                patientId,
                "Billing",
                "Audit",
                adminUserId,
                UUID.randomUUID().toString());
        return patientId;
    }

    private Integer createProvider(Integer adminUserId) {
        Integer providerId = nextId("provider", "provider_id");
        String providerUuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "insert into provider "
                        + "(provider_id, name, identifier, creator, date_created, retired, uuid) "
                        + "values (?, ?, ?, ?, current_timestamp, false, ?)",
                providerId,
                "Billing Audit Provider",
                "AUD-" + providerUuid.substring(0, 8),
                adminUserId,
                providerUuid);
        return providerId;
    }

    private Integer nextId(String tableName, String columnName) {
        Integer nextValue = jdbcTemplate.queryForObject(
                "select coalesce(max(" + columnName + "), 0) + 1000 from " + tableName,
                Integer.class);
        assertNotNull(nextValue);
        return nextValue;
    }

    private Boolean booleanColumn(String sql, Object value) {
        return jdbcTemplate.queryForObject(sql, Boolean.class, value);
    }

    private Integer intColumn(String sql, Object value) {
        return jdbcTemplate.queryForObject(sql, Integer.class, value);
    }

    private String stringColumn(String sql, Object value) {
        return jdbcTemplate.queryForObject(sql, String.class, value);
    }

    private Date dateColumn(String sql, Object value) {
        return jdbcTemplate.queryForObject(sql, Date.class, value);
    }

    private record BillingFixtures(
            String cashPointUuid,
            String paymentModeUuid,
            String billableServiceUuid,
            String billUuid,
            String lineItemUuid) {}
}
