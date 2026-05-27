/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.web.rest.resource;

import java.math.BigDecimal;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.api.BillLineItemService;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.CashierItemPriceService;
import org.openmrs.module.billing.api.base.entity.IEntityDataService;
import org.openmrs.module.billing.api.model.BillLineItem;
import org.openmrs.module.billing.api.model.BillableService;
import org.openmrs.module.billing.api.model.CashierItemPrice;
import org.openmrs.module.billing.web.base.resource.BaseRestDataResource;
import org.openmrs.module.billing.web.rest.controller.base.CashierResourceController;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;

/** REST resource representing a {@link BillLineItem}. */
@Resource(
    name = RestConstants.VERSION_1 + CashierResourceController.BILLING_NAMESPACE + "/billLineItem",
    supportedClass = BillLineItem.class,
    supportedOpenmrsVersions = {"2.0 - 9.*"})
@Slf4j
public class BillLineItemResource extends BaseRestDataResource<BillLineItem> {

  @Override
  protected org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging<BillLineItem>
      doGetAll(RequestContext context) {
    return new org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging<>(
        Collections.emptyList(), context);
  }

  @Override
  public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
    DelegatingResourceDescription description = super.getRepresentationDescription(rep);
    if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
      description.addProperty("item");
      description.addProperty("billableService", Representation.REF);
      description.addProperty("quantity");
      description.addProperty("price");
      description.addProperty("priceName");
      description.addProperty("priceUuid");
      description.addProperty("lineItemOrder");
      description.addProperty("paymentStatus");
      return description;
    }
    return null;
  }

  @PropertySetter(value = "item")
  public void setItem(BillLineItem instance, Object item) {
    if (item == null) {
      instance.setItem(null);
      return;
    }
    if (!(item instanceof String)) {
      throw new ConversionException("Stock item must be referenced by uuid");
    }
    StockManagementService service = Context.getService(StockManagementService.class);
    String itemUuid = (String) item;
    StockItem stockItem = service.getStockItemByUuid(itemUuid);
    if (stockItem == null) {
      throw new ObjectNotFoundException("No stock item found with uuid: " + itemUuid);
    }
    instance.setItem(stockItem);
  }

  @PropertySetter(value = "billableService")
  public void setBillableService(BillLineItem instance, Object item) {
    if (item == null) {
      instance.setBillableService(null);
      return;
    }
    if (!(item instanceof String)) {
      throw new ConversionException("Billable service must be referenced by uuid");
    }
    BillableServiceService service = Context.getService(BillableServiceService.class);
    String serviceUuid = (String) item;
    BillableService billableService = service.getBillableServiceByUuid(serviceUuid);
    if (billableService == null) {
      throw new ObjectNotFoundException("No billable service found with uuid: " + serviceUuid);
    }
    instance.setBillableService(billableService);
  }

  @PropertyGetter(value = "item")
  public String getItem(BillLineItem instance) {
    try {
      StockItem stockItem = instance.getItem();
      return stockItem.getDrug().getName();
    } catch (Exception e) {
      return "";
    }
  }

  @PropertyGetter(value = "billableService")
  public String getBillableService(BillLineItem instance) {
    try {
      BillableService service = instance.getBillableService();
      return service.getName();
    } catch (Exception e) {
      return "";
    }
  }

  @PropertySetter(value = "price")
  public void setPriceValue(BillLineItem instance, Object price) {
    instance.setPrice(toBigDecimal(price));
  }

  @PropertySetter(value = "priceName")
  public void setPriceName(BillLineItem instance, String name) {
    instance.setPriceName(name);
  }

  @PropertyGetter(value = "priceName")
  public String getPriceName(BillLineItem instance) {
    String itemName = instance.getPriceName();
    return StringUtils.isNotBlank(itemName) ? itemName : "";
  }

  @PropertySetter(value = "priceUuid")
  public void setItemPrice(BillLineItem instance, String uuid) {
    if (StringUtils.isBlank(uuid)) {
      instance.setItemPrice(null);
      instance.setPriceName("");
      return;
    }
    CashierItemPrice itemPrice =
        Context.getService(CashierItemPriceService.class).getCashierItemPriceByUuid(uuid);
    if (itemPrice != null) {
      instance.setItemPrice(itemPrice);
      instance.setPriceName("");
    } else {
      throw new ObjectNotFoundException("No cashier item price found with uuid: " + uuid);
    }
  }

  @PropertyGetter(value = "priceUuid")
  public String getItemPriceUuid(BillLineItem instance) {
    try {
      CashierItemPrice itemPrice = instance.getItemPrice();
      return itemPrice == null ? "" : itemPrice.getUuid();
    } catch (Exception e) {
      log.warn("Price probably was deleted", e);
      return "";
    }
  }

  @Override
  public BillLineItem getByUniqueId(String uuid) {
    if (StringUtils.isEmpty(uuid)) {
      return null;
    }

    return Context.getService(BillLineItemService.class).getBillLineItemByUuid(uuid);
  }

  @Override
  public BillLineItem newDelegate() {
    return new BillLineItem();
  }

  @Override
  public Class<IEntityDataService<BillLineItem>> getServiceClass() {
    // BillLineItemService doesn't implement IEntityDataService, so return null
    // Line items are managed through BillService, not directly
    return null;
  }

  @Override
  public void delete(BillLineItem lineItem, String reason, RequestContext context) {
    if (StringUtils.isBlank(reason)) {
      throw new ConversionException("Reason is required");
    }

    Context.getService(BillLineItemService.class).voidBillLineItem(lineItem, reason);
  }

  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    }
    try {
      return new BigDecimal(value.toString());
    } catch (NumberFormatException e) {
      throw new ConversionException("Cannot convert '" + value + "' to BigDecimal", e);
    }
  }
}
