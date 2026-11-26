CREATE DATABASE IF NOT EXISTS lyrica_platform
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE lyrica_platform;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS songs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(250) NOT NULL UNIQUE,
    artist_name VARCHAR(150) NOT NULL,
    cover_url VARCHAR(500),
    audio_url VARCHAR(500),
    video_url VARCHAR(500),
    video_type ENUM('NONE', 'YOUTUBE', 'UPLOAD') DEFAULT 'NONE',
    uploaded_by BIGINT NOT NULL,
    views_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_songs_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lyric_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL,
    line_order INT NOT NULL,
    start_time_ms INT DEFAULT 0,
    kanji_text TEXT NOT NULL,
    furigana_text TEXT,
    romaji_text TEXT,
    vietnamese_text TEXT,
    CONSTRAINT fk_lyrics_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    INDEX idx_song_order (song_id, line_order)
);

CREATE TABLE IF NOT EXISTS favorites (
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, song_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_song FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);

INSERT IGNORE INTO users (id, username, email, password_hash, role)
VALUES (1, 'thebao', 'thebao@lyrica.local', 'hashed_password_placeholder', 'ADMIN');