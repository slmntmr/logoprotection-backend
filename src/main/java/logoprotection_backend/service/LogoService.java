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

    // 📌 Yüklenen logoların saklanacağı KESİN dizin (Geçici değil, sabit klasör)
    private static final String LOGO_DIR = System.getProperty("user.dir") + File.separator + "uploaded-logos";

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
            log.error("Dosya yükleme veya koruma sırasında hata oluştu: ", e);
            throw new RuntimeException("Dosya yüklenirken hata oluştu: " + e.getMessage());
        }
    }

    // --- Yardımcı Metotlar ---

    // MultipartFile dosyasını belirlenen dizine kaydeder
    private File saveFile(MultipartFile file) throws IOException {
        File directory = new File(LOGO_DIR);

        // 📌 Eğer klasör yoksa oluştur
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IOException("Klasör oluşturulamadı: " + directory.getAbsolutePath());
            }
        }

        // 📌 Dosya adını güvenli hale getir (boşlukları ve özel karakterleri kaldır)
        String safeFileName = file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9\\.\\-]", "_"); // Özel karakterleri "_" ile değiştir

        File destinationFile = new File(directory, safeFileName);
        file.transferTo(destinationFile);

        log.info("Dosya başarıyla kaydedildi: {}", destinationFile.getAbsolutePath());

        return destinationFile;
    }

    // Dosyanın SHA-256 hash değerini hesaplar
    private String generateSHA256Hash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Dosyanın tüm byte'larını oku
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        log.info("SHA-256 hash hesaplanıyor, dosya boyutu: {}", fileBytes.length);

        // Byte dizisini SHA-256 ile hash'le
        byte[] hashBytes = digest.digest(fileBytes);

        // Hash byte dizisini hexadecimal (16'lık) String'e çevir
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            hexString.append(String.format("%02x", b));
        }

        log.info("SHA-256 hash başarıyla oluşturuldu: {}", hexString.toString());

        return hexString.toString();
    }

    // Oluşan hash değerini OpenTimestamps aracılığıyla Blockchain'e gönderir
    private void sendHashToOpenTimestamps(byte[] sha256Hash) {
        RestTemplate restTemplate = new RestTemplate();
        String otsUrl = "https://a.pool.opentimestamps.org/digest";

        try {
            log.info("Blockchain'e hash gönderiliyor: {}", sha256Hash);

            // OpenTimestamps REST API çağrısı
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    otsUrl,
                    sha256Hash,
                    byte[].class
            );

            log.info("OpenTimestamps yanıt kodu: {}", response.getStatusCode());

            // Başarılı yanıt kontrolü
            if (response.getStatusCode() == HttpStatus.OK) {
                byte[] timestampProof = response.getBody();
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
