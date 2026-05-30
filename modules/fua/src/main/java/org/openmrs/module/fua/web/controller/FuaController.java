package org.openmrs.module.fua.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.Fua;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.FuaEstado;
import org.openmrs.module.fua.api.FuaEstadoService;
import org.openmrs.module.fua.api.FuaService;
import org.openmrs.module.fua.api.FuaVersionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;

@Controller
@RequestMapping("/module/fua")
public class FuaController {

  private static final String FORM_VIEW = "/module/fua/pages/addFua";
  private static final String OPENMRS_MSG_ATTR = "openmrs_msg";
  private static final String OPENMRS_ERROR_ATTR = "openmrs_error";
  private static final String VISIT_REST_URL = "http://localhost:8080/openmrs/ws/rest/v1/visit/";

  protected final Log log = LogFactory.getLog(getClass());

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();
  private final FuaService fuaService;
  private final FuaEstadoService fuaEstadoService;
  private final FuaVersionService fuaVersionService;

  public FuaController(
      FuaService fuaService,
      FuaEstadoService fuaEstadoService,
      FuaVersionService fuaVersionService) {
    this.fuaService = fuaService;
    this.fuaEstadoService = fuaEstadoService;
    this.fuaVersionService = fuaVersionService;
  }

  @RequestMapping(method = RequestMethod.GET)
  public String onGet(
      ModelMap model, @RequestParam(value = "fuaId", required = false) Integer fuaId) {
    Fua fua = fuaId != null ? fuaService.getFua(fuaId) : new Fua();
    model.addAttribute("fua", fua);
    model.addAttribute("fuas", fuaService.getAllFuas());
    model.addAttribute("fuaEstados", fuaEstadoService.getAllEstados());
    return FORM_VIEW;
  }

  @RequestMapping(method = RequestMethod.POST)
  public String onPost(
      HttpSession httpSession,
      @ModelAttribute("fua") Fua fua,
      BindingResult errors,
      @RequestParam(required = false, value = "action") String action) {
    if (errors.hasErrors()) {
      return FORM_VIEW;
    }

    if (!Context.isAuthenticated()) {
      errors.reject("fua.auth.required");
    } else if ("purge".equals(action)) {
      try {
        fuaService.purgeFua(fua);
        httpSession.setAttribute(OPENMRS_MSG_ATTR, "fua.delete.success");
      } catch (Exception ex) {
        httpSession.setAttribute(OPENMRS_ERROR_ATTR, "fua.delete.failure");
        log.error("Error deleting FUA", ex);
      }
    } else {
      fuaService.saveFua(fua);
      httpSession.setAttribute(OPENMRS_MSG_ATTR, "fua.saved");
    }
    return "redirect:/module/fua";
  }

