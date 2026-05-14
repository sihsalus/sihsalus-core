package org.sihsalus.module.htmlwidgets;

import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsService;
import org.openmrs.module.htmlwidgets.service.HtmlWidgetsServiceImpl;
import org.openmrs.module.htmlwidgets.service.db.HibernateHtmlWidgetsDAO;
import org.openmrs.module.htmlwidgets.service.db.HtmlWidgetsDAO;
import org.openmrs.module.htmlwidgets.web.controller.ConceptSearchController;
import org.openmrs.module.htmlwidgets.web.handler.BooleanHandler;
import org.openmrs.module.htmlwidgets.web.handler.ClassHandler;
import org.openmrs.module.htmlwidgets.web.handler.CodedHandler;
import org.openmrs.module.htmlwidgets.web.handler.CollectionHandler;
import org.openmrs.module.htmlwidgets.web.handler.ConceptHandler;
import org.openmrs.module.htmlwidgets.web.handler.DateHandler;
import org.openmrs.module.htmlwidgets.web.handler.DoubleHandler;
import org.openmrs.module.htmlwidgets.web.handler.DrugHandler;
import org.openmrs.module.htmlwidgets.web.handler.EncounterTypeHandler;
import org.openmrs.module.htmlwidgets.web.handler.EnumHandler;
import org.openmrs.module.htmlwidgets.web.handler.FormHandler;
import org.openmrs.module.htmlwidgets.web.handler.IntegerHandler;
import org.openmrs.module.htmlwidgets.web.handler.LocationHandler;
import org.openmrs.module.htmlwidgets.web.handler.ObjectHandler;
import org.openmrs.module.htmlwidgets.web.handler.OpenmrsMetadataHandler;
import org.openmrs.module.htmlwidgets.web.handler.OrderTypeHandler;
import org.openmrs.module.htmlwidgets.web.handler.PatientHandler;
import org.openmrs.module.htmlwidgets.web.handler.PatientIdentifierTypeHandler;
import org.openmrs.module.htmlwidgets.web.handler.PersonAttributeTypeHandler;
import org.openmrs.module.htmlwidgets.web.handler.PersonHandler;
import org.openmrs.module.htmlwidgets.web.handler.ProgramHandler;
import org.openmrs.module.htmlwidgets.web.handler.ProgramWorkflowHandler;
import org.openmrs.module.htmlwidgets.web.handler.ProgramWorkflowStateHandler;
import org.openmrs.module.htmlwidgets.web.handler.PropertiesHandler;
import org.openmrs.module.htmlwidgets.web.handler.StringHandler;
import org.openmrs.module.htmlwidgets.web.handler.UserHandler;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ConceptSearchController.class)
public class SihsalusHtmlWidgetsConfiguration {

    @Bean
    HtmlWidgetsDAO htmlWidgetsDao(DbSessionFactory dbSessionFactory) {
        HibernateHtmlWidgetsDAO dao = new HibernateHtmlWidgetsDAO();
        dao.setSessionFactory(dbSessionFactory);
        return dao;
    }

    @Bean
    HtmlWidgetsService htmlWidgetsService(HtmlWidgetsDAO htmlWidgetsDao) {
        HtmlWidgetsServiceImpl service = new HtmlWidgetsServiceImpl();
        service.setDao(htmlWidgetsDao);
        return service;
    }

    @Bean
    SmartInitializingSingleton htmlWidgetsServiceRegistrar(
            ServiceContext serviceContext, HtmlWidgetsService htmlWidgetsService) {
        return () -> serviceContext.setService(HtmlWidgetsService.class, htmlWidgetsService);
    }

    @Bean
    BooleanHandler htmlWidgetsBooleanHandler() {
        return new BooleanHandler();
    }

    @Bean
    ClassHandler htmlWidgetsClassHandler() {
        return new ClassHandler();
    }

    @Bean
    CodedHandler htmlWidgetsCodedHandler() {
        return new CodedHandler();
    }

    @Bean
    CollectionHandler htmlWidgetsCollectionHandler() {
        return new CollectionHandler();
    }

    @Bean
    ConceptHandler htmlWidgetsConceptHandler() {
        return new ConceptHandler();
    }

    @Bean
    DateHandler htmlWidgetsDateHandler() {
        return new DateHandler();
    }

    @Bean
    DoubleHandler htmlWidgetsDoubleHandler() {
        return new DoubleHandler();
    }

    @Bean
    DrugHandler htmlWidgetsDrugHandler() {
        return new DrugHandler();
    }

    @Bean
    EncounterTypeHandler htmlWidgetsEncounterTypeHandler() {
        return new EncounterTypeHandler();
    }

    @Bean
    EnumHandler htmlWidgetsEnumHandler() {
        return new EnumHandler();
    }

    @Bean
    FormHandler htmlWidgetsFormHandler() {
        return new FormHandler();
    }

    @Bean
    IntegerHandler htmlWidgetsIntegerHandler() {
        return new IntegerHandler();
    }

    @Bean
    LocationHandler htmlWidgetsLocationHandler() {
        return new LocationHandler();
    }

    @Bean
    ObjectHandler htmlWidgetsObjectHandler() {
        return new ObjectHandler();
    }

    @Bean
    OpenmrsMetadataHandler htmlWidgetsOpenmrsMetadataHandler() {
        return new OpenmrsMetadataHandler();
    }

    @Bean
    OrderTypeHandler htmlWidgetsOrderTypeHandler() {
        return new OrderTypeHandler();
    }

    @Bean
    PatientHandler htmlWidgetsPatientHandler() {
        return new PatientHandler();
    }

    @Bean
    PatientIdentifierTypeHandler htmlWidgetsPatientIdentifierTypeHandler() {
        return new PatientIdentifierTypeHandler();
    }

    @Bean
    PersonAttributeTypeHandler htmlWidgetsPersonAttributeTypeHandler() {
        return new PersonAttributeTypeHandler();
    }

    @Bean
    PersonHandler htmlWidgetsPersonHandler() {
        return new PersonHandler();
    }

    @Bean
    ProgramHandler htmlWidgetsProgramHandler() {
        return new ProgramHandler();
    }

    @Bean
    ProgramWorkflowHandler htmlWidgetsProgramWorkflowHandler() {
        return new ProgramWorkflowHandler();
    }

    @Bean
    ProgramWorkflowStateHandler htmlWidgetsProgramWorkflowStateHandler() {
        return new ProgramWorkflowStateHandler();
    }

    @Bean
    PropertiesHandler htmlWidgetsPropertiesHandler() {
        return new PropertiesHandler();
    }

    @Bean
    StringHandler htmlWidgetsStringHandler() {
        return new StringHandler();
    }

    @Bean
    UserHandler htmlWidgetsUserHandler() {
        return new UserHandler();
    }
}
