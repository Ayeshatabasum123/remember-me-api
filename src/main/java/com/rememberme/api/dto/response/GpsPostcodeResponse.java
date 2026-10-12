package com.rememberme.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GpsPostcodeResponse {
    private String postcode;
    private String normalizedPostcode;
    private String country;
    private String region;
    private Double latitude;
    private Double longitude;
}
