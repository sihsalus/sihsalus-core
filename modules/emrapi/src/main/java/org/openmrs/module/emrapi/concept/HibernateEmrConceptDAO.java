/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.concept;

import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSearchResult;
import org.openmrs.ConceptSource;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.transaction.annotation.Transactional;

/** */
public class HibernateEmrConceptDAO implements EmrConceptDAO {

  DbSessionFactory sessionFactory;

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Concept> getConceptsMappedTo(
      Collection<ConceptMapType> mapTypes, ConceptReferenceTerm term) {
    Query query =
        sessionFactory
            .getCurrentSession()
            .createQuery(
                "select distinct mapping.concept from ConceptMap mapping "
                    + "where mapping.conceptMapType in :mapTypes "
                    + "and mapping.conceptReferenceTerm = :term");
    query.setParameter("mapTypes", mapTypes);
    query.setParameter("term", term);
    return query.getResultList();
  }

  /**
   * @see
   *     org.openmrs.module.emrapi.concept.EmrConceptDAO#conceptSearch(String,Locale,Collection,Collection,Collection,Integer)
   */
  @Override
  @Transactional(readOnly = true)
  public List<ConceptSearchResult> conceptSearch(
      String query,
      Locale locale,
      Collection<ConceptClass> classes,
      Collection<Concept> inSets,
      Collection<ConceptSource> sources,
      Integer limit) {
    List<String> uniqueWords = getUniqueWords(query, locale);
    if (uniqueWords.isEmpty()) {
      return Collections.emptyList();
    }

    List<ConceptSearchResult> results = new ArrayList<ConceptSearchResult>();

    List<ConceptName> matchedNames =
        findMatchingConceptNames(query, locale, classes, inSets, sources, uniqueWords, limit);
    Set<Concept> conceptsMatchedByPreferredName = new HashSet<Concept>();
    for (ConceptName matchedName : matchedNames) {
      results.add(
          new ConceptSearchResult(
              null,
              matchedName.getConcept(),
              matchedName,
              calculateMatchScore(query, matchedName)));
      if (matchedName.isLocalePreferred()) {
        conceptsMatchedByPreferredName.add(matchedName.getConcept());
      }
    }

    // don't display synonym matches if the preferred name matches too
    for (Iterator<ConceptSearchResult> i = results.iterator(); i.hasNext(); ) {
      ConceptSearchResult candidate = i.next();
      if (!candidate.getConceptName().isLocalePreferred()
          && conceptsMatchedByPreferredName.contains(candidate.getConcept())) {
        i.remove();
      }
    }

    if (!CollectionUtils.isEmpty(sources)) {
      for (ConceptMap mapping : findMatchingConceptMappings(query, classes, sources, limit)) {
        results.add(
            new ConceptSearchResult(
                null, mapping.getConcept(), null, calculateMatchScore(query, mapping)));
      }
    }

    Collections.sort(
        results,
        new Comparator<ConceptSearchResult>() {

          @Override
          public int compare(ConceptSearchResult left, ConceptSearchResult right) {
            return right.getTransientWeight().compareTo(left.getTransientWeight());
          }
        });

    if (results.size() > limit) {
      results = results.subList(0, limit);
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  private List<ConceptName> findMatchingConceptNames(
      String query,
      Locale locale,
      Collection<ConceptClass> classes,
      Collection<Concept> inSets,
      Collection<ConceptSource> sources,
      List<String> uniqueWords,
      Integer limit) {
    StringBuilder hql =
        new StringBuilder(
            "select distinct cn from ConceptName cn join cn.concept c where cn.voided = false and c.retired = false");

    boolean countrySpecificLocale =
        StringUtils.isNotBlank(locale.getCountry()) || StringUtils.isNotBlank(locale.getVariant());
    if (countrySpecificLocale) {
      hql.append(" and cn.locale in :locales");
    } else {
      hql.append(" and cn.locale = :locale");
    }
    if (!CollectionUtils.isEmpty(inSets)) {
      hql.append(
          " and c in (select conceptSet.concept from ConceptSet conceptSet where conceptSet.conceptSet in :inSets)");
    }
    if (!CollectionUtils.isEmpty(classes) && CollectionUtils.isEmpty(inSets)) {
      hql.append(" and c.conceptClass in :classes");
    }
    if (!CollectionUtils.isEmpty(sources) && CollectionUtils.isEmpty(inSets)) {
      hql.append(
          " and exists (select mapping from ConceptMap mapping "
              + "where mapping.concept = c and mapping.conceptReferenceTerm.conceptSource in :sources)");
    }
    for (int i = 0; i < uniqueWords.size(); i++) {
      hql.append(" and lower(cn.name) like :word").append(i);
    }

    Query conceptNameQuery = sessionFactory.getCurrentSession().createQuery(hql.toString());
    if (countrySpecificLocale) {
      conceptNameQuery.setParameter("locales", List.of(locale, new Locale(locale.getLanguage())));
    } else {
      conceptNameQuery.setParameter("locale", locale);
    }
    if (!CollectionUtils.isEmpty(inSets)) {
      conceptNameQuery.setParameter("inSets", inSets);
    }
    if (!CollectionUtils.isEmpty(classes) && CollectionUtils.isEmpty(inSets)) {
      conceptNameQuery.setParameter("classes", classes);
    }
    if (!CollectionUtils.isEmpty(sources) && CollectionUtils.isEmpty(inSets)) {
      conceptNameQuery.setParameter("sources", sources);
    }
    for (int i = 0; i < uniqueWords.size(); i++) {
      conceptNameQuery.setParameter("word" + i, "%" + uniqueWords.get(i).toLowerCase(locale) + "%");
    }
    if (limit != null) {
      conceptNameQuery.setMaxResults(limit);
    }
    return conceptNameQuery.getResultList();
  }

  @SuppressWarnings("unchecked")
  private List<ConceptMap> findMatchingConceptMappings(
      String query,
      Collection<ConceptClass> classes,
      Collection<ConceptSource> sources,
      Integer limit) {
    StringBuilder hql =
        new StringBuilder(
            "select distinct mapping from ConceptMap mapping "
                + "join mapping.concept concept "
                + "join mapping.conceptReferenceTerm term "
                + "where concept.retired = false "
                + "and term.retired = false "
                + "and term.conceptSource in :sources "
                + "and lower(term.code) = :code");
    if (!CollectionUtils.isEmpty(classes)) {
      hql.append(" and concept.conceptClass in :classes");
    }

    Query mappingQuery = sessionFactory.getCurrentSession().createQuery(hql.toString());
    mappingQuery.setParameter("sources", sources);
    mappingQuery.setParameter("code", query.toLowerCase(Locale.ROOT));
    if (!CollectionUtils.isEmpty(classes)) {
      mappingQuery.setParameter("classes", classes);
    }
    if (limit != null) {
      mappingQuery.setMaxResults(limit);
    }
    return mappingQuery.getResultList();
  }

  /**
   * Copied over from OpenMRS 1.9.8 to provide backwards compatibility. It's no longer available in
   * 1.11.
   *
   * @param phrase
   * @param locale
   * @return
   */
  public static List<String> getUniqueWords(String phrase, Locale locale) {
    String[] parts = splitPhrase(phrase);
    List<String> uniqueParts = new Vector<String>();

    if (parts != null) {
      List<String> conceptStopWords = Context.getConceptService().getConceptStopWords(locale);
      for (String part : parts) {
        if (!StringUtils.isBlank(part)) {
          String upper = part.trim().toUpperCase();
          if (!conceptStopWords.contains(upper) && !uniqueParts.contains(upper))
            uniqueParts.add(upper);
        }
      }
    }

    return uniqueParts;
  }

  /**
   * Copied over from OpenMRS 1.9.8 to provide backwards compatibility. It's no longer available in
   * 1.11.
   *
   * @param phrase
   * @return
   */
  public static String[] splitPhrase(String phrase) {
    if (StringUtils.isBlank(phrase)) {
      return null;
    }
    if (phrase.length() > 2) {
      phrase = phrase.replaceAll(OpenmrsConstants.REGEX_LARGE, " ");
    } else {
      phrase = phrase.replaceAll(OpenmrsConstants.REGEX_SMALL, " ");
    }

    return phrase.trim().replace('\n', ' ').split(" ");
  }

  private Double calculateMatchScore(String query, ConceptMap matchedMapping) {
    // eventually consider weighting this by map type (e.g. same-as > narrower-than > others)
    return 10000d;
  }

  private Double calculateMatchScore(String query, ConceptName matchedName) {
    double score = 0d;
    if (query.equalsIgnoreCase(matchedName.getName())) {
      score += 1000d;
    }
    if (matchedName.isLocalePreferred()) {
      score += 500d;
    }
    score -= matchedName.getName().length();
    return score;
  }
}
