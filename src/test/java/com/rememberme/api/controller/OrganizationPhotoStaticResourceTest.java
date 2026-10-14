package com.rememberme.api.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrganizationPhotoStaticResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.upload.dir:uploads}")
    private String uploadDirProperty;

    private Path testPhotoPath;

    @BeforeEach
    public void setUp() throws Exception {
        Path orgDir = Paths.get(uploadDirProperty).resolve("organizations").toAbsolutePath().normalize();
        Files.createDirectories(orgDir);
        testPhotoPath = orgDir.resolve("test-church-photo.png");
        byte[] fakePng = new byte[]{
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
        };
        Files.write(testPhotoPath, fakePng);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (testPhotoPath != null) {
            Files.deleteIfExists(testPhotoPath);
        }
    }

    @Test
    public void accessUploadedPhoto_ReturnsHttp200AndContent() throws Exception {
        mockMvc.perform(get("/uploads/organizations/test-church-photo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    public void accessMissingPhoto_ReturnsHttp404NotFound() throws Exception {
        mockMvc.perform(get("/uploads/organizations/nonexistent-photo.png"))
                .andExpect(status().isNotFound());
    }
}
