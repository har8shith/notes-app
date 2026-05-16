package org.notesapi.repository;

import org.notesapi.model.Note;
import org.notesapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    // 1. Fetch only active notes for the main dashboard dashboard view
    List<Note> findByUserAndArchivedFalseAndTrashedFalse(User user);

    // 2. Fetch archived notes
    List<Note> findByUserAndArchivedTrueAndTrashedFalse(User user);

    // 3. Fetch trashed notes
    List<Note> findByUserAndTrashedTrue(User user);
}
