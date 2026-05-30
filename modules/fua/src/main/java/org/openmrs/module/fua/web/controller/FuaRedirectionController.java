package org.openmrs.module.fua.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.openmrs.api.context.Context;
import org.openmrs.module.fua.FuaConfig;
import org.openmrs.module.fua.web.utils.MultipartInputStreamFileResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

@Controller
public class FuaRedirectionController {

  protected final Log log = LogFactory.getLog(getClass());

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();

  @RequestMapping(value = "/FUAFormat", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity<String> redirectFuaRequest(
      @RequestParam("name") String name,
      @RequestParam("createdBy") String createdBy,
      @RequestParam("formatPayload") MultipartFile formatPayload)
      throws IOException {
    HttpHeaders headers = fuaGeneratorHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("name", name);
    body.add("createdBy", createdBy);
    body.add(
        "formatPayload",
        new MultipartInputStreamFileResource(
            formatPayload.getInputStream(),
            formatPayload.getOriginalFilename(),
            formatPayload.getSize()));

    ResponseEntity<String> response =
        restTemplate.postForEntity(
            getFuaGeneratorBaseUrl() + "/ws/FUAFormat",
            new HttpEntity<>(body, headers),
            String.class);

      return ResponseEntity.status(response.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(response.getBody());
  }

  @RequestMapping(value = "/FUAFormat", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<String> redirectFuaFormatGetRequest() throws IOException {
    ResponseEntity<String> response =
        restTemplate.exchange(
            getFuaGeneratorBaseUrl() + "/ws/FUAFormat",
            HttpMethod.GET,
            new HttpEntity<Void>(fuaGeneratorHeaders()),
            String.class);

    String responseBody = stripFormatContent(response.getBody());
    return ResponseEntity.status(response.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(responseBody);
  }

  @RequestMapping(
      value = "/FUAFormat/{id}/render",
      method = RequestMethod.POST,
      produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public ResponseEntity<String> redirectFuaFormatRenderRequest(@PathVariable("id") String id) {
    String remoteUrl =
        getFuaGeneratorBaseUrl()
            + "/ws/FUAFormat/"
            + UriUtils.encodePathSegment(id, StandardCharsets.UTF_8)
            + "/render";
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              remoteUrl,
              HttpMethod.POST,
              new HttpEntity<Void>(fuaGeneratorHeaders()),
              String.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        return renderError(response.getStatusCode(), remoteUrl, response.getBody());
      }

      return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(sanitizeHtml(response.getBody()));
    } catch (HttpStatusCodeException ex) {
      log.error("HTTP error rendering FUAFormat id " + id + " at " + remoteUrl, ex);
      return renderError(ex.getStatusCode(), remoteUrl, ex.getResponseBodyAsString());
    } catch (Exception ex) {
      log.error("Internal error rendering FUAFormat id " + id + " at " + remoteUrl, ex);
      return renderError(
          HttpStatus.INTERNAL_SERVER_ERROR, remoteUrl, ex.getMessage() == null ? "" : ex.getMessage());
    }
  }

  private String stripFormatContent(String responseBody) throws IOException {
    if (StringUtils.isBlank(responseBody)) {
      return responseBody;
    }

    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode results = root.get("results");
    if (results != null && results.isArray()) {
      for (JsonNode item : results) {
        if (item.isObject()) {
          ((ObjectNode) item).remove("content");
        }
      }
    }
    root.findParents("content").forEach(node -> ((ObjectNode) node).remove("content"));
    return objectMapper.writeValueAsString(root);
  }

  private ResponseEntity<String> renderError(
      org.springframework.http.HttpStatusCode statusCode, String remoteUrl, String body) {
    return ResponseEntity.status(statusCode)
        .contentType(MediaType.TEXT_HTML)
        .body(
            "<h2>Error renderizando FUAFormat</h2><pre>Status: "
          + HtmlUtils.htmlEscape(String.valueOf(statusCode))
                + "\nURL: "
          + HtmlUtils.htmlEscape(StringUtils.defaultString(remoteUrl))
                + "\n\n"
          + HtmlUtils.htmlEscape(StringUtils.defaultString(body))
                + "</pre>");
  }

  private HttpHeaders fuaGeneratorHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("fuagentoken", "fuagenerator");
    return headers;
  }

  private String sanitizeHtml(String body) {
    return StringUtils.defaultString(Jsoup.clean(StringUtils.defaultString(body), Safelist.relaxed()));
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
}
