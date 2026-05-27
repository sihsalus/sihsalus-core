/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.attachments;

import static org.openmrs.module.attachments.AttachmentsContext.getCompressionRatio;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.ConceptComplex;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.Visit;
import org.openmrs.module.attachments.AttachmentsConstants.ContentFamily;
import org.openmrs.module.attachments.obs.ComplexDataHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component(AttachmentsConstants.COMPONENT_COMPLEXOBS_SAVER)
public class ComplexObsSaver {

  protected final Log log = LogFactory.getLog(getClass());

  @Autowired
  @Qualifier(AttachmentsConstants.COMPONENT_ATT_CONTEXT)
  protected AttachmentsContext context;

  @Autowired
  @Qualifier(AttachmentsConstants.COMPONENT_COMPLEXDATA_HELPER)
  protected ComplexDataHelper complexDataHelper;

  @Autowired
  @Qualifier(AttachmentsConstants.COMPONENT_VISIT_COMPATIBILITY)
  protected VisitCompatibility visitCompatibility;

  protected Obs obs = new Obs();

  protected ConceptComplex conceptComplex;

  public Obs getObs() {
    return obs;
  }

  protected void prepareComplexObs(
      Visit visit,
      Person person,
      Encounter encounter,
      String fileCaption,
      String formFieldNamespace,
      String formFieldPath) {
    obs =
        new Obs(
            person,
            conceptComplex,
            visit == null || visit.getStopDatetime() == null ? new Date() : visit.getStopDatetime(),
            encounter != null ? encounter.getLocation() : null);
    obs.setEncounter(encounter); // may be null
    obs.setComment(fileCaption);
    if (StringUtils.isNotBlank(formFieldNamespace) && StringUtils.isNotBlank(formFieldPath)) {
      obs.setFormField(formFieldNamespace, formFieldPath);
    }
  }

  public Obs saveImageAttachment(
      Visit visit,
      Person person,
      Encounter encounter,
      String fileCaption,
      MultipartFile multipartFile,
      String instructions,
      String formFieldNamespace,
      String formFieldPath)
      throws IOException {

    conceptComplex = context.getConceptComplex(ContentFamily.IMAGE);
    prepareComplexObs(visit, person, encounter, fileCaption, formFieldNamespace, formFieldPath);

    Object image = multipartFile.getBytes();
    double compressionRatio =
        getCompressionRatio(multipartFile.getSize(), 1000000 * context.getMaxStorageFileSize());
    if (compressionRatio < 1) {
      BufferedImage sourceImage = ImageIO.read(multipartFile.getInputStream());
      if (sourceImage == null) {
        throw new IOException("Failed to read the uploaded image.");
      }
      image = scaleImage(sourceImage, compressionRatio);
    }
    obs.setComplexData(
        complexDataHelper
            .build(
                instructions,
                sanitizeFilename(multipartFile.getOriginalFilename()),
                image,
                multipartFile.getContentType())
            .asComplexData());
    obs = context.getObsService().saveObs(obs, getClass().toString());
    return obs;
  }

  public Obs saveOtherAttachment(
      Visit visit,
      Person person,
      Encounter encounter,
      String fileCaption,
      MultipartFile multipartFile,
      String instructions,
      String formFieldNamespace,
      String formFieldPath)
      throws IOException {
    conceptComplex = context.getConceptComplex(ContentFamily.OTHER);
    prepareComplexObs(visit, person, encounter, fileCaption, formFieldNamespace, formFieldPath);

    obs.setComplexData(
        complexDataHelper
            .build(
                instructions,
                sanitizeFilename(multipartFile.getOriginalFilename()),
                multipartFile.getBytes(),
                multipartFile.getContentType())
            .asComplexData());
    obs = context.getObsService().saveObs(obs, getClass().toString());
    return obs;
  }

  // since we use | as a separator in filenames, we need to replace it with _
  private String sanitizeFilename(String filename) {
    String safeName = FilenameUtils.getName(filename);
    return safeName != null ? safeName.replaceAll(Pattern.quote("|"), "_") : null;
  }

  private BufferedImage scaleImage(BufferedImage image, double ratio) {
    int width = Math.max(1, (int) Math.round(image.getWidth() * ratio));
    int height = Math.max(1, (int) Math.round(image.getHeight() * ratio));
    BufferedImage scaled =
        new BufferedImage(
            width, height, image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType());
    Graphics2D graphics = scaled.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.drawImage(image, 0, 0, width, height, null);
    } finally {
      graphics.dispose();
    }
    return scaled;
  }
}
