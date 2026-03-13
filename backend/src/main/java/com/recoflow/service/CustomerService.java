package com.recoflow.service;

import com.recoflow.config.TenantContext;
import com.recoflow.dto.request.CreateCustomerRequest;
import com.recoflow.entity.Customer;
import com.recoflow.entity.Tenant;
import com.recoflow.repository.CustomerRepository;
import com.recoflow.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    public Page<Customer> list(Pageable pageable) {
        return customerRepository.findAllByTenantTenantId(TenantContext.getTenantId(), pageable);
    }

    public List<Customer> search(String q) {
        return customerRepository.searchByTenant(TenantContext.getTenantId(), q);
    }

    public Customer getById(UUID id) {
        return customerRepository.findByCustomerIdAndTenantTenantId(id, TenantContext.getTenantId())
            .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @Transactional
    public Customer create(CreateCustomerRequest req) {
        Tenant tenant = tenantRepository.getReferenceById(TenantContext.getTenantId());
        return customerRepository.save(Customer.builder()
            .tenant(tenant)
            .name(req.getName())
            .phone(req.getPhone())
            .email(req.getEmail())
            .gstin(req.getGstin())
            .vpaHint(req.getVpaHint())
            .notes(req.getNotes())
            .build());
    }

    @Transactional
    public Customer update(UUID id, CreateCustomerRequest req) {
        Customer c = getById(id);
        c.setName(req.getName());
        c.setPhone(req.getPhone());
        c.setEmail(req.getEmail());
        c.setGstin(req.getGstin());
        c.setVpaHint(req.getVpaHint());
        c.setNotes(req.getNotes());
        return customerRepository.save(c);
    }

    @Transactional
    public void delete(UUID id) {
        Customer c = getById(id);
        customerRepository.delete(c);
    }
}
