package org.sihsalus.core.api;

import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.ModuleFactory;
import org.openmrs.util.OpenmrsThreadPoolHolder;
import org.openmrs.util.PrivilegeConstants;

/**
 * Executes legacy module background work when the OMOD runtime is not present to issue daemon tokens.
 */
public final class StaticModuleTaskRunner {

    private StaticModuleTaskRunner() {
    }

    public static boolean hasValidDaemonToken(DaemonToken daemonToken) {
        return ModuleFactory.isTokenValid(daemonToken);
    }

    public static Future<?> runInBackground(DaemonToken daemonToken, Runnable task) {
        if (hasValidDaemonToken(daemonToken)) {
            return Daemon.runInDaemonThreadWithoutResult(task, daemonToken);
        }
        return OpenmrsThreadPoolHolder.threadExecutor.submit(() -> runAuthenticated(task));
    }

    public static void runAndWait(DaemonToken daemonToken, Runnable task) {
        if (hasValidDaemonToken(daemonToken)) {
            Daemon.runInDaemonThreadAndWait(task, daemonToken);
            return;
        }
        runAuthenticated(task);
    }

    public static void runAuthenticated(Runnable task) {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }

        UserContext originalUserContext = openedSession ? null : Context.getUserContext();
        boolean authenticatedForTask = !Context.isAuthenticated();
        try {
            if (authenticatedForTask) {
                Context.setUserContext(new UserContext(Context.getAuthenticationScheme()));
                authenticateSchedulerUser();
            }
            task.run();
        } finally {
            if (authenticatedForTask && Context.isSessionOpen()) {
                Context.logout();
            }
            if (authenticatedForTask && originalUserContext != null) {
                Context.setUserContext(originalUserContext);
            }
            if (openedSession && Context.isSessionOpen()) {
                Context.closeSession();
            }
        }
    }

    private static void authenticateSchedulerUser() {
        AdministrationService administrationService = Context.getAdministrationService();
        String username;
        String password;

        Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        try {
            username = administrationService.getGlobalProperty("scheduler.username");
            password = administrationService.getGlobalProperty("scheduler.password");
        } finally {
            Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
        }

        if (!StringUtils.isNotBlank(username) || !StringUtils.isNotBlank(password)) {
            throw new APIAuthenticationException(
                    "Static module background tasks require scheduler.username and scheduler.password global properties");
        }

        Context.authenticate(username, password);
    }
}
