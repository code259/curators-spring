package com.open.spring.mvc.lessonprogress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lesson")
public class LessonProgressApiController {
    
    @Autowired
    private LessonProgressJpaRepository repository;

    @GetMapping("/")
    public ResponseEntity<List<LessonProgress>> getProgress(@RequestParam String username) {
        return new ResponseEntity<>(repository.findByUsername(username), HttpStatus.OK);
    }

    @PostMapping("/complete")
    public ResponseEntity<LessonProgress> markComplete(@RequestBody LessonProgressRequest request) {
        Optional<LessonProgress> optional = repository.findByUsernameAndLessonIdAndSubLessonId(
            request.getUsername(), 
            request.getLessonId(), 
            request.getSubLessonId()
        );

        if (optional.isPresent()) {
            LessonProgress progress = optional.get();
            progress.setCompleted(true);
            repository.save(progress);
            return new ResponseEntity<>(progress, HttpStatus.OK);
        } else {
            LessonProgress newProgress = new LessonProgress(
                request.getUsername(), 
                request.getLessonId(), 
                request.getSubLessonId(), 
                true
            );
            repository.save(newProgress);
            return new ResponseEntity<>(newProgress, HttpStatus.OK);
        }
    }

    @PostMapping("/incomplete")
    public ResponseEntity<LessonProgress> markIncomplete(@RequestBody LessonProgressRequest request) {
        Optional<LessonProgress> optional = repository.findByUsernameAndLessonIdAndSubLessonId(
            request.getUsername(), 
            request.getLessonId(), 
            request.getSubLessonId()
        );

        if (optional.isPresent()) {
            LessonProgress progress = optional.get();
            progress.setCompleted(false);
            repository.save(progress);
            return new ResponseEntity<>(progress, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class LessonProgressRequest {
    private String username;
    private int lessonId;
    private int subLessonId;
}