package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.RememberMeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search people or rememberMes")
public class SearchController {

    private final DeceasedPersonRepository deceasedPersonRepository;
    private final RememberMeRepository rememberMeRepository;

    @GetMapping
    public ApiResponse<Map<String, Object>> search(@RequestParam String query) {
        List<DeceasedPerson> people = deceasedPersonRepository.findByFullNameContainingIgnoreCase(query);
        List<RememberMe> rememberMes = rememberMeRepository.findByNameContainingIgnoreCase(query);

        return ApiResponse.success(Map.of(
                "people", people,
                "rememberMes", rememberMes
        ));
    }
}
