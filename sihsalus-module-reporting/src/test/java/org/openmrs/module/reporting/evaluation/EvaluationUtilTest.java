package org.openmrs.module.reporting.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EvaluationUtilTest {

	@Test
	void parseParameterNameFromExpressionReturnsNameBeforeOperations() {
		assertEquals("report.startDate", EvaluationUtil.parseParameterNameFromExpression("${report.startDate+5d-1w}"));
	}

	@Test
	void evaluateParameterExpressionAppliesIntegerOperationsInOrder() {
		Object evaluated = EvaluationUtil.evaluateParameterExpression("count+5*2-8", Map.of("count", 5));

		assertEquals(12, evaluated);
	}

	@Test
	void evaluateParameterExpressionAppliesDoubleOperations() {
		Object evaluated = EvaluationUtil.evaluateParameterExpression("count+2.5", Map.of("count", 2));

		assertEquals(4.5, (Double) evaluated, 0.0001);
	}

	@Test
	void evaluateParameterExpressionAppliesDateOperations() {
		Object evaluated = EvaluationUtil.evaluateParameterExpression("start+1d", Map.of("start", new Date(0)));

		assertEquals(new Date(86400000), evaluated);
	}
}
