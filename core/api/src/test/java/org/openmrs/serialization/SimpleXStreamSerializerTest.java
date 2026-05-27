package org.openmrs.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SimpleXStreamSerializerTest {

  @Test
  void deserializeAllowsWhitelistedString() throws SerializationException {
    SimpleXStreamSerializer serializer = new SimpleXStreamSerializer();

    assertEquals("safe", serializer.deserialize("<string>safe</string>", String.class));
  }

  @Test
  void deserializeRejectsTypesOutsideTheWhitelist() throws SerializationException {
    SimpleXStreamSerializer serializer = new SimpleXStreamSerializer();

    assertThrows(
        SerializationException.class,
        () -> serializer.deserialize("<java.lang.ProcessBuilder/>", ProcessBuilder.class));
  }

  @Test
  void deserializeRejectsUnexpectedResultType() throws SerializationException {
    SimpleXStreamSerializer serializer = new SimpleXStreamSerializer();

    assertThrows(
        SerializationException.class,
        () -> serializer.deserialize("<string>safe</string>", Integer.class));
  }
}
