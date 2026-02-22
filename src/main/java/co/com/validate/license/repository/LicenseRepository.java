package co.com.validate.license.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.com.validate.license.model.License;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByLicenseKey(String licenseKey);

    boolean existsByLicenseKey(String licenseKey);

    /**
     * Find all active licenses that expire on the given date
     */
    @Query("SELECT l FROM License l WHERE l.expirationDate = :date AND l.active = true")
    List<License> findByExpirationDateAndActiveTrue(@Param("date") LocalDate date);
}
