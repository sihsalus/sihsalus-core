package org.openmrs.module.attachments.rest;

import static org.openmrs.module.attachments.AttachmentsContext.getContentFamily;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.context.Context;
import org.openmrs.module.attachments.AttachmentsConstants;
import org.openmrs.module.attachments.AttachmentsContext;
import org.openmrs.module.attachments.obs.AttachmentComplexData;
import org.openmrs.module.attachments.obs.ComplexViewHelper;
import org.openmrs.module.attachments.obs.ValueComplex;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.obs.ComplexData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

@Controller
@OpenmrsProfile(openmrsPlatformVersion = "2.2.* - 9.*")
@RequestMapping(
    value = "/rest/" + RestConstants.VERSION_1 + "/" + AttachmentsConstants.ATTACHMENT_URI)
public class AttachmentBytesResource extends BaseRestController {

  protected final Log log = LogFactory.getLog(getClass());

  @GetMapping(value = AttachmentsConstants.ATTACHMENT_BYTES_URI)
  public void getFile(
      @PathVariable("uuid") String uuid,
      @RequestParam(required = false, value = "view") String view,
      HttpServletResponse response)
      throws ResponseException {
    Context.requirePrivilege(AttachmentsConstants.VIEW_ATTACHMENTS);

    AttachmentsContext context =
        Context.getRegisteredComponent(
            AttachmentsConstants.COMPONENT_ATT_CONTEXT, AttachmentsContext.class);

    // Getting the Core/Platform complex data object
    Obs obs = context.getObsService().getObsByUuid(uuid);
    if (obs == null) {
      throw new ObjectNotFoundException("Attachment obs not found: " + uuid);
    }

    if (!obs.isComplex()) {
      throw new IllegalRequestException(
          "The following obs is not a complex obs, no complex data can be retrieved. "
              + "Obs UUID: "
              + obs.getUuid());
    }

    ComplexViewHelper viewHelper = context.getComplexViewHelper();

    Obs complexObs =
        Context.getObsService().getComplexObs(obs.getObsId(), viewHelper.getView(obs, view));
    ComplexData complexData = complexObs.getComplexData();

    // Switching to our complex data object
    ValueComplex valueComplex = new ValueComplex(obs.getValueComplex());
    AttachmentComplexData attComplexData =
        context.getComplexDataHelper().build(valueComplex.getInstructions(), complexData);

    String mimeType =
        StringUtils.defaultIfBlank(attComplexData.getMimeType(), "application/octet-stream");
    String title = sanitizeHeaderValue(attComplexData.getTitle());

    // The attachment metadata is sent as HTTP headers.
    response.setContentType(mimeType);
    response.addHeader("Content-Family", getContentFamily(mimeType).name());
    response.addHeader("File-Name", title);
    response.addHeader(
        "File-Ext", sanitizeHeaderValue(getExtension(attComplexData.getTitle(), mimeType)));
    response.addHeader("X-Content-Type-Options", "nosniff");

    try {
      byte[] bytes = attComplexData.asByteArray();
      if (mimeType != null && mimeType.startsWith("text/html")) {
        String byteString = HtmlUtils.htmlEscape(new String(bytes, StandardCharsets.UTF_8));
        bytes = byteString.getBytes(StandardCharsets.UTF_8);
      }
      response.getOutputStream().write(bytes);
    } catch (IOException ex) {
      response.setStatus(500);
      throw new GenericRestException(
          "There was an error when downloading the attachment's bytes content."
              + " Perhaps the file content is corrupted.",
          ex);
    }
  }

  public static String getExtension(String fileName, String mimeType) {
    String ext = FilenameUtils.getExtension(fileName);
    String extFromMimeType = AttachmentsContext.getExtension(mimeType);
    if (!StringUtils.isEmpty(ext)) {
      if (ext.length()
          > 6) { // this is a bit arbitrary, just to discriminate funny named files such as
        // "uiohdz.iuhezuidhuih"
        ext = extFromMimeType;
      }
    } else {
      ext = extFromMimeType;
    }
    return ext;
  }

  static String sanitizeHeaderValue(String value) {
    if (value == null) {
      return "";
    }
    return value.replace('\r', '_').replace('\n', '_');
  }
}
