package org.sihsalus.module.billing;

import java.util.List;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.event.Event;
import org.openmrs.module.billing.api.BillDiscountService;
import org.openmrs.module.billing.api.BillExemptionService;
import org.openmrs.module.billing.api.BillLineItemService;
import org.openmrs.module.billing.api.BillRefundService;
import org.openmrs.module.billing.api.BillService;
import org.openmrs.module.billing.api.BillableServiceService;
import org.openmrs.module.billing.api.CashPointService;
import org.openmrs.module.billing.api.CashierItemPriceService;
import org.openmrs.module.billing.api.ICashierOptionsService;
import org.openmrs.module.billing.api.IPaymentModeAttributeTypeService;
import org.openmrs.module.billing.api.ISequentialReceiptNumberGeneratorService;
import org.openmrs.module.billing.api.ITimesheetService;
import org.openmrs.module.billing.api.ItemPriceService;
import org.openmrs.module.billing.api.PaymentModeService;
import org.openmrs.module.billing.api.base.entity.db.hibernate.BaseHibernateRepository;
import org.openmrs.module.billing.api.base.entity.db.hibernate.BaseHibernateRepositoryImpl;
import org.openmrs.module.billing.api.billing.BillingEventListener;
import org.openmrs.module.billing.api.billing.OrderBillingEventListener;
import org.openmrs.module.billing.api.billing.impl.DrugOrderBillingStrategy;
import org.openmrs.module.billing.api.billing.impl.TestOrderBillingStrategy;
import org.openmrs.module.billing.api.db.BillDAO;
import org.openmrs.module.billing.api.db.BillDiscountDAO;
import org.openmrs.module.billing.api.db.BillExemptionDAO;
import org.openmrs.module.billing.api.db.BillLineItemDAO;
import org.openmrs.module.billing.api.db.BillRefundDAO;
import org.openmrs.module.billing.api.db.BillableServiceDAO;
import org.openmrs.module.billing.api.db.CashPointDAO;
import org.openmrs.module.billing.api.db.CashierItemPriceDAO;
import org.openmrs.module.billing.api.db.PaymentModeDAO;
import org.openmrs.module.billing.api.db.hibernate.BillExemptionDAOImpl;
import org.openmrs.module.billing.api.db.hibernate.HibernateBillDAO;
import org.openmrs.module.billing.api.db.hibernate.HibernateBillDiscountDAO;
import org.openmrs.module.billing.api.db.hibernate.HibernateBillLineItemDAO;
import org.openmrs.module.billing.api.db.hibernate.HibernateBillRefundDAO;
import org.openmrs.module.billing.api.db.hibernate.HibernateBillableServiceDAOImpl;
import org.openmrs.module.billing.api.db.hibernate.HibernateCashPointDAOImpl;
import org.openmrs.module.billing.api.db.hibernate.HibernateCashierItemPriceDAOImpl;
import org.openmrs.module.billing.api.db.hibernate.HibernatePaymentModeDAOImpl;
import org.openmrs.module.billing.api.evaluator.ExemptionEvaluator;
import org.openmrs.module.billing.api.evaluator.ExemptionRuleEngine;
import org.openmrs.module.billing.api.evaluator.impl.JSExemptionEvaluator;
import org.openmrs.module.billing.api.impl.BillDiscountServiceImpl;
import org.openmrs.module.billing.api.impl.BillExemptionServiceImpl;
import org.openmrs.module.billing.api.impl.BillLineItemServiceImpl;
import org.openmrs.module.billing.api.impl.BillRefundServiceImpl;
import org.openmrs.module.billing.api.impl.BillServiceImpl;
import org.openmrs.module.billing.api.impl.BillableServiceServiceImpl;
import org.openmrs.module.billing.api.impl.CashPointServiceImpl;
import org.openmrs.module.billing.api.impl.CashierItemPriceServiceImpl;
import org.openmrs.module.billing.api.impl.CashierOptionsServiceGpImpl;
import org.openmrs.module.billing.api.impl.ItemPriceServiceImpl;
import org.openmrs.module.billing.api.impl.PaymentModeAttributeTypeServiceImpl;
import org.openmrs.module.billing.api.impl.PaymentModeServiceImpl;
import org.openmrs.module.billing.api.impl.SequentialReceiptNumberGeneratorServiceImpl;
import org.openmrs.module.billing.api.impl.TimesheetServiceImpl;
import org.sihsalus.core.api.HibernateMappingContributor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "org.openmrs.module.billing.web.rest",
    "org.openmrs.module.billing.web.base.resource",
    "org.openmrs.module.billing.api.db.hibernate"
})
public class SihsalusBillingConfiguration {

