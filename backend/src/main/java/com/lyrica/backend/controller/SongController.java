package com.lyrica.backend.controller;

import com.lyrica.backend.entity.LyricLine;
import com.lyrica.backend.entity.Song;
import com.lyrica.backend.entity.User;
import com.lyrica.backend.repository.SongRepository;
import com.lyrica.backend.repository.UserRepository;
import com.lyrica.backend.service.AiTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final AiTranslationService aiTranslationService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    public List<Song> getAllSongs() {
        return songRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Song> getSongBySlug(@PathVariable String slug) {
        return songRepository.findBySlug(slug)
            .map(song -> {
                song.setViewsCount(song.getViewsCount() + 1);
                return ResponseEntity.ok(songRepository.save(song));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Song> uploadSong(
            @RequestParam("title") String title,
            @RequestParam("artistName") String artistName,
            @RequestParam(value = "rawLyrics", required = false) String rawLyrics,
            @RequestParam(value = "youtubeUrl", required = false) String youtubeUrl,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile
    ) throws IOException {

        User uploader = userRepository.findById(1L)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user mẫu ID=1"));

        Song song = new Song();
        song.setTitle(title);
        song.setArtistName(artistName);
        song.setSlug(generateSlug(title));
        song.setUploadedBy(uploader);

        Path mediaPathForAi = null;
        String mediaMimeType = null;

        if (coverFile != null && !coverFile.isEmpty()) {
            song.setCoverUrl("/uploads/" + saveFile(coverFile).getFileName());
        }
        if (audioFile != null && !audioFile.isEmpty()) {
            Path savedAudio = saveFile(audioFile);
            song.setAudioUrl("/uploads/" + savedAudio.getFileName());
            mediaPathForAi = savedAudio;
            mediaMimeType = audioFile.getContentType();
        }
        if (videoFile != null && !videoFile.isEmpty()) {
            Path savedVideo = saveFile(videoFile);
            song.setVideoUrl("/uploads/" + savedVideo.getFileName());
            song.setVideoType("UPLOAD");
            if (mediaPathForAi == null) {
                mediaPathForAi = savedVideo;
                mediaMimeType = videoFile.getContentType();
            }
        } else if (youtubeUrl != null && !youtubeUrl.isBlank()) {
            song.setVideoUrl(youtubeUrl.trim());
            song.setVideoType("YOUTUBE");
        }

        List<LyricLine> processedLyrics;
        if (rawLyrics != null && !rawLyrics.isBlank()) {
            processedLyrics = aiTranslationService.translateTextLyrics(rawLyrics);
        } else if (mediaPathForAi != null) {
            processedLyrics = aiTranslationService.transcribeAndTranslateMedia(mediaPathForAi, mediaMimeType);
        } else {
            processedLyrics = List.of();
        }

        for (LyricLine line : processedLyrics) {
            line.setSong(song);
        }
        song.setLyrics(processedLyrics);

        return ResponseEntity.ok(songRepository.save(song));
    }

    @PutMapping("/{id}/lyrics")
    public ResponseEntity<Song> updateLyrics(
            @PathVariable Long id,
            @RequestBody List<LyricLine> updatedLines
    ) {
        return songRepository.findById(id)
            .map(song -> {
                song.getLyrics().clear();
                int order = 1;
                for (LyricLine incoming : updatedLines) {
                    LyricLine line = new LyricLine();
                    line.setSong(song);
                    line.setLineOrder(order++);
                    line.setStartTimeMs(incoming.getStartTimeMs() != null ? incoming.getStartTimeMs() : 0);
                    line.setKanjiText(incoming.getKanjiText() != null ? incoming.getKanjiText() : "");
                    line.setFuriganaText(incoming.getFuriganaText() != null ? incoming.getFuriganaText() : "");
                    line.setRomajiText(incoming.getRomajiText() != null ? incoming.getRomajiText() : "");
                    line.setVietnameseText(incoming.getVietnameseText() != null ? incoming.getVietnameseText() : "");
                    song.getLyrics().add(line);
                }
                return ResponseEntity.ok(songRepository.save(song));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private Path saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), destination);
        return destination;
    }

    private String generateSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
        return normalized + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}