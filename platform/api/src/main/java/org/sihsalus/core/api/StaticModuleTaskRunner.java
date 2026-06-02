package org.sihsalus.core.api;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.context.UsernamePasswordCredentials;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.ModuleFactory;
import org.openmrs.util.OpenmrsThreadPoolHolder;
import org.openmrs.util.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes legacy module background work when the OMOD runtime is not present to issue daemon
 * tokens.
 */
public final class StaticModuleTaskRunner {

  private static final Logger log = LoggerFactory.getLogger(StaticModuleTaskRunner.class);

  private StaticModuleTaskRunner() {}

  public static boolean hasValidDaemonToken(DaemonToken daemonToken) {
    return ModuleFactory.isTokenValid(daemonToken);
  }

  public static Future<?> runInBackground(DaemonToken daemonToken, Runnable task) {
    Runnable loggedTask = withFailureLogging(task);
    if (hasValidDaemonToken(daemonToken)) {
      return Daemon.runInDaemonThreadWithoutResult(loggedTask, daemonToken);
    }
    return OpenmrsThreadPoolHolder.threadExecutor.submit(() -> runAuthenticated(loggedTask));
  }

  public static void runAndWait(DaemonToken daemonToken, Runnable task) {
    if (hasValidDaemonToken(daemonToken)) {
      waitFor(Daemon.runInDaemonThreadWithoutResult(task, daemonToken));
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
          "Static module background tasks require scheduler.username and scheduler.password global"
              + " properties");
    }

    Context.authenticate(new UsernamePasswordCredentials(username, password));
  }

  private static Runnable withFailureLogging(Runnable task) {
    return () -> {
      try {
        task.run();
      } catch (RuntimeException | Error e) {
        log.error("Static module background task failed", e);
        throw e;
      }
    };
  }

  private static void waitFor(Future<?> future) {
    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for static module task", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException("Static module task failed", cause);
    }
  }
}
