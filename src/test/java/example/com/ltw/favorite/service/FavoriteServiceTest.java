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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private FavoriteService favoriteService;

    @Test
    void addFavoriteCreatesUserScopedRecordForActiveProduct() {
        User user = new User();
        Product product = new Product();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(2L, ProductStatus.ACTIVE)).thenReturn(Optional.of(product));
        when(favoriteRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(false);

        assertTrue(favoriteService.addFavorite(1L, 2L));

        org.mockito.ArgumentCaptor<Favorite> favoriteCaptor = org.mockito.ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(favoriteCaptor.capture());
        assertEquals(user, favoriteCaptor.getValue().getUser());
        assertEquals(product, favoriteCaptor.getValue().getProduct());
    }

    @Test
    void addFavoriteDoesNotCreateDuplicate() {
        User user = new User();
        Product product = new Product();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStatus(2L, ProductStatus.ACTIVE)).thenReturn(Optional.of(product));
        when(favoriteRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(true);

        assertFalse(favoriteService.addFavorite(1L, 2L));

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void addFavoriteRejectsProductsThatAreNotActive() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new User()));
        when(productRepository.findByIdAndStatus(2L, ProductStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> favoriteService.addFavorite(1L, 2L));

        verify(favoriteRepository, never()).save(any(Favorite.class));
        verify(favoriteRepository, never()).existsByUserIdAndProductId(1L, 2L);
    }

    @Test
    void getFavoritesRequestsOnlyActiveProductsAndMapsThem() {
        Favorite favorite = new Favorite();
        Product product = new Product();
        favorite.setProduct(product);
        ProductCardDto card = new ProductCardDto();
        when(favoriteRepository.findVisibleByUserId(1L, ProductStatus.ACTIVE)).thenReturn(List.of(favorite));
        when(productService.mapToCardDto(product)).thenReturn(card);

        assertEquals(List.of(card), favoriteService.getFavorites(1L));

        verify(favoriteRepository).findVisibleByUserId(1L, ProductStatus.ACTIVE);
    }

    @Test
    void removeFavoriteDeletesOnlyMatchingCustomersFavorite() {
        User user = new User();
        Favorite favorite = new Favorite();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserIdAndProductId(1L, 2L)).thenReturn(Optional.of(favorite));

        assertTrue(favoriteService.removeFavorite(1L, 2L));

        verify(favoriteRepository).findByUserIdAndProductId(1L, 2L);
        verify(favoriteRepository).delete(favorite);
    }

    @Test
    void removeFavoriteDoesNotDeleteWhenCustomerDoesNotOwnIt() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new User()));
        when(favoriteRepository.findByUserIdAndProductId(1L, 2L)).thenReturn(Optional.empty());

        assertFalse(favoriteService.removeFavorite(1L, 2L));

        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }
}
