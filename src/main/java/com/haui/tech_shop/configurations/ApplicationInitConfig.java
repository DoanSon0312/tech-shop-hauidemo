package com.haui.tech_shop.configurations;

import com.haui.tech_shop.dtos.requests.RoleRequest;
import com.haui.tech_shop.entities.Address;
import com.haui.tech_shop.entities.Role;
import com.haui.tech_shop.entities.User;
import com.haui.tech_shop.repositories.UserRepository;
import com.haui.tech_shop.services.Impl.AddressServiceImpl;
import com.haui.tech_shop.services.Impl.RoleServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ApplicationInitConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final RoleServiceImpl roleService;
    private final AddressServiceImpl addressService;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            try {
                // Tạo các role mặc định
                String[] roleNames = {"user", "admin", "manager", "shipper"};
                for (String roleName : roleNames) {
                    if (roleService.getRoleByName(roleName) == null) {
                        RoleRequest role = RoleRequest.builder().name(roleName).build();
                        roleService.createRole(role);
                        log.info("Default role created: " + roleName);
                    }
                }
    
                // Tạo admin
                if (userRepository.findByUsername("admin").isEmpty()) {
                    Role roleAdmin = roleService.getRoleByName("admin");
                    User user = User.builder()
                            .username("admin")
                            .email("admin@gmail.com")
                            .password(passwordEncoder().encode("admin"))
                            .firstName("admin")
                            .lastName("tech system")
                            .isActive(true)
                            .role(roleAdmin)
                            .build();
                    User savedUser = userRepository.save(user); // ← save user TRƯỚC
    
                    Address addressAdmin = Address.builder()
                            .detailLocation("Đường 1 ngõ 1 ngách 1")
                            .district("Quận 12")
                            .city("TPHCM")
                            .user(savedUser) // ← gán user vào address
                            .build();
                    addressService.save(addressAdmin);
                    log.info("Default admin user created");
                }
    
                // Tạo 10 user test
                for (int i = 0; i < 10; i++) {
                    if (userRepository.findByUsername("user" + i).isEmpty()) {
                        Role roleUser = roleService.getRoleByName("user");
                        User user = User.builder()
                                .username("user" + i)
                                .email("user" + i + "@gmail.com")
                                .password(passwordEncoder().encode("user"))
                                .firstName("system")
                                .lastName("user" + i)
                                .isActive(true)
                                .role(roleUser)
                                .build();
                        User savedUser = userRepository.save(user); // ← save user TRƯỚC
    
                        Address address = Address.builder()
                                .detailLocation("Đường " + i + " ngõ " + i + " ngách " + i)
                                .district("Quận 12")
                                .city("TPHCM")
                                .user(savedUser) // ← gán user vào address
                                .build();
                        addressService.save(address);
                        log.info("Default user created: {}", user.getUsername());
                    }
                }
    
            } catch (Exception e) {
                log.error("ApplicationInitConfig failed: ", e);
            }
        };
    }
}
