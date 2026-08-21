package com.porterclone.customer.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.customer.entity.Customer;
import com.porterclone.customer.repository.CustomerRepository;
import com.porterclone.delivery.entity.DeliveryRequest;
import com.porterclone.delivery.repository.DeliveryRequestRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final DeliveryRequestRepository deliveryRequestRepository;

    public CustomerController(CustomerRepository customerRepository,
                               DeliveryRequestRepository deliveryRequestRepository) {
        this.customerRepository = customerRepository;
        this.deliveryRequestRepository = deliveryRequestRepository;
    }

    @GetMapping("/{customerId}")
    public ApiResponse<Customer> getProfile(@PathVariable Long customerId) {
        return ApiResponse.ok(customerRepository.findById(customerId).orElseThrow());
    }

    @GetMapping("/{customerId}/trips")
    public ApiResponse<List<DeliveryRequest>> tripHistory(@PathVariable Long customerId) {
        return ApiResponse.ok(deliveryRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }
}
