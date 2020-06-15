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
    long duration = request.getDuration();

    // Meetings can't be longer than a day
    if (duration > 24 * 60) {
      return options;
    } else {
      options.add(TimeRange.WHOLE_DAY);
    }
    
    unavailableBlocks = unavailableTime(events, request);

    for (TimeRange block : unavailableBlocks) {
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
  private ArrayList<TimeRange> unavailableTime(Collection<Event> allEvents, MeetingRequest request) {
    ArrayList<TimeRange> attendeeEvents = attendeeEvents(allEvents, request);
    return merge(attendeeEvents, request);
  }

  /**
   * Merge all attendees' events
   */
  private ArrayList<TimeRange> merge(ArrayList<TimeRange> events, MeetingRequest request) {
    long duration = request.getDuration();
    Collections.sort(events, TimeRange.ORDER_BY_START);

    for (int i = 0; i < events.size() - 1; i++) {
      TimeRange current = events.get(i);
      TimeRange next = events.get(i+1);
      boolean nextEnd = (next.end() == TimeRange.END_OF_DAY);

      // Unable to schedule meeting between events
      if (current.overlaps(next)) {
        if ((next.end() >= current.end()) || (next.start() - current.start() < duration)) {
          events.add(TimeRange.fromStartEnd(current.start(), next.end(), nextEnd));  
          events.remove(current);  // replaced with merged block
        }
        events.remove(next); // absorbed
      }
    }
    return events;
  }

  /**
   * Return meeting request's attendees' events
   */
  private ArrayList<TimeRange> attendeeEvents(Collection<Event> allEvents, MeetingRequest request){
    ArrayList<TimeRange> events = new ArrayList<TimeRange>();

    for (Event event : allEvents) {
      for (String attendee : request.getAttendees()) {
        if (event.getAttendees().contains(attendee)) {
          events.add(event.getWhen());
          break; // prevent event repetition
        }
      }
    }
    return events;
  }
}
