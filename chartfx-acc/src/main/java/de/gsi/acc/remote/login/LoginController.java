package de.gsi.acc.remote.login;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.acc.remote.BasicRestRoles;
import de.gsi.acc.remote.RestServer;
import de.gsi.acc.remote.user.RestUserHandler;
import de.gsi.acc.remote.util.MessageBundle;

import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;

@SuppressWarnings("PMD.FieldNamingConventions")
public class LoginController { // NOPMD - nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private static final String DEFAULT_USER = "anonymous";
    private static final String ENDPOINT_LOGIN = "/login";
    private static final String ENDPOINT_LOGOUT = "/logout";
    private static final String ENDPOINT_CHANGE_PASSWORD = "/changepassword";

    private static final String ATTR_LOCALE = "locale";
    private static final String ATTR_CURRENT_USER = "currentUser";
    private static final String ATTR_CURRENT_ROLES = "currentRoles";
    private static final String ATTR_LOGIN_REDIRECT = "loginRedirect";
    private static final String ATTR_LOGGED_OUT = "loggedOut";

    private static final String QUERY_PASSWORD = "password";
    private static final String QUERY_PASSWORD_NEW1 = "passwordNew1";
    private static final String QUERY_PASSWORD_NEW2 = "passwordNew2";
    private static final String QUERY_USERNAME = "username";

    private static final String AUTHENTICATION_SUCCEEDED = "authenticationSucceeded";
    private static final String AUTHENTICATION_FAILED = "authenticationFailed";
    private static final String AUTHENTICATION_PASSWORD_MISMATCH = "authenticationFailedPasswordsMismatch";

    private static final String TEMPLATE_LOGIN = "/velocity/login/login.vm";
    private static final String TEMPLATE_PASSWORD_CHANGE = "/velocity/login/changePassword.vm";

    /**
     * Locale change can be initiated from any page The locale is extracted from the
     * request and saved to the user's session
     */
    private static final Handler handleLocaleChange = ctx -> {
        if (ctx.queryParam(ATTR_LOCALE) != null) {
            ctx.sessionAttribute(ATTR_LOCALE, ctx.queryParam(ATTR_LOCALE));
            ctx.redirect(ctx.path());
        }
    };

