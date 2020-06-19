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
    long duration = request.getDuration();

    // Meetings can't be longer than a day
    if (duration > TimeRange.WHOLE_DAY.duration()) {
      return options;
    } else {
      options.add(TimeRange.WHOLE_DAY);
    }
    
    Collection<String> optAttendees = request.getOptionalAttendees();
    Collection<String> reqAttendees = request.getAttendees();
    
    Collection<String> allAttendees = new ArrayList();
    allAttendees.addAll(reqAttendees);
    allAttendees.addAll(optAttendees);

    ArrayList<TimeRange> allUnavailableBlocks;
    
    // Do not need to check if optional attendees are compatible with meeting request
    if (reqAttendees.size() == 0 || optAttendees.size() == 0){
      allUnavailableBlocks = unavailableTime(events, allAttendees, duration);
      options = findFreeTime(options, allUnavailableBlocks, duration);

    } else {
      ArrayList<TimeRange> reqUnavailableBlocks = unavailableTime(events, reqAttendees, duration);
      ArrayList<TimeRange> optUnavailableBlocks = unavailableTime(events, optAttendees, duration);
      allUnavailableBlocks = unavailableTime(events, allAttendees, duration);
      
      // Preserve options in case cannot accommodate opt attendees
      ArrayList<TimeRange> bestOptionsFound = findFreeTime(new ArrayList<>(options), allUnavailableBlocks, duration);
      if (bestOptionsFound.size() == 0) { // opt attendees not compatible, find optimal time ranges
        // Filter down to required attendees' free times
        bestOptionsFound = findFreeTime(options, reqUnavailableBlocks, duration);

        bestOptionsFound = findCompatibleAttendees(events, bestOptionsFound, optAttendees, duration);
        
        bestOptionsFound = findOptimalTimeSlot(bestOptionsFound);
      }
      options = bestOptionsFound;
    }
    return options;
  }
  
  /**
   * Find all possible time ranges for meeting 
   */
  private ArrayList<TimeRange> findCompatibleAttendees(Collection<Event> events, ArrayList<TimeRange> options, Collection<String> attendees, long duration) {
    ArrayList<TimeRange> optOptions = new ArrayList<>(options);
    
    for (String attendee : attendees){
      ArrayList<TimeRange> attendeeEvents = unavailableTime(events, Arrays.asList(attendee), duration);
      ArrayList<TimeRange> attendeeFreeTime = findFreeTime(new ArrayList<>(optOptions), attendeeEvents, duration);

      // Remove opt attendees with incompatible free time
      if (attendeeFreeTime.size() != 0) {
        optOptions.addAll(attendeeFreeTime);
      }
    }
    return optOptions;
  }

  /**
   * Find free time ranges with most of opt attendees
   */
  private ArrayList<TimeRange> findOptimalTimeSlot(ArrayList<TimeRange> options) {
    Collections.sort(options, TimeRange.ORDER_BY_END);
    int maxOptAttendeeCount = 0;
    ArrayList<TimeRange> maxOptOptions = new ArrayList<TimeRange>();

    for (int i = 0; i < options.size(); i++) {
      TimeRange freeBlock = options.get(i);
      int currentCount = 0;

      for (int j = i; j < options.size(); j++) {
        TimeRange currentBlock = options.get(j);

        if (currentBlock.equals(freeBlock) || currentBlock.overlaps(freeBlock)) {
          currentCount++;
        } else {
          // Sorted list; there will be no more instances of time range
          break;
        }
      }
      // Update list
      if (currentCount > maxOptAttendeeCount) {
        maxOptOptions.clear();
        maxOptOptions.add(freeBlock);
        maxOptAttendeeCount = currentCount;
      } else if (currentCount == maxOptAttendeeCount) {
        maxOptOptions.add(freeBlock);
      }
    }
    return maxOptOptions;
  }

  /**
   * Find all possible time ranges for meeting
   * @param options available time to schedule meetings
   * @param blockedTime events that make time ranges unavailable
   * @return time range options for meeting based on available time & events
   * MODIFIES the options list passed in
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
  private ArrayList<TimeRange> mergeTimeRanges(ArrayList<TimeRange> busyEvents, long duration) {
    Collections.sort(busyEvents, TimeRange.ORDER_BY_START);

    ArrayList<TimeRange> blockedTime = new ArrayList<TimeRange>(busyEvents); // prevent list modification during loop
    
    for (int i = 0; i < busyEvents.size() - 1; i++) {
      TimeRange current = busyEvents.get(i);
      TimeRange next = busyEvents.get(i+1);
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