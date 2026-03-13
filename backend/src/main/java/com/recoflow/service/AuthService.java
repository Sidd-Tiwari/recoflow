package com.recoflow.service;

import com.recoflow.config.TenantContext;
import com.recoflow.dto.request.LoginRequest;
import com.recoflow.dto.request.RegisterRequest;
import com.recoflow.dto.response.AuthResponse;
import com.recoflow.entity.Role;
import com.recoflow.entity.Tenant;
import com.recoflow.entity.User;
import com.recoflow.repository.RoleRepository;
import com.recoflow.repository.TenantRepository;
import com.recoflow.repository.UserRepository;
import com.recoflow.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        Tenant tenant = tenantRepository.save(Tenant.builder()
            .name(req.getOrganizationName())
            .plan("FREE")
            .build());

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
            .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        User user = userRepository.save(User.builder()
            .tenant(tenant)
            .name(req.getName())
            .email(req.getEmail())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .roles(Set.of(adminRole))
            .build());

        TenantContext.setTenantId(tenant.getTenantId());
        auditService.log(user, "TENANT_REGISTERED", "TENANT", tenant.getTenantId().toString(), null, null);

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String token = jwtUtil.generateToken(user.getUserId(), tenant.getTenantId(), user.getEmail(), roles);
        return new AuthResponse(token, user.getUserId(), tenant.getTenantId(), user.getName(), roles);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailAndTenantTenantId(req.getEmail(), req.getTenantId())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(user.getUserId().toString(), req.getPassword())
        );

        List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        String token = jwtUtil.generateToken(user.getUserId(), req.getTenantId(), user.getEmail(), roles);
        return new AuthResponse(token, user.getUserId(), req.getTenantId(), user.getName(), roles);
    }
}
