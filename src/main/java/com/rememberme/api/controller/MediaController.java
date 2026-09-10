package com.rememberme.api.controller;

import com.rememberme.api.dto.response.ApiResponse;
import com.rememberme.api.entity.Photo;
import com.rememberme.api.exception.ApiException;
import com.rememberme.api.repository.GraveRepository;
import com.rememberme.api.repository.MemorialRepository;
import com.rememberme.api.repository.PhotoRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media Upload", description = "Upload grave photos/profile photos")
public class MediaController {

    private final PhotoRepository photoRepository;
    private final GraveRepository graveRepository;
    private final MemorialRepository memorialRepository;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<Photo> uploadPhoto(@RequestParam MultipartFile file,
                                           @RequestParam String ownerType,
                                           @RequestParam Long ownerId) {
        Photo.OwnerType type;
        try {
            type = Photo.OwnerType.valueOf(ownerType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException("Invalid ownerType: " + ownerType + ". Valid values are: GRAVE, MEMORIAL", HttpStatus.BAD_REQUEST);
        }

        if (type == Photo.OwnerType.GRAVE) {
            if (!graveRepository.existsById(ownerId)) {
                throw new ApiException("Grave with ID " + ownerId + " not found", HttpStatus.NOT_FOUND);
            }
        } else if (type == Photo.OwnerType.MEMORIAL) {
            if (!memorialRepository.existsById(ownerId)) {
                throw new ApiException("Memorial with ID " + ownerId + " not found", HttpStatus.NOT_FOUND);
            }
        } else {
            throw new ApiException("Invalid ownerType: " + ownerType + ". Valid values are: GRAVE, MEMORIAL", HttpStatus.BAD_REQUEST);
        }

        // TODO: upload `file` to AWS S3 / Cloudinary and get back a URL.
        String uploadedUrl = "https://your-bucket.s3.amazonaws.com/" + file.getOriginalFilename();

        Photo photo = Photo.builder()
                .url(uploadedUrl)
                .ownerType(type)
                .ownerId(ownerId)
                .build();

        return ApiResponse.success(photoRepository.save(photo));
    }
}

