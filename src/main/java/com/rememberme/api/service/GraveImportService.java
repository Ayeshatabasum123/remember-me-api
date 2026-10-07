package com.rememberme.api.service;

import com.rememberme.api.dto.response.GraveImportErrorDto;
import com.rememberme.api.dto.response.GraveImportResponseDto;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraveImportService {

    private final GraveRepository graveRepository;
    private final RememberMeRepository rememberMeRepository;
    private final DeceasedPersonRepository deceasedPersonRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:Admin@123}")
    private String adminPasswordConfig;

    @Value("${app.super-admin.password:${app.admin.password:Admin@123}}")
    private String superAdminPasswordConfig;

    @Data
    @Builder
    private static class ParsedGraveRow {
        private int rowNumber;
        private String serialNumber;
        private String cemeteryName;
        private Long cemeteryId;
        private String graveNumber;
        private Double latitude;
        private Double longitude;
        private String section;
        private String row;
        private Double locationAccuracy;
        private Grave.VerificationStatus verificationStatus;
        private String deceasedName;
        private LocalDate dateOfBirth;
        private LocalDate dateOfDeath;
        private DeceasedPerson.Gender gender;
        private String photoUrl;
    }

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
     * Imports grave records and associated cemetery / deceased person data from a multipart CSV file.
     */
    @Transactional
    public GraveImportResponseDto importGravesFromCsv(MultipartFile file, String superAdminPassword, String authenticatedEmail) {
        // Step 1: Verify Super Admin password
        verifySuperAdminPassword(superAdminPassword, authenticatedEmail);

        // Fetch authenticated admin user entity if available
        User adminUser = authenticatedEmail != null ? userRepository.findByEmail(authenticatedEmail).orElse(null) : null;

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
        List<ParsedGraveRow> validRows = new ArrayList<>();
        Set<String> seenInCsv = new HashSet<>();

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

                parseAndValidateRow(lineNumber, columns, headerIndexMap, errors, validRows, seenInCsv);
            }

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            throw new ApiException("Failed to parse CSV file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Step 3: Process valid rows -> resolve or auto-create cemeteries -> save graves & deceased persons
        int successfulRecords = 0;
        Map<String, RememberMe> resolvedGraveyardsByName = new HashMap<>();
        Map<Long, RememberMe> resolvedGraveyardsById = new HashMap<>();

        for (ParsedGraveRow row : validRows) {
            RememberMe graveyard = resolveOrCreateGraveyard(row, resolvedGraveyardsByName, resolvedGraveyardsById, adminUser, errors);
            if (graveyard == null) {
                // Error already recorded in errors list
                continue;
            }

            // Check duplicate grave within this specific graveyard in DB
            if (graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(graveyard.getId(), row.getGraveNumber())) {
                errors.add(new GraveImportErrorDto(row.getRowNumber(),
                        "Duplicate grave record: Grave number '" + row.getGraveNumber() + "' already exists in cemetery '" +
                                graveyard.getName() + "' (ID: " + graveyard.getId() + ")"));
                continue;
            }

            // Create and persist Grave
            LocalDateTime now = LocalDateTime.now();
            Grave grave = Grave.builder()
                    .rememberMe(graveyard)
                    .graveNumber(row.getGraveNumber())
                    .section(row.getSection())
                    .row(row.getRow())
                    .latitude(row.getLatitude())
                    .longitude(row.getLongitude())
                    .locationAccuracy(row.getLocationAccuracy())
                    .verificationStatus(row.getVerificationStatus() != null ? row.getVerificationStatus() : Grave.VerificationStatus.UNVERIFIED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            grave = graveRepository.save(grave);

            // If deceased person information is provided, create and persist DeceasedPerson record
            if (row.getDeceasedName() != null && !row.getDeceasedName().trim().isEmpty()) {
                DeceasedPerson deceasedPerson = DeceasedPerson.builder()
                        .fullName(row.getDeceasedName().trim())
                        .dateOfBirth(row.getDateOfBirth())
                        .dateOfDeath(row.getDateOfDeath())
                        .gender(row.getGender())
                        .photoUrl(row.getPhotoUrl())
                        .grave(grave)
                        .addedBy(adminUser)
                        .duplicateChecked(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                deceasedPersonRepository.save(deceasedPerson);
            }

            successfulRecords++;
        }

        int failedRecords = errors.size();
        int totalRecords = successfulRecords + failedRecords;

        log.info("Grave CSV import finished: total={}, successful={}, failed={}", totalRecords, successfulRecords, failedRecords);

        return GraveImportResponseDto.builder()
                .totalRecords(totalRecords)
                .successfulRecords(successfulRecords)
                .failedRecords(failedRecords)
                .errors(errors)
                .build();
    }

    private RememberMe resolveOrCreateGraveyard(ParsedGraveRow row,
                                                Map<String, RememberMe> resolvedByName,
                                                Map<Long, RememberMe> resolvedById,
                                                User adminUser,
                                                List<GraveImportErrorDto> errors) {
        // 1. By ID if specified
        if (row.getCemeteryId() != null) {
            RememberMe existingById = resolvedById.get(row.getCemeteryId());
            if (existingById != null) {
                return existingById;
            }
            Optional<RememberMe> dbById = rememberMeRepository.findById(row.getCemeteryId());
            if (dbById.isPresent()) {
                RememberMe found = dbById.get();
                resolvedById.put(found.getId(), found);
                if (found.getName() != null) {
                    resolvedByName.put(found.getName().trim().toLowerCase(), found);
                }
                return found;
            } else {
                errors.add(new GraveImportErrorDto(row.getRowNumber(), "Graveyard not found with ID: " + row.getCemeteryId()));
                return null;
            }
        }

        // 2. By Name
        if (row.getCemeteryName() != null && !row.getCemeteryName().trim().isEmpty()) {
            String cleanName = row.getCemeteryName().trim();
            String nameKey = cleanName.toLowerCase();

            RememberMe existingByName = resolvedByName.get(nameKey);
            if (existingByName != null) {
                return existingByName;
            }

            Optional<RememberMe> dbByName = rememberMeRepository.findFirstByNameIgnoreCase(cleanName);
            if (dbByName.isPresent()) {
                RememberMe found = dbByName.get();
                resolvedByName.put(nameKey, found);
                resolvedById.put(found.getId(), found);
                return found;
            }

            // Cemetery does not exist -> Automatically create new cemetery/graveyard
            LocalDateTime now = LocalDateTime.now();
            RememberMe newGraveyard = RememberMe.builder()
                    .name(cleanName)
                    .latitude(row.getLatitude())
                    .longitude(row.getLongitude())
                    .status(RememberMe.ApprovalStatus.APPROVED)
                    .managedBy(adminUser)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            newGraveyard = rememberMeRepository.save(newGraveyard);
            log.info("Auto-created new cemetery/graveyard '{}' (ID: {}) during CSV import", newGraveyard.getName(), newGraveyard.getId());

            resolvedByName.put(nameKey, newGraveyard);
            resolvedById.put(newGraveyard.getId(), newGraveyard);
            return newGraveyard;
        }

        errors.add(new GraveImportErrorDto(row.getRowNumber(), "Cemetery/Graveyard name (or graveyardId) is required"));
        return null;
    }

    private void parseAndValidateRow(int rowNumber,
                                     List<String> columns,
                                     Map<String, Integer> headerIndexMap,
                                     List<GraveImportErrorDto> errors,
                                     List<ParsedGraveRow> validRows,
                                     Set<String> seenInCsv) {

        String serialNumber = getColumnValue(columns, headerIndexMap, "serialnumber");
        String graveNumberStr = getColumnValue(columns, headerIndexMap, "gravenumber");
        String cemeteryName = getColumnValue(columns, headerIndexMap, "cemeteryname");
        String cemeteryIdStr = getColumnValue(columns, headerIndexMap, "cemeteryid");
        String section = getColumnValue(columns, headerIndexMap, "section");
        String row = getColumnValue(columns, headerIndexMap, "row");
        String latitudeStr = getColumnValue(columns, headerIndexMap, "latitude");
        String longitudeStr = getColumnValue(columns, headerIndexMap, "longitude");
        String locationAccuracyStr = getColumnValue(columns, headerIndexMap, "locationaccuracy");
        String verificationStatusStr = getColumnValue(columns, headerIndexMap, "verificationstatus");
        String deceasedName = getColumnValue(columns, headerIndexMap, "deceasedname");
        String dobStr = getColumnValue(columns, headerIndexMap, "dateofbirth");
        String dodStr = getColumnValue(columns, headerIndexMap, "dateofdeath");
        String genderStr = getColumnValue(columns, headerIndexMap, "gender");
        String photoUrl = getColumnValue(columns, headerIndexMap, "photourl");

        // 1. Resolve effective grave number (uses graveNumber if provided, fallback to serialNumber)
        String effectiveGraveNumber = null;
        if (graveNumberStr != null && !graveNumberStr.trim().isEmpty()) {
            effectiveGraveNumber = graveNumberStr.trim();
        } else if (serialNumber != null && !serialNumber.trim().isEmpty()) {
            effectiveGraveNumber = serialNumber.trim();
        } else {
            errors.add(new GraveImportErrorDto(rowNumber, "Grave number (or serial number) is required"));
            return;
        }

        // 2. Validate Cemetery reference (name or ID)
        Long cemeteryId = null;
        if (cemeteryIdStr != null && !cemeteryIdStr.trim().isEmpty()) {
            try {
                cemeteryId = Long.parseLong(cemeteryIdStr.trim());
            } catch (NumberFormatException e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid graveyard ID format: '" + cemeteryIdStr + "'. Must be a valid integer."));
                return;
            }
        }
        if (cemeteryId == null && (cemeteryName == null || cemeteryName.trim().isEmpty())) {
            errors.add(new GraveImportErrorDto(rowNumber, "Cemetery/Graveyard name (or graveyardId) is required"));
            return;
        }
        if (cemeteryName != null) {
            cemeteryName = cemeteryName.trim();
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

        // 5. Validate Dates (optional)
        LocalDate dateOfBirth = null;
        if (dobStr != null && !dobStr.trim().isEmpty()) {
            try {
                dateOfBirth = parseFlexibleDate(dobStr.trim());
            } catch (Exception e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid dateOfBirth format: '" + dobStr + "'. Expected format: YYYY-MM-DD"));
                return;
            }
        }

        LocalDate dateOfDeath = null;
        if (dodStr != null && !dodStr.trim().isEmpty()) {
            try {
                dateOfDeath = parseFlexibleDate(dodStr.trim());
            } catch (Exception e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid dateOfDeath format: '" + dodStr + "'. Expected format: YYYY-MM-DD"));
                return;
            }
        }

        if (dateOfBirth != null && dateOfDeath != null && dateOfDeath.isBefore(dateOfBirth)) {
            errors.add(new GraveImportErrorDto(rowNumber, "dateOfDeath (" + dateOfDeath + ") cannot be before dateOfBirth (" + dateOfBirth + ")"));
            return;
        }

        // 6. Validate Gender (optional)
        DeceasedPerson.Gender gender = null;
        if (genderStr != null && !genderStr.trim().isEmpty()) {
            try {
                gender = DeceasedPerson.Gender.valueOf(genderStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(new GraveImportErrorDto(rowNumber, "Invalid gender: '" + genderStr + "'. Allowed values: MALE, FEMALE, OTHER"));
                return;
            }
        }

        // 7. Validate Location Accuracy (optional)
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

        // 8. Validate Verification Status (optional, default: UNVERIFIED)
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

        // 9. Check for duplicate within the CSV file for the SAME cemetery
        String cemeteryKey = cemeteryId != null ? "ID:" + cemeteryId : "NAME:" + cemeteryName.toLowerCase();
        String csvKey = cemeteryKey + "::" + effectiveGraveNumber.toLowerCase();
        if (!seenInCsv.add(csvKey)) {
            String cemeteryDisplay = cemeteryName != null ? cemeteryName : String.valueOf(cemeteryId);
            errors.add(new GraveImportErrorDto(rowNumber, "Duplicate grave record in CSV file: Grave number '" +
                    effectiveGraveNumber + "' already listed for cemetery '" + cemeteryDisplay + "'"));
            return;
        }

        // Validated row -> create ParsedGraveRow
        ParsedGraveRow parsed = ParsedGraveRow.builder()
                .rowNumber(rowNumber)
                .serialNumber(serialNumber != null ? serialNumber.trim() : null)
                .cemeteryName(cemeteryName)
                .cemeteryId(cemeteryId)
                .graveNumber(effectiveGraveNumber)
                .latitude(latitude)
                .longitude(longitude)
                .section(section != null && !section.trim().isEmpty() ? section.trim() : null)
                .row(row != null && !row.trim().isEmpty() ? row.trim() : null)
                .locationAccuracy(locationAccuracy)
                .verificationStatus(status)
                .deceasedName(deceasedName != null && !deceasedName.trim().isEmpty() ? deceasedName.trim() : null)
                .dateOfBirth(dateOfBirth)
                .dateOfDeath(dateOfDeath)
                .gender(gender)
                .photoUrl(photoUrl != null && !photoUrl.trim().isEmpty() ? photoUrl.trim() : null)
                .build();

        validRows.add(parsed);
    }

    private void validateHeaders(Map<String, Integer> headerIndexMap) {
        boolean hasCemetery = headerIndexMap.containsKey("cemeteryname") || headerIndexMap.containsKey("cemeteryid");
        boolean hasGraveIdentifier = headerIndexMap.containsKey("gravenumber") || headerIndexMap.containsKey("serialnumber");
        boolean hasLatitude = headerIndexMap.containsKey("latitude");
        boolean hasLongitude = headerIndexMap.containsKey("longitude");

        if (!hasCemetery || !hasGraveIdentifier || !hasLatitude || !hasLongitude) {
            List<String> missing = new ArrayList<>();
            if (!hasCemetery) missing.add("cemeteryName (or cemeteryId/graveyardName)");
            if (!hasGraveIdentifier) missing.add("graveNumber (or serialNumber)");
            if (!hasLatitude) missing.add("latitude");
            if (!hasLongitude) missing.add("longitude");

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
            if (normalized.equals("serialnumber") || normalized.equals("serialno") || normalized.equals("serial") || normalized.equals("seq")) {
                map.put("serialnumber", i);
            } else if (normalized.equals("gravenumber") || normalized.equals("graveno") || normalized.equals("plotnumber")
                    || normalized.equals("plotno") || normalized.equals("plot") || normalized.equals("grave")) {
                map.put("gravenumber", i);
            } else if (normalized.equals("cemeteryname") || normalized.equals("cemetery") || normalized.equals("graveyardname")
                    || normalized.equals("graveyard") || normalized.equals("remembermename") || normalized.equals("rememberme")) {
                map.put("cemeteryname", i);
            } else if (normalized.equals("cemeteryid") || normalized.equals("graveyardid") || normalized.equals("remembermeid")) {
                map.put("cemeteryid", i);
            } else if (normalized.equals("latitude") || normalized.equals("lat")) {
                map.put("latitude", i);
            } else if (normalized.equals("longitude") || normalized.equals("lng") || normalized.equals("lon") || normalized.equals("long")) {
                map.put("longitude", i);
            } else if (normalized.equals("deceasedname") || normalized.equals("deceased") || normalized.equals("fullname")
                    || normalized.equals("personname") || normalized.equals("name")) {
                map.put("deceasedname", i);
            } else if (normalized.equals("dateofbirth") || normalized.equals("dob") || normalized.equals("birthdate") || normalized.equals("born")) {
                map.put("dateofbirth", i);
            } else if (normalized.equals("dateofdeath") || normalized.equals("dod") || normalized.equals("deathdate") || normalized.equals("died")) {
                map.put("dateofdeath", i);
            } else if (normalized.equals("section") || normalized.equals("block") || normalized.equals("sec")) {
                map.put("section", i);
            } else if (normalized.equals("row") || normalized.equals("rownumber") || normalized.equals("rowno")) {
                map.put("row", i);
            } else if (normalized.equals("locationaccuracy") || normalized.equals("accuracy") || normalized.equals("gpsaccuracy")) {
                map.put("locationaccuracy", i);
            } else if (normalized.equals("verificationstatus") || normalized.equals("status") || normalized.equals("verification")) {
                map.put("verificationstatus", i);
            } else if (normalized.equals("gender") || normalized.equals("sex")) {
                map.put("gender", i);
            } else if (normalized.equals("photourl") || normalized.equals("photo") || normalized.equals("image") || normalized.equals("imageurl")) {
                map.put("photourl", i);
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

    private LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String clean = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,                    // yyyy-MM-dd
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy/M/d"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(clean, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Unparseable date: " + dateStr);
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
