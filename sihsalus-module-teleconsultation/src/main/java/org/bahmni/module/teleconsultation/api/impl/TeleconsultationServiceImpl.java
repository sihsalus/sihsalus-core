package org.bahmni.module.teleconsultation.api.impl;

import org.bahmni.module.teleconsultation.api.TeleconsultationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.util.PrivilegeConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

public class TeleconsultationServiceImpl extends BaseOpenmrsService implements TeleconsultationService {
	
	private final static String PROP_TC_SERVER = "bahmni.appointment.teleConsultation.serverUrlPattern";
	
	private final static String DEFAULT_TC_SERVER_URL_PATTERN = "https://meet.jit.si/{0}";

	@Override
	public String generateTeleconsultationLink(String uuid) {
		if (uuid == null || uuid.trim().isEmpty()) {
			throw new IllegalArgumentException("Teleconsultation uuid is required");
		}
		String tcServerUrl = getTeleconsultationServerUrl();
		if (tcServerUrl == null || tcServerUrl.isEmpty()) {
			tcServerUrl = DEFAULT_TC_SERVER_URL_PATTERN;
		}
		return new MessageFormat(tcServerUrl).format(new Object[] { encodeRoomId(uuid.trim()) });
	}

	private String getTeleconsultationServerUrl() {
		boolean addedProxyPrivilege = Context.isAuthenticated()
		        && !Context.hasPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		if (addedProxyPrivilege) {
			Context.addProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
		}
		try {
			return Context.getAdministrationService().getGlobalProperty(PROP_TC_SERVER);
		}
		finally {
			if (addedProxyPrivilege) {
				Context.removeProxyPrivilege(PrivilegeConstants.GET_GLOBAL_PROPERTIES);
			}
		}
	}

	private String encodeRoomId(String roomId) {
		try {
			return URLEncoder.encode(roomId, StandardCharsets.UTF_8.name()).replace("+", "%20");
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("UTF-8 is not available", e);
		}
	}
	
}
