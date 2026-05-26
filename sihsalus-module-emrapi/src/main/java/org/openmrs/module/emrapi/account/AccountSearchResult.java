/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.emrapi.account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountSearchResult {

  private Long totalCount;

  private List<AccountDomainWrapper> accounts = new ArrayList<>();

  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }

  public List<AccountDomainWrapper> getAccounts() {
    return Collections.unmodifiableList(accounts);
  }

  public void setAccounts(List<AccountDomainWrapper> accounts) {
    this.accounts = accounts == null ? null : new ArrayList<>(accounts);
  }

  public void addAccount(AccountDomainWrapper account) {
    accounts.add(account);
  }
}
