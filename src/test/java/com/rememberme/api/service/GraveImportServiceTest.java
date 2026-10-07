package com.rememberme.api.service;

import com.rememberme.api.dto.response.GraveImportResponseDto;
import com.rememberme.api.entity.Grave;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.exception.ApiException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GraveImportServiceTest {

    @Mock
    private GraveRepository graveRepository;

    @Mock
    private RememberMeRepository rememberMeRepository;

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
    public void importGraves_Success_AllValidRecords() {
        String csvContent = "serialNumber,graveyardId,section,row,latitude,longitude,locationAccuracy,verificationStatus\n" +
                "A-101,1,Section A,Row 1,24.8607,67.0011,1.5,VERIFIED\n" +
                "A-102,1,Section A,Row 2,24.8608,67.0012,2.0,UNVERIFIED\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "A-101")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "A-102")).thenReturn(false);

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(2, response.getTotalRecords());
        assertEquals(2, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Grave>> captor = ArgumentCaptor.forClass(List.class);
        verify(graveRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals("A-101", captor.getValue().get(0).getGraveNumber());
        assertEquals(Grave.VerificationStatus.VERIFIED, captor.getValue().get(0).getVerificationStatus());
    }

    @Test
    public void importGraves_InvalidSuperAdminPassword_ThrowsApiException() {
        String csvContent = "serialNumber,graveyardId,latitude,longitude\nA-101,1,24.8607,67.0011\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGravesFromCsv(file, "WrongPassword", "admin@example.com"));

        assertTrue(ex.getMessage().contains("Invalid Super Admin password"));
        verify(graveRepository, never()).saveAll(any());
    }

    @Test
    public void importGraves_MissingRequiredHeaders_ThrowsApiException() {
        String csvContent = "section,row,locationAccuracy\nSection A,Row 1,1.5\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com"));

        assertTrue(ex.getMessage().contains("missing required column headers"));
    }

    @Test
    public void importGraves_PartialErrors_RecordsFailedRowsAndSavesValidRows() {
        String csvContent = "serialNumber,graveyardId,latitude,longitude\n" +
                "A-101,1,24.8607,67.0011\n" +                    // Valid (Row 2)
                ",1,24.8608,67.0012\n" +                          // Missing serialNumber (Row 3)
                "A-103,999,24.8609,67.0013\n" +                   // Non-existent graveyard (Row 4)
                "A-104,1,invalid_lat,67.0014\n" +                 // Invalid latitude format (Row 5)
                "A-105,1,105.0000,67.0015\n" +                    // Latitude out of range (Row 6)
                "A-106,1,24.8610,67.0016\n" +                     // Duplicate in DB (Row 7)
                "A-101,1,24.8611,67.0017\n";                      // Duplicate in CSV (Row 8)

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(mockGraveyard));
        when(rememberMeRepository.findById(999L)).thenReturn(Optional.empty());

        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "A-101")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "A-106")).thenReturn(true);

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(7, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(6, response.getFailedRecords());
        assertEquals(6, response.getErrors().size());

        assertEquals(3, response.getErrors().get(0).getRow());
        assertTrue(response.getErrors().get(0).getMessage().contains("Serial number"));

        assertEquals(4, response.getErrors().get(1).getRow());
        assertTrue(response.getErrors().get(1).getMessage().contains("Graveyard not found"));

        assertEquals(5, response.getErrors().get(2).getRow());
        assertTrue(response.getErrors().get(2).getMessage().contains("Invalid latitude format"));

        assertEquals(6, response.getErrors().get(3).getRow());
        assertTrue(response.getErrors().get(3).getMessage().contains("Latitude must be between -90.0 and 90.0"));

        assertEquals(7, response.getErrors().get(4).getRow());
        assertTrue(response.getErrors().get(4).getMessage().contains("already exists in graveyard"));

        assertEquals(8, response.getErrors().get(5).getRow());
        assertTrue(response.getErrors().get(5).getMessage().contains("already listed for graveyard"));

        verify(graveRepository, times(1)).saveAll(any());
    }

    @Test
    public void importGraves_HandlesQuotedCsvFields() {
        String csvContent = "serialNumber,graveyardId,section,row,latitude,longitude\n" +
                "\"G-200, Block B\",1,\"Section, South\",Row 5,24.8607,67.0011\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(rememberMeRepository.findById(1L)).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "G-200, Block B")).thenReturn(false);

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Grave>> captor = ArgumentCaptor.forClass(List.class);
        verify(graveRepository).saveAll(captor.capture());
        assertEquals("G-200, Block B", captor.getValue().get(0).getGraveNumber());
        assertEquals("Section, South", captor.getValue().get(0).getSection());
    }
}
