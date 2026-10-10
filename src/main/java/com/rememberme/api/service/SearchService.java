package com.rememberme.api.service;

import com.rememberme.api.dto.response.ApiResponse;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface SearchService {
    ApiResponse<Map<String, Object>> search(String query, String city, String country, String type, Authentication authentication);
}
