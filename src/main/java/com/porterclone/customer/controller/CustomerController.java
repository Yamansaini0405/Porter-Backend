package com.porterclone.customer.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.customer.entity.Customer;
import com.porterclone.customer.repository.CustomerRepository;
import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.delivery.repository.DeliveryRequestRepository;
import com.porterclone.user.entity.Role;
import com.porterclone.user.entity.User;
import com.porterclone.user.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final DeliveryRequestRepository deliveryRequestRepository;
    private final UserRepository userRepository;

    public CustomerController(CustomerRepository customerRepository,
                              DeliveryRequestRepository deliveryRequestRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.deliveryRequestRepository = deliveryRequestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{customerId}")
    public ApiResponse<Customer> getProfile(@PathVariable Long customerId) {
        return ApiResponse.ok(customerRepository.findById(customerId).orElseThrow());
    }

    @GetMapping("/all")
    public ApiResponse<List<User>> getAllCustomers() {
        return ApiResponse.ok(userRepository.findByRole(Role.CUSTOMER).orElseThrow());
    }

    @GetMapping("/{customerId}/trips")
    public ApiResponse<List<DeliveryRequest>> tripHistory(@PathVariable Long customerId) {
        return ApiResponse.ok(deliveryRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }
}
