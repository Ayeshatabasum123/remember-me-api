package com.rememberme.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Admin request body to broadcast VIP / National funeral notifications")
public class VipBroadcastRequest {

    private Long funeralId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    private String targetCountry;
    private String targetRegion;
}
