package com.receiptapp.expenses.company;

import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Companies are the top of every tenant, so looking one up by id is safe:
 * the id always comes from the logged-in user, never from a request.
 */
public interface CompanyRepository extends Repository<Company, Long> {

    <S extends Company> S save(S company);

    Optional<Company> findById(Long id);
}