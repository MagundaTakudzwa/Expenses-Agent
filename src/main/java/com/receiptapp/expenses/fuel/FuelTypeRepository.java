package com.receiptapp.expenses.fuel;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface FuelTypeRepository extends Repository<FuelType, Long> {

    <S extends FuelType> S save(S fuelType);

    Optional<FuelType> findByIdAndCompanyId(Long id, Long companyId);

    /** The fuel picker. */
    List<FuelType> findByCompanyIdAndActiveTrueOrderBySortOrderAscNameAsc(Long companyId);

    List<FuelType> findByCompanyIdOrderBySortOrderAscNameAsc(Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}