package com.haui.tech_shop.controllers.manager;

import com.haui.tech_shop.dtos.requests.ProductRequest;
import com.haui.tech_shop.entities.Product;
import com.haui.tech_shop.entities.ProductImage;
import com.haui.tech_shop.repositories.ProductRepository;
import com.haui.tech_shop.services.interfaces.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@RequestMapping("/manager/products")
public class ProductController {
    @Autowired
    IProductService productService;
    @Autowired
    IBrandService brandService;
    @Autowired
    ICategoryService categoryService;
    @Autowired
    IProductImageService productImageService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    IOCRService ocrService;

    @GetMapping("") // localhost:8080/manager/products
    public String index(Model model,
                        @RequestParam(defaultValue = "0") int pageNumber,
                        @RequestParam(defaultValue = "8") int pageSize) {
        // Sử dụng phân trang thay vì findAll()
        Page<Product> currentPage = productService.findActiveProducts(pageNumber, pageSize);
        List<Product> products = currentPage.getContent();

        model.addAttribute("products", products);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", currentPage.getTotalPages());

        return "manager/products/productlist";
    }

    @GetMapping("/productDetail")
    public String computerDetail(Model model, @RequestParam Long id) {
        Optional<Product> product = productService.findById(id);
        List<ProductImage> images = productImageService.findByProductId(id);
        model.addAttribute("product", product);
        model.addAttribute("productImages", images);
        return "manager/products/productDetail";
    }

    // ENDPOINT MỚI: Thêm sản phẩm từ ảnh sử dụng OCR
    @GetMapping("/add-from-image")
    public String addFromImage(Model model) {
        ProductRequest productDTO = new ProductRequest();
        model.addAttribute("productDto", productDTO);
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        return "manager/products/addproduct-ocr";
    }

    // API endpoint để xử lý OCR và trả về JSON
    @PostMapping("/ocr-extract")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> extractFromImage(
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Please select an image file");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra file có phải là ảnh không
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("status", "error");
                response.put("message", "File must be an image");
                return ResponseEntity.badRequest().body(response);
            }

            // Extract text từ ảnh
            String ocrText = ocrService.extractTextFromImage(file);

            // Parse thành ProductRequest
            ProductRequest productRequest = ocrService.parseProductFromOCR(ocrText);

            // Điền giá trị mặc định cho các trường trống
            ocrService.fillDefaultValues(productRequest);

            response.put("status", "success");
            response.put("ocrText", ocrText);
            response.put("productData", productRequest);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error processing image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Thêm sản phẩm từ OCR với khả năng chỉnh sửa trước khi lưu
    @PostMapping("/create-from-ocr")
    public String createFromOCR(
            @Valid @ModelAttribute("productDto") ProductRequest productDTO,
            BindingResult bindingResult,
            @RequestParam("files") MultipartFile file,
            @RequestParam("categoryId") Long categoryId,
            Model model) throws IOException {

        if (bindingResult.hasErrors()) {
            model.addAttribute("productDto", productDTO);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("brands", brandService.findAll());
            return "manager/products/addproduct-ocr";
        }

        // Điền giá trị mặc định cho các trường trống
        ocrService.fillDefaultValues(productDTO);

        productDTO.setCategoryId(categoryId);

        if (productService.createProduct(productDTO, file)) {
            Product savedProduct = productService.findByName(productDTO.getName());
            productImageService.createProductImages(savedProduct.getId(), file);
            return "redirect:/manager/products";
        }

        return "manager/products/addproduct-ocr";
    }

