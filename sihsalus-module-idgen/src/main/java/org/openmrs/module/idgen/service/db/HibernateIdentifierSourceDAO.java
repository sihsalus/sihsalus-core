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
package org.openmrs.module.idgen.service.db;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.idgen.AutoGenerationOption;
import org.openmrs.module.idgen.EmptyIdentifierPoolException;
import org.openmrs.module.idgen.IdentifierPool;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.LogEntry;
import org.openmrs.module.idgen.PooledIdentifier;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *  Hibernate Implementation of the IdentifierSourceDAO Interface
 */
public class HibernateIdentifierSourceDAO implements IdentifierSourceDAO {
	
	protected Log log = LogFactory.getLog(getClass());
	
	//***** PROPERTIES *****
	
	private DbSessionFactory sessionFactory;
	
	//***** INSTANCE METHODS *****

	/** 
	 * @see IdentifierSourceService#getIdentifierSource(Integer)
	 */
	public IdentifierSource getIdentifierSource(Integer id) throws APIException {
		return (IdentifierSource) sessionFactory.getCurrentSession().get(IdentifierSource.class, id);
	}

	/** 
	 * @see IdentifierSourceDAO#getAllIdentifierSources(boolean)
	 */
	@SuppressWarnings("unchecked")
	public List<IdentifierSource> getAllIdentifierSources(boolean includeRetired) throws DAOException {
		String hql = "from IdentifierSource source";
		if (!includeRetired) {
			hql += " where source.retired = false";
		}
		hql += " order by source.name";
		return sessionFactory.getCurrentSession().createQuery(hql).getResultList();
	}

	/**
	 * @see IdentifierSourceService#saveIdentifierSource(IdentifierSource)
	 */
	public IdentifierSource saveIdentifierSource(IdentifierSource identifierSource) throws APIException {
		DbSession currentSession = sessionFactory.getCurrentSession();
		currentSession.saveOrUpdate(identifierSource);
		currentSession.flush();
		refreshIdentifierSource(identifierSource);
		return identifierSource;
	}

	/** 
	 * @see IdentifierSourceService#purgeIdentifierSource(IdentifierSource)
	 */
	public void purgeIdentifierSource(IdentifierSource identifierSource) {
		sessionFactory.getCurrentSession().delete(identifierSource);
	}
	
	/**
	 * 
	 * @see IdentifierSourceDAO#getAvailableIdentifiers(IdentifierPool, int)
	 */
	@SuppressWarnings("unchecked")
	public List<PooledIdentifier> getAvailableIdentifiers(IdentifierPool pool, int quantity) {
		Query query = sessionFactory.getCurrentSession()
		        .createQuery("from PooledIdentifier identifier where identifier.dateUsed is null and identifier.pool = :pool"
		                + (pool.isSequential() ? " order by identifier.identifier" : " order by identifier.uuid"));
		query.setParameter("pool", pool);
		query.setMaxResults(quantity);
		List<PooledIdentifier> results = query.getResultList();
		if (results.size() < quantity) {
			throw new EmptyIdentifierPoolException("Unable to retrieve " + quantity + " available identifiers from Pool " + pool + ".  Maybe you need to add more identifiers to your pool first.");
		}
		return results;
	}
	
	/**
	 * @see IdentifierSourceDAO#getQuantityInPool(IdentifierPool, boolean, boolean)
	 */
	public int getQuantityInPool(IdentifierPool pool, boolean availableOnly, boolean usedOnly) {
		String hql = "select count(identifier) from PooledIdentifier identifier where identifier.pool = :pool";
		if (availableOnly) {
			hql += " and identifier.dateUsed is null";
		}
		if (usedOnly) {
			hql += " and identifier.dateUsed is not null";
		}
		Query query = sessionFactory.getCurrentSession().createQuery(hql);
		query.setParameter("pool", pool);
		return ((Number) query.getSingleResult()).intValue();
	}

