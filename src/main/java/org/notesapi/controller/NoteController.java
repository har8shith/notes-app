package org.notesapi.controller;

import jakarta.validation.Valid;
import org.notesapi.model.Note;
import org.notesapi.model.User;
import org.notesapi.repository.UserRepository;
import org.notesapi.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private UserRepository userRepository;

    // Helper method to safely fetch the real User object using the token's email
    private User getAuthenticatedUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    // 1. CREATE A NOTE (POST /notes)
    @PostMapping
    public ResponseEntity<Note> createNote(@Valid @RequestBody Note note, Principal principal) {
        User user = getAuthenticatedUser(principal);
        Note createdNote = noteService.createNote(note, user);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
    }

    // 2. GET ALL NOTES FOR AUTHENTICATED USER (GET /notes)
    @GetMapping
    public ResponseEntity<List<Note>> getAllNotes(Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<Note> notes = noteService.getAllNotesForUser(user);
        return ResponseEntity.ok(notes);
    }

    // 3. GET A SPECIFIC NOTE BY ID (GET /notes/{id})
    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable UUID id, Principal principal) {
        User user = getAuthenticatedUser(principal);
        return noteService.getNoteByIdAndUser(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 4. UPDATE AN EXISTING NOTE (PUT /notes/{id})
    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable UUID id, @Valid @RequestBody Note note, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            Note updatedNote = noteService.updateNote(id, note, user);
            return ResponseEntity.ok(updatedNote);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // 5. PERMANENT DELETE A NOTE (DELETE /notes/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID id, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            noteService.deleteNote(id, user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // 6. SHARE A NOTE (POST /notes/{id}/share)
    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, String>> shareNote(@PathVariable UUID id, @RequestBody Map<String, String> payload, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            String shareWithEmail = payload.get("share_with_email");

            // Validation edge case: Ensure email is provided
            if (shareWithEmail == null || shareWithEmail.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "share_with_email is required in the payload"));
            }

            noteService.shareNote(id, shareWithEmail, user);

            // Returns a proper JSON response which automated tests expect
            return ResponseEntity.ok(Map.of("message", "Note successfully shared with " + shareWithEmail));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // 7. GET ARCHIVED NOTES (GET /notes/archived) - Custom Feature
    @GetMapping("/archived")
    public ResponseEntity<List<Note>> getArchivedNotes(Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<Note> archivedNotes = noteService.getArchivedNotesForUser(user);
        return ResponseEntity.ok(archivedNotes);
    }

    // 8. GET TRASHED NOTES (GET /notes/trash) - Custom Feature
    @GetMapping("/trash")
    public ResponseEntity<List<Note>> getTrashedNotes(Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<Note> trashedNotes = noteService.getTrashedNotesForUser(user);
        return ResponseEntity.ok(trashedNotes);
    }

    // 9. TOGGLE ARCHIVE STATE (PATCH /notes/{id}/archive) - Custom Feature Action
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Note> toggleArchive(@PathVariable UUID id, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            Note archivedNote = noteService.toggleArchiveNote(id, user);
            return ResponseEntity.ok(archivedNote);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // 10. TOGGLE TRASH STATE (PATCH /notes/{id}/trash) - Custom Feature Action
    @PatchMapping("/{id}/trash")
    public ResponseEntity<Note> toggleTrash(@PathVariable UUID id, Principal principal) {
        try {
            User user = getAuthenticatedUser(principal);
            Note trashedNote = noteService.toggleTrashNote(id, user);
            return ResponseEntity.ok(trashedNote);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
