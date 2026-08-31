package com.chesst.opening;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpeningRepository extends JpaRepository<Opening, Long> {

    @Query("""
        SELECT o FROM Opening o
        WHERE (:q IS NULL OR :q = '' OR
               LOWER(o.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(o.eco)  LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:eco IS NULL OR :eco = '' OR LOWER(o.eco) LIKE LOWER(CONCAT(:eco, '%')))
        ORDER BY o.eco, o.name
    """)
    Page<Opening> search(@Param("q") String q, @Param("eco") String eco, Pageable pageable);

    long countByEcoStartingWith(String ecoPrefix);
}
