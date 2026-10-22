package com.rememberme.api.communityfund.dto.response;

import com.rememberme.api.communityfund.enums.FundReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a fund reservation")
public class FundReservationResponseDto {

    @Schema(description = "Reservation ID", example = "1")
    private Long id;

    @Schema(description = "Associated support request ID", example = "1")
    private Long supportRequestId;

    @Schema(description = "Reserved amount in GBP", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Reservation status: RESERVED, DISBURSED, RELEASED, EXPIRED", example = "RESERVED")
    private FundReservationStatus reservationStatus;

    @Schema(description = "Reason / justification for reservation", example = "Approved burial assistance.")
    private String reason;

    @Schema(description = "ID of user who reserved funds", example = "99")
    private Long reservedByUserId;

    @Schema(description = "Email of user who reserved funds", example = "admin1@rememberme.com")
    private String reservedByEmail;

    @Schema(description = "ID of user who released reservation", example = "null")
    private Long releasedByUserId;

    @Schema(description = "Release audit reason", example = "null")
    private String releaseReason;

    @Schema(description = "Reservation timestamp", example = "2026-10-22T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Release timestamp", example = "null")
    private LocalDateTime releasedAt;
}
