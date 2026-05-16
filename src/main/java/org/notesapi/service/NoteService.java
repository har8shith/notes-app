package org.notesapi.service;

import org.notesapi.model.Note;
import org.notesapi.model.User;
import org.notesapi.repository.NoteRepository;
import org.notesapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NoteService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    // 1. Create a new note and assign it to the logged-in user
    public Note createNote(Note note, User user) {
        note.setUser(user);
        return noteRepository.save(note);
    }

    // 2. Fetch only active notes for the main dashboard view (Excludes Archived & Trashed)
    public List<Note> getAllNotesForUser(User user) {
        return noteRepository.findByUserAndArchivedFalseAndTrashedFalse(user);
    }

    // 3. Fetch all archived notes
    public List<Note> getArchivedNotesForUser(User user) {
        return noteRepository.findByUserAndArchivedTrueAndTrashedFalse(user);
    }

    // 4. Fetch all trashed notes
    public List<Note> getTrashedNotesForUser(User user) {
        return noteRepository.findByUserAndTrashedTrue(user);
    }

    // 5. Fetch a specific note (Authorized if owner OR if note is shared with user)
    public Optional<Note> getNoteByIdAndUser(UUID id, User user) {
        return noteRepository.findById(id)
                .filter(note -> note.getUser().getId().equals(user.getId()) ||
                        note.getSharedUsers().stream().anyMatch(u -> u.getId().equals(user.getId())));
    }

    // 6. Update an existing note, verifying the user owns it
    public Note updateNote(UUID id, Note updatedNote, User user) {
        Note existingNote = noteRepository.findById(id)
                .filter(note -> note.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));

        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());
        return noteRepository.save(existingNote);
    }

    // 7. Permanent Delete a note, verifying the user owns it
    public void deleteNote(UUID id, User user) {
        Note existingNote = noteRepository.findById(id)
                .filter(note -> note.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));

        noteRepository.delete(existingNote);
    }

    // 8. Share a note with another user by email
    public void shareNote(UUID noteId, String shareWithEmail, User owner) {
        // Find the note and ensure the person sharing it is the actual owner
        Note note = noteRepository.findById(noteId)
                .filter(n -> n.getUser().getId().equals(owner.getId()))
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));

        // Find the target user by their email
        User targetUser = userRepository.findByEmail(shareWithEmail)
                .orElseThrow(() -> new RuntimeException("User to share with not found"));

        // Add the user to the shared list and save
        note.getSharedUsers().add(targetUser);
        noteRepository.save(note);
    }

    // 9. Custom Feature: Toggle Archive State
    public Note toggleArchiveNote(UUID id, User user) {
        Note note = noteRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));
        note.setArchived(!note.isArchived());
        return noteRepository.save(note);
    }

    // 10. Custom Feature: Soft-delete to Trash bin or Restore from Trash
    public Note toggleTrashNote(UUID id, User user) {
        Note note = noteRepository.findById(id)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Note not found or unauthorized"));
        note.setTrashed(!note.isTrashed());
        return noteRepository.save(note);
    }
}
