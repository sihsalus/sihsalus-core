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

package org.openmrs.module.imaging.api.client;

import org.apache.commons.io.IOUtils;
import org.openmrs.module.imaging.OrthancConfiguration;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public class OrthancHttpClient {

	private static final int CONNECT_TIMEOUT_MS = 5000;

	private static final int READ_TIMEOUT_MS = 30000;

	private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
	
	public HttpURLConnection createConnection(String method, String url, String path, String username, String password)
	        throws IOException {
		URI baseUri = validateBaseUrl(url);
		URI pathUri = validateRelativePath(path);
		URI serverUri = baseUri.resolve(pathUri).normalize();
		if (!sameAuthority(baseUri, serverUri)) {
			throw new IOException("Orthanc request path must stay on the configured Orthanc server");
		}
		String encoding = Base64.getEncoder().encodeToString((nullToEmpty(username) + ":" + nullToEmpty(password))
		        .getBytes(StandardCharsets.UTF_8));
		URL serverURL = serverUri.toURL();
		HttpURLConnection con = (HttpURLConnection) serverURL.openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("Authorization", "Basic " + encoding);
		con.setUseCaches(false);
		con.setConnectTimeout(CONNECT_TIMEOUT_MS);
		con.setReadTimeout(READ_TIMEOUT_MS);
		return con;
	}
	
	/**
	 * @param con http url request connection
	 * @param query the query string
	 * @throws IOException IO exception
	 */
	public void sendOrthancQuery(HttpURLConnection con, String query) throws IOException {
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty( "charset", "utf-8");
        con.setDoOutput(true);
        byte[] data = query.getBytes(StandardCharsets.UTF_8);
        con.setRequestProperty( "Content-Length", Integer.toString(data.length));
        try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.write(data);
        }
    }
	
	/**
	 * @param config the orthanc server configuration
	 * @param con the http url connection
	 * @throws IOException the IO exception
	 */
	public static void throwConnectionException(OrthancConfiguration config, HttpURLConnection con) throws IOException {
		String errorMessage;
		try {
			InputStream errorStream = con.getErrorStream();
			if (errorStream != null) {
				errorMessage = IOUtils.toString(errorStream, StandardCharsets.UTF_8);
			} else {
				errorMessage = "Unknown error";
			}
		}
		catch (IOException e) {
			errorMessage = "Failed to read error stream: " + e.getMessage();
		}
		
		errorMessage = abbreviate(errorMessage);
		throw new IOException("Request to Orthanc server " + redactUrl(config.getOrthancBaseUrl()) + " failed with error: "
		        + errorMessage);
	}
	
	/**
	 * @param config
	 * @return
	 */
	public boolean isOrthancReachable(OrthancConfiguration config) {
		try {
			HttpURLConnection connection = createConnection("GET", config.getOrthancBaseUrl(), "/system",
			    config.getOrthancUsername(), config.getOrthancPassword());
			int responseCode = connection.getResponseCode();
			connection.disconnect();
			return responseCode == 200;
		}
		catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * @param url the Url
	 * @param username the user name
	 * @param password the password
	 * @return the response status
	 * @throws IOException the IO exception
	 */
	public int testOrthancConnection(String url, String username, String password) throws IOException {
		HttpURLConnection con = createConnection("GET", url, "/system", username, password);
		int status = con.getResponseCode();
		con.disconnect();
		return status;
	}

	public static String encodePathSegment(String value) {
		if (value == null) {
			return "";
		}
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}
	
	public int getStatus(HttpURLConnection con) throws IOException {
		return con.getResponseCode();
	}
	
	public InputStream getResponseStream(HttpURLConnection con) throws IOException {
		return con.getInputStream();
	}
	
	public String getErrorMessage(HttpURLConnection con) throws IOException {
		return con.getResponseMessage();
	}

	private static URI validateBaseUrl(String url) throws IOException {
		if (url == null || url.trim().isEmpty()) {
			throw new IOException("Orthanc base URL is required");
		}
		try {
			URI uri = new URI(url.trim());
			String scheme = uri.getScheme();
			if (scheme == null || uri.getHost() == null ||
			        !("http".equals(scheme.toLowerCase(Locale.ROOT)) ||
			                "https".equals(scheme.toLowerCase(Locale.ROOT)))) {
				throw new IOException("Orthanc base URL must be a valid HTTP(S) URL");
			}
			if (uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
				throw new IOException("Orthanc base URL must not include credentials, query, or fragment");
			}
			return uri;
		}
		catch (URISyntaxException e) {
			throw new IOException("Orthanc base URL must be a valid HTTP(S) URL", e);
		}
	}

	private static URI validateRelativePath(String path) throws IOException {
		if (path == null || path.trim().isEmpty()) {
			return URI.create("/");
		}
		URI pathUri;
		try {
			pathUri = URI.create(path);
		}
		catch (IllegalArgumentException e) {
			throw new IOException("Orthanc request path must be a valid URI path", e);
		}
		if (pathUri.isAbsolute() || path.startsWith("//")) {
			throw new IOException("Orthanc request path must be relative");
		}
		return pathUri;
	}

	private static boolean sameAuthority(URI expectedBase, URI actual) {
		return expectedBase.getScheme().equalsIgnoreCase(actual.getScheme()) &&
		        expectedBase.getHost().equalsIgnoreCase(actual.getHost()) &&
		        effectivePort(expectedBase) == effectivePort(actual);
	}

	private static int effectivePort(URI uri) {
		if (uri.getPort() != -1) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private static String redactUrl(String url) {
		try {
			URI uri = new URI(url);
			return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
		}
		catch (Exception e) {
			return "<invalid Orthanc URL>";
		}
	}

	private static String abbreviate(String message) {
		if (message == null || message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "...";
	}
}
