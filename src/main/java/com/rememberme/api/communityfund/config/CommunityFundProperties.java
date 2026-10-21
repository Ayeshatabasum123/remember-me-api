package com.rememberme.api.communityfund.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "community-fund")
@Data
public class CommunityFundProperties {

    private String currency = "GBP";
    private String currencySymbol = "£";
    private BigDecimal minDonationAmount = BigDecimal.valueOf(1.00);
    private BigDecimal maxDonationAmount = BigDecimal.valueOf(50000.00);
    private List<BigDecimal> presetDonationAmounts = List.of(
            BigDecimal.valueOf(5.00),
            BigDecimal.valueOf(10.00),
            BigDecimal.valueOf(50.00)
    );
    private int maxDocumentsPerRequest = 10;
    private long maxDocumentSizeBytes = 10 * 1024 * 1024; // 10 MB
    private List<String> allowedMimeTypes = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
}
