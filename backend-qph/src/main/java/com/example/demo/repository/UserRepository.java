package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "rol")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "rol")
    Optional<User> findWithRolById(Long id);

    @EntityGraph(attributePaths = "rol")
    List<User> findAllByOrderByIdAsc();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
