package com.rememberme.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentMemorialsResponseDto {
    private List<MemorialSummaryDto> memorials;
    private PaginationDto pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationDto {
        private int currentPage;
        private int pageSize;
        private long totalRecords;
        private int totalPages;
        private boolean hasNext;
    }
}
