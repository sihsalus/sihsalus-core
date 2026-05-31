package org.openmrs.module.serialization.xstream.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class CollectionCompatibilityConverterTest {

  private final CollectionCompatibilityConverter converter = new CollectionCompatibilityConverter();

  @Test
  void compatibilityCopyPreservesSortedMapComparatorAndEntries() {
    Comparator<String> comparator = Comparator.reverseOrder();
    SortedMap<String, Integer> source = new TreeMap<>(comparator);
    source.put("a", 1);
    source.put("b", 2);

    Object copy = converter.compatibilityCopy(source);

    assertTrue(copy instanceof TreeMap<?, ?>);
    TreeMap<?, ?> treeMap = (TreeMap<?, ?>) copy;
    assertSame(comparator, treeMap.comparator());
    assertEquals(source, treeMap);
  }

  @Test
  void compatibilityCopyPreservesSortedSetComparatorAndValues() {
    Comparator<String> comparator = Comparator.reverseOrder();
    SortedSet<String> source = new TreeSet<>(comparator);
    source.addAll(List.of("a", "b"));

    Object copy = converter.compatibilityCopy(source);

    assertTrue(copy instanceof TreeSet<?>);
    TreeSet<?> treeSet = (TreeSet<?>) copy;
    assertSame(comparator, treeSet.comparator());
    assertEquals(source, treeSet);
  }

  @Test
  void compatibilityCopyConvertsMapToHashMap() {
    Map<String, Integer> source = new LinkedHashMap<>();
    source.put("a", 1);
    source.put("b", 2);

    Object copy = converter.compatibilityCopy(source);

    assertTrue(copy instanceof HashMap<?, ?>);
    assertEquals(source, copy);
  }

  @Test
  void compatibilityCopyConvertsSetToHashSet() {
    Set<String> source = new LinkedHashSet<>(List.of("a", "b"));

    Object copy = converter.compatibilityCopy(source);

    assertTrue(copy instanceof HashSet<?>);
    assertEquals(source, copy);
  }

  @Test
  void compatibilityCopyConvertsListsAndCollectionsToArrayList() {
    List<String> list = new LinkedList<>(List.of("a", "b"));
    Collection<String> collection = new ArrayDeque<>(List.of("c", "d"));

    Object listCopy = converter.compatibilityCopy(list);
    Object collectionCopy = converter.compatibilityCopy(collection);

    assertTrue(listCopy instanceof ArrayList<?>);
    assertEquals(list, listCopy);
    assertTrue(collectionCopy instanceof ArrayList<?>);
    assertEquals(new ArrayList<>(collection), collectionCopy);
  }
}
