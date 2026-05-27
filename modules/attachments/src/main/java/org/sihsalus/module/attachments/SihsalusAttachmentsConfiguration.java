package org.sihsalus.module.attachments;

import java.util.Map;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.module.attachments.AttachmentsContext;
import org.openmrs.module.attachments.AttachmentsService;
import org.openmrs.module.attachments.AttachmentsServiceImpl;
import org.openmrs.module.attachments.obs.DefaultAttachmentHandler;
import org.openmrs.module.attachments.obs.ImageAttachmentHandler;
import org.openmrs.module.attachments.rest.AttachmentBytesResource;
import org.openmrs.obs.ComplexObsHandler;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {AttachmentsContext.class, AttachmentBytesResource.class})
public class SihsalusAttachmentsConfiguration {

  @Bean
  AttachmentsService attachmentsService() {
    return new AttachmentsServiceImpl();
  }

  @Bean
  SmartInitializingSingleton attachmentsServiceRegistrar(
      ServiceContext serviceContext, AttachmentsService attachmentsService) {
    return () -> serviceContext.setService(AttachmentsService.class, attachmentsService);
  }

  @Bean
  DefaultAttachmentHandler defaultAttachmentHandler() {
    return new DefaultAttachmentHandler();
  }

  @Bean
  ImageAttachmentHandler imageAttachmentHandler() {
    return new ImageAttachmentHandler();
  }

  @Bean
  SmartInitializingSingleton attachmentsComplexObsHandlerRegistrar(
      @Qualifier("handlers") Map<String, ComplexObsHandler> handlers,
      DefaultAttachmentHandler defaultAttachmentHandler,
      ImageAttachmentHandler imageAttachmentHandler) {
    return () -> {
      handlers.put(DefaultAttachmentHandler.class.getSimpleName(), defaultAttachmentHandler);
      handlers.put(ImageAttachmentHandler.class.getSimpleName(), imageAttachmentHandler);
    };
  }
}
