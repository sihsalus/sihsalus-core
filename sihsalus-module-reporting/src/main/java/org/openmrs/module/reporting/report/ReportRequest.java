/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.reporting.report;

import org.apache.commons.lang.StringUtils;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.definition.DefinitionContext;
import org.openmrs.module.reporting.evaluation.Definition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameterizable;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;
import org.openmrs.module.reporting.serializer.ReportingSerializer;
import org.openmrs.util.OpenmrsUtil;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a request to run and render a report.
 * (The natural ordering of this class places higher priority requests first, so that it may be used in a PriorityQueue.)
 */
public class ReportRequest extends BaseOpenmrsObject {

	//***** PROPERTIES *****

	private Integer id;
	private Mapped<CohortDefinition> baseCohort; //optional
	private String baseCohortUuid;
	private String baseCohortParameterMappings;
	private Mapped<ReportDefinition> reportDefinition;
	private String reportDefinitionUuid;
	private String reportDefinitionParameterMappings;
	private RenderingMode renderingMode;
	private Class<? extends ReportRenderer> rendererType;
	private String rendererArgument;
	private Priority priority = Priority.NORMAL;
	private String schedule; //optional, in cron format
	private boolean processAutomatically = false;
	private User requestedBy;
	private Date requestDate;
	private Status status;
	private Date evaluationDate = null;
	private Date evaluateStartDatetime;
	private Date evaluateCompleteDatetime;
	private Date renderCompleteDatetime;
    private Integer minimumDaysToPreserve;
	private String description;

	//*****  CONSTRUCTORS ******

	/**
	 * Default Constructor
	 */
	public ReportRequest() {
		super();
		setUuid(UUID.randomUUID().toString());
		this.requestDate = new Date();
		this.requestedBy = Context.getAuthenticatedUser();
	}

	/**
	 * Full Constructor
	 */
	public ReportRequest(Mapped<ReportDefinition> reportDefinition, Mapped<CohortDefinition> baseCohort,
	                     RenderingMode renderingMode, Priority priority, String schedule) {
	    this();
	    setReportDefinition(reportDefinition);
	    setBaseCohort(baseCohort);
	    setRenderingMode(renderingMode);
	    this.priority = priority;
	    this.schedule = schedule;
    }

