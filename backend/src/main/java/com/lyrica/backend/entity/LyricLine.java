package com.lyrica.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lyric_lines")
@Data
public class LyricLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    @JsonBackReference
    private Song song;

    @Column(name = "line_order")
    private Integer lineOrder;

    @Column(name = "start_time_ms")
    private Integer startTimeMs = 0;

    @Column(name = "kanji_text", columnDefinition = "TEXT")
    private String kanjiText;

    @Column(name = "furigana_text", columnDefinition = "TEXT")
    private String furiganaText;

    @Column(name = "romaji_text", columnDefinition = "TEXT")
    private String romajiText;

    @Column(name = "vietnamese_text", columnDefinition = "TEXT")
    private String vietnameseText;
}