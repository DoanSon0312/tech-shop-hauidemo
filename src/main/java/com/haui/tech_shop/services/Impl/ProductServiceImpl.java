package com.haui.tech_shop.services.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.haui.tech_shop.builder.ProductFilterBuilder;
import com.haui.tech_shop.convert.ProductFilterBuilderConverter;
import com.haui.tech_shop.dtos.requests.ProductRequest;
import com.haui.tech_shop.dtos.responses.ProductResponse;
import com.haui.tech_shop.entities.*;
import com.haui.tech_shop.repositories.*;
import com.haui.tech_shop.repositories.custome.ProductRepositoryCustom;
import com.haui.tech_shop.services.interfaces.ICartDetailService;
import com.haui.tech_shop.services.interfaces.IProductImageService;
import com.haui.tech_shop.services.interfaces.IProductService;
import com.haui.tech_shop.utils.Constant;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductFilterBuilderConverter productFilterBuilderConverter;
    private final OrderDetailRepository orderDetailRepository;
    private final IProductImageService productImageService;
    private final OrderRepository orderRepository;
    private final CartDetailRepository cartDetailRepository;
    private final RatingRepository ratingRepository;
    private final Cloudinary cloudinary;

    @Override
    public List<ProductResponse> findByNameContaining(String name) {
        List<Product> products = productRepository.findByNameContaining(name);
        List<ProductResponse> productResponses = new ArrayList<>();
        for (Product product : products) {
            productResponses.add(ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(Constant.formatter.format(product.getPrice()))
                    .thumbnail(product.getThumbnail())
                    .build());
        }
        return productResponses;
    }

    @Override
    public void decreaseStockQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId).get();
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
    }

    @Override
    public void increaseStockQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId).get();
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }


    @Override
    public void init() {

    }

    @Override
    public List<ProductResponse> getAllProducts(List<Product> products) {
        return products.stream().map(p -> getProductResponse(p.getId())).toList();
    }

    @Override
    public ProductResponse getProductResponse(Long productId) {
        Product p = productRepository.findById(productId).get();
        String oldPrice = Constant.formatter.format(p.getPrice().add(p.getPrice().multiply(BigDecimal.valueOf(0.2))));
        // Kiểm tra URL ảnh
        boolean isUrlImage = p.getThumbnail() != null && p.getThumbnail().startsWith("http");
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(Constant.formatter.format(p.getPrice()))
                .oldPrice(oldPrice)
                .thumbnail(p.getThumbnail())
                .description(p.getDescription())
                .isUrlImage(isUrlImage)
                .build();
    }


    public Page<ProductResponse> filterProducts(Map<String, Object> params, Pageable pageable) {
        // THÊM: Đảm bảo luôn filter active = true
        params.put("active", "true");

        // Chuyển đổi tham số filter từ Map sang ProductFilterBuilder
        ProductFilterBuilder builder = productFilterBuilderConverter.toProductFilterBuilder(params);

        // Lấy Page<Product> từ ProductRepository với filter và phân trang
        Page<Product> productPage = productRepository.findAll(
                ProductRepositoryCustom.filter(builder), pageable
        );

        // Chuyển đổi Page<Product> sang Page<ProductResponse>
        return productPage.map(this::convertToProductResponse);
    }

    private ProductResponse convertToProductResponse(Product p) {
        String oldPrice = Constant.formatter.format(p.getPrice().add(BigDecimal.valueOf(2000000)));
        // Logic kiểm tra ảnh URL
        boolean isUrlImage = p.getThumbnail() != null && p.getThumbnail().startsWith("http");

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(Constant.formatter.format(p.getPrice()))
                .oldPrice(oldPrice)
                .thumbnail(p.getThumbnail())
                .isUrlImage(isUrlImage)
                .build();
    }

    public String saveImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();

            // Cấu hình tham số upload
            Map params = ObjectUtils.asMap(
                    "public_id", uniqueFileName,
                    "folder", "tech_shop_products" // Folder trên Cloudinary
            );

            // Upload và lấy về URL
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteImage(String url) {
        try {
            if (url == null || url.isEmpty()) return false;

            // Lấy public_id từ URL
            String publicId = getPublicIdFromUrl(url);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                return true;
            }
        } catch (IOException e) {
            System.out.println("Lỗi xóa ảnh trên Cloudinary: " + e.getMessage());
            // Không throw exception để tránh crash luồng chính nếu xóa ảnh thất bại
        }
        return false;
    }

    private String getPublicIdFromUrl(String url) {
        try {
            // URL ví dụ: https://res.cloudinary.com/demo/image/upload/v12345678/tech_shop_products/anh1.jpg
            int startIndex = url.lastIndexOf("/");
            String filenameWithExt = url.substring(startIndex + 1); // anh1.jpg
            String filename = filenameWithExt.substring(0, filenameWithExt.lastIndexOf(".")); // anh1

            // Nếu bạn dùng folder "tech_shop_products", cần return "tech_shop_products/filename"
            // Cách đơn giản nhất là dựa vào logic folder bạn đã đặt
            if (url.contains("tech_shop_products")) {
                return "tech_shop_products/" + filename;
            }
            return filename;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean createProduct(ProductRequest productRequest, MultipartFile file) throws IOException {
        Category categoryExisting = categoryRepository.findById(productRequest.getCategoryId()).get();
        Brand brand = brandRepository.findById(productRequest.getBrandId()).get();
        try {
            String thumbnail = "";
            if (file == null) {
                // Bạn có thể để link ảnh default online hoặc giữ nguyên
                thumbnail = "https://res.cloudinary.com/djxfn3ykc/image/upload/v1/tech_shop_products/default-product.jpg";
            } else {
                if (!isValidSuffixImage(Objects.requireNonNull(file.getOriginalFilename()))) {
                    throw new BadRequestException("Image is not valid");
                }
                thumbnail = saveImage(file); // Hàm này giờ trả về URL HTTPS
            }
            Product product = Product.builder()
                    .name(productRequest.getName())
                    .description(productRequest.getDescription())
                    .price(productRequest.getPrice())
                    .cpu(productRequest.getCpu())
                    .ram(productRequest.getRam())
                    .os(productRequest.getOs())
                    .monitor(productRequest.getMonitor())
                    .battery(productRequest.getBattery())
                    .graphicCard(productRequest.getGraphicCard())
                    .port(productRequest.getPort())
                    .rearCamera(productRequest.getRearCamera())
                    .frontCamera(productRequest.getFrontCamera())
                    .stockQuantity(productRequest.getStockQuantity())
                    .warranty(productRequest.getWarranty())
                    .weight(productRequest.getWeight())
                    .thumbnail(thumbnail)
                    .category(categoryExisting)
                    .brand(brand)
                    .build();
            productRepository.save(product);
            return true;
        } catch (IOException ioe) {
            throw new IOException("Cannot create product: " + ioe.getMessage());
        }
    }

    private boolean isValidSuffixImage(String img) {
        return img.endsWith(".jpg") || img.endsWith(".jpeg") ||
                img.endsWith(".png") || img.endsWith(".gif") ||
                img.endsWith(".bmp");
    }

    @Override
    public Product updateProduct(Long productId, ProductRequest productRequest) throws IOException {
        Brand brandExisting = brandRepository.findById(productRequest.getBrandId()).get();
        Product existingProduct = productRepository.findById(productId).get();

        // Update các trường thông tin
        existingProduct.setName(productRequest.getName());
        existingProduct.setDescription(productRequest.getDescription());
        existingProduct.setPrice(productRequest.getPrice());
        existingProduct.setCpu(productRequest.getCpu());
        existingProduct.setRam(productRequest.getRam());
        existingProduct.setOs(productRequest.getOs());
        existingProduct.setMonitor(productRequest.getMonitor());
        existingProduct.setBattery(productRequest.getBattery());
        existingProduct.setGraphicCard(productRequest.getGraphicCard());
        existingProduct.setPort(productRequest.getPort());
        existingProduct.setRearCamera(productRequest.getRearCamera());
        existingProduct.setFrontCamera(productRequest.getFrontCamera());
        existingProduct.setStockQuantity(productRequest.getStockQuantity());
        existingProduct.setWarranty(productRequest.getWarranty());
        existingProduct.setWeight(productRequest.getWeight());
        existingProduct.setBrand(brandExisting);
        existingProduct.setUpdatedAt(LocalDate.now());

        return productRepository.save(existingProduct);
    }

    @Override
    public Product updateProduct(Long productId, ProductRequest productRequest, MultipartFile file) throws IOException {
        Brand brandExisting = brandRepository.findById(productRequest.getBrandId()).get();
        Product existingProduct = productRepository.findById(productId).get();

        String thumbnail = existingProduct.getThumbnail();
        if (file != null && !file.isEmpty()) {
            if (!isValidSuffixImage(Objects.requireNonNull(file.getOriginalFilename()))) {
                throw new BadRequestException("Image is not valid");
            }
            // Xóa ảnh cũ trên Cloudinary (nếu có)
            deleteImage(existingProduct.getThumbnail());
            // Upload ảnh mới
            thumbnail = saveImage(file);
        }

        // Update thông tin
        existingProduct.setName(productRequest.getName());
        existingProduct.setDescription(productRequest.getDescription());
        existingProduct.setPrice(productRequest.getPrice());
        existingProduct.setCpu(productRequest.getCpu());
        existingProduct.setRam(productRequest.getRam());
        existingProduct.setOs(productRequest.getOs());
        existingProduct.setMonitor(productRequest.getMonitor());
        existingProduct.setBattery(productRequest.getBattery());
        existingProduct.setGraphicCard(productRequest.getGraphicCard());
        existingProduct.setPort(productRequest.getPort());
        existingProduct.setRearCamera(productRequest.getRearCamera());
        existingProduct.setFrontCamera(productRequest.getFrontCamera());
        existingProduct.setStockQuantity(productRequest.getStockQuantity());
        existingProduct.setWarranty(productRequest.getWarranty());
        existingProduct.setWeight(productRequest.getWeight());
        existingProduct.setThumbnail(thumbnail);
        existingProduct.setBrand(brandExisting);
        existingProduct.setUpdatedAt(LocalDate.now());

        return productRepository.save(existingProduct);
    }

    @Transactional
    @Override
    public boolean deleteProduct(Long productId) {
        try {
            Optional<Product> optionalProduct = productRepository.findById(productId);
            if (optionalProduct.isEmpty()) {
                return false;
            }

            Product product = optionalProduct.get();

            // Soft delete - đánh dấu thời gian xóa
            product.setActive(false);
            product.setDeletedAt(LocalDateTime.now()); // THÊM DÒNG NÀY
            productRepository.save(product);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Query("SELECT p FROM Product p WHERE p.name LIKE %?1%")
    @Override
    public Product findByName(String name) {
        return productRepository.findByName(name);
    }

    @Query("SELECT p FROM Product p WHERE p.cpu LIKE %?1%")
    @Override
    public List<Product> findByCpu(String cpu) {
        return productRepository.findByCpu(cpu);
    }

    @Query("SELECT p FROM Product p WHERE p.ram LIKE %?1%")
    @Override
    public List<Product> findByRam(String ram) {
        return productRepository.findByRam(ram);
    }

    @Query("SELECT p FROM Product p WHERE p.os LIKE %?1%")
    @Override
    public List<Product> findByOs(String os) {
        return productRepository.findByOs(os);
    }

    @Query("SELECT p FROM Product p WHERE p.monitor LIKE %?1%")
    @Override
    public List<Product> findByMonitor(String monitor) {
        return productRepository.findByMonitor(monitor);
    }

    @Query("SELECT p FROM Product p WHERE p.weight = ?1")
    @Override
    public List<Product> findByWeight(Double weight) {
        return productRepository.findByWeight(weight);
    }

    @Query("SELECT p FROM Product p WHERE p.battery LIKE %?1%")
    @Override
    public List<Product> findByBattery(String battery) {
        return productRepository.findByBattery(battery);
    }

    @Query("SELECT p FROM Product p WHERE p.graphicCard LIKE %?1%")
    @Override
    public List<Product> findByGraphicCard(String graphicCard) {
        return productRepository.findByGraphicCard(graphicCard);
    }

    @Query("SELECT p FROM Product p WHERE p.port LIKE %?1%")
    @Override
    public List<Product> findByPort(String port) {
        return productRepository.findByPort(port);
    }

    @Query("SELECT p FROM Product p WHERE p.rearCamera LIKE %?1%")
    @Override
    public List<Product> findByRearCamera(String rearCamera) {
        return productRepository.findByRearCamera(rearCamera);
    }

    @Query("SELECT p FROM Product p WHERE p.frontCamera LIKE %?1%")
    @Override
    public List<Product> findByFrontCamera(String frontCamera) {
        return productRepository.findByFrontCamera(frontCamera);
    }

    @Query("SELECT p FROM Product p WHERE p.stockQuantity = ?1")
    @Override
    public List<Product> findByStockQuantity(int stockQuantity) {
        return productRepository.findByStockQuantity(stockQuantity);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public <S extends Product> S save(S entity) {
        return productRepository.save(entity);
    }

    @Override
    public Optional<Product> findById(Long aLong) {
        return productRepository.findById(aLong);
    }

    @Override
    public boolean existsById(Long aLong) {
        return productRepository.existsById(aLong);
    }

    @Override
    public long count() {
        return productRepository.count();
    }

    @Override
    public void deleteById(Long aLong) {
        productRepository.deleteById(aLong);
    }

    @Override
    public Page<Product> findActiveProducts(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
        return productRepository.findByActiveTrue(pageable);
    }

    @Override
    public List<ProductRequest> findByCategoryName(String categoryName) {
        List<ProductRequest> productDTOList = new ArrayList<>();
        List<Product> products = productRepository.findByCategoryName(categoryName);
        for (Product product : products) {
            String oldPrice = Constant.formatter.format(product.getPrice().add(BigDecimal.valueOf(2000000)));
            ProductRequest productDTO = new ProductRequest();
            BeanUtils.copyProperties(product, productDTO);
            productDTO.setOldPrice(oldPrice);
            productDTO.setImg(product.getThumbnail());
            productDTO.setUrlImage(product.getThumbnail().startsWith("https"));
            productDTOList.add(productDTO);
        }
        return productDTOList;
    }

    @Override
    public Page<Product> getAllProducts(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> getAllSortedProducts(int pageNumber, int pageSize, Sort sort) {
        return null;
    }

    @Override
    public int getTotalStockQuantity() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .mapToInt(Product::getStockQuantity)
                .sum();
    }

    @Override
    public List<Product> getTop4BestSellingProducts() {
        List<Object[]> result = orderDetailRepository.findTop4BestSellingProducts();
        List<Product> top4Products = new ArrayList<>();
        for (Object[] row : result) {
            Product product = (Product) row[0];
            top4Products.add(product);

            if (top4Products.size() >= 4) {
                break;
            }
        }
        return top4Products;
    }

    @Override
    public List<Product> get4NewProducts() {
        return productRepository.findTop4ByOrderByCreatedAtDesc();
    }

    @Override
    public List<Product> getRecentlyProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                .limit(5)
                .toList();
    }

    @Override
    public List<ProductResponse> findByBrandNameAndAndCategoryName(String brandName, String categoryName) {
        List<Product> products = productRepository.findByBrandNameAndAndCategoryName(brandName, categoryName);
        List<ProductResponse> productResponseList = new ArrayList<>();
        for (Product product : products) {
            productResponseList.add(convertToProductResponse(product));
        }
        return productResponseList;
    }

    @Override
    public Map<String, Object> getProductCategoryDistribution() {
        List<Object[]> results = productRepository.getCategoryDistribution();
        List<String> categories = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (Object[] row : results) {
            categories.add(row[0].toString());
            counts.add(((Number) row[1]).intValue());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("counts", counts);
        return data;
    }


    @Override
    public Map<String, Object> getTopSellingCategories() {
        List<Object[]> results = productRepository.getTopSellingCategoriesRaw();
        List<String> categories = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        for (Object[] row : results) {
            categories.add(row[0].toString());
            counts.add(((Number) row[1]).intValue());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("counts", counts);
        return data;
    }

    // Trong ProductService
    @Override
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    @Override
    public Optional<Product> getActiveProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id);
    }

    @Override
    public List<Product> getActiveProductsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return productRepository.findByActiveTrueAndCategory(category);
    }

    // Thêm method này nếu muốn restore sản phẩm
    @Transactional
    @Override
    public boolean restoreProduct(Long productId) {
        try {
            Optional<Product> optionalProduct = productRepository.findById(productId);
            if (optionalProduct.isEmpty()) {
                return false;
            }

            Product product = optionalProduct.get();
            product.setActive(true);
            product.setDeletedAt(null); // THÊM DÒNG NÀY - Xóa thời gian khi restore
            productRepository.save(product);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    @Override
    public boolean permanentDeleteProduct(Long productId) {
        try {
            Optional<Product> optionalProduct = productRepository.findById(productId);
            if (optionalProduct.isEmpty()) {
                return false;
            }

            Product product = optionalProduct.get();

            long orderCount = orderDetailRepository.countByProductId(productId);
            if (orderCount > 0) {
                return false;
            }

            // Xóa ratings
            if (product.getRatings() != null && !product.getRatings().isEmpty()) {
                List<Rating> ratingsToDelete = new ArrayList<>(product.getRatings());
                product.getRatings().clear();
                ratingRepository.deleteAll(ratingsToDelete);
                ratingRepository.flush();
            }

            // Xóa cart details
            if (product.getCartDetails() != null && !product.getCartDetails().isEmpty()) {
                List<CartDetail> cartDetailsToDelete = new ArrayList<>(product.getCartDetails());
                product.getCartDetails().clear();
                cartDetailRepository.deleteAll(cartDetailsToDelete);
                cartDetailRepository.flush();
            }

            // Xóa wishlist items (nếu có)
            // wishlistItemRepository.deleteByProductId(productId);

            // Xóa thumbnail
            if (product.getThumbnail() != null && !product.getThumbnail().isEmpty()) {
                try {
                    deleteImage(product.getThumbnail());
                } catch (Exception e) {
                    System.out.println("Failed to delete thumbnail: " + e.getMessage());
                }
            }

            // Xóa product images
            productImageService.deleteAllByProduct_Id(productId);

            // Xóa product
            productRepository.delete(product);
            productRepository.flush();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // THÊM METHOD NÀY - Tự động xóa sản phẩm quá 30 ngày
    @Transactional
    @Override
    public int autoDeleteExpiredProducts() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<Product> expiredProducts = productRepository
                    .findByActiveFalseAndDeletedAtBefore(thirtyDaysAgo);

            int deletedCount = 0;
            for (Product product : expiredProducts) {
                if (permanentDeleteProduct(product.getId())) {
                    deletedCount++;
                }
            }

            return deletedCount;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Page<Product> getAllActiveProducts(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return productRepository.findByActiveTrue(pageable);
    }

    @Override
    public long countProductsToAutoDelete() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return productRepository.countProductsToAutoDelete(thirtyDaysAgo);
    }

}
