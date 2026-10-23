package example.com.ltw.favorite.service;

import example.com.ltw.catalog.dto.ProductCardDto;
import example.com.ltw.catalog.entity.Product;
import example.com.ltw.catalog.entity.ProductStatus;
import example.com.ltw.catalog.repository.ProductRepository;
import example.com.ltw.catalog.service.ProductService;
import example.com.ltw.favorite.entity.Favorite;
import example.com.ltw.favorite.repository.FavoriteRepository;
import example.com.ltw.user.entity.User;
import example.com.ltw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional
    public boolean addFavorite(Long userId, Long productId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc hiện không được kinh doanh."));

        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return false;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);
        favoriteRepository.save(favorite);
        return true;
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long productId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng."));
        return favoriteRepository.findByUserIdAndProductId(userId, productId)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getFavorites(Long userId) {
        return favoriteRepository.findVisibleByUserId(userId, ProductStatus.ACTIVE).stream()
                .map(Favorite::getProduct)
                .map(productService::mapToCardDto)
                .toList();
    }
}
