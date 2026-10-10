package com.rememberme.api.service;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.DeceasedPerson;
import com.rememberme.api.entity.RememberMe;
import com.rememberme.api.entity.User;
import com.rememberme.api.repository.DeceasedPersonRepository;
import com.rememberme.api.repository.RememberMeRepository;
import com.rememberme.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    private DeceasedPersonRepository deceasedPersonRepository;

    @Mock
    private RememberMeRepository rememberMeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    private RememberMe mockMemorial;
    private DeceasedPerson mockPerson;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMemorial = RememberMe.builder()
                .id(10L)
                .name("Raj Ghat")
                .city("New Delhi")
                .country("India")
                .status(RememberMe.ApprovalStatus.APPROVED)
                .build();

        mockPerson = DeceasedPerson.builder()
                .id(1L)
                .fullName("Mahatma Gandhi")
                .build();

        mockUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .build();
    }

    @Test
    @DisplayName("City-only search returns matching records")
    void search_CityOnly_ReturnsMatchingRecords() {
        when(rememberMeRepository.searchByCityAndCountryUser(eq("New Delhi"), isNull(), any()))
                .thenReturn(List.of(mockMemorial));
        when(deceasedPersonRepository.searchByCityAndCountryUser(eq("New Delhi"), isNull(), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search(null, "New Delhi", null, null, null);

        assertTrue(response.isSuccess());
        assertEquals("Records found", response.getMessage());
        List<?> rememberMes = (List<?>) response.getData().get("rememberMes");
        assertEquals(1, rememberMes.size());
        assertEquals(mockMemorial, rememberMes.get(0));
    }

    @Test
    @DisplayName("Country-only search returns matching records")
    void search_CountryOnly_ReturnsMatchingRecords() {
        when(rememberMeRepository.searchByCityAndCountryUser(isNull(), eq("India"), any()))
                .thenReturn(List.of(mockMemorial));
        when(deceasedPersonRepository.searchByCityAndCountryUser(isNull(), eq("India"), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search(null, null, "India", null, null);

        assertTrue(response.isSuccess());
        assertEquals("Records found", response.getMessage());
        List<?> rememberMes = (List<?>) response.getData().get("rememberMes");
        assertEquals(1, rememberMes.size());
    }

    @Test
    @DisplayName("Combined city and country search returns matching records")
    void search_CombinedCityAndCountry_ReturnsMatchingRecords() {
        when(rememberMeRepository.searchByCityAndCountryUser(eq("New Delhi"), eq("India"), any()))
                .thenReturn(List.of(mockMemorial));
        when(deceasedPersonRepository.searchByCityAndCountryUser(eq("New Delhi"), eq("India"), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search(null, "New Delhi", "India", null, null);

        assertTrue(response.isSuccess());
        assertEquals("Records found", response.getMessage());
        List<?> rememberMes = (List<?>) response.getData().get("rememberMes");
        assertEquals(1, rememberMes.size());
    }

    @Test
    @DisplayName("Partial match search via query parameter returns matching records")
    void search_PartialMatch_ReturnsMatchingRecords() {
        when(rememberMeRepository.searchAllFieldsUser(eq("del"), any()))
                .thenReturn(List.of(mockMemorial));
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("del"), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search("del", null, null, null, null);

        assertTrue(response.isSuccess());
        assertEquals("Records found", response.getMessage());
        List<?> rememberMes = (List<?>) response.getData().get("rememberMes");
        assertEquals(1, rememberMes.size());
    }

    @Test
    @DisplayName("Non-existent location search returns 200 with 'No city or country found.'")
    void search_NonExistentLocation_ReturnsNoCityOrCountryFound() {
        when(rememberMeRepository.searchAllFieldsUser(eq("Atlantis"), any()))
                .thenReturn(Collections.emptyList());
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("Atlantis"), any()))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search("Atlantis", null, null, null, null);

        assertTrue(response.isSuccess());
        assertEquals("No city or country found.", response.getMessage());
        List<?> people = (List<?>) response.getData().get("people");
        List<?> rememberMes = (List<?>) response.getData().get("rememberMes");
        assertTrue(people.isEmpty());
        assertTrue(rememberMes.isEmpty());
    }

    @Test
    @DisplayName("Admin search calls admin repository methods")
    void search_AdminRole_CallsAdminRepositoryMethods() {
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(rememberMeRepository.searchAllFieldsAdmin(eq("Delhi")))
                .thenReturn(List.of(mockMemorial));
        when(deceasedPersonRepository.searchByNameOrLocationAdmin(eq("Delhi")))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search("Delhi", null, null, null, adminAuth);

        assertTrue(response.isSuccess());
        assertEquals("Records found", response.getMessage());
        verify(rememberMeRepository).searchAllFieldsAdmin("Delhi");
        verify(deceasedPersonRepository).searchByNameOrLocationAdmin("Delhi");
        verify(rememberMeRepository, never()).searchAllFieldsUser(any(), any());
    }

    @Test
    @DisplayName("Authenticated user resolves user ID for role filtering")
    void search_AuthenticatedUser_ResolvesUserId() {
        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));

        when(rememberMeRepository.searchAllFieldsUser(eq("Delhi"), eq(1L)))
                .thenReturn(List.of(mockMemorial));
        when(deceasedPersonRepository.searchByNameOrLocationUser(eq("Delhi"), eq(1L)))
                .thenReturn(Collections.emptyList());

        ApiResponse<Map<String, Object>> response = searchService.search("Delhi", null, null, null, userAuth);

        assertTrue(response.isSuccess());
        verify(rememberMeRepository).searchAllFieldsUser("Delhi", 1L);
    }

    @Test
    @DisplayName("No search criteria returns 'No city or country found.'")
    void search_NoCriteria_ReturnsNoCityOrCountryFound() {
        ApiResponse<Map<String, Object>> response = searchService.search(null, null, null, null, null);

        assertTrue(response.isSuccess());
        assertEquals("No city or country found.", response.getMessage());
        List<?> people = (List<?>) response.getData().get("people");
        List<?> rememberMes = (List<?>) response.getData().get("rememberMes");
        assertTrue(people.isEmpty());
        assertTrue(rememberMes.isEmpty());
    }
}
