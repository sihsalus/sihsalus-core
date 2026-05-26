package org.sihsalus.module.stockmanagement;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.stockmanagement.StockLocationTags;
import org.openmrs.module.stockmanagement.api.Privileges;
import org.openmrs.module.stockmanagement.api.StockManagementException;
import org.openmrs.module.stockmanagement.api.dto.BatchJobDTO;
import org.openmrs.module.stockmanagement.api.dto.BatchJobOwnerDTO;
import org.openmrs.module.stockmanagement.api.dto.BatchJobSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.DispenseRequest;
import org.openmrs.module.stockmanagement.api.dto.ImportResult;
import org.openmrs.module.stockmanagement.api.dto.OrderItemDTO;
import org.openmrs.module.stockmanagement.api.dto.OrderItemSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.PartyDTO;
import org.openmrs.module.stockmanagement.api.dto.PartySearchFilter;
import org.openmrs.module.stockmanagement.api.dto.PrivilegeScope;
import org.openmrs.module.stockmanagement.api.dto.RecordPrivilegeFilter;
import org.openmrs.module.stockmanagement.api.dto.Result;
import org.openmrs.module.stockmanagement.api.dto.SessionInfo;
import org.openmrs.module.stockmanagement.api.dto.StockBatchDTO;
import org.openmrs.module.stockmanagement.api.dto.StockBatchSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockInventoryResult;
import org.openmrs.module.stockmanagement.api.dto.StockItemDTO;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventory;
import org.openmrs.module.stockmanagement.api.dto.StockItemInventorySearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUOMDTO;
import org.openmrs.module.stockmanagement.api.dto.StockItemPackagingUOMSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockItemSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockItemTransactionDTO;
import org.openmrs.module.stockmanagement.api.dto.StockItemTransactionSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockOperationAction;
import org.openmrs.module.stockmanagement.api.dto.StockOperationActionLineItem;
import org.openmrs.module.stockmanagement.api.dto.StockOperationBatchNumbersDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemCost;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationItemSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockOperationLinkDTO;
import org.openmrs.module.stockmanagement.api.dto.StockOperationSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockRuleCurrentQuantity;
import org.openmrs.module.stockmanagement.api.dto.StockRuleNotificationUser;
import org.openmrs.module.stockmanagement.api.dto.StockRuleDTO;
import org.openmrs.module.stockmanagement.api.dto.StockRuleSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.StockSourceSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeLocationDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeOperationTypeDTO;
import org.openmrs.module.stockmanagement.api.dto.UserRoleScopeSearchFilter;
import org.openmrs.module.stockmanagement.api.dto.reporting.DispensingLineFilter;
import org.openmrs.module.stockmanagement.api.dto.reporting.DispensingLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.Fullfillment;
import org.openmrs.module.stockmanagement.api.dto.reporting.PrescriptionLineFilter;
import org.openmrs.module.stockmanagement.api.dto.reporting.PrescriptionLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockBatchLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockExpiryFilter;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItem;
import org.openmrs.module.stockmanagement.api.dto.reporting.StockOperationLineItemFilter;
import org.openmrs.module.stockmanagement.api.jobs.StockBatchExpiryJob;
import org.openmrs.module.stockmanagement.api.jobs.StockItemImportJob;
import org.openmrs.module.stockmanagement.api.jobs.StockRuleEvaluationJob;
import org.openmrs.module.stockmanagement.api.model.BatchJob;
import org.openmrs.module.stockmanagement.api.model.BatchJobOwner;
import org.openmrs.module.stockmanagement.api.model.BatchJobStatus;
import org.openmrs.module.stockmanagement.api.model.LocationTree;
import org.openmrs.module.stockmanagement.api.model.OrderItem;
import org.openmrs.module.stockmanagement.api.model.Party;
import org.openmrs.module.stockmanagement.api.model.ReservedTransaction;
import org.openmrs.module.stockmanagement.api.model.StockBatch;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.stockmanagement.api.model.StockItemPackagingUOM;
import org.openmrs.module.stockmanagement.api.model.StockItemReference;
import org.openmrs.module.stockmanagement.api.model.StockItemTransaction;
import org.openmrs.module.stockmanagement.api.model.StockOperation;
import org.openmrs.module.stockmanagement.api.model.StockOperationItem;
import org.openmrs.module.stockmanagement.api.model.StockOperationLink;
import org.openmrs.module.stockmanagement.api.model.StockOperationStatus;
import org.openmrs.module.stockmanagement.api.model.StockOperationType;
import org.openmrs.module.stockmanagement.api.model.StockOperationTypeLocationScope;
import org.openmrs.module.stockmanagement.api.model.StockRule;
import org.openmrs.module.stockmanagement.api.model.StockSource;
import org.openmrs.module.stockmanagement.api.model.UserRoleScope;
import org.openmrs.module.stockmanagement.api.model.UserRoleScopeLocation;
import org.openmrs.module.stockmanagement.api.model.UserRoleScopeOperationType;
import org.openmrs.module.stockmanagement.api.reporting.Report;
import org.openmrs.module.stockmanagement.api.utils.DateUtil;
import org.openmrs.module.stockmanagement.api.utils.GlobalProperties;
import org.openmrs.module.stockmanagement.api.utils.SmtpUtil;
import org.openmrs.notification.Alert;
import org.openmrs.notification.Template;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings({ "unchecked", "rawtypes" })
class PartialStockManagementService implements InvocationHandler {

    private static final Log log = LogFactory.getLog(PartialStockManagementService.class);

    private final SessionFactory sessionFactory;

    private final PlatformTransactionManager transactionManager;

    PartialStockManagementService(SessionFactory sessionFactory, TransactionManager transactionManager) {
        this.sessionFactory = sessionFactory;
        if (!(transactionManager instanceof PlatformTransactionManager platformTransactionManager)) {
            throw new IllegalArgumentException(
                    "Stock Management requires a PlatformTransactionManager-compatible transaction manager");
        }
        this.transactionManager = platformTransactionManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Object[] safeArgs = args == null ? new Object[0] : args;
        String methodName = method.getName();
        if ("onStartup".equals(methodName) || "onShutdown".equals(methodName)) {
            return null;
        }
        if ("toString".equals(methodName)) {
            return "Static StockManagementService Hibernate 7 partial implementation";
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(methodName)) {
            return safeArgs.length == 1 && proxy == safeArgs[0];
        }

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Transactional annotation = method.getAnnotation(Transactional.class);
        transaction.setReadOnly(annotation != null ? annotation.readOnly() : looksReadOnly(methodName));
        return transaction.execute(status -> dispatch(method, safeArgs));
    }

    private Object dispatch(Method method, Object[] args) {
        String name = method.getName();
        switch (name) {
            case "getCompleteLocationTree":
                return args.length == 0 ? listAll(LocationTree.class) : getCompleteLocationTree((Integer) args[0]);
            case "deleteLocationTreeNodes":
                deleteLocationTreeNodes((List<LocationTree>) args[0]);
                return null;
            case "deleteLocation":
                deleteLocation((String) args[0]);
                return null;
            case "saveLocationTreeNodes":
                saveAll((List<LocationTree>) args[0]);
                return null;
            case "getCompletePartyList":
                return getCompletePartyList((Integer) args[0], false);
            case "getCompleteStockDispensingLocationPartyList":
                return getCompletePartyList((Integer) args[0], true);
            case "getMainPharmacyLocations":
                return getLocationsByTag(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG);
            case "getMainPharmacyPartyList":
                return partiesForLocations(getMainPharmacyLocationIds());
            case "getAllStockHoldingPartyList":
                return getAllStockHoldingPartyList();
            case "findUserRoleScopes":
                return findUserRoleScopes((UserRoleScopeSearchFilter) args[0]);
            case "getStockOperationByUuid":
                return byUuid(StockOperation.class, (String) args[0]);
            case "saveStockOperation":
                return saveStockOperation((StockOperationDTO) args[0]);
            case "submitStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.SUBMIT, null);
                return null;
            case "completeStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.COMPLETE, null);
                return null;
            case "approveStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.APPROVE, null);
                return null;
            case "dispatchStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.DISPATCH, null);
                return null;
            case "rejectStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.REJECT,
                    (String) args[1]);
                return null;
            case "returnStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.RETURN,
                    (String) args[1]);
                return null;
            case "cancelStockOperation":
                processStockOperationAction((StockOperationDTO) args[0], StockOperationAction.Action.CANCEL,
                    (String) args[1]);
                return null;
            case "stockOperationItemsReceived":
                stockOperationItemsReceived((StockOperationDTO) args[0], (List<StockOperationActionLineItem>) args[1]);
                return null;
            case "saveStockOperationBatchNumbers":
                return saveStockOperationBatchNumbers((StockOperationBatchNumbersDTO) args[0]);
            case "dispenseStockItems":
                dispenseStockItems((List<DispenseRequest>) args[0]);
                return null;
            case "getStockOperationItemsByStockOperation":
                return getStockOperationItemsByStockOperation((Integer) args[0]);
            case "findStockOperationItems":
                return findStockOperationItems((StockOperationItemSearchFilter) args[0]);
            case "getStockOperationItemCosts":
                return getStockOperationItemCosts((StockOperationItemSearchFilter) args[0]);
            case "findStockOperationLinks":
                return findStockOperationLinks((String) args[0], null);
            case "getStockOperationTypeByUuid":
                return byUuid(StockOperationType.class, (String) args[0]);
            case "getStockOperationTypeByType":
                return stockOperationTypeByType((String) args[0]);
            case "getAllStockOperationTypes":
                return sortedById(listAll(StockOperationType.class));
            case "getStockOperationTypeLocationScopeByUuid":
                return byUuid(StockOperationTypeLocationScope.class, (String) args[0]);
            case "getAllStockOperationTypeLocationScopes":
                return sortedById(listAll(StockOperationTypeLocationScope.class));
            case "getUserRoleScopeOperationTypeByUuid":
                return byUuid(UserRoleScopeOperationType.class, (String) args[0]);
            case "getUserRoleScopeByUuid":
                return byUuid(UserRoleScope.class, (String) args[0]);
            case "saveUserRoleScope":
                if (args[0] instanceof UserRoleScope) {
                    return saveOrUpdate(args[0]);
                }
                if (args[0] instanceof UserRoleScopeDTO) {
                    return saveUserRoleScope((UserRoleScopeDTO) args[0]);
                }
                throw unsupported(method);
            case "saveUserRoleScopeLocation":
            case "saveUserRoleScopeOperationType":
            case "saveOrderItem":
            case "saveStockItemReference":
                return saveOrUpdate(args[0]);
            case "voidUserRoleScopes":
                voidByUuids(UserRoleScope.class, (List<String>) args[0], (String) args[1], null);
                return null;
            case "voidUserRoleScopeLocations":
                voidByUuids(UserRoleScopeLocation.class, (List<String>) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "voidUserRoleScopeOperationTypes":
                voidByUuids(UserRoleScopeOperationType.class, (List<String>) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "findStockItemEntities":
                return findStockItemEntities((StockItemSearchFilter) args[0]);
            case "searchStockItemCommonName":
                return searchStockItemCommonName((String) args[0], (Boolean) args[1], (Boolean) args[2],
                    (Integer) args[3]);
            case "findStockItems":
                return findStockItems((StockItemSearchFilter) args[0]);
            case "importStockItems":
                return importStockItems((Path) args[0], (Boolean) args[1]);
            case "saveStockItem":
                return saveStockItem(args);
            case "saveStockRule":
                return saveStockRule((StockRuleDTO) args[0]);
            case "findStockRules":
                return args.length > 1
                        ? findStockRules((StockRuleSearchFilter) args[0], (HashSet<RecordPrivilegeFilter>) args[1])
                        : findStockRules((StockRuleSearchFilter) args[0], null);
            case "getStockRuleByUuid":
                return byUuid(StockRule.class, (String) args[0]);
            case "voidStockRules":
                voidByUuids(StockRule.class, (List<String>) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "getStockItemByUuid":
                return byUuid(StockItem.class, (String) args[0]);
            case "findStockOperations":
                return args.length > 1
                        ? findStockOperations((StockOperationSearchFilter) args[0],
                            (HashSet<RecordPrivilegeFilter>) args[1])
                        : findStockOperations((StockOperationSearchFilter) args[0], null);
            case "findStockItemTransactions":
                return findStockItemTransactions((StockItemTransactionSearchFilter) args[0], null);
            case "getStockBatchLocationInventory":
                return getStockBatchLocationInventory((List<Integer>) args[0]);
            case "getStockInventory":
                if (args.length > 3) {
                    getStockInventory((StockItemInventorySearchFilter) args[0],
                        (HashSet<RecordPrivilegeFilter>) args[1], (Function<StockItemInventory, Boolean>) args[2],
                        (Class<StockItemInventory>) args[3]);
                    return null;
                }
                return args.length > 1
                        ? getStockInventory((StockItemInventorySearchFilter) args[0],
                            (HashSet<RecordPrivilegeFilter>) args[1])
                        : getStockInventory((StockItemInventorySearchFilter) args[0], null);
            case "getStockInventoryForecastData":
                getStockInventoryForecastData((StockItemInventorySearchFilter) args[0],
                    (Function<Object[], Boolean>) args[1]);
                return null;
            case "getStockInventoryExpiryForecastData":
                getStockInventoryExpiryForecastData((StockItemInventorySearchFilter) args[0],
                    (Function<Object[], Boolean>) args[1]);
                return null;
            case "postProcessInventoryResult":
                return postProcessInventoryResult((StockItemInventorySearchFilter) args[0],
                    (StockInventoryResult) args[1]);
            case "setStockItemInformation":
                setStockItemInformation((List<StockItemInventory>) args[0]);
                return null;
            case "getLeastMovingStockInventory":
                return getMovingStockInventory((StockItemInventorySearchFilter) args[0], false);
            case "getMostMovingStockInventory":
                return getMovingStockInventory((StockItemInventorySearchFilter) args[0], true);
            case "getParentStockOperationLinks":
                return findStockOperationLinks(null, (String) args[0]);
            case "getStockSourceByUuid":
                return byUuid(StockSource.class, (String) args[0]);
            case "voidStockOperationItem":
                voidByUuid(StockOperationItem.class, (String) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "findStockSources":
                return findStockSources((StockSourceSearchFilter) args[0]);
            case "saveStockSource":
                return saveOrUpdate(args[0]);
            case "voidStockSources":
                voidByUuids(StockSource.class, (List<String>) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "getPartyByStockSource":
                return firstByEntity(Party.class, "stockSource", args[0]);
            case "getPartyByLocation":
                return firstByEntity(Party.class, "location", args[0]);
            case "saveParty":
                return saveOrUpdate(args[0]);
            case "findParty":
                if (args[0] instanceof PartySearchFilter) {
                    return findParty((PartySearchFilter) args[0]);
                }
                return findParty((Boolean) args[0], (Boolean) args[1]);
            case "getPartyByUuid":
                return byUuid(Party.class, (String) args[0]);
            case "getAllParties":
                return getAllParties();
            case "getCurrentUserSessionInfo":
                return getCurrentUserSessionInfo();
            case "findStockItemPackagingUOMs":
                return findStockItemPackagingUOMs((StockItemPackagingUOMSearchFilter) args[0]);
            case "saveStockItemPackagingUOM":
                return saveStockItemPackagingUOM(args[0]);
            case "userHasStockManagementPrivilege":
                return userHasStockManagementPrivilege((User) args[0], (String) args[3]);
            case "getPrivilegeScopes":
                return getPrivilegeScopes((User) args[0], args[3]);
            case "getStockItemPackagingUOMByUuid":
                return byUuid(StockItemPackagingUOM.class, (String) args[0]);
            case "voidStockItemPackagingUOM":
                voidByUuid(StockItemPackagingUOM.class, (String) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "findStockBatches":
                return findStockBatches((StockBatchSearchFilter) args[0]);
            case "getExistingStockItemIds":
                return getExistingStockItemIds((Collection<StockItemSearchFilter.ItemGroupFilter>) args[0]);
            case "getDrugs":
                return listByIds(Drug.class, "drugId", (Collection<Integer>) args[0]);
            case "getConcepts":
                return listByIds(Concept.class, "conceptId", (Collection<Integer>) args[0]);
            case "getStockItems":
                return listByIds(StockItem.class, "id", (Collection<Integer>) args[0]);
            case "getStockItemPackagingUOMs":
                return getStockItemPackagingUOMs((List<StockItemPackagingUOMSearchFilter.ItemGroupFilter>) args[0]);
            case "getStockItemByDrug":
                return stockItemsByDrug((Integer) args[0]);
            case "getStockItemByConcept":
                return stockItemsByConcept((Integer) args[0]);
            case "getStockItemPackagingUOMByConcept":
                return stockItemPackagingUOMByConcept(args);
            case "getOrderItemsByOrder":
                return getOrderItemsByOrder((Integer[]) args[0]);
            case "getOrderItemsByEncounter":
                return getOrderItemsByEncounter((Integer[]) args[0]);
            case "findOrderItems":
                return args.length > 1
                        ? findOrderItems((OrderItemSearchFilter) args[0], (HashSet<RecordPrivilegeFilter>) args[1])
                        : findOrderItems((OrderItemSearchFilter) args[0], null);
            case "getStockItemNames":
                return getStockItemNames((List<Integer>) args[0]);
            case "getConceptNames":
                return getConceptNames((List<Integer>) args[0]);
            case "getLocationNames":
                return getLocationNames((List<Integer>) args[0]);
            case "getHealthCenterName":
                return healthCenterName();
            case "updateStockBatchExpiryNotificationDate":
                updateStockBatchExpiryNotificationDate((Collection<Integer>) args[0], (Date) args[1]);
                return null;
            case "updateStockRuleJobNextEvaluationDate":
                updateStockRuleDate((List<Integer>) args[0], (Date) args[1], true);
                return null;
            case "updateStockRuleJobNextActionDate":
                updateStockRuleDate((List<Integer>) args[0], (Date) args[1], false);
                return null;
            case "setStockItemCurrentBalanceWithDescendants":
                setStockItemCurrentBalanceWithDescendants((List<StockRuleCurrentQuantity>) args[0]);
                return null;
            case "setStockItemCurrentBalanceWithoutDescendants":
                setStockItemCurrentBalanceWithoutDescendants((List<StockRuleCurrentQuantity>) args[0]);
                return null;
            case "getDueStockRules":
                return getDueStockRules((Integer) args[0], (Integer) args[1]);
            case "getActiveUsersAssignedForScope":
                return getActiveUsersAssignedForScope((Integer) args[0], (List<String>) args[1]);
            case "getExpiringStockBatchesDueForNotification":
                return getExpiringStockBatchesDueForNotification((Integer) args[0]);
            case "runStockRuleEvaluationJob":
                runStockRuleEvaluationJob();
                return null;
            case "runStockBatchExpiryNoticationJob":
                runStockBatchExpiryNoticationJob();
                return null;
            case "saveBatchJob":
                if (args[0] instanceof BatchJob) {
                    saveOrUpdate(args[0]);
                    return null;
                }
                if (args[0] instanceof BatchJobDTO) {
                    return saveBatchJob((BatchJobDTO) args[0]);
                }
                throw unsupported(method);
            case "findBatchJobs":
                return findBatchJobs((BatchJobSearchFilter) args[0]);
            case "cancelBatchJob":
                cancelBatchJob((String) args[0], (String) args[1]);
                return null;
            case "failBatchJob":
                failBatchJob((String) args[0], (String) args[1]);
                return null;
            case "expireBatchJob":
                expireBatchJob((String) args[0], (String) args[1]);
                return null;
            case "getReports":
                return Report.getAllReports();
            case "getNextActiveBatchJob":
                return getNextActiveBatchJob();
            case "getBatchJobByUuid":
                return byUuid(BatchJob.class, (String) args[0]);
            case "updateBatchJobRunning":
                updateBatchJobStatus((String) args[0], BatchJobStatus.Running, null);
                return null;
            case "updateBatchJobExecutionState":
                updateBatchJobExecutionState((String) args[0], (String) args[1]);
                return null;
            case "getExpiredBatchJobs":
                return getExpiredBatchJobs();
            case "deleteBatchJob":
                remove(args[0]);
                return null;
            case "checkStockBatchHasTransactionsAfterOperation":
                return checkStockBatchHasTransactionsAfterOperation((Integer) args[0], (List<Integer>) args[1]);
            case "deleteReservedTransations":
                deleteReservedTransations((Integer) args[0]);
                return null;
            case "getStockItemByReference":
                return getStockItemByReference((StockSource) args[0], (String) args[1]);
            case "voidStockItemReference":
                voidByUuid(StockItemReference.class, (String) args[0], (String) args[1], (Integer) args[2]);
                return null;
            case "getStockItemReferenceByUuid":
                return byUuid(StockItemReference.class, (String) args[0]);
            case "getStockItemReferenceByStockItem":
                return getStockItemReferenceByStockItem((String) args[0]);
            case "getUserEmailAddress":
                return getUserEmailAddress((User) args[0]);
            case "sendStockOperationNotification":
                sendStockOperationNotification((String) args[0], (StockOperationAction.Action) args[1],
                    (String) args[2], (Integer) args[3]);
                return null;
            case "getExpiringStockBatchList":
                return getExpiringStockBatchList((StockExpiryFilter) args[0]);
            case "findStockOperationLineItems":
                return findStockOperationLineItems((StockOperationLineItemFilter) args[0]);
            case "findDispensingLineItems":
                return findDispensingLineItems((DispensingLineFilter) args[0]);
            case "findPrescriptionLineItems":
                return findPrescriptionLineItems((PrescriptionLineFilter) args[0]);
            default:
                throw unsupported(method);
        }
    }

    private boolean looksReadOnly(String methodName) {
        return methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("search")
                || methodName.startsWith("check");
    }

    private UnsupportedOperationException unsupported(Method method) {
        return new UnsupportedOperationException(
                "Stock Management service method " + method.getName() + " requires the Hibernate 7 DAO migration");
    }

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    private <T> List<T> listAll(Class<T> type) {
        return query(type, (cb, root) -> new ArrayList<>());
    }

    private List<LocationTree> getCompleteLocationTree(Integer atLocationId) {
        if (atLocationId == null) {
            return listAll(LocationTree.class);
        }
        return query(LocationTree.class, (cb, root) -> predicates(cb.equal(root.get("parentLocationId"), atLocationId)));
    }

    private void deleteLocationTreeNodes(List<LocationTree> nodes) {
        if (nodes == null) {
            return;
        }
        for (LocationTree node : nodes) {
            remove(node);
        }
    }

    private void deleteLocation(String uuid) {
        try {
            deleteLocationInternal(uuid);
        } catch (StockManagementException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new StockManagementException("Location is in use", exception);
        }
    }

    private void deleteLocationInternal(String uuid) {
        Location location = Context.getLocationService().getLocationByUuid(uuid);
        if (location == null) {
            throw new StockManagementException("Location does not exist");
        }
        if (location.getChildLocations() != null && !location.getChildLocations().isEmpty()) {
            throw new StockManagementException("Location has child locations");
        }
        Party party = firstByEntity(Party.class, "location", location);
        if (party != null) {
            remove(party);
        }
        deleteLocationTreeNodes(location.getLocationId());
        if (location.getTags() != null && !location.getTags().isEmpty()) {
            new ArrayList<>(location.getTags()).forEach(location::removeTag);
            Context.getLocationService().saveLocation(location);
        }
        if (location.getAttributes() != null && !location.getAttributes().isEmpty()) {
            new ArrayList<>(location.getAttributes()).forEach(this::remove);
            location.setAttributes(new HashSet<>());
        }
        remove(location);
    }

    private void deleteLocationTreeNodes(Integer locationId) {
        if (locationId == null) {
            return;
        }
        session().createMutationQuery(
            "delete from stockmanagement.LocationTree where parentLocationId = :locationId"
                    + " or childLocationId = :locationId")
                .setParameter("locationId", locationId)
                .executeUpdate();
    }

    private void saveAll(List<? extends Object> entities) {
        if (entities == null) {
            return;
        }
        for (Object entity : entities) {
            saveOrUpdate(entity);
        }
    }

    private List<PartyDTO> getCompletePartyList(Integer atLocationId, boolean onlyDispensingLocations) {
        List<Integer> locationIds = getCompleteLocationTree(atLocationId).stream()
                .map(LocationTree::getChildLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (locationIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (onlyDispensingLocations) {
            Set<Integer> dispensingLocationIds = new HashSet<>(getMainPharmacyLocationIds());
            dispensingLocationIds.addAll(getLocationIdsByTag(StockLocationTags.DISPENSARY_LOCATION_TAG));
            locationIds.removeIf(id -> !dispensingLocationIds.contains(id));
        }
        return partiesForLocations(locationIds);
    }

    private List<PartyDTO> partiesForLocations(List<Integer> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return new ArrayList<>();
        }
        PartySearchFilter filter = new PartySearchFilter();
        filter.setLocationIds(locationIds);
        filter.setIncludeVoided(false);
        return findParty(filter).getData();
    }

    private List<PartyDTO> getAllStockHoldingPartyList() {
        Set<Integer> locationIds = new HashSet<>();
        locationIds.addAll(getLocationIdsByTag(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG));
        locationIds.addAll(getLocationIdsByTag(StockLocationTags.DISPENSARY_LOCATION_TAG));
        locationIds.addAll(getLocationIdsByTag(StockLocationTags.MAIN_STORE_LOCATION_TAG));
        return partiesForLocations(new ArrayList<>(locationIds));
    }

    private List<Location> getLocationsByTag(String tagName) {
        LocationTag tag = Context.getLocationService().getLocationTagByName(tagName);
        if (tag == null) {
            return new ArrayList<>();
        }
        return Context.getLocationService().getLocationsByTag(tag).stream()
                .filter(location -> !Boolean.TRUE.equals(location.getRetired()))
                .collect(Collectors.toList());
    }

    private List<Integer> getMainPharmacyLocationIds() {
        return getLocationIdsByTag(StockLocationTags.MAIN_PHARMACY_LOCATION_TAG);
    }

    private List<Integer> getLocationIdsByTag(String tagName) {
        return getLocationsByTag(tagName).stream().map(Location::getId).collect(Collectors.toList());
    }

    private Result<UserRoleScopeDTO> findUserRoleScopes(UserRoleScopeSearchFilter filter) {
        UserRoleScopeSearchFilter safeFilter = filter == null ? new UserRoleScopeSearchFilter() : filter;
        List<UserRoleScope> scopes = query(UserRoleScope.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(safeFilter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), safeFilter.getUuid()));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            if (safeFilter.getUsers() != null && !safeFilter.getUsers().isEmpty()) {
                predicates.add(root.get("user").in(safeFilter.getUsers()));
            }
            if (safeFilter.getLocation() != null) {
                Join<UserRoleScope, UserRoleScopeLocation> locationJoin = root.join("userRoleScopeLocations",
                    JoinType.LEFT);
                predicates.add(cb.equal(locationJoin.get("location"), safeFilter.getLocation()));
                predicates.add(cb.isFalse(locationJoin.get("voided")));
            }
            if (safeFilter.getOperationType() != null) {
                Join<UserRoleScope, UserRoleScopeOperationType> operationJoin = root.join(
                    "userRoleScopeOperationTypes", JoinType.LEFT);
                predicates.add(cb.equal(operationJoin.get("stockOperationType"), safeFilter.getOperationType()));
                predicates.add(cb.isFalse(operationJoin.get("voided")));
            }
            return predicates;
        });
        scopes.sort(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)));
        List<UserRoleScopeDTO> dtos = scopes.stream().map(this::userRoleScopeToDto).collect(Collectors.toList());
        return resultFromList(dtos, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private UserRoleScopeDTO userRoleScopeToDto(UserRoleScope scope) {
        UserRoleScopeDTO dto = new UserRoleScopeDTO();
        dto.setId(scope.getId() == null ? 0 : scope.getId());
        dto.setUuid(scope.getUuid());
        dto.setActiveFrom(scope.getActiveFrom());
        dto.setActiveTo(scope.getActiveTo());
        dto.setEnabled(scope.getEnabled());
        dto.setPermanent(scope.getPermanent());
        User user = scope.getUser();
        if (user != null) {
            dto.setUserUuid(user.getUuid());
            dto.setUserName(user.getUsername());
            dto.setUserGivenName(user.getGivenName());
            dto.setUserFamilyName(user.getFamilyName());
        }
        Role role = scope.getRole();
        if (role != null) {
            dto.setRole(role.getRole());
        }
        Set<UserRoleScopeLocation> locations = scope.getUserRoleScopeLocations();
        if (locations != null) {
            dto.setLocations(locations.stream().filter(location -> !Boolean.TRUE.equals(location.getVoided()))
                    .map(this::userRoleScopeLocationToDto).collect(Collectors.toList()));
        }
        Set<UserRoleScopeOperationType> operationTypes = scope.getUserRoleScopeOperationTypes();
        if (operationTypes != null) {
            dto.setOperationTypes(operationTypes.stream()
                    .filter(operationType -> !Boolean.TRUE.equals(operationType.getVoided()))
                    .map(this::userRoleScopeOperationTypeToDto).collect(Collectors.toList()));
        }
        return dto;
    }

    private UserRoleScopeLocationDTO userRoleScopeLocationToDto(UserRoleScopeLocation location) {
        UserRoleScopeLocationDTO dto = new UserRoleScopeLocationDTO();
        dto.setUuid(location.getUuid());
        dto.setUserRoleScopeId(location.getUserRoleScope() == null ? null : location.getUserRoleScope().getId());
        dto.setEnableDescendants(location.getEnableDescendants());
        if (location.getLocation() != null) {
            dto.setLocationUuid(location.getLocation().getUuid());
            dto.setLocationName(location.getLocation().getName());
        }
        return dto;
    }

    private UserRoleScopeOperationTypeDTO userRoleScopeOperationTypeToDto(UserRoleScopeOperationType operationType) {
        UserRoleScopeOperationTypeDTO dto = new UserRoleScopeOperationTypeDTO();
        dto.setUuid(operationType.getUuid());
        dto.setUserRoleScopeId(
            operationType.getUserRoleScope() == null ? null : operationType.getUserRoleScope().getId());
        if (operationType.getStockOperationType() != null) {
            dto.setOperationTypeUuid(operationType.getStockOperationType().getUuid());
            dto.setOperationTypeName(operationType.getStockOperationType().getName());
        }
        return dto;
    }

    private UserRoleScope saveUserRoleScope(UserRoleScopeDTO dto) {
        if (dto == null) {
            throw new StockManagementException("User role scope payload is required");
        }

        UserRoleScope userRoleScope;
        if (StringUtils.isNotBlank(dto.getUuid())) {
            userRoleScope = byUuid(UserRoleScope.class, dto.getUuid());
            if (userRoleScope == null) {
                throw new StockManagementException("User role scope " + dto.getUuid() + " not found");
            }
            rejectSelfUserRoleScopeUpdate(userRoleScope.getUser());
        } else {
            userRoleScope = new UserRoleScope();
            User user = Context.getUserService().getUserByUuid(dto.getUserUuid());
            if (user == null) {
                throw new StockManagementException("User role scope user not found");
            }
            rejectSelfUserRoleScopeUpdate(user);
            userRoleScope.setUser(user);
        }

        Role role = Context.getUserService().getRole(dto.getRole());
        if (role == null) {
            throw new StockManagementException("User role scope role not found");
        }

        userRoleScope.setRole(role);
        if (dto.getPermanent() != null) {
            userRoleScope.setPermanent(dto.getPermanent());
        }
        userRoleScope.setActiveFrom(dto.getActiveFrom());
        userRoleScope.setActiveTo(dto.getActiveTo());
        userRoleScope.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));

        List<UserRoleScopeLocation> locationsToRemove = dto.getLocations() == null ? new ArrayList<>()
                : activeUserRoleScopeLocations(userRoleScope).stream()
                        .filter(location -> dto.getLocations().stream()
                                .noneMatch(incoming -> sameUuid(incoming.getLocationUuid(), location.getLocation())))
                        .collect(Collectors.toList());
        List<UserRoleScopeOperationType> operationTypesToRemove = dto.getOperationTypes() == null ? new ArrayList<>()
                : activeUserRoleScopeOperationTypes(userRoleScope).stream()
                        .filter(operationType -> dto.getOperationTypes().stream()
                                .noneMatch(incoming -> sameUuid(
                                    incoming.getOperationTypeUuid(), operationType.getStockOperationType())))
                        .collect(Collectors.toList());

        userRoleScope = saveOrUpdate(userRoleScope);

        for (UserRoleScopeLocation location : locationsToRemove) {
            voidByUuid(UserRoleScopeLocation.class, location.getUuid(), null, null);
        }
        for (UserRoleScopeOperationType operationType : operationTypesToRemove) {
            voidByUuid(UserRoleScopeOperationType.class, operationType.getUuid(), null, null);
        }
        saveUserRoleScopeLocations(userRoleScope, dto.getLocations());
        saveUserRoleScopeOperationTypes(userRoleScope, dto.getOperationTypes());

        return userRoleScope;
    }

