package com.receiptapp.expenses.user;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends Repository<AppUser, Long> {

    <S extends AppUser> S save(S user);

    /** Used only when logging in: emails are unique across the whole system. */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByIdAndCompanyId(Long id, Long companyId);

    /** The admin's team list. */
    List<AppUser> findByCompanyIdOrderByFullNameAsc(Long companyId);

    /** e.g. "all active managers", for notifications. */
    List<AppUser> findByCompanyIdAndRoleAndActiveTrue(Long companyId, Role role);
}