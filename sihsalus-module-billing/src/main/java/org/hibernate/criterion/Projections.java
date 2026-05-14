package org.hibernate.criterion;

public final class Projections {
	
	private static final Projection ROW_COUNT = new RowCountProjection();
	
	private Projections() {
	}
	
	public static Projection rowCount() {
		return ROW_COUNT;
	}
	
	public static boolean isRowCount(Projection projection) {
		return projection instanceof RowCountProjection;
	}
	
	private static final class RowCountProjection implements Projection {
	}
}
