package com.haui.tech_shop.services.Impl;

import com.haui.tech_shop.entities.Customers;
import com.haui.tech_shop.enums.CustomerStatus;
import com.haui.tech_shop.repositories.CustomersRepository;
import com.haui.tech_shop.services.interfaces.ICustomersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomersServiceImpl implements ICustomersService {
    @Autowired
    private CustomersRepository customersRepository;

    @Override
    public void saveContact(Customers customers) {
        customersRepository.save(customers);
    }

    @Override
    public List<Customers> findAll() {
        return customersRepository.findAll();
    }

    @Override
    public Optional<Customers> findById(Long id) {
        return customersRepository.findById(id);
    }

    @Override
    public boolean deleteById(Long id) {
        if (customersRepository.existsById(id)) {
            customersRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public void setPending(Long id) {
        Customers customer = findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setStatus(CustomerStatus.PENDING);
        customersRepository.save(customer);
    }

    @Override
    public void setResponded(Long id) {
        Customers customer = findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setStatus(CustomerStatus.RESPONDED);
        customersRepository.save(customer);
    }

    @Override
    public void setCancelled(Long id) {
        Customers customer = findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setStatus(CustomerStatus.CANCELLED);
        customersRepository.save(customer);
    }
}
