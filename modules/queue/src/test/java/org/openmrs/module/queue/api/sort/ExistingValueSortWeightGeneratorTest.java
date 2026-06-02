package org.openmrs.module.queue.api.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openmrs.module.queue.model.QueueEntry;

class ExistingValueSortWeightGeneratorTest {

  @Test
  void generateSortWeightReturnsExistingSortWeight() {
    QueueEntry queueEntry = new QueueEntry();
    ExistingValueSortWeightGenerator generator = new ExistingValueSortWeightGenerator();

    for (double sortWeight = 0.0; sortWeight < 20.0; sortWeight += 1.0) {
      queueEntry.setSortWeight(sortWeight);

      assertEquals(sortWeight, generator.generateSortWeight(queueEntry));
    }
  }
}
