package org.sihsalus.initializer;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.openmrs.messagesource.impl.MutableResourceBundleMessageSource;
import org.springframework.beans.factory.SmartInitializingSingleton;

final class SihsalusMessagePropertiesRegistrar implements SmartInitializingSingleton {

  private static final String MESSAGE_PROPERTIES_DOMAIN = "messageproperties";

  private final MutableResourceBundleMessageSource messageSource;

  SihsalusMessagePropertiesRegistrar(MutableResourceBundleMessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Override
  public void afterSingletonsInstantiated() {
    try {
      Path configRoot = SihsalusContentPaths.resolveConfigRoot();
      if (configRoot == null) {
        return;
      }

      Path directory =
          SihsalusContentPaths.resolveDomainDirectory(configRoot, MESSAGE_PROPERTIES_DOMAIN);
      if (directory == null || !hasMessagesFile(directory)) {
        return;
      }

      Path basename = directory.resolve("messages").normalize();
      if (!basename.startsWith(directory)) {
        throw new IllegalStateException("Message properties basename escapes content directory.");
      }

      messageSource.setBasenames(
          basename.toUri().toString(), "classpath:custom_messages", "classpath:messages");
      messageSource.clearCache();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to register SIH Salus message properties.", e);
    }
  }

  private boolean hasMessagesFile(Path directory) throws Exception {
    try (Stream<Path> stream = Files.list(directory)) {
      return stream
          .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
          .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
          .anyMatch(
              fileName -> fileName.startsWith("messages") && fileName.endsWith(".properties"));
    }
  }
}