    /**
     * @see IdentifierSourceDAO#getAutoGenerationOption(Integer)
     */
    @Override
    public AutoGenerationOption getAutoGenerationOption(Integer autoGenerationOptionId) throws DAOException {
        Query query = sessionFactory.getCurrentSession()
                .createQuery("from AutoGenerationOption option where option.id = :id");
        query.setParameter("id", autoGenerationOptionId);
        return uniqueResult(query);
    }

    /**
	 * @see IdentifierSourceService#getAutoGenerationOptionByUuid(String)
	 */
    @Override
	public AutoGenerationOption getAutoGenerationOptionByUuid(String uuid) {
        Query query = sessionFactory.getCurrentSession()
                .createQuery("from AutoGenerationOption option where option.uuid = :uuid");
        query.setParameter("uuid", uuid);
        return uniqueResult(query);
	}
    
    /**
	 * @see IdentifierSourceDAO#getAutoGenerationOption(PatientIdentifierType,Location)
	 */
	public AutoGenerationOption getAutoGenerationOption(PatientIdentifierType type, Location location) throws APIException {
		Query query = sessionFactory.getCurrentSession()
		        .createQuery("from AutoGenerationOption option where option.identifierType = :type"
		                + " and (option.location = :location or option.location is null)");
		query.setParameter("type", type);
		query.setParameter("location", location);
		return uniqueResult(query);
	}

    /**
     * @see IdentifierSourceDAO#getAutoGenerationOption(PatientIdentifierType)
     */
    public List<AutoGenerationOption> getAutoGenerationOptions(PatientIdentifierType type) throws APIException {
        Query query = sessionFactory.getCurrentSession()
                .createQuery("from AutoGenerationOption option where option.identifierType = :type");
        query.setParameter("type", type);
        return query.getResultList();
    }

    /**
     * @see IdentifierSourceDAO#getAutoGenerationOption(PatientIdentifierType)
     */
    public AutoGenerationOption getAutoGenerationOption(PatientIdentifierType type) throws APIException {
        Query query = sessionFactory.getCurrentSession()
                .createQuery("from AutoGenerationOption option where option.identifierType = :type");
        query.setParameter("type", type);
        return uniqueResult(query);
    }

	/** 
	 * @see IdentifierSourceDAO#saveAutoGenerationOption(AutoGenerationOption)
	 */
	public AutoGenerationOption saveAutoGenerationOption(AutoGenerationOption option) throws APIException {
		sessionFactory.getCurrentSession().saveOrUpdate(option);
		return option;
	}

	/** 
	 * @see IdentifierSourceDAO#purgeAutoGenerationOption(AutoGenerationOption)
	 */
	public void purgeAutoGenerationOption(AutoGenerationOption option) throws APIException {
		sessionFactory.getCurrentSession().delete(option);
	}

