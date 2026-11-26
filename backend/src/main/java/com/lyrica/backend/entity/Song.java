package com.lyrica.backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "songs")
@Data
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String slug;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "video_type")
    private String videoType = "NONE";

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "views_count")
    private Integer viewsCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    @JsonManagedReference
    private List<LyricLine> lyrics = new ArrayList<>();
}