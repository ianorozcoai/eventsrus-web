package com.web.eventsrus.backend;

import java.math.BigDecimal;
import java.util.List;

public record BackendReferralOverview(
        String referralCode,
        String referralLink,
        BigDecimal totalPendingCommission,
        BigDecimal totalPaidCommission,
        List<BackendReferralSummary> referrals) {}