    // Thêm sản phẩm tự động hoàn toàn từ ảnh (không cần xác nhận)
    @PostMapping("/auto-create-from-image")
    public ResponseEntity<Map<String, String>> autoCreateFromImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("brandId") Long brandId) {

        Map<String, String> response = new HashMap<>();

        try {
            // Extract text từ ảnh
            String ocrText = ocrService.extractTextFromImage(file);

            // Parse thành ProductRequest
            ProductRequest productRequest = ocrService.parseProductFromOCR(ocrText);

            // Điền giá trị mặc định
            ocrService.fillDefaultValues(productRequest);

            // Set category và brand
            productRequest.setCategoryId(categoryId);
            productRequest.setBrandId(brandId);

            // Lưu vào database
            if (productService.createProduct(productRequest, file)) {
                Product savedProduct = productService.findByName(productRequest.getName());
                productImageService.createProductImages(savedProduct.getId(), file);

                response.put("status", "success");
                response.put("message", "Product created successfully from image");
                response.put("productId", savedProduct.getId().toString());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to create product");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/add/computer")
    public String addComputer(Model model) {
        ProductRequest productDTO = new ProductRequest();
        model.addAttribute("productDto", productDTO);
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("categoryId", 1);
        model.addAttribute("categoryChoice", "computer");
        return "manager/products/addproduct";
    }

    @GetMapping("/add/phone")
    public String addPhone(Model model) {
        ProductRequest productDTO = new ProductRequest();
        model.addAttribute("productDto", productDTO);
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("categoryId", 2);
        model.addAttribute("categoryChoice", "phone");
        return "manager/products/addproduct";
    }

    @GetMapping("/add/accessory")
    public String addAccessory(Model model) {
        ProductRequest productDTO = new ProductRequest();
        model.addAttribute("productDto", productDTO);
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("categoryId", 3);
        model.addAttribute("categoryChoice", "accessory");
        return "manager/products/addproduct";
    }

    @GetMapping("/edit")
    public String edit(Model model, @RequestParam Long id) {
        Product product = productService.findById(id).get();
        model.addAttribute("productID", id);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("brands", brandService.findAll());
        model.addAttribute("product", product);
        if (product.getCategory().getName().equals("Computer")) {
            return "manager/products/editComputer";
        }
        if (product.getCategory().getName().equals("Phone")) {
            return "manager/products/editPhone";
        }
        if (product.getCategory().getName().equals("Accessory")) {
            return "manager/products/editAccessory";
        }
        return "redirect:manager/products";
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable(value = "id") Long id) {
        Map<String, String> response = new HashMap<>();
        if (productService.deleteProduct(id)) {
            response.put("status", "success");
            response.put("message", "Product deleted successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to delete product.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/deleted")
    public String deletedProducts(Model model) {
        List<Product> deletedProducts = productRepository.findByActiveFalse();
        model.addAttribute("products", deletedProducts);

        // Tính số ngày còn lại trước khi tự động xóa
        Map<Long, Long> daysUntilDeletion = new HashMap<>();

        for (Product product : deletedProducts) {
            if (product.getDeletedAt() != null) {
                LocalDateTime autoDeleteDate = product.getDeletedAt().plusDays(30);
                long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), autoDeleteDate);
                daysUntilDeletion.put(product.getId(), Math.max(0, daysLeft));
            }
        }

        model.addAttribute("daysUntilDeletion", daysUntilDeletion);
        return "manager/products/deleted";
    }

    // Endpoint để restore
    @GetMapping("/restore/{id}")
    public ResponseEntity<Map<String, String>> restore(@PathVariable(value = "id") Long id) {
        Map<String, String> response = new HashMap<>();
        if (productService.restoreProduct(id)) {
            response.put("status", "success");
            response.put("message", "Product restored successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to restore product.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Endpoint xóa vĩnh viễn
    @GetMapping("/permanent-delete/{id}")
    public ResponseEntity<Map<String, String>> permanentDelete(@PathVariable(value = "id") Long id) {
        Map<String, String> response = new HashMap<>();
        if (productService.permanentDeleteProduct(id)) {
            response.put("status", "success");
            response.put("message", "Product permanently deleted.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Cannot delete product. It may have orders or doesn't exist.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Endpoint kích hoạt xóa tự động ngay (cho admin)
    @GetMapping("/auto-delete-now")
    public ResponseEntity<Map<String, Object>> triggerAutoDelete() {
        Map<String, Object> response = new HashMap<>();
        int deletedCount = productService.autoDeleteExpiredProducts();
        response.put("status", "success");
        response.put("deletedCount", deletedCount);
        response.put("message", "Auto-deleted " + deletedCount + " expired products.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/images")
    public String images(Model model, @RequestParam Long id) {
        List<ProductImage> images = productImageService.findByProductId(id);
        model.addAttribute("id", id);
        model.addAttribute("productImages", images);
        return "manager/products/images";
    }

    @GetMapping("/images/delete")
    public String deleteImage(@RequestParam("image_id") Integer imageId) {
        productImageService.deleteById(imageId);
        return "redirect:/manager/products";
    }

    @PostMapping("/create")
    public String insert(@Valid @ModelAttribute("productDto") ProductRequest productDTO,
                         BindingResult bindingResult,
                         @RequestParam("files") MultipartFile file,
                         @RequestParam("categoryId") Long categoryId,
                         @RequestParam("categoryChoice") String categoryChoice,
                         Model model) throws IOException {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productDto", productDTO);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("categoryId", categoryId);
            model.addAttribute("categoryChoice", categoryChoice);
            return "manager/products/addproduct";
        }
        productDTO.setCategoryId(categoryId);
        if (productService.createProduct(productDTO, file)) {
            Product savedProduct = productService.findByName(productDTO.getName());
            productImageService.createProductImages(savedProduct.getId(), file);
            return "redirect:/manager/products";
        }
        return "manager/products/addproduct";
    }

    @PostMapping("/update")
    public String update(Model model,
                         @RequestParam Long productID,
                         @Valid @ModelAttribute("product") ProductRequest productDTO,
                         BindingResult bindingResult,
                         @RequestParam("files") MultipartFile file,
                         @RequestParam("editComputer") String editComputer,
                         @RequestParam("editPhone") String editPhone,
                         @RequestParam("editAccessory") String editAccessory) throws IOException {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productID", productID);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("brands", brandService.findAll());
            model.addAttribute("product", productDTO);
            if (editComputer.equals("1")) {
                return "manager/products/editComputer";
            }
            if (editPhone.equals("1")) {
                return "manager/products/editPhone";
            }
            if (editAccessory.equals("1")) {
                return "manager/products/editAccessory";
            }
            return "manager/products/productlist";
        }
        if (file == null || file.isEmpty()) {
            Product product = productService.updateProduct(productID, productDTO);
            if (product == null) {
                // handle exception with alert, use js code
            }
            return "redirect:/manager/products";
        }

        Product product = productService.updateProduct(productID, productDTO, file);
        if (product == null) {
            // handle exception with alert, use js code
        }
        return "redirect:/manager/products";
    }

    @PostMapping("/images/create")
    public String insertImage(Model model,
                              @RequestParam("id") Long productid,
                              @RequestParam("files") MultipartFile[] files) throws IOException {
        Arrays.asList(files).stream().forEach(file -> {
            try {
                productImageService.createProductImages(productid, file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return "redirect:/manager/products";
    }
}
