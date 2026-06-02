package org.openmrs.module.queue.api.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.model.QueueEntry;

class BasicPrioritySortWeightGeneratorTest {

  @Test
  void generateSortWeightReturnsConfiguredPriorityPosition() {
    Concept concept1 = new Concept(1);
    Concept concept2 = new Concept(2);
    Concept concept3 = new Concept(3);
    QueueEntry queueEntry = new QueueEntry();
    BasicPrioritySortWeightGenerator generator =
        new BasicPrioritySortWeightGenerator(
            servicesReturningPriorities(List.of(concept1, concept2, concept3)));

    assertEquals(0.0, generator.generateSortWeight(queueEntry));
    queueEntry.setPriority(concept1);
    assertEquals(0.0, generator.generateSortWeight(queueEntry));
    queueEntry.setPriority(concept2);
    assertEquals(1.0, generator.generateSortWeight(queueEntry));
    queueEntry.setPriority(concept3);
    assertEquals(2.0, generator.generateSortWeight(queueEntry));
  }

  @Test
  void generateSortWeightDefaultsToLowestPriorityWhenPriorityIsNotAllowed() {
    QueueEntry queueEntry = new QueueEntry();
    queueEntry.setPriority(new Concept(99));
    BasicPrioritySortWeightGenerator generator =
        new BasicPrioritySortWeightGenerator(servicesReturningPriorities(List.of(new Concept(1))));

    assertEquals(0.0, generator.generateSortWeight(queueEntry));
  }

  private QueueServicesWrapper servicesReturningPriorities(List<Concept> priorities) {
    return new QueueServicesWrapper(null, null, null, null, null, null, null, null, null, null) {
      @Override
      public List<Concept> getAllowedPriorities(Queue queue) {
        return priorities;
      }
    };
  }
}
