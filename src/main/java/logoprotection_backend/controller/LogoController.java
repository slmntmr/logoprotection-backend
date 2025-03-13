package logoprotection_backend.controller;

import logoprotection_backend.service.LogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 📌 Eksik olan bu satırı ekledik
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController // REST API controller olduğunu belirtir
@RequestMapping("/api/logos") // API'nin ana yolu (endpoint başlangıcı)
@RequiredArgsConstructor // LogoService'in constructor injection'ını sağlar (final ile)
@Slf4j // 📌 Loglama için eklenmesi gereken anotasyon
public class LogoController {

    private final LogoService logoService;

    // Kullanıcıdan logo yüklemek için kullanılan endpoint
    // http://localhost:8080/api/logos/upload
    @PostMapping("/upload")
    public ResponseEntity<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        log.info("Gelen dosya: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            log.error("Dosya boş geldi!");
            return ResponseEntity.badRequest().body("Dosya yüklenemedi! Lütfen geçerli bir dosya seçin.");
        }

        try {
            log.info("Dosya işleniyor...");
            String fileHash = logoService.uploadAndProtectLogo(file);
            log.info("Dosya başarıyla işlendi. Hash: {}", fileHash);
            return ResponseEntity.ok("Logo başarıyla koruma altına alındı. Hash: " + fileHash);
        } catch (Exception e) {
            log.error("Dosya işlenirken hata oluştu: ", e);
            return ResponseEntity.internalServerError().body("Dosya işlenirken hata oluştu: " + e.getMessage());
        }
    }


}
