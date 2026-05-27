package org.openmrs.module.stockmanagement.api.utils;

import java.io.File;
import org.openmrs.module.stockmanagement.api.ModuleConstants;
import org.openmrs.util.OpenmrsUtil;

public class FileUtil {

  public static File getWorkingDirectory() {
    File workingDirectory =
        OpenmrsUtil.getDirectoryInApplicationDataDirectory(ModuleConstants.APP_DATA_WORKING_DIR);
    return workingDirectory;
  }

  public static File getBatchJobFolder() {
    File folder = new File(getWorkingDirectory(), "batchjob");
    if (!folder.exists()) {
      folder.mkdirs();
    }
    return folder;
  }
}
