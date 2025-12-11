package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.requests.ProductRequest;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IOCRService {

    String extractTextFromImage(MultipartFile file) throws IOException, TesseractException;

    ProductRequest parseProductFromOCR(String ocrText);

    void fillDefaultValues(ProductRequest productRequest);

    ProductRequest processImageToProduct(MultipartFile file) throws IOException, TesseractException;
}