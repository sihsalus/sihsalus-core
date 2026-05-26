package org.openmrs.module.stockmanagement.api.dto.reporting;

public enum Fulfillment {
  All(),
  Full(),
  Partial(),
  None;

  public static Fulfillment findByName(String name) {
    return findInList(name, values());
  }

  public static Fulfillment findInList(String name, Fulfillment[] parameterList) {
    Fulfillment result = null;
    for (Fulfillment enumValue : parameterList) {
      if (enumValue.name().equalsIgnoreCase(name)) {
        result = enumValue;
        break;
      }
    }
    return result;
  }
}
