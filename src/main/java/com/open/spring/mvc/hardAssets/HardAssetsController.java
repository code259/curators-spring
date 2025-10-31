package com.open.spring.mvc.hardAssets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class HardAssetsController {

    private static final String UPLOAD_DIR = "./uploads/";

    @Autowired
    private HardAssetsRepository repository;

    @GetMapping("")
    public ResponseEntity<List<HardAsset>> getUploads() {
        System.out.println("GET request for /api/upload");
        return new ResponseEntity<>(repository.findAll(), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<String> postUpload(@RequestParam("file") MultipartFile file) {
        System.out.println("POST request for /api/upload");
        if (file.isEmpty()) {
            return new ResponseEntity<>("Please select a file to upload.", HttpStatus.BAD_REQUEST);
        }
        try {
            // Create the upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), filePath);

            HardAsset newAsset = new HardAsset(fileName);
            repository.save(newAsset);
            System.out.println("File uploaded and saved to database: " + fileName);
            return new ResponseEntity<>("Successfully uploaded and saved '" + fileName + "'", HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}