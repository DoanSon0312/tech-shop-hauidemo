package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.entities.Customers;

import java.util.List;
import java.util.Optional;

public interface ICustomersService {
    void saveContact(Customers customers);
    List<Customers> findAll();
    Optional<Customers> findById(Long id);
    boolean deleteById(Long id);
    void setPending(Long id);
    void setResponded(Long id);
    void setCancelled(Long id);
}