	//*****  INSTANCE METHODS ******

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder("Report Request (" + getUuid() + ")");
        if (getReportDefinition() != null && getReportDefinition().getParameterizable() != null) {
            sb.append("for " + getReportDefinition().getParameterizable().getName());
        }
        if (getRenderingMode() != null && getRenderingMode().getRenderer() != null) {
            sb.append(" to " + getRenderingMode().getRenderer().getClass().getSimpleName());
        }
        return sb.toString();
	}

	/**
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
        }
		if (!(obj instanceof ReportRequest)) {
			return false;
        }
		ReportRequest other = (ReportRequest) obj;
		if (getUuid() == null) {
			return false;
        }
		return getUuid().equals(other.getUuid());
	}

	/**
	 * @see Object#hashCode()
	 */
	public int hashCode() {
        return (getUuid() == null ? super.hashCode() : getUuid().hashCode());
	}

	//*****  PROPERTY ACCESS ******

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

    /**
     * @return the baseCohort
     */
    public Mapped<CohortDefinition> getBaseCohort() {
    	if (baseCohort == null && StringUtils.isNotBlank(baseCohortUuid)) {
			baseCohort = mappedDefinition(CohortDefinition.class, baseCohortUuid, baseCohortParameterMappings);
		}
    	return baseCohort;
    }

    /**
     * @param baseCohort the baseCohort to set
     */
    public void setBaseCohort(Mapped<CohortDefinition> baseCohort) {
    	this.baseCohort = baseCohort;
    	this.baseCohortUuid = getMappedUuid(baseCohort);
    	this.baseCohortParameterMappings = serializeParameterMappings(baseCohort);
    }

	/**
     * @return the reportDefinition
     */
    public Mapped<ReportDefinition> getReportDefinition() {
    	if (reportDefinition == null && StringUtils.isNotBlank(reportDefinitionUuid)) {
			reportDefinition = mappedDefinition(ReportDefinition.class, reportDefinitionUuid, reportDefinitionParameterMappings);
		}
    	return reportDefinition;
    }

    /**
     * @param reportDefinition the reportDefinition to set
     */
    public void setReportDefinition(Mapped<ReportDefinition> reportDefinition) {
    	this.reportDefinition = reportDefinition;
    	this.reportDefinitionUuid = getMappedUuid(reportDefinition);
    	this.reportDefinitionParameterMappings = serializeParameterMappings(reportDefinition);
    }

    /**
     * @return the renderingMode
     */
    public RenderingMode getRenderingMode() {
    	if (renderingMode == null && rendererType != null) {
			try {
				ReportRenderer renderer = rendererType.getDeclaredConstructor().newInstance();
				renderingMode = new RenderingMode(renderer, renderer.getClass().getSimpleName(), rendererArgument, null);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to instantiate report renderer " + rendererType, e);
			}
		}
    	return renderingMode;
    }

    /**
     * @param renderingMode the renderingMode to set
     */
    public void setRenderingMode(RenderingMode renderingMode) {
    	this.renderingMode = renderingMode;
    	this.rendererType = renderingMode == null || renderingMode.getRenderer() == null ? null : renderingMode.getRenderer().getClass();
    	this.rendererArgument = renderingMode == null ? null : renderingMode.getArgument();
    }

	public String getBaseCohortUuid() {
		return getMappedUuid(baseCohort, baseCohortUuid);
	}

	public void setBaseCohortUuid(String baseCohortUuid) {
		this.baseCohortUuid = baseCohortUuid;
		this.baseCohort = null;
	}

	public String getBaseCohortParameterMappings() {
		return baseCohort == null ? baseCohortParameterMappings : serializeParameterMappings(baseCohort);
	}

	public void setBaseCohortParameterMappings(String baseCohortParameterMappings) {
		this.baseCohortParameterMappings = baseCohortParameterMappings;
		this.baseCohort = null;
	}

	public String getReportDefinitionUuid() {
		return getMappedUuid(reportDefinition, reportDefinitionUuid);
	}

	public void setReportDefinitionUuid(String reportDefinitionUuid) {
		this.reportDefinitionUuid = reportDefinitionUuid;
		this.reportDefinition = null;
	}

	public String getReportDefinitionParameterMappings() {
		return reportDefinition == null ? reportDefinitionParameterMappings : serializeParameterMappings(reportDefinition);
	}

	public void setReportDefinitionParameterMappings(String reportDefinitionParameterMappings) {
		this.reportDefinitionParameterMappings = reportDefinitionParameterMappings;
		this.reportDefinition = null;
	}

	public Class<? extends ReportRenderer> getRendererType() {
		return renderingMode == null || renderingMode.getRenderer() == null ? rendererType : renderingMode.getRenderer().getClass();
	}

	public void setRendererType(Class<? extends ReportRenderer> rendererType) {
		this.rendererType = rendererType;
		this.renderingMode = null;
	}

	public String getRendererArgument() {
		return renderingMode == null ? rendererArgument : renderingMode.getArgument();
	}

	public void setRendererArgument(String rendererArgument) {
		this.rendererArgument = rendererArgument;
		this.renderingMode = null;
	}

    /**
     * @return the priority
     */
    public Priority getPriority() {
    	return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(Priority priority) {
    	this.priority = priority;
    }

	/**
	 * @return the schedule
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * @param schedule the schedule to set
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * @return the processAutomatically
	 */
	public boolean isProcessAutomatically() {
		return processAutomatically;
	}

	/**
	 * @param processAutomatically the processAutomatically to set
	 */
	public void setProcessAutomatically(boolean processAutomatically) {
		this.processAutomatically = processAutomatically;
	}

	/**
     * @return the requestedBy
     */
    public User getRequestedBy() {
    	return requestedBy;
    }

    /**
     * @param requestedBy the requestedBy to set
     */
    public void setRequestedBy(User requestedBy) {
    	this.requestedBy = requestedBy;
    }

    /**
     * @return the requestDate
     */
    public Date getRequestDate() {
    	return requestDate;
    }

    /**
     * @param requestDate the requestDate to set
     */
    public void setRequestDate(Date requestDate) {
    	this.requestDate = requestDate;
    }

	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * @return the evaluationDate
	 */
	public Date getEvaluationDate() {
		return evaluationDate;
	}

	/**
	 * @param evaluationDate the evaluationDate to set
	 */
	public void setEvaluationDate(Date evaluationDate) {
		this.evaluationDate = evaluationDate;
	}

	/**
	 * @return the evaluateStartDatetime
	 */
	public Date getEvaluateStartDatetime() {
		return evaluateStartDatetime;
	}

	/**
	 * @param evaluateStartDatetime the evaluateStartDatetime to set
	 */
	public void setEvaluateStartDatetime(Date evaluateStartDatetime) {
		this.evaluateStartDatetime = evaluateStartDatetime;
	}

	/**
	 * @return the evaluateCompleteDatetime
	 */
	public Date getEvaluateCompleteDatetime() {
		return evaluateCompleteDatetime;
	}

	/**
	 * @param evaluateCompleteDatetime the evaluateCompleteDatetime to set
	 */
	public void setEvaluateCompleteDatetime(Date evaluateCompleteDatetime) {
		this.evaluateCompleteDatetime = evaluateCompleteDatetime;
	}

	/**
	 * @return the renderCompleteDatetime
	 */
	public Date getRenderCompleteDatetime() {
		return renderCompleteDatetime;
	}

	/**
	 * @param renderCompleteDatetime the renderCompleteDatetime to set
	 */
	public void setRenderCompleteDatetime(Date renderCompleteDatetime) {
		this.renderCompleteDatetime = renderCompleteDatetime;
	}

    /**
     * @return the minimumDaysToPreserve
     */
    public Integer getMinimumDaysToPreserve() {
        return minimumDaysToPreserve;
    }

    /**
     * @param minimumDaysToPreserve the minimumDaysToPreserve to set
     */
    public void setMinimumDaysToPreserve(Integer minimumDaysToPreserve) {
        this.minimumDaysToPreserve = minimumDaysToPreserve;
    }

    /**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	//***** ENUMS *****

	/**
	 * Priority with which to run these reports. Generally speaking
	 * <ul>
	 * <li>a request that a User initiates that expects an interactive result (e.g. a WebRenderer)
	 * should get HIGHEST priority</li>
	 * <li>a request that a User initiates that expects a file download result should get HIGH
	 * priority</li>
	 * <li>a request created by a demon thread should have LOW or LOWEST priority.</li>
	 * </ul>
	 */
	public enum Priority {
		HIGHEST,
		HIGH,
		NORMAL,
		LOW,
		LOWEST
	}

	/**
	 * Represents the Status of a Report
	 */
	public enum Status {
		REQUESTED,
		SCHEDULED,
		PROCESSING,
		FAILED,
		COMPLETED,
		SAVED,
		SCHEDULE_COMPLETED
	}

	/**
	 * Compares 2 Requests By Priority, in order to determine which should be evaluated first
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 * @should compare by priority
	 * @should compare by request date when priority is the same
	 */
	public static class PriorityComparator implements Comparator<ReportRequest> {
		public int compare(ReportRequest r1, ReportRequest r2) {
			if (r1 == null && r2 == null) { return 0; }
			if (r1 == null) { return -1; }
			if (r2 == null) { return 1; }
			Date d1 = ObjectUtil.nvl(r1.getEvaluateCompleteDatetime(), r1.getEvaluateStartDatetime());
			Date d2 = ObjectUtil.nvl(r2.getEvaluateCompleteDatetime(), r2.getEvaluateStartDatetime());
			int ret = OpenmrsUtil.compareWithNullAsLatest(d1, d2);
			if (ret == 0) {
				ret = r1.getPriority().compareTo(r2.getPriority());
			}
			if (ret == 0) {
				ret = OpenmrsUtil.compareWithNullAsLatest(r1.getRequestDate(), r2.getRequestDate());
			}
		    if (ret == 0) {
		    	ret = OpenmrsUtil.compareWithNullAsGreatest(r1.getUuid(), r2.getUuid());
		    }
			return ret;
		}
	}

	private <T extends Definition> Mapped<T> mappedDefinition(Class<T> mappedType, String uuid, String serializedMappings) {
		T definition = DefinitionContext.getDefinitionByUuid(mappedType, uuid);
		Map<String, Object> mappings = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(serializedMappings)) {
			try {
				mappings = Context.getSerializationService()
						.deserialize(serializedMappings, Map.class, ReportingSerializer.class);
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to deserialize parameter mappings for " + mappedType.getName(), e);
			}
		}
		return new Mapped<T>(definition, mappings);
	}

	private String getMappedUuid(Mapped<? extends Parameterizable> mapped) {
		return getMappedUuid(mapped, null);
	}

	private String getMappedUuid(Mapped<? extends Parameterizable> mapped, String currentUuid) {
		return mapped == null ? currentUuid : mapped.getUuidOfMappedOpenmrsObject();
	}

	private String serializeParameterMappings(Mapped<? extends Parameterizable> mapped) {
		if (mapped == null || mapped.getParameterMappings() == null || mapped.getParameterMappings().isEmpty()) {
			return null;
		}
		try {
			return Context.getSerializationService()
					.serialize(mapped.getParameterMappings(), ReportingSerializer.class);
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to serialize parameter mappings", e);
		}
	}
}
