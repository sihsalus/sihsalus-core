package org.openmrs.module.addresshierarchy.db.hibernate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.AddressToEntryMap;
import org.openmrs.module.addresshierarchy.db.AddressHierarchyDAO;
import org.openmrs.module.addresshierarchy.exception.AddressHierarchyModuleException;

/**
 * Hibernate implementation of the Address Hierarchy DAO. The upstream module used the legacy
 * Criteria/SQLQuery APIs; SIH Salus keeps the same DAO contract but runs it on Hibernate 7.
 */
public class HibernateAddressHierarchyDAO implements AddressHierarchyDAO {

	protected final Log log = LogFactory.getLog(getClass());

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public int getAddressHierarchyEntryCount() {
		Long count = getCurrentSession()
		        .createQuery("select count(entry) from AddressHierarchyEntry entry", Long.class)
		        .getSingleResult();
		return count.intValue();
	}

	@Override
	public int getAddressHierarchyEntryCountByLevel(AddressHierarchyLevel level) {
		Long count = getCurrentSession()
		        .createQuery(
		            "select count(entry) from AddressHierarchyEntry entry where entry.level.levelId = :levelId",
		            Long.class)
		        .setParameter("levelId", level.getId())
		        .getSingleResult();
		return count.intValue();
	}

	@Override
	public AddressHierarchyEntry getAddressHierarchyEntry(int addressHierarchyEntryId) {
		return getCurrentSession().get(AddressHierarchyEntry.class, addressHierarchyEntryId);
	}

