package com.rememberme.api.repository;

import com.rememberme.api.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByOwnerTypeAndOwnerId(Photo.OwnerType ownerType, Long ownerId);
}

