package com.rememberme.api.service;

import com.rememberme.api.dto.response.GraveImportErrorDto;
import com.rememberme.api.dto.response.GraveImportResponseDto;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraveImportService {

    private final GraveRepository graveRepository;
    private final RememberMeRepository rememberMeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:Admin@123}")
    private String adminPasswordConfig;

    @Value("${app.super-admin.password:${app.admin.password:Admin@123}}")
    private String superAdminPasswordConfig;

    /**
     * Verifies the super admin password against configured credentials,
     * super admin users in the database, and the authenticated user.
     */
    public void verifySuperAdminPassword(String superAdminPassword, String authenticatedEmail) {
        if (superAdminPassword == null || superAdminPassword.trim().isEmpty()) {
            throw new ApiException("Super Admin password is required", HttpStatus.BAD_REQUEST);
        }

        // 1. Check against application properties
        if (superAdminPassword.equals(superAdminPasswordConfig) || superAdminPassword.equals(adminPasswordConfig)) {
            return;
        }

        // 2. Check against the currently authenticated user if present
        if (authenticatedEmail != null) {
            Optional<User> authUserOpt = userRepository.findByEmail(authenticatedEmail);
            if (authUserOpt.isPresent()) {
                User authUser = authUserOpt.get();
                if ((authUser.getRole() == User.Role.SUPER_ADMIN || authUser.getRole() == User.Role.ADMIN)
                        && passwordEncoder.matches(superAdminPassword, authUser.getPassword())) {
                    return;
                }
            }
        }

        // 3. Check against any user in database with SUPER_ADMIN role
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getRole() == User.Role.SUPER_ADMIN && passwordEncoder.matches(superAdminPassword, user.getPassword())) {
                return;
            }
        }

        throw new ApiException("Invalid Super Admin password", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Imports grave records from a multipart CSV file.
     */
    @Transactional
    public GraveImportResponseDto importGravesFromCsv(MultipartFile file, String superAdminPassword, String authenticatedEmail) {
        // Step 1: Verify Super Admin password
        verifySuperAdminPassword(superAdminPassword, authenticatedEmail);

        // Step 2: Validate file existence and extension
        if (file == null || file.isEmpty()) {
            throw new ApiException("CSV file is required and cannot be empty", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.trim().isEmpty()) {
            String lowerName = originalFilename.toLowerCase();
            if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".txt")) {
                throw new ApiException("Invalid file format. Please upload a valid CSV file (.csv)", HttpStatus.BAD_REQUEST);
            }
        }

        List<GraveImportErrorDto> errors = new ArrayList<>();
        List<Grave> validGraves = new ArrayList<>();
        Set<String> seenInCsv = new HashSet<>();
        Map<Long, Optional<RememberMe>> graveyardCacheById = new HashMap<>();
        Map<String, Optional<RememberMe>> graveyardCacheByName = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new ApiException("CSV file is empty or missing a valid header row", HttpStatus.BAD_REQUEST);
            }

            // Strip UTF-8 BOM if present
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndexMap = mapHeaders(headers);

            // Validate required headers
            validateHeaders(headerIndexMap);

            String line;
            int lineNumber = 1; // Row 1 was header

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip completely empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                if (isRowEmpty(columns)) {
                    continue;
                }

                processRow(lineNumber, columns, headerIndexMap, errors, validGraves, seenInCsv, graveyardCacheById, graveyardCacheByName);
            }

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            throw new ApiException("Failed to parse CSV file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Save all valid records in bulk
        if (!validGraves.isEmpty()) {
            graveRepository.saveAll(validGraves);
            log.info("Successfully imported {} grave records", validGraves.size());
        }

        int successfulRecords = validGraves.size();
        int failedRecords = errors.size();
        int totalRecords = successfulRecords + failedRecords;

        return GraveImportResponseDto.builder()
                .totalRecords(totalRecords)
                .successfulRecords(successfulRecords)
                .failedRecords(failedRecords)
                .errors(errors)
                .build();
    }

    private void processRow(int rowNumber,
                            List<String> columns,
                            Map<String, Integer> headerIndexMap,
                            List<GraveImportErrorDto> errors,
                            List<Grave> validGraves,
                            Set<String> seenInCsv,
                            Map<Long, Optional<RememberMe>> graveyardCacheById,
                            Map<String, Optional<RememberMe>> graveyardCacheByName) {

        String serialNumber = getColumnValue(columns, headerIndexMap, "serialnumber");
        String graveyardIdStr = getColumnValue(columns, headerIndexMap, "graveyardid");
        String graveyardNameStr = getColumnValue(columns, headerIndexMap, "graveyardname");
        String section = getColumnValue(columns, headerIndexMap, "section");
        String row = getColumnValue(columns, headerIndexMap, "row");
        String latitudeStr = getColumnValue(columns, headerIndexMap, "latitude");
        String longitudeStr = getColumnValue(columns, headerIndexMap, "longitude");
        String locationAccuracyStr = getColumnValue(columns, headerIndexMap, "locationaccuracy");
        String verificationStatusStr = getColumnValue(columns, headerIndexMap, "verificationstatus");

        // 1. Validate Serial Number (graveNumber)
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            errors.add(new GraveImportErrorDto(rowNumber, "Serial number (grave number) is required"));
            return;
        }
        serialNumber = serialNumber.trim();

        // 2. Validate Graveyard / Cemetery reference
        RememberMe graveyard = null;
        if (graveyardIdStr != null && !graveyardIdStr.trim().isEmpty()) {
            try {
                Long graveyardId = Long.parseLong(graveyardIdStr.trim());
                graveyard = graveyardCacheById.computeIfAbsent(graveyardId, rememberMeRepository::findById).orElse(null);
                if (graveyard == null) {
                    errors.add(new GraveImportErrorDto(rowNumber, "Graveyard not found with ID: " + graveyardId));
                    return;
                }
            } catch (NumberFormatException e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid graveyard ID format: '" + graveyardIdStr + "'. Must be a valid integer."));
                return;
            }
        } else if (graveyardNameStr != null && !graveyardNameStr.trim().isEmpty()) {
            String cleanName = graveyardNameStr.trim();
            graveyard = graveyardCacheByName.computeIfAbsent(cleanName.toLowerCase(), name -> {
                List<RememberMe> matches = rememberMeRepository.findByNameContainingIgnoreCase(cleanName);
                return matches.stream()
                        .filter(g -> g.getName().equalsIgnoreCase(cleanName))
                        .findFirst()
                        .or(() -> matches.stream().findFirst());
            }).orElse(null);

            if (graveyard == null) {
                errors.add(new GraveImportErrorDto(rowNumber, "Graveyard not found with name: '" + cleanName + "'"));
                return;
            }
        } else {
            errors.add(new GraveImportErrorDto(rowNumber, "Graveyard ID (or graveyardName) is required"));
            return;
        }

        // 3. Validate Latitude
        if (latitudeStr == null || latitudeStr.trim().isEmpty()) {
            errors.add(new GraveImportErrorDto(rowNumber, "Latitude is required"));
            return;
        }
        Double latitude;
        try {
            latitude = Double.parseDouble(latitudeStr.trim());
        } catch (NumberFormatException e) {
            errors.add(new GraveImportErrorDto(rowNumber, "Invalid latitude format: '" + latitudeStr + "'. Must be a numeric decimal value"));
            return;
        }
        if (latitude < -90.0 || latitude > 90.0) {
            errors.add(new GraveImportErrorDto(rowNumber, "Latitude must be between -90.0 and 90.0. Provided: " + latitude));
            return;
        }

        // 4. Validate Longitude
        if (longitudeStr == null || longitudeStr.trim().isEmpty()) {
            errors.add(new GraveImportErrorDto(rowNumber, "Longitude is required"));
            return;
        }
        Double longitude;
        try {
            longitude = Double.parseDouble(longitudeStr.trim());
        } catch (NumberFormatException e) {
            errors.add(new GraveImportErrorDto(rowNumber, "Invalid longitude format: '" + longitudeStr + "'. Must be a numeric decimal value"));
            return;
        }
        if (longitude < -180.0 || longitude > 180.0) {
            errors.add(new GraveImportErrorDto(rowNumber, "Longitude must be between -180.0 and 180.0. Provided: " + longitude));
            return;
        }

        // 5. Validate Location Accuracy (optional)
        Double locationAccuracy = null;
        if (locationAccuracyStr != null && !locationAccuracyStr.trim().isEmpty()) {
            try {
                locationAccuracy = Double.parseDouble(locationAccuracyStr.trim());
                if (locationAccuracy < 0) {
                    errors.add(new GraveImportErrorDto(rowNumber, "Location accuracy must be a non-negative number"));
                    return;
                }
            } catch (NumberFormatException e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid location accuracy format: '" + locationAccuracyStr + "'"));
                return;
            }
        }

        // 6. Validate Verification Status (optional, default: UNVERIFIED)
        Grave.VerificationStatus status = Grave.VerificationStatus.UNVERIFIED;
        if (verificationStatusStr != null && !verificationStatusStr.trim().isEmpty()) {
            try {
                status = Grave.VerificationStatus.valueOf(verificationStatusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid verification status: '" + verificationStatusStr +
                        "'. Allowed values: UNVERIFIED, VERIFIED, DISPUTED"));
                return;
            }
        }

        // 7. Check for duplicate within the CSV file
        String csvKey = graveyard.getId() + ":" + serialNumber.toLowerCase();
        if (!seenInCsv.add(csvKey)) {
            errors.add(new GraveImportErrorDto(rowNumber, "Duplicate grave record in CSV file: Serial number '" +
                    serialNumber + "' already listed for graveyard ID " + graveyard.getId()));
            return;
        }

        // 8. Check for duplicate in the database
        if (graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(graveyard.getId(), serialNumber)) {
            errors.add(new GraveImportErrorDto(rowNumber, "Duplicate grave record: Serial number '" +
                    serialNumber + "' already exists in graveyard ID " + graveyard.getId()));
            return;
        }

        // All validations passed -> build entity
        LocalDateTime now = LocalDateTime.now();
        Grave grave = Grave.builder()
                .rememberMe(graveyard)
                .graveNumber(serialNumber)
                .section(section != null && !section.trim().isEmpty() ? section.trim() : null)
                .row(row != null && !row.trim().isEmpty() ? row.trim() : null)
                .latitude(latitude)
                .longitude(longitude)
                .locationAccuracy(locationAccuracy)
                .verificationStatus(status)
                .createdAt(now)
                .updatedAt(now)
                .build();

        validGraves.add(grave);
    }

    private void validateHeaders(Map<String, Integer> headerIndexMap) {
        boolean hasSerialNumber = headerIndexMap.containsKey("serialnumber");
        boolean hasLatitude = headerIndexMap.containsKey("latitude");
        boolean hasLongitude = headerIndexMap.containsKey("longitude");
        boolean hasGraveyard = headerIndexMap.containsKey("graveyardid") || headerIndexMap.containsKey("graveyardname");

        if (!hasSerialNumber || !hasLatitude || !hasLongitude || !hasGraveyard) {
            List<String> missing = new ArrayList<>();
            if (!hasSerialNumber) missing.add("serialNumber (or graveNumber)");
            if (!hasLatitude) missing.add("latitude");
            if (!hasLongitude) missing.add("longitude");
            if (!hasGraveyard) missing.add("graveyardId (or graveyardName)");

            throw new ApiException("CSV is missing required column headers: " + String.join(", ", missing), HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Integer> mapHeaders(List<String> rawHeaders) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            String raw = rawHeaders.get(i);
            if (raw == null) continue;
            String normalized = raw.trim().toLowerCase().replaceAll("[_\\-\\s]+", "");

            // Alias mapping
            if (normalized.equals("serialnumber") || normalized.equals("gravenumber") || normalized.equals("serialno")
                    || normalized.equals("graveno") || normalized.equals("serial") || normalized.equals("number")) {
                map.put("serialnumber", i);
            } else if (normalized.equals("graveyardid") || normalized.equals("cemeteryid") || normalized.equals("remembermeid")
                    || normalized.equals("graveyard") || normalized.equals("cemetery")) {
                map.put("graveyardid", i);
            } else if (normalized.equals("graveyardname") || normalized.equals("cemeteryname") || normalized.equals("remembermename")) {
                map.put("graveyardname", i);
            } else if (normalized.equals("section") || normalized.equals("block") || normalized.equals("sec")) {
                map.put("section", i);
            } else if (normalized.equals("row") || normalized.equals("rownumber") || normalized.equals("rowno")) {
                map.put("row", i);
            } else if (normalized.equals("latitude") || normalized.equals("lat")) {
                map.put("latitude", i);
            } else if (normalized.equals("longitude") || normalized.equals("lng") || normalized.equals("lon") || normalized.equals("long")) {
                map.put("longitude", i);
            } else if (normalized.equals("locationaccuracy") || normalized.equals("accuracy") || normalized.equals("gpsaccuracy")) {
                map.put("locationaccuracy", i);
            } else if (normalized.equals("verificationstatus") || normalized.equals("status") || normalized.equals("verification")) {
                map.put("verificationstatus", i);
            }
        }
        return map;
    }

    private String getColumnValue(List<String> columns, Map<String, Integer> headerIndexMap, String key) {
        Integer index = headerIndexMap.get(key);
        if (index == null || index < 0 || index >= columns.size()) {
            return null;
        }
        String val = columns.get(index);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : null;
    }

    private boolean isRowEmpty(List<String> columns) {
        if (columns == null || columns.isEmpty()) {
            return true;
        }
        for (String col : columns) {
            if (col != null && !col.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a single CSV line according to RFC 4180 rules:
     * Handles quoted fields, embedded commas, escaped double quotes ("").
     */
    public List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null) {
            return tokens;
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    // Escaped quote: "" -> "
                    sb.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());

        return tokens;
    }
}
