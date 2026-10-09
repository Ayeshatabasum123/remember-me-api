package com.rememberme.api.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GraveImportServiceTest {

    @Mock
    private GraveRepository graveRepository;

    @Mock
    private RememberMeRepository rememberMeRepository;

    @Mock
    private DeceasedPersonRepository deceasedPersonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PhotoUrlValidator photoUrlValidator;

    @InjectMocks
    private GraveImportService graveImportService;

    private RememberMe mockGraveyard;
    private User mockAdminUser;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(graveImportService, "adminPasswordConfig", "Admin@123");
        ReflectionTestUtils.setField(graveImportService, "superAdminPasswordConfig", "SuperAdmin@123");

        mockGraveyard = RememberMe.builder()
                .id(1L)
                .name("Lincoln Tomb")
                .latitude(39.8203)
                .longitude(-89.6538)
                .build();

        mockAdminUser = User.builder()
                .id(1L)
                .email("superadmin@example.com")
                .role(User.Role.ADMIN)
                .password("encodedSuperPassword")
                .build();
    }

    @Test
    public void importGraves_ValidRow_SuccessWithBothDateFormatsAndBiography() {
        // Tests all 4 combinations: (slash/slash), (hyphen/hyphen), (slash/hyphen), (hyphen/slash)
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Mahatma Gandhi,02/10/1869,30/01/1948,Leader of the Indian independence movement.\n" +
                "2,Lincoln Tomb,2,39.8204,-89.6539,Mahatma Gandhi,02-10-1869,30-01-1948,Leader of the Indian independence movement.\n" +
                "3,Lincoln Tomb,3,39.8205,-89.6540,Mahatma Gandhi,02/10/1869,30-01-1948,Leader of the Indian independence movement.\n" +
                "4,Lincoln Tomb,4,39.8206,-89.6541,Mahatma Gandhi,02-10-1869,30/01/1948,Leader of the Indian independence movement.\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "1")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "2")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "3")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "4")).thenReturn(false);

        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(4, response.getTotalRecords());
        assertEquals(4, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        // Verify deceased persons creation with correctly parsed dates and biography
        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository, times(4)).save(deceasedCaptor.capture());
        List<DeceasedPerson> savedPersons = deceasedCaptor.getAllValues();

        // Row 1 (slash/slash)
        assertEquals("Mahatma Gandhi", savedPersons.get(0).getFullName());
        assertEquals(LocalDate.of(1869, 10, 2), savedPersons.get(0).getDateOfBirth());
        assertEquals(LocalDate.of(1948, 1, 30), savedPersons.get(0).getDateOfDeath());
        assertEquals("Leader of the Indian independence movement.", savedPersons.get(0).getBiography());

        // Row 2 (hyphen/hyphen)
        assertEquals("Mahatma Gandhi", savedPersons.get(1).getFullName());
        assertEquals(LocalDate.of(1869, 10, 2), savedPersons.get(1).getDateOfBirth());
        assertEquals(LocalDate.of(1948, 1, 30), savedPersons.get(1).getDateOfDeath());
        assertEquals("Leader of the Indian independence movement.", savedPersons.get(1).getBiography());

        // Row 3 (slash/hyphen)
        assertEquals("Mahatma Gandhi", savedPersons.get(2).getFullName());
        assertEquals(LocalDate.of(1869, 10, 2), savedPersons.get(2).getDateOfBirth());
        assertEquals(LocalDate.of(1948, 1, 30), savedPersons.get(2).getDateOfDeath());
        assertEquals("Leader of the Indian independence movement.", savedPersons.get(2).getBiography());

        // Row 4 (hyphen/slash)
        assertEquals("Mahatma Gandhi", savedPersons.get(3).getFullName());
        assertEquals(LocalDate.of(1869, 10, 2), savedPersons.get(3).getDateOfBirth());
        assertEquals(LocalDate.of(1948, 1, 30), savedPersons.get(3).getDateOfDeath());
        assertEquals("Leader of the Indian independence movement.", savedPersons.get(3).getBiography());
    }

    @Test
    public void importGraves_BiographyEmpty_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,\n" +
                "2,Lincoln Tomb,2,39.8204,-89.6539,Alan Turing,23-06-1912,07-06-1954,   \n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(2, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(2, response.getFailedRecords());
        assertEquals("Biography is required.", response.getErrors().get(0).getMessage());
        assertEquals("Biography is required.", response.getErrors().get(1).getMessage());
    }

    @Test
    public void importGraves_BiographyTooLong_RejectsRow() {
        String longBio = "B".repeat(201);
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865," + longBio + "\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Biography must not exceed 200 characters.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_BiographyExact200Chars_Success() {
        String exact200Bio = "B".repeat(200);
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12-02-1809,15/04/1865," + exact200Bio + "\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "1")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository).save(deceasedCaptor.capture());
        assertEquals(exact200Bio, deceasedCaptor.getValue().getBiography());
    }

    @Test
    public void importGraves_CemeteryNameTooLong_RejectsRow() {
        String longCemeteryName = "C".repeat(101);
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1," + longCemeteryName + ",1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Cemetery name must be 100 characters or fewer.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_DeceasedNameTooLong_RejectsRow() {
        String longDeceasedName = "D".repeat(101);
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538," + longDeceasedName + ",12/02/1809,15/04/1865,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Deceased name must be 100 characters or fewer.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_CemeteryAndDeceasedNameExact100Chars_Success() {
        String exact100CemeteryName = "C".repeat(100);
        String exact100DeceasedName = "D".repeat(100);
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1," + exact100CemeteryName + ",1,39.8203,-89.6538," + exact100DeceasedName + ",12-02-1809,15-04-1865,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        RememberMe graveyard100 = RememberMe.builder()
                .id(2L)
                .name(exact100CemeteryName)
                .latitude(39.8203)
                .longitude(-89.6538)
                .build();

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase(exact100CemeteryName)).thenReturn(Optional.of(graveyard100));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(2L, "1")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository).save(deceasedCaptor.capture());
        assertEquals(exact100DeceasedName, deceasedCaptor.getValue().getFullName());
    }

    @Test
    public void importGraves_InvalidCoordinates_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,invalid_lat,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,President\n" +
                "2,Lincoln Tomb,2,95.0000,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,President\n" +
                "3,Lincoln Tomb,3,39.8203,200.0000,Abraham Lincoln,12/02/1809,15/04/1865,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(3, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(3, response.getFailedRecords());
        assertEquals("Latitude must be a valid floating-point number between -90 and 90.", response.getErrors().get(0).getMessage());
        assertEquals("Latitude must be a valid floating-point number between -90 and 90.", response.getErrors().get(1).getMessage());
        assertEquals("Longitude must be a valid floating-point number between -180 and 180.", response.getErrors().get(2).getMessage());
    }

    @Test
    public void importGraves_InvalidDateFormat_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,1809-02-12,15/04/1865,President\n" +   // Hyphenated YYYY-MM-DD rejected
                "2,Lincoln Tomb,2,39.8203,-89.6538,Abraham Lincoln,12021809,15/04/1865,President\n" +     // DDMMYYYY without slashes rejected
                "3,Lincoln Tomb,3,39.8203,-89.6538,Abraham Lincoln,31/02/1948,30-01-1948,President\n" +   // 31 Feb rejected
                "4,Lincoln Tomb,4,39.8203,-89.6538,Abraham Lincoln,32-01-2000,15/04/1865,President\n" +   // 32 Jan rejected
                "5,Lincoln Tomb,5,39.8203,-89.6538,Abraham Lincoln,00/00/0000,15/04/1865,President\n" +   // 00/00/0000 rejected
                "6,Lincoln Tomb,6,39.8203,-89.6538,Abraham Lincoln,29-02-2021,15/04/1865,President\n" +   // 29 Feb non-leap rejected
                "7,Lincoln Tomb,7,39.8203,-89.6538,Abraham Lincoln,23/06-1912,15/04/1865,President\n";    // mixed delimiter rejected

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(7, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(7, response.getFailedRecords());
        for (int i = 0; i < 7; i++) {
            assertEquals("Date of birth must be a valid date in DD/MM/YYYY or DD-MM-YYYY format.", response.getErrors().get(i).getMessage());
        }
    }

    @Test
    public void importGraves_InvalidDateOfDeathFormat_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,31/02/1865,President\n" +
                "2,Lincoln Tomb,2,39.8203,-89.6538,Abraham Lincoln,12-02-1809,1865-04-15,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(2, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(2, response.getFailedRecords());
        assertEquals("Date of death must be a valid date in DD/MM/YYYY or DD-MM-YYYY format.", response.getErrors().get(0).getMessage());
        assertEquals("Date of death must be a valid date in DD/MM/YYYY or DD-MM-YYYY format.", response.getErrors().get(1).getMessage());
    }

    @Test
    public void importGraves_DateOfDeathEarlierThanDateOfBirth_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,15/04/1865,12-02-1809,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Date of death cannot be earlier than date of birth.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_DuplicateInSameCemetery_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Person One,12/02/1809,15-04-1865,President\n" +
                "2,Lincoln Tomb,1,39.8204,-89.6539,Person Two,12-02-1809,15/04/1865,President\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(2, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals(3, response.getErrors().get(0).getRow());
        assertTrue(response.getErrors().get(0).getMessage().contains("Duplicate grave record in CSV file"));
    }

    @Test
    public void importGraves_NonAdminUser_ThrowsForbidden() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,President\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        User normalUser = User.builder()
                .id(2L)
                .email("user@example.com")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(normalUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGraves(file, "user@example.com"));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Admin role required"));
    }

    @Test
    public void importGraves_InvalidFileFormat_ThrowsApiException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "graves.pdf", "application/pdf", new byte[]{1, 2, 3});

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGraves(pdfFile, "SuperAdmin@123", "superadmin@example.com"));

        assertTrue(ex.getMessage().contains("Invalid file format. Please upload a valid CSV (.csv) or Excel (.xls, .xlsx) file"));
    }

    @Test
    public void importGraves_Xlsx_ValidData_SuccessWithDateCellsAndTextDates() throws Exception {
        // Prepare calendar dates for Date cells (Excel native date system epoch is 1900+)
        java.util.Calendar calDob = java.util.Calendar.getInstance();
        calDob.set(1940, java.util.Calendar.OCTOBER, 2, 0, 0, 0);
        calDob.set(java.util.Calendar.MILLISECOND, 0);

        java.util.Calendar calDod = java.util.Calendar.getInstance();
        calDod.set(2020, java.util.Calendar.JANUARY, 30, 0, 0, 0);
        calDod.set(java.util.Calendar.MILLISECOND, 0);

        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber", "latitude", "longitude", "deceasedName", "dateOfBirth", "dateOfDeath", "biography"),
                // Row 1: Native Date cells (post-1900)
                List.of(1, "Lincoln Tomb", 1, 39.8203, -89.6538, "John Doe", calDob.getTime(), calDod.getTime(), "Leader of independence"),
                // Row 2: Slash text dates (can be any year, including pre-1900)
                List.of(2, "Lincoln Tomb", 2, 39.8204, -89.6539, "Mahatma Gandhi", "02/10/1869", "30/01/1948", "Leader of independence"),
                // Row 3: Hyphen text dates (can be any year, including pre-1900)
                List.of(3, "Lincoln Tomb", 3, 39.8205, -89.6540, "Mahatma Gandhi", "02-10-1869", "30-01-1948", "Leader of independence")
        );

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "1")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "2")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "3")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(3, response.getTotalRecords());
        assertEquals(3, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository, times(3)).save(deceasedCaptor.capture());
        List<DeceasedPerson> saved = deceasedCaptor.getAllValues();

        assertEquals(LocalDate.of(1940, 10, 2), saved.get(0).getDateOfBirth());
        assertEquals(LocalDate.of(2020, 1, 30), saved.get(0).getDateOfDeath());
    }

    @Test
    public void importGraves_Xls_ValidData_Success() throws Exception {
        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber", "latitude", "longitude", "deceasedName", "dateOfBirth", "dateOfDeath", "biography"),
                List.of(1, "Lincoln Tomb", 1, 39.8203, -89.6538, "Abraham Lincoln", "12/02/1809", "15/04/1865", "16th US President")
        );

        byte[] xlsBytes = createXlsWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xls", "application/vnd.ms-excel", xlsBytes);

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "1")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
    }

    @Test
    public void importGraves_Xlsx_MissingHeaders_ThrowsApiException() throws Exception {
        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber") // Missing required columns
        );

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com"));

        assertTrue(ex.getMessage().contains("Excel file is missing required column headers:"));
    }

    @Test
    public void importGraves_Xlsx_EmptyFile_ThrowsApiException() throws Exception {
        List<List<Object>> rows = List.of();

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com"));

        assertTrue(ex.getMessage().contains("Excel file is empty or missing a valid header row"));
    }

    @Test
    public void importGraves_Xlsx_DuplicateInSameSheet_RejectsRow() throws Exception {
        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber", "latitude", "longitude", "deceasedName", "dateOfBirth", "dateOfDeath", "biography"),
                List.of(1, "Lincoln Tomb", 101, 39.8203, -89.6538, "Person A", "12/02/1809", "15-04-1865", "President"),
                List.of(2, "Lincoln Tomb", 101, 39.8204, -89.6539, "Person B", "12-02-1809", "15/04/1865", "President")
        );

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(2, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertTrue(response.getErrors().get(0).getMessage().contains("Duplicate grave record in Excel file"));
    }

    @Test
    public void importGraves_Xlsx_DateOfDeathEarlierThanDateOfBirth_RejectsRow() throws Exception {
        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber", "latitude", "longitude", "deceasedName", "dateOfBirth", "dateOfDeath", "biography"),
                List.of(1, "Lincoln Tomb", 1, 39.8203, -89.6538, "Person A", "15/04/1865", "12-02-1809", "President")
        );

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        GraveImportResponseDto response = graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Date of death cannot be earlier than date of birth.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_Csv_ValidPhotoUrl_Success() {
        String photoUrl = "https://live.staticflickr.com/84/255569844_3760184197_o.jpg";
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography,photoUrl\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,16th President," + photoUrl + "\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "1")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));
        when(photoUrlValidator.validatePhotoUrl(photoUrl)).thenReturn(PhotoUrlValidator.ValidationResult.success());

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository).save(deceasedCaptor.capture());
        assertEquals(photoUrl, deceasedCaptor.getValue().getPhotoUrl());
    }

    @Test
    public void importGraves_Csv_InvalidPhotoUrl_RejectsRow() {
        String invalidUrl = "https://example.com/not-allowed.png";
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography,photoUrl\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,16th President," + invalidUrl + "\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(photoUrlValidator.validatePhotoUrl(invalidUrl))
                .thenReturn(PhotoUrlValidator.ValidationResult.failure("Only JPG and JPEG image URLs are allowed."));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals(2, response.getErrors().get(0).getRow());
        assertEquals("Only JPG and JPEG image URLs are allowed.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_Xlsx_ValidPhotoUrl_Success() throws Exception {
        String photoUrl = "https://live.staticflickr.com/84/255569844_3760184197_o.jpg";
        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber", "latitude", "longitude", "deceasedName", "dateOfBirth", "dateOfDeath", "biography", "photoUrl"),
                List.of(1, "Lincoln Tomb", 1, 39.8203, -89.6538, "Abraham Lincoln", "12/02/1809", "15/04/1865", "16th US President", photoUrl)
        );

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "1")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));
        when(photoUrlValidator.validatePhotoUrl(photoUrl)).thenReturn(PhotoUrlValidator.ValidationResult.success());

        GraveImportResponseDto response = graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());

        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository).save(deceasedCaptor.capture());
        assertEquals(photoUrl, deceasedCaptor.getValue().getPhotoUrl());
    }

    @Test
    public void importGraves_Xlsx_InvalidPhotoUrl_RejectsRow() throws Exception {
        String photoUrl = "https://example.com/oversized.jpg";
        List<List<Object>> rows = List.of(
                List.of("serialNumber", "cemeteryName", "graveNumber", "latitude", "longitude", "deceasedName", "dateOfBirth", "dateOfDeath", "biography", "photoUrl"),
                List.of(1, "Lincoln Tomb", 1, 39.8203, -89.6538, "Abraham Lincoln", "12/02/1809", "15/04/1865", "16th US President", photoUrl)
        );

        byte[] xlsxBytes = createXlsxWorkbookBytes(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        when(photoUrlValidator.validatePhotoUrl(photoUrl))
                .thenReturn(PhotoUrlValidator.ValidationResult.failure("Image size must not exceed 1 MB."));

        GraveImportResponseDto response = graveImportService.importGraves(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals(2, response.getErrors().get(0).getRow());
        assertEquals("Image size must not exceed 1 MB.", response.getErrors().get(0).getMessage());
    }

    private byte[] createXlsxWorkbookBytes(List<List<Object>> rows) throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Graves");
            org.apache.poi.ss.usermodel.CreationHelper creationHelper = workbook.getCreationHelper();
            org.apache.poi.ss.usermodel.CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            for (int r = 0; r < rows.size(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r);
                List<Object> cellValues = rows.get(r);
                for (int c = 0; c < cellValues.size(); c++) {
                    Object val = cellValues.get(c);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    if (val instanceof String s) {
                        cell.setCellValue(s);
                    } else if (val instanceof Double d) {
                        cell.setCellValue(d);
                    } else if (val instanceof Integer i) {
                        cell.setCellValue(i);
                    } else if (val instanceof java.util.Date d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(dateCellStyle);
                    } else if (val != null) {
                        cell.setCellValue(val.toString());
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createXlsWorkbookBytes(List<List<Object>> rows) throws Exception {
        try (org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Graves");
            org.apache.poi.ss.usermodel.CreationHelper creationHelper = workbook.getCreationHelper();
            org.apache.poi.ss.usermodel.CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd/mm/yyyy"));

            for (int r = 0; r < rows.size(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r);
                List<Object> cellValues = rows.get(r);
                for (int c = 0; c < cellValues.size(); c++) {
                    Object val = cellValues.get(c);
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    if (val instanceof String s) {
                        cell.setCellValue(s);
                    } else if (val instanceof Double d) {
                        cell.setCellValue(d);
                    } else if (val instanceof Integer i) {
                        cell.setCellValue(i);
                    } else if (val instanceof java.util.Date d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(dateCellStyle);
                    } else if (val != null) {
                        cell.setCellValue(val.toString());
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
