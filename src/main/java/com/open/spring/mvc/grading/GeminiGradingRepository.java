package com.open.spring.mvc.grading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeminiGradingRepository extends JpaRepository<GeminiGrading, Long> {
    // ...existing code...
}