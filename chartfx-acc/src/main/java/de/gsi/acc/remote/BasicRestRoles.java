package de.gsi.acc.remote;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import io.javalin.core.security.Role;

public enum BasicRestRoles implements Role {
    NULL,
    ANYONE,
    ADMIN,
    READ_ONLY,
    READ_WRITE;

    public static String getRoles(final Set<Role> roleSet) {
        return roleSet.stream().map(Role::toString).collect(Collectors.joining(", "));
    }

    public static Set<Role> getRoles(@NotNull final String roleString) {
        if (roleString.contains(":")) {
            throw new IllegalArgumentException("roleString must not contain [:]");
        }

        final HashSet<Role> roles = new HashSet<>();
        for (final String role : roleString.replaceAll("\\s", "").split(",")) {
            if (role == null || role.isEmpty() || "*".equals(role)) { // NOPMD
                continue;
            }
            roles.add(valueOf(role.toUpperCase(Locale.UK)));
        }

        return Collections.unmodifiableSet(roles);
    }
}