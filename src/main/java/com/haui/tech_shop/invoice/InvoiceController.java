package com.haui.tech_shop.invoice;

import com.haui.tech_shop.entities.Order;
import com.haui.tech_shop.services.interfaces.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.itextpdf.text.DocumentException;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/user/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final IOrderService orderService;

    @GetMapping("/{orderId}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId) {
        try {
            log.info("Attempting to download invoice for order ID: {}", orderId);

            Optional<Order> orderOptional = orderService.findOrderByIdWithDetails(orderId);

            if (orderOptional.isEmpty()) {
                log.warn("Order not found with ID: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Order not found".getBytes());
            }

            Order order = orderOptional.get();
            log.info("Order found, generating PDF for order: {}", orderId);

            byte[] pdfBytes = invoiceService.generateInvoicePdf(order);

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("Generated PDF is empty for order: {}", orderId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to generate PDF".getBytes());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "hoa-don-" + orderId + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setPragma("public");
            headers.setExpires(0);
            headers.setContentLength(pdfBytes.length);

            log.info("PDF generated successfully for order: {}, size: {} bytes", orderId, pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("IOException while generating PDF for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Loi doc file: " + e.getMessage()).getBytes());
        } catch (DocumentException e) {
            log.error("DocumentException while generating PDF for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Loi tao PDF: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Unexpected error while generating PDF for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Loi he thong: " + e.getMessage()).getBytes());
        }
    }
}