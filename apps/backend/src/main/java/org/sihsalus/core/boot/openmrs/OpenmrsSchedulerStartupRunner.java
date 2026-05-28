package org.sihsalus.core.boot.openmrs;

import org.openmrs.scheduler.SchedulerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
final class OpenmrsSchedulerStartupRunner implements ApplicationRunner {

  private final SchedulerService schedulerService;

  private final boolean startupEnabled;

  OpenmrsSchedulerStartupRunner(
      SchedulerService schedulerService,
      @Value("${sihsalus.scheduler.startup.enabled:true}") boolean startupEnabled) {
    this.schedulerService = schedulerService;
    this.startupEnabled = startupEnabled;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (startupEnabled) {
      schedulerService.onStartup();
    }
  }
}
