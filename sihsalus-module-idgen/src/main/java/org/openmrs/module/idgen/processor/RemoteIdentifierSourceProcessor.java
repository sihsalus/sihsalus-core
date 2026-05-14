/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.idgen.processor;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.RemoteIdentifierSource;
import org.openmrs.module.idgen.RemoteIdentifiersMessage;
import org.springframework.stereotype.Component;

/**
 * Evaluates a RemoteIdentifierSource
 * By default, this expects an HTTP request to return a comma-separated String of identifiers.
 * This can be overridden in subclasses as needed
 */
@Component
public class RemoteIdentifierSourceProcessor implements IdentifierSourceProcessor {

    private static Log log = LogFactory.getLog(RemoteIdentifierSourceProcessor.class);
    /**
     * @see IdentifierSourceProcessor#getIdentifiers(IdentifierSource, int)
     */
    @Override
    public List<String> getIdentifiers(IdentifierSource source, int batchSize) {
        RemoteIdentifierSource remoteIdentifierSource = (RemoteIdentifierSource) source;
        String response;
        try {
            response = doHttpPost(remoteIdentifierSource, batchSize);
        }
        catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(ex);
        }
        
        try {
        	ObjectMapper mapper = new ObjectMapper();
        	RemoteIdentifiersMessage message = mapper.readValue(response, RemoteIdentifiersMessage.class);
        	return message.getIdentifiers();
        }
        catch (IOException ex) {
        	throw new RuntimeException("Unexpected response: " + response, ex);
        }
    }

    protected String doHttpPost(RemoteIdentifierSource source, int batchSize) throws IOException, InterruptedException {
        List<FormValue> formValues = new ArrayList<>();
        formValues.add(new FormValue("numberToGenerate", Integer.toString(batchSize)));
        if (StringUtils.isNotBlank(source.getUser())) {
            formValues.add(new FormValue("username", source.getUser()));
            formValues.add(new FormValue("password", source.getPassword()));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(source.getUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(formValues)))
                .build();
        HttpResponse<String> httpResponse =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String responseText = httpResponse.body();
        int statusCode = httpResponse.statusCode();

        if (statusCode != 200) {
            throw new IOException("Unexpected response: " + statusCode + "\n" + responseText);
        }

        return responseText;
    }

    private String formEncode(List<FormValue> formValues) {
        return formValues.stream()
                .map(value -> encode(value.name()) + "=" + encode(value.value()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private record FormValue(String name, String value) {}

}
