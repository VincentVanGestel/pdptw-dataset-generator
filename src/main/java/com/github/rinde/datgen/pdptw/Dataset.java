/*
 * Copyright (C) 2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.datgen.pdptw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.TreeBasedTable;

final class Dataset<T> implements Iterable<T> {
  Comparator<T> comparator;
  SortedMap<Double, RowSortedTable<Long, Double, SortedSet<T>>> data;
  Set<T> valuesSet;

  private Dataset(Comparator<T> comp) {
    comparator = comp;

    data = new TreeMap<>();
    valuesSet = new HashSet<>();
  }

  static <T extends Comparable<T>> Dataset<T> naturalOrder() {
    return new Dataset<>(Ordering.<T>natural());
  }

  static <T> Dataset<T> orderedBy(Comparator<T> comparator) {
    return new Dataset<>(comparator);
  }

  void put(double dyn, long urg, double scl, T value) {
    synchronized (data) {
      // checkArgument(!valuesSet.contains(value), "Value %s already exists.",
      // value); // TODO sorry, to heavy for large scenarios
      if (!data.containsKey(dyn)) {
        data.put(dyn, TreeBasedTable.<Long, Double, SortedSet<T>>create());
      }
      if (!data.get(dyn).contains(urg, scl)) {
        data.get(dyn).put(urg, scl, new TreeSet<>(comparator));
      }

      // checkArgument(!data.get(dyn).get(urg, scl).contains(value),
      // "At (%s,%s,%s) value %s already exists.", dyn, urg, scl, value); //
      // TODO Same here
      data.get(dyn).get(urg, scl).add(value);
      valuesSet.add(value);
    }
  }

  boolean containsEntry(double dyn, long urg, double scl, T value) {
    return data.containsKey(dyn)
      && data.get(dyn).contains(urg, scl)
      && data.get(dyn).get(urg, scl).contains(value);
  }

  SortedSet<T> get(double dyn, long urg, double scl) {
    if (!data.containsKey(dyn) || !data.get(dyn).contains(urg, scl)) {
      return ImmutableSortedSet.of();
    }
    return Collections.unmodifiableSortedSet(data.get(dyn).get(urg, scl));
  }

  int size() {
    return valuesSet.size();
  }

  @Override
  public int hashCode() {
    return Objects.hash(comparator, data, valuesSet);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof Dataset<?>)) {
      return false;
    }

    final Dataset<?> o = (Dataset<?>) other;
    return Iterators.elementsEqual(iterator(), o.iterator());
  }

  @Override
  public String toString() {
    return Iterators.toString(iterator());
  }

  @Override
  public Iterator<T> iterator() {
    final List<Iterator<T>> its = new ArrayList<>();
    for (final Entry<Double, RowSortedTable<Long, Double, SortedSet<T>>> entry : data
      .entrySet()) {
      for (final SortedSet<T> set : entry.getValue().values()) {
        its.add(set.iterator());
      }
    }
    return Iterators.concat(its.iterator());
  }
}
