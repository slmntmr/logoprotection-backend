package logoprotection_backend.controller;

import logoprotection_backend.service.LogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController // REST API controller olduğunu belirtir
@RequestMapping("/api/logos") // API'nin ana yolu (endpoint başlangıcı)
@RequiredArgsConstructor // LogoService'in constructor injection'ını sağlar (final ile)
public class LogoController {

    private final LogoService logoService;

    // Kullanıcıdan logo yüklemek için kullanılan endpoint
    @PostMapping("/upload")
    public ResponseEntity<String> uploadLogo(@RequestParam("file") MultipartFile file) {
        // LogoService'e logoyu gönder, yükle ve koruma altına al
        String fileHash = logoService.uploadAndProtectLogo(file);

        // Başarılı işlem sonrası kullanıcıya bilgilendirme mesajı ve hash döner
        return ResponseEntity.ok("Logo başarıyla koruma altına alındı. Hash: " + fileHash);
    }
}
