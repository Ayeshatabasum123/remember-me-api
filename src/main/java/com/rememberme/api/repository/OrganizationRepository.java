package com.rememberme.api.repository;

import com.rememberme.api.entity.Organization;
import com.rememberme.api.entity.OrganizationApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    List<Organization> findByApprovalStatus(OrganizationApprovalStatus approvalStatus);
    Optional<Organization> findByOrganizationCode(String organizationCode);
    boolean existsByEmail(String email);
    boolean existsByOrganizationCode(String organizationCode);
}
