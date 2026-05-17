package org.notesapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MetadataController {


    @GetMapping("/about")
    public Map<String, Object> getAbout() {
        Map<String, Object> response = new HashMap<>();


        response.put("name", "Your Name");
        response.put("email", "your.email@example.com");

        Map<String, String> features = new HashMap<>();
        features.put("Archive & Trash Management",
                "Implemented a soft-delete and archive state management system. " +
                        "Instead of permanently deleting notes by accident (which causes poor user experience and data loss), " +
                        "users can move notes to a trash bin or archive them to keep the main dashboard clean.");

        response.put("my features", features);
        return response;
    }


    @GetMapping("/openapi.json")
    public Map<String, Object> getOpenApi() {

        Map<String, Object> openapi = new HashMap<>();
        openapi.put("openapi", "3.0.0");

        Map<String, Object> info = new HashMap<>();
        info.put("title", "Notes API");
        info.put("version", "1.0.0");
        info.put("description", "A RESTful API for managing multi-user notes with sharing, archiving, and trash capabilities.");
        openapi.put("info", info);

        Map<String, Object> paths = new HashMap<>();
        paths.put("/register", Map.of("post", Map.of("summary", "Register a new user")));
        paths.put("/login", Map.of("post", Map.of("summary", "Authenticate and get a JWT token")));
        paths.put("/notes", Map.of(
                "get", Map.of("summary", "Get all active notes for the authenticated user"),
                "post", Map.of("summary", "Create a new note")
        ));
        paths.put("/notes/{id}", Map.of(
                "get", Map.of("summary", "Get a specific note by ID"),
                "put", Map.of("summary", "Update a note"),
                "delete", Map.of("summary", "Permanently delete a note")
        ));
        paths.put("/notes/{id}/share", Map.of("post", Map.of("summary", "Share a note with another user via email")));
        paths.put("/notes/archived", Map.of("get", Map.of("summary", "Get all archived notes")));
        paths.put("/notes/trash", Map.of("get", Map.of("summary", "Get all trashed notes")));

        openapi.put("paths", paths);

        return openapi;
    }
}