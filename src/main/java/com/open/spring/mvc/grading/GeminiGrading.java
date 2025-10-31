package com.open.spring.mvc.grading;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class GeminiGrading {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String question;

    @Column(nullable = false, length = 2000)
    private String studentResponse;

    @Lob
    @Column(nullable = true)
    private String geminiReply;

    @Column(nullable = false)
    private Instant createdAt;

    public GeminiGrading(String question, String studentResponse, String geminiReply, Instant createdAt) {
        this.question = question;
        this.studentResponse = studentResponse;
        this.geminiReply = geminiReply;
        this.createdAt = createdAt;
    }
}
