package com.porterclone.dispute.controller;

import com.porterclone.common.ApiResponse;
import com.porterclone.dispute.dto.RaiseDisputeRequest;
import com.porterclone.dispute.entity.Dispute;
import com.porterclone.dispute.entity.DisputeStatus;
import com.porterclone.dispute.repository.DisputeRepository;
import com.porterclone.dispute.service.DisputeService;
import com.porterclone.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DisputeController {

    private final DisputeService disputeService;
    private final DisputeRepository disputeRepository;

    public DisputeController(DisputeService disputeService, DisputeRepository disputeRepository) {
        this.disputeService = disputeService;
        this.disputeRepository = disputeRepository;
    }

    @PostMapping("/disputes")
    public ApiResponse<Dispute> raise(@Valid @RequestBody RaiseDisputeRequest request) {
        return ApiResponse.ok(disputeService.raise(request));
    }

    @GetMapping("/admin/disputes")
    public ApiResponse<List<Dispute>> openDisputes() {
        return ApiResponse.ok(disputeRepository.findByStatus(DisputeStatus.OPEN));
    }

    @PostMapping("/admin/disputes/{disputeId}/resolve")
    public ApiResponse<Dispute> resolve(@PathVariable Long disputeId,
                                         @RequestParam String notes,
                                         @RequestParam boolean accepted,
                                         @AuthenticationPrincipal UserPrincipal admin) {
        return ApiResponse.ok(disputeService.resolve(disputeId, admin.userId(), notes, accepted));
    }
}
