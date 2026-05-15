/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.metadatamapping.api.db.hibernate;

import jakarta.persistence.Query;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.openmrs.Concept;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.metadatamapping.MetadataSet;
import org.openmrs.module.metadatamapping.MetadataSetMember;
import org.openmrs.module.metadatamapping.MetadataSource;
import org.openmrs.module.metadatamapping.MetadataTermMapping;
import org.openmrs.module.metadatamapping.RetiredHandlingMode;
import org.openmrs.module.metadatamapping.api.MetadataSetSearchCriteria;
import org.openmrs.module.metadatamapping.api.MetadataSourceSearchCriteria;
import org.openmrs.module.metadatamapping.api.MetadataTermMappingSearchCriteria;
import org.openmrs.module.metadatamapping.api.db.MetadataMappingDAO;
import org.openmrs.module.metadatamapping.api.exception.InvalidMetadataTypeException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate DAO implementation backed by HQL so the static module works with Hibernate 7.
 */
public class HibernateMetadataMappingDAO implements MetadataMappingDAO {

    private DbSessionFactory sessionFactory;

    public void setSessionFactory(DbSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public DbSession getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * @see MetadataMappingDAO#getConcepts(int, int)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Concept> getConcepts(final int firstResult, final int maxResults) {
        Query query = getCurrentSession().createQuery("from Concept c order by c.conceptId");
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);
        return list(query);
    }

    @Override
    public MetadataSource saveMetadataSource(MetadataSource metadataSource) {
        return (MetadataSource) getCurrentSession().merge(metadataSource);
    }

    @Override
    public List<MetadataSource> getMetadataSources(MetadataSourceSearchCriteria searchCriteria) {
        StringBuilder hql = new StringBuilder("from MetadataSource source where 1 = 1");
        if (!searchCriteria.isIncludeAll()) {
            hql.append(" and source.retired = false");
        }
        if (searchCriteria.getSourceName() != null) {
            hql.append(" and source.name = :sourceName");
        }
        hql.append(" order by source.name asc, source.id asc");

        Query query = getCurrentSession().createQuery(hql.toString());
        if (searchCriteria.getSourceName() != null) {
            query.setParameter("sourceName", searchCriteria.getSourceName());
        }
        applyPaging(query, searchCriteria.getFirstResult(), searchCriteria.getMaxResults());
        return list(query);
    }

    @Override
    public MetadataSource getMetadataSource(Integer metadataSourceId) {
        return (MetadataSource) getCurrentSession().get(MetadataSource.class, metadataSourceId);
    }

    @Override
    public MetadataSource getMetadataSourceByName(String metadataSourceName) {
        Query query = getCurrentSession().createQuery("from MetadataSource source where source.name = :name");
        query.setParameter("name", metadataSourceName);
        return uniqueResult(query);
    }

    @Override
    public MetadataTermMapping saveMetadataTermMapping(MetadataTermMapping metadataTermMapping) {
        return internalSaveMetadataTermMapping(metadataTermMapping);
    }

    @Override
    public Collection<MetadataTermMapping> saveMetadataTermMappings(Collection<MetadataTermMapping> metadataTermMappings) {
        for (MetadataTermMapping metadataTermMapping : metadataTermMappings) {
            internalSaveMetadataTermMapping(metadataTermMapping);
        }
        return metadataTermMappings;
    }

    @Override
    public MetadataTermMapping getMetadataTermMapping(Integer metadataTermMappingId) {
        return (MetadataTermMapping) getCurrentSession().get(MetadataTermMapping.class, metadataTermMappingId);
    }

    @Override
    public <T extends OpenmrsObject> T getByUuid(Class<T> openmrsObjectClass, String uuid) {
        return internalGetByUuid(openmrsObjectClass, uuid);
    }

    @Override
    public List<MetadataTermMapping> getMetadataTermMappings(MetadataTermMappingSearchCriteria searchCriteria) {
        StringBuilder hql = new StringBuilder("from MetadataTermMapping mapping where 1 = 1");

        if (searchCriteria.getReferredObject() != null) {
            hql.append(" and mapping.metadataUuid = :referredUuid");
            hql.append(" and mapping.metadataClass = :referredClass");
        }
        if (searchCriteria.getMetadataUuid() != null) {
            hql.append(" and mapping.metadataUuid = :metadataUuid");
        }
        if (searchCriteria.getMetadataClass() != null) {
            hql.append(" and mapping.metadataClass = :metadataClass");
        }
        if (!searchCriteria.isIncludeAll()) {
            hql.append(" and mapping.retired = false");
        }
        if (searchCriteria.getMapped() != null) {
            hql.append(searchCriteria.getMapped()
                    ? " and mapping.metadataUuid is not null"
                    : " and mapping.metadataUuid is null");
        }
        if (searchCriteria.getMetadataSource() != null) {
            hql.append(" and mapping.metadataSource = :metadataSource");
        }
        if (searchCriteria.getMetadataTermCode() != null) {
            hql.append(" and mapping.code = :code");
        }
        if (searchCriteria.getMetadataTermName() != null) {
            hql.append(" and mapping.name = :name");
        }
        hql.append(" order by mapping.metadataSource asc, mapping.metadataTermMappingId asc");

        Query query = getCurrentSession().createQuery(hql.toString());
        if (searchCriteria.getReferredObject() != null) {
            query.setParameter("referredUuid", searchCriteria.getReferredObject().getUuid());
            query.setParameter("referredClass", searchCriteria.getReferredObject().getClass().getCanonicalName());
        }
        if (searchCriteria.getMetadataUuid() != null) {
            query.setParameter("metadataUuid", searchCriteria.getMetadataUuid());
        }
        if (searchCriteria.getMetadataClass() != null) {
            query.setParameter("metadataClass", searchCriteria.getMetadataClass());
        }
        if (searchCriteria.getMetadataSource() != null) {
            query.setParameter("metadataSource", searchCriteria.getMetadataSource());
        }
        if (searchCriteria.getMetadataTermCode() != null) {
            query.setParameter("code", searchCriteria.getMetadataTermCode());
        }
        if (searchCriteria.getMetadataTermName() != null) {
            query.setParameter("name", searchCriteria.getMetadataTermName());
        }
        applyPaging(query, searchCriteria.getFirstResult(), searchCriteria.getMaxResults());
        return list(query);
    }

    @Override
    public MetadataTermMapping getMetadataTermMapping(MetadataSource metadataSource, String metadataTermCode) {
        Query query = getCurrentSession().createQuery(
                "from MetadataTermMapping mapping where mapping.metadataSource = :metadataSource and mapping.code = :code");
        query.setParameter("metadataSource", metadataSource);
        query.setParameter("code", metadataTermCode);
        return uniqueResult(query);
    }

    @Override
    public <T extends OpenmrsMetadata> T getMetadataItem(
            Class<T> type, String metadataSourceName, String metadataTermCode) {
        MetadataTermMapping metadataTermMapping =
                getSourceMetadataTerm(metadataSourceName, null, metadataTermCode);

        T metadataItem = null;
        if (metadataTermMapping != null) {
            if (!type.getCanonicalName().equals(metadataTermMapping.getMetadataClass())) {
                throw new InvalidMetadataTypeException("requested type " + type + " of metadata term mapping "
                        + metadataTermMapping.getUuid() + " refers to type " + metadataTermMapping.getMetadataClass());
            }
            metadataItem = internalGetByUuid(type, metadataTermMapping.getMetadataUuid());
        }
        return metadataItem;
    }

    @Override
    public <T extends OpenmrsMetadata> List<T> getMetadataItems(Class<T> type, String metadataSourceName) {
        List<T> metadataItems = new LinkedList<>();
        for (MetadataTermMapping metadataTermMapping :
                getSourceMetadataTerms(metadataSourceName, type, null, null, null)) {
            T metadataItem = internalGetByUuid(type, metadataTermMapping.getMetadataUuid());
            if (metadataItem != null) {
                metadataItems.add(metadataItem);
            }
        }
        return metadataItems;
    }

    @Override
    public MetadataSet saveMetadataSet(MetadataSet metadataSet) {
        return (MetadataSet) sessionFactory.getCurrentSession().merge(metadataSet);
    }

    @Override
    public MetadataSet getMetadataSet(Integer metadataSetId) {
        return (MetadataSet) sessionFactory.getCurrentSession().get(MetadataSet.class, metadataSetId);
    }

    @Override
    public List<MetadataSet> getMetadataSet(MetadataSetSearchCriteria searchCriteria) {
        String hql = searchCriteria.isIncludeAll()
                ? "from MetadataSet metadataSet"
                : "from MetadataSet metadataSet where metadataSet.retired = false";
        Query query = getCurrentSession().createQuery(hql);
        applyPaging(query, searchCriteria.getFirstResult(), searchCriteria.getMaxResults());
        return list(query);
    }

    @Override
    public MetadataSet getMetadataSetByUuid(String metadataSetUuid) {
        return internalGetByUuid(MetadataSet.class, metadataSetUuid);
    }

    @Override
    public MetadataSetMember saveMetadataSetMember(MetadataSetMember metadataSetMember) {
        return internalSaveMetadataSetMember(metadataSetMember);
    }

    @Override
    public Collection<MetadataSetMember> saveMetadataSetMembers(Collection<MetadataSetMember> metadataSetMembers) {
        for (MetadataSetMember metadataSetMember : metadataSetMembers) {
            internalSaveMetadataSetMember(metadataSetMember);
        }
        return metadataSetMembers;
    }

    @Override
    public MetadataSetMember getMetadataSetMember(Integer metadataSetMemberId) {
        return (MetadataSetMember) sessionFactory.getCurrentSession().get(MetadataSetMember.class, metadataSetMemberId);
    }

    @Override
    public List<MetadataSetMember> getMetadataSetMembers(
            MetadataSet metadataSet, Integer firstResult, Integer maxResults, RetiredHandlingMode retiredHandlingMode) {
        StringBuilder hql = new StringBuilder("from MetadataSetMember member where member.metadataSet = :metadataSet");
        if (RetiredHandlingMode.ONLY_ACTIVE.equals(retiredHandlingMode)) {
            hql.append(" and member.retired = false");
        }
        hql.append(" order by member.sortWeight desc");

        Query query = sessionFactory.getCurrentSession().createQuery(hql.toString());
        query.setParameter("metadataSet", metadataSet);
        applyPaging(query, firstResult, maxResults);
        return list(query);
    }

    @Override
    public List<MetadataSetMember> getMetadataSetMembers(
            String metadataSetUuid, Integer firstResult, Integer maxResults, RetiredHandlingMode retiredHandlingMode) {
        MetadataSet metadataSet = getMetadataSetByUuid(metadataSetUuid);
        return getMetadataSetMembers(metadataSet, firstResult, maxResults, retiredHandlingMode);
    }

    @Override
    public <T extends OpenmrsMetadata> List<T> getMetadataSetItems(
            Class<T> type, MetadataSet metadataSet, Integer firstResult, Integer maxResults) {
        return internalGetMetadataSetItems(type, metadataSet, firstResult, maxResults);
    }

    @Override
    public <T extends OpenmrsMetadata> List<T> getMetadataSetItems(Class<T> type, MetadataSet metadataSet) {
        return internalGetMetadataSetItems(type, metadataSet, null, null);
    }

    private MetadataTermMapping internalSaveMetadataTermMapping(MetadataTermMapping metadataTermMapping) {
        return (MetadataTermMapping) getCurrentSession().merge(metadataTermMapping);
    }

    private MetadataSetMember internalSaveMetadataSetMember(MetadataSetMember metadataSetMember) {
        return (MetadataSetMember) sessionFactory.getCurrentSession().merge(metadataSetMember);
    }

    private <T extends OpenmrsObject> T internalGetByUuid(Class<T> openmrsObjectClass, String uuid) {
        Query query =
                getCurrentSession().createQuery("from " + openmrsObjectClass.getName() + " object where object.uuid = :uuid");
        query.setParameter("uuid", uuid);
        return uniqueResult(query);
    }

    private MetadataTermMapping getSourceMetadataTerm(
            String metadataSourceName, Class<?> metadataClass, String metadataTermCode) {
        return uniqueResult(getSourceMetadataTermsQuery(metadataSourceName, metadataClass, metadataTermCode, null, null));
    }

    private List<MetadataTermMapping> getSourceMetadataTerms(
            String metadataSourceName,
            Class<?> metadataClass,
            String metadataTermCode,
            Integer firstResult,
            Integer maxResults) {
        return list(getSourceMetadataTermsQuery(metadataSourceName, metadataClass, metadataTermCode, firstResult, maxResults));
    }

    private Query getSourceMetadataTermsQuery(
            String metadataSourceName, Class<?> metadataClass, String metadataTermCode, Integer firstResult, Integer maxResults) {
        StringBuilder hql = new StringBuilder(
                "from MetadataTermMapping mapping where mapping.retired = false and mapping.metadataSource.name = :sourceName");
        if (metadataClass != null) {
            hql.append(" and mapping.metadataClass = :metadataClass");
        }
        if (metadataTermCode != null) {
            hql.append(" and mapping.code = :code");
        }

        Query query = getCurrentSession().createQuery(hql.toString());
        query.setParameter("sourceName", metadataSourceName);
        if (metadataClass != null) {
            query.setParameter("metadataClass", metadataClass.getCanonicalName());
        }
        if (metadataTermCode != null) {
            query.setParameter("code", metadataTermCode);
        }
        applyPaging(query, firstResult, maxResults);
        return query;
    }

    private <T extends OpenmrsMetadata> List<T> internalGetMetadataSetItems(
            Class<T> type, MetadataSet metadataSet, Integer firstResult, Integer maxResults) {
        if (metadataSet == null) {
            throw new IllegalArgumentException("To obtain MetadataSet items, reference to MetadataSet must be given");
        }

        Query query = sessionFactory.getCurrentSession().createQuery(
                "select item from "
                        + type.getName()
                        + " item, MetadataSetMember member"
                        + " where item.uuid = member.metadataUuid"
                        + " and item.retired = false"
                        + " and member.retired = false"
                        + " and member.metadataSet = :metadataSet"
                        + " order by member.sortWeight desc");
        query.setParameter("metadataSet", metadataSet);
        applyPaging(query, firstResult, maxResults);
        return list(query);
    }

    private void applyPaging(Query query, Integer firstResult, Integer maxResults) {
        if (firstResult != null) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(Query query) {
        return query.getResultList();
    }

    private <T> T uniqueResult(Query query) {
        List<T> results = list(query);
        return results.isEmpty() ? null : results.get(0);
    }
}
