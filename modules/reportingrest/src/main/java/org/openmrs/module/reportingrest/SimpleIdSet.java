/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.reportingrest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.reporting.query.IdSet;

/** A simple implementation of IdSet that can be safely serialized by Jackson or XStream. */
public class SimpleIdSet implements IdSet<OpenmrsObject> {

  @JsonProperty private Set<Integer> memberIds;

  public SimpleIdSet() {
    memberIds = new HashSet<Integer>();
  }

  public SimpleIdSet(Set<Integer> memberIds) {
    this.memberIds = memberIds;
  }

  @Override
  public Set<Integer> getMemberIds() {
    return memberIds;
  }

  @Override
  public boolean contains(Integer id) {
    return memberIds.contains(id);
  }

  @Override
  public int getSize() {
    return memberIds.size();
  }

  @Override
  public boolean isEmpty() {
    return memberIds.isEmpty();
  }

  @Override
  public IdSet<OpenmrsObject> clone() {
    return new SimpleIdSet(new HashSet<Integer>(memberIds));
  }

  @Override
  public void retainAll(IdSet<OpenmrsObject> idSet) {
    memberIds.retainAll(idSet.getMemberIds());
  }

  @Override
  public void removeAll(IdSet<OpenmrsObject> idSet) {
    memberIds.removeAll(idSet.getMemberIds());
  }

  @Override
  public void addAll(IdSet<OpenmrsObject> idSet) {
    memberIds.addAll(idSet.getMemberIds());
  }

  @Override
  public void setMemberIds(Set<Integer> set) {
    memberIds = set;
  }
}