	@Override
	public AddressHierarchyEntry getAddressHierarchyEntryByUserGenId(String userGeneratedId) {
		return firstOrNull(getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where entry.userGeneratedId = :userGeneratedId",
		            AddressHierarchyEntry.class)
		        .setParameter("userGeneratedId", userGeneratedId));
	}

	@Override
	public List<AddressHierarchyEntry> getAddressHierarchyEntriesByLevel(AddressHierarchyLevel addressHierarchyLevel) {
		return getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where entry.level.levelId = :levelId",
		            AddressHierarchyEntry.class)
		        .setParameter("levelId", addressHierarchyLevel.getId())
		        .getResultList();
	}

	@Override
	public List<AddressHierarchyEntry> getAddressHierarchyEntriesByLevelAndName(
	        AddressHierarchyLevel addressHierarchyLevel, String name) {
		Query<AddressHierarchyEntry> query = getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where entry.level.levelId = :levelId and "
		                    + namePredicate("entry.name"),
		            AddressHierarchyEntry.class)
		        .setParameter("levelId", addressHierarchyLevel.getId());
		setNameParameter(query, name);
		return query.getResultList();
	}

	@Override
	public List<AddressHierarchyEntry> getAddressHierarchyEntriesByLevelAndNameAndParent(
	        AddressHierarchyLevel addressHierarchyLevel, String name, AddressHierarchyEntry parent) {
		Query<AddressHierarchyEntry> query = getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where entry.level.levelId = :levelId and "
		                    + parentPredicate("entry.parent", parent) + " and " + namePredicate("entry.name"),
		            AddressHierarchyEntry.class)
		        .setParameter("levelId", addressHierarchyLevel.getId());
		setParentParameter(query, parent);
		setNameParameter(query, name);
		return query.getResultList();
	}

	@Override
	public List<AddressHierarchyEntry> getAddressHierarchyEntriesByLevelAndLikeNameAndParent(
	        AddressHierarchyLevel addressHierarchyLevel, String name, AddressHierarchyEntry parent) {
		Query<AddressHierarchyEntry> query = getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where entry.level.levelId = :levelId and "
		                    + parentPredicate("entry.parent", parent) + " and lower(entry.name) like :name",
		            AddressHierarchyEntry.class)
		        .setParameter("levelId", addressHierarchyLevel.getId())
		        .setParameter("name", likePattern(name));
		setParentParameter(query, parent);
		return query.getResultList();
	}

	@Override
	public List<AddressHierarchyEntry> getChildAddressHierarchyEntries(AddressHierarchyEntry entry) {
		Query<AddressHierarchyEntry> query = getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where " + parentPredicate("entry.parent", entry),
		            AddressHierarchyEntry.class);
		setParentParameter(query, entry);
		return query.getResultList();
	}

	@Override
	public AddressHierarchyEntry getChildAddressHierarchyEntryByName(AddressHierarchyEntry entry, String childName) {
		Query<AddressHierarchyEntry> query = getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where " + parentPredicate("entry.parent", entry)
		                    + " and " + namePredicate("entry.name"),
		            AddressHierarchyEntry.class);
		setParentParameter(query, entry);
		setNameParameter(query, childName);

		List<AddressHierarchyEntry> entries = query.getResultList();
		if (entries.isEmpty()) {
			return null;
		}
		if (entries.size() > 1) {
			log.error("Duplicate address hierarchy entries: " + entries.get(0).getName());
		}
		return entries.get(0);
	}

	@Override
	public void saveAddressHierarchyEntry(AddressHierarchyEntry ah) {
		try {
			getCurrentSession().saveOrUpdate(ah);
		}
		catch (Throwable t) {
			throw new DAOException(t);
		}
	}

	@Override
	public void deleteAllAddressHierarchyEntries() {
		AddressHierarchyLevel top = getTopAddressHierarchyLevel();
		if (top != null) {
			for (AddressHierarchyEntry entry : getAddressHierarchyEntriesByLevel(top)) {
				getCurrentSession().delete(entry);
			}
		}
	}

	@Override
	public List<AddressHierarchyLevel> getAddressHierarchyLevels() {
		return getCurrentSession()
		        .createQuery("from AddressHierarchyLevel level", AddressHierarchyLevel.class)
		        .getResultList();
	}

	@Override
	public AddressHierarchyLevel getTopAddressHierarchyLevel() {
		try {
			return uniqueOrNull(
			    getCurrentSession()
			            .createQuery(
			                "from AddressHierarchyLevel level where level.parent is null",
			                AddressHierarchyLevel.class),
			    "Unable to fetch top level address hierarchy type");
		}
		catch (Exception e) {
			if (e instanceof AddressHierarchyModuleException moduleException) {
				throw moduleException;
			}
			throw new AddressHierarchyModuleException("Unable to fetch top level address hierarchy type", e);
		}
	}

	@Override
	public AddressHierarchyLevel getAddressHierarchyLevel(int levelId) {
		return getCurrentSession().get(AddressHierarchyLevel.class, levelId);
	}

	@Override
	public AddressHierarchyLevel getAddressHierarchyLevelByParent(AddressHierarchyLevel parent) {
		try {
			Query<AddressHierarchyLevel> query = getCurrentSession()
			        .createQuery(
			            "from AddressHierarchyLevel level where "
			                    + parentPredicate("level.parent", parent),
			            AddressHierarchyLevel.class);
			setParentParameter(query, parent);
			return uniqueOrNull(query, "Unable to fetch child address hierarchy type");
		}
		catch (Exception e) {
			if (e instanceof AddressHierarchyModuleException moduleException) {
				throw moduleException;
			}
			throw new AddressHierarchyModuleException("Unable to fetch child address hierarchy type", e);
		}
	}

	@Override
	public void saveAddressHierarchyLevel(AddressHierarchyLevel level) {
		try {
			getCurrentSession().saveOrUpdate(level);
		}
		catch (Throwable t) {
			throw new DAOException(t);
		}
	}

	@Override
	public void deleteAddressHierarchyLevel(AddressHierarchyLevel level) {
		try {
			getCurrentSession().delete(level);
		}
		catch (Throwable t) {
			throw new DAOException(t);
		}
	}

	@Override
	public AddressToEntryMap getAddressToEntryMap(int id) {
		return getCurrentSession().get(AddressToEntryMap.class, id);
	}

	@Override
	public List<AddressToEntryMap> getAddressToEntryMapByPersonAddress(PersonAddress address) {
		return getCurrentSession()
		        .createQuery(
		            "from AddressToEntryMap map where map.address.personAddressId = :addressId",
		            AddressToEntryMap.class)
		        .setParameter("addressId", address.getId())
		        .getResultList();
	}

	@Override
	public void saveAddressToEntryMap(AddressToEntryMap addressToEntryMap) {
		try {
			getCurrentSession().saveOrUpdate(addressToEntryMap);
		}
		catch (Throwable t) {
			throw new DAOException(t);
		}
	}

	@Override
	public void deleteAddressToEntryMap(AddressToEntryMap addressToEntryMap) {
		try {
			getCurrentSession().delete(addressToEntryMap);
		}
		catch (Throwable t) {
			throw new DAOException(t);
		}
	}

	@Override
	public List<Patient> findAllPatientsWithDateChangedAfter(Date date) {
		return getCurrentSession()
		        .createQuery(
		            "from Patient patient where patient.dateChanged >= :date and patient.voided = false",
		            Patient.class)
		        .setParameter("date", date)
		        .getResultList();
	}

	@Override
	@Deprecated
	public void associateCoordinates(AddressHierarchyEntry ah, double latitude, double longitude) {
		ah.setLatitude(latitude);
		ah.setLongitude(longitude);
		getCurrentSession().update(ah);
	}

	@Override
	@Deprecated
	public List<AddressHierarchyEntry> getLeafNodes(AddressHierarchyEntry ah) {
		List<AddressHierarchyEntry> leafList = new ArrayList<>();
		getLowestLevel(ah, leafList);
		return leafList;
	}

	@Deprecated
	private List<AddressHierarchyEntry> getLowestLevel(
	        AddressHierarchyEntry ah, List<AddressHierarchyEntry> leafList) {
		List<AddressHierarchyEntry> children = getChildAddressHierarchyEntries(ah);
		if (!children.isEmpty()) {
			for (AddressHierarchyEntry addressHierarchy : children) {
				getLowestLevel(addressHierarchy, leafList);
			}
		} else {
			leafList.add(ah);
		}
		return children;
	}

	@Override
	@Deprecated
	public void initializeRwandaHierarchyTables() {
		Session session = getCurrentSession();

		AddressHierarchyLevel country = new AddressHierarchyLevel();
		country.setName("Country");

		AddressHierarchyLevel province = new AddressHierarchyLevel();
		province.setName("Province");

		AddressHierarchyLevel district = new AddressHierarchyLevel();
		district.setName("District");

		AddressHierarchyLevel sector = new AddressHierarchyLevel();
		sector.setName("Sector");

		AddressHierarchyLevel cell = new AddressHierarchyLevel();
		cell.setName("Cell");

		AddressHierarchyLevel umudugudu = new AddressHierarchyLevel();
		umudugudu.setName("Umudugudu");

		session.save(country);
		session.save(province);
		session.save(country);
		session.save(district);
		session.save(sector);
		session.save(cell);
		session.save(umudugudu);

		province.setParent(country);
		district.setParent(province);
		sector.setParent(district);
		cell.setParent(sector);
		umudugudu.setParent(cell);
	}

	@Override
	@Deprecated
	public int getUnstructuredCount(int page) {
		String invalidAddressCount = "select count(*) "
		        + " from person_address "
		        + " left join patient_identifier on patient_identifier.patient_id = person_address.person_id "
		        + " left join patient_program on patient_program.patient_id = person_address.person_id "
		        + " left join patient_state on patient_program.patient_program_id = patient_state.patient_program_id "
		        + " left join program_workflow_state on patient_state.state = program_workflow_state.program_workflow_state_id "
		        + " left join concept_name on concept_name.concept_id = program_workflow_state.concept_id "
		        + " left join person_name on person_name.person_id = person_address.person_id "
		        + " where person_address.voided = false AND "
		        + " patient_identifier.preferred = true AND "
		        + " person_name.preferred = true AND "
		        + " patient_program.voided = false AND "
		        + " patient_program.date_completed is null AND "
		        + " (person_address.country not in (select name from address_hierarchy where type_id = 1) "
		        + " OR person_address.state_province not in (select name from address_hierarchy where type_id = 2 and parent_id in (select address_hierarchy_id from address_hierarchy where name = person_address.country and type_id = 1)) "
		        + " OR person_address.county_district not in (select name from address_hierarchy where type_id = 3 and parent_id in (select address_hierarchy_id from address_hierarchy where name = person_address.state_province and type_id = 2))"
		        + " OR person_address.city_village not in (select name from address_hierarchy where type_id = 4 and parent_id in (select address_hierarchy_id from address_hierarchy where name = person_address.county_district and type_id = 3))"
		        + " OR person_address.neighborhood_cell not in (select name from address_hierarchy where type_id = 5 and parent_id in (select address_hierarchy_id from address_hierarchy where name = person_address.city_village and type_id = 4))"
		        + " OR person_address.address1 not in (select name from address_hierarchy where type_id = 6 and parent_id in (select address_hierarchy_id from address_hierarchy where name = person_address.neighborhood_cell and type_id = 5)))";

		Object result = getCurrentSession().createNativeQuery(invalidAddressCount).getSingleResult();
		return result instanceof Number number ? number.intValue() : 0;
	}

	@Override
	@Deprecated
	@SuppressWarnings("unchecked")
	public List<Object[]> findUnstructuredAddresses(int page, int locationId) {
		int startIndex = page > 0 ? page * 100 - 100 : 0;
		String cellUmu = "select x.state_province, x.county_district, x.city_village, x.neighborhood_cell, "
		        + "x.address1, pi.patient_id, pi.identifier, location.name "
		        + "from (select identifier, location_id, patient_id, patient_identifier_id from patient_identifier where preferred = true) pi "
		        + "left join (select address1, state_province, county_district, city_village, neighborhood_cell, "
		        + "date_created, person_id, person_address_id from person_address pa "
		        + "left join address_hierarchy on pa.address1 = address_hierarchy.name "
		        + "inner join address_hierarchy ah2 on pa.neighborhood_cell = ah2.name "
		        + "and address_hierarchy.parent_id = ah2.address_hierarchy_id "
		        + "and ah2.type_id = (select location_attribute_type_id from address_hierarchy_type where name = 'Cell') "
		        + "where voided = false) x on pi.patient_id = x.person_id "
		        + "inner join location on location.location_id = pi.location_id "
		        + "where location.location_id = :locationId and x.person_id is null "
		        + "order by x.date_created desc";

		NativeQuery<?> query = getCurrentSession().createNativeQuery(cellUmu);
		query.setParameter("locationId", locationId);
		query.setMaxResults(100);
		query.setFirstResult(startIndex);
		return (List<Object[]>) query.getResultList();
	}

	@Override
	@Deprecated
	@SuppressWarnings("unchecked")
	public List<Object[]> getLocationAddressBreakdown(int locationId) {
		String locationBreakdown = "select pa.county_district, pa.city_village, count(*) "
		        + "from (select identifier, location_id, patient_id, patient_identifier_id from patient_identifier where preferred = true) pi "
		        + "inner join location on location.location_id = pi.location_id and location.location_id = :locationId "
		        + "inner join (select country, state_province, county_district, city_village, person_id "
		        + "from person_address where voided = false and preferred = true) pa on pi.patient_id = pa.person_id "
		        + "group by pa.country, pa.state_province, pa.county_district, pa.city_village";

		return (List<Object[]>) getCurrentSession()
		        .createNativeQuery(locationBreakdown)
		        .setParameter("locationId", locationId)
		        .getResultList();
	}

	@Override
	@Deprecated
	@SuppressWarnings("unchecked")
	public List<Object[]> getAllAddresses(int page) {
		int startIndex = page > 0 ? page * 400 - 400 : 0;
		String allAddresses = "select * from (select max(date_created), patient_id from patient_program group by patient_id) pp "
		        + "inner join person_address on pp.patient_id = person_address.person_id "
		        + "where person_address.voided = false order by person_address.date_created desc";

		NativeQuery<?> query = getCurrentSession().createNativeQuery(allAddresses);
		query.setMaxResults(100);
		query.setFirstResult(startIndex);
		return (List<Object[]>) query.getResultList();
	}

	@Override
	public List<AddressHierarchyEntry> getAddressHierarchyEntriesByLevelAndLikeName(
	        AddressHierarchyLevel level, String name, int limit) {
		return getCurrentSession()
		        .createQuery(
		            "from AddressHierarchyEntry entry where entry.level.levelId = :levelId and lower(entry.name) like :name",
		            AddressHierarchyEntry.class)
		        .setParameter("levelId", level.getId())
		        .setParameter("name", likePattern(name))
		        .setMaxResults(limit)
		        .getResultList();
	}

	@Override
	public AddressHierarchyEntry getAddressHierarchyEntryByUuid(String uuid) {
		return firstOrNull(getCurrentSession()
		        .createQuery("from AddressHierarchyEntry entry where entry.uuid = :uuid", AddressHierarchyEntry.class)
		        .setParameter("uuid", uuid));
	}

	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	private String namePredicate(String property) {
		return "((:nameIsNull = true and " + property + " is null) or (:nameIsNull = false and lower(" + property
		        + ") = :name))";
	}

	private String parentPredicate(String property, Object parent) {
		return parent == null ? property + " is null" : property + " = :parent";
	}

	private void setNameParameter(Query<?> query, String name) {
		query.setParameter("nameIsNull", name == null);
		query.setParameter("name", name == null ? "" : name.toLowerCase());
	}

	private void setParentParameter(Query<?> query, Object parent) {
		if (parent != null) {
			query.setParameter("parent", parent);
		}
	}

	private String likePattern(String name) {
		return "%" + (name == null ? "" : name.toLowerCase()) + "%";
	}

	private <T> T firstOrNull(Query<T> query) {
		List<T> results = query.setMaxResults(1).getResultList();
		return results.isEmpty() ? null : results.get(0);
	}

	private <T> T uniqueOrNull(Query<T> query, String message) {
		List<T> results = query.setMaxResults(2).getResultList();
		if (results.size() > 1) {
			throw new AddressHierarchyModuleException(message);
		}
		return results.isEmpty() ? null : results.get(0);
	}
}
