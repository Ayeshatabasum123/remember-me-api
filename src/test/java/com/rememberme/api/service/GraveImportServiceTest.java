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
import static org.mockito.ArgumentMatchers.eq;
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
    public void importGraves_AutoCreatesNewCemeteryAndSavesDeceasedPerson() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,1809-02-12,1865-04-15\n";

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

        // Verify cemetery was auto-created
        ArgumentCaptor<RememberMe> cemeteryCaptor = ArgumentCaptor.forClass(RememberMe.class);
        verify(rememberMeRepository).save(cemeteryCaptor.capture());
        assertEquals("Lincoln Tomb", cemeteryCaptor.getValue().getName());
        assertEquals(RememberMe.ApprovalStatus.APPROVED, cemeteryCaptor.getValue().getStatus());

        // Verify grave was saved
        ArgumentCaptor<Grave> graveCaptor = ArgumentCaptor.forClass(Grave.class);
        verify(graveRepository).save(graveCaptor.capture());
        assertEquals("1", graveCaptor.getValue().getGraveNumber());

        // Verify deceased person was saved
        ArgumentCaptor<DeceasedPerson> deceasedCaptor = ArgumentCaptor.forClass(DeceasedPerson.class);
        verify(deceasedPersonRepository).save(deceasedCaptor.capture());
        assertEquals("Abraham Lincoln", deceasedCaptor.getValue().getFullName());
        assertEquals(LocalDate.of(1809, 2, 12), deceasedCaptor.getValue().getDateOfBirth());
        assertEquals(LocalDate.of(1865, 4, 15), deceasedCaptor.getValue().getDateOfDeath());
        assertEquals(savedGrave, deceasedCaptor.getValue().getGrave());
    }

    @Test
    public void importGraves_ReusesExistingCemeteryWithoutDuplicating() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Al-Baqi Cemetery,G-1,24.4672,39.6142,Person One,1950-01-01,2020-01-01\n" +
                "2,Al-Baqi Cemetery,G-2,24.4673,39.6143,Person Two,1960-01-01,2021-01-01\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("superadmin@example.com")).thenReturn(Optional.of(mockAdminUser));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Al-Baqi Cemetery")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "G-1")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(1L, "G-2")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertNotNull(response);
        assertEquals(2, response.getTotalRecords());
        assertEquals(2, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());

        // No new cemetery should be saved because it already exists
        verify(rememberMeRepository, never()).save(any());
        verify(graveRepository, times(2)).save(any(Grave.class));
        verify(deceasedPersonRepository, times(2)).save(any(DeceasedPerson.class));
    }

    @Test
    public void importGraves_MultipleCemeteriesWithSameSerialNumber_IsValid() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName\n" +
                "1,Nelson Mandela family grave,1,-31.8600,28.5600,Nelson Mandela\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        RememberMe cemetery1 = RememberMe.builder().id(11L).name("Nelson Mandela family grave").build();
        RememberMe cemetery2 = RememberMe.builder().id(12L).name("Lincoln Tomb").build();

        when(rememberMeRepository.findFirstByNameIgnoreCase("Nelson Mandela family grave")).thenReturn(Optional.of(cemetery1));
        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(cemetery2));

        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(11L, "1")).thenReturn(false);
        when(graveRepository.existsByRememberMeIdAndGraveNumberIgnoreCase(12L, "1")).thenReturn(false);
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(2, response.getTotalRecords());
        assertEquals(2, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
    }

    @Test
    public void importGraves_DuplicateGraveInSameCemetery_IsRejected() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538\n" +
                "2,Lincoln Tomb,1,39.8204,-89.6539\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(rememberMeRepository.findFirstByNameIgnoreCase("Lincoln Tomb")).thenReturn(Optional.of(mockGraveyard));
        when(graveRepository.save(any(Grave.class))).thenAnswer(i -> i.getArgument(0));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(2, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertEquals(1, response.getErrors().size());
        assertEquals(3, response.getErrors().get(0).getRow());
        assertTrue(response.getErrors().get(0).getMessage().contains("Duplicate grave record in CSV"));
    }


    @Test
    public void importGraves_InvalidDates_ReportsError() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,1900-01-01,1800-01-01\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        GraveImportResponseDto response = graveImportService.importGravesFromCsv(file, "SuperAdmin@123", "superadmin@example.com");

        assertEquals(1, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertTrue(response.getErrors().get(0).getMessage().contains("cannot be before dateOfBirth"));
    }

    @Test
    public void importGraves_InvalidSuperAdminPassword_ThrowsApiException() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude\n1,Lincoln Tomb,1,39.8203,-89.6538\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGravesFromCsv(file, "WrongPassword", "admin@example.com"));

        assertTrue(ex.getMessage().contains("Invalid Super Admin password"));
    }
}
