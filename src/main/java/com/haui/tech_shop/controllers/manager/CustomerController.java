package com.haui.tech_shop.controllers.manager;

import com.haui.tech_shop.entities.Customers;
import com.haui.tech_shop.enums.CustomerStatus;
import com.haui.tech_shop.services.interfaces.ICustomersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller("controllerOfManagerCustomer")
@RequestMapping("/manager/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final ICustomersService customersService;

    @GetMapping("") // localhost:8080/manager/customers
    public String index(Model model) {
        model.addAttribute("customers", customersService.findAll()
                .stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .toList());
        model.addAttribute("customerStatus", CustomerStatus.values());
        return "manager/customers/customerList";
    }

    @GetMapping("/detail")
    public String customerDetail(Model model, @RequestParam Long id) {
        Optional<Customers> customer = customersService.findById(id);
        model.addAttribute("customerStatus", CustomerStatus.values());
        model.addAttribute("customer", customer);
        return "manager/customers/customerDetail"; // Assuming a detail template exists
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable(value = "id") Long id) {
        Map<String, String> response = new HashMap<>();
        if (customersService.deleteById(id)) {
            response.put("status", "success");
            response.put("message", "Customer deleted successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to delete customer.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/pending")
    public String pending(@RequestParam Long id) {
        customersService.setPending(id);
        return "redirect:/manager/customers";
    }

    @PostMapping("/responded")
    public String responded(@RequestParam Long id) {
        customersService.setResponded(id);
        return "redirect:/manager/customers";
    }

    @PostMapping("/cancelled")
    public String cancelled(@RequestParam Long id) {
        customersService.setCancelled(id);
        return "redirect:/manager/customers";
    }
}
