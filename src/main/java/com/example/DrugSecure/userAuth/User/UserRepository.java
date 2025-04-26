package com.example.DrugSecure.userAuth.User;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}
