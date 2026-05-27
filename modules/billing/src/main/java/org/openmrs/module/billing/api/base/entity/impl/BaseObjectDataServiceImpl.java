/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.api.base.entity.impl;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.billing.api.base.PagingInfo;
import org.openmrs.module.billing.api.base.Utility;
import org.openmrs.module.billing.api.base.criteria.BillingCriteria;
import org.openmrs.module.billing.api.base.criteria.BillingOrder;
import org.openmrs.module.billing.api.base.criteria.BillingRestrictions;
import org.openmrs.module.billing.api.base.entity.IObjectDataService;
import org.openmrs.module.billing.api.base.entity.db.hibernate.BaseHibernateRepository;
import org.openmrs.module.billing.api.base.entity.security.IObjectAuthorizationPrivileges;
import org.openmrs.module.billing.api.base.f.Action1;
import org.openmrs.module.billing.api.base.util.PrivilegeUtil;
import org.springframework.transaction.annotation.Transactional;

/**
 * The base type for object services. Provides the core implementation for the common {@link
 * org.openmrs.OpenmrsObject} operations.
 *
 * @param <E> The {@link org.openmrs.OpenmrsObject} model type.
 */
@Transactional
public abstract class BaseObjectDataServiceImpl<
        E extends OpenmrsObject, P extends IObjectAuthorizationPrivileges>
    extends BaseOpenmrsService implements IObjectDataService<E> {

  private BaseHibernateRepository repository;

  private Class entityClass = null;

  protected abstract P getPrivileges();

  protected abstract void validate(E object);

  protected Collection<? extends OpenmrsObject> getRelatedObjects(E entity) {
    return null;
  }

  protected BillingOrder[] getDefaultSort() {
    return null;
  }

  public BaseHibernateRepository getRepository() {
    return this.repository;
  }

  public void setRepository(BaseHibernateRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public E save(E object) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getSavePrivilege())) {
      PrivilegeUtil.requirePrivileges(
          Context.getAuthenticatedUser(), privileges.getSavePrivilege());
    }
    if (object == null) {
      throw new NullPointerException("The object to save cannot be null.");
    }
    validate(object);
    return repository.save(object);
  }

  @Override
  @Transactional
  public E saveAll(E object, Collection<? extends OpenmrsObject> related) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getSavePrivilege())) {
      PrivilegeUtil.requirePrivileges(
          Context.getAuthenticatedUser(), privileges.getSavePrivilege());
    }
    if (object == null) {
      throw new NullPointerException("The object to save cannot be null.");
    }
    validate(object);
    Collection<OpenmrsObject> saveAll = new ArrayList<OpenmrsObject>();
    saveAll.add(object);
    saveAll(related);
    repository.saveAll(saveAll);
    return object;
  }

  @Override
  @Transactional
  public void saveAll(Collection<? extends OpenmrsObject> collection) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getSavePrivilege())) {
      PrivilegeUtil.requirePrivileges(
          Context.getAuthenticatedUser(), privileges.getSavePrivilege());
    }
    repository.saveAll(collection);
  }

  @Override
  @Transactional
  public void purge(E object) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getPurgePrivilege())) {
      PrivilegeUtil.requirePrivileges(
          Context.getAuthenticatedUser(), privileges.getPurgePrivilege());
    }
    if (object == null) {
      throw new NullPointerException("The object to purge cannot be null.");
    }
    repository.delete(object);
  }

  @Override
  @Transactional(readOnly = true)
  public List<E> getAll() {
    return getAll(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<E> getAll(PagingInfo pagingInfo) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getGetPrivilege())) {
      PrivilegeUtil.requirePrivileges(Context.getAuthenticatedUser(), privileges.getGetPrivilege());
    }
    return executeCriteria(getEntityClass(), pagingInfo, null, getDefaultSort());
  }

  @Override
  @Transactional(readOnly = true)
  public E getById(int entityId) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getGetPrivilege())) {
      PrivilegeUtil.requirePrivileges(Context.getAuthenticatedUser(), privileges.getGetPrivilege());
    }
    return repository.selectSingle(getEntityClass(), entityId);
  }

  @Override
  @Transactional(readOnly = true)
  public E getByUuid(String uuid) {
    P privileges = getPrivileges();
    if (privileges != null && !StringUtils.isEmpty(privileges.getGetPrivilege())) {
      PrivilegeUtil.requirePrivileges(Context.getAuthenticatedUser(), privileges.getGetPrivilege());
    }
    if (StringUtils.isEmpty(uuid)) {
      throw new IllegalArgumentException("The UUID must be defined.");
    }
    BillingCriteria criteria = repository.createCriteria(getEntityClass());
    criteria.add(BillingRestrictions.eq("uuid", uuid));
    return repository.selectSingle(getEntityClass(), criteria);
  }

  @SuppressWarnings("unchecked")
  protected Class<E> getEntityClass() {
    if (entityClass == null) {
      ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
      entityClass = (Class<E>) parameterizedType.getActualTypeArguments()[0];
    }
    return entityClass;
  }

  protected <T extends OpenmrsObject> List<T> executeCriteria(
      Class<T> clazz, Action1<BillingCriteria> action) {
    return executeCriteria(clazz, null, action, (BillingOrder[]) null);
  }

  protected <T extends OpenmrsObject> List<T> executeCriteria(
      Class<T> clazz, PagingInfo pagingInfo, Action1<BillingCriteria> action) {
    return executeCriteria(clazz, pagingInfo, action, (BillingOrder[]) null);
  }

  protected <T extends OpenmrsObject> List<T> executeCriteria(
      Class<T> clazz,
      PagingInfo pagingInfo,
      Action1<BillingCriteria> action,
      BillingOrder... orderBy) {
    BillingCriteria criteria = repository.createCriteria(clazz);

    if (action != null) {
      action.apply(criteria);
    }

    loadPagingTotal(pagingInfo, criteria);

    if (orderBy != null) {
      for (BillingOrder order : orderBy) {
        criteria.addOrder(order);
      }
    }

    return repository.select(clazz, createPagingCriteria(pagingInfo, criteria));
  }

  protected <T extends OpenmrsObject> List<T> executeOnRelatedObjects(
      Class<T> clazz, E entity, Action1<T> action) {
    List<T> updatedObjects = new ArrayList<T>();
    Collection<? extends OpenmrsObject> relatedObjects = getRelatedObjects(entity);
    if (relatedObjects != null && !relatedObjects.isEmpty()) {
      for (OpenmrsObject object : relatedObjects) {
        T data = Utility.as(clazz, object);
        if (data != null) {
          action.apply(data);
          updatedObjects.add(data);
        }
      }
    }
    return updatedObjects;
  }

  protected void loadPagingTotal(PagingInfo pagingInfo) {
    loadPagingTotal(pagingInfo, null);
  }

  protected void loadPagingTotal(PagingInfo pagingInfo, BillingCriteria criteria) {
    if (pagingInfo != null
        && pagingInfo.getPage() > 0
        && pagingInfo.getPageSize() > 0
        && pagingInfo.getLoadRecordCount()) {
      if (criteria == null) {
        criteria = repository.createCriteria(getEntityClass());
      }
      long count = criteria.count();
      pagingInfo.setTotalRecordCount(count);
      pagingInfo.setLoadRecordCount(false);
    }
  }

  protected BillingCriteria createPagingCriteria(PagingInfo pagingInfo) {
    return createPagingCriteria(pagingInfo, null);
  }

  protected BillingCriteria createPagingCriteria(PagingInfo pagingInfo, BillingCriteria criteria) {
    if (pagingInfo != null && pagingInfo.getPage() > 0 && pagingInfo.getPageSize() > 0) {
      if (criteria == null) {
        criteria = repository.createCriteria(getEntityClass());
      }
      criteria.setFirstResult((pagingInfo.getPage() - 1) * pagingInfo.getPageSize());
      criteria.setMaxResults(pagingInfo.getPageSize());
      criteria.setFetchSize(pagingInfo.getPageSize());
    }
    return criteria;
  }
}
