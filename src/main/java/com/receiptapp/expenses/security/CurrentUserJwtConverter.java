package com.receiptapp.expenses.security;

import com.receiptapp.expenses.user.AppUser;
import com.receiptapp.expenses.user.AppUserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs on every request after the token's signature and expiry are checked.
 * Loads the user from the database, so deactivations and role changes apply
 * immediately.
 */
@Component
public class CurrentUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AppUserRepository users;

    public CurrentUserJwtConverter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        Long companyId = Long.valueOf(jwt.getClaimAsString("cid"));

        AppUser user = users.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new BadCredentialsException("This account no longer exists."));
        if (!user.isActive()) {
            throw new DisabledException("This account has been deactivated.");
        }

        CurrentUser currentUser = new CurrentUser(user.getId(), companyId, user.getRole(), user.getFullName());
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return UsernamePasswordAuthenticationToken.authenticated(currentUser, jwt, authorities);
    }
}