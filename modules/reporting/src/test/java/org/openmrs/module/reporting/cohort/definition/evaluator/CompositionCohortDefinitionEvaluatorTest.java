/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.reporting.cohort.definition.evaluator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.CompositionCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.common.TestUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.test.BaseModuleContextSensitiveTest;

/** Tests the expected behavior of the CompositionCohortDefinitionEvaluator */
public class CompositionCohortDefinitionEvaluatorTest extends BaseModuleContextSensitiveTest {

  protected final Log log = LogFactory.getLog(getClass());
  protected static final String XML_DATASET_PATH = "org/openmrs/module/reporting/include/";
  protected static final String XML_REPORT_TEST_DATASET = "ReportTestDataset";

  @Before
  public void setup() throws Exception {
    executeDataSet(
        XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REPORT_TEST_DATASET));
  }

  public CompositionCohortDefinition getBaseDefinition() {
    CompositionCohortDefinition ccd = new CompositionCohortDefinition();
    ccd.addSearch(
        "c1",
        Mapped.noMappings(
            new SqlCohortDefinition(
                "select patient_id from patient where patient_id in (2,6,7,8)")));
    ccd.addSearch(
        "c2",
        Mapped.noMappings(
            new SqlCohortDefinition(
                "select patient_id from patient where patient_id in (21,22,23,24)")));
    ccd.addSearch(
        "c3",
        Mapped.noMappings(
            new SqlCohortDefinition(
                "select patient_id from patient where patient_id in (7,8,21,22)")));
    return ccd;
  }

  public void testComposition(String compositionString, Integer... expectedIds) throws Exception {
    CompositionCohortDefinition ccd = getBaseDefinition();
    ccd.setCompositionString(compositionString);
    EvaluatedCohort cohort =
        Context.getService(CohortDefinitionService.class).evaluate(ccd, new EvaluationContext());
    if (expectedIds == null) {
      Assert.assertEquals(0, cohort.size());
    } else {
      Assert.assertEquals(expectedIds.length, cohort.size());
      for (Integer expectedId : expectedIds) {
        Assert.assertTrue(cohort.contains(expectedId));
      }
    }
  }

  @Test
  public void evaluate_shouldHandleAnd() throws Exception {
    testComposition("c1 and c2");
    testComposition("c2 and c3", 21, 22);
    testComposition("c1 and c3", 7, 8);
  }

  @Test
  public void evaluate_shouldHandleOr() throws Exception {
    testComposition("c1 or c2", 2, 6, 7, 8, 21, 22, 23, 24);
    testComposition("c2 or c3", 7, 8, 21, 22, 23, 24);
    testComposition("c1 or c3", 2, 6, 7, 8, 21, 22);
  }

  @Test
  public void evaluate_shouldHandleNot() throws Exception {
    testComposition("not c1", 20, 21, 22, 23, 24);
    testComposition("c1 and not c3", 2, 6);
  }

  @Test
  public void evaluate_shouldHandleParenthesis() throws Exception {
    testComposition("(c1 or c3) and not c2", 2, 6, 7, 8);
    testComposition("(c1 or c2) and not c3", 2, 6, 23, 24);
  }
}
