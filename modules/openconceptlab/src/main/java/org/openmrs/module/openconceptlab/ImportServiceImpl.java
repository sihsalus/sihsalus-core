/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.openconceptlab;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.query.Query;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.api.impl.BaseOpenmrsService;

public class ImportServiceImpl extends BaseOpenmrsService implements ImportService {

  DbSessionFactory sessionFactory;

  AdministrationService adminService;

  ConceptService conceptService;

  OclConceptService oclConceptService;

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void setAdminService(AdministrationService adminService) {
    this.adminService = adminService;
  }

  public void setConceptService(ConceptService conceptService) {
    this.conceptService = conceptService;
  }

  public void setOclConceptService(OclConceptService oclConceptService) {
    this.oclConceptService = oclConceptService;
  }

  /**
   * @should return all updates ordered descending by ids
   */
  @Override
  public List<Import> getImportsInOrder(int first, int max) {
    Query<Import> query = createQuery("from OclImport i order by i.importId desc");
    query.setFirstResult(first);
    query.setMaxResults(max);
    return query.list();
  }

  @Override
  public List<Import> getInProgressImports() {
    return this.<Import>createQuery(
            "from OclImport i where i.localDateStopped is null order by i.importId desc")
        .list();
  }

  @Override
  public List<Concept> getConceptsByName(String name, Locale locale) {
    StringBuilder hql = new StringBuilder("from ConceptName cn where cn.voided = false");
    Query<ConceptName> query;
    if (adminService.isDatabaseStringComparisonCaseSensitive()) {
      hql.append(" and lower(cn.name) = :name");
      query = createQuery(hql.append(" and cn.locale = :locale").toString());
      query.setParameter("name", name.toLowerCase(Locale.ROOT));
    } else {
      hql.append(" and cn.name = :name");
      query = createQuery(hql.append(" and cn.locale = :locale").toString());
      query.setParameter("name", name);
    }
    query.setParameter("locale", locale);
    List<ConceptName> conceptNames = query.list();

    Set<Concept> concepts = new LinkedHashSet<Concept>();
    for (ConceptName conceptName : conceptNames) {
      concepts.add(conceptName.getConcept());
    }
    return new ArrayList<Concept>(concepts);
  }

