package org.openmrs.module.queue.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QueueTest {

  @Test
  void addQueueRoomAddsNewQueueRoomWhenQueueRoomsIsNull() {
    Queue queue = new Queue();
    queue.setQueueRooms(null);
    QueueRoom queueRoom = new QueueRoom();

    queue.addQueueRoom(queueRoom);

    assertEquals(1, queue.getQueueRooms().size());
    assertTrue(queue.getQueueRooms().contains(queueRoom));
  }

  @Test
  void getActiveQueueRoomsDoesNotReturnRetiredQueueRooms() {
    Queue queue = new Queue();
    QueueRoom queueRoom = new QueueRoom();

    queue.addQueueRoom(queueRoom);

    assertIterableEquals(queue.getQueueRooms(), queue.getActiveQueueRooms());
    queueRoom.setRetired(true);
    assertTrue(queue.getQueueRooms().contains(queueRoom));
    assertFalse(queue.getActiveQueueRooms().contains(queueRoom));
  }

  @Test
  void getActiveQueueRoomsReturnsEmptyListWhenQueueRoomsIsNull() {
    Queue queue = new Queue();
    queue.setQueueRooms(null);

    assertTrue(queue.getActiveQueueRooms().isEmpty());
  }
}
