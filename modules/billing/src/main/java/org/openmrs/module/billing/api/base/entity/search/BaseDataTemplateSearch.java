/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.entity.search;

import org.openmrs.OpenmrsData;
import org.openmrs.module.billing.api.base.criteria.BillingCriteria;
import org.openmrs.module.billing.api.base.criteria.BillingRestrictions;

/**
 * Base template search class for {@link org.openmrs.OpenmrsData} models.
 *
 * @param <T> The model class.
 */
public class BaseDataTemplateSearch<T extends OpenmrsData> extends BaseAuditableTemplateSearch<T> {

  public static final long serialVersionUID = 0L;

  private DateComparisonType dateVoidedComparisonType;

  private StringComparisonType voidReasonComparisonType;

  private Boolean includeVoided;

  public BaseDataTemplateSearch(T template) {
    this(template, null);
  }

  public BaseDataTemplateSearch(T template, Boolean includeVoided) {
    super(template);
    this.includeVoided = includeVoided;
    this.dateVoidedComparisonType = DateComparisonType.EQUAL;
    this.voidReasonComparisonType = StringComparisonType.EQUAL;
  }

  public DateComparisonType getDateVoidedComparisonType() {
    return dateVoidedComparisonType;
  }

  public void setDateVoidedComparisonType(DateComparisonType dateVoidedComparisonType) {
    this.dateVoidedComparisonType = dateVoidedComparisonType;
  }

  public StringComparisonType getVoidReasonComparisonType() {
    return voidReasonComparisonType;
  }

  public void setVoidReasonComparisonType(StringComparisonType voidReasonComparisonType) {
    this.voidReasonComparisonType = voidReasonComparisonType;
  }

  public boolean getIncludeVoided() {
    return includeVoided;
  }

  public void setIncludeVoided(boolean includeVoided) {
    this.includeVoided = includeVoided;
  }

  @Override
  public void updateCriteria(BillingCriteria criteria) {
    super.updateCriteria(criteria);

    T t = getTemplate();

    if (includeVoided != null) {
      if (!includeVoided) {
        criteria.add(BillingRestrictions.eq("voided", false));
      }
    } else if (t.getVoided() != null) {
      criteria.add(BillingRestrictions.eq("voided", t.getVoided()));
    }

    if (t.getVoidedBy() != null) {
      criteria.add(BillingRestrictions.eq("voidedBy", t.getVoidedBy()));
    }
    if (t.getDateVoided() != null) {
      criteria.add(
          (cb, root) ->
              createPredicate(cb, root, "dateVoided", t.getDateVoided(), dateVoidedComparisonType));
    }
    if (t.getVoidReason() != null) {
      criteria.add(
          (cb, root) ->
              createPredicate(cb, root, "voidReason", t.getVoidReason(), voidReasonComparisonType));
    }
  }
}
