package org.bahmni.module.teleconsultation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.BaseModuleActivator;

public class TeleconsultationActivator extends BaseModuleActivator {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	public void started() {
		log.info("Started Teleconsultation");
	}
	
	public void shutdown() {
		log.info("Shutdown Teleconsultation");
	}
	
}
