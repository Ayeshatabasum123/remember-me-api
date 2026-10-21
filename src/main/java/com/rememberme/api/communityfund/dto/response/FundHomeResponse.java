package com.rememberme.api.communityfund.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Community Burial Fund public home and informational profile")
public class FundHomeResponse {

    @Schema(description = "Public official name of the fund", example = "Remember Me Community Burial Fund")
    private String fundName;

    @Schema(description = "Introductory summary statement", example = "Providing compassionate, communal burial assistance to families facing financial hardship.")
    private String introduction;

    @Schema(description = "Formal purpose statement of the fund", example = "To relieve burial and funeral poverty by providing direct, discreet financial assistance to verified burial service providers on behalf of bereaved families in need.")
    private String purpose;

    @Schema(description = "Detailed description of the fund and community initiative")
    private String description;

    @Schema(description = "Information on how contributions are received and allocated")
    private String donationInformation;

    @Schema(description = "Applicant and donor privacy policy guarantees")
    private String privacyInformation;

    @Schema(description = "Legal and operational funding disclaimers")
    private String fundingDisclaimer;

    @Schema(description = "Timestamp when the fund details were last reviewed/updated")
    private LocalDateTime updatedAt;
}
