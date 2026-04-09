package com.taskmanager.repository;

import com.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ this is required to check if email exists
    User findByEmail(String email);

}