package com.open.spring.mvc.assignments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responses")
@CrossOrigin(origins = "*")
public class StudentResponseController {

    @Autowired
    private StudentResponseRepository repository;

    @PostMapping
    public StudentResponse saveResponse(@RequestBody StudentResponse response) {
        return repository.save(response);
    }

    @GetMapping
    public List<StudentResponse> getAllResponses() {
        return repository.findAll();
    }
}



