package com.rememberme.api.repository;

import com.rememberme.api.entity.OrganizationAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationAdminRepository extends JpaRepository<OrganizationAdmin, Long> {
    Optional<OrganizationAdmin> findByUserId(Long userId);
    Optional<OrganizationAdmin> findByUserEmail(String email);
    List<OrganizationAdmin> findByOrganizationId(Long organizationId);
}
