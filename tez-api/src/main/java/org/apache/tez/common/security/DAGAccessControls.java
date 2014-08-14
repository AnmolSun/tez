/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tez.common.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.conf.Configuration;
import org.apache.tez.dag.api.TezConfiguration;

/**
 * Access controls for the DAG
 */
public class DAGAccessControls {

  private final Set<String> usersWithViewACLs;
  private final Set<String> usersWithModifyACLs;
  private final Set<String> groupsWithViewACLs;
  private final Set<String> groupsWithModifyACLs;

  public DAGAccessControls() {
    this.usersWithViewACLs = new HashSet<String>();
    this.usersWithModifyACLs = new HashSet<String>();
    this.groupsWithViewACLs = new HashSet<String>();
    this.groupsWithModifyACLs = new HashSet<String>();
  }

  /**
   * Helper API to support YARN and MapReduce format for specifying users and groups.
   * Format supports a comma-separated list of users and groups with the users and groups separated
   * by whitespace. e.g. "user1,user2 group1,group2"
   * If the value specified is "*", all users are allowed to do the operation.
   * @param viewACLsStr
   * @param modifyACLsStr
   */
  public DAGAccessControls(String viewACLsStr, String modifyACLsStr) {
    final Configuration conf = new Configuration(false);
    conf.set(TezConfiguration.TEZ_DAG_VIEW_ACLS, (viewACLsStr != null ? viewACLsStr : ""));
    conf.set(TezConfiguration.TEZ_DAG_MODIFY_ACLS, (modifyACLsStr != null ? modifyACLsStr : ""));
    ACLConfigurationParser parser = new ACLConfigurationParser(conf, true);

    this.usersWithViewACLs = new HashSet<String>();
    this.usersWithModifyACLs = new HashSet<String>();
    this.groupsWithViewACLs = new HashSet<String>();
    this.groupsWithModifyACLs = new HashSet<String>();

    Map<ACLType, Set<String>> allowedUsers = parser.getAllowedUsers();
    Map<ACLType, Set<String>> allowedGroups = parser.getAllowedGroups();

    if (allowedUsers.containsKey(ACLType.DAG_VIEW_ACL)) {
      this.usersWithViewACLs.addAll(allowedUsers.get(ACLType.DAG_VIEW_ACL));
    }
    if (allowedUsers.containsKey(ACLType.DAG_MODIFY_ACL)) {
      this.usersWithModifyACLs.addAll(allowedUsers.get(ACLType.DAG_MODIFY_ACL));
    }
    if (allowedGroups.containsKey(ACLType.DAG_VIEW_ACL)) {
      this.groupsWithViewACLs.addAll(allowedGroups.get(ACLType.DAG_VIEW_ACL));
    }
    if (allowedGroups.containsKey(ACLType.DAG_MODIFY_ACL)) {
      this.groupsWithModifyACLs.addAll(allowedGroups.get(ACLType.DAG_MODIFY_ACL));
    }

  }

  /**
   * Set the list of users that view permissions on the DAG. If all users are allowed,
   * pass in a single entry "*"
   * @param users Set of users with view permissions
   * @return
   */
  public synchronized DAGAccessControls setUsersWithViewACLs(Collection<String> users) {
    this.usersWithViewACLs.clear();
    this.usersWithViewACLs.addAll(users);
    return this;
  }

  /**
   * Set the list of users that have modify permissions on the DAG. If all users are allowed,
   * pass in a single entry "*"
   * @param users Set of users with modify permissions
   * @return
   */
  public synchronized DAGAccessControls setUsersWithModifyACLs(Collection<String> users) {
    this.usersWithModifyACLs.clear();
    this.usersWithModifyACLs.addAll(users);
    return this;
  }

  /**
   * Set the list of groups that have view permissions on the DAG.
   * @param groups Set of groups with view permissions
   * @return
   */
  public synchronized DAGAccessControls setGroupsWithViewACLs(Collection<String> groups) {
    this.groupsWithViewACLs.clear();
    this.groupsWithViewACLs.addAll(groups);
    return this;
  }

  /**
   * Set the list of groups that have modify permissions on the DAG.
   * @param groups Set of groups with modify permissions
   * @return
   */
  public synchronized DAGAccessControls setGroupsWithModifyACLs(Collection<String> groups) {
    this.groupsWithModifyACLs.clear();
    this.groupsWithModifyACLs.addAll(groups);
    return this;
  }

  @Private
  public Set<String> getUsersWithViewACLs() {
    return Collections.unmodifiableSet(usersWithViewACLs);
  }

  @Private
  public Set<String> getUsersWithModifyACLs() {
    return Collections.unmodifiableSet(usersWithModifyACLs);
  }

  @Private
  public Set<String> getGroupsWithViewACLs() {
    return Collections.unmodifiableSet(groupsWithViewACLs);
  }

  @Private
  public Set<String> getGroupsWithModifyACLs() {
    return Collections.unmodifiableSet(groupsWithModifyACLs);
  }

  @Private
  public void serializeToConfiguration(Configuration conf) {
    if (usersWithViewACLs.contains(ACLManager.WILDCARD_ACL_VALUE)) {
      conf.set(TezConfiguration.TEZ_DAG_VIEW_ACLS, ACLManager.WILDCARD_ACL_VALUE);
    } else {
      String userList = ACLManager.toCommaSeparatedString(usersWithViewACLs);
      String groupList = ACLManager.toCommaSeparatedString(groupsWithViewACLs);
      conf.set(TezConfiguration.TEZ_DAG_VIEW_ACLS,
          userList + " " + groupList);
    }
    if (usersWithModifyACLs.contains(ACLManager.WILDCARD_ACL_VALUE)) {
      conf.set(TezConfiguration.TEZ_DAG_MODIFY_ACLS, ACLManager.WILDCARD_ACL_VALUE);
    } else {
      String userList = ACLManager.toCommaSeparatedString(usersWithModifyACLs);
      String groupList = ACLManager.toCommaSeparatedString(groupsWithModifyACLs);
      conf.set(TezConfiguration.TEZ_DAG_MODIFY_ACLS,
          userList + " " + groupList);
    }
  }

}