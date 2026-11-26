package com.lyrica.backend.repository;

import com.lyrica.backend.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {
    Optional<Song> findBySlug(String slug);
    List<Song> findAllByOrderByCreatedAtDesc();
}