    private void rejectSelfUserRoleScopeUpdate(User user) {
        User authenticatedUser = authenticatedUser();
        if (user != null && authenticatedUser != null && user.getUuid().equalsIgnoreCase(authenticatedUser.getUuid())) {
            throw new StockManagementException("Users cannot update their own stock management role scopes");
        }
    }

    private void saveUserRoleScopeLocations(UserRoleScope userRoleScope, List<UserRoleScopeLocationDTO> locationDtos) {
        if (locationDtos == null) {
            return;
        }
        for (UserRoleScopeLocationDTO locationDto : locationDtos) {
            Location location = Context.getLocationService().getLocationByUuid(locationDto.getLocationUuid());
            if (location == null) {
                throw new StockManagementException("User role scope location not found");
            }
            UserRoleScopeLocation scopeLocation = activeUserRoleScopeLocations(userRoleScope).stream()
                    .filter(existing -> sameUuid(locationDto.getLocationUuid(), existing.getLocation()))
                    .findFirst()
                    .orElseGet(UserRoleScopeLocation::new);
            scopeLocation.setUserRoleScope(userRoleScope);
            scopeLocation.setLocation(location);
            scopeLocation.setEnableDescendants(Boolean.TRUE.equals(locationDto.getEnableDescendants()));
            saveOrUpdate(scopeLocation);
        }
    }

    private void saveUserRoleScopeOperationTypes(
            UserRoleScope userRoleScope, List<UserRoleScopeOperationTypeDTO> operationTypeDtos) {
        if (operationTypeDtos == null) {
            return;
        }
        for (UserRoleScopeOperationTypeDTO operationTypeDto : operationTypeDtos) {
            StockOperationType operationType = byUuid(StockOperationType.class, operationTypeDto.getOperationTypeUuid());
            if (operationType == null) {
                throw new StockManagementException("User role scope operation type not found");
            }
            UserRoleScopeOperationType scopeOperationType = activeUserRoleScopeOperationTypes(userRoleScope).stream()
                    .filter(existing -> sameUuid(operationTypeDto.getOperationTypeUuid(), existing.getStockOperationType()))
                    .findFirst()
                    .orElseGet(UserRoleScopeOperationType::new);
            scopeOperationType.setUserRoleScope(userRoleScope);
            scopeOperationType.setStockOperationType(operationType);
            saveOrUpdate(scopeOperationType);
        }
    }

    private List<UserRoleScopeLocation> activeUserRoleScopeLocations(UserRoleScope userRoleScope) {
        if (userRoleScope.getUserRoleScopeLocations() == null) {
            return new ArrayList<>();
        }
        return userRoleScope.getUserRoleScopeLocations().stream()
                .filter(location -> !Boolean.TRUE.equals(location.getVoided()))
                .collect(Collectors.toList());
    }

    private List<UserRoleScopeOperationType> activeUserRoleScopeOperationTypes(UserRoleScope userRoleScope) {
        if (userRoleScope.getUserRoleScopeOperationTypes() == null) {
            return new ArrayList<>();
        }
        return userRoleScope.getUserRoleScopeOperationTypes().stream()
                .filter(operationType -> !Boolean.TRUE.equals(operationType.getVoided()))
                .collect(Collectors.toList());
    }

    private boolean sameUuid(String uuid, BaseOpenmrsObject object) {
        return StringUtils.isNotBlank(uuid) && object != null && uuid.equalsIgnoreCase(object.getUuid());
    }

    private StockOperation saveStockOperation(StockOperationDTO dto) {
        if (dto == null) {
            throw new StockManagementException("Stock operation is required");
        }
        boolean isNew = StringUtils.isBlank(dto.getUuid());
        StockOperation operation;
        StockOperationType operationType;
        if (isNew) {
            operation = new StockOperation();
            operation.setOperationOrder(1);
            operation.setStatus(StockOperationStatus.NEW);
            operation.setLocked(false);
            operation.setStockOperationItems(new HashSet<>());
            operation.setStockItemTransactions(new HashSet<>());
            operation.setReservedTransactions(new HashSet<>());
            operation.setParentStockOperationLinks(new HashSet<>());
            operation.setChildStockOperationLinks(new HashSet<>());
            operationType = requireByUuid(StockOperationType.class, dto.getOperationTypeUuid(),
                "Stock operation type not found");
            operation.setStockOperationType(operationType);
        } else {
            operation = requireByUuid(StockOperation.class, dto.getUuid(), "Stock operation not found");
            operationType = operation.getStockOperationType();
            if (operationType == null) {
                throw new StockManagementException("Stock operation type not found");
            }
            if (operation.getStockOperationItems() == null) {
                operation.setStockOperationItems(new HashSet<>());
            }
        }

        applyStockOperationFields(operation, dto);
        StockOperationLink requisitionLink = isNew ? stockOperationRequisitionLink(operationType, dto) : null;
        List<StockOperationItem> operationItems = saveStockOperationItems(operation, operationType, dto);
        saveOrUpdate(operation);
        if (isNew && StringUtils.isBlank(operation.getOperationNumber())) {
            operation.setOperationNumber(operationType.getAcronym() + "-"
                    + StringUtils.leftPad(Integer.toString(operation.getId()), 4, '0'));
            saveOrUpdate(operation);
        }
        for (StockOperationItem item : operationItems) {
            item.setStockOperation(operation);
            saveOrUpdate(item);
        }
        if (requisitionLink != null) {
            requisitionLink.setChild(operation);
            saveOrUpdate(requisitionLink);
        }
        return operation;
    }

    private void applyStockOperationFields(StockOperation operation, StockOperationDTO dto) {
        operation.setApprovalRequired(dto.getApprovalRequired());
        operation.setOperationDate(dto.getOperationDate());
        operation.setExternalReference(dto.getExternalReference());
        operation.setRemarks(StringUtils.isBlank(dto.getRemarks()) ? null : dto.getRemarks());
        operation.setAtLocation(byUuid(Location.class, dto.getAtLocationUuid()));
        operation.setSource(StringUtils.isBlank(dto.getSourceUuid()) ? null : byUuid(Party.class, dto.getSourceUuid()));
        operation.setDestination(StringUtils.isBlank(dto.getDestinationUuid())
                ? null
                : byUuid(Party.class, dto.getDestinationUuid()));
        operation.setReason(StringUtils.isBlank(dto.getReasonUuid())
                ? null
                : Context.getConceptService().getConceptByUuid(dto.getReasonUuid()));
        if (StringUtils.isNotBlank(dto.getResponsiblePersonUuid())) {
            User user = Context.getUserService().getUserByUuid(dto.getResponsiblePersonUuid());
            if (user == null) {
                throw new StockManagementException("Responsible person not found");
            }
            operation.setResponsiblePerson(user);
            operation.setResponsiblePersonOther(null);
        } else {
            operation.setResponsiblePerson(null);
            operation.setResponsiblePersonOther(StringUtils.isBlank(dto.getResponsiblePersonOther())
                    ? null
                    : dto.getResponsiblePersonOther());
        }
    }

    private StockOperationLink stockOperationRequisitionLink(StockOperationType operationType, StockOperationDTO dto) {
        if (operationType.canBeRelatedToRequisition() && StringUtils.isNotBlank(dto.getRequisitionStockOperationUuid())) {
            StockOperation parent = byUuid(StockOperation.class, dto.getRequisitionStockOperationUuid());
            if (parent == null) {
                throw new StockManagementException("Requisition stock operation not found");
            }
            StockOperationLink link = new StockOperationLink();
            link.setParent(parent);
            return link;
        }
        if (StockOperationType.STOCK_ISSUE.equals(operationType.getOperationType())
                && !GlobalProperties.allowStockIssueWithoutRequisition()) {
            throw new StockManagementException("Requisition stock operation is required");
        }
        return null;
    }

    private List<StockOperationItem> saveStockOperationItems(StockOperation operation,
            StockOperationType operationType, StockOperationDTO dto) {
        List<StockOperationItemDTO> itemDtos = dto.getStockOperationItems() == null
                ? new ArrayList<>()
                : dto.getStockOperationItems();
        List<StockOperationItem> existingItems = operation.getStockOperationItems() == null
                ? new ArrayList<>()
                : new ArrayList<>(operation.getStockOperationItems());
        List<StockOperationItem> savedItems = new ArrayList<>();
        for (StockOperationItemDTO itemDto : itemDtos) {
            StockOperationItem item = resolveStockOperationItem(existingItems, itemDto);
            StockItem stockItem = resolveStockOperationStockItem(itemDto);
            item.setStockItem(stockItem);
            if (itemDto.getQuantity() != null) {
                item.setQuantity(itemDto.getQuantity());
                item.setStockItemPackagingUOM(requireByUuid(StockItemPackagingUOM.class,
                    itemDto.getStockItemPackagingUOMUuid(), "Stock item packaging unit not found"));
            }
            if (itemDto.getPurchasePrice() != null) {
                item.setPurchasePrice(itemDto.getPurchasePrice());
            }
            item.setStockBatch(resolveStockOperationBatch(operationType, stockItem, itemDto));
            applyStockOperationItemQuantities(item, itemDto);
            if (operation.getStockOperationItems() != null) {
                operation.getStockOperationItems().add(item);
            }
            savedItems.add(item);
        }
        return savedItems;
    }

    private StockOperationItem resolveStockOperationItem(List<StockOperationItem> existingItems,
            StockOperationItemDTO itemDto) {
        if (StringUtils.isBlank(itemDto.getUuid())) {
            return new StockOperationItem();
        }
        return existingItems.stream()
                .filter(item -> itemDto.getUuid().equals(item.getUuid()))
                .findFirst()
                .orElseThrow(() -> new StockManagementException("Stock operation item not found: "
                        + itemDto.getUuid()));
    }

    private StockItem resolveStockOperationStockItem(StockOperationItemDTO itemDto) {
        StockItem stockItem = StringUtils.isNotBlank(itemDto.getStockItemUuid())
                ? byUuid(StockItem.class, itemDto.getStockItemUuid())
                : first(listByIds(StockItem.class, "id", List.of(itemDto.getStockItemId())));
        if (stockItem == null) {
            throw new StockManagementException("Stock item not found");
        }
        return stockItem;
    }

    private StockBatch resolveStockOperationBatch(StockOperationType operationType, StockItem stockItem,
            StockOperationItemDTO itemDto) {
        if (operationType.requiresBatchUuid()) {
            return requireByUuid(StockBatch.class, itemDto.getStockBatchUuid(), "Stock batch not found");
        }
        if (!operationType.requiresActualBatchInformation()) {
            return null;
        }
        StockBatch stockBatch = findStockBatch(stockItem, itemDto.getBatchNo(), itemDto.getExpiration());
        if (stockBatch != null) {
            return stockBatch;
        }
        stockBatch = new StockBatch();
        stockBatch.setStockItem(stockItem);
        stockBatch.setBatchNo(itemDto.getBatchNo());
        if (Boolean.TRUE.equals(stockItem.getHasExpiration())) {
            stockBatch.setExpiration(itemDto.getExpiration());
        }
        saveOrUpdate(stockBatch);
        return stockBatch;
    }

