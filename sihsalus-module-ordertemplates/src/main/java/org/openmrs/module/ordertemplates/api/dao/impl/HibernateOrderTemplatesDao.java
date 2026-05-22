package org.openmrs.module.ordertemplates.api.dao.impl;

import java.util.List;
import org.hibernate.Session;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ordertemplates.api.dao.OrderTemplatesDao;
import org.openmrs.module.ordertemplates.model.OrderTemplate;
import org.openmrs.module.ordertemplates.parameter.OrderTemplateCriteria;

/**
 * Hibernate implementation of the OrderTemplatesDao
 * 
 * @author Arthur D. Mugume, Samuel Male [UCSF] date: 20/07/2022
 */
public class HibernateOrderTemplatesDao implements OrderTemplatesDao {
	
	private DbSessionFactory sessionFactory;
	
	@Override
	public OrderTemplate getOrderTemplate(Integer orderTemplateId) {
		return (OrderTemplate) sessionFactory.getCurrentSession().get(OrderTemplate.class, orderTemplateId);
	}
	
	@Override
	public OrderTemplate getOrderTemplateByUuid(String uuid) {
		return currentSession()
		        .createQuery("select ot from OrderTemplate ot where ot.uuid = :uuid", OrderTemplate.class)
		        .setParameter("uuid", uuid)
		        .uniqueResult();
	}
	
	@Override
	public List<OrderTemplate> getOrderTemplatesByDrug(Drug drug) {
		
		if (drug == null) {
			throw new IllegalArgumentException("Drug is required");
		}
		
		if (drug.getDrugId() != null) {
			return currentSession()
			        .createQuery(
			                "select ot from OrderTemplate ot where ot.drug = :drug order by ot.orderTemplateId desc",
			                OrderTemplate.class)
			        .setParameter("drug", drug)
			        .getResultList();
		}
		return List.of();
	}
	
	@Override
	public List<OrderTemplate> getOrderTemplatesByConcept(Concept concept) {
		
		if (concept == null) {
			throw new IllegalArgumentException("Concept is required");
		}
		
		if (concept.getConceptId() != null) {
			return currentSession()
			        .createQuery(
			                "select ot from OrderTemplate ot where ot.concept = :concept order by ot.orderTemplateId desc",
			                OrderTemplate.class)
			        .setParameter("concept", concept)
			        .getResultList();
		}
		return List.of();
	}
	
	@Override
	public List<OrderTemplate> getOrderTemplateByCriteria(OrderTemplateCriteria searchCriteria) {
		
		Concept concept = searchCriteria.getConcept();
		Drug drug = searchCriteria.getDrug();

		StringBuilder hql = new StringBuilder("select ot from OrderTemplate ot where 1 = 1");
		if (drug != null && drug.getDrugId() != null) {
			hql.append(" and ot.drug = :drug");
		}
		if (concept != null && concept.getConceptId() != null) {
			hql.append(" and ot.concept = :concept");
		}
		if (!searchCriteria.isIncludeRetired()) {
			hql.append(" and ot.retired = false");
		}
		hql.append(" order by ot.orderTemplateId desc");

		var query = currentSession().createQuery(hql.toString(), OrderTemplate.class);
		if (drug != null && drug.getDrugId() != null) {
			query.setParameter("drug", drug);
		}
		if (concept != null && concept.getConceptId() != null) {
			query.setParameter("concept", concept);
		}
		return query.getResultList();
	}
	
	@Override
	public List<OrderTemplate> getAllOrderTemplates(boolean includeRetired) {
		String hql = includeRetired
		        ? "select ot from OrderTemplate ot"
		        : "select ot from OrderTemplate ot where ot.retired = false";
		return currentSession().createQuery(hql, OrderTemplate.class).getResultList();
	}
	
	@Override
	public OrderTemplate saveOrderTemplate(OrderTemplate orderTemplate) {
		sessionFactory.getCurrentSession().merge(orderTemplate);
		return orderTemplate;
	}
	
	@Override
	public void deleteOrderTemplate(OrderTemplate orderTemplate) {
		sessionFactory.getCurrentSession().delete(orderTemplate);
	}
	
	public DbSessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	public void setSessionFactory(DbSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session currentSession() {
		return sessionFactory.getHibernateSessionFactory().getCurrentSession();
	}
}
