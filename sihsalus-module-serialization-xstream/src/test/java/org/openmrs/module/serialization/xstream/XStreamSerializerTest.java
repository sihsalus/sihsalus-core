package org.openmrs.module.serialization.xstream;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thoughtworks.xstream.security.ForbiddenClassException;
import org.junit.jupiter.api.Test;
import org.openmrs.serialization.SerializationException;

class XStreamSerializerTest {
	
	@Test
	void constructorInitializesXStreamSecurityOutsideSpring() throws SerializationException {
		XStreamSerializer serializer = new XStreamSerializer();
		
		assertThrows(ForbiddenClassException.class, () -> serializer.getXstream().fromXML("<java.lang.ProcessBuilder/>"));
	}
}
