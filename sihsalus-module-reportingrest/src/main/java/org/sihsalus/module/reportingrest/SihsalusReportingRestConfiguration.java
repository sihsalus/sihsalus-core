package org.sihsalus.module.reportingrest;

import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.reporting.report.task.ReportingTimerTask;
import org.openmrs.module.reportingrest.adhoc.task.DeleteOldOldAdHocReportDefinitionsTask;
import org.openmrs.module.reportingrest.web.controller.ReportingRestController;
import org.openmrs.module.reportingrest.web.resource.ReportDefinitionResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {ReportingRestController.class, ReportDefinitionResource.class})
public class SihsalusReportingRestConfiguration {

    @Bean
    ReportingTimerTask deleteOldAdHocReportDefinitionsTask(DbSessionFactory dbSessionFactory) {
        ReportingTimerTask task = new ReportingTimerTask();
        task.setTaskClass(DeleteOldOldAdHocReportDefinitionsTask.class);
        task.setSessionFactory(dbSessionFactory);
        return task;
    }
}
