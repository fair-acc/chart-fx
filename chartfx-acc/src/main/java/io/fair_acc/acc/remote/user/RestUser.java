package io.fair_acc.acc.remote.user;

import java.util.Collections;
import java.util.Set;

import io.javalin.core.security.Role;

public class RestUser {
    protected final String userName;
    protected String salt;
    protected String hashedPassword;
    private final Set<Role> roles;

    public RestUser(final String username, final String salt, final String hashedPassword, final Set<Role> roles) {
        this.userName = username;
        this.salt = salt;
        this.hashedPassword = hashedPassword;
        this.roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    protected Set<Role> getRoles() {
        return roles;
    }
}
