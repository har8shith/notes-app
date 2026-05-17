package org.notesapi.repository;

import org.notesapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user for login/authentication purposes
    Optional<User> findByEmail(String email);

    // Checks if an email is already taken during registration!
    boolean existsByEmail(String email);
}