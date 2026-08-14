package com.rememberme.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RememberMeRequest {

    @NotBlank
    private String name;

    private String address;
    private String city;
    private String state;
    private String country;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
