package com.chesst.analysis;

import com.chesst.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByGameOrderByCreatedAtDesc(Game game);
    List<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
