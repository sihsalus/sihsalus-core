package org.openmrs.module.webservices.rest.web;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RestUtilTest {

	@Test
	void ipMatchesSupportsValidCidrMask() {
		assertTrue(RestUtil.ipMatches("192.168.0.12", List.of("192.168.0.0/24")));
	}

	@Test
	void ipMatchesRejectsNonNumericMask() {
		assertThrows(IllegalArgumentException.class,
		    () -> RestUtil.ipMatches("192.168.0.12", List.of("192.168.0.0/not-a-mask")));
	}

	@Test
	void ipMatchesRejectsNegativeMask() {
		assertThrows(IllegalArgumentException.class, () -> RestUtil.ipMatches("10.0.0.1", List.of("192.168.0.0/-1")));
	}

	@Test
	void ipMatchesRejectsMaskLargerThanAddressSize() {
		assertThrows(IllegalArgumentException.class, () -> RestUtil.ipMatches("192.168.0.12", List.of("192.168.0.0/33")));
	}
}