    @Bean
    static BeanFactoryPostProcessor billingWebBeansDependOnServices() {
        return beanFactory -> {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
                String className = definition.getBeanClassName();
                if (className != null
                        && (className.startsWith("org.openmrs.module.billing.web.rest.")
                                || className.startsWith("org.openmrs.module.billing.web.base.resource."))) {
                    definition.setDependsOn("billingServicesRegistered");
                }
            }
        };
    }

    @Bean
    HibernateMappingContributor billingHibernateMappingContributor() {
        return () -> List.of("Bill.hbm.xml", "Cashier.hbm.xml", "SequentialReceiptNumberGenerator.hbm.xml");
    }

    @Bean
    BaseHibernateRepository genericRepositoryDao(DbSessionFactory dbSessionFactory) {
        return new BaseHibernateRepositoryImpl(dbSessionFactory);
    }

    @Bean
    BillLineItemDAO billLineItemDAO(SessionFactory sessionFactory) {
        return new HibernateBillLineItemDAO(sessionFactory);
    }

    @Bean
    BillDAO billDAO(SessionFactory sessionFactory) {
        return new HibernateBillDAO(sessionFactory);
    }

    @Bean
    BillExemptionDAO billExemptionDAO(SessionFactory sessionFactory) {
        return new BillExemptionDAOImpl(sessionFactory);
    }

    @Bean
    BillDiscountDAO billDiscountDAO(SessionFactory sessionFactory) {
        return new HibernateBillDiscountDAO(sessionFactory);
    }

    @Bean
    BillRefundDAO billRefundDAO(SessionFactory sessionFactory) {
        return new HibernateBillRefundDAO(sessionFactory);
    }

    @Bean
    BillableServiceDAO billableServiceDAO(SessionFactory sessionFactory) {
        return new HibernateBillableServiceDAOImpl(sessionFactory);
    }

    @Bean
    PaymentModeDAO paymentModeDAO(SessionFactory sessionFactory) {
        return new HibernatePaymentModeDAOImpl(sessionFactory);
    }

    @Bean
    CashPointDAO cashPointDAO(SessionFactory sessionFactory) {
        return new HibernateCashPointDAOImpl(sessionFactory);
    }

    @Bean
    CashierItemPriceDAO cashierItemPriceDAO(SessionFactory sessionFactory) {
        return new HibernateCashierItemPriceDAOImpl(sessionFactory);
    }

    @Bean
    ItemPriceService itemPriceService(BaseHibernateRepository genericRepositoryDao) {
        ItemPriceServiceImpl service = new ItemPriceServiceImpl();
        service.setRepository(genericRepositoryDao);
        return service;
    }

    @Bean
    PaymentModeService paymentModeService(PaymentModeDAO paymentModeDAO) {
        PaymentModeServiceImpl service = new PaymentModeServiceImpl();
        service.setPaymentModeDAO(paymentModeDAO);
        return service;
    }

    @Bean
    IPaymentModeAttributeTypeService cashierPaymentModeAttributeTypeService(
            BaseHibernateRepository genericRepositoryDao) {
        PaymentModeAttributeTypeServiceImpl service = new PaymentModeAttributeTypeServiceImpl();
        service.setRepository(genericRepositoryDao);
        return service;
    }

    @Bean
    CashPointService cashPointService(CashPointDAO cashPointDAO) {
        CashPointServiceImpl service = new CashPointServiceImpl();
        service.setCashPointDAO(cashPointDAO);
        return service;
    }

    @Bean
    ITimesheetService cashierTimesheetService(BaseHibernateRepository genericRepositoryDao) {
        TimesheetServiceImpl service = new TimesheetServiceImpl();
        service.setRepository(genericRepositoryDao);
        return service;
    }

    @Bean
    ISequentialReceiptNumberGeneratorService seqReceiptNumberGeneratorService(
            BaseHibernateRepository genericRepositoryDao) {
        SequentialReceiptNumberGeneratorServiceImpl service = new SequentialReceiptNumberGeneratorServiceImpl();
        service.setRepository(genericRepositoryDao);
        return service;
    }

    @Bean
    ICashierOptionsService cashierOptionsService() {
        return new CashierOptionsServiceGpImpl();
    }

    @Bean
    BillableServiceService billableServiceService(BillableServiceDAO billableServiceDAO) {
        BillableServiceServiceImpl service = new BillableServiceServiceImpl();
        service.setBillableServiceDAO(billableServiceDAO);
        return service;
    }

    @Bean
    CashierItemPriceService cashierItemPriceService(CashierItemPriceDAO cashierItemPriceDAO) {
        CashierItemPriceServiceImpl service = new CashierItemPriceServiceImpl();
        service.setCashierItemPriceDAO(cashierItemPriceDAO);
        return service;
    }

    @Bean
    BillService billService(BillDAO billDAO) {
        BillServiceImpl service = new BillServiceImpl();
        service.setBillDAO(billDAO);
        return service;
    }

    @Bean
    BillLineItemService billLineItemService(BillLineItemDAO billLineItemDAO) {
        BillLineItemServiceImpl service = new BillLineItemServiceImpl();
        service.setBillLineItemDAO(billLineItemDAO);
        return service;
    }

    @Bean
    BillExemptionService billingExemptionService(BillExemptionDAO billExemptionDAO) {
        return new BillExemptionServiceImpl(billExemptionDAO);
    }

    @Bean
    BillDiscountService billDiscountService(BillDiscountDAO billDiscountDAO) {
        return new BillDiscountServiceImpl(billDiscountDAO);
    }

    @Bean
    BillRefundService billRefundService(BillRefundDAO billRefundDAO) {
        return new BillRefundServiceImpl(billRefundDAO);
    }

    @Bean
    ExemptionEvaluator javascriptRuleEvaluator() {
        return new JSExemptionEvaluator();
    }

    @Bean
    ExemptionRuleEngine ruleEngine(ExemptionEvaluator javascriptRuleEvaluator) {
        return new ExemptionRuleEngine(List.of(javascriptRuleEvaluator));
    }

    @Bean
    BillingEventListener orderBillingEventListener() {
        return new OrderBillingEventListener();
    }

    @Bean
    SmartInitializingSingleton billingEventListenerSubscriber(List<BillingEventListener> billingEventListeners) {
        return () -> billingEventListeners.forEach(listener -> Event.subscribe(
                listener.getSubscribedClass(), listener.getSubscribedAction().name(), listener));
    }

    @Bean
    DrugOrderBillingStrategy drugOrderBillingStrategy() {
        return new DrugOrderBillingStrategy();
    }

    @Bean
    TestOrderBillingStrategy testOrderBillingStrategy() {
        return new TestOrderBillingStrategy();
    }

    @Bean
    Object billingServicesRegistered(
            ServiceContext serviceContext,
            ItemPriceService itemPriceService,
            BillLineItemService billLineItemService,
            PaymentModeService paymentModeService,
            IPaymentModeAttributeTypeService cashierPaymentModeAttributeTypeService,
            CashPointService cashPointService,
            ITimesheetService cashierTimesheetService,
            ISequentialReceiptNumberGeneratorService seqReceiptNumberGeneratorService,
            ICashierOptionsService cashierOptionsService,
            BillableServiceService billableServiceService,
            CashierItemPriceService cashierItemPriceService,
            BillService billService,
            BillExemptionService billingExemptionService,
            BillDiscountService billDiscountService,
            BillRefundService billRefundService) {
        serviceContext.setService(ItemPriceService.class, itemPriceService);
        serviceContext.setService(BillLineItemService.class, billLineItemService);
        serviceContext.setService(PaymentModeService.class, paymentModeService);
        serviceContext.setService(IPaymentModeAttributeTypeService.class, cashierPaymentModeAttributeTypeService);
        serviceContext.setService(CashPointService.class, cashPointService);
        serviceContext.setService(ITimesheetService.class, cashierTimesheetService);
        serviceContext.setService(ISequentialReceiptNumberGeneratorService.class, seqReceiptNumberGeneratorService);
        serviceContext.setService(ICashierOptionsService.class, cashierOptionsService);
        serviceContext.setService(BillableServiceService.class, billableServiceService);
        serviceContext.setService(CashierItemPriceService.class, cashierItemPriceService);
        serviceContext.setService(BillService.class, billService);
        serviceContext.setService(BillExemptionService.class, billingExemptionService);
        serviceContext.setService(BillDiscountService.class, billDiscountService);
        serviceContext.setService(BillRefundService.class, billRefundService);
        return new Object();
    }
}
