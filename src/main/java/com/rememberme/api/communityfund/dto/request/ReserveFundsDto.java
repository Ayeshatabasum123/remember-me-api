package com.rememberme.api.communityfund.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Fund reservation payload for approved burial assistance")
public class ReserveFundsDto {

    @NotNull(message = "Reservation amount is required")
    @DecimalMin(value = "0.01", message = "Reservation amount must be greater than zero")
    @Schema(description = "Amount of communal funds to reserve in GBP", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Audit justification for fund reservation", example = "Approved burial assistance.")
    private String reason;
}
