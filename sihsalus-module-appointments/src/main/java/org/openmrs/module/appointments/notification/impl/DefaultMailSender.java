package org.openmrs.module.appointments.notification.impl;

import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.module.appointments.notification.MailSender;
import org.openmrs.util.OpenmrsUtil;

public class DefaultMailSender implements MailSender {
  private static final String EMAIL_PROPERTIES_FILENAME = "mail-config.properties";
  private Log log = LogFactory.getLog(this.getClass());
  private volatile Session session = null;

  private AdministrationService administrationService;

  public DefaultMailSender(AdministrationService administrationService) {
    this.administrationService = administrationService;
  }

  @Override
  public void send(String subject, String bodyText, String[] to, String[] cc, String[] bcc) {
    try {
      MimeMessage mail = new MimeMessage(getSession());
      if (!Objects.equals(mail.getSession().getProperty("mail.send"), "true")) return;
      mail.setFrom(
          new InternetAddress(this.administrationService.getGlobalProperty("mail.from", "")));
      Address[] toAddresses = getAddresses(to);
      if (toAddresses.length == 0) {
        throw new IllegalArgumentException("At least one recipient address is required");
      }
      mail.setRecipients(Message.RecipientType.TO, toAddresses);
      if (cc != null && cc.length > 0) {
        mail.setRecipients(Message.RecipientType.CC, getAddresses(cc));
      }
      if (bcc != null && bcc.length > 0) {
        mail.setRecipients(Message.RecipientType.BCC, getAddresses(bcc));
      }
      mail.setSubject(subject);
      mail.setSentDate(new Date());

      MimeBodyPart mimeBodyPart = new MimeBodyPart();
      // TODO: might need to read from GP mail.default_content_type
      // mail.setContent(bodyText, "text/html;charset=utf-8");
      mimeBodyPart.setContent(bodyText, "text/html");
      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(mimeBodyPart);
      mail.setContent(multipart);

      Transport transport = session.getTransport();
      log.info("Sending Mail");
      transport.connect(
          session.getProperty("mail.smtp.host"),
          session.getProperty("mail.user"),
          session.getProperty("mail.password"));
      transport.sendMessage(mail, mail.getAllRecipients());
      log.info("Mail Sent");
      transport.close();
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while sending email", e);
    }
  }

  private Address[] getAddresses(String[] addrs) throws AddressException {
    if (addrs != null && addrs.length > 0) {
      Address[] addresses = new Address[addrs.length];
      for (int i = 0; i < addrs.length; i++) {
        addresses[i] = new InternetAddress(addrs[i]);
      }
      return addresses;
    }
    return new Address[0];
  }

  private Session getSession() {
    if (session == null) {
      synchronized (this) {
        if (session == null) {
          Properties sessionProperties = mailSessionPropertiesFromPath();
          if (sessionProperties == null) {
            log.info(
                "Could not load mail properties from application data directory. Loading from OMRS settings.");
            sessionProperties = mailSessionPropertiesFromOMRS();
          }
          final String user = sessionProperties.getProperty("mail.user");
          final String password = sessionProperties.getProperty("mail.password");
          if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            session =
                Session.getInstance(
                    sessionProperties,
                    new Authenticator() {
                      public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                      }
                    });
          } else {
            session = Session.getInstance(sessionProperties);
          }
        }
      }
    }
    return session;
  }

  /**
   * To be used as fallback. Mail properties are visible in openmrs settings.
   *
   * @return
   */
  private Properties mailSessionPropertiesFromOMRS() {
    Properties p = new Properties();
    p.put(
        "mail.transport.protocol",
        administrationService.getGlobalProperty("mail.transport_protocol", "smtp"));
    p.put("mail.smtp.host", administrationService.getGlobalProperty("mail.smtp_host", ""));
    p.put(
        "mail.smtp.port",
        administrationService.getGlobalProperty("mail.smtp_port", "25")); // mail.smtp_port
    p.put(
        "mail.smtp.auth",
        administrationService.getGlobalProperty("mail.smtp_auth", "false")); // mail.smtp_auth
    p.put(
        "mail.smtp.starttls.enable",
        administrationService.getGlobalProperty("mail.smtp.starttls.enable", "true"));
    p.put(
        "mail.smtp.ssl.enable",
        administrationService.getGlobalProperty("mail.smtp.ssl.enable", "true"));
    p.put("mail.debug", administrationService.getGlobalProperty("mail.debug", "false"));
    p.put("mail.from", administrationService.getGlobalProperty("mail.from", ""));
    p.put("mail.user", administrationService.getGlobalProperty("mail.user", ""));
    p.put("mail.password", administrationService.getGlobalProperty("mail.password", ""));
    // p.put("mail.smtp.ssl.trust", "smtp.gmail.com");
    return p;
  }

  private Properties mailSessionPropertiesFromPath() {
    Path propertyFilePath =
        Paths.get(OpenmrsUtil.getApplicationDataDirectory(), EMAIL_PROPERTIES_FILENAME);
    if (Files.exists(propertyFilePath)) {
      Properties properties = new Properties();
      try (InputStream inputStream = Files.newInputStream(propertyFilePath)) {
        log.info("Reading properties from: " + propertyFilePath);
        properties.load(inputStream);
        return properties;
      } catch (IOException e) {
        log.error("Could not load email properties from: " + propertyFilePath, e);
      }
    } else {
      log.warn("No mail configuration defined at " + propertyFilePath);
    }
    return null;
  }
}
