/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.reporting.report.task;

import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.DaemonToken;
import org.sihsalus.core.api.StaticModuleTaskRunner;

/** Generic superclass for a Reports task */
public class ReportingTimerTask extends TimerTask {

  private final Log log = LogFactory.getLog(getClass());
  private static DaemonToken daemonToken;
  private static boolean enabled = false;
  private ReportingTask task;

  // ***** PROPERTIES THAT NEED TO BE SET ON EACH INSTANCE

  private Class<? extends ReportingTask> taskClass;
  private DbSessionFactory sessionFactory;

  /**
   * @see TimerTask#run()
   */
  @Override
  public final void run() {
    if (enabled) {
      createAndRunTask();
    } else {
      log.debug("Not running scheduled task. enabled = " + enabled);
    }
  }

  /** Construct a new instance of the configured task and execute it */
  public void createAndRunTask() {
    try {
      log.info("Running reporting task: " + getTaskClass().getSimpleName());
      task = getTaskClass().getDeclaredConstructor().newInstance();
      task.setScheduledExecutionTime(System.currentTimeMillis());
      task.setSessionFactory(sessionFactory);
      StaticModuleTaskRunner.runInBackground(daemonToken, task);
    } catch (Exception e) {
      log.error("An error occurred while running scheduled reporting task", e);
    }
  }

  @Override
  public boolean cancel() {
    boolean ret = super.cancel();
    if (task != null) {
      task.cancelTask();
    }
    return ret;
  }

  public Class<? extends ReportingTask> getTaskClass() {
    return taskClass;
  }

  public void setTaskClass(Class<? extends ReportingTask> taskClass) {
    this.taskClass = taskClass;
  }

  public DbSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void setSessionFactory(DbSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  /** Sets the daemon token */
  public static void setDaemonToken(DaemonToken token) {
    daemonToken = token;
  }

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean enabled) {
    ReportingTimerTask.enabled = enabled;
  }
}
