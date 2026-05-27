package org.openmrs.module.idgen.task;

import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Generic superclass for an Idgen task */
public abstract class IdgenTask extends TimerTask {

  private final Log log = LogFactory.getLog(getClass());
  private static boolean enabled = false;

  /**
   * @see TimerTask#run()
   */
  @Override
  public final void run() {
    if (enabled) {
      createAndRunTask();
    } else {
      log.warn("Not running scheduled task. enabled = " + enabled);
    }
  }

  /** Construct a new instance of the configured task and execute it */
  public synchronized void createAndRunTask() {
    try {
      log.info("Running idgen task: " + getClass().getSimpleName());
      getRunnableTask().run();
    } catch (Exception e) {
      log.error("An error occurred while running scheduled idgen task", e);
    }
  }

  public abstract Runnable getRunnableTask();

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean enabled) {
    IdgenTask.enabled = enabled;
  }
}
