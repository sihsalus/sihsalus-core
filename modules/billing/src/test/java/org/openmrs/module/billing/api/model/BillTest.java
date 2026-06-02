package org.openmrs.module.billing.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class BillTest {

  @Test
  void getTotalPaymentsExcludesVoidedPaymentsFromTotal() {
    Bill bill = billWithPayments(payment("50", false), payment("30", false), payment("20", true));

    assertBigDecimalEquals("80", bill.getTotalPayments());
  }

  @Test
  void getTotalPaymentsReturnsZeroWhenAllPaymentsAreVoided() {
    Bill bill = billWithPayments(payment("100", true));

    assertBigDecimalEquals("0", bill.getTotalPayments());
  }

  @Test
  void getTotalExcludesVoidedLineItemsFromTotal() {
    Bill bill =
        billWithLineItems(
            lineItem("100", 2, false),
            lineItem("50", 1, false),
            lineItem("75", 3, true),
            lineItem("30", 2, true));

    assertBigDecimalEquals("250", bill.getTotal());
  }

  @Test
  void getTotalReturnsZeroWhenAllLineItemsAreVoided() {
    Bill bill = billWithLineItems(lineItem("100", 5, true));

    assertBigDecimalEquals("0", bill.getTotal());
  }

  @Test
  void synchronizeBillStatusUpdatesStatusToPaidWhenFullyPaid() {
    BillLineItem lineItem = lineItem("100", 1, false);
    Bill bill = billWithLineItems(lineItem);
    bill.setPayments(new HashSet<>());
    bill.getPayments().add(payment("100", false));

    bill.synchronizeBillStatus();

    assertEquals(BillStatus.PAID, bill.getStatus());
    assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
  }

  @Test
  void synchronizeBillStatusUpdatesStatusToPostedWhenPartiallyPaid() {
    Bill bill = billWithLineItems(lineItem("100", 1, false));
    bill.setPayments(new HashSet<>());
    bill.getPayments().add(payment("50", false));

    bill.synchronizeBillStatus();

    assertEquals(BillStatus.POSTED, bill.getStatus());
  }

  @Test
  void synchronizeBillStatusUpdatesStatusToPaidAfterVoidingLineItems() {
    BillLineItem lineItem1 = lineItem("100", 1, false);
    BillLineItem lineItem2 = lineItem("50", 1, false);
    Bill bill = billWithLineItems(lineItem1, lineItem2);
    bill.setPayments(new HashSet<>());
    bill.getPayments().add(payment("100", false));

    bill.synchronizeBillStatus();
    assertEquals(BillStatus.POSTED, bill.getStatus());

    lineItem2.setVoided(true);
    bill.synchronizeBillStatus();

    assertEquals(BillStatus.PAID, bill.getStatus());
    assertEquals(BillStatus.PAID, lineItem1.getPaymentStatus());
  }

  @Test
  void synchronizeBillStatusUpdatesAllNonVoidedLineItemsToPaidWhenBillIsFullyPaid() {
    BillLineItem lineItem1 = lineItem("50", 1, false);
    BillLineItem lineItem2 = lineItem("30", 1, false);
    BillLineItem voidedLineItem = lineItem("20", 1, true);
    Bill bill = billWithLineItems(lineItem1, lineItem2, voidedLineItem);
    bill.setPayments(new HashSet<>());
    bill.getPayments().add(payment("80", false));

    bill.synchronizeBillStatus();

    assertEquals(BillStatus.PAID, bill.getStatus());
    assertEquals(BillStatus.PAID, lineItem1.getPaymentStatus());
    assertEquals(BillStatus.PAID, lineItem2.getPaymentStatus());
    assertNull(voidedLineItem.getPaymentStatus());
  }

  @Test
  void synchronizeBillStatusFlipsToPaidWhenPaymentEqualsAmountAfterDiscount() {
    BillLineItem lineItem = lineItem("100", 1, false);
    Bill bill = billWithLineItems(lineItem);
    bill.setPayments(new HashSet<>());
    bill.setDiscounts(new HashSet<>());
    bill.getDiscounts().add(fixedDiscount("30", DiscountStatus.APPROVED, false));
    bill.getPayments().add(payment("70", false));

    bill.synchronizeBillStatus();

    assertEquals(BillStatus.PAID, bill.getStatus());
    assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
  }

  @Test
  void synchronizeBillStatusStaysPostedWhenPaymentBelowAmountAfterDiscount() {
    Bill bill = billWithLineItems(lineItem("100", 1, false));
    bill.setPayments(new HashSet<>());
    bill.setDiscounts(new HashSet<>());
    bill.getDiscounts().add(fixedDiscount("30", DiscountStatus.APPROVED, false));
    bill.getPayments().add(payment("60", false));

    bill.synchronizeBillStatus();

    assertEquals(BillStatus.POSTED, bill.getStatus());
  }

  @Test
  void getAmountAfterDiscountRecomputesPercentageDiscountWhenLineItemsChange() {
    Bill bill = billWithLineItems(lineItem("100", 1, false));
    bill.setDiscounts(new HashSet<>());
    bill.addDiscount(percentageDiscount("10", DiscountStatus.APPROVED, false));

    assertBigDecimalEquals("90", bill.getAmountAfterDiscount());

    bill.addLineItem(lineItem("50", 1, false));

    assertBigDecimalEquals("135.00", bill.getAmountAfterDiscount());
  }

  @Test
  void synchronizeBillStatusStaysPostedWhenDiscountExceedsCurrentTotal() {
    Bill bill = billWithLineItems(lineItem("100", 1, false));
    bill.setPayments(new HashSet<>());
    bill.setDiscounts(new HashSet<>());
    bill.getDiscounts().add(fixedDiscount("190", DiscountStatus.APPROVED, false));
    bill.getPayments().add(payment("0.01", false));

    bill.synchronizeBillStatus();

    assertEquals(BillStatus.POSTED, bill.getStatus());
  }

  @Test
  void getAmountAfterDiscountDoesNotApplyPendingDiscount() {
    Bill bill = billWithLineItems(lineItem("100", 1, false));
    bill.setDiscounts(new HashSet<>());
    bill.getDiscounts().add(fixedDiscount("30", DiscountStatus.PENDING, false));

    assertBigDecimalEquals("100", bill.getAmountAfterDiscount());
  }

  @Test
  void getAmountAfterDiscountDoesNotApplyRejectedDiscount() {
    Bill bill = billWithLineItems(lineItem("100", 1, false));
    bill.setDiscounts(new HashSet<>());
    bill.getDiscounts().add(fixedDiscount("30", DiscountStatus.REJECTED, false));

    assertBigDecimalEquals("100", bill.getAmountAfterDiscount());
  }

  @Test
  void setLineItemsAllowsSettingLineItemsOnNewBill() {
    Bill bill = new Bill();
    bill.setStatus(BillStatus.PENDING);
    ArrayList<BillLineItem> lineItems = new ArrayList<>();
    lineItems.add(lineItem("100", 1, false));

    bill.setLineItems(lineItems);

    assertEquals(1, bill.getLineItems().size());
  }

  private Bill billWithLineItems(BillLineItem... lineItems) {
    Bill bill = new Bill();
    bill.setLineItems(new ArrayList<>());
    for (BillLineItem lineItem : lineItems) {
      bill.getLineItems().add(lineItem);
    }
    return bill;
  }

  private Bill billWithPayments(Payment... payments) {
    Bill bill = new Bill();
    bill.setPayments(new HashSet<>());
    for (Payment payment : payments) {
      bill.getPayments().add(payment);
    }
    return bill;
  }

  private BillLineItem lineItem(String price, int quantity, boolean voided) {
    BillLineItem lineItem = new BillLineItem();
    lineItem.setPrice(new BigDecimal(price));
    lineItem.setQuantity(quantity);
    lineItem.setVoided(voided);
    return lineItem;
  }

  private Payment payment(String amountTendered, boolean voided) {
    Payment payment = new Payment();
    payment.setAmountTendered(new BigDecimal(amountTendered));
    payment.setVoided(voided);
    return payment;
  }

  private BillDiscount fixedDiscount(String value, DiscountStatus status, boolean voided) {
    BillDiscount discount = new BillDiscount();
    discount.setDiscountType(DiscountType.FIXED_AMOUNT);
    discount.setDiscountValue(new BigDecimal(value));
    discount.setStatus(status);
    discount.setVoided(voided);
    return discount;
  }

  private BillDiscount percentageDiscount(String value, DiscountStatus status, boolean voided) {
    BillDiscount discount = new BillDiscount();
    discount.setDiscountType(DiscountType.PERCENTAGE);
    discount.setDiscountValue(new BigDecimal(value));
    discount.setStatus(status);
    discount.setVoided(voided);
    return discount;
  }

  private void assertBigDecimalEquals(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual));
  }
}
