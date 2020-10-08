package de.gsi.acc.remote.user;

import java.util.Set;

import io.javalin.core.security.Role;

/**
 * Basic user handler interface to control access to various routes. 
 * 
 * N.B. new implementations may be injected through the RestServer factory.
 * 
 * @author rstein
 * @see de.gsi.acc.remote.RestServer#setUserHandler(RestUserHandler)
 */
public interface RestUserHandler {
    /**
     * Authenticates user against given back-end.
     * 
     * @param username the user name
     * @param password the secret password
     * @return {@code true} if successful
     */
    boolean authenticate(String username, String password);

    Iterable<String> getAllUserNames();

    RestUser getUserByUsername(String username);

    Set<Role> getUserRolesByUsername(String username);

    /**
     * Sets new user password. 
     * 
     * N.B. Implementation may be implemented or omitted based on the specific back-end.
     * 
     * @param userName existing 
     * @param oldPassword to verify
     * @param newPassword to set
     * @throws SecurityException if underlying implementation does not allow to change the password.
     * @return {@code true} if successful
     */
    boolean setPassword(String userName, String oldPassword, String newPassword) throws SecurityException; //NOPMD - name overload and exception intended
}