  @Override
  public List<ConceptName> changeDuplicateConceptNamesToIndexTerms(Concept conceptToImport) {
    List<ConceptName> result = new ArrayList<ConceptName>();

    if (conceptToImport.isRetired()) {
      return Collections.emptyList();
    }

    boolean dbCaseSensitive = adminService.isDatabaseStringComparisonCaseSensitive();
    Iterator<ConceptName> it = conceptToImport.getNames().iterator();
    while (it.hasNext()) {
      ConceptName nameToImport = it.next();

      if (Boolean.TRUE.equals(nameToImport.getVoided())) {
        continue;
      }

      if (ConceptNameType.INDEX_TERM.equals(nameToImport.getConceptNameType())) {
        continue; // index terms are never considered duplicates
      }

      if (nameToImport.isLocalePreferred()
          || nameToImport.isFullySpecifiedName()
          || nameToImport.equals(nameToImport.getConcept().getName(nameToImport.getLocale()))) {
        StringBuilder hql = new StringBuilder("from ConceptName cn where cn.voided = false");
        Query<ConceptName> query;
        if (dbCaseSensitive) {
          hql.append(" and lower(cn.name) = :name");
          query =
              createQuery(
                  hql.append(" and (cn.locale = :locale or cn.locale = :languageLocale)")
                      .toString());
          query.setParameter("name", nameToImport.getName().toLowerCase(Locale.ROOT));
        } else {
          hql.append(" and cn.name = :name");
          query =
              createQuery(
                  hql.append(" and (cn.locale = :locale or cn.locale = :languageLocale)")
                      .toString());
          query.setParameter("name", nameToImport.getName());
        }
        query.setParameter("locale", nameToImport.getLocale());
        query.setParameter("languageLocale", Locale.of(nameToImport.getLocale().getLanguage()));
        List<ConceptName> conceptNames = query.list();

        for (ConceptName conceptName : conceptNames) {
          if (conceptName.getConcept().isRetired()) {
            continue;
          } else if (conceptName.getConcept().getUuid().equals(conceptToImport.getUuid())) {
            continue;
          } else if (conceptName.isLocalePreferred()
              || conceptName.isFullySpecifiedName()
              || conceptName.equals(conceptName.getConcept().getName(nameToImport.getLocale()))) {
            // if it is the default name for locale
            nameToImport.setConceptNameType(ConceptNameType.INDEX_TERM);
            nameToImport.setLocalePreferred(false);
            result.add(nameToImport);

            // start again since any previous name to import can be the default name for locale now
            it = conceptToImport.getNames().iterator();
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * @should return update with id
   * @should throw IllegalArgumentException if update does not exist
   */
  @Override
  public Import getImport(Long id) {
    Import update = (Import) getSession().get(Import.class, id);
    if (update == null) {
      throw new IllegalArgumentException("No update with the given id " + id);
    }
    return update;
  }

  @Override
  public Import getImport(String uuid) {
    Import update =
        this.<Import>createQuery("from OclImport i where i.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
    return update;
  }

  @Override
  public Import getLastImport() {
    return this.<Import>createQuery("from OclImport i order by i.importId desc")
        .setMaxResults(1)
        .uniqueResult();
  }

  @Override
  public Import getLastSuccessfulSubscriptionImport() {
    return this.<Import>createQuery(
            "from OclImport i where i.errorMessage is null and i.oclDateStarted is not null "
                + "order by i.importId desc")
        .setMaxResults(1)
        .uniqueResult();
  }

  @Override
  public Boolean isLastImportSuccessful() {

    Import lastSuccessfulSubscriptionImport = getLastSuccessfulSubscriptionImport();
    if (lastSuccessfulSubscriptionImport != null) {
      Import lastUpdate = getLastImport();
      return lastSuccessfulSubscriptionImport.equals(lastUpdate);
    } else {
      return false;
    }
  }

  @Override
  public void ignoreAllErrors(Import anImport) {
    Query query =
        getSession()
            .createQuery(
                "update OclItem i set i.state = :newState where i.anImport = :anImport and i.state = :oldState");
    query.setParameter("newState", ItemState.IGNORED_ERROR);
    query.setParameter("anImport", anImport);
    query.setParameter("oldState", ItemState.ERROR);
    query.executeUpdate();

    anImport.setErrorMessage(null);
    getSession().merge(anImport);
  }

  @Override
  public void failImport(Import anImport) {
    failImport(anImport, null);
  }

  @Override
  public void failImport(Import update, String errorMessage) {
    update = getImport(update.getImportId());

    if (!StringUtils.isBlank(errorMessage)) {
      update.setErrorMessage(errorMessage);
    } else {
      update.setErrorMessage("Errors found");
    }
    getSession().merge(update);
  }

  /**
   * @should throw IllegalStateException if another update is in progress
   */
  @Override
  public void startImport(Import anImport) {
    Import lastImport = getLastImport();
    if (lastImport != null && !lastImport.isStopped()) {
      throw new IllegalStateException(
          "Cannot start the import, if there is another import in progress.");
    }
    getSession().save(anImport);
  }

  @Override
  public void updateOclDateStarted(Import update, Date oclDateStarted) {
    update.setOclDateStarted(oclDateStarted);
    getSession().save(update);
  }

  @Override
  public void updateReleaseVersion(Import anImport, String version) {
    anImport.setReleaseVersion(version);
    getSession().save(anImport);
  }

  /**
   * @should throw IllegalArgumentException if not scheduled
   * @should throw IllegalStateException if trying to stop twice
   */
  @Override
  public void stopImport(Import anImport) {
    if (anImport.getImportId() == null) {
      throw new IllegalArgumentException("Cannot stop the import, if it has not been started.");
    }
    if (anImport.getLocalDateStopped() != null) {
      throw new IllegalStateException("Cannot stop the import twice.");
    }

    anImport = getImport(anImport.getImportId());

    anImport.stop();

    getSession().merge(anImport);
  }

  @Override
  public Item getLastSuccessfulItemByUrl(String url) {
    return getLastSuccessfulItemByUrl(url, new CacheService(conceptService, oclConceptService));
  }

  @Override
  public Item getLastSuccessfulItemByUrl(String url, CacheService cacheService) {
    Item item =
        this.<Item>createQuery(
                "from OclItem i where i.hashedUrl = :hashedUrl and i.url = :url "
                    + "and i.state <> :errorState order by i.itemId desc")
            .setParameter("hashedUrl", Item.hashUrl(url))
            .setParameter("url", url)
            .setParameter("errorState", ItemState.ERROR)
            .setMaxResults(1)
            .uniqueResult();
    if (item != null) {
      switch (item.getType()) {
        case MAPPING:
          ConceptMap map = cacheService.getConceptMapByUuid(item.getUuid(), this);
          if (map == null) {
            return null;
          }
          break;
        case CONCEPT:
          Concept concept = cacheService.getConceptByUuid(item.getUuid());
          if (concept == null) {
            return null;
          }
          break;
        default:
          throw new RuntimeException(
              "Item with UUID=" + item.getUuid() + " couldn't be recognized as Concept or Mapping");
      }
    }
    return item;
  }

  @Override
  public void saveItem(Item item) {
    getSession().merge(item);
  }

  @Override
  public void saveItems(Iterable<? extends Item> items) {
    Import attachedImport = null;
    for (Item item : items) {
      // Fetch the Import once and reuse for all items in the batch
      if (attachedImport == null) {
        attachedImport = getImport(item.getAnImport().getImportId());
      }
      item.setAnImport(attachedImport);

      saveItem(item);
    }
  }

  @Override
  public Item getItem(String uuid) {
    Item item =
        this.<Item>createQuery("from OclItem i where i.uuid = :uuid")
            .setParameter("uuid", uuid)
            .uniqueResult();
    return item;
  }

  @Override
  public Subscription getSubscription() {
    String url = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
    if (url == null) {
      return null;
    }
    Subscription subscription = new Subscription();
    subscription.setUrl(StringEscapeUtils.unescapeHtml4(url));

    String uuid = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
    subscription.setUuid(uuid);

    String token = adminService.getGlobalProperty(OpenConceptLabConstants.GP_TOKEN);
    subscription.setToken(StringEscapeUtils.unescapeHtml4(token));

    String validationType =
        adminService.getGlobalProperty(OpenConceptLabConstants.GP_VALIDATION_TYPE);
    if (StringUtils.isNotBlank(validationType)) {
      subscription.setValidationType(ValidationType.valueOf(validationType));
    }

    String days = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
    if (!StringUtils.isBlank(days)) {
      subscription.setDays(parseIntegerProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS, days));
    }

    String subscribedToSnapshot =
        adminService.getGlobalProperty(OpenConceptLabConstants.GP_SUBSCRIBED_TO_SNAPSHOT);
    subscription.setSubscribedToSnapshot(Boolean.valueOf(subscribedToSnapshot));

    String time = adminService.getGlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME);
    if (!StringUtils.isBlank(time)) {
      String[] formattedTime = time.split(":");
      if (formattedTime.length != 2) {
        throw new IllegalStateException(
            "Time in the wrong format. Expected 'HH:mm', given: " + time);
      }

      subscription.setHours(
          parseIntegerProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME, formattedTime[0]));
      subscription.setMinutes(
          parseIntegerProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME, formattedTime[1]));
    }

    return subscription;
  }

  private Integer parseIntegerProperty(String propertyName, String value) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "Invalid integer value for global property " + propertyName + ": " + value, e);
    }
  }

  private DbSession getSession() {
    return sessionFactory.getCurrentSession();
  }

  @SuppressWarnings("unchecked")
  private <T> Query<T> createQuery(String hql) {
    return (Query<T>) getSession().createQuery(hql);
  }

  @Override
  public void saveSubscription(Subscription subscription) {
    GlobalProperty uuid =
        adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
    if (uuid == null) {
      uuid = new GlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_UUID);
    }
    uuid.setPropertyValue(subscription.getUuid());
    adminService.saveGlobalProperty(uuid);

    GlobalProperty url =
        adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
    if (url == null) {
      url = new GlobalProperty(OpenConceptLabConstants.GP_SUBSCRIPTION_URL);
    }
    url.setPropertyValue(prependApiIfAbsent(subscription.getUrl()));
    adminService.saveGlobalProperty(url);

    GlobalProperty token = adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_TOKEN);
    if (token == null) {
      token = new GlobalProperty(OpenConceptLabConstants.GP_TOKEN);
    }
    token.setPropertyValue(subscription.getToken());
    adminService.saveGlobalProperty(token);

    GlobalProperty validationType =
        adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_VALIDATION_TYPE);
    if (validationType == null) {
      validationType = new GlobalProperty(OpenConceptLabConstants.GP_VALIDATION_TYPE);
    }
    validationType.setPropertyValue(subscription.getValidationType().name());
    adminService.saveGlobalProperty(validationType);

    GlobalProperty days =
        adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
    if (days == null) {
      days = new GlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_DAYS);
    }

    if (subscription.getDays() != null) {
      days.setPropertyValue(subscription.getDays().toString());
    } else {
      days.setPropertyValue("");
    }
    adminService.saveGlobalProperty(days);

    GlobalProperty time =
        adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SCHEDULED_TIME);
    if (time == null) {
      time = new GlobalProperty(OpenConceptLabConstants.GP_SCHEDULED_TIME);
    }
    if (subscription.getHours() != null && subscription.getMinutes() != null) {
      time.setPropertyValue(subscription.getHours() + ":" + subscription.getMinutes());
    } else {
      time.setPropertyValue("");
    }
    adminService.saveGlobalProperty(time);

    GlobalProperty subscribedToSnapshot =
        adminService.getGlobalPropertyObject(OpenConceptLabConstants.GP_SUBSCRIBED_TO_SNAPSHOT);
    if (subscribedToSnapshot == null) {
      subscribedToSnapshot = new GlobalProperty(OpenConceptLabConstants.GP_SUBSCRIBED_TO_SNAPSHOT);
    }
    subscribedToSnapshot.setPropertyValue(String.valueOf(subscription.isSubscribedToSnapshot()));
    adminService.saveGlobalProperty(subscribedToSnapshot);
  }

  private String prependApiIfAbsent(String stringUrl) {
    if (StringUtils.isNotBlank(stringUrl)) {
      try {
        URL url = new URL(stringUrl);
        String host = url.getHost();
        if (!host.startsWith("api.")) {
          return url.toString().replace(host, "api." + host);
        }
        return url.toString();
      } catch (MalformedURLException e) {
        throw new IllegalStateException("Wrong url address");
      }
    } else {
      return stringUrl;
    }
  }

  @Override
  public void unsubscribe() {
    saveSubscription(new Subscription());
    getSession().createQuery("delete from OclItem").executeUpdate();
    getSession().createQuery("delete from OclImport").executeUpdate();
  }

  /**
   * @param anImport the update to be passed
   * @param first starting index
   * @param max maximum limit
   * @return a list of items
   */
  @Override
  public List<Item> getImportItems(Import anImport, int first, int max, Set<ItemState> states) {
    StringBuilder hql = new StringBuilder("from OclItem i where i.anImport = :anImport");
    Query<Item> items;
    if (!states.isEmpty()) {
      hql.append(" and i.state in (:states)");
    }
    hql.append(" order by i.state desc");
    items = createQuery(hql.toString());
    items.setParameter("anImport", anImport);
    if (!states.isEmpty()) {
      items.setParameterList("states", states);
    }
    items.setFirstResult(first);
    items.setMaxResults(max);

    return items.list();
  }

  /**
   * @param anImport the update to be passed
   * @param states set of states passed
   * @return a count of items
   */
  @Override
  public Integer getImportItemsCount(Import anImport, Set<ItemState> states) {
    StringBuilder hql =
        new StringBuilder("select count(i) from OclItem i where i.anImport = :anImport");
    Query<Long> items;
    if (!(states.isEmpty())) {
      hql.append(" and i.state in (:states)");
    }
    items = createQuery(hql.toString());
    items.setParameter("anImport", anImport);
    if (!states.isEmpty()) {
      items.setParameterList("states", states);
    }
    return items.uniqueResult().intValue();
  }

  /**
   * @param uuid the uuid to search a concept with
   * @return true if subscribed else false
   */
  @Override
  public Boolean isSubscribedConcept(String uuid) {
    Long count =
        this.<Long>createQuery(
                "select count(i) from OclItem i where i.type = :type and i.uuid = :uuid")
            .setParameter("type", ItemType.CONCEPT)
            .setParameter("uuid", uuid)
            .uniqueResult();
    return count > 0;
  }

  @Override
  public ConceptMap getConceptMapByUuid(String uuid) {
    return this.<ConceptMap>createQuery("from ConceptMap cm where cm.uuid = :uuid")
        .setParameter("uuid", uuid)
        .uniqueResult();
  }

  @Override
  public Concept updateConceptWithoutValidation(Concept concept) {
    getSession().merge(concept);
    return concept;
  }

  @Override
  public ConceptReferenceTerm updateConceptReferenceTermWithoutValidation(
      ConceptReferenceTerm term) {
    getSession().merge(term);
    return term;
  }

  @Override
  public void updateSubscriptionUrl(Import anImport, String url) {
    anImport.setSubscriptionUrl(url);
    getSession().merge(anImport);
  }

  @Override
  public <T> T runInTransaction(Callable<T> callable) throws Exception {
    return callable.call();
  }

  @Override
  public void flushAndClearSession() {
    DbSession session = getSession();
    session.flush();
    session.clear();
  }
}
