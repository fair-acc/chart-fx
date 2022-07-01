package io.fair_acc.acc.remote.user;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.acc.remote.BasicRestRoles;
import io.fair_acc.acc.remote.RestServer;

import io.javalin.core.security.Role;

public class RestUserHandlerImpl implements RestUserHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestUserHandlerImpl.class);
    private static final String REST_USER_PASSWORD_STORE = "restUserPasswordStore";
    /**
     * the password file location is statically allocated so that it cannot (for
     * security reasons) be overwritting during run time
     */
    private static final String REST_USER_PASSWORD_FILE = getUserPasswordStore();

    private final Object usersLock = new Object();
    private List<RestUser> users = Collections.emptyList();

    /**
     * Authenticate the user by hashing the input password using the stored salt, 
     * then comparing the generated hashed password to the stored hashed password
     */
    @Override
    public boolean authenticate(@NotNull final String username, @NotNull final String password) {
        synchronized (usersLock) {
            final RestUser user = getUserByUsername(username);
            if (user == null) {
                return false;
            }
            final String hashedPassword = BCrypt.hashpw(password, user.salt);
            return hashedPassword.equals(user.hashedPassword);
        }
    }

    @Override
    public Iterable<String> getAllUserNames() {
        synchronized (usersLock) {
            if (users.isEmpty()) {
                readPasswordFile();
            }
            return users.stream().map(user -> user.userName).collect(Collectors.toList());
        }
    }

    @Override
    public RestUser getUserByUsername(final String userName) {
        synchronized (usersLock) {
            if (users.isEmpty()) {
                readPasswordFile();
            }
            return users.stream().filter(b -> b.userName.equals(userName)).findFirst().orElse(null);
        }
    }

    @Override
    public Set<Role> getUserRolesByUsername(final String userName) {
        synchronized (usersLock) {
            if (users.isEmpty()) {
                readPasswordFile();
            }
            Optional<RestUser> user = users.stream().filter(b -> b.userName.equals(userName)).findFirst();
            if (user.isPresent()) {
                return user.get().getRoles();
            }
            return Collections.singleton(BasicRestRoles.NULL);
        }
    }

    public void readPasswordFile() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("readPasswordFile called");
        }
        synchronized (usersLock) {
            try (BufferedReader br = REST_USER_PASSWORD_FILE == null ? new BufferedReader(new InputStreamReader(RestServer.class.getResourceAsStream("/DefaultRestUserPasswords.pwd"), StandardCharsets.UTF_8)) //
                                                                     : Files.newBufferedReader(Paths.get(new File(REST_USER_PASSWORD_FILE).getPath()), StandardCharsets.UTF_8)) {
                final List<RestUser> newUserList = new ArrayList<>(10);
                String userLine;
                int lineCount = 0;
                while ((userLine = br.readLine()) != null) { // NOPMD NOSONAR -- early return/continue on purpose
                    if (userLine.startsWith("#")) {
                        continue;
                    }
                    lineCount++;
                    parsePasswordLine(newUserList, userLine, lineCount);
                }
                users = Collections.unmodifiableList(newUserList);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().log("PasswordFile successfully read");
                }
            } catch (IOException e) {
                LOGGER.atError().setCause(e).addArgument(REST_USER_PASSWORD_FILE).log("could not read rest user passwords to '{}'");
            }
        }
    }

    @Override
    public boolean setPassword(@NotNull final String userName, @NotNull final String oldPassword, @NotNull final String newPassword) {
        if (REST_USER_PASSWORD_FILE == null) {
            LOGGER.atWarn().log("cannot set password for default user password store");
            return false;
        }
        synchronized (usersLock) {
            if (authenticate(userName, oldPassword)) {
                // N.B. default rounds is 2^10, increase this if necessary to harden passwords
                final String newSalt = BCrypt.gensalt();
                final String newHashedPassword = BCrypt.hashpw(newPassword, newSalt);
                final RestUser user = getUserByUsername(userName);
                if (user == null) {
                    return false;
                }
                user.salt = newSalt;
                user.hashedPassword = newHashedPassword;
                writePasswordFile();
                return true;
            }
            return false;
        }
    }

    public void writePasswordFile() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("updatePasswordFile called");
        }
        if (REST_USER_PASSWORD_FILE == null) {
            LOGGER.atWarn().log("cannot write password for default user password store");
            return;
        }
        synchronized (usersLock) {
            final File file = new File(REST_USER_PASSWORD_FILE);
            try {
                if (file.createNewFile()) {
                    LOGGER.atInfo().addArgument(REST_USER_PASSWORD_FILE).log("needed to create new password file '{}'");
                }
            } catch (SecurityException | IOException e) {
                LOGGER.atError().setCause(e).addArgument(REST_USER_PASSWORD_FILE).log("could not create user passwords file '{}'");
                return;
            }

            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file.getPath()), StandardCharsets.UTF_8)) {
                final StringBuilder builder = new StringBuilder();
                for (final RestUser user : users) {
                    builder.delete(0, builder.length()); // inits and re-uses builder
                    builder.append(user.userName).append(':').append(user.salt).append(':').append(user.hashedPassword).append(':');
                    // write roles
                    builder.append(BasicRestRoles.getRoles(user.getRoles())).append(':');
                    bw.write(builder.toString());
                    bw.newLine();
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().log("PasswordFile successfully updated");
                }
            } catch (IOException e) {
                LOGGER.atError().setCause(e).addArgument(REST_USER_PASSWORD_FILE).log("could not store rest user passwords to '{}'");
            }
        }
    }

    private void parsePasswordLine(final List<RestUser> newUserList, final String userLine, final int lineCount) {
        try {
            final String[] items = userLine.split(":");
            if (items.length < 4) { // NOPMD
                LOGGER.atWarn().addArgument(items.length).addArgument(lineCount).addArgument(userLine).log("insufficient arguments ({} < 4)- parsing line {}: '{}'");
                return;
            }
            newUserList.add(new RestUser(items[0], items[1], items[2], BasicRestRoles.getRoles(items[3]))); // NOPMD - needed
        } catch (Exception e) { // NOPMD - catch generic exception since a faulty login should not crash the rest of the REST service
            LOGGER.atWarn().setCause(e).addArgument(lineCount).addArgument(userLine).log("could not parse line {}: '{}'");
        }
    }

    private static String getUserPasswordStore() {
        final String passWordStore = System.getProperty(REST_USER_PASSWORD_STORE);
        if (passWordStore == null) {
            LOGGER.atWarn().log("using internal UserPasswordStore -- PLEASE CHANGE FOR PRODUCTION -- THIS IS UNSAFE PRACTICE");
        }
        return passWordStore;
    }
}
