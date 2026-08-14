package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Photo;
import com.rememberme.api.repository.PhotoRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media Upload", description = "Upload grave photos/profile photos")
public class MediaController {

    private final PhotoRepository photoRepository;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<Photo> uploadPhoto(@RequestParam MultipartFile file,
                                           @RequestParam String ownerType,
                                           @RequestParam Long ownerId) {
        // TODO: upload `file` to AWS S3 / Cloudinary and get back a URL.
        String uploadedUrl = "https://your-bucket.s3.amazonaws.com/" + file.getOriginalFilename();

        Photo photo = Photo.builder()
                .url(uploadedUrl)
                .ownerType(Photo.OwnerType.valueOf(ownerType))
                .ownerId(ownerId)
                .build();

        return ApiResponse.success(photoRepository.save(photo));
    }
}