    private StockBatch findStockBatch(StockItem stockItem, String batchNo, Date expiration) {
        if (stockItem == null || StringUtils.isBlank(batchNo)) {
            return null;
        }
        List<StockBatch> batches = query(StockBatch.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("stockItem"), stockItem));
            predicates.add(cb.equal(cb.lower(root.get("batchNo").as(String.class)),
                batchNo.toLowerCase(Locale.ROOT)));
            if (Boolean.TRUE.equals(stockItem.getHasExpiration())) {
                if (expiration == null) {
                    predicates.add(cb.isNull(root.get("expiration")));
                } else {
                    predicates.add(cb.equal(root.get("expiration"), expiration));
                }
            } else {
                predicates.add(cb.isNull(root.get("expiration")));
            }
            return predicates;
        });
        batches.sort(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)));
        return first(batches);
    }

    private void applyStockOperationItemQuantities(StockOperationItem item, StockOperationItemDTO itemDto) {
        if (itemDto.getQuantityReceived() != null) {
            item.setQuantityReceived(itemDto.getQuantityReceived());
            item.setQuantityReceivedPackagingUOM(requireByUuid(StockItemPackagingUOM.class,
                itemDto.getQuantityReceivedPackagingUOMUuid(), "Quantity received packaging unit not found"));
        }
        if (itemDto.getQuantityRequested() != null) {
            item.setQuantityRequested(itemDto.getQuantityRequested());
            item.setQuantityRequestedPackagingUOM(requireByUuid(StockItemPackagingUOM.class,
                itemDto.getQuantityRequestedPackagingUOMUuid(), "Quantity requested packaging unit not found"));
        }
    }

    private void processStockOperationAction(StockOperationDTO dto, StockOperationAction.Action action,
            String reason) {
        if (dto == null || StringUtils.isBlank(dto.getUuid())) {
            throw new StockManagementException("Stock operation uuid is required");
        }
        StockOperation operation = requireByUuid(StockOperation.class, dto.getUuid(), "Stock operation not found");
        ensureStockOperationCollections(operation);
        StockOperationType operationType = operation.getStockOperationType();
        if (operationType == null) {
            throw new StockManagementException("Stock operation type not found");
        }
        validateStockOperationAction(operation, operationType, action);
        boolean lockAndReserve = action == StockOperationAction.Action.SUBMIT
                || (operation.isUpdateable() && (action == StockOperationAction.Action.COMPLETE
                        || action == StockOperationAction.Action.DISPATCH));
        if (lockAndReserve && !operationType.isQuantityOptional()) {
            createReservedTransactions(operation);
        }
        applyStockOperationAction(operation, operationType, action, reason, lockAndReserve);
        saveOrUpdate(operation);
        notifyStockOperationAction(operation, operationType, action, reason);
    }

    private void notifyStockOperationAction(StockOperation operation, StockOperationType operationType,
            StockOperationAction.Action action, String reason) {
        if (!shouldNotifyStockOperationAction(operationType, action)) {
            return;
        }
        try {
            User user = authenticatedUser();
            sendStockOperationNotification(operation.getUuid(), action, reason, user == null ? null : user.getUserId());
        } catch (Exception exception) {
            log.error("Unable to send stock operation notification", exception);
        }
    }

    private boolean shouldNotifyStockOperationAction(StockOperationType operationType,
            StockOperationAction.Action action) {
        if (operationType == null || action == null) {
            return false;
        }
        switch (action) {
            case SUBMIT:
                return Boolean.TRUE.equals(operationType.getNotifySubmitted());
            case APPROVE:
                return Boolean.TRUE.equals(operationType.getNotifyApproved());
            case DISPATCH:
                return Boolean.TRUE.equals(operationType.getNotifyDispatched());
            case RETURN:
                return Boolean.TRUE.equals(operationType.getNotifyReturned());
            case REJECT:
                return Boolean.TRUE.equals(operationType.getNotifyRejected());
            case COMPLETE:
                return Boolean.TRUE.equals(operationType.getNotifyCompleted());
            case CANCEL:
                return Boolean.TRUE.equals(operationType.getNotifyCancelled());
            default:
                return false;
        }
    }

    private void ensureStockOperationCollections(StockOperation operation) {
        if (operation.getStockOperationItems() == null) {
            operation.setStockOperationItems(new HashSet<>());
        }
        if (operation.getReservedTransactions() == null) {
            operation.setReservedTransactions(new HashSet<>());
        }
        if (operation.getStockItemTransactions() == null) {
            operation.setStockItemTransactions(new HashSet<>());
        }
    }

    private void validateStockOperationAction(StockOperation operation, StockOperationType operationType,
            StockOperationAction.Action action) {
        boolean allowed;
        switch (action) {
            case SUBMIT:
                allowed = operation.isUpdateable();
                break;
            case COMPLETE:
                allowed = (operation.isUpdateable() && !operationType.requiresDispatchAcknowledgement())
                        || (!operation.isUpdateable() && operationType.requiresDispatchAcknowledgement()
                                && operation.getStatus() == StockOperationStatus.DISPATCHED);
                break;
            case DISPATCH:
                allowed = operationType.requiresDispatchAcknowledgement()
                        && (operation.isUpdateable() || operation.getStatus() == StockOperationStatus.SUBMITTED);
                break;
            case APPROVE:
                allowed = operation.getStatus() == StockOperationStatus.SUBMITTED
                        && !operationType.requiresDispatchAcknowledgement();
                break;
            case RETURN:
                allowed = operation.getStatus() == StockOperationStatus.SUBMITTED
                        || operation.getStatus() == StockOperationStatus.DISPATCHED;
                break;
            case REJECT:
                allowed = operation.getStatus() == StockOperationStatus.SUBMITTED;
                break;
            case CANCEL:
                allowed = operation.isUpdateable() || operation.getStatus() == StockOperationStatus.SUBMITTED;
                break;
            default:
                allowed = false;
        }
        if (!allowed) {
            throw new StockManagementException("Stock operation action is not allowed");
        }
        validateStockOperationActionPrivilege(operation, operationType, action);
    }

    private void validateStockOperationActionPrivilege(StockOperation operation, StockOperationType operationType,
            StockOperationAction.Action action) {
        String privilege = stockOperationActionPrivilege(operation, action);
        Location location = stockOperationActionLocation(operation, action);
        if (location == null || !operationType.userCanProcess(authenticatedUser(), location, privilege)) {
            throw new StockManagementException("User cannot process stock operation");
        }
    }

    private String stockOperationActionPrivilege(StockOperation operation, StockOperationAction.Action action) {
        switch (action) {
            case APPROVE:
            case REJECT:
                return Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE;
            case RETURN:
                return operation.getStatus() == StockOperationStatus.DISPATCHED
                        ? Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS
                        : Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE;
            case COMPLETE:
                return operation.getStatus() == StockOperationStatus.DISPATCHED
                        ? Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS
                        : Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE;
            case DISPATCH:
                return operation.isUpdateable()
                        ? Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE
                        : Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE;
            case CANCEL:
                return operation.getStatus() == StockOperationStatus.SUBMITTED
                        ? Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_APPROVE
                        : Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE;
            case SUBMIT:
            default:
                return Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE;
        }
    }

    private Location stockOperationActionLocation(StockOperation operation, StockOperationAction.Action action) {
        if ((action == StockOperationAction.Action.COMPLETE || action == StockOperationAction.Action.RETURN)
                && operation.getStatus() == StockOperationStatus.DISPATCHED
                && operation.getDestination() != null) {
            return operation.getDestination().getLocation();
        }
        return operation.getAtLocation();
    }

    private void createReservedTransactions(StockOperation operation) {
        operation.clearReservedTransactions();
        for (StockOperationItem item : operation.getStockOperationItems()) {
            if (Boolean.TRUE.equals(item.getVoided())) {
                continue;
            }
            ReservedTransaction transaction = new ReservedTransaction(operation, item);
            transaction.setCreator(authenticatedUser());
            transaction.setDateCreated(new Date());
            transaction.setParty(operation.getSource());
            operation.addReservedTransaction(transaction);
        }
    }

    private void applyStockOperationAction(StockOperation operation, StockOperationType operationType,
            StockOperationAction.Action action, String reason, boolean lockAndReserve) {
        User user = authenticatedUser();
        Date now = new Date();
        if (lockAndReserve) {
            operation.setLocked(true);
            if (action == StockOperationAction.Action.SUBMIT) {
                operation.setSubmittedBy(user);
                operation.setSubmittedDate(now);
                operation.setStatus(StockOperationStatus.SUBMITTED);
            } else if (action == StockOperationAction.Action.COMPLETE) {
                operation.setCompletedBy(user);
                operation.setCompletedDate(now);
                operation.setStatus(StockOperationStatus.COMPLETED);
            } else if (action == StockOperationAction.Action.DISPATCH) {
                resetDispatchedQuantities(operation);
                operation.setDispatchedBy(user);
                operation.setDispatchedDate(now);
                operation.setStatus(StockOperationStatus.DISPATCHED);
            }
            operationType.onPending(operation);
            if (action == StockOperationAction.Action.COMPLETE) {
                operationType.onCompleted(operation);
            }
            return;
        }
        if (action == StockOperationAction.Action.DISPATCH) {
            resetDispatchedQuantities(operation);
            operation.setDispatchedBy(user);
            operation.setDispatchedDate(now);
            operation.setStatus(StockOperationStatus.DISPATCHED);
        } else if (action == StockOperationAction.Action.COMPLETE || action == StockOperationAction.Action.APPROVE) {
            operation.setCompletedBy(user);
            operation.setCompletedDate(now);
            operation.setStatus(StockOperationStatus.COMPLETED);
            operationType.onCompleted(operation);
        } else if (action == StockOperationAction.Action.RETURN) {
            operation.setLocked(false);
            operation.setReturnedBy(user);
            operation.setReturnedDate(now);
            operation.setReturnReason(reason);
            operation.setStatus(StockOperationStatus.RETURNED);
            operationType.onCancelled(operation);
        } else if (action == StockOperationAction.Action.REJECT) {
            operation.setRejectedBy(user);
            operation.setRejectedDate(now);
            operation.setRejectionReason(reason);
            operation.setStatus(StockOperationStatus.REJECTED);
            operationType.onCancelled(operation);
        } else if (action == StockOperationAction.Action.CANCEL) {
            operation.setCancelledBy(user);
            operation.setCancelledDate(now);
            operation.setCancelReason(reason);
            operation.setStatus(StockOperationStatus.CANCELLED);
            operationType.onCancelled(operation);
        }
    }

    private void resetDispatchedQuantities(StockOperation operation) {
        for (StockOperationItem item : operation.getStockOperationItems()) {
            if (Boolean.TRUE.equals(item.getVoided())) {
                continue;
            }
            item.setQuantityReceived(item.getQuantity());
            item.setQuantityReceivedPackagingUOM(item.getStockItemPackagingUOM());
        }
    }

    private void stockOperationItemsReceived(StockOperationDTO dto, List<StockOperationActionLineItem> lineItems) {
        if (dto == null || StringUtils.isBlank(dto.getUuid()) || lineItems == null || lineItems.isEmpty()) {
            return;
        }
        StockOperation operation = requireByUuid(StockOperation.class, dto.getUuid(), "Stock operation not found");
        ensureStockOperationCollections(operation);
        StockOperationType operationType = operation.getStockOperationType();
        if (operationType == null || !operationType.requiresDispatchAcknowledgement()
                || !operation.canReceiveItems()) {
            throw new StockManagementException("Stock operation cannot receive items");
        }
        Location destination = operation.getDestination() == null ? null : operation.getDestination().getLocation();
        if (destination == null || !operationType.userCanProcess(authenticatedUser(), destination,
            Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_RECEIVEITEMS)) {
            throw new StockManagementException("User cannot receive stock operation items");
        }
        BigDecimal threshold = GlobalProperties.getExcessReceivedItemThreshold();
        for (StockOperationActionLineItem lineItem : lineItems) {
            if (lineItem.getAmount() == null || lineItem.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new StockManagementException("Received quantity must be positive");
            }
            StockOperationItem item = operation.getStockOperationItems().stream()
                    .filter(candidate -> lineItem.getUuid() != null && lineItem.getUuid().equals(candidate.getUuid()))
                    .findFirst()
                    .orElseThrow(() -> new StockManagementException("Stock operation line item not found"));
            StockItemPackagingUOM uom = stockOperationItemUom(item, lineItem.getPackagingUoMUuId());
            if (uom == null) {
                throw new StockManagementException("Received packaging unit not found");
            }
            validateReceivedQuantity(item, lineItem.getAmount(), uom, threshold);
            item.setQuantityReceived(lineItem.getAmount());
            item.setQuantityReceivedPackagingUOM(uom);
        }
        saveOrUpdate(operation);
    }

    private void validateReceivedQuantity(StockOperationItem item, BigDecimal amount,
            StockItemPackagingUOM uom, BigDecimal threshold) {
        if (item.getQuantity() == null || item.getStockItemPackagingUOM() == null
                || item.getStockItemPackagingUOM().getFactor() == null || uom.getFactor() == null) {
            return;
        }
        BigDecimal receivedBase = amount.multiply(uom.getFactor());
        BigDecimal sentBase = item.getQuantity().multiply(item.getStockItemPackagingUOM().getFactor());
        BigDecimal allowedMultiplier = BigDecimal.valueOf(100).add(threshold == null ? BigDecimal.ZERO : threshold)
                .divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_EVEN);
        if (receivedBase.compareTo(sentBase.multiply(allowedMultiplier)) > 0) {
            throw new StockManagementException("Received quantity exceeds allowed threshold");
        }
    }

    private StockOperationBatchNumbersDTO saveStockOperationBatchNumbers(
            StockOperationBatchNumbersDTO batchNumbersDto) {
        if (batchNumbersDto == null || StringUtils.isBlank(batchNumbersDto.getUuid())
                || batchNumbersDto.getBatchNumbers() == null || batchNumbersDto.getBatchNumbers().isEmpty()) {
            throw new StockManagementException("Stock operation batch numbers are required");
        }
        StockOperation operation = requireByUuid(StockOperation.class, batchNumbersDto.getUuid(),
            "Stock operation not found");
        ensureStockOperationCollections(operation);
        StockOperationType operationType = operation.getStockOperationType();
        if (operationType == null || !operationType.requiresActualBatchInformation()) {
            throw new StockManagementException("Stock operation does not support batch number updates");
        }
        if (!StockOperationStatus.canUpdateBatchInformation(operation.getStatus())
                || !Boolean.TRUE.equals(operationType.getAllowBatchInfoUpdate())) {
            throw new StockManagementException("Stock operation batch number update denied");
        }
        if (operation.getAtLocation() == null || !operationType.userCanProcess(authenticatedUser(),
            operation.getAtLocation(), Privileges.TASK_STOCKMANAGEMENT_STOCKOPERATIONS_MUTATE)) {
            throw new StockManagementException("User cannot update stock operation batch numbers");
        }
        Map<Integer, Boolean> batchesInUse = checkStockBatchHasTransactionsAfterOperation(operation.getId(),
            operation.getStockOperationItems().stream()
                    .map(StockOperationItem::getStockBatch)
                    .filter(Objects::nonNull)
                    .map(StockBatch::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList()));
        for (StockOperationBatchNumbersDTO.StockOperationItemBatchNumber batchNumber : batchNumbersDto.getBatchNumbers()) {
            updateStockOperationItemBatchNumber(operation, batchNumber, batchesInUse);
        }
        saveOrUpdate(operation);
        return batchNumbersDto;
    }

    private void updateStockOperationItemBatchNumber(StockOperation operation,
            StockOperationBatchNumbersDTO.StockOperationItemBatchNumber batchNumber, Map<Integer, Boolean> batchesInUse) {
        if (batchNumber == null || StringUtils.isBlank(batchNumber.getUuid())
                || StringUtils.isBlank(batchNumber.getBatchNo())) {
            throw new StockManagementException("Stock operation item batch number is required");
        }
        StockOperationItem item = operation.getStockOperationItems().stream()
                .filter(candidate -> batchNumber.getUuid().equals(candidate.getUuid()))
                .findFirst()
                .orElseThrow(() -> new StockManagementException("Stock operation item not found"));
        StockBatch currentBatch = item.getStockBatch();
        if (currentBatch != null && batchesInUse.containsKey(currentBatch.getId())) {
            throw new StockManagementException("Stock batch already has later transactions");
        }
        StockItem stockItem = item.getStockItem();
        if (stockItem == null) {
            throw new StockManagementException("Stock item not found");
        }
        if (Boolean.TRUE.equals(stockItem.getHasExpiration())) {
            if (batchNumber.getExpiration() == null) {
                throw new StockManagementException("Batch expiration is required");
            }
            if (!batchNumber.getExpiration().after(DateUtil.today())) {
                throw new StockManagementException("Batch expiration must be in the future");
            }
        }
        StockBatch newBatch = findStockBatch(stockItem, batchNumber.getBatchNo(), batchNumber.getExpiration());
        if (newBatch == null) {
            newBatch = new StockBatch();
            newBatch.setStockItem(stockItem);
            newBatch.setBatchNo(batchNumber.getBatchNo());
            if (Boolean.TRUE.equals(stockItem.getHasExpiration())) {
                newBatch.setExpiration(batchNumber.getExpiration());
            }
            saveOrUpdate(newBatch);
        }
        item.setStockBatch(newBatch);
        if (operation.getReservedTransactions() != null) {
            for (ReservedTransaction transaction : operation.getReservedTransactions()) {
                if (transaction.getStockOperationItem() != null
                        && Objects.equals(transaction.getStockOperationItem().getId(), item.getId())) {
                    transaction.setStockBatch(newBatch);
                }
            }
        }
        if (operation.getStockItemTransactions() != null) {
            for (StockItemTransaction transaction : operation.getStockItemTransactions()) {
                if (transaction.getStockOperationItem() != null
                        && Objects.equals(transaction.getStockOperationItem().getId(), item.getId())) {
                    transaction.setStockBatch(newBatch);
                }
            }
        }
    }

    private StockItemPackagingUOM stockOperationItemUom(StockOperationItem item, String uomUuid) {
        if (item == null || item.getStockItem() == null || StringUtils.isBlank(uomUuid)) {
            return null;
        }
        if (item.getStockItem().getStockItemPackagingUOMs() != null) {
            for (StockItemPackagingUOM uom : item.getStockItem().getStockItemPackagingUOMs()) {
                if (uomUuid.equals(uom.getUuid())) {
                    return uom;
                }
            }
        }
        return byUuid(StockItemPackagingUOM.class, uomUuid);
    }

    private void dispenseStockItems(List<DispenseRequest> dispenseRequests) {
        if (dispenseRequests == null || dispenseRequests.isEmpty()) {
            return;
        }
        List<DispenseLine> lines = dispenseRequests.stream()
                .map(this::dispenseLine)
                .collect(Collectors.toList());
        validateDispenseBalances(lines);
        for (DispenseLine line : lines) {
            StockItemTransaction transaction = new StockItemTransaction();
            transaction.setParty(line.party);
            transaction.setPatient(line.patient);
            transaction.setCreator(authenticatedUser());
            transaction.setDateCreated(new Date());
            transaction.setStockItem(line.stockItem);
            transaction.setStockBatch(line.stockBatch);
            transaction.setQuantity(line.quantity.multiply(BigDecimal.valueOf(-1)));
            transaction.setStockItemPackagingUOM(line.uom);
            transaction.setOrder(line.order);
            transaction.setEncounter(line.encounter);
            saveOrUpdate(transaction);
        }
    }

    private DispenseLine dispenseLine(DispenseRequest request) {
        Location location = byUuid(Location.class, request.getLocationUuid());
        if (location == null) {
            throw new StockManagementException("Dispense location is required");
        }
        Party party = firstByEntity(Party.class, "location", location);
        if (party == null) {
            throw new StockManagementException("Dispense location party is required");
        }
        StockItem stockItem = requireByUuid(StockItem.class, request.getStockItemUuid(), "Stock item is required");
        StockBatch stockBatch = requireByUuid(StockBatch.class, request.getStockBatchUuid(), "Stock batch is required");
        Patient patient = request.getPatientId() == null ? null : Context.getPatientService().getPatient(request.getPatientId());
        if (patient == null) {
            throw new StockManagementException("Patient is required");
        }
        StockItemPackagingUOM uom = requireByUuid(StockItemPackagingUOM.class,
            request.getStockItemPackagingUOMUuid(), "Stock item packaging unit is required");
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new StockManagementException("Dispense quantity must be greater than zero");
        }
        Order order = request.getOrderId() == null ? null : Context.getOrderService().getOrder(request.getOrderId());
        if (request.getOrderId() != null && order == null) {
            throw new StockManagementException("Order not found");
        }
        Encounter encounter = request.getEncounterId() == null ? null
                : Context.getEncounterService().getEncounter(request.getEncounterId());
        if (request.getEncounterId() != null && encounter == null) {
            throw new StockManagementException("Encounter not found");
        }
        return new DispenseLine(party, patient, order, encounter, stockItem, stockBatch, request.getQuantity(), uom);
    }

    private void validateDispenseBalances(List<DispenseLine> lines) {
        Map<String, BigDecimal> requestedByKey = new LinkedHashMap<>();
        Map<String, DispenseLine> lineByKey = new LinkedHashMap<>();
        for (DispenseLine line : lines) {
            BigDecimal factor = line.uom.getFactor() == null ? BigDecimal.ONE : line.uom.getFactor();
            String key = inventoryKey(line.party.getId(), line.stockItem.getId(), line.stockBatch.getId());
            requestedByKey.merge(key, line.quantity.multiply(factor), BigDecimal::add);
            lineByKey.putIfAbsent(key, line);
        }
        for (Map.Entry<String, BigDecimal> entry : requestedByKey.entrySet()) {
            DispenseLine line = lineByKey.get(entry.getKey());
            BigDecimal balance = currentStockBalance(line.party, line.stockItem, line.stockBatch);
            if (balance.subtract(entry.getValue()).compareTo(BigDecimal.ZERO) < 0) {
                throw new StockManagementException("Dispense quantity would make stock negative for "
                        + stockItemName(line.stockItem) + " batch " + line.stockBatch.getBatchNo());
            }
        }
    }

    private BigDecimal currentStockBalance(Party party, StockItem stockItem, StockBatch stockBatch) {
        return query(StockItemTransaction.class, (cb, root) -> predicates(
            cb.equal(root.get("party"), party),
            cb.equal(root.get("stockItem"), stockItem),
            cb.equal(root.get("stockBatch"), stockBatch))).stream()
                .map(this::stockItemTransactionQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static class DispenseLine {

        private final Party party;

        private final Patient patient;

        private final Order order;

        private final Encounter encounter;

        private final StockItem stockItem;

        private final StockBatch stockBatch;

        private final BigDecimal quantity;

        private final StockItemPackagingUOM uom;

        DispenseLine(Party party, Patient patient, Order order, Encounter encounter, StockItem stockItem,
                StockBatch stockBatch, BigDecimal quantity, StockItemPackagingUOM uom) {
            this.party = party;
            this.patient = patient;
            this.order = order;
            this.encounter = encounter;
            this.stockItem = stockItem;
            this.stockBatch = stockBatch;
            this.quantity = quantity;
            this.uom = uom;
        }
    }

    private List<StockOperationItem> getStockOperationItemsByStockOperation(Integer stockOperationId) {
        if (stockOperationId == null) {
            return new ArrayList<>();
        }
        return query(StockOperationItem.class, (cb, root) -> predicates(
            cb.equal(root.get("stockOperation").get("id"), stockOperationId),
            cb.isFalse(root.get("voided"))));
    }

    private Result<StockOperationItemDTO> findStockOperationItems(StockOperationItemSearchFilter filter) {
        StockOperationItemSearchFilter safeFilter = filter == null ? new StockOperationItemSearchFilter() : filter;
        List<StockOperationItemDTO> items = queryStockOperationItems(safeFilter).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> stockOperationItemToDto(item, safeFilter))
                .collect(Collectors.toList());
        return resultFromList(items, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private Result<StockOperationItemCost> getStockOperationItemCosts(StockOperationItemSearchFilter filter) {
        StockOperationItemSearchFilter safeFilter = filter == null ? new StockOperationItemSearchFilter() : filter;
        List<StockOperationItemCost> costs = queryStockOperationItems(safeFilter).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockOperationItemToCost)
                .collect(Collectors.toList());
        return resultFromList(costs, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private List<StockOperationItem> queryStockOperationItems(StockOperationItemSearchFilter filter) {
        return query(StockOperationItem.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockOperationItem, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            Join<StockOperationItem, StockOperation> stockOperation = root.join("stockOperation", JoinType.LEFT);
            if (StringUtils.isNotBlank(filter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), filter.getUuid()));
            }
            if (filter.getStockItemId() != null) {
                predicates.add(cb.equal(stockItem.get("id"), filter.getStockItemId()));
            }
            if (StringUtils.isNotBlank(filter.getStockItemUuid())) {
                predicates.add(cb.equal(stockItem.get("uuid"), filter.getStockItemUuid()));
            }
            if (filter.getStockOperationIds() != null && !filter.getStockOperationIds().isEmpty()) {
                predicates.add(stockOperation.get("id").in(filter.getStockOperationIds()));
            }
            if (filter.getStockOperationUuids() != null && !filter.getStockOperationUuids().isEmpty()) {
                predicates.add(stockOperation.get("uuid").in(filter.getStockOperationUuids()));
            }
            if (!filter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        });
    }

    private StockOperationItemDTO stockOperationItemToDto(
            StockOperationItem item, StockOperationItemSearchFilter filter) {
        StockOperationItemDTO dto = new StockOperationItemDTO();
        dto.setId(item.getId());
        dto.setUuid(item.getUuid());
        dto.setQuantity(item.getQuantity());
        dto.setPurchasePrice(item.getPurchasePrice());
        dto.setQuantityReceived(item.getQuantityReceived());
        dto.setQuantityRequested(item.getQuantityRequested());
        StockItem stockItem = item.getStockItem();
        if (stockItem != null) {
            dto.setStockItemId(stockItem.getId());
            dto.setStockItemUuid(stockItem.getUuid());
            dto.setCommonName(stockItem.getCommonName());
            dto.setAcronym(stockItem.getAcronym());
            dto.setHasExpiration(Boolean.TRUE.equals(stockItem.getHasExpiration()));
            if (stockItem.getDrug() != null) {
                dto.setStockItemDrugId(stockItem.getDrug().getDrugId());
            }
            if (stockItem.getConcept() != null) {
                dto.setStockItemConceptId(stockItem.getConcept().getConceptId());
            }
            if (filter.getIncludeStockUnitName()) {
                dto.setStockItemName(stockItemName(stockItem));
            }
        }
        setOperationItemUom(dto, item.getStockItemPackagingUOM(), "quantity", filter.getIncludePackagingUnitName());
        setOperationItemUom(dto, item.getQuantityReceivedPackagingUOM(), "received",
            filter.getIncludePackagingUnitName());
        setOperationItemUom(dto, item.getQuantityRequestedPackagingUOM(), "requested",
            filter.getIncludePackagingUnitName());
        StockBatch stockBatch = item.getStockBatch();
        if (stockBatch != null) {
            dto.setStockBatchId(stockBatch.getId());
            dto.setStockBatchUuid(stockBatch.getUuid());
            dto.setBatchNo(stockBatch.getBatchNo());
            dto.setExpiration(stockBatch.getExpiration());
        }
        StockOperation stockOperation = item.getStockOperation();
        if (stockOperation != null) {
            dto.setStockOperationId(stockOperation.getId());
            dto.setStockOperationUuid(stockOperation.getUuid());
        }
        return dto;
    }

    private void setOperationItemUom(
            StockOperationItemDTO dto, StockItemPackagingUOM uom, String target, boolean includeName) {
        if (uom == null) {
            return;
        }
        Concept packaging = uom.getPackagingUom();
        if ("quantity".equals(target)) {
            dto.setStockItemPackagingUOMUuid(uom.getUuid());
            dto.setStockItemPackagingUOMFactor(uom.getFactor());
            if (packaging != null) {
                dto.setPackagingUoMId(packaging.getConceptId());
                if (includeName) {
                    dto.setStockItemPackagingUOMName(conceptName(packaging));
                }
            }
        } else if ("received".equals(target)) {
            dto.setQuantityReceivedPackagingUOMUuid(uom.getUuid());
            dto.setQuantityReceivedPackagingUOMFactor(uom.getFactor());
            if (packaging != null) {
                dto.setQuantityReceivedPackagingUOMUoMId(packaging.getConceptId());
                if (includeName) {
                    dto.setQuantityReceivedPackagingUOMName(conceptName(packaging));
                }
            }
        } else if ("requested".equals(target)) {
            dto.setQuantityRequestedPackagingUOMUuid(uom.getUuid());
            dto.setQuantityRequestedPackagingUOMFactor(uom.getFactor());
            if (packaging != null) {
                dto.setQuantityRequestedPackagingUOMUoMId(packaging.getConceptId());
                if (includeName) {
                    dto.setQuantityRequestedPackagingUOMName(conceptName(packaging));
                }
            }
        }
    }

    private StockOperationItemCost stockOperationItemToCost(StockOperationItem item) {
        StockOperationItemCost cost = new StockOperationItemCost();
        cost.setId(item.getId());
        cost.setUuid(item.getUuid());
        cost.setQuantity(item.getQuantity());
        StockItem stockItem = item.getStockItem();
        if (stockItem != null) {
            cost.setStockItemId(stockItem.getId());
            cost.setStockItemUuid(stockItem.getUuid());
        }
        StockBatch stockBatch = item.getStockBatch();
        if (stockBatch != null) {
            cost.setStockBatchId(stockBatch.getId());
            cost.setStockBatchUuid(stockBatch.getUuid());
            cost.setBatchNo(stockBatch.getBatchNo());
        }
        StockItemPackagingUOM quantityUom = item.getStockItemPackagingUOM();
        setCostQuantityUom(cost, quantityUom);
        if (stockItem == null || stockItem.getPurchasePrice() == null || stockItem.getPurchasePriceUoM() == null) {
            return cost;
        }
        StockItemPackagingUOM purchaseUom = stockItem.getPurchasePriceUoM();
        setPurchaseCost(cost, stockItem.getPurchasePrice(), purchaseUom, quantityUom);
        return cost;
    }

    private void setCostQuantityUom(StockOperationItemCost cost, StockItemPackagingUOM uom) {
        if (uom == null) {
            return;
        }
        cost.setStockItemPackagingUOMUuid(uom.getUuid());
        if (uom.getPackagingUom() != null) {
            cost.setPackagingUoMId(uom.getPackagingUom().getConceptId());
            cost.setStockItemPackagingUOMName(conceptName(uom.getPackagingUom()));
        }
    }

    private void setPurchaseCost(StockOperationItemCost cost, BigDecimal purchasePrice,
            StockItemPackagingUOM purchaseUom, StockItemPackagingUOM quantityUom) {
        if (quantityUom == null || cost.getQuantity() == null || purchaseUom.getFactor() == null
                || quantityUom.getFactor() == null) {
            setUnitCost(cost, purchasePrice, purchaseUom);
            return;
        }
        if (Objects.equals(purchaseUom.getId(), quantityUom.getId())) {
            setUnitCost(cost, purchasePrice, purchaseUom);
            cost.setTotalCost(cost.getQuantity().multiply(purchasePrice));
            return;
        }
        BigDecimal baseUnitPrice = purchasePrice.divide(purchaseUom.getFactor(), 5, RoundingMode.HALF_EVEN);
        BigDecimal convertedUnitPrice = baseUnitPrice.multiply(quantityUom.getFactor()).setScale(2, RoundingMode.HALF_EVEN);
        setUnitCost(cost, convertedUnitPrice, quantityUom);
        cost.setTotalCost(convertedUnitPrice.multiply(cost.getQuantity()));
    }

    private void setUnitCost(StockOperationItemCost cost, BigDecimal unitCost, StockItemPackagingUOM uom) {
        cost.setUnitCost(unitCost);
        if (uom == null || uom.getPackagingUom() == null) {
            return;
        }
        cost.setUnitCostUOMUuid(uom.getUuid());
        cost.setUnitCostUOMId(uom.getPackagingUom().getConceptId());
        cost.setUnitCostUOMName(conceptName(uom.getPackagingUom()));
    }

    private Result<StockItemTransactionDTO> findStockItemTransactions(
            StockItemTransactionSearchFilter filter, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        StockItemTransactionSearchFilter safeFilter = filter == null ? new StockItemTransactionSearchFilter() : filter;
        List<StockItemTransactionDTO> transactions = queryStockItemTransactions(safeFilter, recordPrivilegeFilters)
                .stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)).reversed())
                .map(this::stockItemTransactionToDto)
                .collect(Collectors.toList());
        return resultFromList(transactions, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private List<StockItemTransaction> queryStockItemTransactions(
            StockItemTransactionSearchFilter filter, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        Set<Integer> allowedPartyIds = allowedPartyIds(recordPrivilegeFilters);
        if (allowedPartyIds != null && allowedPartyIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (allowedPartyIds != null && filter.getPartyId() != null && !allowedPartyIds.contains(filter.getPartyId())) {
            return new ArrayList<>();
        }
        return query(StockItemTransaction.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(filter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), filter.getUuid()));
            }
            if (filter.getPartyId() != null) {
                predicates.add(cb.equal(root.get("party").get("id"), filter.getPartyId()));
            } else if (allowedPartyIds != null) {
                predicates.add(root.get("party").get("id").in(allowedPartyIds));
            }
            if (filter.getStockOperationId() != null) {
                predicates.add(cb.equal(root.get("stockOperation").get("id"), filter.getStockOperationId()));
            }
            if (filter.getStockItemId() != null) {
                predicates.add(cb.equal(root.get("stockItem").get("id"), filter.getStockItemId()));
            }
            if (filter.getTransactionDateMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Date>get("dateCreated"),
                    filter.getTransactionDateMin()));
            }
            if (filter.getTransactionDateMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("dateCreated"),
                    filter.getTransactionDateMax()));
            }
            if (filter.getIsPatientTransaction() != null) {
                predicates.add(Boolean.TRUE.equals(filter.getIsPatientTransaction())
                        ? cb.isNotNull(root.get("patient"))
                        : cb.isNull(root.get("patient")));
            }
            return predicates;
        });
    }

    private Set<Integer> allowedPartyIds(HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        if (recordPrivilegeFilters == null) {
            return null;
        }
        List<Integer> locationIds = recordPrivilegeFilters.stream()
                .map(RecordPrivilegeFilter::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (locationIds.isEmpty()) {
            return Set.of();
        }
        PartySearchFilter partySearchFilter = new PartySearchFilter();
        partySearchFilter.setIncludeVoided(true);
        partySearchFilter.setLocationIds(locationIds);
        return findParty(partySearchFilter).getData().stream()
                .map(PartyDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private StockItemTransactionDTO stockItemTransactionToDto(StockItemTransaction transaction) {
        StockItemTransactionDTO dto = new StockItemTransactionDTO();
        dto.setDateCreated(transaction.getDateCreated());
        dto.setUuid(transaction.getUuid());
        dto.setQuantity(transaction.getQuantity());
        Party party = transaction.getParty();
        if (party != null) {
            dto.setPartyId(party.getId());
            dto.setPartyUuid(party.getUuid());
            dto.setPartyName(partyName(party));
        }
        Patient patient = transaction.getPatient();
        if (patient != null) {
            dto.setPatientId(patient.getPatientId());
            dto.setPatientUuid(patient.getUuid());
        }
        if (transaction.getOrder() != null) {
            dto.setOrderId(transaction.getOrder().getOrderId());
        }
        if (transaction.getEncounter() != null) {
            dto.setEncounterId(transaction.getEncounter().getEncounterId());
        }
        StockItem stockItem = transaction.getStockItem();
        if (stockItem != null) {
            dto.setStockItemId(stockItem.getId());
            dto.setStockItemUuid(stockItem.getUuid());
        }
        StockBatch stockBatch = transaction.getStockBatch();
        if (stockBatch != null) {
            dto.setStockBatchUuid(stockBatch.getUuid());
            dto.setStockBatchNo(stockBatch.getBatchNo());
            dto.setExpiration(stockBatch.getExpiration());
        }
        setTransactionUom(dto, transaction.getStockItemPackagingUOM());
        StockOperation stockOperation = transaction.getStockOperation();
        if (stockOperation != null) {
            dto.setStockOperationUuid(stockOperation.getUuid());
            dto.setStockOperationStatus(stockOperation.getStatus());
            dto.setStockOperationNumber(stockOperation.getOperationNumber());
            if (stockOperation.getStockOperationType() != null) {
                dto.setStockOperationTypeName(stockOperation.getStockOperationType().getName());
            }
            if (stockOperation.getSource() != null) {
                dto.setOperationSourcePartyId(stockOperation.getSource().getId());
                dto.setOperationSourcePartyName(partyName(stockOperation.getSource()));
            }
            if (stockOperation.getDestination() != null) {
                dto.setOperationDestinationPartyId(stockOperation.getDestination().getId());
                dto.setOperationDestinationPartyName(partyName(stockOperation.getDestination()));
            }
        }
        return dto;
    }

    private void setTransactionUom(StockItemTransactionDTO dto, StockItemPackagingUOM uom) {
        if (uom == null) {
            return;
        }
        dto.setStockItemPackagingUOMUuid(uom.getUuid());
        dto.setPackagingUomFactor(uom.getFactor());
        if (uom.getPackagingUom() != null) {
            dto.setPackagingUoMId(uom.getPackagingUom().getConceptId());
            dto.setPackagingUomName(conceptName(uom.getPackagingUom()));
        }
    }

    private List<StockItemInventory> getStockBatchLocationInventory(List<Integer> stockBatchIds) {
        if (stockBatchIds == null || stockBatchIds.isEmpty()) {
            return new ArrayList<>();
        }
        Date today = DateUtil.today();
        Map<String, StockItemInventory> inventoryByKey = new LinkedHashMap<>();
        List<StockItemTransaction> transactions = query(StockItemTransaction.class, (cb, root) -> predicates(
            root.get("stockBatch").get("id").in(stockBatchIds),
            cb.or(cb.isNull(root.get("stockBatch").get("expiration")),
                cb.greaterThan(root.<Date>get("stockBatch").get("expiration"), today))));
        for (StockItemTransaction transaction : transactions) {
            if (transaction.getStockBatch() == null || transaction.getStockItemPackagingUOM() == null
                    || transaction.getQuantity() == null) {
                continue;
            }
            StockItemPackagingUOM uom = transaction.getStockItemPackagingUOM();
            if (uom.getFactor() == null) {
                continue;
            }
            StockBatch batch = transaction.getStockBatch();
            StockItem stockItem = transaction.getStockItem();
            Party party = transaction.getParty();
            Integer partyId = party == null ? null : party.getId();
            Integer stockItemId = stockItem == null ? null : stockItem.getId();
            String key = partyId + ":" + stockItemId + ":" + batch.getId();
            StockItemInventory inventory = inventoryByKey.computeIfAbsent(key,
                ignored -> stockItemInventory(party, stockItem, batch));
            BigDecimal existingQuantity = inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity();
            inventory.setQuantity(existingQuantity.add(transaction.getQuantity().multiply(uom.getFactor())));
        }
        List<StockItemInventory> result = new ArrayList<>(inventoryByKey.values());
        applyPreferredInventoryUoms(result);
        return result;
    }

    private StockItemInventory stockItemInventory(Party party, StockItem stockItem, StockBatch batch) {
        StockItemInventory inventory = new StockItemInventory();
        if (party != null) {
            inventory.setPartyId(party.getId());
            inventory.setPartyUuid(party.getUuid());
            inventory.setPartyName(partyName(party));
            if (party.getLocation() != null) {
                inventory.setLocationUuid(party.getLocation().getUuid());
            }
        }
        if (stockItem != null) {
            inventory.setStockItemId(stockItem.getId());
            inventory.setStockItemUuid(stockItem.getUuid());
            inventory.setCommonName(stockItem.getCommonName());
            inventory.setAcronym(stockItem.getAcronym());
            if (stockItem.getDrug() != null) {
                inventory.setDrugId(stockItem.getDrug().getDrugId());
                inventory.setDrugUuid(stockItem.getDrug().getUuid());
                inventory.setDrugName(stockItem.getDrug().getName());
                inventory.setDrugStrength(stockItem.getDrug().getStrength());
            }
            if (stockItem.getConcept() != null) {
                inventory.setConceptId(stockItem.getConcept().getConceptId());
                inventory.setConceptUuid(stockItem.getConcept().getUuid());
                inventory.setConceptName(conceptName(stockItem.getConcept()));
            }
            if (stockItem.getCategory() != null) {
                inventory.setStockItemCategoryName(conceptName(stockItem.getCategory()));
            }
        }
        inventory.setStockBatchId(batch.getId());
        inventory.setStockBatchUuid(batch.getUuid());
        inventory.setBatchNumber(batch.getBatchNo());
        inventory.setExpiration(batch.getExpiration());
        inventory.setQuantity(BigDecimal.ZERO);
        return inventory;
    }

    private void applyPreferredInventoryUoms(List<StockItemInventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return;
        }
        StockItemPackagingUOMSearchFilter uomFilter = new StockItemPackagingUOMSearchFilter();
        uomFilter.setStockItemIds(inventories.stream()
                .map(StockItemInventory::getStockItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        Map<Integer, List<StockItemPackagingUOMDTO>> uomsByStockItem = findStockItemPackagingUOMs(uomFilter).getData()
                .stream()
                .collect(Collectors.groupingBy(StockItemPackagingUOMDTO::getStockItemId));
        boolean uomPriorityIsBigToSmall = GlobalProperties.uomPriorityIsBigToSmall();
        for (StockItemInventory inventory : inventories) {
            List<StockItemPackagingUOMDTO> uoms = uomsByStockItem.get(inventory.getStockItemId());
            StockItemPackagingUOMDTO preferredUom = preferredPackagingUom(
                inventory.getQuantity(), uoms, false, uomPriorityIsBigToSmall, null);
            if (preferredUom == null || preferredUom.getFactor() == null || BigDecimal.ZERO.compareTo(preferredUom.getFactor()) == 0) {
                continue;
            }
            inventory.setQuantity(inventory.getQuantity().divide(preferredUom.getFactor(), 5, RoundingMode.HALF_EVEN));
            inventory.setQuantityUoM(preferredUom.getPackagingUomName());
            inventory.setQuantityUoMUuid(preferredUom.getUuid());
            inventory.setQuantityFactor(preferredUom.getFactor());
        }
    }

    private StockInventoryResult getStockInventory(StockItemInventorySearchFilter filter,
            HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        StockItemInventorySearchFilter safeFilter = filter == null ? new StockItemInventorySearchFilter() : filter;
        List<StockItemInventory> inventories = computeStockInventory(safeFilter, recordPrivilegeFilters);
        StockInventoryResult result = new StockInventoryResult();
        result.setTotalRecordCount((long) inventories.size());
        Integer limit = safeFilter.getLimit() == null || safeFilter.getLimit() <= 0 ? null : safeFilter.getLimit();
        Result<StockItemInventory> page = resultFromList(inventories, safeFilter.getStartIndex(), limit);
        result.setData(page.getData());
        result.setPageIndex(page.getPageIndex());
        result.setPageSize(page.getPageSize());
        result.setTotals(inventoryTotals(safeFilter, inventories));
        return result;
    }

    private <T extends StockItemInventory> void getStockInventory(StockItemInventorySearchFilter filter,
            HashSet<RecordPrivilegeFilter> recordPrivilegeFilters, Function<T, Boolean> consumer, Class<T> resultClass) {
        if (consumer == null) {
            return;
        }
        for (StockItemInventory inventory : computeStockInventory(
                filter == null ? new StockItemInventorySearchFilter() : filter, recordPrivilegeFilters)) {
            if (!Boolean.TRUE.equals(consumer.apply((T) inventory))) {
                break;
            }
        }
    }

    private StockInventoryResult postProcessInventoryResult(StockItemInventorySearchFilter filter,
            StockInventoryResult result) {
        StockInventoryResult safeResult = result == null ? new StockInventoryResult() : result;
        StockItemInventorySearchFilter safeFilter = filter == null ? new StockItemInventorySearchFilter() : filter;
        List<StockItemInventory> data = safeResult.getData() == null ? new ArrayList<>() : safeResult.getData();
        if (safeFilter.getDoSetQuantityUoM()) {
            applyPreferredInventoryUoms(data);
        }
        safeResult.setTotals(inventoryTotals(safeFilter, data));
        return safeResult;
    }

    private Result<StockItemInventory> getMovingStockInventory(StockItemInventorySearchFilter filter,
            boolean mostMoving) {
        StockItemInventorySearchFilter safeFilter = filter == null ? new StockItemInventorySearchFilter() : filter;
        List<StockItemInventory> inventories = computeStockInventory(safeFilter, null);
        Comparator<StockItemInventory> comparator = Comparator.comparing(
            inventory -> inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity().abs(),
            Comparator.nullsLast(BigDecimal::compareTo));
        if (mostMoving) {
            comparator = comparator.reversed();
        }
        inventories.sort(comparator.thenComparing(StockItemInventory::getStockItemId,
            Comparator.nullsLast(Integer::compareTo)));
        return resultFromList(inventories, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private void getStockInventoryForecastData(StockItemInventorySearchFilter filter,
            Function<Object[], Boolean> consumer) {
        if (consumer == null) {
            return;
        }
        StockItemInventorySearchFilter safeFilter = filter == null ? new StockItemInventorySearchFilter() : filter;
        if (!hasUsableForecastFilter(safeFilter)) {
            return;
        }
        boolean groupByParty = true;
        boolean groupByStockItem = true;
        boolean groupByBatch = true;
        StockItemInventorySearchFilter.InventoryGroupBy groupBy = safeFilter.getInventoryGroupBy();
        if (groupBy != null) {
            groupByParty = groupBy == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem
                    || groupBy == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo;
            groupByStockItem = groupBy == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItem
                    || groupBy == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo
                    || groupBy == StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly;
            groupByBatch = groupBy == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo;
        }
        List<LocalDate> months = forecastMonths(safeFilter);
        Map<String, ForecastAccumulator> accumulators = new LinkedHashMap<>();
        for (StockItemTransaction transaction : forecastTransactions(safeFilter, false)) {
            if (!matchesForecastFilters(transaction, safeFilter)) {
                continue;
            }
            Integer partyId = groupByParty && transaction.getParty() != null ? transaction.getParty().getId() : null;
            Integer stockItemId = groupByStockItem && transaction.getStockItem() != null
                    ? transaction.getStockItem().getId()
                    : null;
            Integer stockBatchId = groupByBatch && transaction.getStockBatch() != null
                    ? transaction.getStockBatch().getId()
                    : null;
            ForecastAccumulator accumulator = accumulators.computeIfAbsent(
                forecastKey(partyId, stockItemId, stockBatchId, null),
                ignored -> new ForecastAccumulator(partyId, stockItemId, stockBatchId, null, months.size()));
            addForecastTransaction(accumulator, transaction, months, safeFilter, false);
        }
        for (ForecastAccumulator accumulator : accumulators.values()) {
            if (!Boolean.TRUE.equals(consumer.apply(accumulator.toForecastRow()))) {
                break;
            }
        }
    }

    private void getStockInventoryExpiryForecastData(StockItemInventorySearchFilter filter,
            Function<Object[], Boolean> consumer) {
        if (consumer == null) {
            return;
        }
        StockItemInventorySearchFilter safeFilter = filter == null ? new StockItemInventorySearchFilter() : filter;
        if (!hasUsableForecastFilter(safeFilter)) {
            return;
        }
        List<LocalDate> months = forecastMonths(safeFilter);
        Map<String, ForecastAccumulator> accumulators = new LinkedHashMap<>();
        for (StockItemTransaction transaction : forecastTransactions(safeFilter, true)) {
            if (!matchesForecastFilters(transaction, safeFilter)) {
                continue;
            }
            StockItem stockItem = transaction.getStockItem();
            StockBatch stockBatch = transaction.getStockBatch();
            Integer stockItemId = stockItem == null ? null : stockItem.getId();
            Integer stockBatchId = stockBatch == null ? null : stockBatch.getId();
            Date expiration = stockBatch == null ? null : stockBatch.getExpiration();
            ForecastAccumulator accumulator = accumulators.computeIfAbsent(
                forecastKey(null, stockItemId, stockBatchId, expiration),
                ignored -> new ForecastAccumulator(null, stockItemId, stockBatchId, expiration, months.size()));
            addForecastTransaction(accumulator, transaction, months, safeFilter, true);
        }
        for (ForecastAccumulator accumulator : accumulators.values()) {
            if (!Boolean.TRUE.equals(consumer.apply(accumulator.toExpiryForecastRow()))) {
                break;
            }
        }
    }

    private boolean hasUsableForecastFilter(StockItemInventorySearchFilter filter) {
        if (filter.getStartDate() == null || filter.getEndDate() == null) {
            return false;
        }
        if (!filter.isRequireItemGroupFilters()) {
            return true;
        }
        return filter.getItemGroupFilters() != null
                && filter.getItemGroupFilters().stream().anyMatch(this::hasForecastItemGroupConstraint);
    }

    private List<StockItemTransaction> forecastTransactions(StockItemInventorySearchFilter filter,
            boolean requireExpiringBatch) {
        Date today = DateUtil.today();
        return query(StockItemTransaction.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockItemTransaction, StockBatch> batch = root.join("stockBatch", JoinType.INNER);
            Join<StockItemTransaction, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            Join<StockItemTransaction, StockItemPackagingUOM> uom = root.join("stockItemPackagingUOM", JoinType.INNER);
            predicates.add(cb.isNotNull(uom.get("id")));
            if (requireExpiringBatch) {
                predicates.add(cb.greaterThan(batch.<Date>get("expiration"), filter.getStartDate()));
            } else {
                predicates.add(cb.or(cb.isNull(batch.get("expiration")),
                    cb.greaterThan(batch.<Date>get("expiration"), filter.getStartDate())));
            }
            if (filter.getStockItemCategoryConceptId() != null) {
                predicates.add(cb.equal(stockItem.get("category").get("conceptId"),
                    filter.getStockItemCategoryConceptId()));
            }
            return predicates;
        }).stream()
                .filter(transaction -> transaction.getStockBatch() != null)
                .filter(transaction -> transaction.getStockItemPackagingUOM() != null)
                .filter(transaction -> !requireExpiringBatch || transaction.getStockBatch().getExpiration() != null)
                .filter(transaction -> transaction.getStockBatch().getExpiration() == null
                        || transaction.getStockBatch().getExpiration().after(today)
                        || transaction.getDateCreated() != null)
                .sorted(Comparator
                        .comparing((StockItemTransaction transaction) -> transaction.getStockItem() == null
                                ? null
                                : transaction.getStockItem().getId(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(transaction -> transaction.getStockBatch() == null
                                ? null
                                : transaction.getStockBatch().getExpiration(), Comparator.nullsLast(Date::compareTo))
                        .thenComparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

    private boolean matchesForecastFilters(StockItemTransaction transaction, StockItemInventorySearchFilter filter) {
        if (filter.getUnRestrictedPartyIds() != null && !filter.getUnRestrictedPartyIds().isEmpty()) {
            Integer partyId = transaction.getParty() == null ? null : transaction.getParty().getId();
            if (!filter.getUnRestrictedPartyIds().contains(partyId)) {
                return false;
            }
        }
        if (filter.getItemGroupFilters() == null || filter.getItemGroupFilters().isEmpty()) {
            return true;
        }
        return filter.getItemGroupFilters().stream()
                .filter(this::hasForecastItemGroupConstraint)
                .anyMatch(group -> forecastItemGroupMatches(transaction, group));
    }

    private boolean hasForecastItemGroupConstraint(StockItemInventorySearchFilter.ItemGroupFilter group) {
        return group != null
                && ((group.getPartyIds() != null && !group.getPartyIds().isEmpty())
                        || (group.getPartyUuids() != null && !group.getPartyUuids().isEmpty())
                        || group.getStockItemId() != null
                        || StringUtils.isNotBlank(group.getStockItemUuid())
                        || (group.getStockBatchIds() != null && !group.getStockBatchIds().isEmpty()));
    }

    private boolean forecastItemGroupMatches(StockItemTransaction transaction,
            StockItemInventorySearchFilter.ItemGroupFilter group) {
        Party party = transaction.getParty();
        StockItem stockItem = transaction.getStockItem();
        StockBatch batch = transaction.getStockBatch();
        if (group.getPartyIds() != null && !group.getPartyIds().isEmpty()
                && (party == null || !group.getPartyIds().contains(party.getId()))) {
            return false;
        }
        if (group.getPartyUuids() != null && !group.getPartyUuids().isEmpty()
                && (party == null || !group.getPartyUuids().contains(party.getUuid()))) {
            return false;
        }
        if (group.getStockItemId() != null
                && (stockItem == null || !Objects.equals(stockItem.getId(), group.getStockItemId()))) {
            return false;
        }
        if (StringUtils.isNotBlank(group.getStockItemUuid())
                && (stockItem == null || !group.getStockItemUuid().equals(stockItem.getUuid()))) {
            return false;
        }
        return group.getStockBatchIds() == null || group.getStockBatchIds().isEmpty()
                || (batch != null && group.getStockBatchIds().contains(batch.getId()));
    }

    private void addForecastTransaction(ForecastAccumulator accumulator, StockItemTransaction transaction,
            List<LocalDate> months, StockItemInventorySearchFilter filter, boolean expiryOnly) {
        BigDecimal quantity = stockItemTransactionQuantity(transaction);
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        StockBatch batch = transaction.getStockBatch();
        Date today = DateUtil.today();
        if (batch != null && (batch.getExpiration() == null || batch.getExpiration().after(today))) {
            accumulator.quantity = accumulator.quantity.add(quantity);
        }
        if (transaction.getQuantity() == null || transaction.getQuantity().compareTo(BigDecimal.ZERO) >= 0
                || transaction.getDateCreated() == null || transaction.getDateCreated().before(filter.getStartDate())
                || transaction.getDateCreated().after(filter.getEndDate())) {
            return;
        }
        int monthIndex = forecastMonthIndex(transaction.getDateCreated(), months);
        if (monthIndex >= 0) {
            accumulator.consumedByMonth[monthIndex] = accumulator.consumedByMonth[monthIndex]
                    .add(quantity.abs());
        }
    }

    private List<LocalDate> forecastMonths(StockItemInventorySearchFilter filter) {
        List<LocalDate> months = new ArrayList<>();
        LocalDate start = filter.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .withDayOfMonth(1);
        LocalDate end = filter.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .withDayOfMonth(1);
        while (!start.isAfter(end)) {
            months.add(start);
            start = start.plusMonths(1);
        }
        return months;
    }

    private int forecastMonthIndex(Date date, List<LocalDate> months) {
        LocalDate month = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1);
        for (int i = 0; i < months.size(); i++) {
            if (months.get(i).equals(month)) {
                return i;
            }
        }
        return -1;
    }

    private String forecastKey(Integer partyId, Integer stockItemId, Integer stockBatchId, Date expiration) {
        return partyId + ":" + stockItemId + ":" + stockBatchId + ":"
                + (expiration == null ? "" : expiration.getTime());
    }

    private static class ForecastAccumulator {

        private final Integer partyId;

        private final Integer stockItemId;

        private final Integer stockBatchId;

        private final Date expiration;

        private final BigDecimal[] consumedByMonth;

        private BigDecimal quantity = BigDecimal.ZERO;

        ForecastAccumulator(Integer partyId, Integer stockItemId, Integer stockBatchId, Date expiration,
                int monthCount) {
            this.partyId = partyId;
            this.stockItemId = stockItemId;
            this.stockBatchId = stockBatchId;
            this.expiration = expiration;
            this.consumedByMonth = new BigDecimal[monthCount];
            for (int i = 0; i < consumedByMonth.length; i++) {
                consumedByMonth[i] = BigDecimal.ZERO;
            }
        }

        Object[] toForecastRow() {
            Object[] row = new Object[3 + consumedByMonth.length + 1];
            row[0] = partyId;
            row[1] = stockItemId;
            row[2] = stockBatchId;
            for (int i = 0; i < consumedByMonth.length; i++) {
                row[3 + i] = consumedByMonth[i];
            }
            row[row.length - 1] = quantity;
            return row;
        }

        Object[] toExpiryForecastRow() {
            Object[] row = new Object[3 + consumedByMonth.length + 1];
            row[0] = stockItemId;
            row[1] = stockBatchId;
            row[2] = expiration;
            for (int i = 0; i < consumedByMonth.length; i++) {
                row[3 + i] = consumedByMonth[i];
            }
            row[row.length - 1] = quantity;
            return row;
        }
    }

    private void setStockItemInformation(List<StockItemInventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return;
        }
        Map<Integer, StockItem> stockItems = listByIds(StockItem.class, "id", inventories.stream()
                .map(StockItemInventory::getStockItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(StockItem::getId, Function.identity(), (a, b) -> a));
        Map<Integer, StockBatch> stockBatches = listByIds(StockBatch.class, "id", inventories.stream()
                .map(StockItemInventory::getStockBatchId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(StockBatch::getId, Function.identity(), (a, b) -> a));
        Map<Integer, Party> parties = listByIds(Party.class, "id", inventories.stream()
                .map(StockItemInventory::getPartyId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(Party::getId, Function.identity(), (a, b) -> a));
        for (StockItemInventory inventory : inventories) {
            populateInventoryStockItem(inventory, stockItems.get(inventory.getStockItemId()));
            populateInventoryBatch(inventory, stockBatches.get(inventory.getStockBatchId()));
            populateInventoryParty(inventory, parties.get(inventory.getPartyId()));
        }
    }

    private List<StockItemInventory> computeStockInventory(StockItemInventorySearchFilter filter,
            HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        List<StockItemInventorySearchFilter.ItemGroupFilter> itemGroupFilters = filter.getItemGroupFilters();
        if (filter.isRequireItemGroupFilters() && (itemGroupFilters == null || itemGroupFilters.isEmpty())) {
            return new ArrayList<>();
        }
        Set<Integer> allowedPartyIds = allowedPartyIds(recordPrivilegeFilters);
        if (allowedPartyIds != null && allowedPartyIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Integer> requestedPartyIds = inventoryPartyIds(itemGroupFilters);
        Set<Integer> unrestrictedPartyIds = filter.getUnRestrictedPartyIds() == null ? new HashSet<>()
                : new HashSet<>(filter.getUnRestrictedPartyIds());
        if (allowedPartyIds != null) {
            requestedPartyIds = requestedPartyIds.isEmpty() ? new HashSet<>(allowedPartyIds)
                    : requestedPartyIds.stream()
                            .filter(partyId -> allowedPartyIds.contains(partyId) || unrestrictedPartyIds.contains(partyId))
                            .collect(Collectors.toSet());
            if (requestedPartyIds.isEmpty()) {
                return new ArrayList<>();
            }
        }
        Set<Integer> stockItemIds = inventoryStockItemIds(itemGroupFilters);
        Set<Integer> stockBatchIds = inventoryStockBatchIds(itemGroupFilters);
        Date today = DateUtil.today();
        Set<Integer> queryPartyIds = requestedPartyIds;
        StockItemInventorySearchFilter queryFilter = filter;
        Map<String, StockItemInventory> inventoryByKey = new LinkedHashMap<>();
        for (StockItemTransaction transaction : query(StockItemTransaction.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!queryPartyIds.isEmpty()) {
                predicates.add(root.get("party").get("id").in(queryPartyIds));
            }
            if (!stockItemIds.isEmpty()) {
                predicates.add(root.get("stockItem").get("id").in(stockItemIds));
            }
            if (!stockBatchIds.isEmpty()) {
                predicates.add(root.get("stockBatch").get("id").in(stockBatchIds));
            }
            if (queryFilter.getDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("dateCreated"), queryFilter.getDate()));
            }
            if (queryFilter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Date>get("dateCreated"), queryFilter.getStartDate()));
            }
            if (queryFilter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("dateCreated"), queryFilter.getEndDate()));
            }
            if (queryFilter.getStockItemCategoryConceptId() != null) {
                predicates.add(cb.equal(root.get("stockItem").get("category").get("conceptId"),
                    queryFilter.getStockItemCategoryConceptId()));
            }
            if (Boolean.TRUE.equals(queryFilter.getRequireNonExpiredStockBatches())) {
                predicates.add(cb.or(cb.isNull(root.get("stockBatch").get("expiration")),
                    cb.greaterThan(root.<Date>get("stockBatch").get("expiration"), today)));
            }
            return predicates;
        })) {
            addInventoryTransaction(filter, inventoryByKey, transaction);
        }
        List<StockItemInventory> inventories = new ArrayList<>(inventoryByKey.values());
        inventories.removeIf(inventory -> inventory.getQuantity() == null
                || BigDecimal.ZERO.compareTo(inventory.getQuantity()) == 0);
        inventories.sort(Comparator
                .comparing(StockItemInventory::getPartyId, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(StockItemInventory::getStockItemId, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(StockItemInventory::getStockBatchId, Comparator.nullsLast(Integer::compareTo)));
        if (filter.getDoSetQuantityUoM()) {
            applyPreferredInventoryUoms(inventories);
        }
        return inventories;
    }

    private void addInventoryTransaction(StockItemInventorySearchFilter filter,
            Map<String, StockItemInventory> inventoryByKey, StockItemTransaction transaction) {
        BigDecimal quantity = transactionQuantityInBaseUnits(transaction);
        if (quantity == null || transaction.getStockItem() == null) {
            return;
        }
        Party party = filter.getInventoryGroupBy() == StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly
                ? null
                : transaction.getParty();
        StockBatch batch = filter.getInventoryGroupBy() == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo
                ? transaction.getStockBatch()
                : null;
        String key = inventoryKey(party == null ? null : party.getId(), transaction.getStockItem().getId(),
            batch == null ? null : batch.getId());
        StockItemInventory inventory = inventoryByKey.computeIfAbsent(key,
            ignored -> inventoryRow(party, transaction.getStockItem(), batch));
        inventory.setQuantity((inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity()).add(quantity));
    }

    private StockItemInventory inventoryRow(Party party, StockItem stockItem, StockBatch batch) {
        StockItemInventory inventory = new StockItemInventory();
        inventory.setQuantity(BigDecimal.ZERO);
        populateInventoryParty(inventory, party);
        populateInventoryStockItem(inventory, stockItem);
        populateInventoryBatch(inventory, batch);
        return inventory;
    }

    private void populateInventoryParty(StockItemInventory inventory, Party party) {
        if (party == null) {
            return;
        }
        inventory.setPartyId(party.getId());
        inventory.setPartyUuid(party.getUuid());
        inventory.setPartyName(partyName(party));
        if (party.getLocation() != null) {
            inventory.setLocationUuid(party.getLocation().getUuid());
        }
    }

    private void populateInventoryStockItem(StockItemInventory inventory, StockItem stockItem) {
        if (stockItem == null) {
            return;
        }
        inventory.setStockItemId(stockItem.getId());
        inventory.setStockItemUuid(stockItem.getUuid());
        inventory.setCommonName(stockItem.getCommonName());
        inventory.setAcronym(stockItem.getAcronym());
        inventory.setReorderLevel(stockItem.getReorderLevel());
        if (stockItem.getDrug() != null) {
            inventory.setDrugId(stockItem.getDrug().getDrugId());
            inventory.setDrugUuid(stockItem.getDrug().getUuid());
            inventory.setDrugName(stockItem.getDrug().getName());
            inventory.setDrugStrength(stockItem.getDrug().getStrength());
        }
        if (stockItem.getConcept() != null) {
            inventory.setConceptId(stockItem.getConcept().getConceptId());
            inventory.setConceptUuid(stockItem.getConcept().getUuid());
            inventory.setConceptName(conceptName(stockItem.getConcept()));
        }
        if (stockItem.getCategory() != null) {
            inventory.setStockItemCategoryName(conceptName(stockItem.getCategory()));
        }
        StockItemPackagingUOM reorderUom = stockItem.getReorderLevelUOM();
        if (reorderUom != null) {
            inventory.setReorderLevelFactor(reorderUom.getFactor());
            if (reorderUom.getPackagingUom() != null) {
                inventory.setReorderLevelUoM(conceptName(reorderUom.getPackagingUom()));
            }
        }
    }

    private void populateInventoryBatch(StockItemInventory inventory, StockBatch batch) {
        if (batch == null) {
            return;
        }
        inventory.setStockBatchId(batch.getId());
        inventory.setStockBatchUuid(batch.getUuid());
        inventory.setBatchNumber(batch.getBatchNo());
        inventory.setExpiration(batch.getExpiration());
    }

    private Set<Integer> inventoryPartyIds(List<StockItemInventorySearchFilter.ItemGroupFilter> filters) {
        if (filters == null) {
            return new HashSet<>();
        }
        Set<Integer> partyIds = filters.stream()
                .filter(Objects::nonNull)
                .flatMap(filter -> filter.getPartyIds() == null ? java.util.stream.Stream.empty()
                        : filter.getPartyIds().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> partyUuids = filters.stream()
                .filter(Objects::nonNull)
                .flatMap(filter -> filter.getPartyUuids() == null ? java.util.stream.Stream.empty()
                        : filter.getPartyUuids().stream())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (!partyUuids.isEmpty()) {
            query(Party.class, (cb, root) -> predicates(root.get("uuid").in(partyUuids))).stream()
                    .map(Party::getId)
                    .filter(Objects::nonNull)
                    .forEach(partyIds::add);
        }
        return partyIds;
    }

    private Set<Integer> inventoryStockItemIds(List<StockItemInventorySearchFilter.ItemGroupFilter> filters) {
        if (filters == null) {
            return new HashSet<>();
        }
        Set<Integer> stockItemIds = filters.stream()
                .filter(Objects::nonNull)
                .map(StockItemInventorySearchFilter.ItemGroupFilter::getStockItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> stockItemUuids = filters.stream()
                .filter(Objects::nonNull)
                .map(StockItemInventorySearchFilter.ItemGroupFilter::getStockItemUuid)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (!stockItemUuids.isEmpty()) {
            query(StockItem.class, (cb, root) -> predicates(root.get("uuid").in(stockItemUuids))).stream()
                    .map(StockItem::getId)
                    .filter(Objects::nonNull)
                    .forEach(stockItemIds::add);
        }
        return stockItemIds;
    }

    private Set<Integer> inventoryStockBatchIds(List<StockItemInventorySearchFilter.ItemGroupFilter> filters) {
        if (filters == null) {
            return new HashSet<>();
        }
        return filters.stream()
                .filter(Objects::nonNull)
                .flatMap(filter -> filter.getStockBatchIds() == null ? java.util.stream.Stream.empty()
                        : filter.getStockBatchIds().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<StockItemInventory> inventoryTotals(StockItemInventorySearchFilter filter,
            List<StockItemInventory> inventories) {
        if (filter.getTotalBy() == null || inventories == null || inventories.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, StockItemInventory> totalsByKey = new LinkedHashMap<>();
        for (StockItemInventory inventory : inventories) {
            String key = inventoryKey(
                filter.getTotalBy() == StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly ? null
                        : inventory.getPartyId(),
                inventory.getStockItemId(),
                filter.getTotalBy() == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo
                        ? inventory.getStockBatchId() : null);
            StockItemInventory total = totalsByKey.computeIfAbsent(key, ignored -> {
                StockItemInventory row = new StockItemInventory();
                row.setPartyId(filter.getTotalBy() == StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly
                        ? null : inventory.getPartyId());
                row.setPartyName(filter.getTotalBy() == StockItemInventorySearchFilter.InventoryGroupBy.StockItemOnly
                        ? null : inventory.getPartyName());
                row.setStockItemId(inventory.getStockItemId());
                row.setStockItemUuid(inventory.getStockItemUuid());
                row.setCommonName(inventory.getCommonName());
                row.setDrugName(inventory.getDrugName());
                row.setConceptName(inventory.getConceptName());
                row.setStockBatchId(filter.getTotalBy() == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo
                        ? inventory.getStockBatchId() : null);
                row.setBatchNumber(filter.getTotalBy() == StockItemInventorySearchFilter.InventoryGroupBy.LocationStockItemBatchNo
                        ? inventory.getBatchNumber() : null);
                row.setQuantity(BigDecimal.ZERO);
                return row;
            });
            total.setQuantity(total.getQuantity().add(inventory.getQuantity() == null ? BigDecimal.ZERO : inventory.getQuantity()));
        }
        return new ArrayList<>(totalsByKey.values());
    }

    private String inventoryKey(Integer partyId, Integer stockItemId, Integer stockBatchId) {
        return partyId + ":" + stockItemId + ":" + stockBatchId;
    }

    private StockItemPackagingUOMDTO preferredPackagingUom(BigDecimal value, List<StockItemPackagingUOMDTO> uoms,
            boolean isDispensing, boolean uomPriorityIsBigToSmall, Integer preferredStockItemPackagingUomId) {
        if (value == null || uoms == null || uoms.isEmpty()) {
            return null;
        }
        Comparator<StockItemPackagingUOMDTO> comparator = Comparator
                .comparing(StockItemPackagingUOMDTO::getIsDefaultStockOperationsUoM,
                    Comparator.nullsLast(Boolean::compareTo))
                .reversed();
        if (isDispensing || !uomPriorityIsBigToSmall) {
            comparator = comparator.thenComparing(StockItemPackagingUOMDTO::getFactor,
                Comparator.nullsLast(BigDecimal::compareTo));
        } else {
            comparator = comparator.thenComparing(Comparator
                    .comparing(StockItemPackagingUOMDTO::getFactor, Comparator.nullsLast(BigDecimal::compareTo))
                    .reversed());
        }
        uoms.sort(comparator);
        StockItemPackagingUOMDTO foundUom = null;
        boolean checkedPreferred = false;
        boolean checkPreferred = preferredStockItemPackagingUomId != null;
        for (StockItemPackagingUOMDTO uom : uoms) {
            if (uom.getFactor() == null || BigDecimal.ZERO.compareTo(uom.getFactor()) == 0) {
                continue;
            }
            boolean isPreferred = checkPreferred && preferredStockItemPackagingUomId.equals(uom.getId());
            if (value.divide(uom.getFactor(), 5, RoundingMode.HALF_EVEN).abs().compareTo(BigDecimal.ONE) >= 0) {
                if (!checkPreferred || isPreferred) {
                    return uom;
                }
                if (checkedPreferred) {
                    return foundUom == null ? uom : foundUom;
                }
                if (foundUom == null) {
                    foundUom = uom;
                }
            }
            if (isPreferred) {
                checkedPreferred = true;
            }
        }
        return checkedPreferred && foundUom != null ? foundUom : uoms.get(0);
    }

    private StockOperationType stockOperationTypeByType(String type) {
        return first(query(StockOperationType.class,
            (cb, root) -> predicates(cb.equal(root.get("operationType"), type))));
    }

    private Result<StockOperationLinkDTO> findStockOperationLinks(
            String parentOrChildStockOperationUuid, String childStockOperationUuid) {
        if (StringUtils.isBlank(parentOrChildStockOperationUuid) && StringUtils.isBlank(childStockOperationUuid)) {
            return new Result<>(new ArrayList<>(), 0);
        }
        List<StockOperationLinkDTO> links = query(StockOperationLink.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockOperationLink, StockOperation> parent = root.join("parent", JoinType.INNER);
            Join<StockOperationLink, StockOperation> child = root.join("child", JoinType.INNER);
            if (StringUtils.isNotBlank(parentOrChildStockOperationUuid)) {
                predicates.add(cb.or(
                    cb.equal(parent.get("uuid"), parentOrChildStockOperationUuid),
                    cb.equal(child.get("uuid"), parentOrChildStockOperationUuid)));
            }
            if (StringUtils.isNotBlank(childStockOperationUuid)) {
                predicates.add(cb.equal(child.get("uuid"), childStockOperationUuid));
            }
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockOperationLinkToDto)
                .collect(Collectors.toList());
        return resultFromList(links, 0, 10);
    }

    private StockOperationLinkDTO stockOperationLinkToDto(StockOperationLink link) {
        StockOperationLinkDTO dto = new StockOperationLinkDTO();
        dto.setId(link.getId() == null ? 0 : link.getId());
        dto.setUuid(link.getUuid());
        StockOperation parent = link.getParent();
        if (parent != null) {
            dto.setParentUuid(parent.getUuid());
            dto.setParentOperationNumber(parent.getOperationNumber());
            dto.setParentStatus(parent.getStatus());
            dto.setParentVoided(Boolean.TRUE.equals(parent.getVoided()));
            if (parent.getStockOperationType() != null) {
                dto.setParentOperationTypeName(parent.getStockOperationType().getName());
            }
        }
        StockOperation child = link.getChild();
        if (child != null) {
            dto.setChildUuid(child.getUuid());
            dto.setChildOperationNumber(child.getOperationNumber());
            dto.setChildStatus(child.getStatus());
            dto.setChildVoided(Boolean.TRUE.equals(child.getVoided()));
            if (child.getStockOperationType() != null) {
                dto.setChildOperationTypeName(child.getStockOperationType().getName());
            }
        }
        return dto;
    }

    private Result<StockOperationDTO> findStockOperations(
            StockOperationSearchFilter filter, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        if (recordPrivilegeFilters != null && recordPrivilegeFilters.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }
        StockOperationSearchFilter safeFilter = filter == null ? new StockOperationSearchFilter() : filter;
        List<StockOperationDTO> operations = query(StockOperation.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockOperation, StockOperationType> operationType = root.join("stockOperationType", JoinType.INNER);
            Join<StockOperation, Party> source = root.join("source", JoinType.LEFT);
            Join<StockOperation, Party> destination = root.join("destination", JoinType.LEFT);
            Join<Party, Location> sourceLocation = source.join("location", JoinType.LEFT);
            Join<Party, Location> destinationLocation = destination.join("location", JoinType.LEFT);
            Join<Party, StockSource> sourceStockSource = source.join("stockSource", JoinType.LEFT);
            Join<Party, StockSource> destinationStockSource = destination.join("stockSource", JoinType.LEFT);
            Join<StockSource, Concept> sourceType = sourceStockSource.join("sourceType", JoinType.LEFT);
            Join<StockSource, Concept> destinationType = destinationStockSource.join("sourceType", JoinType.LEFT);

            if (StringUtils.isNotBlank(safeFilter.getStockOperationUuid())) {
                predicates.add(cb.equal(root.get("uuid"), safeFilter.getStockOperationUuid()));
            }
            if (safeFilter.getLocationId() != null) {
                predicates.add(cb.or(
                    cb.equal(sourceLocation.get("locationId"), safeFilter.getLocationId()),
                    cb.equal(destinationLocation.get("locationId"), safeFilter.getLocationId())));
            }
            if (safeFilter.getPartyId() != null) {
                predicates.add(cb.or(
                    cb.equal(source.get("id"), safeFilter.getPartyId()),
                    cb.equal(destination.get("id"), safeFilter.getPartyId())));
            }
            if (safeFilter.getStockSourceId() != null) {
                predicates.add(cb.or(
                    cb.equal(sourceStockSource.get("id"), safeFilter.getStockSourceId()),
                    cb.equal(destinationStockSource.get("id"), safeFilter.getStockSourceId())));
            }
            if (safeFilter.getOperationTypeId() != null && !safeFilter.getOperationTypeId().isEmpty()) {
                predicates.add(operationType.get("id").in(safeFilter.getOperationTypeId()));
            }
            if (safeFilter.getStatus() != null && !safeFilter.getStatus().isEmpty()) {
                predicates.add(root.get("status").in(safeFilter.getStatus()));
            }
            if (safeFilter.getOperationDateMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Date>get("operationDate"),
                    safeFilter.getOperationDateMin()));
            }
            if (safeFilter.getOperationDateMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("operationDate"),
                    safeFilter.getOperationDateMax()));
            }
            if (StringUtils.isNotBlank(safeFilter.getOperationNumber())) {
                String operationNumber = safeFilter.getOperationNumber().replace("%", "");
                if (operationNumber.isEmpty()) {
                    return predicates(cb.disjunction());
                }
                predicates.add(cb.like(cb.upper(root.get("operationNumber").as(String.class)),
                    operationNumber.toUpperCase(Locale.ROOT) + "%"));
            }
            if (Boolean.TRUE.equals(safeFilter.getIsLocationOther())) {
                predicates.add(cb.or(
                    cb.isNotNull(sourceStockSource.get("id")),
                    cb.isNotNull(destinationStockSource.get("id"))));
            }
            if (safeFilter.getSourceTypeIds() != null && !safeFilter.getSourceTypeIds().isEmpty()) {
                predicates.add(cb.or(
                    sourceType.get("conceptId").in(safeFilter.getSourceTypeIds()),
                    destinationType.get("conceptId").in(safeFilter.getSourceTypeIds())));
            }
            if (StringUtils.isNotBlank(safeFilter.getSearchText())) {
                String query = safeFilter.getSearchText().replace("%", "").toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    return predicates(cb.disjunction());
                }
                String term = query + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("operationNumber").as(String.class)), term),
                    cb.like(cb.lower(sourceLocation.get("name").as(String.class)), term),
                    cb.like(cb.lower(destinationLocation.get("name").as(String.class)), term),
                    cb.like(cb.lower(sourceStockSource.get("name").as(String.class)), term),
                    cb.like(cb.lower(destinationStockSource.get("name").as(String.class)), term)));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            if (safeFilter.getStockItemId() != null) {
                Join<StockOperation, StockOperationItem> item = root.join("stockOperationItems", JoinType.INNER);
                predicates.add(cb.equal(item.get("stockItem").get("id"), safeFilter.getStockItemId()));
                predicates.add(cb.isFalse(item.get("voided")));
            }
            addRecordPrivilegePredicates(cb, predicates, operationType, sourceLocation, destinationLocation,
                recordPrivilegeFilters);
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)).reversed())
                .map(this::stockOperationToDto)
                .collect(Collectors.toList());
        return resultFromList(operations, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private void addRecordPrivilegePredicates(CriteriaBuilder cb, List<Predicate> predicates,
            Join<StockOperation, StockOperationType> operationType, Join<Party, Location> sourceLocation,
            Join<Party, Location> destinationLocation, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        if (recordPrivilegeFilters == null) {
            return;
        }
        List<Predicate> allowedScopes = recordPrivilegeFilters.stream()
                .filter(scope -> scope.getLocationId() != null && scope.getOperationTypeId() != null)
                .map(scope -> cb.and(
                    cb.equal(operationType.get("id"), scope.getOperationTypeId()),
                    cb.or(
                        cb.equal(sourceLocation.get("locationId"), scope.getLocationId()),
                        cb.equal(destinationLocation.get("locationId"), scope.getLocationId()))))
                .collect(Collectors.toList());
        predicates.add(allowedScopes.isEmpty() ? cb.disjunction()
                : cb.or(allowedScopes.toArray(new Predicate[0])));
    }

    private StockOperationDTO stockOperationToDto(StockOperation operation) {
        StockOperationDTO dto = new StockOperationDTO();
        dto.setId(operation.getId());
        dto.setUuid(operation.getUuid());
        dto.setCancelReason(operation.getCancelReason());
        setUser(dto::setCancelledBy, dto::setCancelledByGivenName, dto::setCancelledByFamilyName,
            operation.getCancelledBy());
        dto.setCancelledDate(operation.getCancelledDate());
        setUser(dto::setCompletedBy, dto::setCompletedByGivenName, dto::setCompletedByFamilyName,
            operation.getCompletedBy());
        dto.setCompletedDate(operation.getCompletedDate());
        setUser(dto::setSubmittedBy, dto::setSubmittedByGivenName, dto::setSubmittedByFamilyName,
            operation.getSubmittedBy());
        dto.setSubmittedDate(operation.getSubmittedDate());
        setUser(dto::setDispatchedBy, dto::setDispatchedByGivenName, dto::setDispatchedByFamilyName,
            operation.getDispatchedBy());
        dto.setDispatchedDate(operation.getDispatchedDate());
        setUser(dto::setReturnedBy, dto::setReturnedByGivenName, dto::setReturnedByFamilyName,
            operation.getReturnedBy());
        dto.setReturnedDate(operation.getReturnedDate());
        setUser(dto::setRejectedBy, dto::setRejectedByGivenName, dto::setRejectedByFamilyName,
            operation.getRejectedBy());
        dto.setRejectedDate(operation.getRejectedDate());
        setParty(dto, operation.getDestination(), true);
        setParty(dto, operation.getSource(), false);
        dto.setExternalReference(operation.getExternalReference());
        if (operation.getAtLocation() != null) {
            dto.setAtLocationUuid(operation.getAtLocation().getUuid());
            dto.setAtLocationName(operation.getAtLocation().getName());
        }
        dto.setOperationDate(operation.getOperationDate());
        dto.setLocked(operation.getLocked());
        dto.setOperationNumber(operation.getOperationNumber());
        dto.setOperationOrder(operation.getOperationOrder());
        dto.setRemarks(operation.getRemarks());
        dto.setStatus(operation.getStatus());
        dto.setReturnReason(operation.getReturnReason());
        dto.setRejectionReason(operation.getRejectionReason());
        if (operation.getStockOperationType() != null) {
            dto.setOperationTypeUuid(operation.getStockOperationType().getUuid());
            dto.setOperationType(operation.getStockOperationType().getOperationType());
            dto.setOperationTypeName(operation.getStockOperationType().getName());
        }
        if (operation.getResponsiblePerson() != null) {
            dto.setResponsiblePerson(operation.getResponsiblePerson().getId());
            dto.setResponsiblePersonUuid(operation.getResponsiblePerson().getUuid());
            dto.setResponsiblePersonGivenName(operation.getResponsiblePerson().getGivenName());
            dto.setResponsiblePersonFamilyName(operation.getResponsiblePerson().getFamilyName());
        }
        dto.setResponsiblePersonOther(operation.getResponsiblePersonOther());
        setUser(dto::setCreator, dto::setCreatorGivenName, dto::setCreatorFamilyName, operation.getCreator());
        dto.setDateCreated(operation.getDateCreated());
        if (operation.getReason() != null) {
            dto.setReasonId(operation.getReason().getConceptId());
            dto.setReasonUuid(operation.getReason().getUuid());
            dto.setReasonName(conceptName(operation.getReason()));
        }
        dto.setApprovalRequired(operation.getApprovalRequired());
        dto.setVoided(Boolean.TRUE.equals(operation.getVoided()));
        return dto;
    }

    private void setParty(StockOperationDTO dto, Party party, boolean destination) {
        if (party == null) {
            return;
        }
        if (destination) {
            dto.setDestinationUuid(party.getUuid());
            dto.setDestinationName(partyName(party));
        } else {
            dto.setSourceUuid(party.getUuid());
            dto.setSourceName(partyName(party));
        }
    }

    private String partyName(Party party) {
        if (party.getLocation() != null) {
            return party.getLocation().getName();
        }
        return party.getStockSource() == null ? null : party.getStockSource().getName();
    }

    private void setUser(java.util.function.Consumer<Integer> idSetter,
            java.util.function.Consumer<String> givenNameSetter, java.util.function.Consumer<String> familyNameSetter,
            User user) {
        if (user == null) {
            return;
        }
        idSetter.accept(user.getId());
        givenNameSetter.accept(user.getGivenName());
        familyNameSetter.accept(user.getFamilyName());
    }

    private Result<StockItem> findStockItemEntities(StockItemSearchFilter filter) {
        StockItemSearchFilter safeFilter = filter == null ? new StockItemSearchFilter() : filter;
        List<StockItem> stockItems = queryStockItems(safeFilter);
        stockItems.sort(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)));
        return resultFromList(stockItems, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private Result<StockItemDTO> findStockItems(StockItemSearchFilter filter) {
        StockItemSearchFilter safeFilter = filter == null ? new StockItemSearchFilter() : filter;
        List<StockItemDTO> dtos = queryStockItems(safeFilter).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockItemToDto)
                .collect(Collectors.toList());
        return resultFromList(dtos, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private List<StockItem> queryStockItems(StockItemSearchFilter filter) {
        return query(StockItem.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockItem, Drug> drug = root.join("drug", JoinType.LEFT);
            Join<StockItem, Concept> concept = root.join("concept", JoinType.LEFT);
            Join<StockItem, Concept> category = root.join("category", JoinType.LEFT);
            if (StringUtils.isNotBlank(filter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), filter.getUuid()));
            }
            if (!filter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            if (filter.getDrugId() != null) {
                predicates.add(cb.equal(drug.get("drugId"), filter.getDrugId()));
            }
            if (filter.getConceptId() != null) {
                predicates.add(cb.equal(concept.get("conceptId"), filter.getConceptId()));
            }
            if (filter.getIsDrug() != null) {
                predicates.add(filter.getIsDrug() ? cb.isNotNull(drug.get("drugId")) : cb.isNull(drug.get("drugId")));
            }
            if (filter.getStockItemIds() != null && !filter.getStockItemIds().isEmpty()) {
                predicates.add(root.get("id").in(filter.getStockItemIds()));
            }
            List<Predicate> drugConceptPredicates = new ArrayList<>();
            if (filter.getDrugs() != null && !filter.getDrugs().isEmpty()) {
                drugConceptPredicates.add(root.get("drug").in(filter.getDrugs()));
            }
            if (filter.getConcepts() != null && !filter.getConcepts().isEmpty()) {
                drugConceptPredicates.add(root.get("concept").in(filter.getConcepts()));
            }
            if (!drugConceptPredicates.isEmpty()) {
                predicates.add(filter.getSearchEitherDrugsOrConcepts()
                        ? cb.or(drugConceptPredicates.toArray(new Predicate[0]))
                        : cb.and(drugConceptPredicates.toArray(new Predicate[0])));
            }
            List<Integer> categoryIds = new ArrayList<>();
            if (filter.getCategoryId() != null) {
                categoryIds.add(filter.getCategoryId());
            }
            if (filter.getCategories() != null) {
                categoryIds.addAll(filter.getCategories().stream().map(Concept::getConceptId).collect(Collectors.toList()));
            }
            if (!categoryIds.isEmpty()) {
                predicates.add(category.get("conceptId").in(categoryIds));
            }
            return predicates;
        });
    }

    private List<Integer> searchStockItemCommonName(String text, Boolean isDrugSearch, boolean includeAll, int maxItems) {
        if (StringUtils.isBlank(text) || maxItems <= 0) {
            return new ArrayList<>();
        }
        String term = "%" + text.trim().toLowerCase(Locale.ROOT).replace("%", "") + "%";
        List<StockItem> matches = query(StockItem.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("commonName").as(String.class)), term),
                cb.like(cb.lower(root.get("acronym").as(String.class)), term)));
            if (isDrugSearch != null) {
                predicates.add(cb.equal(root.get("isDrug"), isDrugSearch));
            }
            if (!includeAll) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        });
        return matches.stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .limit(maxItems)
                .map(StockItem::getId)
                .collect(Collectors.toList());
    }

    private ImportResult importStockItems(Path file, boolean hasHeader) {
        if (file == null) {
            ImportResult result = new ImportResult();
            result.setErrors(new ArrayList<>(List.of("Import file is required")));
            return result;
        }
        StockItemImportJob importJob = new StockItemImportJob(file, hasHeader);
        importJob.execute();
        return (ImportResult) importJob.getResult();
    }

    private Object saveStockItem(Object[] args) {
        if (args.length == 1 && args[0] instanceof StockItemDTO) {
            return saveStockItem((StockItemDTO) args[0]);
        }
        if (args.length == 1 && args[0] instanceof StockItem) {
            return saveOrUpdate(args[0]);
        }
        if (args.length == 2 && args[0] instanceof StockItem && args[1] instanceof StockItemPackagingUOM) {
            StockItem stockItem = (StockItem) args[0];
            StockItemPackagingUOM uom = (StockItemPackagingUOM) args[1];
            boolean isNewUom = uom.getId() == null;
            boolean isDispensingUom = stockItem.getDispensingUnitPackagingUoM() == uom;
            boolean isDefaultOperationUom = stockItem.getDefaultStockOperationsUoM() == uom;
            boolean isPurchasePriceUom = stockItem.getPurchasePriceUoM() == uom;
            boolean isReorderLevelUom = stockItem.getReorderLevelUOM() == uom;
            if (isNewUom) {
                clearTransientStockItemUomReferences(stockItem, uom);
            }
            stockItem = saveOrUpdate(stockItem);
            uom.setStockItem(stockItem);
            uom = saveOrUpdate(uom);
            restoreStockItemUomReferences(stockItem, uom, isDispensingUom, isDefaultOperationUom,
                isPurchasePriceUom, isReorderLevelUom);
            saveOrUpdate(stockItem);
            return stockItem;
        }
        throw new UnsupportedOperationException("Unsupported saveStockItem signature");
    }

    private void clearTransientStockItemUomReferences(StockItem stockItem, StockItemPackagingUOM uom) {
        if (stockItem.getDispensingUnitPackagingUoM() == uom) {
            stockItem.setDispensingUnitPackagingUoM(null);
        }
        if (stockItem.getDefaultStockOperationsUoM() == uom) {
            stockItem.setDefaultStockOperationsUoM(null);
        }
        if (stockItem.getPurchasePriceUoM() == uom) {
            stockItem.setPurchasePriceUoM(null);
        }
        if (stockItem.getReorderLevelUOM() == uom) {
            stockItem.setReorderLevelUOM(null);
        }
    }

    private void restoreStockItemUomReferences(StockItem stockItem, StockItemPackagingUOM uom, boolean dispensing,
            boolean defaultOperation, boolean purchasePrice, boolean reorderLevel) {
        if (dispensing) {
            stockItem.setDispensingUnitPackagingUoM(uom);
        }
        if (defaultOperation) {
            stockItem.setDefaultStockOperationsUoM(uom);
        }
        if (purchasePrice) {
            stockItem.setPurchasePriceUoM(uom);
        }
        if (reorderLevel) {
            stockItem.setReorderLevelUOM(uom);
        }
    }

    private StockItem saveStockItem(StockItemDTO dto) {
        StockItem stockItem;
        boolean isNew = StringUtils.isBlank(dto.getUuid());
        if (isNew) {
            stockItem = new StockItem();
            stockItem.setCreator(authenticatedUser());
            stockItem.setDateCreated(new Date());
            if (StringUtils.isNotBlank(dto.getDrugUuid())) {
                Drug drug = byUuid(Drug.class, dto.getDrugUuid());
                if (drug == null) {
                    throw new StockManagementException("Stock item drug does not exist");
                }
                ensureNoExistingStockItemForDrug(drug);
                stockItem.setDrug(drug);
                stockItem.setConcept(drug.getConcept());
                stockItem.setIsDrug(true);
            } else if (StringUtils.isNotBlank(dto.getConceptUuid())) {
                Concept concept = byUuid(Concept.class, dto.getConceptUuid());
                if (concept == null) {
                    throw new StockManagementException("Stock item concept does not exist");
                }
                ensureNoExistingStockItemForConcept(concept);
                stockItem.setConcept(concept);
                stockItem.setIsDrug(false);
            } else {
                throw new StockManagementException("Stock item requires a drug or concept");
            }
        } else {
            stockItem = byUuid(StockItem.class, dto.getUuid());
            if (stockItem == null) {
                throw new StockManagementException("Stock item not found");
            }
            stockItem.setChangedBy(authenticatedUser());
            stockItem.setDateChanged(new Date());
            if (stockItem.getIsDrug() == null) {
                stockItem.setIsDrug(stockItem.getDrug() != null);
            }
        }
        stockItem.setCommonName(dto.getCommonName());
        stockItem.setAcronym(dto.getAcronym());
        stockItem.setHasExpiration(Boolean.TRUE.equals(dto.getHasExpiration()));
        stockItem.setExpiryNotice(dto.getExpiryNotice());
        stockItem.setPreferredVendor(StringUtils.isBlank(dto.getPreferredVendorUuid()) ? null
                : requireByUuid(StockSource.class, dto.getPreferredVendorUuid(), "Stock source not found"));
        stockItem.setDispensingUnit(StringUtils.isBlank(dto.getDispensingUnitUuid()) ? null
                : requireByUuid(Concept.class, dto.getDispensingUnitUuid(), "Dispensing unit concept not found"));
        stockItem.setCategory(StringUtils.isBlank(dto.getCategoryUuid()) ? null
                : requireByUuid(Concept.class, dto.getCategoryUuid(), "Category concept not found"));
        if (!isNew) {
            stockItem.setPurchasePrice(dto.getPurchasePrice());
            stockItem.setPurchasePriceUoM(StringUtils.isBlank(dto.getPurchasePriceUoMUuid()) ? null
                    : requireRelatedUom(dto.getPurchasePriceUoMUuid(), stockItem));
            stockItem.setReorderLevel(dto.getReorderLevel());
            stockItem.setReorderLevelUOM(StringUtils.isBlank(dto.getReorderLevelUoMUuid()) ? null
                    : requireRelatedUom(dto.getReorderLevelUoMUuid(), stockItem));
            stockItem.setDispensingUnitPackagingUoM(StringUtils.isBlank(dto.getDispensingUnitPackagingUoMUuid()) ? null
                    : requireRelatedUom(dto.getDispensingUnitPackagingUoMUuid(), stockItem));
            stockItem.setDefaultStockOperationsUoM(StringUtils.isBlank(dto.getDefaultStockOperationsUoMUuid()) ? null
                    : requireRelatedUom(dto.getDefaultStockOperationsUoMUuid(), stockItem));
        }
        return saveOrUpdate(stockItem);
    }

    private void ensureNoExistingStockItemForDrug(Drug drug) {
        if (!stockItemsByDrug(drug.getDrugId()).stream().filter(item -> !Boolean.TRUE.equals(item.getVoided())).toList()
                .isEmpty()) {
            throw new StockManagementException("Stock item for drug already exists");
        }
    }

    private void ensureNoExistingStockItemForConcept(Concept concept) {
        if (!stockItemsByConcept(concept.getConceptId()).stream()
                .filter(item -> !Boolean.TRUE.equals(item.getVoided())).toList().isEmpty()) {
            throw new StockManagementException("Stock item for concept already exists");
        }
    }

    private StockItemPackagingUOM requireRelatedUom(String uuid, StockItem stockItem) {
        StockItemPackagingUOM uom = requireByUuid(StockItemPackagingUOM.class, uuid, "Stock item UOM not found");
        if (uom.getStockItem() == null || !Objects.equals(uom.getStockItem().getId(), stockItem.getId())) {
            throw new StockManagementException("Stock item UOM is not related to the stock item");
        }
        return uom;
    }

    private StockRuleDTO saveStockRule(StockRuleDTO dto) {
        if (dto == null) {
            throw new StockManagementException("Stock rule payload is required");
        }
        validateStockRule(dto);

        boolean isNew = StringUtils.isBlank(dto.getUuid());
        StockRule stockRule;
        if (isNew) {
            stockRule = new StockRule();
            stockRule.setCreator(authenticatedUser());
            stockRule.setDateCreated(new Date());
        } else {
            stockRule = requireByUuid(StockRule.class, dto.getUuid(), "Stock rule not found");
            stockRule.setChangedBy(authenticatedUser());
            stockRule.setDateChanged(new Date());
        }

        StockItem stockItem = requireByUuid(StockItem.class, dto.getStockItemUuid(), "Stock item not found");
        StockItemPackagingUOM uom = requireRelatedUom(dto.getStockItemPackagingUOMUuid(), stockItem);
        Location location = Context.getLocationService().getLocationByUuid(dto.getLocationUuid());
        if (location == null) {
            throw new StockManagementException("Stock rule location not found");
        }
        if (!isNew && !sameUuid(dto.getStockItemUuid(), stockRule.getStockItem())) {
            throw new StockManagementException("Stock rule stock item cannot be changed");
        }
        if (!isNew && !sameUuid(dto.getLocationUuid(), stockRule.getLocation())) {
            throw new StockManagementException("Stock rule location cannot be changed");
        }

        stockRule.setStockItem(stockItem);
        stockRule.setLocation(location);
        stockRule.setName(dto.getName());
        stockRule.setDescription(dto.getDescription());
        stockRule.setQuantity(dto.getQuantity());
        stockRule.setStockItemPackagingUOM(uom);
        stockRule.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        if (!isNew && stockRule.getLastEvaluation() != null && dto.getEvaluationFrequency() != null
                && !Objects.equals(dto.getEvaluationFrequency(), stockRule.getEvaluationFrequency())) {
            stockRule.setNextEvaluation(addMinutes(stockRule.getLastEvaluation(), boundedMinutes(dto.getEvaluationFrequency())));
        }
        stockRule.setEvaluationFrequency(dto.getEvaluationFrequency());
        stockRule.setActionFrequency(dto.getActionFrequency());
        stockRule.setAlertRole(StringUtils.isBlank(dto.getAlertRole()) ? null : dto.getAlertRole());
        stockRule.setMailRole(StringUtils.isBlank(dto.getMailRole()) ? null : dto.getMailRole());
        stockRule.setEnableDescendants(Boolean.TRUE.equals(dto.getEnableDescendants()));

        return stockRuleToDto(saveOrUpdate(stockRule));
    }

    private void validateStockRule(StockRuleDTO dto) {
        if (StringUtils.isBlank(dto.getStockItemUuid())) {
            throw new StockManagementException("Stock rule stock item is required");
        }
        if (StringUtils.isBlank(dto.getName())) {
            throw new StockManagementException("Stock rule name is required");
        }
        if (dto.getName().length() > 255) {
            throw new StockManagementException("Stock rule name cannot exceed 255 characters");
        }
        if (StringUtils.isNotBlank(dto.getDescription()) && dto.getDescription().length() > 500) {
            throw new StockManagementException("Stock rule description cannot exceed 500 characters");
        }
        if (StringUtils.isBlank(dto.getLocationUuid())) {
            throw new StockManagementException("Stock rule location is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity().signum() <= 0) {
            throw new StockManagementException("Stock rule quantity must be greater than zero");
        }
        if (dto.getEnabled() == null) {
            throw new StockManagementException("Stock rule enabled flag is required");
        }
        if (dto.getEvaluationFrequency() == null || dto.getEvaluationFrequency() <= 0) {
            throw new StockManagementException("Stock rule evaluation frequency must be greater than zero");
        }
        if (dto.getActionFrequency() == null || dto.getActionFrequency() <= 0) {
            throw new StockManagementException("Stock rule action frequency must be greater than zero");
        }
        if (StringUtils.isBlank(dto.getStockItemPackagingUOMUuid())) {
            throw new StockManagementException("Stock rule packaging unit is required");
        }
        requireRole(dto.getAlertRole(), "Stock rule alert role not found");
        requireRole(dto.getMailRole(), "Stock rule mail role not found");
    }

    private void requireRole(String roleName, String message) {
        if (StringUtils.isBlank(roleName)) {
            return;
        }
        if (Context.getUserService().getRole(roleName) == null) {
            throw new StockManagementException(message);
        }
    }

    private Result<StockRuleDTO> findStockRules(
            StockRuleSearchFilter filter, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        if (recordPrivilegeFilters != null && recordPrivilegeFilters.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }
        StockRuleSearchFilter safeFilter = filter == null ? new StockRuleSearchFilter() : filter;
        Set<Integer> allowedLocationIds = recordPrivilegeFilters == null ? null
                : recordPrivilegeFilters.stream()
                        .map(RecordPrivilegeFilter::getLocationId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (allowedLocationIds != null && allowedLocationIds.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }
        List<StockRuleDTO> dtos = query(StockRule.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockRule, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            Join<StockRule, Location> location = root.join("location", JoinType.INNER);
            if (safeFilter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), safeFilter.getId()));
            }
            if (safeFilter.getUuids() != null && !safeFilter.getUuids().isEmpty()) {
                predicates.add(root.get("uuid").in(safeFilter.getUuids()));
            }
            if (safeFilter.getStockItemUuids() != null && !safeFilter.getStockItemUuids().isEmpty()) {
                predicates.add(stockItem.get("uuid").in(safeFilter.getStockItemUuids()));
            }
            if (safeFilter.getLocationUuids() != null && !safeFilter.getLocationUuids().isEmpty()) {
                predicates.add(location.get("uuid").in(safeFilter.getLocationUuids()));
            }
            if (allowedLocationIds != null) {
                predicates.add(location.get("locationId").in(allowedLocationIds));
            }
            addNullableDateRangePredicate(cb, predicates, root, "lastEvaluation",
                safeFilter.getLastEvaluationMin(), safeFilter.getLastEvaluationMax());
            addNullableDateRangePredicate(cb, predicates, root, "nextEvaluation",
                safeFilter.getNextEvaluationMin(), safeFilter.getNextEvaluationMax());
            addNullableDateRangePredicate(cb, predicates, root, "lastActionDate",
                safeFilter.getLastActionDateMin(), safeFilter.getLastActionDateMax());
            if (safeFilter.getHasNotificationRoleSet() != null) {
                Predicate alertSet = cb.and(cb.isNotNull(root.get("alertRole")),
                    cb.notEqual(root.get("alertRole"), ""));
                Predicate mailSet = cb.and(cb.isNotNull(root.get("mailRole")),
                    cb.notEqual(root.get("mailRole"), ""));
                predicates.add(safeFilter.getHasNotificationRoleSet()
                        ? cb.or(alertSet, mailSet)
                        : cb.and(cb.not(alertSet), cb.not(mailSet)));
            }
            if (safeFilter.getEnabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), safeFilter.getEnabled()));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)).reversed())
                .map(this::stockRuleToDto)
                .collect(Collectors.toList());
        return resultFromList(dtos, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private void addNullableDateRangePredicate(CriteriaBuilder cb, List<Predicate> predicates, Root<StockRule> root,
            String field, Date min, Date max) {
        if (min != null) {
            predicates.add(cb.or(cb.greaterThanOrEqualTo(root.<Date>get(field), min), cb.isNull(root.get(field))));
        }
        if (max != null) {
            predicates.add(cb.or(cb.lessThanOrEqualTo(root.<Date>get(field), max), cb.isNull(root.get(field))));
        }
    }

    private StockRuleDTO stockRuleToDto(StockRule stockRule) {
        StockRuleDTO dto = new StockRuleDTO();
        dto.setId(stockRule.getId());
        dto.setUuid(stockRule.getUuid());
        dto.setName(stockRule.getName());
        dto.setDescription(stockRule.getDescription());
        dto.setQuantity(stockRule.getQuantity());
        dto.setEnabled(stockRule.getEnabled());
        dto.setEvaluationFrequency(stockRule.getEvaluationFrequency());
        dto.setLastEvaluation(stockRule.getLastEvaluation());
        dto.setNextEvaluation(stockRule.getNextEvaluation());
        dto.setActionFrequency(stockRule.getActionFrequency());
        dto.setLastActionDate(stockRule.getLastActionDate());
        dto.setNextActionDate(stockRule.getNextActionDate());
        dto.setAlertRole(stockRule.getAlertRole());
        dto.setMailRole(stockRule.getMailRole());
        dto.setEnableDescendants(stockRule.getEnableDescendants());
        dto.setVoided(Boolean.TRUE.equals(stockRule.getVoided()));
        dto.setDateCreated(stockRule.getDateCreated());
        if (stockRule.getCreator() != null) {
            dto.setCreator(stockRule.getCreator().getId());
            dto.setCreatorGivenName(stockRule.getCreator().getGivenName());
            dto.setCreatorFamilyName(stockRule.getCreator().getFamilyName());
        }
        StockItem stockItem = stockRule.getStockItem();
        if (stockItem != null) {
            dto.setStockItemId(stockItem.getId());
            dto.setStockItemUuid(stockItem.getUuid());
        }
        Location location = stockRule.getLocation();
        if (location != null) {
            dto.setLocationId(location.getLocationId());
            dto.setLocationUuid(location.getUuid());
            dto.setLocationName(location.getName());
        }
        StockItemPackagingUOM uom = stockRule.getStockItemPackagingUOM();
        if (uom != null) {
            dto.setStockItemPackagingUOMId(uom.getId());
            dto.setStockItemPackagingUOMUuid(uom.getUuid());
            if (uom.getPackagingUom() != null) {
                dto.setPackagingUoMId(uom.getPackagingUom().getConceptId());
                dto.setPackagingUomName(conceptName(uom.getPackagingUom()));
            }
        }
        return dto;
    }

    private Result<StockSource> findStockSources(StockSourceSearchFilter filter) {
        StockSourceSearchFilter safeFilter = filter == null ? new StockSourceSearchFilter() : filter;
        List<StockSource> sources = query(StockSource.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.isNotBlank(safeFilter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), safeFilter.getUuid()));
            }
            if (safeFilter.getSourceType() != null) {
                predicates.add(cb.equal(root.get("sourceType"), safeFilter.getSourceType()));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            if (StringUtils.isNotBlank(safeFilter.getTextSearch())) {
                String term = safeFilter.getTextSearch().replace("%", "").toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name").as(String.class)), term),
                    cb.like(cb.lower(root.get("acronym").as(String.class)), term)));
            }
            return predicates;
        });
        sources.sort(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)));
        return resultFromList(sources, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private Result<PartyDTO> findParty(PartySearchFilter filter) {
        PartySearchFilter safeFilter = filter == null ? new PartySearchFilter() : filter;
        List<Party> parties = query(Party.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Party, Location> location = root.join("location", JoinType.LEFT);
            Join<Party, StockSource> stockSource = root.join("stockSource", JoinType.LEFT);
            if (safeFilter.getPartyIds() != null && !safeFilter.getPartyIds().isEmpty()) {
                predicates.add(root.get("id").in(safeFilter.getPartyIds()));
            }
            if (safeFilter.getPartyUuids() != null && !safeFilter.getPartyUuids().isEmpty()) {
                predicates.add(root.get("uuid").in(safeFilter.getPartyUuids()));
            }
            if (safeFilter.getLocationIds() != null && !safeFilter.getLocationIds().isEmpty()) {
                predicates.add(location.get("locationId").in(safeFilter.getLocationIds()));
            }
            if (safeFilter.getLocationUuids() != null && !safeFilter.getLocationUuids().isEmpty()) {
                predicates.add(location.get("uuid").in(safeFilter.getLocationUuids()));
            }
            if (StringUtils.isNotBlank(safeFilter.getSearchText())) {
                String term = "%" + safeFilter.getSearchText().replace("%", "").toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(location.get("name").as(String.class)), term),
                    cb.like(cb.lower(stockSource.get("name").as(String.class)), term)));
            }
            if (safeFilter.getIncludeVoided() == null || !safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        });
        List<PartyDTO> dtos = parties.stream()
                .map(this::partyToDto)
                .sorted(Comparator.comparing(PartyDTO::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
        return resultFromList(dtos, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private List<Party> findParty(Boolean hasLocation, Boolean hasStockSource) {
        return query(Party.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasLocation != null) {
                predicates.add(hasLocation ? cb.isNotNull(root.get("location")) : cb.isNull(root.get("location")));
            }
            if (hasStockSource != null) {
                predicates.add(hasStockSource ? cb.isNotNull(root.get("stockSource")) : cb.isNull(root.get("stockSource")));
            }
            return predicates;
        });
    }

    private List<PartyDTO> getAllParties() {
        return sortedById(listAll(Party.class)).stream().map(this::partyToDto).collect(Collectors.toList());
    }

    private PartyDTO partyToDto(Party party) {
        PartyDTO dto = new PartyDTO();
        dto.setId(party.getId());
        dto.setUuid(party.getUuid());
        dto.setVoided(Boolean.TRUE.equals(party.getVoided()));
        if (party.getLocation() != null) {
            dto.setName(party.getLocation().getName());
            dto.setLocationUuid(party.getLocation().getUuid());
            dto.setLocationId(party.getLocation().getId());
            dto.setTags(party.getLocation().getTags().stream().map(LocationTag::getName).collect(Collectors.toList()));
        }
        if (party.getStockSource() != null) {
            dto.setName(party.getStockSource().getName());
            dto.setAcronym(party.getStockSource().getAcronym());
            dto.setStockSourceUuid(party.getStockSource().getUuid());
            dto.setStockSourceId(party.getStockSource().getId());
        }
        return dto;
    }

    private SessionInfo getCurrentUserSessionInfo() {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setPrivileges(getPrivilegeScopes(authenticatedUser(), null));
        return sessionInfo;
    }

    private boolean userHasStockManagementPrivilege(User user, String privilege) {
        return user != null && (StringUtils.isBlank(privilege) || user.hasPrivilege(privilege));
    }

    private HashSet<PrivilegeScope> getPrivilegeScopes(User user, Object privileges) {
        HashSet<PrivilegeScope> scopes = new HashSet<>();
        if (user == null) {
            return scopes;
        }
        List<String> privilegeNames = new ArrayList<>();
        if (privileges instanceof String && StringUtils.isNotBlank((String) privileges)) {
            privilegeNames.add((String) privileges);
        } else if (privileges instanceof List) {
            privilegeNames.addAll((List<String>) privileges);
        }
        if (privilegeNames.isEmpty()) {
            privilegeNames = user.getAllRoles().stream()
                    .flatMap(role -> role.getPrivileges().stream())
                    .map(privilege -> privilege.getPrivilege())
                    .distinct()
                    .collect(Collectors.toList());
        }
        for (String privilegeName : privilegeNames) {
            if (user.hasPrivilege(privilegeName)) {
                PrivilegeScope scope = new PrivilegeScope();
                scope.setPrivilege(privilegeName);
                scopes.add(scope);
            }
        }
        return scopes;
    }

    private Result<StockItemPackagingUOMDTO> findStockItemPackagingUOMs(StockItemPackagingUOMSearchFilter filter) {
        StockItemPackagingUOMSearchFilter safeFilter = filter == null ? new StockItemPackagingUOMSearchFilter() : filter;
        List<StockItemPackagingUOMDTO> dtos = query(StockItemPackagingUOM.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockItemPackagingUOM, StockItem> stockItem = root.join("stockItem", JoinType.LEFT);
            if (StringUtils.isNotBlank(safeFilter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), safeFilter.getUuid()));
            }
            if (safeFilter.getStockItemIds() != null && !safeFilter.getStockItemIds().isEmpty()) {
                predicates.add(stockItem.get("id").in(safeFilter.getStockItemIds()));
            }
            if (safeFilter.getStockItemUuids() != null && !safeFilter.getStockItemUuids().isEmpty()) {
                predicates.add(stockItem.get("uuid").in(safeFilter.getStockItemUuids()));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(uom -> stockItemPackagingUomToDto(uom, safeFilter.includingDispensingUnit()))
                .collect(Collectors.toList());
        return resultFromList(dtos, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private Object saveStockItemPackagingUOM(Object arg) {
        if (arg instanceof StockItemPackagingUOM) {
            return saveOrUpdate(arg);
        }
        StockItemPackagingUOMDTO dto = (StockItemPackagingUOMDTO) arg;
        StockItemPackagingUOM uom;
        if (StringUtils.isNotBlank(dto.getUuid())) {
            uom = requireByUuid(StockItemPackagingUOM.class, dto.getUuid(), "Stock item UOM not found");
            uom.setChangedBy(authenticatedUser());
            uom.setDateChanged(new Date());
        } else {
            uom = new StockItemPackagingUOM();
            uom.setCreator(authenticatedUser());
            uom.setDateCreated(new Date());
            uom.setStockItem(requireByUuid(StockItem.class, dto.getStockItemUuid(), "Stock item not found"));
        }
        uom.setPackagingUom(requireByUuid(Concept.class, dto.getPackagingUomUuid(), "Packaging UOM concept not found"));
        uom.setFactor(dto.getFactor());
        return saveOrUpdate(uom);
    }

    private StockItemPackagingUOMDTO stockItemPackagingUomToDto(StockItemPackagingUOM uom, boolean includeDispensingUnit) {
        StockItemPackagingUOMDTO dto = new StockItemPackagingUOMDTO();
        dto.setId(uom.getId());
        dto.setUuid(uom.getUuid());
        dto.setVoided(Boolean.TRUE.equals(uom.getVoided()));
        dto.setFactor(uom.getFactor());
        if (uom.getPackagingUom() != null) {
            dto.setPackagingUomId(uom.getPackagingUom().getConceptId());
            dto.setPackagingUomUuid(uom.getPackagingUom().getUuid());
            dto.setPackagingUomName(conceptName(uom.getPackagingUom()));
        }
        StockItem stockItem = uom.getStockItem();
        if (stockItem != null) {
            dto.setStockItemId(stockItem.getId());
            dto.setStockItemUuid(stockItem.getUuid());
            dto.setIsDispensingUnit(stockItem.getDispensingUnitPackagingUoM() != null
                    && Objects.equals(stockItem.getDispensingUnitPackagingUoM().getId(), uom.getId()));
            dto.setIsDefaultStockOperationsUoM(stockItem.getDefaultStockOperationsUoM() != null
                    && Objects.equals(stockItem.getDefaultStockOperationsUoM().getId(), uom.getId()));
            if (includeDispensingUnit && stockItem.getDispensingUnit() != null) {
                dto.setStockItemDispensingUnitId(stockItem.getDispensingUnit().getConceptId());
                dto.setStockItemDispensingUnitName(conceptName(stockItem.getDispensingUnit()));
            }
        }
        return dto;
    }

    private Result<StockBatchDTO> findStockBatches(StockBatchSearchFilter filter) {
        StockBatchSearchFilter safeFilter = filter == null ? new StockBatchSearchFilter() : filter;
        List<StockBatchDTO> batches = query(StockBatch.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockBatch, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            if (safeFilter.getStockBatchIds() != null && !safeFilter.getStockBatchIds().isEmpty()) {
                predicates.add(root.get("id").in(safeFilter.getStockBatchIds()));
            }
            if (StringUtils.isNotBlank(safeFilter.getStockBatchUuid())) {
                predicates.add(cb.equal(root.get("uuid"), safeFilter.getStockBatchUuid()));
            }
            if (safeFilter.getStockItemId() != null) {
                predicates.add(cb.equal(stockItem.get("id"), safeFilter.getStockItemId()));
            }
            if (StringUtils.isNotBlank(safeFilter.getStockItemUuid())) {
                predicates.add(cb.equal(stockItem.get("uuid"), safeFilter.getStockItemUuid()));
            }
            if (Boolean.TRUE.equals(safeFilter.getExcludeExpired())) {
                predicates.add(cb.or(cb.isNull(root.get("expiration")),
                    cb.greaterThan(root.<Date>get("expiration"), DateUtil.today())));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockBatchToDto)
                .collect(Collectors.toList());

        if (StringUtils.isNotBlank(safeFilter.getLocationUuid()) && !batches.isEmpty()) {
            batches = filterStockBatchesByLocation(batches, safeFilter);
        }

        return resultFromList(batches, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private List<StockBatchDTO> filterStockBatchesByLocation(
            List<StockBatchDTO> batches, StockBatchSearchFilter filter) {
        Location location = Context.getLocationService().getLocationByUuid(filter.getLocationUuid());
        if (location == null) {
            return new ArrayList<>();
        }
        Party party = firstByEntity(Party.class, "location", location);
        if (party == null) {
            return new ArrayList<>();
        }
        Map<Integer, BigDecimal> balances = stockBatchBalancesForParty(
            batches.stream().map(StockBatchDTO::getId).collect(Collectors.toList()), party);
        return batches.stream()
                .filter(batch -> balances.containsKey(batch.getId()))
                .filter(batch -> !Boolean.TRUE.equals(filter.getExcludeEmptyStock())
                        || balances.get(batch.getId()).compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }

    private Map<Integer, BigDecimal> stockBatchBalancesForParty(Collection<Integer> batchIds, Party party) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Map.of();
        }
        return query(StockItemTransaction.class, (cb, root) -> predicates(
            cb.equal(root.get("party"), party),
            root.get("stockBatch").get("id").in(batchIds))).stream()
                .filter(transaction -> transaction.getStockBatch() != null)
                .collect(Collectors.groupingBy(
                    transaction -> transaction.getStockBatch().getId(),
                    Collectors.reducing(BigDecimal.ZERO, this::stockItemTransactionQuantity, BigDecimal::add)));
    }

    private BigDecimal stockItemTransactionQuantity(StockItemTransaction transaction) {
        BigDecimal quantity = transaction.getQuantity() == null ? BigDecimal.ZERO : transaction.getQuantity();
        StockItemPackagingUOM uom = transaction.getStockItemPackagingUOM();
        BigDecimal factor = uom == null || uom.getFactor() == null ? BigDecimal.ONE : uom.getFactor();
        return quantity.multiply(factor);
    }

    private StockBatchDTO stockBatchToDto(StockBatch stockBatch) {
        StockBatchDTO dto = new StockBatchDTO();
        dto.setId(stockBatch.getId());
        dto.setUuid(stockBatch.getUuid());
        dto.setBatchNo(stockBatch.getBatchNo());
        dto.setExpiration(stockBatch.getExpiration());
        dto.setExpiryNotificationDate(stockBatch.getExpiryNotificationDate());
        dto.setVoided(Boolean.TRUE.equals(stockBatch.getVoided()));
        if (stockBatch.getStockItem() != null) {
            dto.setStockItemUuid(stockBatch.getStockItem().getUuid());
        }
        return dto;
    }

    private List<StockBatchDTO> getExpiringStockBatchesDueForNotification(
            Integer defaultExpiryNotificationNoticePeriod) {
        Date today = DateUtil.today();
        return query(StockBatch.class, (cb, root) -> {
            Join<StockBatch, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            return predicates(
                cb.isNotNull(root.get("expiration")),
                cb.greaterThanOrEqualTo(root.<Date>get("expiration"), today),
                cb.isNull(root.get("expiryNotificationDate")),
                cb.isFalse(root.get("voided")),
                cb.isFalse(stockItem.get("voided")));
        }).stream()
                .filter(batch -> expiresWithinNoticePeriod(batch, today, defaultExpiryNotificationNoticePeriod))
                .sorted(Comparator.comparing(StockBatch::getExpiration, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockBatchToDto)
                .collect(Collectors.toList());
    }

    private boolean expiresWithinNoticePeriod(StockBatch batch, Date today, Integer defaultNoticePeriod) {
        StockItem stockItem = batch.getStockItem();
        Integer noticePeriod = stockItem == null ? null : stockItem.getExpiryNotice();
        if (noticePeriod == null) {
            noticePeriod = defaultNoticePeriod;
        }
        return noticePeriod != null && !batch.getExpiration().after(addDays(today, noticePeriod));
    }

    private Result<StockOperationLineItem> findStockOperationLineItems(StockOperationLineItemFilter filter) {
        StockOperationLineItemFilter safeFilter = filter == null ? new StockOperationLineItemFilter() : filter;
        Set<Integer> atLocationIds = reportLocationIds(safeFilter.getAtLocationId(), safeFilter.getChildLocations());
        Set<Integer> sourcePartyIds = reportPartyIds(safeFilter.getSourcePartyId(),
            safeFilter.getSourcePartyChildLocations());
        Set<Integer> destinationPartyIds = reportPartyIds(safeFilter.getDestinationPartyId(),
            safeFilter.getDestinationPartyChildLocations());
        if ((safeFilter.getSourcePartyId() != null && sourcePartyIds.isEmpty())
                || (safeFilter.getDestinationPartyId() != null && destinationPartyIds.isEmpty())) {
            return new Result<>(new ArrayList<>(), 0);
        }
        Set<Integer> operationTypeIds = safeFilter.getStockOperationTypes() == null ? Set.of()
                : safeFilter.getStockOperationTypes().stream()
                        .map(StockOperationType::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        List<StockOperationLineItem> lineItems = query(StockOperationItem.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockOperationItem, StockOperation> operation = root.join("stockOperation", JoinType.INNER);
            Join<StockOperationItem, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            Join<StockOperation, Location> atLocation = operation.join("atLocation", JoinType.LEFT);
            Join<StockOperation, Party> source = operation.join("source", JoinType.LEFT);
            Join<StockOperation, Party> destination = operation.join("destination", JoinType.LEFT);
            if (safeFilter.getStockOperationIdMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(operation.get("id"), safeFilter.getStockOperationIdMin()));
            }
            if (safeFilter.getStockOperationItemIdMin() != null) {
                predicates.add(cb.greaterThan(root.get("id"), safeFilter.getStockOperationItemIdMin()));
            }
            addInPredicate(predicates, atLocation.get("locationId"), atLocationIds);
            addInPredicate(predicates, source.get("id"), sourcePartyIds);
            addInPredicate(predicates, destination.get("id"), destinationPartyIds);
            addInPredicate(predicates, operation.get("stockOperationType").get("id"), operationTypeIds);
            addInPredicate(predicates, operation.get("status"), safeFilter.getStockOperationStatuses());
            if (safeFilter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(operation.<Date>get("dateCreated"),
                    safeFilter.getStartDate()));
            }
            if (safeFilter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(operation.<Date>get("dateCreated"), safeFilter.getEndDate()));
            }
            if (safeFilter.getStockItemCategoryConceptId() != null) {
                predicates.add(cb.equal(stockItem.get("category").get("conceptId"),
                    safeFilter.getStockItemCategoryConceptId()));
            }
            predicates.add(cb.isFalse(operation.get("voided")));
            predicates.add(cb.isFalse(root.get("voided")));
            return predicates;
        }).stream()
                .sorted(Comparator
                        .comparing((StockOperationItem item) -> item.getStockOperation() == null
                                ? null
                                : item.getStockOperation().getId(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockOperationLineItem)
                .collect(Collectors.toList());
        return reportResultFromList(lineItems, safeFilter.getStartIndex(), safeFilter.getLimit(),
            safeFilter.getStockOperationItemIdMin() != null);
    }

    private StockOperationLineItem stockOperationLineItem(StockOperationItem item) {
        StockOperationLineItem lineItem = new StockOperationLineItem();
        lineItem.setStockOperationItemId(item.getId());
        lineItem.setQuantity(item.getQuantity());
        lineItem.setPurchasePrice(item.getPurchasePrice());
        lineItem.setQuantityRequested(item.getQuantityRequested());
        setStockOperationLineStockItem(lineItem, item.getStockItem());
        setStockOperationLineBatch(lineItem, item.getStockBatch());
        setStockOperationLineUom(lineItem, item.getStockItemPackagingUOM());
        setStockOperationLineRequestedUom(lineItem, item.getQuantityRequestedPackagingUOM());
        setStockOperationLineOperation(lineItem, item.getStockOperation());
        return lineItem;
    }

    private void setStockOperationLineStockItem(StockOperationLineItem lineItem, StockItem stockItem) {
        if (stockItem == null) {
            return;
        }
        lineItem.setStockItemId(stockItem.getId());
        lineItem.setCommonName(stockItem.getCommonName());
        lineItem.setAcronym(stockItem.getAcronym());
        if (stockItem.getDrug() != null) {
            lineItem.setStockItemDrugId(stockItem.getDrug().getDrugId());
            lineItem.setStockItemDrugName(stockItem.getDrug().getName());
        }
        if (stockItem.getConcept() != null) {
            lineItem.setStockItemConceptId(stockItem.getConcept().getConceptId());
            lineItem.setStockItemConceptName(conceptName(stockItem.getConcept()));
        }
        if (stockItem.getCategory() != null) {
            lineItem.setStockItemCategoryConceptId(stockItem.getCategory().getConceptId());
            lineItem.setStockItemCategoryName(conceptName(stockItem.getCategory()));
        }
    }

    private void setStockOperationLineBatch(StockOperationLineItem lineItem, StockBatch batch) {
        if (batch == null) {
            return;
        }
        lineItem.setBatchNo(batch.getBatchNo());
        lineItem.setExpiration(batch.getExpiration());
    }

    private void setStockOperationLineUom(StockOperationLineItem lineItem, StockItemPackagingUOM uom) {
        if (uom == null) {
            return;
        }
        lineItem.setStockItemPackagingUOMFactor(uom.getFactor());
        if (uom.getPackagingUom() != null) {
            lineItem.setPackagingUoMId(uom.getPackagingUom().getConceptId());
            lineItem.setStockItemPackagingUOMName(conceptName(uom.getPackagingUom()));
        }
    }

    private void setStockOperationLineRequestedUom(StockOperationLineItem lineItem, StockItemPackagingUOM uom) {
        if (uom == null) {
            return;
        }
        lineItem.setQuantityRequestedPackagingUOMFactor(uom.getFactor());
        if (uom.getPackagingUom() != null) {
            lineItem.setQuantityRequestedPackagingUoMId(uom.getPackagingUom().getConceptId());
            lineItem.setQuantityRequestedPackagingUOMName(conceptName(uom.getPackagingUom()));
        }
    }

    private void setStockOperationLineOperation(StockOperationLineItem lineItem, StockOperation operation) {
        if (operation == null) {
            return;
        }
        lineItem.setStockOperationId(operation.getId());
        lineItem.setOperationDate(operation.getOperationDate());
        lineItem.setOperationNumber(operation.getOperationNumber());
        lineItem.setCompletedDate(operation.getCompletedDate());
        lineItem.setStockOperationStatus(operation.getStatus());
        lineItem.setSourceName(operation.getSource() == null ? null : partyName(operation.getSource()));
        lineItem.setDestinationName(operation.getDestination() == null ? null : partyName(operation.getDestination()));
        lineItem.setResponsiblePersonOther(operation.getResponsiblePersonOther());
        lineItem.setRemarks(operation.getRemarks());
        lineItem.setDateCreated(operation.getDateCreated());
        if (operation.getStockOperationType() != null) {
            lineItem.setOperationTypeName(operation.getStockOperationType().getName());
        }
        if (operation.getReason() != null) {
            lineItem.setReasonId(operation.getReason().getConceptId());
            lineItem.setReasonName(conceptName(operation.getReason()));
        }
        setLineUser(operation.getCompletedBy(), lineItem::setCompletedBy,
            lineItem::setCompletedByGivenName, lineItem::setCompletedByFamilyName);
        setLineUser(operation.getResponsiblePerson(), lineItem::setResponsiblePerson,
            lineItem::setResponsiblePersonGivenName, lineItem::setResponsiblePersonFamilyName);
        setLineUser(operation.getCreator(), lineItem::setCreator,
            lineItem::setCreatorGivenName, lineItem::setCreatorFamilyName);
        if (operation.getParentStockOperationLinks() != null) {
            operation.getParentStockOperationLinks().stream()
                    .filter(link -> link.getParent() != null && !Boolean.TRUE.equals(link.getVoided()))
                    .findFirst()
                    .ifPresent(link -> lineItem.setRequisitionOperationNumber(
                        link.getParent().getOperationNumber()));
        }
    }

    private Result<DispensingLineItem> findDispensingLineItems(DispensingLineFilter filter) {
        DispensingLineFilter safeFilter = filter == null ? new DispensingLineFilter() : filter;
        Set<Integer> partyIds = dispensingPartyIds(safeFilter);
        if (safeFilter.getAtLocationId() != null && partyIds.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }
        List<DispensingLineItem> lineItems = query(StockItemTransaction.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockItemTransaction, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            Join<StockItemTransaction, Patient> patient = root.join("patient", JoinType.LEFT);
            Join<StockItemTransaction, Party> party = root.join("party", JoinType.LEFT);
            if (safeFilter.getStockItemTransactionMin() != null) {
                predicates.add(cb.greaterThan(root.get("id"), safeFilter.getStockItemTransactionMin()));
            }
            addInPredicate(predicates, party.get("id"), partyIds);
            if (safeFilter.getStockItemId() != null) {
                predicates.add(cb.equal(stockItem.get("id"), safeFilter.getStockItemId()));
            }
            if (safeFilter.getPatientId() != null) {
                predicates.add(cb.equal(patient.get("patientId"), safeFilter.getPatientId()));
            } else {
                predicates.add(cb.isNotNull(patient.get("patientId")));
            }
            if (safeFilter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Date>get("dateCreated"), safeFilter.getStartDate()));
            }
            if (safeFilter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("dateCreated"), safeFilter.getEndDate()));
            }
            if (safeFilter.getStockItemCategoryConceptId() != null) {
                predicates.add(cb.equal(stockItem.get("category").get("conceptId"),
                    safeFilter.getStockItemCategoryConceptId()));
            }
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::dispensingLineItem)
                .collect(Collectors.toList());
        return reportResultFromList(lineItems, safeFilter.getStartIndex(), safeFilter.getLimit(),
            safeFilter.getStockItemTransactionMin() != null);
    }

    private DispensingLineItem dispensingLineItem(StockItemTransaction transaction) {
        DispensingLineItem lineItem = new DispensingLineItem();
        lineItem.setStockItemTransactionId(transaction.getId());
        lineItem.setDateCreated(transaction.getDateCreated());
        lineItem.setQuantity(transaction.getQuantity());
        setLineUser(transaction.getCreator(), lineItem::setCreator,
            lineItem::setCreatorGivenName, lineItem::setCreatorFamilyName);
        Party party = transaction.getParty();
        if (party != null) {
            lineItem.setPartyId(party.getId());
            lineItem.setPartyName(partyName(party));
        }
        setDispensingLinePatient(lineItem, transaction.getPatient());
        setDispensingLineOrder(lineItem, transaction.getOrder());
        if (transaction.getEncounter() != null) {
            lineItem.setEncounterId(transaction.getEncounter().getEncounterId());
        }
        setDispensingLineStockItem(lineItem, transaction.getStockItem());
        setDispensingLineUom(lineItem, transaction.getStockItemPackagingUOM());
        if (transaction.getStockBatch() != null) {
            lineItem.setBatchNo(transaction.getStockBatch().getBatchNo());
            lineItem.setExpiration(transaction.getStockBatch().getExpiration());
        }
        return lineItem;
    }

    private void setDispensingLinePatient(DispensingLineItem lineItem, Patient patient) {
        if (patient == null) {
            return;
        }
        lineItem.setPatientId(patient.getPatientId());
        lineItem.setPatientGivenName(patient.getGivenName());
        lineItem.setPatientMiddleName(patient.getMiddleName());
        lineItem.setPatientFamilyName(patient.getFamilyName());
        if (patient.getPatientIdentifier() != null) {
            lineItem.setPatientIdentifier(patient.getPatientIdentifier().getIdentifier());
        }
    }

    private void setDispensingLineOrder(DispensingLineItem lineItem, Order order) {
        if (order == null) {
            return;
        }
        lineItem.setOrderId(order.getOrderId());
        lineItem.setOrderNumber(order.getOrderNumber());
        lineItem.setOrderDateCreated(order.getDateCreated());
        if (order.getEncounter() != null) {
            lineItem.setEncounterId(order.getEncounter().getEncounterId());
        }
        if (order instanceof DrugOrder drugOrder) {
            setDispensingLineDrugOrder(lineItem, drugOrder);
        }
    }

    private void setDispensingLineDrugOrder(DispensingLineItem lineItem, DrugOrder order) {
        lineItem.setDose(order.getDose());
        lineItem.setFrequencyPerDay(order.getFrequency() == null ? null : order.getFrequency().getFrequencyPerDay());
        lineItem.setAsNeeded(order.getAsNeeded());
        lineItem.setAsNeededCondition(order.getAsNeededCondition());
        lineItem.setNumRefills(order.getNumRefills());
        lineItem.setDuration(order.getDuration());
        lineItem.setDispenseAsWritten(order.getDispenseAsWritten());
        setConcept(lineItem::setDoseUnitsConceptId, lineItem::setDoseUnitsConceptName, order.getDoseUnits());
        setConcept(lineItem::setFrequencyConceptId, lineItem::setFrequencyConceptName,
            order.getFrequency() == null ? null : order.getFrequency().getConcept());
        setConcept(lineItem::setQuantityUnitsConceptId, lineItem::setQuantityUnitsConceptName,
            order.getQuantityUnits());
        setConcept(lineItem::setDurationUnitsConceptId, lineItem::setDurationUnitsConceptName,
            order.getDurationUnits());
        if (order.getRoute() != null) {
            lineItem.setRouteConceptId(order.getRoute().getConceptId());
        }
    }

    private void setDispensingLineStockItem(DispensingLineItem lineItem, StockItem stockItem) {
        if (stockItem == null) {
            return;
        }
        lineItem.setStockItemId(stockItem.getId());
        lineItem.setCommonName(stockItem.getCommonName());
        lineItem.setAcronym(stockItem.getAcronym());
        if (stockItem.getDrug() != null) {
            lineItem.setStockItemDrugId(stockItem.getDrug().getDrugId());
            lineItem.setStockItemDrugName(stockItem.getDrug().getName());
        }
        if (stockItem.getConcept() != null) {
            lineItem.setStockItemConceptId(stockItem.getConcept().getConceptId());
            lineItem.setStockItemConceptName(conceptName(stockItem.getConcept()));
        }
        if (stockItem.getCategory() != null) {
            lineItem.setStockItemCategoryConceptId(stockItem.getCategory().getConceptId());
            lineItem.setStockItemCategoryName(conceptName(stockItem.getCategory()));
        }
    }

    private void setDispensingLineUom(DispensingLineItem lineItem, StockItemPackagingUOM uom) {
        if (uom == null) {
            return;
        }
        lineItem.setStockItemPackagingUOMFactor(uom.getFactor());
        if (uom.getPackagingUom() != null) {
            lineItem.setPackagingUoMId(uom.getPackagingUom().getConceptId());
            lineItem.setStockItemPackagingUOMName(conceptName(uom.getPackagingUom()));
        }
    }

    private Result<PrescriptionLineItem> findPrescriptionLineItems(PrescriptionLineFilter filter) {
        PrescriptionLineFilter safeFilter = filter == null ? new PrescriptionLineFilter() : filter;
        Set<Integer> locationIds = reportLocationIds(safeFilter.getAtLocationId(), safeFilter.getChildLocations());
        List<DrugOrder> orders = query(DrugOrder.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<DrugOrder, Drug> drug = root.join("drug", JoinType.LEFT);
            Join<DrugOrder, Patient> patient = root.join("patient", JoinType.LEFT);
            Join<DrugOrder, Encounter> encounter = root.join("encounter", JoinType.LEFT);
            Join<Encounter, Location> location = encounter.join("location", JoinType.LEFT);
            if (safeFilter.getPrescriptionTransactionMin() != null) {
                predicates.add(cb.greaterThan(root.get("orderId"), safeFilter.getPrescriptionTransactionMin()));
            }
            addInPredicate(predicates, location.get("locationId"), locationIds);
            if (safeFilter.getDrugId() != null) {
                predicates.add(cb.equal(drug.get("drugId"), safeFilter.getDrugId()));
            }
            if (safeFilter.getPatientId() != null) {
                predicates.add(cb.equal(patient.get("patientId"), safeFilter.getPatientId()));
            }
            if (safeFilter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Date>get("dateCreated"), safeFilter.getStartDate()));
            }
            if (safeFilter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("dateCreated"), safeFilter.getEndDate()));
            }
            predicates.add(cb.greaterThan(root.<Double>get("quantity"), 0.0d));
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(Order::getOrderId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        Map<Integer, StockItem> stockItemsByDrugId = stockItemsByDrugId(orders.stream()
                .map(DrugOrder::getDrug)
                .filter(Objects::nonNull)
                .map(Drug::getDrugId)
                .collect(Collectors.toSet()));
        Map<Integer, List<StockItemTransaction>> transactionsByOrderId = stockTransactionsByOrderId(orders.stream()
                .map(Order::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<PrescriptionLineItem> lineItems = new ArrayList<>();
        boolean includeFulfillmentInfo = safeFilter.getFullfillments() != null
                && !safeFilter.getFullfillments().isEmpty();
        for (DrugOrder order : orders) {
            StockItem stockItem = order.getDrug() == null ? null : stockItemsByDrugId.get(order.getDrug().getDrugId());
            if (!prescriptionStockItemMatchesFilter(stockItem, safeFilter)) {
                continue;
            }
            List<StockItemTransaction> transactions = transactionsByOrderId.getOrDefault(order.getOrderId(),
                new ArrayList<>());
            if (!includeFulfillmentInfo) {
                lineItems.add(prescriptionLineItem(order, stockItem, null));
                continue;
            }
            BigDecimal orderedQuantity = orderedQuantityInBaseUnits(order, stockItem);
            BigDecimal dispensedQuantity = transactions.stream()
                    .map(this::absoluteTransactionQuantityInBaseUnits)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!matchesFulfillmentFilter(safeFilter.getFullfillments(), orderedQuantity, dispensedQuantity,
                transactions.isEmpty())) {
                continue;
            }
            if (transactions.isEmpty()) {
                lineItems.add(prescriptionLineItem(order, stockItem, null));
            } else {
                transactions.forEach(transaction -> lineItems.add(prescriptionLineItem(order, stockItem, transaction)));
            }
        }
        return reportResultFromList(lineItems, safeFilter.getStartIndex(), safeFilter.getLimit(),
            safeFilter.getPrescriptionTransactionMin() != null);
    }

    private PrescriptionLineItem prescriptionLineItem(DrugOrder order, StockItem stockItem,
            StockItemTransaction transaction) {
        PrescriptionLineItem lineItem = new PrescriptionLineItem();
        lineItem.setId(order.getOrderId());
        lineItem.setOrderId(order.getOrderId());
        lineItem.setPreviousOrderId(order.getPreviousOrder() == null ? null : order.getPreviousOrder().getOrderId());
        lineItem.setDateActivated(order.getDateActivated());
        lineItem.setDateStopped(order.getDateStopped());
        lineItem.setAction(order.getAction() == null ? null : order.getAction().name());
        lineItem.setUrgency(order.getUrgency() == null ? null : order.getUrgency().name());
        lineItem.setDateCreated(order.getDateCreated());
        lineItem.setOrderNumber(order.getOrderNumber());
        lineItem.setQuantity(order.getQuantity());
        lineItem.setDose(order.getDose());
        lineItem.setFrequencyPerDay(order.getFrequency() == null ? null : order.getFrequency().getFrequencyPerDay());
        lineItem.setAsNeeded(order.getAsNeeded());
        lineItem.setDosingInstructions(order.getDosingInstructions());
        lineItem.setAsNeededCondition(order.getAsNeededCondition());
        lineItem.setNumRefills(order.getNumRefills());
        lineItem.setDuration(order.getDuration());
        lineItem.setDispenseAsWritten(order.getDispenseAsWritten());
        setPrescriptionLinePatient(lineItem, order.getPatient());
        setPrescriptionLineOrderer(lineItem, order.getOrderer());
        setPrescriptionLineStockItem(lineItem, stockItem);
        setPrescriptionLineOrderUom(lineItem, order, stockItem);
        setPrescriptionLineLocation(lineItem, order.getEncounter());
        setConcept(lineItem::setDoseUnitsConceptId, lineItem::setDoseUnitsConceptName, order.getDoseUnits());
        setConcept(lineItem::setFrequencyConceptId, lineItem::setFrequencyConceptName,
            order.getFrequency() == null ? null : order.getFrequency().getConcept());
        setConcept(lineItem::setQuantityUnitsConceptId, lineItem::setQuantityUnitsConceptName,
            order.getQuantityUnits());
        setConcept(lineItem::setDurationUnitsConceptId, lineItem::setDurationUnitsConceptName,
            order.getDurationUnits());
        setConcept(lineItem::setRouteConceptId, lineItem::setRouteConceptName, order.getRoute());
        setPrescriptionFulfillment(lineItem, transaction);
        return lineItem;
    }

    private void setPrescriptionLinePatient(PrescriptionLineItem lineItem, Patient patient) {
        if (patient == null) {
            return;
        }
        lineItem.setPatientId(patient.getPatientId());
        lineItem.setPatientGivenName(patient.getGivenName());
        lineItem.setPatientMiddleName(patient.getMiddleName());
        lineItem.setPatientFamilyName(patient.getFamilyName());
        if (patient.getPatientIdentifier() != null) {
            lineItem.setPatientIdentifier(patient.getPatientIdentifier().getIdentifier());
        }
    }

    private void setPrescriptionLineOrderer(PrescriptionLineItem lineItem, Provider provider) {
        if (provider == null || provider.getPerson() == null) {
            return;
        }
        Person person = provider.getPerson();
        lineItem.setOrdererPersonId(person.getPersonId());
        lineItem.setOrdererGivenName(person.getGivenName());
        lineItem.setOrdererMiddleName(person.getMiddleName());
        lineItem.setOrdererFamilyName(person.getFamilyName());
    }

    private void setPrescriptionLineStockItem(PrescriptionLineItem lineItem, StockItem stockItem) {
        if (stockItem == null) {
            return;
        }
        lineItem.setStockItemId(stockItem.getId());
        lineItem.setCommonName(stockItem.getCommonName());
        lineItem.setAcronym(stockItem.getAcronym());
        if (stockItem.getDrug() != null) {
            lineItem.setStockItemDrugId(stockItem.getDrug().getDrugId());
            lineItem.setStockItemDrugName(stockItem.getDrug().getName());
        }
        if (stockItem.getConcept() != null) {
            lineItem.setStockItemConceptId(stockItem.getConcept().getConceptId());
            lineItem.setStockItemConceptName(conceptName(stockItem.getConcept()));
        }
        if (stockItem.getCategory() != null) {
            lineItem.setStockItemCategoryConceptId(stockItem.getCategory().getConceptId());
            lineItem.setStockItemCategoryName(conceptName(stockItem.getCategory()));
        }
    }

    private void setPrescriptionLineOrderUom(PrescriptionLineItem lineItem, DrugOrder order, StockItem stockItem) {
        Concept quantityUnits = order.getQuantityUnits();
        if (quantityUnits != null) {
            lineItem.setPackagingUoMId(quantityUnits.getConceptId());
            lineItem.setQuantityUnitsConceptId(quantityUnits.getConceptId());
            lineItem.setQuantityUnitsConceptName(conceptName(quantityUnits));
        }
        StockItemPackagingUOM uom = packagingUomForConcept(stockItem, quantityUnits);
        if (uom == null && stockItem != null) {
            uom = stockItem.getDispensingUnitPackagingUoM();
        }
        if (uom != null) {
            lineItem.setStockItemPackagingUOMFactor(uom.getFactor());
            if (uom.getPackagingUom() != null) {
                lineItem.setPackagingUoMId(uom.getPackagingUom().getConceptId());
                lineItem.setStockItemPackagingUOMName(conceptName(uom.getPackagingUom()));
            }
        } else {
            lineItem.setStockItemPackagingUOMFactor(BigDecimal.ONE);
        }
    }

    private void setPrescriptionLineLocation(PrescriptionLineItem lineItem, Encounter encounter) {
        if (encounter != null && encounter.getLocation() != null) {
            lineItem.setCreatedFrom(encounter.getLocation().getName());
        }
    }

    private void setPrescriptionFulfillment(PrescriptionLineItem lineItem, StockItemTransaction transaction) {
        if (transaction == null) {
            return;
        }
        lineItem.setStockItemTransactionId(transaction.getId());
        lineItem.setQuantityDispensed(transaction.getQuantity());
        lineItem.setDateDispensed(transaction.getDateCreated());
        setLineUser(transaction.getCreator(), lineItem::setDispenserUserId,
            lineItem::setDispenserGivenName, lineItem::setDispenserFamilyName);
        Party party = transaction.getParty();
        if (party != null) {
            lineItem.setDispensingLocation(partyName(party));
            if (party.getLocation() != null) {
                lineItem.setFulfilmentLocationUuid(party.getLocation().getUuid());
                lineItem.setFulfilmentLocation(party.getLocation().getName());
            }
        }
        StockBatch batch = transaction.getStockBatch();
        if (batch != null) {
            lineItem.setBatchNo(batch.getBatchNo());
            lineItem.setBatchExpiryDate(batch.getExpiration());
        }
        StockItemPackagingUOM uom = transaction.getStockItemPackagingUOM();
        if (uom != null) {
            lineItem.setQuantityDispensedStockItemPackagingUOMFactor(uom.getFactor());
            if (uom.getPackagingUom() != null) {
                lineItem.setQuantityDispensedPackagingUoMId(uom.getPackagingUom().getConceptId());
                lineItem.setQuantityDispensedStockItemPackagingUOMName(conceptName(uom.getPackagingUom()));
            }
        }
    }

    private Set<Integer> reportLocationIds(Integer locationId, Boolean childLocations) {
        if (locationId == null) {
            return null;
        }
        Set<Integer> locationIds = new HashSet<>();
        locationIds.add(locationId);
        if (Boolean.TRUE.equals(childLocations)) {
            getCompleteLocationTree(locationId).stream()
                    .map(LocationTree::getChildLocationId)
                    .filter(Objects::nonNull)
                    .forEach(locationIds::add);
        }
        return locationIds;
    }

    private Set<Integer> reportPartyIds(Integer partyId, Boolean childLocations) {
        if (partyId == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(childLocations)) {
            return Set.of(partyId);
        }
        Party party = first(listByIds(Party.class, "id", List.of(partyId)));
        if (party == null) {
            return Set.of();
        }
        if (party.getLocation() == null || party.getLocation().getLocationId() == null) {
            return Set.of(partyId);
        }
        Set<Integer> locationIds = reportLocationIds(party.getLocation().getLocationId(), true);
        Set<Integer> partyIds = new HashSet<>(getLocationPartyIds(locationIds).values());
        return partyIds.isEmpty() ? Set.of(partyId) : partyIds;
    }

    private Set<Integer> dispensingPartyIds(DispensingLineFilter filter) {
        if (filter.getAtLocationId() == null) {
            return null;
        }
        Location location = first(listByIds(Location.class, "locationId", List.of(filter.getAtLocationId())));
        if (location == null) {
            return Set.of();
        }
        Party party = firstByEntity(Party.class, "location", location);
        if (party == null) {
            return Set.of();
        }
        if (!Boolean.TRUE.equals(filter.getChildLocations())) {
            return Set.of(party.getId());
        }
        Set<Integer> locationIds = reportLocationIds(filter.getAtLocationId(), true);
        Set<Integer> partyIds = new HashSet<>(getLocationPartyIds(locationIds).values());
        return partyIds.isEmpty() ? Set.of(party.getId()) : partyIds;
    }

    private <T> Result<T> reportResultFromList(List<T> data, int pageIndex, int limit, boolean keysetPaging) {
        Result<T> result = new Result<>();
        List<T> safeData = data == null ? new ArrayList<>() : data;
        if (limit > 0) {
            int safePageIndex = Math.max(0, pageIndex);
            int fromIndex = keysetPaging ? 0 : safePageIndex * limit;
            int toIndex = Math.min(safeData.size(), fromIndex + limit);
            result.setData(fromIndex >= safeData.size() ? new ArrayList<>()
                    : new ArrayList<>(safeData.subList(fromIndex, toIndex)));
            result.setPageIndex(safePageIndex);
            result.setPageSize(limit);
        } else {
            result.setData(new ArrayList<>(safeData));
        }
        result.setTotalRecordCount((long) safeData.size());
        return result;
    }

    private Map<Integer, StockItem> stockItemsByDrugId(Collection<Integer> drugIds) {
        if (drugIds == null || drugIds.isEmpty()) {
            return Map.of();
        }
        return query(StockItem.class, (cb, root) -> {
            Join<StockItem, Drug> drug = root.join("drug", JoinType.INNER);
            return predicates(drug.get("drugId").in(drugIds));
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .filter(stockItem -> stockItem.getDrug() != null && stockItem.getDrug().getDrugId() != null)
                .collect(Collectors.toMap(stockItem -> stockItem.getDrug().getDrugId(), Function.identity(),
                    (existing, ignored) -> existing, LinkedHashMap::new));
    }

    private Map<Integer, List<StockItemTransaction>> stockTransactionsByOrderId(Collection<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return query(StockItemTransaction.class, (cb, root) -> predicates(root.get("order").get("orderId").in(orderIds)))
                .stream()
                .filter(transaction -> transaction.getOrder() != null && transaction.getOrder().getOrderId() != null)
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.groupingBy(transaction -> transaction.getOrder().getOrderId(),
                    LinkedHashMap::new, Collectors.toList()));
    }

    private boolean prescriptionStockItemMatchesFilter(StockItem stockItem, PrescriptionLineFilter filter) {
        if (filter.getStockItemId() != null
                && (stockItem == null || !Objects.equals(stockItem.getId(), filter.getStockItemId()))) {
            return false;
        }
        return filter.getStockItemCategoryConceptId() == null || (stockItem != null
                && stockItem.getCategory() != null
                && Objects.equals(stockItem.getCategory().getConceptId(), filter.getStockItemCategoryConceptId()));
    }

    private BigDecimal orderedQuantityInBaseUnits(DrugOrder order, StockItem stockItem) {
        if (order.getQuantity() == null) {
            return null;
        }
        BigDecimal factor = BigDecimal.ONE;
        StockItemPackagingUOM uom = packagingUomForConcept(stockItem, order.getQuantityUnits());
        if (uom == null && stockItem != null) {
            uom = stockItem.getDispensingUnitPackagingUoM();
        }
        if (uom != null && uom.getFactor() != null) {
            factor = uom.getFactor();
        }
        return BigDecimal.valueOf(order.getQuantity()).multiply(factor);
    }

    private BigDecimal absoluteTransactionQuantityInBaseUnits(StockItemTransaction transaction) {
        BigDecimal quantity = transaction.getQuantity();
        if (quantity == null) {
            return null;
        }
        BigDecimal factor = BigDecimal.ONE;
        if (transaction.getStockItemPackagingUOM() != null
                && transaction.getStockItemPackagingUOM().getFactor() != null) {
            factor = transaction.getStockItemPackagingUOM().getFactor();
        }
        return quantity.abs().multiply(factor);
    }

    private boolean matchesFulfillmentFilter(List<Fullfillment> fulfillments, BigDecimal orderedQuantity,
            BigDecimal dispensedQuantity, boolean noTransactions) {
        Set<Fullfillment> requested = new HashSet<>(fulfillments);
        if (requested.contains(Fullfillment.All)
                || (requested.contains(Fullfillment.Full) && requested.contains(Fullfillment.Partial)
                        && requested.contains(Fullfillment.None))) {
            return true;
        }
        if (noTransactions || dispensedQuantity == null || dispensedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return requested.contains(Fullfillment.None);
        }
        if (orderedQuantity == null || orderedQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return requested.contains(Fullfillment.Partial) || requested.contains(Fullfillment.Full);
        }
        int comparison = dispensedQuantity.compareTo(orderedQuantity);
        if (comparison >= 0) {
            return requested.contains(Fullfillment.Full);
        }
        return requested.contains(Fullfillment.Partial);
    }

    private StockItemPackagingUOM packagingUomForConcept(StockItem stockItem, Concept concept) {
        if (stockItem == null || concept == null || stockItem.getStockItemPackagingUOMs() == null) {
            return null;
        }
        return stockItem.getStockItemPackagingUOMs().stream()
                .filter(uom -> !Boolean.TRUE.equals(uom.getVoided()))
                .filter(uom -> uom.getPackagingUom() != null)
                .filter(uom -> Objects.equals(uom.getPackagingUom().getConceptId(), concept.getConceptId()))
                .findFirst()
                .orElse(null);
    }

    private void setLineUser(User user, java.util.function.Consumer<Integer> idSetter,
            java.util.function.Consumer<String> givenNameSetter,
            java.util.function.Consumer<String> familyNameSetter) {
        if (user == null) {
            return;
        }
        idSetter.accept(user.getId());
        givenNameSetter.accept(user.getGivenName());
        familyNameSetter.accept(user.getFamilyName());
    }

    private void setConcept(java.util.function.Consumer<Integer> idSetter,
            java.util.function.Consumer<String> nameSetter, Concept concept) {
        if (concept == null) {
            return;
        }
        idSetter.accept(concept.getConceptId());
        nameSetter.accept(conceptName(concept));
    }

    private Result<StockBatchLineItem> getExpiringStockBatchList(StockExpiryFilter filter) {
        StockExpiryFilter safeFilter = filter == null ? new StockExpiryFilter() : filter;
        List<StockBatchLineItem> lineItems = query(StockBatch.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockBatch, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            if (safeFilter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Date>get("expiration"), safeFilter.getStartDate()));
            }
            if (safeFilter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Date>get("expiration"), safeFilter.getEndDate()));
            }
            if (safeFilter.getStockBatchIdMin() != null) {
                predicates.add(cb.greaterThan(root.get("id"), safeFilter.getStockBatchIdMin()));
            }
            if (safeFilter.getStockItemIdMin() != null) {
                predicates.add(cb.greaterThan(stockItem.get("id"), safeFilter.getStockItemIdMin()));
            }
            if (safeFilter.getStockItemCategoryConceptId() != null) {
                predicates.add(cb.equal(stockItem.get("category").get("conceptId"),
                    safeFilter.getStockItemCategoryConceptId()));
            }
            predicates.add(cb.isFalse(root.get("voided")));
            return predicates;
        }).stream()
                .sorted(Comparator
                        .comparing((StockBatch batch) -> batch.getStockItem() == null ? null : batch.getStockItem().getId(),
                            Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::stockBatchLineItem)
                .collect(Collectors.toList());
        Integer limit = safeFilter.getLimit() > 0 ? safeFilter.getLimit() : null;
        return resultFromList(lineItems, safeFilter.getStartIndex(), limit);
    }

    private StockBatchLineItem stockBatchLineItem(StockBatch batch) {
        StockBatchLineItem lineItem = new StockBatchLineItem();
        lineItem.setStockBatchId(batch.getId());
        lineItem.setBatchNo(batch.getBatchNo());
        lineItem.setExpiration(batch.getExpiration());
        lineItem.setDateCreated(batch.getDateCreated());
        StockItem stockItem = batch.getStockItem();
        if (stockItem == null) {
            return lineItem;
        }
        lineItem.setStockItemId(stockItem.getId());
        lineItem.setCommonName(stockItem.getCommonName());
        lineItem.setAcronym(stockItem.getAcronym());
        lineItem.setExpiryNotice(stockItem.getExpiryNotice());
        lineItem.setReorderLevel(stockItem.getReorderLevel());
        if (stockItem.getDrug() != null) {
            lineItem.setStockItemDrugId(stockItem.getDrug().getDrugId());
            lineItem.setStockItemDrugName(stockItem.getDrug().getName());
        }
        if (stockItem.getConcept() != null) {
            lineItem.setStockItemConceptId(stockItem.getConcept().getConceptId());
            lineItem.setStockItemConceptName(conceptName(stockItem.getConcept()));
        }
        if (stockItem.getCategory() != null) {
            lineItem.setStockItemCategoryConceptId(stockItem.getCategory().getConceptId());
            lineItem.setStockItemCategoryName(conceptName(stockItem.getCategory()));
        }
        StockItemPackagingUOM reorderUom = stockItem.getReorderLevelUOM();
        if (reorderUom != null) {
            lineItem.setReorderLevelFactor(reorderUom.getFactor());
            if (reorderUom.getPackagingUom() != null) {
                lineItem.setReorderLevelUoMId(reorderUom.getPackagingUom().getConceptId());
                lineItem.setReorderLevelUoM(conceptName(reorderUom.getPackagingUom()));
            }
        }
        return lineItem;
    }

    private List<StockItemDTO> getExistingStockItemIds(
            Collection<StockItemSearchFilter.ItemGroupFilter> stockItemFilters) {
        if (stockItemFilters == null || stockItemFilters.isEmpty()) {
            return new ArrayList<>();
        }
        List<StockItemDTO> result = new ArrayList<>();
        for (StockItem item : listAll(StockItem.class)) {
            for (StockItemSearchFilter.ItemGroupFilter filter : stockItemFilters) {
                if (matchesItemGroup(item, filter)) {
                    result.add(stockItemToDto(item));
                    break;
                }
            }
        }
        return result;
    }

    private boolean matchesItemGroup(StockItem item, StockItemSearchFilter.ItemGroupFilter filter) {
        if (filter.getIsDrug() != null && !Objects.equals(Boolean.TRUE.equals(item.getIsDrug()), filter.getIsDrug())) {
            return false;
        }
        if (filter.getDrugId() != null
                && (item.getDrug() == null || !Objects.equals(item.getDrug().getDrugId(), filter.getDrugId()))) {
            return false;
        }
        return filter.getConceptId() == null
                || (item.getConcept() != null && Objects.equals(item.getConcept().getConceptId(), filter.getConceptId()));
    }

    private List<StockItemPackagingUOM> getStockItemPackagingUOMs(
            List<StockItemPackagingUOMSearchFilter.ItemGroupFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return new ArrayList<>();
        }
        return listAll(StockItemPackagingUOM.class).stream()
                .filter(uom -> filters.stream().anyMatch(filter -> uomMatchesFilter(uom, filter)))
                .collect(Collectors.toList());
    }

    private boolean uomMatchesFilter(StockItemPackagingUOM uom, StockItemPackagingUOMSearchFilter.ItemGroupFilter filter) {
        if (uom.getStockItem() == null || !Objects.equals(uom.getStockItem().getId(), filter.getStockItemId())) {
            return false;
        }
        return filter.getPackagingUomIds() == null || filter.getPackagingUomIds().isEmpty()
                || (uom.getPackagingUom() != null
                        && filter.getPackagingUomIds().contains(uom.getPackagingUom().getConceptId()));
    }

    private List<StockItem> stockItemsByDrug(Integer drugId) {
        if (drugId == null) {
            return new ArrayList<>();
        }
        return query(StockItem.class, (cb, root) -> {
            Join<StockItem, Drug> drug = root.join("drug", JoinType.LEFT);
            return predicates(cb.equal(drug.get("drugId"), drugId));
        });
    }

    private List<StockItem> stockItemsByConcept(Integer conceptId) {
        if (conceptId == null) {
            return new ArrayList<>();
        }
        return query(StockItem.class, (cb, root) -> {
            Join<StockItem, Drug> drug = root.join("drug", JoinType.LEFT);
            Join<StockItem, Concept> concept = root.join("concept", JoinType.LEFT);
            return predicates(cb.isNull(drug.get("drugId")), cb.equal(concept.get("conceptId"), conceptId));
        });
    }

    private StockItemPackagingUOM stockItemPackagingUOMByConcept(Object[] args) {
        Integer stockItemId = args[0] instanceof Integer ? (Integer) args[0] : null;
        if (stockItemId == null) {
            StockItem stockItem = byUuid(StockItem.class, (String) args[0]);
            stockItemId = stockItem == null ? null : stockItem.getId();
        }
        Integer conceptId = args[1] instanceof Integer ? (Integer) args[1] : null;
        if (conceptId == null) {
            Concept concept = byUuid(Concept.class, (String) args[1]);
            conceptId = concept == null ? null : concept.getConceptId();
        }
        if (stockItemId == null || conceptId == null) {
            return null;
        }
        Integer finalStockItemId = stockItemId;
        Integer finalConceptId = conceptId;
        List<StockItemPackagingUOM> matches = query(StockItemPackagingUOM.class, (cb, root) -> {
            Join<StockItemPackagingUOM, StockItem> stockItem = root.join("stockItem", JoinType.LEFT);
            Join<StockItemPackagingUOM, Concept> packaging = root.join("packagingUom", JoinType.LEFT);
            return predicates(cb.equal(stockItem.get("id"), finalStockItemId),
                cb.equal(packaging.get("conceptId"), finalConceptId));
        });
        matches.sort(Comparator
                .comparing((StockItemPackagingUOM uom) -> Boolean.TRUE.equals(uom.getVoided()))
                .thenComparing(this::objectId, Comparator.nullsLast(Integer::compareTo)));
        return first(matches);
    }

    private List<OrderItem> getOrderItemsByOrder(Integer... orderIds) {
        if (orderIds == null || orderIds.length == 0) {
            return new ArrayList<>();
        }
        List<Integer> ids = List.of(orderIds);
        return query(OrderItem.class, (cb, root) -> predicates(root.get("order").get("orderId").in(ids)));
    }

    private List<OrderItem> getOrderItemsByEncounter(Integer... encounterIds) {
        if (encounterIds == null || encounterIds.length == 0) {
            return new ArrayList<>();
        }
        List<Integer> ids = List.of(encounterIds);
        return query(OrderItem.class, (cb, root) -> predicates(root.get("order").get("encounter").get("encounterId")
                .in(ids)));
    }

    private Result<OrderItemDTO> findOrderItems(
            OrderItemSearchFilter filter, HashSet<RecordPrivilegeFilter> recordPrivilegeFilters) {
        OrderItemSearchFilter safeFilter = filter == null ? new OrderItemSearchFilter() : filter;
        Set<Integer> createdFromLocationIds = resolveOrderLocationIds(
            safeFilter.getCreatedFromLocationIds(),
            safeFilter.getCreatedFromLocationUuids(),
            safeFilter.getCreatedFromPartyUuids());
        if (hasOrderLocationFilter(safeFilter.getCreatedFromLocationIds(), safeFilter.getCreatedFromLocationUuids(),
            safeFilter.getCreatedFromPartyUuids()) && createdFromLocationIds.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }
        Set<Integer> fulfilmentLocationIds = resolveOrderLocationIds(
            safeFilter.getFulfilmentLocationIds(),
            safeFilter.getFulfilmentLocationUuids(),
            safeFilter.getFulfilmentPartyUuids());
        if (hasOrderLocationFilter(safeFilter.getFulfilmentLocationIds(), safeFilter.getFulfilmentLocationUuids(),
            safeFilter.getFulfilmentPartyUuids()) && fulfilmentLocationIds.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }
        Set<Integer> allowedLocationIds = recordPrivilegeFilters == null ? null
                : recordPrivilegeFilters.stream()
                        .map(RecordPrivilegeFilter::getLocationId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (recordPrivilegeFilters != null && allowedLocationIds.isEmpty()) {
            return new Result<>(new ArrayList<>(), 0);
        }

        List<OrderItemDTO> orderItems = query(OrderItem.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<OrderItem, Order> orderJoin = root.join("order", JoinType.LEFT);
            Join<OrderItem, StockItem> stockItemJoin = root.join("stockItem", JoinType.LEFT);
            Join<StockItem, Drug> drugJoin = stockItemJoin.join("drug", JoinType.LEFT);
            Join<StockItem, Concept> conceptJoin = stockItemJoin.join("concept", JoinType.LEFT);
            Join<OrderItem, Location> createdFromJoin = root.join("createdFrom", JoinType.LEFT);
            Join<OrderItem, Location> fulfilmentLocationJoin = root.join("fulfilmentLocation", JoinType.LEFT);
            if (safeFilter.getId() != null) {
                predicates.add(cb.equal(root.get("id"), safeFilter.getId()));
            }
            if (StringUtils.isNotBlank(safeFilter.getUuid())) {
                predicates.add(cb.equal(root.get("uuid"), safeFilter.getUuid()));
            }
            addInPredicate(predicates, root.get("order").get("orderId"), safeFilter.getOrderIds());
            addInPredicate(predicates, orderJoin.get("uuid"), safeFilter.getOrderUuids());
            addDateRangePredicate(cb, predicates, orderJoin, "dateActivated",
                safeFilter.getOrderDateMin(), safeFilter.getOrderDateMax());
            addInPredicate(predicates, orderJoin.get("encounter").get("encounterId"), safeFilter.getEncounterIds());
            addInPredicate(predicates, orderJoin.get("encounter").get("uuid"), safeFilter.getEncounterUuids());
            addInPredicate(predicates, orderJoin.get("patient").get("patientId"), safeFilter.getPatientIds());
            if (StringUtils.isNotBlank(safeFilter.getOrderNumber())) {
                String orderNumber = safeFilter.getOrderNumber().replace("%", "");
                if (StringUtils.isBlank(orderNumber)) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(cb.like(orderJoin.get("orderNumber"), orderNumber.toUpperCase(Locale.ROOT) + "%"));
                }
            }
            if (safeFilter.getIsDrug() != null) {
                predicates.add(safeFilter.getIsDrug()
                        ? cb.isNotNull(drugJoin.get("drugId"))
                        : cb.isNull(drugJoin.get("drugId")));
            }
            addInPredicate(predicates, stockItemJoin.get("id"), safeFilter.getStockItemIds());
            addInPredicate(predicates, stockItemJoin.get("uuid"), safeFilter.getStockItemUuids());
            addDrugConceptPredicates(cb, predicates, drugJoin, conceptJoin, safeFilter);
            addInPredicate(predicates, createdFromJoin.get("locationId"), createdFromLocationIds);
            addInPredicate(predicates, fulfilmentLocationJoin.get("locationId"), fulfilmentLocationIds);
            if (allowedLocationIds != null) {
                predicates.add(cb.or(
                    createdFromJoin.get("locationId").in(allowedLocationIds),
                    fulfilmentLocationJoin.get("locationId").in(allowedLocationIds)));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)).reversed())
                .map(this::orderItemToDto)
                .collect(Collectors.toList());
        return resultFromList(orderItems, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private void addDrugConceptPredicates(CriteriaBuilder cb, List<Predicate> predicates,
            Join<StockItem, Drug> drugJoin, Join<StockItem, Concept> conceptJoin, OrderItemSearchFilter filter) {
        List<Predicate> drugConceptPredicates = new ArrayList<>();
        addInPredicate(drugConceptPredicates, drugJoin.get("drugId"), filter.getDrugIds());
        addInPredicate(drugConceptPredicates, drugJoin.get("uuid"), filter.getDrugUuids());
        addInPredicate(drugConceptPredicates, conceptJoin.get("conceptId"), filter.getConceptIds());
        addInPredicate(drugConceptPredicates, conceptJoin.get("uuid"), filter.getConceptUuids());
        if (drugConceptPredicates.isEmpty()) {
            return;
        }
        predicates.add(filter.getSearchEitherDrugOrConceptStockItems()
                ? cb.or(drugConceptPredicates.toArray(new Predicate[0]))
                : cb.and(drugConceptPredicates.toArray(new Predicate[0])));
    }

    private boolean hasOrderLocationFilter(List<Integer> locationIds, List<String> locationUuids,
            List<String> partyUuids) {
        return (locationIds != null && !locationIds.isEmpty())
                || (locationUuids != null && !locationUuids.isEmpty())
                || (partyUuids != null && !partyUuids.isEmpty());
    }

    private Set<Integer> resolveOrderLocationIds(List<Integer> locationIds, List<String> locationUuids,
            List<String> partyUuids) {
        Set<Integer> resolved = new HashSet<>();
        if (locationIds != null) {
            resolved.addAll(locationIds.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        }
        if (locationUuids != null && !locationUuids.isEmpty()) {
            query(Location.class, (cb, root) -> predicates(root.get("uuid").in(locationUuids))).stream()
                    .map(Location::getLocationId)
                    .filter(Objects::nonNull)
                    .forEach(resolved::add);
        }
        if (partyUuids != null && !partyUuids.isEmpty()) {
            query(Party.class, (cb, root) -> predicates(root.get("uuid").in(partyUuids))).stream()
                    .map(Party::getLocation)
                    .filter(Objects::nonNull)
                    .map(Location::getLocationId)
                    .filter(Objects::nonNull)
                    .forEach(resolved::add);
        }
        return resolved;
    }

    private void addDateRangePredicate(CriteriaBuilder cb, List<Predicate> predicates, Join<?, ?> join,
            String field, Date min, Date max) {
        if (min != null) {
            predicates.add(cb.greaterThanOrEqualTo(join.<Date>get(field), min));
        }
        if (max != null) {
            predicates.add(cb.lessThanOrEqualTo(join.<Date>get(field), max));
        }
    }

    private <T> void addInPredicate(List<Predicate> predicates, jakarta.persistence.criteria.Path<T> path,
            Collection<T> values) {
        if (values != null && !values.isEmpty()) {
            predicates.add(path.in(values));
        }
    }

    private OrderItemDTO orderItemToDto(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setUuid(orderItem.getUuid());
        dto.setVoided(Boolean.TRUE.equals(orderItem.getVoided()));
        dto.setCreator(orderItem.getCreator() == null ? null : orderItem.getCreator().getId());
        dto.setDateCreated(orderItem.getDateCreated());
        if (orderItem.getCreator() != null) {
            dto.setCreatorGivenName(orderItem.getCreator().getGivenName());
            dto.setCreatorFamilyName(orderItem.getCreator().getFamilyName());
        }
        setOrderItemOrder(dto, orderItem.getOrder());
        setOrderItemStockItem(dto, orderItem.getStockItem());
        setOrderItemPackagingUom(dto, orderItem.getStockItemPackagingUOM());
        setOrderItemLocation(dto, orderItem.getCreatedFrom(), true);
        setOrderItemLocation(dto, orderItem.getFulfilmentLocation(), false);
        return dto;
    }

    private void setOrderItemOrder(OrderItemDTO dto, Order order) {
        if (order == null) {
            return;
        }
        dto.setOrderId(order.getOrderId());
        dto.setOrderUuid(order.getUuid());
        dto.setAction(order.getAction());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setScheduledDate(order.getScheduledDate());
        if (order.getEncounter() != null) {
            dto.setEncounterUuid(order.getEncounter().getUuid());
        }
        Patient patient = order.getPatient();
        if (patient != null) {
            dto.setPatientId(patient.getPatientId());
            dto.setPatientGivenName(patient.getGivenName());
            dto.setPatientFamilyName(patient.getFamilyName());
        }
        if (order instanceof DrugOrder drugOrder) {
            if (drugOrder.getQuantity() != null) {
                dto.setQuantity(BigDecimal.valueOf(drugOrder.getQuantity()));
            }
            dto.setDuration(drugOrder.getDuration());
        }
    }

    private void setOrderItemStockItem(OrderItemDTO dto, StockItem stockItem) {
        if (stockItem == null) {
            return;
        }
        dto.setStockItemId(stockItem.getId());
        dto.setStockItemUuid(stockItem.getUuid());
        dto.setCommonName(stockItem.getCommonName());
        dto.setAcronym(stockItem.getAcronym());
        if (stockItem.getDrug() != null) {
            dto.setDrugId(stockItem.getDrug().getDrugId());
            dto.setDrugUuid(stockItem.getDrug().getUuid());
            dto.setDrugName(stockItem.getDrug().getName());
        }
        if (stockItem.getConcept() != null) {
            dto.setConceptId(stockItem.getConcept().getConceptId());
            dto.setConceptUuid(stockItem.getConcept().getUuid());
            dto.setConceptName(conceptName(stockItem.getConcept()));
        }
    }

    private void setOrderItemPackagingUom(OrderItemDTO dto, StockItemPackagingUOM uom) {
        if (uom == null) {
            return;
        }
        dto.setStockItemPackagingUOMId(uom.getId());
        dto.setStockItemPackagingUOMUuid(uom.getUuid());
        if (uom.getPackagingUom() != null) {
            dto.setStockItemPackagingUOMConceptId(uom.getPackagingUom().getConceptId());
            dto.setStockItemPackagingUOMName(conceptName(uom.getPackagingUom()));
        }
    }

    private void setOrderItemLocation(OrderItemDTO dto, Location location, boolean createdFrom) {
        if (location == null) {
            return;
        }
        Party party = firstByEntity(Party.class, "location", location);
        if (createdFrom) {
            dto.setCreatedFrom(location.getLocationId());
            dto.setCreatedFromUuid(location.getUuid());
            dto.setCreatedFromName(location.getName());
            if (party != null) {
                dto.setCreatedFromPartyUuid(party.getUuid());
                dto.setCreatedFromName(partyName(party));
            }
        } else {
            dto.setFulfilmentLocationId(location.getLocationId());
            dto.setFulfilmentLocationUuid(location.getUuid());
            dto.setFulfilmentLocationName(location.getName());
            if (party != null) {
                dto.setFulfilmentPartyUuid(party.getUuid());
                dto.setFulfilmentLocationName(partyName(party));
            }
        }
    }

    private Map<Integer, String> getStockItemNames(List<Integer> stockItemIds) {
        return listByIds(StockItem.class, "id", stockItemIds).stream()
                .collect(Collectors.toMap(StockItem::getId, this::stockItemName, (a, b) -> a));
    }

    private Map<Integer, String> getConceptNames(List<Integer> conceptIds) {
        return listByIds(Concept.class, "conceptId", conceptIds).stream()
                .collect(Collectors.toMap(Concept::getConceptId, this::conceptName, (a, b) -> a));
    }

    private Map<Integer, String> getLocationNames(List<Integer> locationIds) {
        return listByIds(Location.class, "locationId", locationIds).stream()
                .collect(Collectors.toMap(Location::getId, Location::getName, (a, b) -> a));
    }

    private void runStockRuleEvaluationJob() {
        new StockRuleEvaluationJob().execute();
    }

    private void runStockBatchExpiryNoticationJob() {
        new StockBatchExpiryJob().execute();
    }

    private void sendStockOperationNotification(String stockOperationUuid, StockOperationAction.Action action,
            String actionReason, Integer actionByUserId) {
        StockOperationSearchFilter filter = new StockOperationSearchFilter();
        filter.setStockOperationUuid(stockOperationUuid);
        Result<StockOperationDTO> result = findStockOperations(filter, null);
        StockOperationDTO operation = result.getData().isEmpty() ? null : result.getData().get(0);
        if (operation == null) {
            return;
        }

        String configuredEmail = GlobalProperties.getStockOperationNotificationEmail();
        if (!org.openmrs.module.stockmanagement.api.utils.StringUtils.isValidEmail(configuredEmail)) {
            configuredEmail = null;
        }
        String roleToNotify = GlobalProperties.getStockOperationNotificationRole();
        if (StringUtils.isBlank(configuredEmail) && StringUtils.isBlank(roleToNotify)) {
            log.info("Stock operation notification not sent; email and role settings are empty");
            return;
        }

        User actor = actionByUserId == null ? null : Context.getUserService().getUser(actionByUserId);
        String actorName = actor == null
                ? (actionByUserId == null ? "System" : "User ID " + actionByUserId)
                : actor.getDisplayString();
        List<User> usersToNotify = notificationUsersForRole(roleToNotify, operation, action);
        createStockOperationAlert(operation, action, actor, actorName, usersToNotify);

        List<String> emailRecipients = new ArrayList<>();
        if (usersToNotify != null) {
            usersToNotify.stream()
                    .map(this::getUserEmailAddress)
                    .filter(StringUtils::isNotBlank)
                    .forEach(emailRecipients::add);
        }
        if (StringUtils.isNotBlank(configuredEmail)) {
            emailRecipients.add(configuredEmail);
        }
        emailRecipients = emailRecipients.stream().distinct().collect(Collectors.toList());
        if (emailRecipients.isEmpty()) {
            log.info("Stock operation notification not sent; no valid email recipients");
            return;
        }
        if (!SmtpUtil.hasSmptHostSetup()) {
            log.info("Stock operation notification not sent; SMTP is not configured");
            return;
        }

        Template template = stockOperationNotificationTemplate();
        if (template == null) {
            return;
        }
        String healthCenterName = healthCenterName();
        String details = stockOperationNotificationDetails(operation, action, actionReason);
        String actionName = StockOperationAction.getActionName(action);
        String body = replaceStockOperationNotificationTokens(template.getTemplate(), operation, actionName, actorName,
            details, healthCenterName);
        String subject = replaceStockOperationNotificationTokens(template.getSubject(), operation, actionName, actorName,
            details, healthCenterName);
        try {
            jakarta.mail.Session mailSession = SmtpUtil.getSession();
            SmtpUtil.sendEmail(subject, body, mailSession, emailRecipients.toArray(new String[0]));
        } catch (Exception exception) {
            log.error("Unable to send stock operation notification email", exception);
        }
    }

    private List<User> notificationUsersForRole(String roleToNotify, StockOperationDTO operation,
            StockOperationAction.Action action) {
        if (StringUtils.isBlank(roleToNotify)) {
            return new ArrayList<>();
        }
        Role role = Context.getUserService().getRole(roleToNotify);
        if (role == null) {
            log.info("Stock operation " + operation.getOperationNumber() + " action " + action
                    + " notification not sent to role; role not found");
            return new ArrayList<>();
        }
        return Context.getUserService().getUsersByRole(role);
    }

    private void createStockOperationAlert(StockOperationDTO operation, StockOperationAction.Action action,
            User actor, String actorName, List<User> usersToNotify) {
        if (usersToNotify == null || usersToNotify.isEmpty()) {
            return;
        }
        Alert alert = new Alert();
        alert.setDateCreated(new Date());
        alert.setCreator(actor);
        alert.setDateToExpire(addDays(new Date(), 7));
        alert.setText(String.format(Context.getMessageSourceService().getMessage("stockmanagement.stockoperation.alertmsg"),
            operation.getAtLocationName(), operation.getOperationTypeName(), operation.getOperationNumber(),
            StockOperationAction.getActionName(action), actorName));
        usersToNotify.forEach(alert::addRecipient);
        Context.getAlertService().saveAlert(alert);
    }

    private Template stockOperationNotificationTemplate() {
        try {
            List<Template> templates = Context.getMessageService().getTemplatesByName("STOCK_MGMT_OPERATION_ACTION");
            if (templates.isEmpty()) {
                log.debug("Template STOCK_MGMT_OPERATION_ACTION not found");
                return null;
            }
            return templates.get(0);
        } catch (Exception exception) {
            log.error("Unable to load stock operation notification template", exception);
            return null;
        }
    }

    private String stockOperationNotificationDetails(StockOperationDTO operation, StockOperationAction.Action action,
            String actionReason) {
        MessageSourceService messages = Context.getMessageSourceService();
        StringBuilder details = new StringBuilder();
        if (StringUtils.isNotBlank(operation.getSourceUuid())) {
            appendNotificationRow(details, messages.getMessage("stockmanagement.stockoperation.notification.source"),
                operation.getSourceName());
        }
        if (StringUtils.isNotBlank(operation.getDestinationUuid())) {
            appendNotificationRow(details,
                messages.getMessage("stockmanagement.stockoperation.notification.destination"),
                operation.getDestinationName());
        }
        appendNotificationRow(details, messages.getMessage("stockmanagement.stockoperation.notification.operationdate"),
            DateUtil.formatDDMMMyyyy(operation.getOperationDate()));
        String responsible = operation.getResponsiblePersonFamilyName() != null
                ? operation.getResponsiblePersonFamilyName() + " " + operation.getResponsiblePersonGivenName()
                : operation.getResponsiblePersonOther();
        appendNotificationRow(details,
            messages.getMessage("stockmanagement.stockoperation.notification.responsibleperson"), responsible);
        appendNotificationRow(details, messages.getMessage("stockmanagement.stockoperation.notification.remarks"),
            operation.getRemarks());
        if (action == StockOperationAction.Action.RETURN) {
            appendNotificationRow(details,
                messages.getMessage("stockmanagement.stockoperation.notification.returnreason"), actionReason);
        } else if (action == StockOperationAction.Action.REJECT) {
            appendNotificationRow(details,
                messages.getMessage("stockmanagement.stockoperation.notification.rejectionreason"), actionReason);
        }
        return details.toString();
    }

    private void appendNotificationRow(StringBuilder builder, String label, String value) {
        builder.append("<tr><td style='padding: 0.2rem 0.5rem; font-size: 95%;'><b>")
                .append(escapeHtml(label))
                .append(":</b></td><td style='padding: 0.2rem 0.5rem; font-size: 95%;'>")
                .append(StringUtils.isBlank(value) ? "&nbsp;" : escapeHtml(value))
                .append("</td></tr>");
    }

    private String replaceStockOperationNotificationTokens(String template, StockOperationDTO operation,
            String actionName, String actorName, String details, String healthCenterName) {
        return (template == null ? "" : template)
                .replace("%LOCATION%", nullToEmpty(operation.getAtLocationName()))
                .replace("%OPERATION_TYPE%", nullToEmpty(operation.getOperationTypeName()))
                .replace("%OPERATION_REF%", nullToEmpty(operation.getOperationNumber()))
                .replace("%OPERATION_ACTION%", nullToEmpty(actionName))
                .replace("%ACTION_BY%", nullToEmpty(actorName))
                .replace("%OPERATION_BASIC_INFO%", details == null ? "" : details)
                .replace("%CENTER_NAME%", nullToEmpty(healthCenterName));
    }

    private String healthCenterName() {
        String healthCenterName = GlobalProperties.getHealthCenterName();
        if (StringUtils.isNotBlank(healthCenterName)) {
            return healthCenterName;
        }
        return Context.getAdministrationService().getGlobalProperty("site.name", "NOT SET");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeHtml(String value) {
        return StringEscapeUtils.escapeHtml4(value == null ? "" : value);
    }

    private void updateStockBatchExpiryNotificationDate(Collection<Integer> stockBatchIds, Date notificationDate) {
        for (StockBatch batch : listByIds(StockBatch.class, "id", stockBatchIds)) {
            batch.setExpiryNotificationDate(notificationDate);
            saveOrUpdate(batch);
        }
    }

    private void updateStockRuleDate(List<Integer> stockRuleIds, Date nextEvaluationDate, boolean evaluationDate) {
        for (StockRule rule : listByIds(StockRule.class, "id", stockRuleIds)) {
            if (evaluationDate) {
                rule.setNextEvaluation(nextEvaluationDate);
            } else {
                rule.setNextActionDate(nextEvaluationDate);
            }
            saveOrUpdate(rule);
        }
    }

    private void setStockItemCurrentBalanceWithDescendants(List<StockRuleCurrentQuantity> currentQuantities) {
        if (currentQuantities == null || currentQuantities.isEmpty()) {
            return;
        }
        Set<String> wantedKeys = currentQuantities.stream()
                .filter(quantity -> quantity.getLocationId() != null && quantity.getStockItemId() != null)
                .map(quantity -> inventoryKey(quantity.getLocationId(), quantity.getStockItemId()))
                .collect(Collectors.toSet());
        if (wantedKeys.isEmpty()) {
            return;
        }

        List<LocationTree> locationTrees = listAll(LocationTree.class);
        Set<Integer> ruleLocationIds = currentQuantities.stream()
                .map(StockRuleCurrentQuantity::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> descendantLocationIds = locationTrees.stream()
                .filter(tree -> ruleLocationIds.contains(tree.getParentLocationId()))
                .map(LocationTree::getChildLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        descendantLocationIds.addAll(ruleLocationIds);
        Map<Integer, Integer> partyIdsByLocation = getLocationPartyIds(descendantLocationIds);
        if (partyIdsByLocation.isEmpty()) {
            return;
        }

        Map<Integer, Set<Integer>> ancestorIdsByLocation = locationTrees.stream()
                .filter(tree -> tree.getChildLocationId() != null && tree.getParentLocationId() != null)
                .collect(Collectors.groupingBy(LocationTree::getChildLocationId,
                    Collectors.mapping(LocationTree::getParentLocationId, Collectors.toSet())));
        Map<String, BigDecimal> quantitiesByRule = new LinkedHashMap<>();
        for (StockItemTransaction transaction : currentBalanceTransactions(
                stockItemIds(currentQuantities), partyIdsByLocation.values())) {
            Party party = transaction.getParty();
            StockItem stockItem = transaction.getStockItem();
            if (party == null || party.getLocation() == null || stockItem == null || stockItem.getId() == null) {
                continue;
            }
            BigDecimal quantity = transactionQuantityInBaseUnits(transaction);
            if (quantity == null) {
                continue;
            }
            Integer locationId = party.getLocation().getLocationId();
            Set<Integer> ancestorIds = ancestorIdsByLocation.get(locationId);
            if (ancestorIds == null || ancestorIds.isEmpty()) {
                ancestorIds = Set.of(locationId);
            }
            for (Integer ancestorId : ancestorIds) {
                String key = inventoryKey(ancestorId, stockItem.getId());
                if (wantedKeys.contains(key)) {
                    quantitiesByRule.merge(key, quantity, BigDecimal::add);
                }
            }
        }
        setRuleQuantities(currentQuantities, quantitiesByRule, null);
    }

    private void setStockItemCurrentBalanceWithoutDescendants(List<StockRuleCurrentQuantity> currentQuantities) {
        if (currentQuantities == null || currentQuantities.isEmpty()) {
            return;
        }
        Map<Integer, Integer> partyIdsByLocation = getLocationPartyIds(currentQuantities.stream()
                .map(StockRuleCurrentQuantity::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        if (partyIdsByLocation.isEmpty()) {
            return;
        }
        Set<String> wantedKeys = currentQuantities.stream()
                .filter(quantity -> quantity.getLocationId() != null && quantity.getStockItemId() != null)
                .map(quantity -> {
                    Integer partyId = partyIdsByLocation.get(quantity.getLocationId());
                    return partyId == null ? null : inventoryKey(partyId, quantity.getStockItemId());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (wantedKeys.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> quantitiesByParty = new LinkedHashMap<>();
        for (StockItemTransaction transaction : currentBalanceTransactions(
                stockItemIds(currentQuantities), partyIdsByLocation.values())) {
            Party party = transaction.getParty();
            StockItem stockItem = transaction.getStockItem();
            if (party == null || party.getId() == null || stockItem == null || stockItem.getId() == null) {
                continue;
            }
            String key = inventoryKey(party.getId(), stockItem.getId());
            if (!wantedKeys.contains(key)) {
                continue;
            }
            BigDecimal quantity = transactionQuantityInBaseUnits(transaction);
            if (quantity != null) {
                quantitiesByParty.merge(key, quantity, BigDecimal::add);
            }
        }
        setRuleQuantities(currentQuantities, quantitiesByParty, partyIdsByLocation);
    }

    private Set<Integer> stockItemIds(List<StockRuleCurrentQuantity> currentQuantities) {
        return currentQuantities.stream()
                .map(StockRuleCurrentQuantity::getStockItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<StockItemTransaction> currentBalanceTransactions(Set<Integer> stockItemIds,
            Collection<Integer> partyIds) {
        if (stockItemIds == null || stockItemIds.isEmpty() || partyIds == null || partyIds.isEmpty()) {
            return new ArrayList<>();
        }
        Date today = DateUtil.today();
        return query(StockItemTransaction.class, (cb, root) -> {
            Join<StockItemTransaction, StockBatch> batch = root.join("stockBatch", JoinType.INNER);
            return predicates(
                root.get("party").get("id").in(partyIds),
                root.get("stockItem").get("id").in(stockItemIds),
                cb.or(cb.isNull(batch.get("expiration")), cb.greaterThan(batch.<Date>get("expiration"), today)));
        });
    }

    private BigDecimal transactionQuantityInBaseUnits(StockItemTransaction transaction) {
        if (transaction.getQuantity() == null || transaction.getStockItemPackagingUOM() == null
                || transaction.getStockItemPackagingUOM().getFactor() == null) {
            return null;
        }
        return transaction.getQuantity().multiply(transaction.getStockItemPackagingUOM().getFactor());
    }

    private void setRuleQuantities(List<StockRuleCurrentQuantity> currentQuantities,
            Map<String, BigDecimal> quantitiesByKey, Map<Integer, Integer> partyIdsByLocation) {
        for (StockRuleCurrentQuantity currentQuantity : currentQuantities) {
            if (currentQuantity.getLocationId() == null || currentQuantity.getStockItemId() == null) {
                continue;
            }
            Integer ownerId = partyIdsByLocation == null
                    ? currentQuantity.getLocationId()
                    : partyIdsByLocation.get(currentQuantity.getLocationId());
            BigDecimal quantity = quantitiesByKey.get(inventoryKey(ownerId, currentQuantity.getStockItemId()));
            if (quantity != null) {
                currentQuantity.setQuantity(quantity);
            }
        }
    }

    private String inventoryKey(Integer ownerId, Integer stockItemId) {
        return ownerId + ":" + stockItemId;
    }

    private Map<Integer, Integer> getLocationPartyIds(Collection<Integer> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return query(Party.class, (cb, root) -> predicates(root.get("location").get("locationId").in(locationIds)))
                .stream()
                .filter(party -> party.getLocation() != null && party.getLocation().getLocationId() != null)
                .filter(party -> party.getId() != null)
                .collect(Collectors.toMap(party -> party.getLocation().getLocationId(), Party::getId, (a, b) -> b,
                    LinkedHashMap::new));
    }

    private List<StockRuleNotificationUser> getDueStockRules(Integer lastStockRuleId, int limit) {
        if (limit <= 0) {
            return new ArrayList<>();
        }
        Date now = new Date();
        List<StockRuleNotificationUser> rules = query(StockRule.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<StockRule, StockItem> stockItem = root.join("stockItem", JoinType.INNER);
            predicates.add(cb.greaterThan(root.get("id"), lastStockRuleId == null ? 0 : lastStockRuleId));
            predicates.add(cb.or(cb.isNull(root.get("nextEvaluation")),
                cb.lessThanOrEqualTo(root.<Date>get("nextEvaluation"), now)));
            predicates.add(cb.or(cb.isNull(root.get("nextActionDate")),
                cb.lessThanOrEqualTo(root.<Date>get("nextActionDate"), now)));
            predicates.add(cb.isTrue(root.get("enabled")));
            predicates.add(cb.isFalse(root.get("voided")));
            predicates.add(cb.isFalse(stockItem.get("voided")));
            return predicates;
        }).stream()
                .sorted(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)))
                .limit(limit)
                .map(this::stockRuleNotificationUser)
                .collect(Collectors.toList());
        return rules;
    }

    private StockRuleNotificationUser stockRuleNotificationUser(StockRule rule) {
        StockRuleNotificationUser notificationUser = new StockRuleNotificationUser();
        notificationUser.setId(rule.getId());
        if (rule.getStockItem() != null) {
            notificationUser.setStockItemId(rule.getStockItem().getId());
        }
        if (rule.getLocation() != null) {
            notificationUser.setLocationId(rule.getLocation().getLocationId());
        }
        notificationUser.setAlertRole(rule.getAlertRole());
        notificationUser.setMailRole(rule.getMailRole());
        notificationUser.setEnableDescendants(Boolean.TRUE.equals(rule.getEnableDescendants()));
        notificationUser.setEvaluationFrequency(rule.getEvaluationFrequency());
        notificationUser.setActionFrequency(rule.getActionFrequency());
        BigDecimal factor = BigDecimal.ONE;
        if (rule.getStockItemPackagingUOM() != null) {
            if (rule.getStockItemPackagingUOM().getFactor() != null) {
                factor = rule.getStockItemPackagingUOM().getFactor();
            }
            notificationUser.setFactor(factor);
            if (rule.getStockItemPackagingUOM().getPackagingUom() != null) {
                notificationUser.setPackagingConceptId(
                    rule.getStockItemPackagingUOM().getPackagingUom().getConceptId());
            }
        }
        notificationUser.setQuantity(rule.getQuantity() == null ? null : rule.getQuantity().multiply(factor));
        return notificationUser;
    }

    private List<Integer> getActiveUsersAssignedForScope(Integer locationId, List<String> roles) {
        if (locationId == null || roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Integer> ancestorLocationIds = ancestorLocationIds(locationId);
        Date today = DateUtil.today();
        return listAll(UserRoleScope.class).stream()
                .filter(scope -> scope.getUser() != null && !Boolean.TRUE.equals(scope.getUser().getRetired()))
                .filter(scope -> scope.getRole() != null && roles.contains(scope.getRole().getRole()))
                .filter(scope -> !Boolean.TRUE.equals(scope.getVoided()))
                .filter(scope -> Boolean.TRUE.equals(scope.getEnabled()))
                .filter(scope -> activeToday(scope, today))
                .filter(scope -> scopeLocationsMatch(scope, locationId, ancestorLocationIds))
                .map(scope -> scope.getUser().getId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Set<Integer> ancestorLocationIds(Integer locationId) {
        Set<Integer> ids = new HashSet<>();
        ids.add(locationId);
        query(LocationTree.class, (cb, root) -> predicates(cb.equal(root.get("childLocationId"), locationId))).stream()
                .map(LocationTree::getParentLocationId)
                .filter(Objects::nonNull)
                .forEach(ids::add);
        return ids;
    }

    private boolean activeToday(UserRoleScope scope, Date today) {
        if (Boolean.TRUE.equals(scope.getPermanent())) {
            return true;
        }
        return scope.getActiveFrom() != null && scope.getActiveTo() != null
                && !scope.getActiveFrom().after(today) && !scope.getActiveTo().before(today);
    }

    private boolean scopeLocationsMatch(UserRoleScope scope, Integer locationId, Set<Integer> ancestorLocationIds) {
        return activeUserRoleScopeLocations(scope).stream().anyMatch(scopeLocation -> {
            Location location = scopeLocation.getLocation();
            if (location == null || Boolean.TRUE.equals(location.getRetired())) {
                return false;
            }
            Integer scopeLocationId = location.getLocationId();
            return Boolean.TRUE.equals(scopeLocation.getEnableDescendants())
                    ? ancestorLocationIds.contains(scopeLocationId)
                    : Objects.equals(scopeLocationId, locationId);
        });
    }

    private Result<BatchJobDTO> findBatchJobs(BatchJobSearchFilter filter) {
        BatchJobSearchFilter safeFilter = filter == null ? new BatchJobSearchFilter() : filter;
        List<BatchJobDTO> dtos = findBatchJobEntities(safeFilter).stream()
                .map(this::batchJobToDto)
                .collect(Collectors.toList());
        return resultFromList(dtos, safeFilter.getStartIndex(), safeFilter.getLimit());
    }

    private List<BatchJob> findBatchJobEntities(BatchJobSearchFilter filter) {
        BatchJobSearchFilter safeFilter = filter == null ? new BatchJobSearchFilter() : filter;
        List<BatchJob> jobs = query(BatchJob.class, (cb, root) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (safeFilter.getBatchJobIds() != null && !safeFilter.getBatchJobIds().isEmpty()) {
                predicates.add(root.get("id").in(safeFilter.getBatchJobIds()));
            }
            if (safeFilter.getBatchJobUuids() != null && !safeFilter.getBatchJobUuids().isEmpty()) {
                predicates.add(root.get("uuid").in(safeFilter.getBatchJobUuids()));
            }
            if (safeFilter.getBatchJobType() != null) {
                predicates.add(cb.equal(root.get("batchJobType"), safeFilter.getBatchJobType()));
            }
            if (safeFilter.getBatchJobStatus() != null && !safeFilter.getBatchJobStatus().isEmpty()) {
                predicates.add(root.get("status").in(safeFilter.getBatchJobStatus()));
            }
            if (safeFilter.getDateCreatedMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateCreated"), safeFilter.getDateCreatedMin()));
            }
            if (safeFilter.getDateCreatedMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateCreated"), safeFilter.getDateCreatedMax()));
            }
            if (safeFilter.getCompletedDateMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("completedDate"), safeFilter.getCompletedDateMin()));
            }
            if (safeFilter.getCompletedDateMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("completedDate"), safeFilter.getCompletedDateMax()));
            }
            if (StringUtils.isNotBlank(safeFilter.getParameters())) {
                predicates.add(cb.equal(root.get("parameters"), safeFilter.getParameters()));
            }
            if (safeFilter.getLocationScopeIds() != null && !safeFilter.getLocationScopeIds().isEmpty()) {
                predicates.add(root.get("locationScope").get("locationId").in(safeFilter.getLocationScopeIds()));
            }
            if (StringUtils.isNotBlank(safeFilter.getPrivilegeScope())) {
                predicates.add(cb.equal(root.get("privilegeScope"), safeFilter.getPrivilegeScope()));
            }
            if (!safeFilter.getIncludeVoided()) {
                predicates.add(cb.isFalse(root.get("voided")));
            }
            return predicates;
        });
        jobs.sort(Comparator.comparing(BatchJob::getDateCreated, Comparator.nullsLast(Date::compareTo)).reversed());
        return jobs;
    }

    private BatchJobDTO saveBatchJob(BatchJobDTO dto) {
        Location locationScope = StringUtils.isBlank(dto.getLocationScopeUuid())
                ? null
                : Context.getLocationService().getLocationByUuid(dto.getLocationScopeUuid());
        if (StringUtils.isNotBlank(dto.getLocationScopeUuid()) && locationScope == null) {
            throw new StockManagementException("Batch job location scope not found");
        }

        BatchJobSearchFilter filter = new BatchJobSearchFilter();
        filter.setBatchJobType(dto.getBatchJobType());
        filter.setParameters(dto.getParameters());
        filter.setPrivilegeScope(dto.getPrivilegeScope());
        filter.setBatchJobStatus(List.of(BatchJobStatus.Pending, BatchJobStatus.Running));
        if (locationScope != null) {
            filter.setLocationScopeIds(List.of(locationScope.getId()));
        }

        BatchJob batchJob = first(findBatchJobEntities(filter));
        User currentUser = authenticatedUser();
        Date now = new Date();
        if (batchJob == null) {
            batchJob = new BatchJob();
            batchJob.setCreator(currentUser);
            batchJob.setDateCreated(now);
            batchJob.setBatchJobType(dto.getBatchJobType());
            batchJob.setStatus(BatchJobStatus.Pending);
            batchJob.setDescription(dto.getDescription());
            batchJob.setExpiration(minutesFromNow(GlobalProperties.getBatchJobExpiryInMinutes()));
            batchJob.setParameters(dto.getParameters());
            batchJob.setPrivilegeScope(dto.getPrivilegeScope());
            batchJob.setLocationScope(locationScope);
        }

        if (currentUser != null && (batchJob.getBatchJobOwners() == null
                || batchJob.getBatchJobOwners().stream()
                        .noneMatch(owner -> owner.getOwner() != null
                                && Objects.equals(owner.getOwner().getId(), currentUser.getId())))) {
            BatchJobOwner owner = new BatchJobOwner();
            owner.setOwner(currentUser);
            owner.setDateCreated(now);
            batchJob.addBatchJobOwner(owner);
        }
        return batchJobToDto(saveOrUpdate(batchJob));
    }

    private void failBatchJob(String uuid, String reason) {
        BatchJob job = requireTransitionableBatchJob(uuid);
        if (job == null) {
            return;
        }
        job.setExitMessage(truncate(reason, 2500));
        job.setStatus(BatchJobStatus.Failed);
        saveOrUpdate(job);
    }

    private void cancelBatchJob(String uuid, String reason) {
        BatchJob job = requireTransitionableBatchJob(uuid);
        if (job == null) {
            return;
        }
        job.setStatus(BatchJobStatus.Cancelled);
        job.setCancelReason(truncate(reason, 500));
        job.setCancelledBy(authenticatedUser());
        job.setCancelledDate(new Date());
        saveOrUpdate(job);
    }

    private void expireBatchJob(String uuid, String reason) {
        BatchJob job = byUuid(BatchJob.class, uuid);
        if (job == null) {
            return;
        }
        job.setStatus(BatchJobStatus.Expired);
        if (job.getStartTime() != null) {
            job.setEndTime(new Date());
        }
        job.setExitMessage(truncate(reason, 2500));
        saveOrUpdate(job);
    }

    private BatchJob requireTransitionableBatchJob(String uuid) {
        BatchJob job = byUuid(BatchJob.class, uuid);
        if (job == null) {
            return null;
        }
        if (job.getStatus() != BatchJobStatus.Pending && job.getStatus() != BatchJobStatus.Running) {
            throw new StockManagementException("Batch job is not cancellable");
        }
        return job;
    }

    private BatchJobDTO batchJobToDto(BatchJob job) {
        BatchJobDTO dto = new BatchJobDTO();
        dto.setId(job.getId());
        dto.setUuid(job.getUuid());
        dto.setBatchJobType(job.getBatchJobType());
        dto.setStatus(job.getStatus());
        dto.setDescription(job.getDescription());
        dto.setStartTime(job.getStartTime());
        dto.setEndTime(job.getEndTime());
        dto.setExpiration(job.getExpiration());
        dto.setParameters(job.getParameters());
        dto.setPrivilegeScope(job.getPrivilegeScope());
        dto.setExecutionState(job.getExecutionState());
        dto.setCancelReason(job.getCancelReason());
        dto.setCancelledDate(job.getCancelledDate());
        dto.setExitMessage(job.getExitMessage());
        dto.setCompletedDate(job.getCompletedDate());
        dto.setDateCreated(job.getDateCreated());
        dto.setVoided(Boolean.TRUE.equals(job.getVoided()));
        dto.setOutputArtifactSize(job.getOutputArtifactSize());
        dto.setOutputArtifactFileExt(job.getOutputArtifactFileExt());
        dto.setOutputArtifactViewable(job.getOutputArtifactViewable());
        if (job.getCreator() != null) {
            dto.setCreator(job.getCreator().getId());
            dto.setCreatorUuid(job.getCreator().getUuid());
            dto.setCreatorGivenName(job.getCreator().getGivenName());
            dto.setCreatorFamilyName(job.getCreator().getFamilyName());
        }
        if (job.getCancelledBy() != null) {
            dto.setCancelledBy(job.getCancelledBy().getId());
            dto.setCancelledByUuid(job.getCancelledBy().getUuid());
            dto.setCancelledByGivenName(job.getCancelledBy().getGivenName());
            dto.setCancelledByFamilyName(job.getCancelledBy().getFamilyName());
        }
        if (job.getLocationScope() != null) {
            dto.setLocationScope(job.getLocationScope().getName());
            dto.setLocationScopeId(job.getLocationScope().getId());
            dto.setLocationScopeUuid(job.getLocationScope().getUuid());
        }
        if (job.getBatchJobOwners() != null) {
            dto.setOwners(job.getBatchJobOwners().stream()
                    .map(this::batchJobOwnerToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private BatchJobOwnerDTO batchJobOwnerToDto(BatchJobOwner owner) {
        BatchJobOwnerDTO dto = new BatchJobOwnerDTO();
        dto.setId(owner.getId());
        dto.setUuid(owner.getUuid());
        dto.setDateCreated(owner.getDateCreated());
        if (owner.getBatchJob() != null) {
            dto.setBatchJobId(owner.getBatchJob().getId());
            dto.setBatchJobUuid(owner.getBatchJob().getUuid());
        }
        if (owner.getOwner() != null) {
            dto.setOwnerUserId(owner.getOwner().getId());
            dto.setOwnerUserUuid(owner.getOwner().getUuid());
            dto.setOwnerGivenName(owner.getOwner().getGivenName());
            dto.setOwnerFamilyName(owner.getOwner().getFamilyName());
        }
        return dto;
    }

    private BatchJob getNextActiveBatchJob() {
        List<BatchJob> jobs = query(BatchJob.class, (cb, root) -> predicates(
            cb.equal(root.get("status"), BatchJobStatus.Pending),
            cb.isFalse(root.get("voided"))));
        jobs.sort(Comparator.comparing(BatchJob::getDateCreated, Comparator.nullsLast(Date::compareTo)));
        return first(jobs);
    }

    private void updateBatchJobStatus(String uuid, BatchJobStatus status, String reason) {
        BatchJob job = byUuid(BatchJob.class, uuid);
        if (job == null) {
            return;
        }
        job.setStatus(status);
        if (status == BatchJobStatus.Running && job.getStartTime() == null) {
            job.setStartTime(new Date());
        }
        if (reason != null) {
            job.setExitMessage(truncate(reason, 2500));
        }
        saveOrUpdate(job);
    }

    private void updateBatchJobExecutionState(String uuid, String executionState) {
        BatchJob job = byUuid(BatchJob.class, uuid);
        if (job != null) {
            job.setExecutionState(executionState);
            saveOrUpdate(job);
        }
    }

    private List<BatchJob> getExpiredBatchJobs() {
        Date now = new Date();
        return query(BatchJob.class, (cb, root) -> predicates(
            cb.lessThan(root.get("expiration"), now),
            root.get("status").in(List.of(BatchJobStatus.Pending, BatchJobStatus.Running)),
            cb.isFalse(root.get("voided"))));
    }

    private Map<Integer, Boolean> checkStockBatchHasTransactionsAfterOperation(Integer stockOperationId,
            List<Integer> stockBatchIds) {
        if (stockOperationId == null || stockBatchIds == null || stockBatchIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Integer lastOperationTransactionId = query(StockItemTransaction.class, (cb, root) -> predicates(
            cb.equal(root.get("stockOperation").get("id"), stockOperationId))).stream()
                .map(StockItemTransaction::getId)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
        if (lastOperationTransactionId == null) {
            return new LinkedHashMap<>();
        }
        return query(StockItemTransaction.class, (cb, root) -> {
            Join<StockItemTransaction, StockBatch> batch = root.join("stockBatch", JoinType.INNER);
            return predicates(
                cb.greaterThan(root.get("id"), lastOperationTransactionId),
                batch.get("id").in(stockBatchIds));
        }).stream()
                .map(transaction -> transaction.getStockBatch() == null ? null : transaction.getStockBatch().getId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> Boolean.TRUE, (a, b) -> a, LinkedHashMap::new));
    }

    private void deleteReservedTransations(Integer stockOperationId) {
        if (stockOperationId == null) {
            return;
        }
        for (ReservedTransaction reservedTransaction : query(ReservedTransaction.class, (cb, root) -> predicates(
            cb.equal(root.get("stockOperation").get("id"), stockOperationId)))) {
            remove(reservedTransaction);
        }
    }

    private StockItem getStockItemByReference(StockSource stockSource, String code) {
        if (stockSource == null || StringUtils.isBlank(code)) {
            return null;
        }
        StockItemReference reference = first(query(StockItemReference.class, (cb, root) -> predicates(
            cb.equal(root.get("referenceSource"), stockSource),
            cb.equal(root.get("stockReferenceCode"), code),
            cb.isFalse(root.get("voided")))));
        return reference == null ? null : reference.getStockItem();
    }

    private List<StockItemReference> getStockItemReferenceByStockItem(String uuid) {
        StockItem stockItem = byUuid(StockItem.class, uuid);
        if (stockItem == null) {
            return new ArrayList<>();
        }
        return query(StockItemReference.class, (cb, root) -> predicates(
            cb.equal(root.get("stockItem"), stockItem),
            cb.isFalse(root.get("voided"))));
    }

    private String getUserEmailAddress(User user) {
        if (user == null) {
            return null;
        }
        String email = user.getEmail();
        if (!org.openmrs.module.stockmanagement.api.utils.StringUtils.isValidEmail(email)) {
            email = user.getUserProperty(OpenmrsConstants.USER_PROPERTY_NOTIFICATION_ADDRESS);
        }
        if (!org.openmrs.module.stockmanagement.api.utils.StringUtils.isValidEmail(email)) {
            email = user.getUsername();
        }
        return org.openmrs.module.stockmanagement.api.utils.StringUtils.isValidEmail(email) ? email : null;
    }

    private StockItemDTO stockItemToDto(StockItem item) {
        StockItemDTO dto = new StockItemDTO();
        dto.setId(item.getId());
        dto.setUuid(item.getUuid());
        dto.setVoided(Boolean.TRUE.equals(item.getVoided()));
        dto.setHasExpiration(item.getHasExpiration());
        dto.setCommonName(item.getCommonName());
        dto.setAcronym(item.getAcronym());
        dto.setReorderLevel(item.getReorderLevel());
        dto.setPurchasePrice(item.getPurchasePrice());
        dto.setExpiryNotice(item.getExpiryNotice());
        dto.setCreator(item.getCreator() == null ? null : item.getCreator().getId());
        dto.setDateCreated(item.getDateCreated());
        if (item.getCreator() != null) {
            dto.setCreatorGivenName(item.getCreator().getGivenName());
            dto.setCreatorFamilyName(item.getCreator().getFamilyName());
        }
        if (item.getDrug() != null) {
            dto.setDrugId(item.getDrug().getDrugId());
            dto.setDrugUuid(item.getDrug().getUuid());
            dto.setDrugName(item.getDrug().getName());
            dto.setDrugStrength(item.getDrug().getStrength());
        }
        if (item.getConcept() != null) {
            dto.setConceptId(item.getConcept().getConceptId());
            dto.setConceptUuid(item.getConcept().getUuid());
            dto.setConceptName(conceptName(item.getConcept()));
        }
        if (item.getPreferredVendor() != null) {
            dto.setPreferredVendorId(item.getPreferredVendor().getId());
            dto.setPreferredVendorUuid(item.getPreferredVendor().getUuid());
            dto.setPreferredVendorName(item.getPreferredVendor().getName());
        }
        if (item.getDispensingUnit() != null) {
            dto.setDispensingUnitId(item.getDispensingUnit().getConceptId());
            dto.setDispensingUnitUuid(item.getDispensingUnit().getUuid());
            dto.setDispensingUnitName(conceptName(item.getDispensingUnit()));
        }
        setUomFields(dto, item.getPurchasePriceUoM(), "purchase");
        setUomFields(dto, item.getDispensingUnitPackagingUoM(), "dispensing");
        setUomFields(dto, item.getDefaultStockOperationsUoM(), "default");
        setUomFields(dto, item.getReorderLevelUOM(), "reorder");
        if (item.getCategory() != null) {
            dto.setCategoryId(item.getCategory().getConceptId());
            dto.setCategoryUuid(item.getCategory().getUuid());
            dto.setCategoryName(conceptName(item.getCategory()));
        }
        if (item.getReferences() != null) {
            dto.setStockItemReferences(new ArrayList<>(item.getReferences()));
        }
        return dto;
    }

    private void setUomFields(StockItemDTO dto, StockItemPackagingUOM uom, String target) {
        if (uom == null) {
            return;
        }
        Concept concept = uom.getPackagingUom();
        if ("purchase".equals(target)) {
            dto.setPurchasePriceUoMId(uom.getId());
            dto.setPurchasePriceUoMUuid(uom.getUuid());
            dto.setPurchasePriceUoMFactor(uom.getFactor());
            if (concept != null) {
                dto.setPurchasePriceConceptId(concept.getConceptId());
                dto.setPurchasePriceUoMName(conceptName(concept));
            }
        } else if ("dispensing".equals(target)) {
            dto.setDispensingUnitPackagingUoMId(uom.getId());
            dto.setDispensingUnitPackagingUoMUuid(uom.getUuid());
            dto.setDispensingUnitPackagingUoMFactor(uom.getFactor());
            if (concept != null) {
                dto.setDispensingUnitPackagingConceptId(concept.getConceptId());
                dto.setDispensingUnitPackagingUoMName(conceptName(concept));
            }
        } else if ("default".equals(target)) {
            dto.setDefaultStockOperationsUoMId(uom.getId());
            dto.setDefaultStockOperationsUoMUuid(uom.getUuid());
            dto.setDefaultStockOperationsUoMFactor(uom.getFactor());
            if (concept != null) {
                dto.setDefaultStockOperationsConceptId(concept.getConceptId());
                dto.setDefaultStockOperationsUoMName(conceptName(concept));
            }
        } else if ("reorder".equals(target)) {
            dto.setReorderLevelUoMId(uom.getId());
            dto.setReorderLevelUoMUuid(uom.getUuid());
            dto.setReorderLevelUoMFactor(uom.getFactor());
            if (concept != null) {
                dto.setReorderLevelConceptId(concept.getConceptId());
                dto.setReorderLevelUoMName(conceptName(concept));
            }
        }
    }

    private String stockItemName(StockItem stockItem) {
        if (StringUtils.isNotBlank(stockItem.getCommonName())) {
            return stockItem.getCommonName();
        }
        if (stockItem.getDrug() != null) {
            return stockItem.getDrug().getName();
        }
        return stockItem.getConcept() == null ? null : conceptName(stockItem.getConcept());
    }

    private String conceptName(Concept concept) {
        if (concept == null) {
            return null;
        }
        ConceptName name = concept.getName(Context.getLocale());
        if (name == null) {
            name = concept.getName();
        }
        return name == null ? null : name.getName();
    }

    private <T> T byUuid(Class<T> type, String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return first(query(type, (cb, root) -> predicates(cb.equal(root.get("uuid"), uuid))));
    }

    private <T> T requireByUuid(Class<T> type, String uuid, String message) {
        T value = byUuid(type, uuid);
        if (value == null) {
            throw new StockManagementException(message);
        }
        return value;
    }

    private <T> T firstByEntity(Class<T> type, String property, Object value) {
        if (value == null) {
            return null;
        }
        return first(query(type, (cb, root) -> predicates(cb.equal(root.get(property), value))));
    }

    private <T> List<T> listByIds(Class<T> type, String property, Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return query(type, (cb, root) -> predicates(root.get(property).in(ids)));
    }

    private <T> List<T> query(Class<T> type, BiFunction<CriteriaBuilder, Root<T>, List<Predicate>> predicateFactory) {
        CriteriaBuilder cb = session().getCriteriaBuilder();
        CriteriaQuery<T> criteria = cb.createQuery(type);
        Root<T> root = criteria.from(type);
        List<Predicate> predicates = predicateFactory.apply(cb, root);
        if (predicates != null && !predicates.isEmpty()) {
            criteria.where(predicates.toArray(new Predicate[0]));
        }
        criteria.distinct(true);
        return session().createQuery(criteria).getResultList();
    }

    private List<Predicate> predicates(Predicate... predicates) {
        return new ArrayList<>(List.of(predicates));
    }

    private <T> Result<T> resultFromList(List<T> data, Integer startIndex, Integer limit) {
        Result<T> result = new Result<>();
        List<T> safeData = data == null ? new ArrayList<>() : data;
        if (limit != null) {
            int fromIndex = startIndex == null ? 0 : Math.max(0, startIndex);
            int toIndex = Math.min(safeData.size(), fromIndex + Math.max(0, limit));
            result.setPageIndex(fromIndex);
            result.setPageSize(limit);
            result.setData(fromIndex >= safeData.size() ? new ArrayList<>() : new ArrayList<>(safeData.subList(fromIndex, toIndex)));
        } else {
            result.setData(new ArrayList<>(safeData));
        }
        result.setTotalRecordCount((long) safeData.size());
        return result;
    }

    private <T> T saveOrUpdate(T entity) {
        touch(entity);
        return HibernateUtil.saveOrUpdate(session(), entity);
    }

    private void remove(Object entity) {
        if (entity == null) {
            return;
        }
        Object target = entity;
        if (entity instanceof BaseOpenmrsObject && ((BaseOpenmrsObject) entity).getId() != null) {
            target = session().find(entity.getClass(), ((BaseOpenmrsObject) entity).getId());
        }
        if (target != null) {
            session().remove(target);
        }
    }

    private void touch(Object entity) {
        if (!(entity instanceof BaseOpenmrsData)) {
            return;
        }
        BaseOpenmrsData data = (BaseOpenmrsData) entity;
        User user = authenticatedUser();
        Date now = new Date();
        if (data.getId() == null) {
            if (data.getCreator() == null) {
                data.setCreator(user);
            }
            if (data.getDateCreated() == null) {
                data.setDateCreated(now);
            }
        } else {
            data.setChangedBy(user);
            data.setDateChanged(now);
        }
    }

    private <T> void voidByUuids(Class<T> type, List<String> uuids, String reason, Integer voidedBy) {
        if (uuids == null) {
            return;
        }
        for (String uuid : uuids) {
            voidByUuid(type, uuid, reason, voidedBy);
        }
    }

    private <T> void voidByUuid(Class<T> type, String uuid, String reason, Integer voidedBy) {
        Object entity = byUuid(type, uuid);
        if (!(entity instanceof BaseOpenmrsData)) {
            return;
        }
        BaseOpenmrsData data = (BaseOpenmrsData) entity;
        data.setVoided(true);
        data.setDateVoided(new Date());
        data.setVoidReason(reason);
        data.setVoidedBy(voidedBy == null ? authenticatedUser() : session().find(User.class, voidedBy));
        HibernateUtil.saveOrUpdate(session(), data);
    }

    private User authenticatedUser() {
        try {
            return Context.getAuthenticatedUser();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private Date minutesFromNow(int minutes) {
        return addMinutes(new Date(), minutes);
    }

    private Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    private int boundedMinutes(Long minutes) {
        if (minutes < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (minutes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.toIntExact(minutes);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private <T> List<T> sortedById(List<T> values) {
        values.sort(Comparator.comparing(this::objectId, Comparator.nullsLast(Integer::compareTo)));
        return values;
    }

    private Integer objectId(Object object) {
        return object instanceof BaseOpenmrsObject ? ((BaseOpenmrsObject) object).getId() : null;
    }
}
