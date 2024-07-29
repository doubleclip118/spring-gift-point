package gift.service;

import gift.controller.dto.WishRequest;
import gift.domain.Product;
import gift.domain.UserInfo;
import gift.domain.Wish;
import gift.repository.ProductRepository;
import gift.repository.UserInfoRepository;
import gift.repository.WishRepository;
import gift.utils.error.ProductNotFoundException;
import gift.utils.error.UserNotFoundException;
import gift.utils.error.WishListNotFoundException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishService {

    private final WishRepository wishRepository;
    private final UserInfoRepository userInfoRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, UserInfoRepository userInfoRepository,
        ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.userInfoRepository = userInfoRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public boolean addToWishlist(String email, WishRequest wishRequest) {
        Product product = productRepository.findById(wishRequest.getProductId()).orElseThrow(
            () -> new ProductNotFoundException("Product Not Found")
        );
        UserInfo byEmail = userInfoRepository.findByEmail(email).orElseThrow(
            () -> new UserNotFoundException("User Not Found"));
        Wish wish = new Wish(product, byEmail, wishRequest.getQuantity());

        product.addWish(wish);
        byEmail.addWish(wish);

        wishRepository.save(wish);
        return true;

    }
    @Transactional
    public boolean removeFromWishlist(String email, Long productId) {
        UserInfo userInfo = userInfoRepository.findByEmail(email).orElseThrow(
            () -> new UserNotFoundException("User Not Found")
        );
        Product product = productRepository.findById(productId).orElseThrow(
            () -> new ProductNotFoundException("Product Not Found")
        );
        if (!wishRepository.existsByUserInfoIdAndProductId(userInfo.getId(), productId)) {
            throw new WishListNotFoundException("Not Found");
        }
        Wish wish = wishRepository.findByUserInfoIdAndProductId(userInfo.getId(), productId).orElseThrow(
            () -> new WishListNotFoundException("Wish Not Found")
        );

        product.removeWish(wish);
        userInfo.removeWish(wish);

        wishRepository.deleteByProductIdAndUserInfoId(productId, userInfo.getId());
        return true;

    }

    public Page<Wish> getWishlistProducts(String email, Pageable pageable) {
        UserInfo userInfo = userInfoRepository.findByEmail(email).orElseThrow(
            () -> new UserNotFoundException("User Not Found")
        );
        return wishRepository.findByUserInfoId(userInfo.getId(), pageable);
    }
    @Transactional
    public boolean changeToWishlist(String email, WishRequest wishRequest) {
        Product product = productRepository.findById(wishRequest.getProductId()).orElseThrow(
            () -> new ProductNotFoundException("Product Not Found")
        );
        UserInfo userInfo = userInfoRepository.findByEmail(email).orElseThrow(
            () -> new UserNotFoundException("User Not Found")
        );
        Wish existingWish = wishRepository.findByUserInfoIdAndProductId(userInfo.getId(),
            product.getId()).orElseThrow(
            () -> new WishListNotFoundException("Wish Not Found")
        );

        if (wishRequest.getQuantity() == 0) {
            if (existingWish != null) {
                product.removeWish(existingWish);
                userInfo.removeWish(existingWish);
                wishRepository.delete(existingWish);
            }
            return true;
        }
        if (existingWish == null) {
            throw new ProductNotFoundException("Product Not Found");
        }
        existingWish.setQuantity(wishRequest.getQuantity());
        return true;

    }


}
