/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.scheduler.jobrunr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.User;
import org.openmrs.scheduler.TaskState;

class JobRunrSchedulerServiceTest {

  private final JobRunrSchedulerService service;

  JobRunrSchedulerServiceTest() {
    service = new JobRunrSchedulerService(null, null, null, null, "admin");
  }

  @BeforeEach
  void setUp() {
    setAuthenticatedUser(null);
  }

  @AfterEach
  void tearDown() {
    setAuthenticatedUser(null);
  }

  @Test
  void getTasksReturnsEmptyStreamWhenUnauthenticated() {
    List<?> tasks = service.getTasks(TaskState.SCHEDULED, Instant.now()).toList();

    assertTrue(tasks.isEmpty());
  }

  @Test
  void getRecurringTasksReturnsEmptyStreamWhenUnauthenticated() {
    List<?> tasks = service.getRecurringTasks().toList();

    assertTrue(tasks.isEmpty());
  }

  @Test
  void hasPrivilegesIsSafeWhenUnauthenticated() {
    assertFalse(service.hasPrivileges(null));
  }

  @Test
  void isSchedulerManagerIsSafeWhenUserIsNull() {
    assertFalse(service.isSchedulerManager(null));
  }

  private static void setAuthenticatedUser(User user) {
    UserContext userContext = new UserContext(credentials -> null);
    try {
      Field userField = UserContext.class.getDeclaredField("user");
      userField.setAccessible(true);
      userField.set(userContext, user);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
    Context.setUserContext(userContext);
  }
}
