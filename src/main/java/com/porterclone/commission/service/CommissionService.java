package com.porterclone.commission.service;

import com.porterclone.commission.entity.CommissionConfig;
import com.porterclone.commission.entity.CommissionType;
import com.porterclone.commission.entity.LedgerEntryType;
import com.porterclone.commission.entity.RiderEarningsLedger;
import com.porterclone.commission.repository.CommissionConfigRepository;
import com.porterclone.commission.repository.RiderEarningsLedgerRepository;
import com.porterclone.rider.entity.Rider;
import com.porterclone.rider.repository.RiderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Applies platform commission to every completed trip and posts the rider's net
 * earning to their ledger + wallet balance. Commission rate is resolved AT THE TIME
 * OF THE TRIP (effective_from/effective_to), so changing rates later never rewrites history.
 */
@Service
public class CommissionService {

    private static final int SCALE = 2;
    private static final BigDecimal DEFAULT_COMMISSION_PERCENTAGE = new BigDecimal("20.00");

    private final CommissionConfigRepository commissionConfigRepository;
    private final RiderEarningsLedgerRepository ledgerRepository;
    private final RiderRepository riderRepository;

    public CommissionService(CommissionConfigRepository commissionConfigRepository,
                              RiderEarningsLedgerRepository ledgerRepository,
                              RiderRepository riderRepository) {
        this.commissionConfigRepository = commissionConfigRepository;
        this.ledgerRepository = ledgerRepository;
        this.riderRepository = riderRepository;
    }

    public record CommissionResult(BigDecimal commissionAmount, BigDecimal commissionPercentage, BigDecimal riderNetEarning) {}

    /** Pure calculation — does not touch the ledger. Used to preview earnings before completion too. */
    public CommissionResult computeCommission(Long vehicleTypeId, BigDecimal grossFare) {
        LocalDateTime now = LocalDateTime.now();

        CommissionConfig config = resolveConfig(vehicleTypeId, now);

        BigDecimal commissionAmount;
        BigDecimal effectivePercentage;

        if (config == null) {
            effectivePercentage = DEFAULT_COMMISSION_PERCENTAGE;
            commissionAmount = grossFare.multiply(effectivePercentage).divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        } else if (config.getCommissionType() == CommissionType.PERCENTAGE) {
            effectivePercentage = config.getValue();
            commissionAmount = grossFare.multiply(effectivePercentage).divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        } else { // FIXED
            commissionAmount = config.getValue().min(grossFare); // never take more than the fare itself
            effectivePercentage = grossFare.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : commissionAmount.multiply(BigDecimal.valueOf(100)).divide(grossFare, SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal riderNetEarning = grossFare.subtract(commissionAmount).setScale(SCALE, RoundingMode.HALF_UP);
        return new CommissionResult(commissionAmount.setScale(SCALE, RoundingMode.HALF_UP), effectivePercentage, riderNetEarning);
    }

    /** Posts the rider's net earning to the append-only ledger and updates their running wallet balance. */
    @Transactional
    public void postTripEarning(Long riderId, Long tripId, BigDecimal riderNetEarning) {
        Rider rider = riderRepository.findById(riderId).orElseThrow();
        BigDecimal newBalance = rider.getWalletBalance().add(riderNetEarning);
        rider.setWalletBalance(newBalance);
        riderRepository.save(rider);

        RiderEarningsLedger entry = new RiderEarningsLedger();
        entry.setRiderId(riderId);
        entry.setTripId(tripId);
        entry.setAmount(riderNetEarning);
        entry.setType(LedgerEntryType.TRIP_EARNING);
        entry.setBalanceAfter(newBalance);
        ledgerRepository.save(entry);
    }

    private CommissionConfig resolveConfig(Long vehicleTypeId, LocalDateTime at) {
        List<CommissionConfig> specific = commissionConfigRepository.findApplicableForVehicleType(vehicleTypeId, at);
        if (!specific.isEmpty()) return specific.get(0);

        List<CommissionConfig> global = commissionConfigRepository.findApplicableGlobal(at);
        return global.isEmpty() ? null : global.get(0);
    }
}
