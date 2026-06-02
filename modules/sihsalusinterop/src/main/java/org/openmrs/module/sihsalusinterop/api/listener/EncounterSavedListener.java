package org.openmrs.module.sihsalusinterop.api.listener;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Encounter;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sihsalusinterop.api.DyakuSenderService;
import org.openmrs.module.sihsalusinterop.api.exception.InteropException;
import org.openmrs.module.sihsalusinterop.api.service.BundleBuilderService;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.core.api.StaticModuleTaskRunner;

/**
 * EncounterSavedListener - Escucha eventos de guardado de Encounter Cuando se guarda un Encounter,
 * construye automáticamente un Bundle FHIR y lo encola para envío a RENHICE. Hospital Santa
 * Clotilde - SIH.SALUS
 */
public class EncounterSavedListener {

  private static final Log log = LogFactory.getLog(EncounterSavedListener.class);

  /** Endpoint por defecto de RENHICE (simulador HAPI FHIR) */
  private static final String DEFAULT_RENHICE_ENDPOINT = "http://hapi-fhir-server:8080/fhir";

  /** Global Property para el endpoint de RENHICE */
  private static final String GP_RENHICE_ENDPOINT = "sihsalusinterop.renhice.endpoint";

  /** Global Property para habilitar/deshabilitar el listener */
  private static final String GP_ENABLED = "sihsalusinterop.renhice.enabled";

  /**
   * Método llamado cuando se guarda un Encounter Este método es llamado automáticamente por
   * EncounterSavedAdvice después de guardar un Encounter en el EncounterService.
   *
   * @param encounter El Encounter que se acaba de guardar
   */
  public static void onEncounterSaved(Encounter encounter) {
    if (encounter == null || encounter.getVoided()) {
      return;
    }
    if (encounter.getEncounterId() == null) {
      return;
    }

    StaticModuleTaskRunner.runInBackground(
        null, () -> processEncounter(encounter.getEncounterId()));
  }

  private static void processEncounter(Integer encounterId) {
    if (!isEnabled()) {
      log.debug(
          ">>> [EVENT] Listener deshabilitado (Global Property sihsalusinterop.renhice.enabled ="
              + " false)");
      return;
    }

    try {
      log.info(">>> [EVENT] Encounter guardado detectado: " + encounterId);

      // Recargar el Encounter con todas sus relaciones para evitar LazyInitializationException
      org.openmrs.api.EncounterService encounterService = Context.getEncounterService();
      Encounter reloadedEncounter = encounterService.getEncounter(encounterId);

      if (reloadedEncounter == null) {
        log.warn(">>> [EVENT] No se pudo recargar el Encounter: " + encounterId);
        return;
      }

      BundleBuilderService bundleService = getBundleBuilderService();
      if (bundleService == null) {
        log.error(
            ">>> [EVENT] BundleBuilderService no disponible. No se puede construir Bundle FHIR.");
        return;
      }

      Bundle bundle;
      try {
        bundle = bundleService.buildClinicalSummaryBundle(reloadedEncounter);
      } catch (InteropException e) {
        if ("DNI_NOT_FOUND".equals(e.getErrorCode())) {
          log.warn(
              ">>> [EVENT] Encounter "
                  + reloadedEncounter.getId()
                  + " no procesado: "
                  + e.getMessage());
          log.warn(
              ">>> [EVENT] El paciente debe tener DNI para enviar a RENHICE. Agregar DNI al"
                  + " paciente en OpenMRS.");
        } else {
          log.error(
              ">>> [EVENT] Error de interoperabilidad al construir Bundle: " + e.getMessage(), e);
        }
        return;
      }

      FhirContext ctx = FhirContext.forR4();
      String jsonPayload = ctx.newJsonParser().setPrettyPrint(false).encodeResourceToString(bundle);

      DyakuSenderService senderService = Context.getService(DyakuSenderService.class);
      if (senderService == null) {
        log.error(">>> [EVENT] DyakuSenderService no disponible. No se puede encolar Bundle FHIR.");
        return;
      }

      senderService.queueMessage("FHIR_BUNDLE", jsonPayload, getRenhiceEndpoint());

      log.info(
          ">>> [EVENT] Bundle FHIR encolado exitosamente para Encounter: "
              + reloadedEncounter.getId());
    } catch (Exception e) {
      log.error(">>> [EVENT] Error al procesar Encounter guardado: " + encounterId, e);
    }
  }

  /** Obtiene el BundleBuilderService como bean de Spring */
  private static BundleBuilderService getBundleBuilderService() {
    try {
      return Context.getRegisteredComponent(
          "sihsalusinterop.BundleBuilderService", BundleBuilderService.class);
    } catch (Exception e) {
      log.warn(">>> Error al obtener BundleBuilderService desde Spring: " + e.getMessage());
      return new BundleBuilderService();
    }
  }

  /** Verifica si el listener está habilitado mediante Global Property */
  private static boolean isEnabled() {
    try {
      return Boolean.parseBoolean(getGlobalProperty(GP_ENABLED, "true"));
    } catch (Exception e) {
      log.warn(
          "Error al leer Global Property " + GP_ENABLED + ". Usando valor por defecto: true", e);
    }
    // Por defecto, está habilitado
    return true;
  }

  /**
   * Obtiene el endpoint de RENHICE desde Global Properties de OpenMRS
   *
   * @return URL del endpoint de RENHICE (por defecto: http://hapi-fhir-server:8080/fhir)
   */
  private static String getRenhiceEndpoint() {
    try {
      String endpoint = getGlobalProperty(GP_RENHICE_ENDPOINT, DEFAULT_RENHICE_ENDPOINT);
      if (endpoint != null && !endpoint.trim().isEmpty()) {
        return endpoint.trim();
      }
    } catch (Exception e) {
      log.warn(
          "Error al leer Global Property " + GP_RENHICE_ENDPOINT + ". Usando valor por defecto", e);
    }
    // Retornar valor por defecto si no se puede leer la Global Property
    return DEFAULT_RENHICE_ENDPOINT;
  }

  private static String getGlobalProperty(String property, String defaultValue) {
    AdministrationService adminService = Context.getAdministrationService();
    boolean addedProxyPrivilege =
        Context.isAuthenticated()
            && !Context.hasPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
    if (addedProxyPrivilege) {
      Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
    }
    try {
      return adminService.getGlobalProperty(property, defaultValue);
    } finally {
      if (addedProxyPrivilege) {
        Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
      }
    }
  }
}
