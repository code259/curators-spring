package com.open.spring.mvc.slack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarEventController {

    @Autowired
    private CalendarEventService calendarEventService;

    @PostMapping("/add")
    public void addEventsFromSlackMessage(@RequestBody Map<String, String> jsonMap) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate weekStartDate = LocalDate.of(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        calendarEventService.parseSlackMessage(jsonMap, weekStartDate);
    }

    @PostMapping("/add_bulk")
    public void addBulkEvents(@RequestBody List<Map<String, String>> events) {
        for (Map<String, String> eventMap : events) {
            String dateStr = eventMap.get("date");
            String title = eventMap.get("title");
            String description = eventMap.get("description");
            String type = eventMap.get("type");
            String period = eventMap.get("period");

            LocalDate date = LocalDate.parse(dateStr);
            CalendarEvent event = new CalendarEvent(date, title, description, type, period);
            calendarEventService.saveEvent(event);
        }
    }

    @PostMapping("/add_event")
    public ResponseEntity<Map<String, String>> addEvent(@RequestBody Map<String, String> jsonMap) {
        Map<String, String> response = new HashMap<>();
        try {
            String title = jsonMap.get("title");
            String dateStr = jsonMap.get("date");

            if (title == null || title.trim().isEmpty()) {
                response.put("message", "Invalid input: 'title' cannot be null or empty.");
                return ResponseEntity.badRequest().body(response);
            }
            if (dateStr == null || dateStr.trim().isEmpty()) {
                response.put("message", "Invalid input: 'date' cannot be null or empty.");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                response.put("message", "Invalid date format. Use YYYY-MM-DD.");
                return ResponseEntity.badRequest().body(response);
            }

            String description = jsonMap.getOrDefault("description", "");
            String type = jsonMap.getOrDefault("type", "general");
            String period = jsonMap.get("period"); // Might be null

            CalendarEvent event = new CalendarEvent(date, title, description, type, period);
            calendarEventService.saveEvent(event);

            response.put("message", "Event added successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error adding event: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/events/{date}")
    public List<CalendarEvent> getEventsByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return calendarEventService.getEventsByDate(localDate);
    }

    @PutMapping("/edit/{id}")
    @CrossOrigin(origins = {"http://127.0.0.1:4500","https://pages.opencodingsociety.com"}, allowCredentials = "true")
    public ResponseEntity<String> editEvent(@PathVariable int id, @RequestBody Map<String, String> payload) {
        try {
            String newTitle = payload.get("newTitle");
            String description = payload.get("description");
            String dateStr = payload.get("date");
            String period = payload.get("period");

            if (newTitle == null || newTitle.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("New title cannot be null or empty.");
            }
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Description cannot be null or empty.");
            }
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Date cannot be null or empty.");
            }
            if (period == null || period.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Period cannot be null or empty.");
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid date format. Use YYYY-MM-DD.");
            }

            boolean updated = calendarEventService.updateEventById(id, newTitle.trim(), description.trim(), date, period.trim());
            return updated ? ResponseEntity.ok("Event updated successfully.")
                        : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Event with the given id not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the event: " + e.getMessage());
        }
    }

    @GetMapping("/events")
    public List<CalendarEvent> getAllEvents() {
        return calendarEventService.getAllEvents();
    }

    @GetMapping("/events/range")
    public List<CalendarEvent> getEventsWithinDateRange(@RequestParam String start, @RequestParam String end) {
        return calendarEventService.getEventsWithinDateRange(LocalDate.parse(start), LocalDate.parse(end));
    }

    @GetMapping("/events/next-day")
    public List<CalendarEvent> getNextDayEvents() {
        return calendarEventService.getEventsByDate(LocalDate.now().plusDays(1));
    }
    
    @DeleteMapping("/delete/{id}")
    @CrossOrigin(origins = {"http://127.0.0.1:4500","https://pages.opencodingsociety.com/"}, allowCredentials = "true")
    public ResponseEntity<String> deleteEvent(@PathVariable int id) {
        System.out.println("Attempting to delete event...");
        try {
            boolean deleted = calendarEventService.deleteEventById(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Event with the given id not found.");
            }
            return ResponseEntity.ok("Event deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/events/bulk")
    public ResponseEntity<List<CalendarEvent>> bulkExtractEvents(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<CalendarEvent> events;
            if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                events = calendarEventService.getEventsWithinDateRange(start, end);
            } else {
                events = calendarEventService.getAllEvents();
            }
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
