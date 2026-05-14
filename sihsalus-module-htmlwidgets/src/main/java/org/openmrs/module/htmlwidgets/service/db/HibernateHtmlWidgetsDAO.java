package org.openmrs.module.htmlwidgets.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openmrs.api.db.hibernate.DbSessionFactory;  
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsService;

/**
 * Hibernate Implementation of the HtmlWidgetsDAO
 */
public class HibernateHtmlWidgetsDAO implements HtmlWidgetsDAO {
	
	//***** PROPERTIES
	private DbSessionFactory sessionFactory;
	
	/**
	 * Default Constructor
	 */
	public HibernateHtmlWidgetsDAO() {}
	
	/**
	 * @see HtmlWidgetsService#getAllMetadataByType(OpenmrsMetadata, boolean)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenmrsMetadata> List<T> getAllMetadataByType(Class<T> type, boolean includeRetired) {
		var builder = currentSession().getCriteriaBuilder();
		var criteria = builder.createQuery(type);
		var root = criteria.from(type);
		criteria.select(root);
		if (!includeRetired) {
			criteria.where(builder.isFalse(root.get("retired").as(Boolean.class)));
		}
		criteria.orderBy(builder.asc(root.get("name")));
		return currentSession().createQuery(criteria).getResultList();
	}

	/**
	 * @see HtmlWidgetsService#getAllObjectsByType(OpenmrsObject)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenmrsObject> List<T> getAllObjectsByType(Class<T> type) {
		var builder = currentSession().getCriteriaBuilder();
		var criteria = builder.createQuery(type);
		var root = criteria.from(type);
		criteria.select(root);
		if (Retireable.class.isAssignableFrom(type)) {
			criteria.where(builder.isFalse(root.get("retired").as(Boolean.class)));
		}
		else if (Voidable.class.isAssignableFrom(type)) {
			criteria.where(builder.isFalse(root.get("voided").as(Boolean.class)));
		}
		return currentSession().createQuery(criteria).getResultList();
	}

	/**
	 * @see HtmlWidgetsService#getObject(OpenmrsObject, Integer)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenmrsObject> T getObject(Class<T> type, Integer id) {
		return (T) currentSession().get(type, id);
	}
	
	/**
	 * @see HtmlWidgetsService#getUserNamesById(Class, String, List)
	 */
	@Override
	public Map<Integer, String> getUserNamesById(String query, List<String> roleNames) {
		
		StringBuilder hql = new StringBuilder();
		hql.append("select 		u.userId, pn.givenName, pn.familyName ");
		hql.append("from		User u, PersonName pn ");
		hql.append("where		u.person = pn.person ");
		
		// Not sure how to join on user_role using hql, so doing it this way for now...
		List<Integer> limitUserIds = getUserIdsForRoles(roleNames);
		if (limitUserIds != null) {
			hql.append("and		u.userId in (:limitUserIds) ");
		}
		List<String> terms = searchTerms(query);
		if (!terms.isEmpty()) {
			hql.append("and (");
			for (int i=0; i<terms.size(); i++) {
				String lc = "like :term" + i;
				hql.append((i == 0 ? "" : "or ") + "lower(pn.givenName) " + lc + " or lower(pn.middleName) " + lc + " ");
				hql.append("or lower(u.username) " + lc + " or lower(pn.familyName) " + lc + " or lower(pn.familyName2) " + lc + " ");
			}
			hql.append(") ");
		}
		hql.append("order by	pn.preferred asc ");
		Query<Object[]> q = currentSession().createQuery(hql.toString(), Object[].class);
		if (limitUserIds != null) {
			q.setParameterList("limitUserIds", limitUserIds);
		}
		bindSearchTerms(q, terms);

		Map<Integer, String> m = new HashMap<Integer, String>();
		for (Object[] row : q.list()) {
			m.put((Integer)row[0], row[2] + ", " + row[1]);
		}
		return m;
	}
	
	/**
	 * @see HtmlWidgetsService#getPersonNamesById(Class, String, List)
	 */
	@Override
	public Map<Integer, String> getPersonNamesById(String query, List<String> roleNames) {
		
		StringBuilder hql = new StringBuilder();
		hql.append("select 		p.personId, pn.givenName, pn.familyName ");
		hql.append("from		Person p, PersonName pn ");
		hql.append("where		p = pn.person ");
		
		// Not sure how to join on user_role using hql, so doing it this way for now...
		List<Integer> limitPersonIds = getPersonIdsForRoles(roleNames);
		if (limitPersonIds != null) {
			hql.append("and		p.personId in (:limitPersonIds) ");
		}
		List<String> terms = searchTerms(query);
		if (!terms.isEmpty()) {
			hql.append("and (");
			for (int i=0; i<terms.size(); i++) {
				String lc = "like :term" + i;
				hql.append((i == 0 ? "" : "or ") + "lower(pn.givenName) " + lc + " or lower(pn.middleName) " + lc + " ");
				hql.append("or lower(pn.familyName) " + lc + " or lower(pn.familyName2) " + lc + " ");
			}
			hql.append(") ");
		}
		hql.append("order by	pn.preferred asc ");
		Query<Object[]> q = currentSession().createQuery(hql.toString(), Object[].class);
		if (limitPersonIds != null) {
			q.setParameterList("limitPersonIds", limitPersonIds);
		}
		bindSearchTerms(q, terms);

		Map<Integer, String> m = new HashMap<Integer, String>();
		for (Object[] row : q.list()) {
			m.put((Integer)row[0], row[1] + ", " + row[2]);
		}
		return m;
	}
	
	/**
	 * @see HtmlWidgetsService#getUserIdsForRoles(List)
	 */
	@SuppressWarnings("unchecked")
	private List<Integer> getUserIdsForRoles(List<String> roleNames) {
		// Not sure how to join on user_role using hql, so doing it this way for now...
		List<Integer> limitUserIds = null;
		if (roleNames != null && roleNames.size() > 0) {
			String roleQuery = "select user_id from user_role where role in (:roleNames)";
			limitUserIds = currentSession()
			        .createNativeQuery(roleQuery, Integer.class)
			        .setParameterList("roleNames", roleNames)
			        .getResultList();
		}
		return limitUserIds;
	}
	
	/**
	 * @see HtmlWidgetsService#getUserIdsForRoles(List)
	 */
	@SuppressWarnings("unchecked")
	private List<Integer> getPersonIdsForRoles(List<String> roleNames) {
		// Not sure how to join on user_role using hql, so doing it this way for now...
		List<Integer> limitPersonIds = null;
		if (roleNames != null && roleNames.size() > 0) {
			String roleQuery = "select u.person_id from user_role r, users u where u.user_id = r.user_id and r.role in (:roleNames)";
			limitPersonIds = currentSession()
			        .createNativeQuery(roleQuery, Integer.class)
			        .setParameterList("roleNames", roleNames)
			        .getResultList();
		}
		return limitPersonIds;
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

	private Session currentSession() {
		return sessionFactory.getHibernateSessionFactory().getCurrentSession();
	}

	private List<String> searchTerms(String query) {
		if (StringUtils.isBlank(query)) {
			return List.of();
		}
		return Stream.of(query.toLowerCase().split(" "))
		        .filter(StringUtils::isNotBlank)
		        .map(term -> "%" + term + "%")
		        .toList();
	}

	private void bindSearchTerms(Query<?> queryObject, List<String> terms) {
		for (int i = 0; i < terms.size(); i++) {
			queryObject.setParameter("term" + i, terms.get(i));
		}
	}
}
