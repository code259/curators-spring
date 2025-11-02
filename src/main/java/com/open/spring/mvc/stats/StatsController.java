package com.open.spring.mvc.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsRepository statsRepository;

    /**
     * GET /api/stats
     * Get stats for a specific user or all users.
     * 
     * Request Body Examples:
     * To get a specific user: {"username": "toby"}
     * To get all users: {} or {"username": null}
     */
    @GetMapping
    public ResponseEntity<?> getStats(@RequestBody(required = false) StatsGetDto getRequest) {
        // If no body or username is null/empty, return all stats
        if (getRequest == null || getRequest.getUsername() == null || getRequest.getUsername().isEmpty()) {
            List<Stats> statsList = statsRepository.findAll();
            return new ResponseEntity<>(statsList, HttpStatus.OK);
        }
        
        // Otherwise, get stats for the specific username
        List<Stats> statsList = statsRepository.findAllByUsername(getRequest.getUsername());
        if (statsList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(statsList, HttpStatus.OK);
    }

    /**
     * POST /api/stats
     * Create a new Stats record.
     * 
     * Request Body Example:
     * {
     * "username": "nolan",
     * "module": "ai",
     * "submodule": 2,
     * }
     */
    @PostMapping
    public ResponseEntity<Stats> createStats(@RequestBody Stats stats) {
        Optional<Stats> existingStats = statsRepository.findByUsernameAndModuleAndSubmodule(
                stats.getUsername(), stats.getModule(), stats.getSubmodule());
        if (existingStats.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT); // Conflict, record exists
        }
        Stats newStats = statsRepository.save(stats);
        return new ResponseEntity<>(newStats, HttpStatus.CREATED);
    }

    /**
     * PUT /api/stats
     * Update stats for a specific column.
     * 
     * Request Body Example:
     * {
     *   "username": "toby",
     *   "module": "ai",
     *   "submodule": 2,
     *   "finished": true,
     *   "time": 321.4
     * }
     */
    @PutMapping
    public ResponseEntity<Stats> updateStats(@RequestBody StatsUpdateDto updateRequest) {

        // 1. Find the user from the DTO
        if (updateRequest.getUsername() == null || updateRequest.getModule() == null || updateRequest.getSubmodule() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Optional<Stats> optionalStats = statsRepository.findByUsernameAndModuleAndSubmodule(
                updateRequest.getUsername(), updateRequest.getModule(), updateRequest.getSubmodule());
        if (!optionalStats.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // User not found
        }

        Stats statsToUpdate = optionalStats.get();

        // 2. Update fields if provided
        boolean updated = false;
        if (updateRequest.getFinished() != null) {
            statsToUpdate.setFinished(updateRequest.getFinished());
            updated = true;
        }
        if (updateRequest.getTime() != null) {
            statsToUpdate.setTime(updateRequest.getTime());
            updated = true;
        }

        if (!updated) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 3. Save the updated object
        Stats updatedStats = statsRepository.save(statsToUpdate);
        return new ResponseEntity<>(updatedStats, HttpStatus.OK);
    }

    /**
     * DELETE /api/stats
     * Delete a user's stats.
     * 
     * Request Body Example:
     * {
     * "username": "toby"
     * }
     */
    @DeleteMapping
    public ResponseEntity<String> deleteStats(@RequestBody StatsDeleteDto deleteRequest) {

        if (deleteRequest.getUsername() == null || deleteRequest.getUsername().isEmpty()) {
            return new ResponseEntity<>("Username is required.", HttpStatus.BAD_REQUEST);
        }

        // Delete all user stats if module not provided
        if (deleteRequest.getModule() == null || deleteRequest.getModule().isEmpty()) {
            List<Stats> statsList = statsRepository.findAllByUsername(deleteRequest.getUsername());
            if (statsList.isEmpty()) {
                return new ResponseEntity<>("User '" + deleteRequest.getUsername() + "' not found.", HttpStatus.NOT_FOUND);
            }
            statsRepository.deleteAll(statsList);
            return new ResponseEntity<>("All stats for '" + deleteRequest.getUsername() + "' deleted successfully.", HttpStatus.OK);
        }

        // Delete all submodules for module if submodule not provided
        if (deleteRequest.getSubmodule() == null) {
            List<Stats> statsList = statsRepository.findAllByUsernameAndModule(
                    deleteRequest.getUsername(), deleteRequest.getModule());
            if (statsList.isEmpty()) {
                return new ResponseEntity<>(
                        "No stats for '" + deleteRequest.getUsername() + "' in module '" + deleteRequest.getModule() + "'.",
                        HttpStatus.NOT_FOUND);
            }
            statsRepository.deleteAll(statsList);
            return new ResponseEntity<>(
                    "Stats for '" + deleteRequest.getUsername() + "' in module '" + deleteRequest.getModule() + "' deleted successfully.",
                    HttpStatus.OK);
        }

        // Delete specific module/submodule entry
        Optional<Stats> optionalStats = statsRepository.findByUsernameAndModuleAndSubmodule(
                deleteRequest.getUsername(), deleteRequest.getModule(), deleteRequest.getSubmodule());
        if (!optionalStats.isPresent()) {
            // Return Not Found if the user doesn't exist
            return new ResponseEntity<>(
                    "No stats for '" + deleteRequest.getUsername() + "' in module '" + deleteRequest.getModule() +
                            "' submodule '" + deleteRequest.getSubmodule() + "'.",
                    HttpStatus.NOT_FOUND);
        }

        // 2. Get the stats object and delete it
        Stats statsToDelete = optionalStats.get();
        statsRepository.delete(statsToDelete);

        // 3. Return a success message
        return new ResponseEntity<>(
                "Stats for '" + deleteRequest.getUsername() + "' in module '" + deleteRequest.getModule() +
                        "' submodule '" + deleteRequest.getSubmodule() + "' deleted successfully.",
                HttpStatus.OK);
    }
}
