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
    ret = getAvailableTimes(occupiedBlocks);
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

  /* Remove occupied occupiedTimeRanges from the a whole day period, by iteratively computing
   * the difference between the latest available timeRange and the earliest occupied timeRange. */
  private List<TimeRange> getAvailableTimes(List<TimeRange> occupiedTimeRanges) {
    Stack<TimeRange> availableStack = new Stack<>();
    Stack<TimeRange> occupiedStack = new Stack<>();

    // The earliest (by start time) TimeRange is on the top of occupiedStack
    occupiedStack.addAll(occupiedTimeRanges);
    occupiedStack.sort(TimeRange.ORDER_BY_START.reversed());

    availableStack.push(TimeRange.WHOLE_DAY);
    while (!occupiedStack.isEmpty()) {
      TimeRange latestAvailableTimeRange = availableStack.pop();
      if (latestAvailableTimeRange.duration()<=0){ break; }
      TimeRange earliestOccupiedTimeRange = occupiedStack.pop();
      availableStack.addAll(getTimeRangeDifference(latestAvailableTimeRange, earliestOccupiedTimeRange));
    }
    return availableStack;
  }

  /* Compute and return the difference tr1\tr2
  *  Returns a zero length TimeRange if */
  private List<TimeRange> getTimeRangeDifference(TimeRange availableTimeRange, TimeRange occupiedTimeRange) {
    List<TimeRange> ret = new ArrayList<>();
    if (occupiedTimeRange.start()<=availableTimeRange.start()){
      //                 |--available---| <--END_OF_DAY
      // |--occupied--|
      // |---occupied----|
      // |-----occupied------|
      // |----------occupied------------|
      //                 |--occupied--|
      //                 |--occupied----|
      TimeRange newTr = TimeRange.fromStartEnd(
              Math.max(availableTimeRange.start(), occupiedTimeRange.end()), TimeRange.END_OF_DAY, true);
      ret.add(newTr);
      return ret;
    }

    // |---------available-----------| <--END_OF_DAY
    //        |--occupied--|
    //        |-------occupied-------|
    ret.add(TimeRange.fromStartEnd(availableTimeRange.start(), occupiedTimeRange.start(), false));
    ret.add(TimeRange.fromStartEnd(occupiedTimeRange.end(), TimeRange.END_OF_DAY, true));
    return ret;
  }

}