    @OpenApi(
            description = "endpoint to receive password login request",
            operationId = "handleLoginPost",
            summary = "POST login command",
            tags = { "LoginController" },
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
                ,
                        @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/json"))
            })
    private static final Handler handleLoginPost
            = ctx -> {
        final Map<String, Object> model = MessageBundle.baseModel(ctx);
        final RestUserHandler userHandler = RestServer.getUserHandler();

        final String userName = ctx.formParam(QUERY_USERNAME);
        if (userHandler.authenticate(userName, ctx.formParam(QUERY_PASSWORD))) {
            ctx.sessionAttribute(ATTR_CURRENT_USER, userName);
            ctx.sessionAttribute(ATTR_CURRENT_ROLES, userHandler.getUserRolesByUsername(userName));

            model.put(AUTHENTICATION_SUCCEEDED, true);
            model.put(ATTR_CURRENT_USER, userName);
            model.put(ATTR_CURRENT_ROLES, userHandler.getUserRolesByUsername(userName));

            final String loginRedirect = ctx.sessionAttribute(ATTR_LOGIN_REDIRECT);
            if (loginRedirect != null) {
                ctx.redirect(loginRedirect);
            }
            ctx.render(TEMPLATE_LOGIN, model);
        } else {
            model.put(AUTHENTICATION_FAILED, true);
            ctx.render(TEMPLATE_LOGIN, model);
        }
    };

    @OpenApi(
            description = "endpoint to receive password changes",
            operationId = "handleChangePasswordPost",
            summary = "POST password change page",
            tags = { "LoginController" },
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
            })
    private static final Handler handleChangePasswordPost
            = ctx -> {
        final Map<String, Object> model = MessageBundle.baseModel(ctx);

        final String userName = ctx.formParam(QUERY_USERNAME);
        final String password1 = ctx.formParam(QUERY_PASSWORD_NEW1);
        final String password2 = ctx.formParam(QUERY_PASSWORD_NEW2);
        if (userName == null || password1 == null || password2 == null) {
            model.put(AUTHENTICATION_FAILED, true);
            ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
            return;
        }

        if (!checkPasswordCriteria(password1) || !checkPasswordCriteria(password2) || !password1.equals(password2)) {
            LOGGER.atWarn().addArgument(userName).log("password do not match for user '{}'");
            model.put(AUTHENTICATION_PASSWORD_MISMATCH, true);
            ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
            return;
        }
        model.put(AUTHENTICATION_PASSWORD_MISMATCH, false);

        try {
            final String password = ctx.formParam(QUERY_PASSWORD);
            if (password == null) {
                model.put(AUTHENTICATION_FAILED, true);
                ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
                return;
            }

            final RestUserHandler userHandler = RestServer.getUserHandler();
            if (userHandler.setPassword(userName, password, password1)) {
                ctx.sessionAttribute(ATTR_CURRENT_USER, userName);
                ctx.sessionAttribute(ATTR_CURRENT_ROLES, userHandler.getUserRolesByUsername(userName));

                model.put(AUTHENTICATION_SUCCEEDED, true);
                model.put(ATTR_CURRENT_USER, userName);
                model.put(ATTR_CURRENT_ROLES, userHandler.getUserRolesByUsername(userName));

                ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
                return;
            }

            model.put(AUTHENTICATION_FAILED, true);
            ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
        } catch (final SecurityException e) {
            LOGGER.atWarn().setCause(e).addArgument(userName).log("may not change password for user '{}'");
        }
        model.put(AUTHENTICATION_FAILED, true);
        ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
    };

    @OpenApi(
            description = "endpoint to receive password logout request",
            operationId = "handleLogoutPost",
            summary = "POST logout command",
            tags = { "LoginController" },
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
                ,
                        @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/json"))
            })
    private static final Handler handleLogoutPost
            = ctx -> {
        ctx.sessionAttribute(ATTR_CURRENT_USER, null);
        ctx.sessionAttribute(ATTR_CURRENT_ROLES, null);
        ctx.sessionAttribute(ATTR_LOGGED_OUT, "true");
        ctx.redirect(ENDPOINT_LOGIN);
    };

    @OpenApi(
            description = "endpoint to serve login page",
            operationId = "serveLoginPage",
            summary = "GET serve login page (HTML-only)",
            tags = { "LoginController" },

            //            method = HttpMethod.GET,
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
            })
    private static final Handler serveLoginPage
            = ctx -> {
        final Map<String, Object> model = MessageBundle.baseModel(ctx);
        model.put(ATTR_LOGGED_OUT, removeSessionAttrLoggedOut(ctx));
        ctx.render(TEMPLATE_LOGIN, model);
    };

    @OpenApi(
            description = "endpoint to serve password change page",
            operationId = "servePasswordChangePage",
            summary = "GET serve password change page (HTML-only)",
            tags = { "LoginController" },
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
            })
    private static final Handler servePasswordChangePage
            = ctx -> {
        final Map<String, Object> model = MessageBundle.baseModel(ctx);
        model.put(ATTR_LOGGED_OUT, removeSessionAttrLoggedOut(ctx));
        ctx.render(TEMPLATE_PASSWORD_CHANGE, model);
    };

    /**
     * The origin of the request (request.pathInfo()) is saved in the session so the
     * user can be redirected back after login
     */
    public static final AccessManager accessManager = (handler, ctx, permittedRoles) -> {
        final Set<Role> userRoles = LoginController.getSessionCurrentRoles(ctx);
        final Set<Role> intersection = new HashSet<>(permittedRoles);
        intersection.retainAll(userRoles);
        if (permittedRoles.isEmpty() || permittedRoles.contains(BasicRestRoles.ANYONE) || !intersection.isEmpty()) {
            handler.handle(ctx);
        } else {
            LOGGER.atWarn().addArgument(ctx.path()).addArgument(permittedRoles).addArgument(intersection) //
                    .log("could not log into '{}' permitted roles {} vs. have {}");

            // try to login
            if (ctx.sessionAttribute(ATTR_CURRENT_USER) == null) {
                ctx.sessionAttribute(ATTR_LOGIN_REDIRECT, ctx.path());
                ctx.redirect(ENDPOINT_LOGIN);
            } else {
                ctx.status(401).result("Unauthorized");
            }
        }
    };

    private LoginController() {
        // primarily static helper class
    }

    public static Set<Role> getSessionCurrentRoles(final Context ctx) {
        Object val = ctx.sessionAttribute(ATTR_CURRENT_ROLES);
        if (val == null) {
            // second attempt mapping to DEFAULT_USER roles
            val = RestServer.getUserHandler().getUserRolesByUsername(DEFAULT_USER);
        }
        if (!(val instanceof Set)) {
            return Collections.singleton(BasicRestRoles.NULL);
        }
        try {
            @SuppressWarnings("unchecked")
            final Set<Role> roles = (Set<Role>) val;
            return roles;
        } catch (final ClassCastException e) {
            LOGGER.atError().setCause(e).addArgument(ATTR_CURRENT_ROLES).log("could not cast '{}' attribute to Set<Role> -- something fishy is going on");
        }

        return Collections.singleton(BasicRestRoles.NULL);
    }

    public static String getSessionCurrentUser(final Context ctx) {
        return ctx.sessionAttribute(ATTR_CURRENT_USER);
    }

    public static String getSessionLocale(final Context ctx) {
        return ctx.sessionAttribute(ATTR_LOCALE);
    }

    /**
     * registers the login/logout and locale change listener
     */
    public static void register() {
        RestServer.getInstance().config.accessManager(accessManager);
        RestServer.getInstance().routes(() -> {
            // before(handleLoginPost)
            before(handleLocaleChange);
            post(ENDPOINT_LOGIN, handleLoginPost);
            post(ENDPOINT_LOGOUT, handleLogoutPost);
            post(ENDPOINT_CHANGE_PASSWORD, handleChangePasswordPost);
            get(ENDPOINT_LOGIN, serveLoginPage);
            get(ENDPOINT_CHANGE_PASSWORD, servePasswordChangePage);
        });
    }

    private static boolean checkPasswordCriteria(final String password) {
        //TODO: add better password rules
        // goal: higher entropy and favour larger number of characters
        // rather than complex special characters and/or number combinations
        // see security recommendations at: https://xkcd.com/936/
        return password != null && password.length() >= 8;
    }

    private static boolean removeSessionAttrLoggedOut(final Context ctx) {
        final String loggedOut = ctx.sessionAttribute(ATTR_LOGGED_OUT);
        ctx.sessionAttribute(ATTR_LOGGED_OUT, null);
        return loggedOut != null;
    }
}
