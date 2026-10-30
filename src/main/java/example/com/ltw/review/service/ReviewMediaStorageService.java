package example.com.ltw.review.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import example.com.ltw.review.entity.ReviewMedia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ReviewMediaStorageService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp",
            "video/mp4", "video/webm", "video/quicktime"
    );

    private final Cloudinary cloudinary;
    private final Set<String> allowedTypes;
    private final long maxFileSizeBytes;
    private final long maxVideoDurationSeconds;

    public ReviewMediaStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret,
            @Value("${reviews.media.allowed-types:image/jpeg,image/png,image/webp,video/mp4,video/webm,video/quicktime}")
            String allowedTypes,
            @Value("${reviews.media.max-file-size-bytes:20971520}") long maxFileSizeBytes,
            @Value("${reviews.media.max-video-duration-seconds:60}") long maxVideoDurationSeconds
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        this.allowedTypes = java.util.Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!SUPPORTED_TYPES.containsAll(this.allowedTypes)
                || this.allowedTypes.isEmpty()
                || maxFileSizeBytes <= 0
                || maxVideoDurationSeconds <= 0) {
            throw new IllegalArgumentException("Cấu hình giới hạn tệp đánh giá không hợp lệ.");
        }
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxVideoDurationSeconds = maxVideoDurationSeconds;
    }

    public ReviewMedia upload(MultipartFile file) {
        DetectedMedia detected = detect(file);
        if (!allowedTypes.contains(detected.contentType())) {
            throw new IllegalArgumentException("Định dạng tệp đánh giá không được hỗ trợ.");
        }

        try {
            Map<String, Object> options = new java.util.HashMap<>();
            options.put("folder", "nexustech/reviews");
            options.put("resource_type", detected.resourceType());

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            String url = result.get("secure_url") instanceof String value ? value : null;
            String publicId = result.get("public_id") instanceof String value ? value : null;
            if (url == null || url.isBlank() || publicId == null || publicId.isBlank()) {
                throw new IllegalStateException("Dịch vụ lưu trữ không trả về thông tin tệp đánh giá.");
            }

            ReviewMedia media = new ReviewMedia();
            media.setUrl(url);
            media.setPublicId(publicId);
            media.setResourceType(detected.resourceType());

            if (detected.resourceType().equals("video")) {
                Object durationValue = result.get("duration");
                if (!(durationValue instanceof Number duration)
                        || duration.doubleValue() > maxVideoDurationSeconds) {
                    delete(publicId, "video");
                    throw new IllegalArgumentException(
                            "Video đánh giá phải có thời lượng tối đa " + maxVideoDurationSeconds + " giây."
                    );
                }
            }
            return media;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("Unable to read review media upload", ex);
            throw new IllegalStateException("Không thể đọc tệp đánh giá.", ex);
        } catch (Exception ex) {
            log.error("Review media upload failed", ex);
            throw new IllegalStateException("Không thể tải tệp đánh giá lên. Vui lòng thử lại.", ex);
        }
    }

    public void delete(String publicId, String resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception ex) {
            log.error("Unable to delete review media {}", publicId, ex);
        }
    }

    private DetectedMedia detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp đánh giá không được để trống.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "Mỗi tệp đánh giá không được vượt quá " + maxFileSizeBytes + " byte."
            );
        }
        String declaredType = file.getContentType();
        if (declaredType == null) {
            throw new IllegalArgumentException("Không xác định được định dạng tệp đánh giá.");
        }
        String contentType = declaredType.toLowerCase(Locale.ROOT);
        try {
            byte[] header = file.getBytes();
            String detected;
            if (startsWith(header, 0xff, 0xd8, 0xff)) {
                detected = "image/jpeg";
            } else if (startsWith(header, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) {
                detected = "image/png";
            } else if (header.length >= 12 && ascii(header, 0, 4).equals("RIFF")
                    && ascii(header, 8, 4).equals("WEBP")) {
                detected = "image/webp";
            } else if (header.length >= 12 && ascii(header, 4, 4).equals("ftyp")) {
                detected = ascii(header, 8, 4).equals("qt  ") ? "video/quicktime" : "video/mp4";
            } else if (startsWith(header, 0x1a, 0x45, 0xdf, 0xa3)) {
                detected = "video/webm";
            } else {
                throw new IllegalArgumentException("Nội dung tệp không thuộc định dạng ảnh hoặc video được hỗ trợ.");
            }
            if (!contentType.equals(detected)) {
                throw new IllegalArgumentException("Định dạng tệp không khớp với nội dung thực tế.");
            }
            return new DetectedMedia(detected, detected.startsWith("video/") ? "video" : "image");
        } catch (IOException ex) {
            log.error("Unable to inspect review media upload", ex);
            throw new IllegalStateException("Không thể đọc tệp đánh giá.", ex);
        }
    }

    private boolean startsWith(byte[] data, int... values) {
        if (data.length < values.length) {
            return false;
        }
        for (int index = 0; index < values.length; index++) {
            if ((data[index] & 0xff) != values[index]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] data, int offset, int length) {
        return new String(data, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private record DetectedMedia(String contentType, String resourceType) {
    }
}