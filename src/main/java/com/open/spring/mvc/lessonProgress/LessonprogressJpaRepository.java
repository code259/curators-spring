package com.open.spring.mvc.lessonprogress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressJpaRepository extends JpaRepository<LessonProgress, Long> {
    Optional<LessonProgress> findByUsernameAndLessonIdAndSubLessonId(String username, int lessonId, int subLessonId);
    List<LessonProgress> findByUsername(String username);
}