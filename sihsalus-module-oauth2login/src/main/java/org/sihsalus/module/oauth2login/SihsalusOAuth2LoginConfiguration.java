package org.sihsalus.module.oauth2login;

import static org.openmrs.module.oauth2login.OAuth2LoginConstants.AUTH_SCHEME_COMPONENT;
import static org.openmrs.module.oauth2login.OAuth2LoginConstants.OAUTH2_SCHEME_ID;
import static org.openmrs.module.oauth2login.OAuth2LoginConstants.OAUTH_PROP_BEAN_NAME;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.openmrs.api.UserService;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.openmrs.module.oauth2login.PropertyUtils;
import org.openmrs.module.oauth2login.authscheme.AuthenticationPostProcessor;
import org.openmrs.module.oauth2login.authscheme.OAuth2TokenCredentials;
import org.openmrs.module.oauth2login.authscheme.OAuth2UserInfoAuthenticationScheme;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SihsalusOAuth2LoginConfiguration {

    @Bean(name = OAUTH_PROP_BEAN_NAME)
    Properties oauth2Properties() {
        Path propertiesPath = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), "oauth2.properties");
        if (!Files.exists(propertiesPath)) {
            return new Properties();
        }
        try {
            return PropertyUtils.getProperties(propertiesPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load OAuth2 properties from " + propertiesPath, exception);
        }
    }

    @Bean(name = AUTH_SCHEME_COMPONENT)
    Object oauth2UserInfoAuthenticationScheme(
            UserService userService, ObjectProvider<AuthenticationPostProcessor> postProcessor) {
        OAuth2UserInfoAuthenticationScheme scheme = new OAuth2UserInfoAuthenticationScheme();
        scheme.setUserService(userService);
        postProcessor.ifAvailable(scheme::setPostProcessor);
        return scheme;
    }

    @Bean
    SmartInitializingSingleton oauth2AuthenticationSchemeRegistration(
            @Qualifier(AUTH_SCHEME_COMPONENT) Object oauth2UserInfoAuthenticationScheme) {
        OAuth2UserInfoAuthenticationScheme scheme = (OAuth2UserInfoAuthenticationScheme) oauth2UserInfoAuthenticationScheme;
        return () -> {
            AuthenticationConfig.registerClassLoader(getClass().getClassLoader());
            AuthenticationConfig.registerAuthenticationScheme(OAUTH2_SCHEME_ID, scheme);
            AuthenticationConfig.registerAuthenticationScheme(OAuth2TokenCredentials.SCHEME_NAME, scheme);
        };
    }
}
