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
                .name("Al-Baqi Cemetery")
                .latitude(24.4672)
                .longitude(39.6142)
                .build();

        mockAdminUser = User.builder()
                .id(1L)
                .email("superadmin@example.com")
                .role(User.Role.SUPER_ADMIN)
                .password("encodedSuperPassword")
                .build();
    }

    @Test
    public void importGraves_ValidRow_Success() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12021809,15041865\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.empty());

        RememberMe createdCemetery = RememberMe.builder()
                .id(10L)
                .name("Lincoln Tomb")
                .latitude(39.8203)
                .longitude(-89.6538)
                .status(RememberMe.ApprovalStatus.APPROVED)
                .build();

        when(rememberMeRepository.save(any(RememberMe.class))).thenReturn(createdCemetery);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(10L, "1")).thenReturn(false);

        Grave savedGrave = Grave.builder()
                .id(100L)
                .rememberMe(createdCemetery)
                .graveNumber("1")
                .latitude(39.8203)
                .longitude(-89.6538)
                .build();
        when(graveRepository.save(any(Grave.class))).thenReturn(savedGrave);

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        // Verify cemetery creation
        ArgumentCaptor<RememberMe> cemeteryCaptor = ArgumentCaptor.forClass(RememberMe.class);
        verify(rememberMeRepository).save(cemeteryCaptor.capture());
        assertEquals("Lincoln Tomb", cemeteryCaptor.getValue().getName());

        // Verify deceased person creation with parsed DDMMYYYY dates
        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository).save(deceasedCaptor.capture());
        assertEquals("Abraham Lincoln", deceasedCaptor.getValue().getFullName());
        assertEquals(LocalDate.of(1809, 2, 12), deceasedCaptor.getValue().getDateOfBirth());
        assertEquals(LocalDate.of(1865, 4, 15), deceasedCaptor.getValue().getDateOfDeath());
    }

    @Test
    public void importGraves_CemeteryNameTooLong_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,This Cemetery Name Is Way Too Long Beyond Twenty,1,39.8203,-89.6538,Abraham Lincoln,12021809,15041865\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Cemetery name must not exceed 20 characters.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_DeceasedNameTooLong_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,This Deceased Name Is Extremely Long,12021809,15041865\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals("Deceased name must not exceed 20 characters.", response.getErrors().get(0).getMessage());
    }

    @Test
    public void importGraves_InvalidCoordinates_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,invalid_lat,-89.6538,Abraham Lincoln,12021809,15041865\n" +
                "2,Lincoln Tomb,2,95.0000,-89.6538,Abraham Lincoln,12021809,15041865\n" +
                "3,Lincoln Tomb,3,39.8203,200.0000,Abraham Lincoln,12021809,15041865\n";

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
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,1809-02-12,15041865\n" +  // Hyphenated format rejected
                "2,Lincoln Tomb,2,39.8203,-89.6538,Abraham Lincoln,31022000,15041865\n" +   // 31 Feb rejected
                "3,Lincoln Tomb,3,39.8203,-89.6538,Abraham Lincoln,32012000,15041865\n" +   // 32 Jan rejected
                "4,Lincoln Tomb,4,39.8203,-89.6538,Abraham Lincoln,00000000,15041865\n";    // 00000000 rejected

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(4, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(4, response.getFailedRecords());
        for (int i = 0; i < 4; i++) {
            assertEquals("Date of birth must be a valid date in DDMMYYYY format.", response.getErrors().get(i).getMessage());
        }
    }

    @Test
    public void importGraves_DateOfDeathEarlierThanDateOfBirth_RejectsRow() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,15041865,12021809\n";

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
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Person One,12021809,15041865\n" +
                "2,Lincoln Tomb,1,39.8204,-89.6539,Person Two,12021809,15041865\n";

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
    public void importGraves_InvalidSuperAdminPassword_ThrowsApiException() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12021809,15041865\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGravesFromCsv(file, "WrongPassword", "admin@example.com"));

        assertTrue(ex.getMessage().contains("Invalid Super Admin password"));
    }
}
