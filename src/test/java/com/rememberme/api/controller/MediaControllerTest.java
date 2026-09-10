package com.rememberme.api.controller;

import com.rememberme.api.entity.Photo;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.repository.PhotoRepository;
import com.rememberme.api.security.CustomUserDetailsService;
import com.rememberme.api.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhotoRepository photoRepository;

    @MockBean
    private GraveRepository graveRepository;

    @MockBean
    private MemorialRepository memorialRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void uploadPhoto_Grave_ValidId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
        when(graveRepository.existsById(14L)).thenReturn(true);

        Photo savedPhoto = Photo.builder()
                .id(1L)
                .url("https://your-bucket.s3.amazonaws.com/test.jpg")
                .ownerType(Photo.OwnerType.GRAVE)
                .ownerId(14L)
                .build();

        when(photoRepository.save(any(Photo.class))).thenReturn(savedPhoto);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("ownerType", "GRAVE")
                        .param("ownerId", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.ownerType").value("GRAVE"))
                .andExpect(jsonPath("$.data.ownerId").value(14));

        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    public void uploadPhoto_Grave_NotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
        when(graveRepository.existsById(200L)).thenReturn(false);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("ownerType", "GRAVE")
                        .param("ownerId", "200"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Grave with ID 200 not found"));

        verify(photoRepository, never()).save(any(Photo.class));
    }

    @Test
    public void uploadPhoto_Memorial_ValidId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
        when(memorialRepository.existsById(14L)).thenReturn(true);

        Photo savedPhoto = Photo.builder()
                .id(2L)
                .url("https://your-bucket.s3.amazonaws.com/test.jpg")
                .ownerType(Photo.OwnerType.MEMORIAL)
                .ownerId(14L)
                .build();

        when(photoRepository.save(any(Photo.class))).thenReturn(savedPhoto);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("ownerType", "MEMORIAL")
                        .param("ownerId", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.ownerType").value("MEMORIAL"))
                .andExpect(jsonPath("$.data.ownerId").value(14));

        verify(photoRepository, times(1)).save(any(Photo.class));
    }

    @Test
    public void uploadPhoto_Memorial_NotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());
        when(memorialRepository.existsById(200L)).thenReturn(false);

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("ownerType", "MEMORIAL")
                        .param("ownerId", "200"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Memorial with ID 200 not found"));

        verify(photoRepository, never()).save(any(Photo.class));
    }

    @Test
    public void uploadPhoto_InvalidOwnerType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image content".getBytes());

        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("ownerType", "INVALID")
                        .param("ownerId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid ownerType: INVALID. Valid values are: GRAVE, MEMORIAL"));

        verify(photoRepository, never()).save(any(Photo.class));
    }
}
