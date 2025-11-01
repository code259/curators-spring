package com.open.spring.mvc.grades;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    @Autowired
    private GradeRepository gradeRepository;


    @GetMapping("/{id}")
    public ResponseEntity<Grade> getGradeById(@PathVariable Long id) {
        Optional<Grade> grade = gradeRepository.findById(id);
        return grade.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public List<Grade> getGradesByStudent(@PathVariable String studentId) {
        return gradeRepository.findByStudentId(studentId);
    }

    @GetMapping("/subject/{subject}")
    public List<Grade> getGradesBySubject(@PathVariable String subject) {
        return gradeRepository.findBySubject(subject);
    }

    @PostMapping
    public Grade createGrade(@RequestBody Grade grade) {
        return gradeRepository.save(grade);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Grade> updateGrade(@PathVariable Long id, @RequestBody Grade gradeDetails) {
        Optional<Grade> optionalGrade = gradeRepository.findById(id);
        if (optionalGrade.isPresent()) {
            Grade grade = optionalGrade.get();
            grade.setStudentId(gradeDetails.getStudentId());
            grade.setSubject(gradeDetails.getSubject());
            grade.setScore(gradeDetails.getScore());
            grade.setGradeLevel(gradeDetails.getGradeLevel());
            return ResponseEntity.ok(gradeRepository.save(grade));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        if (gradeRepository.existsById(id)) {
            gradeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}


