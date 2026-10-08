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
                .name("Lincoln Tomb")
                .latitude(39.8203)
                .longitude(-89.6538)
                .build();

        mockAdminUser = User.builder()
                .id(1L)
                .email("superadmin@example.com")
                .role(User.Role.SUPER_ADMIN)
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
    public void importGraves_InvalidSuperAdminPassword_ThrowsApiException() {
        String csvContent = "serialNumber,cemeteryName,graveNumber,latitude,longitude,deceasedName,dateOfBirth,dateOfDeath,biography\n" +
                "1,Lincoln Tomb,1,39.8203,-89.6538,Abraham Lincoln,12/02/1809,15/04/1865,President\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "graves.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                graveImportService.importGravesFromCsv(file, "WrongPassword", "admin@example.com"));

        assertTrue(ex.getMessage().contains("Invalid Super Admin password"));
    }
}
