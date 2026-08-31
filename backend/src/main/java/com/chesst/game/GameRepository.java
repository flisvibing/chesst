package com.chesst.game;

import com.chesst.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    long countByOwnerId(Long ownerId);
}
