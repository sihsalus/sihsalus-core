/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license. Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the
 * OpenMRS graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.event;

import lombok.Setter;
import org.openmrs.module.DaemonToken;
import org.sihsalus.core.api.StaticModuleTaskRunner;
import org.springframework.context.ApplicationListener;

/**
 * Intended as a superclass for all Application Listeners of Transaction Events Enables a single
 * Listener to support more than one type of transaction event, and ensures that all events that
 * occur after a transaction has completed are processed in a separate Daemon thread
 */
public abstract class TransactionEventListener implements ApplicationListener<TransactionEvent> {

  @Setter private static DaemonToken daemonToken = null;

  @Override
  public final void onApplicationEvent(TransactionEvent transactionEvent) {
    if (transactionEvent.getEvents() != null && !transactionEvent.getEvents().isEmpty()) {
      if (transactionEvent instanceof TransactionAfterBeginEvent) {
        afterTransactionBegin((TransactionAfterBeginEvent) transactionEvent);
      } else if (transactionEvent instanceof TransactionBeforeCompletionEvent) {
        beforeTransactionCompletion((TransactionBeforeCompletionEvent) transactionEvent);
      } else if (transactionEvent instanceof TransactionCommittedEvent) {
        runAfterCompletion(
            () -> transactionCommitted((TransactionCommittedEvent) transactionEvent));
      } else if (transactionEvent instanceof TransactionNotCommittedEvent) {
        runAfterCompletion(
            () -> transactionNotCommitted((TransactionNotCommittedEvent) transactionEvent));
      }
      transactionEvent(transactionEvent);
    }
  }

  private void runAfterCompletion(Runnable runnable) {
    StaticModuleTaskRunner.runAndWait(daemonToken, runnable);
  }

  public void afterTransactionBegin(TransactionAfterBeginEvent transactionEvent) {
    ignore(transactionEvent);
  }

  public void beforeTransactionCompletion(TransactionBeforeCompletionEvent transactionEvent) {
    ignore(transactionEvent);
  }

  public void transactionCommitted(TransactionCommittedEvent transactionEvent) {
    ignore(transactionEvent);
  }

  public void transactionNotCommitted(TransactionNotCommittedEvent transactionEvent) {
    ignore(transactionEvent);
  }

  public void transactionEvent(TransactionEvent transactionEvent) {
    ignore(transactionEvent);
  }

  private static void ignore(Object value) {
    if (value != null) {
      value.hashCode();
    }
  }
}
