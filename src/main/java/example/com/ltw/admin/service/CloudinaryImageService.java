package example.com.ltw.admin.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageService {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_IMAGES_PER_PRODUCT = 8;

    private final Cloudinary cloudinary;

    public List<UploadedImage> upload(List<MultipartFile> files) {
        List<MultipartFile> selected = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (selected.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException("Mỗi sản phẩm được tải lên tối đa 8 ảnh.");
        }
        selected.forEach(this::validate);

        List<UploadedImage> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : selected) {
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "products",
                                "resource_type", "image",
                                "overwrite", false
                        )
                );
                Object url = result.get("secure_url");
                Object publicId = result.get("public_id");
                if (!(url instanceof String imageUrl) || !(publicId instanceof String id)) {
                    throw new IllegalStateException("Cloudinary không trả về URL hoặc public ID của ảnh.");
                }
                uploaded.add(new UploadedImage(imageUrl, id));
            }
            return uploaded;
        } catch (IOException | RuntimeException ex) {
            List<String> failures = delete(uploaded.stream().map(UploadedImage::getPublicId).toList());
            if (!failures.isEmpty()) {
                log.error("Cloudinary cleanup failed after partial product image upload: {}", failures);
            }
            throw new IllegalStateException("Tải ảnh sản phẩm lên Cloudinary thất bại.", ex);
        }
    }

    public List<String> delete(List<String> publicIds) {
        List<String> failures = new ArrayList<>();
        if (publicIds == null) {
            return failures;
        }
        for (String publicId : publicIds) {
            if (publicId == null || publicId.isBlank()) {
                continue;
            }
            try {
                Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                Object status = result.get("result");
                if (!"ok".equals(status) && !"not found".equals(status)) {
                    failures.add(publicId);
                    log.error("Cloudinary did not confirm deletion of product image {}", publicId);
                }
            } catch (IOException | RuntimeException ex) {
                failures.add(publicId);
                log.error("Cloudinary deletion failed for product image {}", publicId, ex);
            }
        }
        return failures;
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Mỗi ảnh không được vượt quá 5 MB.");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = file.getBytes();
            boolean jpeg = contentType.equals("image/jpeg") && bytes.length >= 3
                    && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
            boolean png = contentType.equals("image/png") && bytes.length >= 8
                    && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
            boolean webp = contentType.equals("image/webp") && bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            if (!jpeg && !png && !webp) {
                throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP đúng định dạng.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc nội dung ảnh đã chọn.", ex);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class UploadedImage {
        private final String imageUrl;
        private final String publicId;
    }
}
