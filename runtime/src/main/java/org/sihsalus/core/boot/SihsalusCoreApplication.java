package org.sihsalus.core.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = SihsalusRuntimeProperties.class)
@ComponentScan(basePackages = "org.sihsalus")
public class SihsalusCoreApplication {

  public static void main(String[] args) {
    SihsalusBootstrapProbeServer bootstrapProbe = SihsalusBootstrapProbeServer.start();
    try {
      ConfigurableApplicationContext context =
          SpringApplication.run(SihsalusCoreApplication.class, args);
      bootstrapProbe.markReady();
      context.addApplicationListener((ContextClosedEvent event) -> bootstrapProbe.close());
    } catch (RuntimeException | Error ex) {
      bootstrapProbe.markFailed(ex);
      bootstrapProbe.close();
      throw ex;
    }
  }
}