	/** 
	 * @see IdentifierSourceDAO#getLogEntries(IdentifierSource, Date, Date, String, User, String)
	 */
	@SuppressWarnings("unchecked")
	public List<LogEntry> getLogEntries(IdentifierSource source, Date fromDate, Date toDate, 
										String identifier, User generatedBy, String comment) throws DAOException {
		StringBuilder hql = new StringBuilder("from LogEntry entry where 1 = 1");
		if (source != null) {
			hql.append(" and entry.source = :source");
		}
		if (fromDate != null) {
			Calendar c = Calendar.getInstance();
			c.setTime(fromDate);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			hql.append(" and entry.dateGenerated >= :fromDate");
		}
		if (toDate != null) {
			Calendar c = Calendar.getInstance();
			c.setTime(toDate);
			c.add(Calendar.DATE, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			hql.append(" and entry.dateGenerated < :toDate");
		}
		if (identifier != null) {
			hql.append(" and entry.identifier like :identifier");
		}	
		if (generatedBy != null) {
			hql.append(" and entry.generatedBy = :generatedBy");
		}
		if (comment != null) {
			hql.append(" and entry.comment like :comment");
		}	
		hql.append(" order by entry.dateGenerated desc");
		Query query = sessionFactory.getCurrentSession().createQuery(hql.toString());
		if (source != null) {
			query.setParameter("source", source);
		}
		if (fromDate != null) {
			query.setParameter("fromDate", fromDate);
		}
		if (toDate != null) {
			Calendar c = Calendar.getInstance();
			c.setTime(toDate);
			c.add(Calendar.DATE, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			query.setParameter("toDate", c.getTime());
		}
		if (identifier != null) {
			query.setParameter("identifier", "%" + identifier + "%");
		}
		if (generatedBy != null) {
			query.setParameter("generatedBy", generatedBy);
		}
		if (comment != null) {
			query.setParameter("comment", "%" + comment + "%");
		}
		return query.getResultList();
	}

	/**
	 * @see IdentifierSourceDAO#getLogEntries(IdentifierSource, Date, Date, String, User, String)
	 */
	@SuppressWarnings("unchecked")
	public LogEntry getMostRecentLogEntry(IdentifierSource source) throws DAOException {
		if (source == null) {
			throw new DAOException("You must specify the Identifier Source that you wish to query");
		}
		Query query = sessionFactory.getCurrentSession()
		        .createQuery("from LogEntry entry where entry.source = :source"
		                + " order by entry.dateGenerated desc, entry.id desc");
		query.setParameter("source", source);
		query.setMaxResults(1);
		List<LogEntry> entries = query.getResultList();
		return entries.isEmpty() ? null : entries.get(0);
	}

    /**
     * @see IdentifierSourceDAO#getIdentifierSourceByUuid(String)
     */
    @Override
    public IdentifierSource getIdentifierSourceByUuid(String uuid) {
        Query query = sessionFactory.getCurrentSession()
                .createQuery("from IdentifierSource source where source.uuid = :uuid");
        query.setParameter("uuid", uuid);
        return uniqueResult(query);
    }
    
    /**
     * @see IdentifierSourceDAO#getIdentifierSourcesByType(PatientIdentifierType)
     */
    @Override
    public List<IdentifierSource> getIdentifierSourcesByType(PatientIdentifierType patientIdentifierType) {
        Query query = sessionFactory.getCurrentSession()
                .createQuery("from IdentifierSource source where source.identifierType = :type and source.retired = false");
        query.setParameter("type", patientIdentifierType);
        return query.getResultList();
    }    

    /**
	 * @see org.openmrs.module.idgen.service.db.IdentifierSourceDAO#saveLogEntry(LogEntry)
	 */
	public LogEntry saveLogEntry(LogEntry logEntry) throws DAOException {
		sessionFactory.getCurrentSession().saveOrUpdate(logEntry);
		return logEntry;
	}

    /**
     * @see IdentifierSourceDAO#saveSequenceValue(org.openmrs.module.idgen.SequentialIdentifierGenerator, long)
     */
    @Override
    public void saveSequenceValue(SequentialIdentifierGenerator generator, long sequenceValue) {
        int updated = sessionFactory.getHibernateSessionFactory()
                .getCurrentSession()
                .createNativeMutationQuery("update idgen_seq_id_gen set next_sequence_value = :val where id = :id")
                .setParameter("val", sequenceValue)
                .setParameter("id", generator.getId())
                .executeUpdate();
        if (updated != 1) {
            throw new APIException("Expected to update 1 row but updated " + updated + " rows instead!");
        }
    }

    /**
     * @see IdentifierSourceDAO#getSequenceValue(org.openmrs.module.idgen.SequentialIdentifierGenerator)
     */
    @Override
    public Long getSequenceValue(SequentialIdentifierGenerator generator) {
        Number val = (Number) sessionFactory.getCurrentSession()
                .createSQLQuery("select next_sequence_value from idgen_seq_id_gen where id = :id", Number.class)
                .setParameter("id", generator.getId())
                .uniqueResult();
        return val == null ? null : val.longValue();
	}


    public void refreshIdentifierSource(IdentifierSource source) {
		sessionFactory.getCurrentSession().refresh(source);
    }


	//***** PROPERTY ACCESS *****

	/**
	 * @return the sessionFactory
	 */
	public DbSessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * @param sessionFactory the sessionFactory to set
	 */
	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@SuppressWarnings("unchecked")
	private <T> T uniqueResult(Query query) {
		try {
			return (T) query.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}
}
