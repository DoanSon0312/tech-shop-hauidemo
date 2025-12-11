package com.haui.tech_shop.invoice;

import com.haui.tech_shop.entities.Order;
import com.haui.tech_shop.entities.OrderDetail;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvoiceService {

    public byte[] generateInvoicePdf(Order order) throws IOException, DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Sử dụng font built-in của iText hỗ trợ Unicode
        ClassPathResource fontResource = new ClassPathResource("fonts/arial.TTF");
        BaseFont baseFont = BaseFont.createFont(
                fontResource.getPath(),
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
        );

        Font titleFont = new Font(baseFont, 20, Font.BOLD, BaseColor.BLACK);
        Font headerFont = new Font(baseFont, 12, Font.BOLD, BaseColor.BLACK);
        Font normalFont = new Font(baseFont, 11, Font.NORMAL, BaseColor.BLACK);
        Font italicFont = new Font(baseFont, 11, Font.ITALIC, BaseColor.BLACK);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        try {
            // Tiêu đề hóa đơn
            Paragraph title = new Paragraph("HÓA ĐƠN BÁN HÀNG", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Thông tin cửa hàng
            Paragraph shopInfo = new Paragraph();
            shopInfo.add(new Chunk("Tech-Shop\n", headerFont));
            shopInfo.add(new Chunk("Địa chỉ: 123 Đường ABC, Thành phố Hà Nội\n", normalFont));
            shopInfo.add(new Chunk("Điện thoại: 0123.456.789 | Email: techshop@example.com", normalFont));
            shopInfo.setAlignment(Element.ALIGN_CENTER);
            shopInfo.setSpacingAfter(20);
            document.add(shopInfo);

            // Đường kẻ ngang
            document.add(new Paragraph("================================================================", normalFont));
            document.add(new Paragraph(" ", normalFont));

            // Thông tin đơn hàng
            document.add(new Paragraph("Mã đơn hàng: #" + order.getId(), normalFont));

            // Xử lý ngày đặt hàng
            String formattedDate = "N/A";
            try {
                if (order.getCreatedAt() != null) {
                    // Thử format trực tiếp
                    formattedDate = order.getCreatedAt().format(formatter);
                } else {
                    // Nếu createdAt là null, thử lấy thời gian hiện tại
                    formattedDate = LocalDateTime.now().format(formatter);
                }
            } catch (Exception e) {
                // Nếu có lỗi, sử dụng thời gian hiện tại
                formattedDate = LocalDateTime.now().format(formatter);
            }
            document.add(new Paragraph("Ngày đặt hàng: " + formattedDate, normalFont));

            if (order.getUser() != null) {
                String customerName = "";
                if (order.getUser().getFirstName() != null) {
                    customerName += order.getUser().getFirstName();
                }
                if (order.getUser().getLastName() != null) {
                    customerName += " " + order.getUser().getLastName();
                }
                if (!customerName.trim().isEmpty()) {
                    document.add(new Paragraph("Khách hàng: " + customerName.trim(), normalFont));
                } else {
                    document.add(new Paragraph("Khách hàng: N/A", normalFont));
                }
            } else {
                document.add(new Paragraph("Khách hàng: N/A", normalFont));
            }

            if (order.getAddress() != null) {
                String fullAddress = "";
                if (order.getAddress().getDetailLocation() != null) {
                    fullAddress += order.getAddress().getDetailLocation();
                }
                if (order.getAddress().getStreet() != null) {
                    fullAddress += (fullAddress.isEmpty() ? "" : ", ") + order.getAddress().getStreet();
                }
                if (order.getAddress().getDistrict() != null) {
                    fullAddress += (fullAddress.isEmpty() ? "" : ", ") + order.getAddress().getDistrict();
                }
                if (order.getAddress().getCity() != null) {
                    fullAddress += (fullAddress.isEmpty() ? "" : ", ") + order.getAddress().getCity();
                }
                if (!fullAddress.isEmpty()) {
                    document.add(new Paragraph("Địa chỉ giao hàng: " + fullAddress, normalFont));
                } else {
                    document.add(new Paragraph("Địa chỉ giao hàng: N/A", normalFont));
                }
            } else {
                document.add(new Paragraph("Địa chỉ giao hàng: N/A", normalFont));
            }

            document.add(new Paragraph(" ", normalFont));
            document.add(new Paragraph("================================================================", normalFont));
            document.add(new Paragraph(" ", normalFont));

            // Bảng chi tiết sản phẩm
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});

            // Header của bảng
            PdfPCell headerCell1 = new PdfPCell(new Phrase("Tên sản phẩm", headerFont));
            headerCell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell1.setPadding(8);
            table.addCell(headerCell1);

            PdfPCell headerCell2 = new PdfPCell(new Phrase("Số lượng", headerFont));
            headerCell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell2.setPadding(8);
            table.addCell(headerCell2);

            PdfPCell headerCell3 = new PdfPCell(new Phrase("Đơn giá", headerFont));
            headerCell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell3.setPadding(8);
            table.addCell(headerCell3);

            PdfPCell headerCell4 = new PdfPCell(new Phrase("Thành tiền", headerFont));
            headerCell4.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell4.setPadding(8);
            table.addCell(headerCell4);

            // Dữ liệu sản phẩm
            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                for (OrderDetail detail : order.getOrderDetails()) {
                    PdfPCell cell1 = new PdfPCell(new Phrase(detail.getProduct() != null ? detail.getProduct().getName() : "N/A", normalFont));
                    cell1.setPadding(5);
                    table.addCell(cell1);

                    PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(detail.getQuantity()), normalFont));
                    cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell2.setPadding(5);
                    table.addCell(cell2);

                    PdfPCell cell3 = new PdfPCell(new Phrase(detail.getProduct() != null ? currencyFormat.format(detail.getProduct().getPrice()) : currencyFormat.format(0), normalFont));
                    cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cell3.setPadding(5);
                    table.addCell(cell3);

                    PdfPCell cell4 = new PdfPCell(new Phrase(currencyFormat.format(detail.getTotalPrice()), normalFont));
                    cell4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    cell4.setPadding(5);
                    table.addCell(cell4);
                }
            } else {
                // Thêm hàng trống nếu không có sản phẩm
                PdfPCell emptyCell = new PdfPCell(new Phrase("Không có sản phẩm", normalFont));
                emptyCell.setColspan(4);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(5);
                table.addCell(emptyCell);
            }

            document.add(table);
            document.add(new Paragraph(" ", normalFont));
            document.add(new Paragraph("================================================================", normalFont));

            // Phần tổng kết
            BigDecimal subtotal = BigDecimal.ZERO;
            if (order.getOrderDetails() != null) {
                subtotal = order.getOrderDetails().stream()
                        .map(OrderDetail::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            Paragraph subtotalParagraph = new Paragraph("Tổng phụ: " + currencyFormat.format(subtotal), normalFont);
            subtotalParagraph.setAlignment(Element.ALIGN_RIGHT);
            document.add(subtotalParagraph);

            BigDecimal discount = BigDecimal.ZERO;
            if (order.getVoucher() != null && order.getVoucher().getValue() != null) {
                discount = order.getVoucher().getValue();
                String voucherName = order.getVoucher().getName() != null ? order.getVoucher().getName() : "Voucher";
                Paragraph discountParagraph = new Paragraph("Giảm giá (" + voucherName + "): -" + currencyFormat.format(discount), italicFont);
                discountParagraph.setAlignment(Element.ALIGN_RIGHT);
                document.add(discountParagraph);
            }

            // Tổng thanh toán
            BigDecimal finalPrice = subtotal.subtract(discount);
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }

            Paragraph finalPriceParagraph = new Paragraph("TỔNG THANH TOÁN: " + currencyFormat.format(finalPrice), headerFont);
            finalPriceParagraph.setAlignment(Element.ALIGN_RIGHT);
            finalPriceParagraph.setSpacingBefore(5);
            document.add(finalPriceParagraph);

            document.add(new Paragraph(" ", normalFont));
            document.add(new Paragraph("================================================================", normalFont));

            String paymentMethod = "N/A";
            if (order.getPayment() != null && order.getPayment().getName() != null) {
                paymentMethod = order.getPayment().getName();
            }
            document.add(new Paragraph("Phương thức thanh toán: " + paymentMethod, normalFont));

            document.add(new Paragraph(" ", normalFont));
            Paragraph thankYou = new Paragraph("Cảm ơn quý khách đã mua sắm tại cửa hàng chúng tôi!", italicFont);
            thankYou.setAlignment(Element.ALIGN_CENTER);
            thankYou.setSpacingBefore(20);
            document.add(thankYou);

        } catch (Exception e) {
            e.printStackTrace();
            throw new DocumentException("Error creating PDF: " + e.getMessage());
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }
}