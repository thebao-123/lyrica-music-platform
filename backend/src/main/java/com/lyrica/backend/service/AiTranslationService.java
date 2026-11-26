package com.lyrica.backend.service;

import com.lyrica.backend.entity.LyricLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AiTranslationService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public List<LyricLine> transcribeAndTranslateMedia(Path mediaPath, String mimeType) {
        if (apiKey == null || apiKey.isBlank() || mediaPath == null) {
            return List.of();
        }

        try {
            byte[] fileBytes = Files.readAllBytes(mediaPath);
            String base64Audio = Base64.getEncoder().encodeToString(fileBytes);

            String prompt = """
                Hãy nghe toàn bộ bài hát tiếng Nhật trong tệp âm thanh này, tách từng câu hát theo đúng thời điểm ca sĩ bắt đầu hát câu đó và trả về một mảng JSON hợp lệ (không bọc markdown ```json).
                Mỗi phần tử trong mảng gồm đúng 5 trường:
                - "startTimeMs": số nguyên là thời điểm bắt đầu câu hát tính bằng mili-giây (ví dụ: 14880 cho 00:14.88)
                - "kanji": lời gốc tiếng Nhật chính xác
                - "furigana": phiên âm Hiragana đầy đủ cho câu đó
                - "romaji": phiên âm Romaji chuẩn ngữ cảnh hát
                - "vietnamese": bản dịch tiếng Việt tự nhiên, giàu cảm xúc
                Lưu ý: Tại các đoạn nhạc dạo dài không có lời (trên 8 giây), thêm một phần tử có "startTimeMs" tại thời điểm dứt câu trước và 4 trường text để chuỗi rỗng "".
                """;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                    Map.of("inlineData", Map.of(
                        "mimeType", mimeType != null ? mimeType : "audio/mpeg",
                        "data", base64Audio
                    )),
                    Map.of("text", prompt)
                ))),
                "generationConfig", Map.of("responseMimeType", "application/json")
            );

            return callGeminiAndParse(requestBody);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<LyricLine> translateTextLyrics(String rawLyrics) {
        if (rawLyrics == null || rawLyrics.isBlank()) {
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackSplitLines(rawLyrics);
        }

        String prompt = """
            Bạn là chuyên gia dịch lời nhạc J-Pop. Hãy phân tích từng dòng lời tiếng Nhật dưới đây (nếu đầu dòng có mốc thời gian dạng [mm:ss.xx] hãy quy đổi sang mili-giây vào trường startTimeMs, nếu không có thì để 0) và trả về mảng JSON hợp lệ (không bọc markdown ```json).
            Mỗi phần tử gồm đúng 5 trường:
            - "startTimeMs": số nguyên mili-giây
            - "kanji": câu gốc tiếng Nhật (đã bỏ thẻ thời gian [mm:ss.xx] nếu có)
            - "furigana": phiên âm Hiragana đầy đủ
            - "romaji": phiên âm Romaji chuẩn ngữ cảnh hát
            - "vietnamese": bản dịch tiếng Việt tự nhiên, giàu chất thơ

            Lời bài hát:
            """ + rawLyrics;

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json")
            );
            return callGeminiAndParse(requestBody);
        } catch (Exception e) {
            return fallbackSplitLines(rawLyrics);
        }
    }

    private List<LyricLine> callGeminiAndParse(Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String responseStr = restClient.post()
            .uri(url)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .retrieve()
            .body(String.class);

        if (responseStr == null || responseStr.isBlank()) {
            return List.of();
        }

        Map<String, Object> responseMap = JsonParserFactory.getJsonParser().parseMap(responseStr);
        if (!(responseMap.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) {
            return List.of();
        }
        if (!(candidates.get(0) instanceof Map<?, ?> firstCandidate)) {
            return List.of();
        }
        if (!(firstCandidate.get("content") instanceof Map<?, ?> content)) {
            return List.of();
        }
        if (!(content.get("parts") instanceof List<?> parts) || parts.isEmpty()) {
            return List.of();
        }
        if (!(parts.get(0) instanceof Map<?, ?> firstPart)) {
            return List.of();
        }

        String jsonText = String.valueOf(firstPart.get("text"));
        List<Object> parsedList = JsonParserFactory.getJsonParser().parseList(jsonText);
        List<LyricLine> result = new ArrayList<>();
        int order = 1;

        for (Object obj : parsedList) {
            if (!(obj instanceof Map<?, ?> item)) continue;
            LyricLine line = new LyricLine();
            line.setLineOrder(order++);
            Object timeObj = item.get("startTimeMs");
            line.setStartTimeMs(timeObj instanceof Number num ? num.intValue() : 0);
            line.setKanjiText(item.get("kanji") != null ? String.valueOf(item.get("kanji")) : "");
            line.setFuriganaText(item.get("furigana") != null ? String.valueOf(item.get("furigana")) : "");
            line.setRomajiText(item.get("romaji") != null ? String.valueOf(item.get("romaji")) : "");
            line.setVietnameseText(item.get("vietnamese") != null ? String.valueOf(item.get("vietnamese")) : "");
            result.add(line);
        }
        return result;
    }

    private List<LyricLine> fallbackSplitLines(String rawLyrics) {
        String[] lines = rawLyrics.split("\\r?\\n");
        List<LyricLine> list = new ArrayList<>();
        int order = 1;
        for (String text : lines) {
            if (text.isBlank()) continue;
            LyricLine line = new LyricLine();
            line.setLineOrder(order++);
            line.setStartTimeMs(0);
            line.setKanjiText(text.trim());
            line.setFuriganaText("");
            line.setRomajiText("");
            line.setVietnameseText("(Chưa cấu hình GEMINI_API_KEY)");
            list.add(line);
        }
        return list;
    }
}