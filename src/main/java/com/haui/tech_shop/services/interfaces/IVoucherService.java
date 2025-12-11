package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.requests.VoucherRequest;
import com.haui.tech_shop.entities.Voucher;

import java.util.List;
import java.util.Optional;

public interface IVoucherService {

    void decreaseQuantity(Long id);

    Voucher findValidVoucher(String name);

    List<Voucher> findValidVoucher();

    List<Voucher> findByQuantityGreaterThan(int quantityIsGreaterThan);

    List<Voucher> findAll();

    Voucher save(VoucherRequest voucherDTO);

    boolean deleteById(Long id);

    Optional<Voucher> findById(Long id);

    Optional<Voucher> findByName(String name);

    int getAvailableVoucherCount();
}
