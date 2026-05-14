package org.hibernate.criterion;

import java.util.Locale;

public enum MatchMode {
	EXACT,
	START,
	END,
	ANYWHERE;
	
	String toMatchString(String value) {
		String lowered = value == null ? "" : value.toLowerCase(Locale.ROOT);
		return switch (this) {
			case EXACT -> lowered;
			case START -> lowered + "%";
			case END -> "%" + lowered;
			case ANYWHERE -> "%" + lowered + "%";
		};
	}
}
