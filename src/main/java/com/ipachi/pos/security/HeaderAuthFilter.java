// src/main/java/com/ipachi/pos/security/HeaderAuthFilter.java
package com.ipachi.pos.security;

import com.ipachi.pos.model.*;
import com.ipachi.pos.repo.StaffMemberRepository;
import com.ipachi.pos.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HeaderAuthFilter extends OncePerRequestFilter {

    public static final String HDR_USER = "X-User-Id";
    public static final String HDR_BIZ  = "X-Business-Id";

    private final UserRepository users;
    private final StaffMemberRepository staffRepo;

    public HeaderAuthFilter(UserRepository users, StaffMemberRepository staffRepo) {
        this.users = users;
        this.staffRepo = staffRepo;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        // We ALWAYS try to enrich authorities if headers are present.
        String userIdH = request.getHeader(HDR_USER);
        String bizIdH  = request.getHeader(HDR_BIZ);

        if (StringUtils.hasText(userIdH) && StringUtils.hasText(bizIdH)) {
            try {
                Long userId = Long.valueOf(userIdH.trim());
                Long businessId = Long.valueOf(bizIdH.trim());

                users.findById(userId).filter(User::isEnabled).ifPresent(user -> {
                    // Build module authorities based on role/staff membership
                    Set<SimpleGrantedAuthority> moduleAuths = buildModuleAuthorities(user, businessId);

                    // Merge with existing authorities (e.g., from JWT)
                    Authentication existing = SecurityContextHolder.getContext().getAuthentication();
                    Set<GrantedAuthority> mergedAuths = new HashSet<>(moduleAuths);

                    if (existing != null && existing.getAuthorities() != null) {
                        mergedAuths.addAll(existing.getAuthorities());
                    }

                    // Choose a principal to preserve (prefer existing if present)
                    Object principalObj;
                    if (existing != null && existing.getPrincipal() != null) {
                        principalObj = existing.getPrincipal();
                    } else {
                        principalObj = new SimplePrincipal(userId, businessId, user.getUsername(), user.getRole());
                    }

                    // If no auth yet OR authorities changed, set a new Authentication
                    boolean shouldReplace = existing == null
                            || !equalAuthorities(existing.getAuthorities(), mergedAuths);

                    if (shouldReplace) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principalObj, null, mergedAuths);
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            } catch (NumberFormatException ignored) {
                // headers invalid → skip enrichment
            }
        }

        chain.doFilter(request, response);
    }

    private boolean equalAuthorities(Collection<? extends GrantedAuthority> a,
                                     Collection<? extends GrantedAuthority> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        Set<String> as = a.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        Set<String> bs = b.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return as.equals(bs);
    }

    private Set<SimpleGrantedAuthority> buildModuleAuthorities(User user, Long businessId) {
        Set<SimpleGrantedAuthority> auths = new HashSet<>();

        if (user.getRole() == UserRole.ADMIN) {
            auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            for (RoleModule m : RoleModule.values()) {
                grantAll(auths, m);
            }
            return auths;
        }

        // Staff: fetch their role + permissions for this business
        staffRepo.findByUserIdAndBusinessId(user.getId(), businessId).ifPresent(staff -> {
            Role role = staff.getRole();
            if (role != null && role.getPermissions() != null) {
                // Optional: add ROLE_STAFF (if you rely on it elsewhere)
                auths.add(new SimpleGrantedAuthority("ROLE_STAFF"));

                for (RolePermission p : role.getPermissions()) {
                    RoleModule mod = p.getModule();
                    if (p.isView())   auths.add(new SimpleGrantedAuthority(mod.name() + ":VIEW"));
                    if (p.isCreate()) auths.add(new SimpleGrantedAuthority(mod.name() + ":CREATE"));
                    if (p.isEdit())   auths.add(new SimpleGrantedAuthority(mod.name() + ":EDIT"));
                    if (p.isDelete()) auths.add(new SimpleGrantedAuthority(mod.name() + ":DELETE"));
                }
            }
        });

        return auths;
    }

    private static void grantAll(Set<SimpleGrantedAuthority> auths, RoleModule m) {
        auths.add(new SimpleGrantedAuthority(m.name() + ":VIEW"));
        auths.add(new SimpleGrantedAuthority(m.name() + ":CREATE"));
        auths.add(new SimpleGrantedAuthority(m.name() + ":EDIT"));
        auths.add(new SimpleGrantedAuthority(m.name() + ":DELETE"));
    }

    /** Minimal principal you can read later if needed */
    public record SimplePrincipal(Long userId, Long businessId, String username, UserRole userRole) {}
}
