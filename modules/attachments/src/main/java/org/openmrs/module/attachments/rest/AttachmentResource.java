package org.openmrs.module.attachments.rest;

import static org.openmrs.module.attachments.AttachmentsContext.getContentFamily;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.DateProperty;
import io.swagger.models.properties.StringProperty;
import jakarta.activation.MimetypesFileTypeMap;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.attachments.AttachmentsConstants;
import org.openmrs.module.attachments.AttachmentsContext;
import org.openmrs.module.attachments.AttachmentsService;
import org.openmrs.module.attachments.ComplexObsSaver;
import org.openmrs.module.attachments.obs.Attachment;
import org.openmrs.module.attachments.obs.ValueComplex;
import org.openmrs.module.webservices.docs.swagger.core.property.EnumProperty;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.Uploadable;
import org.openmrs.module.webservices.rest.web.resource.impl.DataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.EmptySearchResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.GenericRestException;
import org.openmrs.module.webservices.rest.web.response.IllegalRequestException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.sihsalus.core.api.authorization.PatientObjectAuthorizationService;
import org.springframework.web.multipart.MultipartFile;

@Resource(
    name = RestConstants.VERSION_1 + "/" + AttachmentsConstants.ATTACHMENT_URI,
    supportedClass = Attachment.class,
    supportedOpenmrsVersions = {"2.2.* - 9.*"})