  @RequestMapping(
      value = "/list",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<Fua> getAllFuas() {
    return fuaService.getAllFuas();
  }

  @RequestMapping(
      value = "/uuid/{uuid}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> getFuaByUuid(@PathVariable("uuid") String uuid) {
    Fua fua = fuaService.getFuaByUuid(uuid);
    if (fua == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FUA no encontrado.");
    }
    return ResponseEntity.ok(fua);
  }

  @RequestMapping(
      value = "/visitInfo/{visitUuid}/generator/{identifierFormat}",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> renderVisitInfo(
      @PathVariable String visitUuid, @PathVariable String identifierFormat) {
    try {
      Fua fua = fuaService.getFuaByVisitUuid(visitUuid);
      if (fua == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("FUA no encontrado para visitUuid: " + visitUuid);
      }

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put(
          "payload",
          StringUtils.isBlank(fua.getPayload()) ? null : objectMapper.readTree(fua.getPayload()));
      requestBody.put("schemaType", "xd");
      requestBody.put("outputType", "xd");
      requestBody.put("createdBy", "Fua-user");
      requestBody.put("FUAFormatFromSchemaId", identifierFormat);

      ResponseEntity<String> remoteResponse =
          restTemplate.exchange(
              getFuaGeneratorBaseUrl() + "/ws/FUAFromVisit",
              HttpMethod.POST,
              jsonEntity(requestBody),
              String.class);

      return ResponseEntity.status(remoteResponse.getStatusCode())
          .contentType(MediaType.APPLICATION_JSON)
          .body(remoteResponse.getBody());
    } catch (Exception ex) {
      log.error("Error processing FUA visit render request", ex);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body("Error procesando la solicitud: " + ex.getMessage());
    }
  }

  @RequestMapping(
      value = "/patient/{patientUuid}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> getFuasByPatientUuid(@PathVariable("patientUuid") String patientUuid) {
    return ResponseEntity.ok(fuaService.getFuasByPatientUuid(patientUuid));
  }

  @RequestMapping(
      value = "/id/{id}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> getFuaById(@PathVariable("id") Integer id) {
    Fua fua = fuaService.getFuaById(id);
    if (fua == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("FUA no encontrado por el Id: " + id);
    }
    return ResponseEntity.ok(fua);
  }

  @RequestMapping(
      value = "/solicitudes",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> getSolicitudesFua(
      @RequestParam(value = "status", required = false) String estado,
      @RequestParam(value = "fechaInicio", required = false) String fechaInicioStr,
      @RequestParam(value = "fechaFin", required = false) String fechaFinStr,
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "size", defaultValue = "10") int size) {
    try {
      LocalDate fechaInicio = parseDate(fechaInicioStr);
      LocalDate fechaFin = parseDate(fechaFinStr);
      return ResponseEntity.ok(
          fuaService.getFuasFiltrados(estado, fechaInicio, fechaFin, page, size));
    } catch (Exception ex) {
      return ResponseEntity.badRequest().body("Formato de fecha inválido. Use yyyy-MM-dd");
    }
  }

  @RequestMapping(
      value = "/generateFromVisit/{visitUuid}",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> generateFuaFromVisit(@PathVariable String visitUuid) {
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              VISIT_REST_URL + visitUuid + "?v=full",
              HttpMethod.GET,
              authenticatedRestEntity(),
              String.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo obtener la visita.");
      }

      FuaEstado estadoPendiente = fuaEstadoService.getEstado(1);
      Fua fua = fuaService.getFuaByVisitUuid(visitUuid);
      if (fua == null) {
        fua = new Fua();
        fua.setName("PRUEBA DE generateFuaFromVisit");
        fua.setVisitUuid(visitUuid);
        fua.setFuaEstado(estadoPendiente);
      } else {
        fuaVersionService.saveFuaVersion(fua, "GenerateFromVisit");
      }

      fua.setPayload(response.getBody());
      fua.setFuaGeneratorUuid(generarFuadeFuaGenerator(fua));
      fuaService.saveFua(fua);

      return ResponseEntity.ok(fua);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.error("HTTP error loading visit " + visitUuid, ex);
      return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
    } catch (Exception ex) {
      log.error("Error generating FUA from visit " + visitUuid, ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error al generar el FUA: " + ex.getMessage());
    }
  }

  @RequestMapping(
      value = "/estado/update/{fuaId}",
      method = RequestMethod.PUT,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<?> actualizarEstadoFua(
      @PathVariable Integer fuaId, @RequestBody Map<String, Object> body) {
    try {
      if (!body.containsKey("estadoId")) {
        return ResponseEntity.badRequest()
            .body("El cuerpo de la solicitud debe incluir 'estadoId'");
      }

      Fua fua = fuaService.getFuaById(fuaId);
      if (fua == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("FUA no encontrado con ID: " + fuaId);
      }

      fuaVersionService.saveFuaVersion(fua, "Update estado de FUA");
      fua.setFuaEstado(fuaEstadoService.getEstado(asInteger(body.get("estadoId"))));
      fuaService.saveFua(fua);
      return ResponseEntity.ok(fua);
    } catch (Exception ex) {
      log.error("Error updating FUA status", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error interno al actualizar el estado del FUA.");
    }
  }

  @RequestMapping(
      value = "/RenderFUA/{visitUuid}",
      method = RequestMethod.POST,
      produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public ResponseEntity<String> renderFua(@PathVariable String visitUuid) {
    try {
      Fua fua = fuaService.getFuaByVisitUuid(visitUuid);
      if (fua == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("<h2>No existe FUA para esta visita</h2>");
      }
      if (StringUtils.isBlank(fua.getFuaGeneratorUuid())) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("<h2>El FUA no tiene UUID del generador</h2>");
      }

      ResponseEntity<String> response =
          restTemplate.exchange(
              getFuaGeneratorBaseUrl()
                  + "/ws/FUAFromVisit/"
                  + fua.getFuaGeneratorUuid()
                  + "/render",
              HttpMethod.POST,
              fuaGeneratorEntity(),
              String.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        return ResponseEntity.status(response.getStatusCode())
            .body("<h2>Error renderizando FUA</h2>");
      }
      return ResponseEntity.ok()
          .contentType(MediaType.TEXT_HTML)
          .body(sanitizeHtml(response.getBody()));
    } catch (Exception ex) {
      log.error("Error rendering FUA", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              "<h2>Error interno renderizando FUA</h2><pre>"
                  + HtmlUtils.htmlEscape(StringUtils.defaultString(ex.getMessage()))
                  + "</pre>");
    }
  }

  private LocalDate parseDate(String value) {
    return value == null ? null : LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private String getFuaGeneratorBaseUrl() {
    String url =
        Context.getAdministrationService().getGlobalProperty(FuaConfig.FUA_GENERATOR_URL_GP);
    if (StringUtils.isBlank(url)) {
      url = FuaConfig.FUA_GENERATOR_URL_DEFAULT;
      log.warn("Global property " + FuaConfig.FUA_GENERATOR_URL_GP + " not set, using " + url);
    }
    return url;
  }

  private String getFuaIdentifierBase() {
    return Context.getAdministrationService().getGlobalProperty(FuaConfig.FUA_GENERATOR_IDENTIFIER);
  }

  private String sanitizeHtml(String body) {
    return StringUtils.defaultString(
        Jsoup.clean(StringUtils.defaultString(body), Safelist.relaxed()));
  }

  private String generarFuadeFuaGenerator(Fua fua) {
    try {
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put(
          "payload",
          StringUtils.isBlank(fua.getPayload()) ? null : objectMapper.readTree(fua.getPayload()));
      requestBody.put("schemaType", "xd");
      requestBody.put("outputType", "xd");
      requestBody.put("createdBy", "Fua-user");
      requestBody.put("FUAFormatFromSchemaId", getFuaIdentifierBase());

      ResponseEntity<String> response =
          restTemplate.exchange(
              getFuaGeneratorBaseUrl() + "/ws/FUAFromVisit",
              HttpMethod.POST,
              jsonEntity(requestBody),
              String.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new IllegalStateException("Error microservicio: " + response.getStatusCode());
      }

      JsonNode root = objectMapper.readTree(response.getBody());
      if (!root.has("uuid")) {
        throw new IllegalStateException("El microservicio no devolvio uuid");
      }
      return root.get("uuid").asText();
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Error generando FUA desde el generador externo: " + ex.getMessage(), ex);
    }
  }

  private HttpEntity<String> authenticatedRestEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("admin", "Admin123");
    return new HttpEntity<>(headers);
  }

  private HttpEntity<Void> fuaGeneratorEntity() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("fuagentoken", "fuagenerator");
    return new HttpEntity<>(headers);
  }

  private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("fuagentoken", "fuagenerator");
    return new HttpEntity<>(body, headers);
  }

  private Integer asInteger(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.valueOf(String.valueOf(value));
  }
}
