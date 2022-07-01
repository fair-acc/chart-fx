package io.fair_acc.acc.remote.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.acc.remote.RestServer;
import io.fair_acc.dataset.remote.MimeType;

import io.javalin.http.Handler;

/**
 * Property handler class -- PROTOTYPE/DRAFT for discussion/evaluation
 * 
 * intention: 
 *  * wrapper around RESTful, CMW, other protocol backends to provide a consistent server-side API
 * 
 * @author rstein
 *
 * @param <G> GET/SUBSCRIBE data model -- a POJO definition
 * @param <S> SET/NOTIFY data model -- a POJO definition
 * @param <R> REQ/REP handler (contains connection details, back-end handler (default deprecated), method that are not needed for user-level server-side code, ...)
 */
public class Property<G, S, R> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Property.class);
    private static final String ACCEPT_HEADER = "Accept";
    private final String propertyName;
    private final String htmlModelGet;
    private final String htmlModelSet;

    public Property(@NotNull final String propertyName, //
            @NotNull final String htmlModelGet, @NotNull final Class<G> dataClassPrototypeGet, @NotNull final PropertyHandler userHandlerGet, // NOPMD NOSONAR need 'dataClassPrototypeGet handler in the future
            @NotNull final String htmlModelSet, @NotNull final Class<S> dataClassPrototypeSet, @NotNull final PropertyHandler userHandlerSet) { // NOPMD NOSONAR need 'dataClassPrototypeSet and userHandlerSet handler in the future
        this.propertyName = propertyName;
        this.htmlModelGet = htmlModelGet;
        this.htmlModelSet = htmlModelSet;
        // parse header and switch between different BINARY, JSON, HTML implementations
        // generate MAP -> call MAP handler
        // [..] etc.
        // parse user form data
        // N.B. convert here from String to binary Object types
        // if GET/SUBSCRIBE
        // launch binary serialiser
        // generate MAP -> call MAP handler as above
        // launch JSON serialiser
        // generate MAP -> call MAP handler as above
        final Handler handler = ctx -> {
            // parse header and switch between different BINARY, JSON, HTML implementations
            final String type = ctx.header(ACCEPT_HEADER);
            final String contentType = ctx.req.getContentType();
            LOGGER.atInfo().addArgument(propertyName).addArgument(type).addArgument(contentType).log("property {} handle type {} contentType {}");

            if (type == null || type.equalsIgnoreCase(MimeType.HTML.toString())) {
                final Map<String, Object> model = MessageBundle.baseModel(ctx);

                // generate MAP -> call MAP handler
                model.put("property", propertyName);
                model.put("selector", "FAIR.SELECTOR.ALL");
                // [..] etc.
                Map<String, Object> fromUser = new HashMap<>();
                Map<String, List<String>> formMap = ctx.formParamMap();
                for (Entry<String, List<String>> entry : formMap.entrySet()) {
                    // parse user form data
                    fromUser.put(entry.getKey(), entry.getValue());
                    // N.B. convert here from String to binary Object types
                }
                userHandlerGet.handle(fromUser, model);
                // if GET/SUBSCRIBE
                ctx.render(htmlModelGet, model);

            } else if (type.equalsIgnoreCase(MimeType.BINARY.toString())) { // NOPMD NOSONAR
                // TODO: launch binary serialiser
                // generate MAP -> call MAP handler as above
            } else if (type.equalsIgnoreCase(MimeType.JSON.toString())) { // NOPMD NOSONAR
                // TODO: launch JSON serialiser
                // generate MAP -> call MAP handler as above
            }
        };

        final CombinedHandler restHandler = new CombinedHandler(handler);
        RestServer.getInstance().get(propertyName, restHandler); // handles GET & SUBSCRIBE
        RestServer.getInstance().post(propertyName, restHandler); // handles SET
    }

    protected String getHtmlModelGet() {
        return htmlModelGet;
    }

    protected String getHtmlModelSet() {
        return htmlModelSet;
    }

    protected String getPropertyName() {
        return propertyName;
    }

    /*
     * Option 1: plain old Hash-Maps 
     * same handler for GET/SET/...
     * pro: many JSON/XML/Yaml/Binary serialiser support this
     * con: many get('key') calls/data copying)
     * 
     * options: maps could be converted/mapped to POJOs <-> performance penalty due to conversion
     * 
     */

    public interface ComHandler<H, D> {
        D getData();
        D getData(D myInitialReference);
        Supplier<D> getData(String selector);
        /*
         * open questions:
         * * allow sub-sets of data fields in <D>? 
         *   - check input completeness/format checks -> fail-safe fallback (drop? partial update (favours Maps)
         *   - interface for partial-SET (and GET?)? -> option of sub-hierarchies
         */
        H getHeader();
    }

    /*
     * Option 2
     * same or different (based on op) handler for GET/SET/...
     * pro: as for Option 1
     * con: as for Option 1
     */

    public enum Operation {
        GET,
        SET,
        SUBSCRIBE
    }

    public interface PropertyGetHandler<G, R> { // N.B. GET/SUBSCRIBE
        void handle(ComHandler<R, Void> requestFomUser, ComHandler<R, G> replyToUser) throws Exception; // NOPMD NOSONAR user may throw any exception
    }

    /*
     * Option 3
     * using POJO data and generic header objects
     * pro: fast w.r.t. serialisation, no server-side user code obfuscation (work with the same POJO for processing/transmitting)
     * con: (how to deal with partial/incomplete user data)
     */

    public interface PropertyHandler {
        void handle(Map<String, Object> fromUser, Map<String, Object> toUser) throws Exception; // NOPMD NOSONAR user may throw any exception
    }

    public interface PropertyHandlerAlt2 {
        void handle(Operation op, Map<String, Object> fromUser, Map<String, Object> toUser) throws Exception; // NOPMD NOSONAR user may throw any exception
    }

    public interface PropertyHandlerAlt3<S, G, R> { // N.B. GET/SET/NOTIFY
        void handle(ComHandler<R, S> requestFomUser, ComHandler<R, G> replyToUser) throws Exception; // NOPMD NOSONAR user may throw any exception
    }

    public interface PropertyHandlerAlt3Set<S, R> { // N.B. SET/NOTIFY
        void handle(ComHandler<R, S> requestFomUser, ComHandler<R, Void> replyToUser) throws Exception; // NOPMD NOSONAR user may throw any exception
    }

    public interface PropertyMap<K, V> extends Map<K, V> {
        Object getData();
    }
}
