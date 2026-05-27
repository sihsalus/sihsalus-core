/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.oauth2login.authscheme;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Authenticated;
import org.openmrs.api.context.BasicAuthenticated;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Credentials;
import org.openmrs.api.context.DaoAuthenticationScheme;
import org.springframework.transaction.annotation.Transactional;

/** A scheme that authenticates with OpenMRS based on the 'username'. */
@Transactional
public class OAuth2UserInfoAuthenticationScheme extends DaoAuthenticationScheme {

  protected Log log = LogFactory.getLog(getClass());

  private AuthenticationPostProcessor postProcessor;

  private UserService userService;

  public void setPostProcessor(AuthenticationPostProcessor postProcessor) {
    this.postProcessor = postProcessor;
  }

  public void setUserService(UserService userService) {
    this.userService = userService;
  }

  public OAuth2UserInfoAuthenticationScheme() {
    setPostProcessor(
        new AuthenticationPostProcessor() {

          @Override
          public void process(UserInfo userInfo) {
            // no post-processing by default
          }
        });
  }

  @Override
  public Authenticated authenticate(Credentials credentials) throws ContextAuthenticationException {

    OAuth2TokenCredentials creds;
    try {
      creds = (OAuth2TokenCredentials) credentials;
    } catch (ClassCastException e) {
      throw new ContextAuthenticationException(
          "The credentials provided did not match those needed for the "
              + getClass().getSimpleName()
              + " authentication scheme.",
          e);
    }

    String clientName = credentials.getClientName();
    if (StringUtils.isBlank(clientName)) {
      throw new ContextAuthenticationException(
          "OAuth2 credentials did not resolve an OpenMRS username.");
    }

    User user = getContextDAO().getUserByUsername(clientName);
    if (!creds.isServiceAccount()) {
      if (user == null) {
        createUser(creds.getUserInfo());
        // Get the user again after the user has been created
        user = getContextDAO().getUserByUsername(clientName);
      } else {
        updateUser(creds.getUserInfo());
      }

      postProcessor.process(creds.getUserInfo());
    }
    if (user == null) {
      throw new ContextAuthenticationException(
          "No OpenMRS user found for OAuth2 client: " + clientName);
    }
    return new BasicAuthenticated(user, credentials.getAuthenticationScheme());
  }

  private void createUser(UserInfo userInfo) throws ContextAuthenticationException {
    try {
      User user = userInfo.getOpenmrsUser("n/a");
      String password = RandomStringUtils.secure().nextAlphanumeric(100);
      getContextDAO().createUser(user, password, userInfo.getRoleNames());
    } catch (Exception e) {
      throw new ContextAuthenticationException(e.getMessage(), e);
    }
  }

  private void updateUser(UserInfo userInfo) {
    try {
      UpdateUserTask task = new UpdateUserTask(userService, userInfo);
      task.run();
    } catch (Exception e) {
      throw new ContextAuthenticationException(e.getMessage(), e);
    }
  }
}
