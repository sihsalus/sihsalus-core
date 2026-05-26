package org.openmrs.module.attachments.obs;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.FilenameUtils;
import org.openmrs.Obs;
import org.openmrs.module.attachments.AttachmentsConstants;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.ComplexObsHandler;
import org.openmrs.obs.handler.ImageHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class ImageAttachmentHandler extends AbstractAttachmentHandler {

  public ImageAttachmentHandler() {
    super();
  }

  @Autowired protected ImageHandler imageHandler;

  @Override
  protected ComplexObsHandler getParent() {
    return imageHandler;
  }

  @Override
  protected ComplexData readComplexData(Obs obs, ValueComplex valueComplex, String view) {

    Obs tmpObs = new Obs();

    if (valueComplex.getKey() == null) {
      // old pre-Core 2.8
      if (view.equals(AttachmentsConstants.ATT_VIEW_THUMBNAIL)
          && storageService.exists(appendThumbnailSuffix(valueComplex.getFileName()))) {
        tmpObs.setValueComplex(appendThumbnailSuffix(valueComplex.getFileName()));
      } else {
        tmpObs.setValueComplex(valueComplex.getSimplifiedValueComplex());
      }
    } else {
      // Core 2.8 and above
      if (view.equals(AttachmentsConstants.ATT_VIEW_THUMBNAIL)
          && storageService.exists(appendThumbnailSuffix(valueComplex.getKey()))) {
        tmpObs.setValueComplex(
            ValueComplex.buildSimplifiedValueComplex(
                valueComplex.getFileName(), appendThumbnailSuffix(valueComplex.getKey())));
      } else {
        tmpObs.setValueComplex(valueComplex.getSimplifiedValueComplex());
      }
    }

    // We invoke the parent to inherit from the file reading routines.
    tmpObs =
        getParent()
            .getObs(tmpObs, AttachmentsConstants.IMAGE_HANDLER_VIEW); // ImageHandler doesn't handle
    // several views
    ComplexData complexData = tmpObs.getComplexData();

    // Then we build our own custom complex data
    return getComplexDataHelper()
        .build(
            valueComplex.getInstructions(),
            complexData.getTitle(),
            complexData.getData(),
            valueComplex.getMimeType())
        .asComplexData();
  }

  @Override
  protected boolean deleteComplexData(Obs obs) {

    // first delete the thumbnail if it exists
    Boolean isThumbnailPurged = null;
    String thumbnailDataKey = appendThumbnailSuffix(((ImageHandler) getParent()).parseDataKey(obs));

    try {
      if (storageService.exists(thumbnailDataKey)) {
        isThumbnailPurged = storageService.purgeData(thumbnailDataKey);
      } else {
        isThumbnailPurged = true;
      }
    } catch (IOException e) {
      log.error("Failed to purge thumbnail file: " + thumbnailDataKey, e);
      isThumbnailPurged = false;
    }

    boolean isImagePurged = getParent().purgeComplexData(obs);
    return isThumbnailPurged && isImagePurged;
  }

  @Override
  protected ValueComplex saveComplexData(Obs obs, AttachmentComplexData complexData) {

    // We invoke the parent to inherit from the file saving routines
    obs = getParent().saveObs(obs);

    // now use the parent method to fetch the complex data, the assumption is it
    // will return a BufferedImage
    // (since that is what ImageHandler getObs in Core returns)
    obs = getParent().getObs(obs, AttachmentsConstants.IMAGE_HANDLER_VIEW);

    // Get image dimensions
    BufferedImage image = (BufferedImage) obs.getComplexData().getData();
    int imageHeight = image.getHeight();
    int imageWidth = image.getWidth();
    saveThumbnailIfNeeded(obs, imageHeight, imageWidth);

    return new ValueComplex(
        complexData.getInstructions(), complexData.getMimeType(), obs.getValueComplex());
  }

  /**
   * If the image is over a certain dimension, it will create a small thumbnail file alongside the
   * original file to be used as thumbnail image.
   *
   * @param obs original obs
   * @param imageHeight image height
   * @param imageWidth image width
   */
  public void saveThumbnailIfNeeded(Obs obs, int imageHeight, int imageWidth) {
    if ((imageHeight <= THUMBNAIL_MAX_HEIGHT) && (imageWidth <= THUMBNAIL_MAX_WIDTH)) {
      return;
    } else {
      String key = appendThumbnailSuffix(((ImageHandler) getParent()).parseDataKey(obs));
      try {
        storageService.saveData(
            outputStream -> {
              Object data = obs.getComplexData().getData();
              if (!(data instanceof BufferedImage)) {
                throw new IllegalArgumentException(
                    "Expected a BufferedImage, but got " + data.getClass().getName());
              }
              BufferedImage image = (BufferedImage) data;
              BufferedImage thumbnail =
                  scaleImage(image, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
              ImageIO.write(thumbnail, getImageFormat(obs), outputStream);
            },
            null,
            null,
            key);
        // the above is a bit of hack... we pass in the entire value we want use as a
        // key instead of specifying the filename, module ID and key suffix and having
        // the storage service to generate the full key for us
        // this is because we need to be able to recreate the key based on the key of
        // the main image
      } catch (IOException e) {
        log.error("Failed to save thumbnail file: " + key, e);
      }
    }
  }

  private static BufferedImage scaleImage(BufferedImage image, int maxWidth, int maxHeight) {
    double ratio =
        Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
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

  private static String getImageFormat(Obs obs) {
    String mimeType = obs.getComplexData().getMimeType();
    if (mimeType != null && mimeType.startsWith("image/")) {
      return mimeType.substring("image/".length()).toLowerCase();
    }
    String extension = FilenameUtils.getExtension(obs.getComplexData().getTitle());
    return extension == null || extension.isBlank() ? "png" : extension.toLowerCase();
  }
}
