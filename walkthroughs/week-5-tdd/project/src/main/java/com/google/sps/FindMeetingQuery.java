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

  private List<TimeRange> complement(TimeRange wholeDay, List<TimeRange> merged) {
    // earliest on top
    Stack<TimeRange> blockStack = new Stack<>();
    merged.sort(TimeRange.ORDER_BY_START.reversed());
    blockStack.addAll(merged);

    Stack<TimeRange> complStack = new Stack<>();
    complStack.push(wholeDay);

    while (!blockStack.isEmpty()) {
      TimeRange tr1 = complStack.pop();
      TimeRange tr2 = blockStack.pop();
      complHelper(tr1, tr2, complStack);
    }
    return complStack;
  }

  private void complHelper(TimeRange tr1, TimeRange tr2, Stack<TimeRange> stack) {
    // tr1 contains tr2
    // this function computes the time range tr1\tr2, and push the result to the stack
    if (!tr1.contains(tr2) || tr1 == tr2) {
      return;
    } else if (tr1.start() == tr2.start()) {
      TimeRange newTr = TimeRange.fromStartEnd(tr2.end(), tr1.end(), true);
      stack.push(newTr);
    } else if (tr1.end() == tr2.end()) {
      TimeRange newTr = TimeRange.fromStartEnd(tr1.start(), tr2.start(), true);
      stack.push(newTr);
    } else {
      // tr1 contains tr2 exclusively
      TimeRange newTr1 = TimeRange.fromStartEnd(tr1.start(), tr2.start(), true);
      TimeRange newTr2 = TimeRange.fromStartEnd(tr2.end(), tr1.end(), true);
      // push early gap first
      stack.push(newTr1);
      stack.push(newTr2);
    }
  }

  private List<TimeRange> union(List<TimeRange> blocks) {
    if (blocks.size() == 0 || blocks.size() == 1) {
      return blocks;
    }

    Stack<TimeRange> unionBlocksStack = new Stack<>();
    Stack<TimeRange> blockStack = new Stack<>();
    // Earliest on top
    blockStack.addAll(blocks);

    unionBlocksStack.push(blockStack.pop());
    while (!blockStack.isEmpty()) {
      TimeRange tr1 = unionBlocksStack.pop();
      TimeRange tr2 = blockStack.pop();
      // tr1 is at least as early as tr2
      unionHelper(tr1, tr2, unionBlocksStack);
    }
    return unionBlocksStack;
  }

  private void unionHelper(TimeRange tr1, TimeRange tr2, Stack<TimeRange> stack) {
    // tr1 is at least as early as tr2
    if (tr1 == tr2) {
      stack.push(tr1);
    } else if (tr1.contains(tr2)) {
      stack.push(tr1);
    } else if (tr2.contains(tr1)) {
      stack.push(tr2);
    } else if (tr1.end() >= tr2.start()) {
      // merge two blocks
      TimeRange newTr = TimeRange.fromStartEnd(tr1.start(), tr2.end(), true);
      stack.push(newTr);
    } else {
      // push the earlier block first
      stack.push(tr1);
      stack.push(tr2);
    }
  }

  /* return sorted list of occupied blocks by start time, reversed */
  private List<TimeRange> recordOccupiedBlocks(Collection<Event> events, Collection<String> names) {
    List<TimeRange> occupiedBlocks = new ArrayList<>();
    for (Event event : events) {
      if (isOccupied(event, names)) {
        occupiedBlocks.add(event.getWhen());
      }
    }
    occupiedBlocks.sort(TimeRange.ORDER_BY_START.reversed());
    return occupiedBlocks;
  }

  private boolean isOccupied(Event event, Collection<String> targetNames) {
    for (String targetName : targetNames) {
      if (event.getAttendees().contains(targetName)) {
        return true;
      }
    }
    return false;
  }
}
