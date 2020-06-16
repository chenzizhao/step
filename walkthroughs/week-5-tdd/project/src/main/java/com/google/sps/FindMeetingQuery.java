// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.*;

@SuppressWarnings("UnnecessaryReturnStatement")
public final class FindMeetingQuery {

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<TimeRange> ret = new ArrayList<>();
    if (request.getDuration() <= 0) {
      return ret;
    }
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return ret;
    }
    List<TimeRange> occupiedBlocks = recordOccupiedBlocks(events, request.getAttendees());
    // Now we need to join all blocked time ranges
    List<TimeRange> merged = union(occupiedBlocks);
    // Then take the complement
    ret = complement(TimeRange.WHOLE_DAY, merged);
    // Lastly remove time ranges that are too short
    ret.removeIf(tr -> tr.duration() < request.getDuration());
    return ret;
  }

  /* Return list of blocks occupied by attendees, unordered */
  private List<TimeRange> recordOccupiedBlocks(Collection<Event> events, Collection<String> names) {
    List<TimeRange> occupiedBlocks = new ArrayList<>();
    for (Event event : events) {
      if (isOccupied(event, names)) {
        occupiedBlocks.add(event.getWhen());
      }
    }
    return occupiedBlocks;
  }

  /* Return true if anyone in the targetNames is involved in the event */
  private boolean isOccupied(Event event, Collection<String> targetNames) {
    for (String targetName : targetNames) {
      if (event.getAttendees().contains(targetName)) {
        return true;
      }
    }
    return false;
  }

  /* Return a list of merged TimeRanges (no overlaps) */
  private List<TimeRange> union(List<TimeRange> timeRanges) {
    if (timeRanges.size() == 0 || timeRanges.size() == 1) {
      return timeRanges;
    }

    Stack<TimeRange> unionStack = new Stack<>();
    Stack<TimeRange> rawStack = new Stack<>();

    // TimeRanges are added in the order that they are returned by timeRanges' iterator
    // The earliest (by start time) TimeRange is on top.
    timeRanges.sort(TimeRange.ORDER_BY_START.reversed());
    rawStack.addAll(timeRanges);

    unionStack.push(rawStack.pop());
    while (!rawStack.isEmpty()) {
      TimeRange tr1 = unionStack.pop();
      TimeRange tr2 = rawStack.pop();
      // tr1 is at least as early as tr2
      unionHelper(tr1, tr2, unionStack);
    }
    return unionStack;
  }

  /* Union tr1 and tr2 and push the result to stack */
  private void unionHelper(TimeRange tr1, TimeRange tr2, Stack<TimeRange> stack) {
    // tr1 is at least as early as tr2
    if (tr1 == tr2) {
      //|--tr1--|
      //|--tr2--|
      stack.push(tr1);
    } else if (tr1.contains(tr2)) {
      //|----tr1----|
      // |--tr2--|
      stack.push(tr1);
    } else if (tr2.contains(tr1)) {
      //|--tr1--|
      //|----tr2----|
      stack.push(tr2);
    } else if (tr1.end() >= tr2.start()) {
      //|--tr1--|
      //      |--tr2--|
      //        |--tr2--|
      // merge two blocks
      TimeRange newTr = TimeRange.fromStartEnd(tr1.start(), tr2.end(), false);
      stack.push(newTr);
    } else {
      //|--tr1--|
      //          |--tr2--|
      // push the early block first
      stack.push(tr1);
      stack.push(tr2);
    }
  }

  /* Return a list of the available TimeRanges */
  private List<TimeRange> complement(TimeRange wholeDay, List<TimeRange> mergedTimeRanges) {
    Stack<TimeRange> complStack = new Stack<>();
    Stack<TimeRange> rawStack = new Stack<>();

    // TimeRanges are added in the order that they are returned by mergedTimeRanges's iterator
    // The earliest (by start time) TimeRange is on top.
    mergedTimeRanges.sort(TimeRange.ORDER_BY_START.reversed());
    rawStack.addAll(mergedTimeRanges);

    complStack.push(wholeDay);
    while (!rawStack.isEmpty()) {
      TimeRange tr1 = complStack.pop();
      TimeRange tr2 = rawStack.pop();
      // tr1 contains or equals tr2
      complHelper(tr1, tr2, complStack);
    }
    return complStack;
  }

  /* Compute the difference tr1\tr2, and push the result to the stack */
  private void complHelper(TimeRange tr1, TimeRange tr2, Stack<TimeRange> stack) {
    if (!tr1.contains(tr2) || tr1 == tr2) {
      return;
      // tr1 should contains and not equal to tr2 from now on
    } else if (tr1.start() == tr2.start()) {
      // |----tr1----|
      // |--tr2--|
      TimeRange newTr = TimeRange.fromStartEnd(tr2.end(), tr1.end(), true);
      stack.push(newTr);
    } else if (tr1.end() == tr2.end()) {
      // |----tr1----|
      //     |--tr2--|
      TimeRange newTr = TimeRange.fromStartEnd(tr1.start(), tr2.start(), false);
      stack.push(newTr);
    } else {
      // |----tr1----|
      //   |--tr2--|
      // tr1 contains tr2 exclusively
      TimeRange newTr1 = TimeRange.fromStartEnd(tr1.start(), tr2.start(), false);
      TimeRange newTr2 = TimeRange.fromStartEnd(tr2.end(), tr1.end(), false);
      // push early gap first
      stack.push(newTr1);
      stack.push(newTr2);
    }
  }

}
