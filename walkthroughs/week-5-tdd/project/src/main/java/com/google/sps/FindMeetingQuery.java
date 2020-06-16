// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.*;

public final class FindMeetingQuery {

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    ArrayList<TimeRange> options = new ArrayList();
    ArrayList<TimeRange> unavailableBlocks = new ArrayList();
    Collection<String> allAttendees = new ArrayList();
    
    long duration = request.getDuration();

    // Meetings can't be longer than a day
    if (duration > TimeRange.WHOLE_DAY.duration()) {
      return options;
    } else {
      options.add(TimeRange.WHOLE_DAY);
    }

    ArrayList<TimeRange> allUnavailableBlocks;
    allAttendees.addAll(request.getAttendees());
    allAttendees.addAll(request.getOptionalAttendees());

    Collection<String> optAttendees = request.getOptionalAttendees();
    Collection<String> reqAttendees = request.getAttendees();
    
    // Do not need to check if optional attendees are compatible with meeting request
    if (reqAttendees.size() == 0 || optAttendees.size() == 0){
      allUnavailableBlocks = unavailableTime(events, allAttendees, duration);
      findFreeTime(options, allUnavailableBlocks, duration);

    } else {
      ArrayList<TimeRange> reqUnavailableBlocks = unavailableTime(events, reqAttendees, duration);
      allUnavailableBlocks = unavailableTime(events, allAttendees, duration);
      
      // Preserve options in case cannot accommodate additional opt attendees
      ArrayList<TimeRange> optionsTemp = findFreeTime(new ArrayList<>(options), allUnavailableBlocks, duration);
      if (optionsTemp.size() == 0) { // opt attendees not compatible, focus on req attendees
        optionsTemp = findFreeTime(new ArrayList<>(options), reqUnavailableBlocks, duration);
      }
      options = optionsTemp;
    }
    return options;
  }

  /**
   * Find all possible time ranges for meeting 
   */
  private ArrayList<TimeRange> findFreeTime(ArrayList<TimeRange> options, ArrayList<TimeRange> blockedTime, long duration) {

    for (TimeRange block : blockedTime) {
      int busyStart = block.start();
      int busyEnd = block.end();

      for (TimeRange freeTime : new ArrayList<>(options)) {

        // Conflict in ranges
        if (block.overlaps(freeTime)) {
          int freeStart = freeTime.start();
          int freeEnd = freeTime.end();

          // Free time extends beyond busy time
          if (!block.contains(freeTime)) {
            // Check for free time before event starts
            if ((freeStart + duration) <= busyStart) {
              options.add(TimeRange.fromStartEnd(freeStart, busyStart, false));
            }
            // Check for free time after event ends
            if ((busyEnd + duration) <= freeEnd) {
              options.add(TimeRange.fromStartEnd(busyEnd, freeEnd, false));
            }
          }
          options.remove(freeTime);
        }
      } 
    }
    return options;
  }

  /**
   * Block off all unavailable time
   */
  private ArrayList<TimeRange> unavailableTime(Collection<Event> allEvents, Collection<String> attendees, long duration) {
    ArrayList<TimeRange> blockedTimeRanges = findAttendeesBlockedTimeRanges(allEvents, attendees);
    return mergeTimeRanges(blockedTimeRanges, duration);
  }

  /**
   * Merge attendees' busy blocks
   */
  private ArrayList<TimeRange> mergeTimeRanges(ArrayList<TimeRange> blockedTime, long duration) {
    Collections.sort(blockedTime, TimeRange.ORDER_BY_START);

    for (int i = 0; i < blockedTime.size() - 1; i++) {
      TimeRange current = blockedTime.get(i);
      TimeRange next = blockedTime.get(i+1);
      boolean nextEndOfDay = (next.end() == TimeRange.END_OF_DAY); // Meetings go to the end of the day should be inclusive of end time

      // Unable to schedule meeting between events
      if (current.overlaps(next)) {
        if ((next.end() >= current.end()) || (next.start() - current.start() < duration)) {
          blockedTime.add(TimeRange.fromStartEnd(current.start(), next.end(), nextEndOfDay));  
          blockedTime.remove(current);  // replaced with merged block
        }
        blockedTime.remove(next); // absorbed
      }
    }
    return blockedTime;
  }

  /**
   * Return attendees' blocked time ranges events
   */
  private ArrayList<TimeRange> findAttendeesBlockedTimeRanges(Collection<Event> allEvents, Collection<String> attendees){
    ArrayList<TimeRange> blockedTime = new ArrayList<TimeRange>();

    for (Event event : allEvents) {
      for (String attendee : attendees) {
        if (event.getAttendees().contains(attendee)) {
          blockedTime.add(event.getWhen());
          break; // prevent event repetition
        }
      }
    }
    return blockedTime;
  }
}