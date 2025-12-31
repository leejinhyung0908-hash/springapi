package site.protoa.api.auth_service.log;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.protoa.api.auth_service.log.dto.LogRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/log")
public class LogController {

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> logLogin(@RequestBody LogRequest request) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy. MM. dd. a h:mm:ss", Locale.KOREAN));

            System.out.println("\n" + "=".repeat(60));
            System.out.println("[" + timestamp + "] 🔹 " + request.getAction());
            System.out.println("URL: " + (request.getUrl() != null ? request.getUrl() : "N/A"));
            if (request.getTokenLength() != null) {
                System.out.println("Token Length: " + request.getTokenLength());
            }
            System.out.println("=".repeat(60) + "\n");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그가 기록되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ 로그인 로그 기록 실패: " + e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "로그 기록 실패");

            return ResponseEntity.status(500).body(response);
        }
    }
}
