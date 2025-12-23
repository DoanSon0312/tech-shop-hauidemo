package com.haui.tech_shop.services.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.haui.tech_shop.dtos.responses.ProductImageRes;
import com.haui.tech_shop.entities.Product;
import com.haui.tech_shop.entities.ProductImage;
import com.haui.tech_shop.repositories.ProductImageRepository;
import com.haui.tech_shop.repositories.ProductRepository;
import com.haui.tech_shop.services.interfaces.IProductImageService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements IProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final Cloudinary cloudinary; // Inject Cloudinary vào đây

    @Override
    public void deleteAllByProduct_Id(Long productId) {
        List<ProductImage> productImages = productImageRepository.findByProductId(productId);
        productImages.forEach(productImage -> {
            // Xóa ảnh trên Cloudinary trước khi xóa trong DB
            deleteImage(productImage.getUrl());
            productImageRepository.deleteById(productImage.getId());
        });
    }

    @Override
    public boolean createProductImages(Long productId, MultipartFile file) throws IOException {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return false;

        try {
            String imageUrl = "";
            if (file == null || file.isEmpty()) {
                // Có thể để ảnh mặc định hoặc bỏ qua
                return false;
            } else {
                if (!isValidSuffixImage(Objects.requireNonNull(file.getOriginalFilename()))) {
                    throw new BadRequestException("Image is not valid");
                }
                // Upload lên Cloudinary và lấy URL về
                imageUrl = saveImage(file);
            }

            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .url(imageUrl) // Lưu URL của Cloudinary vào DB
                    .build();
            productImageRepository.save(productImage);
            return true;
        } catch (IOException ioe) {
            throw new IOException("Cannot create product image: " + ioe.getMessage());
        }
    }

    // Hàm này được sửa đổi để upload lên Cloudinary
    public String saveImage(MultipartFile file) throws IOException {
        // Tạo tên file unique (tùy chọn, Cloudinary có thể tự sinh public_id)
        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();

        // Tham số cấu hình upload
        Map params = ObjectUtils.asMap(
                "public_id", uniqueFileName, // Đặt tên file trên cloud
                "folder", "tech_shop_products" // Gom ảnh vào thư mục riêng cho gọn
        );

        // Thực hiện upload
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

        // Trả về đường dẫn bảo mật (https)
        return uploadResult.get("secure_url").toString();
    }

    private boolean isValidSuffixImage(String img) {
        String lowerCaseImg = img.toLowerCase();
        return lowerCaseImg.endsWith(".jpg") || lowerCaseImg.endsWith(".jpeg") ||
                lowerCaseImg.endsWith(".png") || lowerCaseImg.endsWith(".gif") ||
                lowerCaseImg.endsWith(".bmp");
    }

    @Override
    public List<ProductImage> findByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

    @Override
    public List<ProductImageRes> getProductImages(Long productId) {
        List<ProductImage> productImages = this.findByProductId(productId);
        return productImages.stream()
                .map(productImage -> ProductImageRes.builder()
                        .url(productImage.getUrl())
                        .product(productImage.getProduct())
                        .isUrlImage(true) // Vì giờ tất cả đều là URL từ Cloudinary
                        .build()).toList();
    }

    @Override
    public void deleteById(Integer integer) {
        ProductImage productImage = productImageRepository.findById(integer).orElse(null);
        if (productImage != null) {
            deleteImage(productImage.getUrl());
            productImageRepository.deleteById(integer);
        }
    }

    // Hàm xóa ảnh trên Cloudinary
    public boolean deleteImage(String url) {
        try {
            // Cần lấy public_id từ URL để xóa
            // URL dạng: https://res.cloudinary.com/.../folder/filename.jpg
            String publicId = getPublicIdFromUrl(url);

            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace(); // Log lỗi nhưng không throw để tránh crash luồng chính
        }
        return false;
    }

    // Hàm phụ trợ để tách Public ID từ URL
    private String getPublicIdFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            // Tách lấy phần sau dấu / cuối cùng và trước dấu . (extension)
            // Ví dụ đơn giản (Cần điều chỉnh nếu URL phức tạp hơn hoặc có versioning)
            int startIndex = url.lastIndexOf("/");
            int endIndex = url.lastIndexOf(".");

            // Nếu dùng folder, cần xử lý thêm.
            // Cách an toàn nhất cho cấu trúc tech_shop_products/filename:
            String[] parts = url.split("/");
            String filenameWithExt = parts[parts.length - 1];
            String filename = filenameWithExt.substring(0, filenameWithExt.lastIndexOf("."));

            // Nếu có folder "tech_shop_products"
            return "tech_shop_products/" + filename;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Optional<ProductImage> findById(Integer integer) {
        return productImageRepository.findById(integer);
    }

    @Override
    public Optional<Product> findById(Long aLong) {
        return productRepository.findById(aLong);
    }
}