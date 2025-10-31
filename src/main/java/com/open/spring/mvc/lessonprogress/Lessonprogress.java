package com.open.spring.mvc.lessonprogress;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class LessonProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int lessonId;

    @Column(nullable = false)
    private int subLessonId;

    @Column(nullable = false)
    private boolean completed = false;

    public LessonProgress(String username, int lessonId, int subLessonId, boolean completed) {
        this.username = username;
        this.lessonId = lessonId;
        this.subLessonId = subLessonId;
        this.completed = completed;
    }

    // Sample initialization data
    public static LessonProgress[] init() {
        LessonProgress[] progressArray = {
            // Aidan Lau - completed lesson 1, partially completed lesson 2
            new LessonProgress("Aidan Lau", 1, 1, true),
            new LessonProgress("Aidan Lau", 1, 2, true),
            new LessonProgress("Aidan Lau", 1, 3, true),
            new LessonProgress("Aidan Lau", 1, 4, true),
            new LessonProgress("Aidan Lau", 1, 5, true),
            new LessonProgress("Aidan Lau", 1, 6, true),
            new LessonProgress("Aidan Lau", 2, 1, true),
            new LessonProgress("Aidan Lau", 2, 2, true),
            
            // Saathvik G - partially completed lesson 1
            new LessonProgress("Saathvik G", 1, 1, true),
            new LessonProgress("Saathvik G", 1, 2, true),
            new LessonProgress("Saathvik G", 1, 3, true),
            
            // Sri S - just started
            new LessonProgress("Sri S", 1, 1, true)
        };
        return progressArray;
    }
}