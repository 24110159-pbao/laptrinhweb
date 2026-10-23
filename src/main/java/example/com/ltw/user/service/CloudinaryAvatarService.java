package example.com.ltw.user.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryAvatarService {
    private static final long MAX_AVATAR_BYTES = 5 * 1024 * 1024;
    private final Cloudinary cloudinary;

    public CloudinaryAvatarService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh đại diện.");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Ảnh đại diện không được vượt quá 5 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|webp)")) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "nexustech/avatars",
                    "resource_type", "image"
            ));
            Object secureUrl = result.get("secure_url");
            if (!(secureUrl instanceof String url) || url.isBlank()) {
                throw new IllegalStateException("Cloudinary không trả về URL ảnh đại diện.");
            }
            return url;
        } catch (IOException ex) {
            log.error("Unable to read uploaded avatar", ex);
            throw new IllegalStateException("Không thể đọc ảnh đại diện tải lên.", ex);
        } catch (Exception ex) {
            log.error("Cloudinary avatar upload failed", ex);
            throw new IllegalStateException("Không thể tải ảnh lên Cloudinary. Vui lòng thử lại.", ex);
        }
    }
}
