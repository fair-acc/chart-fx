package io.fair_acc.acc.remote.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import io.fair_acc.acc.remote.RestServer;

import io.javalin.http.Context;

public class MessageBundle {
    private static final String ATTR_CURRENT_MESSAGES = "msg";
    private static final String ATTR_CURRENT_USER = "currentUser";
    private static final String ATTR_CURRENT_ROLES = "currentRoles";
    private final ResourceBundle messages;

    public MessageBundle(final String languageTag) {
        final Locale locale = languageTag == null ? Locale.ENGLISH : new Locale(languageTag);
        messages = ResourceBundle.getBundle("localisation/messages", locale);
    }

    public String get(final String message) {
        return messages.getString(message);
    }

    public final String get(final String key, final Object... args) {
        return MessageFormat.format(get(key), args);
    }

    public static Map<String, Object> baseModel(Context ctx) {
        final Map<String, Object> model = new HashMap<>();
        model.put(ATTR_CURRENT_MESSAGES, new MessageBundle(RestServer.getSessionLocale(ctx)));
        model.put(ATTR_CURRENT_USER, RestServer.getSessionCurrentUser(ctx));
        model.put(ATTR_CURRENT_ROLES, RestServer.getSessionCurrentRoles(ctx));
        return model;
    }
}
