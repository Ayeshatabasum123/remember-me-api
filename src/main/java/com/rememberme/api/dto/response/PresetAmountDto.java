package com.rememberme.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Preset donation amount option")
public class PresetAmountDto {

    @Schema(description = "Amount in integer pence", example = "1000")
    private Long amountPence;

    @Schema(description = "User-facing display string", example = "£10")
    private String displayLabel;
}
