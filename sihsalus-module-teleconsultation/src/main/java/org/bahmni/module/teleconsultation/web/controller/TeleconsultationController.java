package org.bahmni.module.teleconsultation.web.controller;

import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/teleconsultation")
public class TeleconsultationController extends BaseRestController {
	
	@Autowired
	private TeleconsultationService teleconsultationService;
	
	@RequestMapping(value = "/generateLink", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> generateTeleconsultationLink(@RequestParam(value = "uuid", required = true) String uuid) {
        return new ResponseEntity<>(teleconsultationService.generateTeleconsultationLink(uuid), HttpStatus.OK);
    }
}
