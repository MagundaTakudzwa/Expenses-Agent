package com.receiptapp.expenses.category;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends Repository<ExpenseCategory, Long> {

    <S extends ExpenseCategory> S save(S category);

    Optional<ExpenseCategory> findByIdAndCompanyId(Long id, Long companyId);

    /** The worker's category picker: active only, in the company's chosen order. */
    List<ExpenseCategory> findByCompanyIdAndActiveTrueOrderBySortOrderAscNameAsc(Long companyId);

    /** The admin's settings screen: includes switched-off categories. */
    List<ExpenseCategory> findByCompanyIdOrderBySortOrderAscNameAsc(Long companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}