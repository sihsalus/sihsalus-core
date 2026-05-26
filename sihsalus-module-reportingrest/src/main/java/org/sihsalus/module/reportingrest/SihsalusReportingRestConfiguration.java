package org.sihsalus.module.reportingrest;

import org.openmrs.annotation.Handler;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.reporting.report.task.ReportingTimerTask;
import org.openmrs.module.reportingrest.adhoc.AdHocExportManager;
import org.openmrs.module.reportingrest.adhoc.task.DeleteOldOldAdHocReportDefinitionsTask;
import org.openmrs.module.reportingrest.web.controller.ReportingRestController;
import org.openmrs.module.reportingrest.web.resource.ReportDefinitionResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackageClasses = {
      AdHocExportManager.class,
      ReportingRestController.class,
      ReportDefinitionResource.class
    },
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Handler.class))
public class SihsalusReportingRestConfiguration {

  @Bean
  ReportingTimerTask deleteOldAdHocReportDefinitionsTask(DbSessionFactory dbSessionFactory) {
    ReportingTimerTask task = new ReportingTimerTask();
    task.setTaskClass(DeleteOldOldAdHocReportDefinitionsTask.class);
    task.setSessionFactory(dbSessionFactory);
    return task;
  }
}
