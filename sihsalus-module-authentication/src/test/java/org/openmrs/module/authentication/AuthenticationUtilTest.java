package org.openmrs.module.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthenticationUtilTest {

	@Test
	void getIntegerReturnsDefaultForInvalidNumber() {
		assertEquals(7, AuthenticationUtil.getInteger("not-a-number", 7));
	}

	@Test
	void getIntegerReturnsParsedNumber() {
		assertEquals(42, AuthenticationUtil.getInteger("42", 7));
	}
}
