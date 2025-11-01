package com.open.spring.mvc.grades;

import jakarta.persistence.*;

@Entity
@Table(name = "grades")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private String gradeLevel;

    public Grade() {}

    public Grade(String studentId, String subject, Double score, String gradeLevel) {
        this.studentId = studentId;
        this.subject = subject;
        this.score = score;
        this.gradeLevel = gradeLevel;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
}