public class AttachmentResource extends DataDelegatingCrudResource<Attachment>
    implements Uploadable {

  protected static final String REASON = "REST web service";

  @Override
  public Attachment newDelegate() {
    return new Attachment();
  }

  @Override
  public Attachment save(Attachment delegate) {
    Context.requirePrivilege(AttachmentsConstants.CREATE_ATTACHMENTS);

    Obs obs = Context.getObsService().saveObs(delegate.getObs(), REASON);
    return new Attachment(
        obs,
        Context.getRegisteredComponent(
                AttachmentsConstants.COMPONENT_ATT_CONTEXT, AttachmentsContext.class)
            .getComplexDataHelper());
  }

  @Override
  public String getRequiredGetPrivilege() {
    return AttachmentsConstants.VIEW_ATTACHMENTS;
  }

  @Override
  public Attachment getByUniqueId(String uniqueId) {
    Context.requirePrivilege(AttachmentsConstants.VIEW_ATTACHMENTS);

    Obs obs = Context.getObsService().getObsByUuid(uniqueId);
    if (obs == null) {
      throw new ObjectNotFoundException("Attachment obs not found: " + uniqueId);
    }
    if (!obs.isComplex())
      throw new GenericRestException(uniqueId + " does not identify a complex obs.", null);
    else {
      requirePatientAccess(obs);
      return new Attachment(
          obs,
          Context.getRegisteredComponent(
                  AttachmentsConstants.COMPONENT_ATT_CONTEXT, AttachmentsContext.class)
              .getComplexDataHelper());
    }
  }

  @Override
  protected void delete(Attachment delegate, String reason, RequestContext context)
      throws ResponseException {
    Context.requirePrivilege(AttachmentsConstants.CREATE_ATTACHMENTS);

    String encounterUuid =
        delegate.getObs().getEncounter() != null
            ? delegate.getObs().getEncounter().getUuid()
            : null;
    Context.getObsService().voidObs(delegate.getObs(), REASON);
    voidEncounterIfEmpty(Context.getEncounterService(), encounterUuid);
  }

  @Override
  public void purge(Attachment delegate, RequestContext context) throws ResponseException {
    Context.requirePrivilege(AttachmentsConstants.CREATE_ATTACHMENTS);

    String encounterUuid =
        delegate.getObs().getEncounter() != null
            ? delegate.getObs().getEncounter().getUuid()
            : null;
    Context.getObsService().purgeObs(delegate.getObs());
    voidEncounterIfEmpty(Context.getEncounterService(), encounterUuid);
  }

  @Override
  public Object upload(MultipartFile file, RequestContext context)
      throws ResponseException, IOException {
    Context.requirePrivilege(AttachmentsConstants.CREATE_ATTACHMENTS);

    if (file == null) {
      throw new IllegalRequestException(
          "A file parameter must be provided when uploading an attachment.");
    }

    // Prepare Parameters
    Patient patient = Context.getPatientService().getPatientByUuid(context.getParameter("patient"));
    Visit visit = Context.getVisitService().getVisitByUuid(context.getParameter("visit"));
    Encounter encounter =
        Context.getEncounterService().getEncounterByUuid(context.getParameter("encounter"));
    Provider provider =
        Context.getProviderService().getProviderByUuid(context.getParameter("provider"));
    String fileCaption = context.getParameter("fileCaption");
    String formFieldNamespace = context.getParameter("formFieldNamespace");
    String formFieldPath = context.getParameter("formFieldPath");
    String instructions = context.getParameter("instructions");
    String base64Content = context.getParameter("base64Content");

    AttachmentsContext ctx =
        Context.getRegisteredComponent(
            AttachmentsConstants.COMPONENT_ATT_CONTEXT, AttachmentsContext.class);

    String fileName = getSafeOriginalFilename(file.getOriginalFilename());

    if (base64Content != null) {
      file = new Base64MultipartFile(base64Content, file.getName(), fileName);
    }
    file = new OriginalFilenameMultipartFile(file, fileName);

    // Verify File Size
    if (ctx.getMaxUploadFileSize() * 1024 * 1024 < (double) file.getSize()) {
      throw new IllegalRequestException("The file exceeds the maximum size");
    }

    // Verify file extension
    String fileExtension = normalizeExtension(FilenameUtils.getExtension(fileName));

    List<String> allowedExtensions = normalizeExtensions(ctx.getAllowedFileExtensions());
    if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(fileExtension)) {
      throw new IllegalRequestException("The extension " + fileExtension + " is not valid");
    }

    // Verify file name
    List<String> deniedFileNames = normalizeFileNames(ctx.getDeniedFileNames());
    if (deniedFileNames.contains(fileName.toLowerCase(Locale.ROOT))) {
      throw new IllegalRequestException("The file name is not valid");
    }

    // Verify Parameters
    if (patient == null) {
      throw new IllegalRequestException(
          "A patient parameter must be provided when uploading an attachment.");
    }
    PatientObjectAuthorizationService.current().requireCanReadPatient(patient.getUuid());

    if (StringUtils.isEmpty(instructions)) instructions = ValueComplex.INSTRUCTIONS_DEFAULT;

    // Verify Parameters
    if (encounter != null && visit != null) {
      if (encounter.getVisit() == null
          || !StringUtils.equals(encounter.getVisit().getUuid(), visit.getUuid())) {
        throw new IllegalRequestException(
            "The specified encounter does not belong to the provided visit, upload aborted.");
      }
    }

    // Verify Content Type
    if (!allowedExtensions.isEmpty()) {
      String fileType = detectContentType(file);
      if (fileType != null && AttachmentsContext.isMimeTypeHandled(fileType)) {
        String detectedExtension = normalizeExtension(AttachmentsContext.getExtension(fileType));
        if (!allowedExtensions.contains(detectedExtension)) {
          throw new IllegalRequestException(
              "The file content type " + fileType + " is not allowed");
        }
      }
    }

    // Verify the file contents
    // Just in case the magic bytes are manipulated, we are using the submitted file
    // extension to get the mime type
    String mimeType = new MimetypesFileTypeMap().getContentType(fileName);
    if (mimeType.startsWith("image/") && !isValidImage(file.getInputStream())) {
      throw new IllegalRequestException("The file has invalid content");
    }

    if (visit != null && encounter == null) {
      encounter = ctx.getAttachmentEncounter(patient, visit, provider);
    }

    if (encounter != null && visit == null) {
      visit = encounter.getVisit();
    }

    // Save Obs
    Obs obs;
    switch (getContentFamily(file.getContentType())) {
      case IMAGE:
        obs =
            Context.getRegisteredComponent(
                    AttachmentsConstants.COMPONENT_COMPLEXOBS_SAVER, ComplexObsSaver.class)
                .saveImageAttachment(
                    visit,
                    patient,
                    encounter,
                    fileCaption,
                    file,
                    instructions,
                    formFieldNamespace,
                    formFieldPath);
        break;

      case OTHER:
      default:
        obs =
            Context.getRegisteredComponent(
                    AttachmentsConstants.COMPONENT_COMPLEXOBS_SAVER, ComplexObsSaver.class)
                .saveOtherAttachment(
                    visit,
                    patient,
                    encounter,
                    fileCaption,
                    file,
                    instructions,
                    formFieldNamespace,
                    formFieldPath);
        break;
    }

    return ConversionUtil.convertToRepresentation(
        obs, new CustomRepresentation(AttachmentsConstants.REPRESENTATION_OBS));
  }

  private boolean isValidImage(InputStream fileStream) {
    try {
      BufferedImage image = ImageIO.read(fileStream);
      return image != null && image.getHeight() > 0 && image.getWidth() > 0;
    } catch (IOException e) {
      return false;
    } finally {
      if (fileStream.markSupported()) {
        try {
          fileStream.reset();
        } catch (IOException e) {
        }
      }
    }
  }

  private String getSafeOriginalFilename(String originalFilename) {
    String fileName = FilenameUtils.getName(originalFilename);
    if (StringUtils.isBlank(fileName)) {
      throw new IllegalRequestException(
          "A file name must be provided when uploading an attachment.");
    }
    return fileName;
  }

  private List<String> normalizeExtensions(String[] extensions) {
    if (extensions == null || extensions.length == 0) {
      return List.of();
    }
    List<String> normalized = new ArrayList<>();
    Arrays.stream(extensions)
        .filter(StringUtils::isNotBlank)
        .map(this::normalizeExtension)
        .filter(StringUtils::isNotBlank)
        .forEach(normalized::add);
    return normalized;
  }

  private String normalizeExtension(String extension) {
    String normalized = StringUtils.trimToEmpty(extension);
    if (normalized.startsWith(".")) {
      normalized = normalized.substring(1);
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  private List<String> normalizeFileNames(String[] fileNames) {
    if (fileNames == null || fileNames.length == 0) {
      return List.of();
    }
    List<String> normalized = new ArrayList<>();
    Arrays.stream(fileNames)
        .filter(StringUtils::isNotBlank)
        .map(FilenameUtils::getName)
        .map(StringUtils::trimToEmpty)
        .filter(StringUtils::isNotBlank)
        .map(fileName -> fileName.toLowerCase(Locale.ROOT))
        .forEach(normalized::add);
    return normalized;
  }

  private String detectContentType(MultipartFile file) {
    try (InputStream stream = new BufferedInputStream(file.getInputStream())) {
      return URLConnection.guessContentTypeFromStream(stream);
    } catch (IOException e) {
      throw new APIException("Failed to detect the file content type", e);
    }
  }

  @Override
  public DelegatingResourceDescription getCreatableProperties() {
    DelegatingResourceDescription description = new DelegatingResourceDescription();
    description.addProperty("comment");
    description.addProperty("dateTime");
    description.addProperty("filename");
    description.addProperty("bytesMimeType");
    description.addProperty("bytesContentFamily");
    description.addProperty("complexData");
    return description;
  }

  @Override
  public Model getCREATEModel(Representation rep) {
    return new ModelImpl()
        .property("comment", new StringProperty())
        .property("dateTime", new DateProperty())
        .property("filename", new StringProperty())
        .property("bytesMimeType", new StringProperty())
        .property("bytesContentFamily", new EnumProperty(AttachmentsConstants.ContentFamily.class))
        .property("complexData", new StringProperty(StringProperty.Format.URI));
  }

  @Override
  public Model getUPDATEModel(Representation rep) {
    return getCREATEModel(rep);
  }

  @Override
  public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
    DelegatingResourceDescription description = new DelegatingResourceDescription();
    description.addProperty("uuid");
    description.addProperty("dateTime");
    description.addProperty("filename");
    description.addProperty("comment");
    description.addProperty("bytesMimeType");
    description.addProperty("bytesContentFamily");
    description.addSelfLink();
    return description;
  }

  @Override
  public Model getGETModel(Representation rep) {
    ModelImpl model = (ModelImpl) super.getGETModel(rep);
    return model
        .property("uuid", new StringProperty())
        .property("dateTime", new DateProperty())
        .property("filename", new StringProperty())
        .property("comment", new StringProperty())
        .property("bytesMimeType", new StringProperty())
        .property("bytesContentFamily", new EnumProperty(AttachmentsConstants.ContentFamily.class));
  }

  /**
   * Voids the encounter if it contains no non-voided obs.
   *
   * @param encounterService
   * @param encounterUuid
   */
  public static void voidEncounterIfEmpty(EncounterService encounterService, String encounterUuid) {
    Encounter encounter = encounterService.getEncounterByUuid(encounterUuid);
    if (encounter != null && encounter.getAllObs().isEmpty()) {
      encounterService.voidEncounter(encounter, "foo");
    }
  }

  /**
   * Get the Attachments using AttachmentService.
   *
   * @param as specifies the AttachmentService instance.
   * @param patient
   * @param visit
   * @param encounter
   * @param includeEncounterless
   * @param includeVoided
   */
  public List<Attachment> search(
      AttachmentsService as,
      Patient patient,
      Visit visit,
      Encounter encounter,
      String includeEncounterless,
      boolean includeVoided) {
    if (patient != null) {
      PatientObjectAuthorizationService.current().requireCanReadPatient(patient.getUuid());
    }

    List<Attachment> attachmentList = new ArrayList<>();

    if (includeEncounterless != null) {
      if (includeEncounterless.equals("only")) {
        attachmentList = as.getEncounterlessAttachments(patient, includeVoided);

      } else {
        attachmentList =
            as.getAttachments(patient, BooleanUtils.toBoolean(includeEncounterless), includeVoided);
      }
    } else {
      if (encounter != null && visit == null) {
        attachmentList = as.getAttachments(patient, encounter, includeVoided);
      }
      if (visit != null && encounter == null) {
        attachmentList = as.getAttachments(patient, visit, includeVoided);
      }
      if (encounter == null && visit == null) {
        attachmentList = as.getAttachments(patient, includeVoided);
      }
    }
    return attachmentList;
  }

  /**
   * Get Attachments by given parameters (paged according to context if necessary) only if a patient
   * parameter exists in the request set on the {@link RequestContext}, optional encounter, visit ,
   * includeEncounterless , includeVoided request parameters can be specified to filter the
   * attachments.
   *
   * @param context
   * @see
   *     org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource#doSearch(org.openmrs.module.webservices.rest.web.RequestContext)
   * @return Attachments based on the user parameters
   */
  @Override
  protected PageableResult doSearch(RequestContext context) {
    Context.requirePrivilege(AttachmentsConstants.VIEW_ATTACHMENTS);

    // Prepare Parameters
    Patient patient = Context.getPatientService().getPatientByUuid(context.getParameter("patient"));
    Visit visit = Context.getVisitService().getVisitByUuid(context.getParameter("visit"));
    Encounter encounter =
        Context.getEncounterService().getEncounterByUuid(context.getParameter("encounter"));
    String includeEncounterless = context.getParameter("includeEncounterless");
    boolean includeVoided = BooleanUtils.toBoolean(context.getParameter("includeVoided"));

    // Verify Parameters
    if (patient == null) {
      throw new IllegalRequestException(
          "A patient parameter must be provided when searching the attachments.");
    }
    PatientObjectAuthorizationService.current().requireCanReadPatient(patient.getUuid());

    // Search Attachments
    List<Attachment> attachmentList =
        search(
            Context.getRegisteredComponent(
                    AttachmentsConstants.COMPONENT_ATT_CONTEXT, AttachmentsContext.class)
                .getAttachmentsService(),
            patient,
            visit,
            encounter,
            includeEncounterless,
            includeVoided);

    if (attachmentList != null) {
      return new NeedsPaging<>(attachmentList, context);
    }
    return new EmptySearchResult();
  }

  private void requirePatientAccess(Obs obs) {
    if (obs.getPerson() instanceof Patient patient) {
      PatientObjectAuthorizationService.current().requireCanReadPatient(patient.getUuid());
    }
  }

  /**
   * Wrapper class to be passed to ComplexObsSaver#saveImageAttachment
   * ComplexObsSaver#saveImageAttachment needs a MultipartFile but the image could either be a
   * MultipartFile or a base64 encoded image. This class will only implement the methods used by
   * ComplexObsSaver#saveImageAttachment. This way we won't have to make any changes to the
   * implementation of ComplexObsSaver#saveImageAttachment and will also make very little change to
   * the AttachmentResource1_10#upload implementation. This is also helps us avoid adding an extra
   * dependency to MockMultipartFile for converting the base64 encoded String to a MultipartFile
   * object.
   */
  static final class Base64MultipartFile implements MultipartFile {

    private final String fileName;

    private final String originalFileName;

    private final String contentType;

    private final long size;

    private final byte[] bytes;

    public Base64MultipartFile(String base64Image, String fileName, String originalFileName)
        throws IOException {
      String[] parts = base64Image.split(",", 2);
      if (parts.length != 2 || !parts[0].startsWith("data:") || !parts[0].contains(";base64")) {
        throw new IllegalRequestException("The base64Content parameter must be a data URL.");
      }
      String contentType = StringUtils.substringBetween(parts[0], "data:", ";");
      if (StringUtils.isBlank(contentType)) {
        throw new IllegalRequestException(
            "The base64Content parameter must declare a content type.");
      }
      String contents = parts[1].trim();
      byte[] encodedBytes = contents.getBytes(StandardCharsets.US_ASCII);
      if (contents.isEmpty() || !Base64.isBase64(encodedBytes)) {
        throw new IllegalRequestException("The base64Content parameter is not valid base64.");
      }
      byte[] decodedImage = Base64.decodeBase64(encodedBytes);
      if (decodedImage.length == 0) {
        throw new IllegalRequestException("The base64Content parameter is empty.");
      }

      this.fileName = fileName;
      this.originalFileName = originalFileName;
      this.contentType = contentType;
      this.bytes = decodedImage;
      this.size = decodedImage.length;
    }

    @Override
    public String getName() {
      return this.fileName;
    }

    @Override
    public String getOriginalFilename() {
      return this.originalFileName;
    }

    @Override
    public String getContentType() {
      return this.contentType;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public long getSize() {
      return this.size;
    }

    @Override
    public byte[] getBytes() {
      return this.bytes;
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(File dest) throws IllegalStateException {
      throw new APIException("Operation transferTo is not supported for Base64MultipartFile");
    }
  }

  static final class OriginalFilenameMultipartFile implements MultipartFile {

    private final MultipartFile delegate;

    private final String originalFilename;

    OriginalFilenameMultipartFile(MultipartFile delegate, String originalFilename) {
      this.delegate = delegate;
      this.originalFilename = originalFilename;
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public String getOriginalFilename() {
      return originalFilename;
    }

    @Override
    public String getContentType() {
      return delegate.getContentType();
    }

    @Override
    public boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public long getSize() {
      return delegate.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
      return delegate.getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return delegate.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
      delegate.transferTo(dest);
    }
  }
}
