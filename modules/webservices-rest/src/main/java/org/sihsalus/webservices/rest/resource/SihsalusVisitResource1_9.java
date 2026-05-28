/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 */
package org.sihsalus.webservices.rest.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8.LocationResource1_8;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8.PatientResource1_8;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_9.VisitResource1_9;

/**
 * Jakarta-compatible override for the upstream OpenMRS visit REST resource.
 *
 * <p>The upstream 3.4.1 OMOD resource was compiled against {@code javax.servlet}, but this
 * application runs on Spring Boot 4 / Jakarta Servlet. Giving this resource a lower order lets it
 * replace the upstream resource without changing the public REST contract.
 */
@Resource(
    name = RestConstants.VERSION_1 + "/visit",
    supportedClass = Visit.class,
    supportedOpenmrsVersions = {"1.9.* - 9.*"},
    order = 0)
public class SihsalusVisitResource1_9 extends VisitResource1_9 {

  /**
   * @see DelegatingCrudResource#search(RequestContext)
   */
  @Override
  public SimpleObject search(RequestContext context) throws ResponseException {
    String patientParameter = context.getRequest().getParameter("patient");
    String locationParameter = context.getRequest().getParameter("location");
    String includeInactiveParameter = context.getRequest().getParameter("includeInactive");
    String fromStartDate = context.getRequest().getParameter("fromStartDate");
    String toStartDate = context.getRequest().getParameter("toStartDate");
    String fromStopDate = context.getRequest().getParameter("fromStopDate");
    String toStopDate = context.getRequest().getParameter("toStopDate");
    String visitTypeParameter = context.getRequest().getParameter("visitType");
    String includeParentLocations = context.getRequest().getParameter("includeParentLocations");
    if (patientParameter != null
        || includeInactiveParameter != null
        || locationParameter != null
        || visitTypeParameter != null) {
      Date minStartDate =
          fromStartDate != null ? (Date) ConversionUtil.convert(fromStartDate, Date.class) : null;
      Date maxStartDate =
          toStartDate != null ? (Date) ConversionUtil.convert(toStartDate, Date.class) : null;
      Date minStopDate =
          fromStopDate != null ? (Date) ConversionUtil.convert(fromStopDate, Date.class) : null;
      Date maxStopDate =
          toStopDate != null ? (Date) ConversionUtil.convert(toStopDate, Date.class) : null;
      return getVisits(
          context,
          patientParameter,
          includeInactiveParameter,
          minStartDate,
          maxStartDate,
          minStopDate,
          maxStopDate,
          locationParameter,
          visitTypeParameter,
          includeParentLocations);
    } else {
      return super.search(context);
    }
  }

  private SimpleObject getVisits(
      RequestContext context,
      String patientParameter,
      String includeInactiveParameter,
      Date minStartDate,
      Date maxStartDate,
      Date minStopDate,
      Date maxStopDate,
      String locationParameter,
      String visitTypeParameter,
      String includeParentLocations) {
    Collection<Patient> patients =
        patientParameter == null ? null : Arrays.asList(getPatient(patientParameter));
    Collection<Location> locations =
        locationParameter == null
            ? null
            : Boolean.parseBoolean(includeParentLocations)
                ? getLocationAndParents(getLocation(locationParameter), null)
                : Arrays.asList(getLocation(locationParameter));
    Collection<VisitType> visitTypes =
        visitTypeParameter == null ? null : Arrays.asList(getVisitType(visitTypeParameter));
    boolean includeInactive =
        includeInactiveParameter == null || Boolean.parseBoolean(includeInactiveParameter);

    return new NeedsPaging<>(
            Context.getVisitService()
                .getVisits(
                    visitTypes,
                    patients,
                    locations,
                    null,
                    minStartDate,
                    maxStartDate,
                    minStopDate,
                    maxStopDate,
                    null,
                    includeInactive,
                    context.getIncludeAll()),
            context)
        .toSimpleObject(this);
  }

  private List<Location> getLocationAndParents(Location location, List<Location> locations) {
    if (locations == null) {
      locations = new ArrayList<>();
    }
    locations.add(location);
    if (location.getParentLocation() != null) {
      locations = getLocationAndParents(location.getParentLocation(), locations);
    }
    return locations;
  }

  private Patient getPatient(String patientUniqueId) {
    Patient patient =
        ((PatientResource1_8)
                Context.getService(RestService.class)
                    .getResourceByName(RestConstants.VERSION_1 + "/patient"))
            .getByUniqueId(patientUniqueId);
    if (patient == null) {
      throw new ObjectNotFoundException();
    }
    return patient;
  }

  private Location getLocation(String locationUniqueId) {
    Location location =
        ((LocationResource1_8)
                Context.getService(RestService.class)
                    .getResourceByName(RestConstants.VERSION_1 + "/location"))
            .getByUniqueId(locationUniqueId);
    if (location == null) {
      throw new ObjectNotFoundException();
    }
    return location;
  }

  private VisitType getVisitType(String visitTypeUuid) {
    VisitType visitType = Context.getVisitService().getVisitTypeByUuid(visitTypeUuid);
    if (visitType == null) {
      throw new ObjectNotFoundException();
    }
    return visitType;
  }
}
