package com.moviebooking.controller;

import com.moviebooking.domain.entity.RefundPolicy;
import com.moviebooking.dto.request.CreateRefundPolicyRequest;
import com.moviebooking.dto.response.ApiResponse;
import com.moviebooking.service.RefundPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refund-policies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Refund Policies")
public class AdminRefundPolicyController {

    private final RefundPolicyService refundPolicyService;

    @PostMapping
    @Operation(summary = "Create refund policy brackets")
    public ResponseEntity<ApiResponse<List<RefundPolicy>>> createPolicy(
            @Valid @RequestBody CreateRefundPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(refundPolicyService.createPolicy(request)));
    }

    @GetMapping
    @Operation(summary = "List all refund policies")
    public ResponseEntity<ApiResponse<List<RefundPolicy>>> getPolicies() {
        return ResponseEntity.ok(ApiResponse.ok(refundPolicyService.getAllPolicies()));
    }
}
