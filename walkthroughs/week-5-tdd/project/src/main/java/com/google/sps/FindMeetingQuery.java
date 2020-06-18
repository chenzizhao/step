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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

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
    ret = complement(occupiedBlocks);
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

  /* Remove occupied timeRanges from the a whole day period, by iteratively computing
   * the difference between the latest available timeRange and the earliest occupied timeRange. */
  private List<TimeRange> complement(List<TimeRange> timeRanges) {
    Stack<TimeRange> complementStack = new Stack<>();
    Stack<TimeRange> occupiedStack = new Stack<>();

    // TimeRanges are added in the order that they are returned by timeRanges' iterator
    // The earliest (by start time) TimeRange is on the top of occupiedStack
    timeRanges.sort(TimeRange.ORDER_BY_START.reversed());
    occupiedStack.addAll(timeRanges);

    complementStack.push(TimeRange.WHOLE_DAY);
    while (!occupiedStack.isEmpty()) {
      TimeRange tr1 = complementStack.pop();
      TimeRange tr2 = occupiedStack.pop();
      // tr2.end()<=tr1.end()
      complHelper(tr1, tr2, complementStack);
    }
    return complementStack;
  }

  /* Compute the difference tr1\tr2, and push the result to the stack */
  private void complHelper(TimeRange tr1, TimeRange tr2, Stack<TimeRange> stack) {
    // tr2.end()<=tr1.end() guaranteed
    if (tr1 == tr2) {
      return;
    } else if (!tr1.overlaps(tr2)) {
      //          |--tr1--|
      // |--tr2--|
      stack.push(tr1);
    } else if (!tr1.contains(tr2)) {
      //    |--tr1--|
      // |--tr2--|
      TimeRange newTr = TimeRange.fromStartEnd(tr2.end(), tr1.end(), false);
      stack.push(newTr);
    } else {
      // |----tr1----|
      //   |--tr2--|
      // tr1 contains tr2
      TimeRange newTr1 = TimeRange.fromStartEnd(tr1.start(), tr2.start(), false);
      TimeRange newTr2 = TimeRange.fromStartEnd(tr2.end(), tr1.end(), false);
      // push early gap first
      stack.push(newTr1);
      stack.push(newTr2);
    }
  }

}
