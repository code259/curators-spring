package com.open.spring.mvc.assignments;

import jakarta.persistence.*;

@Entity
@Table(name = "responses")  // matches your SQLite table
public class StudentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name")  // maps to SQLite column
    private String name;

    @Column(name = "answer_text")   // maps to SQLite column
    private String response;

    public StudentResponse() {}

    public StudentResponse(String name, String response) {
        this.name = name;
        this.response = response;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getResponse() { return response; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setResponse(String response) { this.response = response; }
}
