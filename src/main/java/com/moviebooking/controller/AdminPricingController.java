package com.moviebooking.controller;

import com.moviebooking.domain.entity.*;
import com.moviebooking.dto.request.*;
import com.moviebooking.dto.response.ApiResponse;
import com.moviebooking.service.*;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Pricing & Discounts")
public class AdminPricingController {

    private final PricingService pricingService;

    @PostMapping("/shows/{showId}/pricing")
    @Operation(summary = "Set pricing tiers for a show")
    public ResponseEntity<ApiResponse<List<PricingTier>>> setPricing(@PathVariable Long showId,
                                                                     @Valid @RequestBody SetPricingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.setPricing(showId, request)));
    }

    @PostMapping("/discount-codes")
    @Operation(summary = "Create a discount code")
    public ResponseEntity<ApiResponse<DiscountCode>> createDiscountCode(
            @Valid @RequestBody CreateDiscountCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(pricingService.createDiscountCode(request)));
    }

    @GetMapping("/discount-codes")
    @Operation(summary = "List all discount codes")
    public ResponseEntity<ApiResponse<List<DiscountCode>>> getDiscountCodes() {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.getAllDiscountCodes()));
    }
}
