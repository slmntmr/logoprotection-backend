package logoprotection_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service // Spring tarafından servis katmanı olarak tanımlanır
@Slf4j // Loglama için (log.info, log.error vb.)
public class LogoService {

    // Yüklenen logoların saklanacağı klasör yolu
    private static final String LOGO_DIR = "uploaded-logos/";

    // Kullanıcının yüklediği logoyu sunucuya kaydeder ve blockchain koruması sağlar
    public String uploadAndProtectLogo(MultipartFile file) {
        try {
            // 1. Gelen logoyu sunucuya kaydet
            File storedFile = saveFile(file);

            // 2. Logonun SHA-256 hash'ini oluştur
            String fileHash = generateSHA256Hash(storedFile);

            // 3. Hash değerini blockchain'e göndererek zaman damgası oluştur
            sendHashToOpenTimestamps(hexStringToByteArray(fileHash));

            log.info("Logo başarıyla koruma altına alındı, Hash: {}", fileHash);

            // 4. Hash değerini kullanıcıya geri döndür
            return fileHash;

        } catch (Exception e) {
            log.error("Dosya yükleme veya koruma sırasında hata oluştu: {}", e.getMessage());
            throw new RuntimeException("Dosya yüklenirken hata oluştu.");
        }
    }

    // --- Yardımcı Metotlar ---

    // MultipartFile dosyasını belirlenen dizine kaydeder
    private File saveFile(MultipartFile file) throws IOException {
        File directory = new File(LOGO_DIR);

        // Klasör mevcut değilse oluştur
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Logoyu belirtilen dizine kaydet
        File destinationFile = new File(directory, file.getOriginalFilename());
        file.transferTo(destinationFile);

        log.info("Dosya başarıyla kaydedildi: {}", destinationFile.getAbsolutePath());

        return destinationFile;
    }

    // Dosyanın SHA-256 hash değerini hesaplar
    private String generateSHA256Hash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Dosyanın tüm byte'larını oku
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        // Byte dizisini SHA-256 ile hash'le
        byte[] hashBytes = digest.digest(fileBytes);

        // Hash byte dizisini hexadecimal (16'lık) String'e çevir
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    // Oluşan hash değerini OpenTimestamps aracılığıyla Blockchain'e gönderir
    private void sendHashToOpenTimestamps(byte[] sha256Hash) {
        RestTemplate restTemplate = new RestTemplate();
        String otsUrl = "https://a.pool.opentimestamps.org/digest";

        try {
            // OpenTimestamps REST API çağrısı (hash'i gönder)
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    otsUrl,
                    sha256Hash,
                    byte[].class
            );

            // Başarılı ise yanıtı kontrol eder
            if (response.getStatusCode() == HttpStatus.OK) {
                byte[] timestampProof = response.getBody();
                // İsteğe bağlı: timestampProof verisini daha sonra doğrulamak için saklayabilirsin
                log.info("Blockchain üzerinde zaman damgası başarıyla oluşturuldu.");
            } else {
                log.error("OpenTimestamps başarısız oldu. Status: {}", response.getStatusCode());
                throw new RuntimeException("OpenTimestamps ile blockchain zaman damgası oluşturma başarısız.");
            }

        } catch (Exception e) {
            log.error("Blockchain entegrasyonu sırasında hata oluştu: {}", e.getMessage());
            throw new RuntimeException("Blockchain entegrasyonu sırasında hata oluştu.");
        }
    }

    // Hexadecimal string'i byte dizisine çeviren yardımcı metot
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
