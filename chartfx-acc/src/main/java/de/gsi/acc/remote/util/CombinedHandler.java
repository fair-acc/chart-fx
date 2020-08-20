package de.gsi.acc.remote.util;

import java.io.IOException;
import java.util.function.Consumer;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.acc.remote.RestServer;
import de.gsi.dataset.remote.MimeType;

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.sse.SseClient;

/**
 * Combined GET and SSE request handler. 
 * 
 * N.B. This based on an original idea/implementation found in Javalin's {@link io.javalin.http.sse.SseHandler}.
 * 
 * @author rstein 
 * 
 * @see io.javalin.http.sse.SseHandler
 */
public class CombinedHandler implements Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombinedHandler.class);
    private final Handler getHandler;

    private final Consumer<SseClient> clientConsumer = client -> {
        final String endPointName = client.ctx.req.getRequestURI();
        RestServer.getEventClients(endPointName).add(client);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(client.ctx.req.getRemoteHost()).addArgument(endPointName).log("added SSE client: '{}' to route '{}'");
        }
        client.sendEvent("connected", "Hello, new SSE client " + client.ctx.req.getRemoteHost());

        client.onClose(() -> {
            RestServer.getEventClients(endPointName).remove(client);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(client.ctx.req.getRemoteHost()).addArgument(endPointName).log("removed client: '{}' from route '{}'");
            }
        });
    };

    public CombinedHandler(@NotNull Handler getHandler) {
        this.getHandler = getHandler;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        //if (MimeType.EVENT_STREAM.toString().equals(ctx.header(Header.ACCEPT))) {
        if (MimeType.EVENT_STREAM.equals(RestServer.getRequestedMimeProtocol(ctx))) {
            ctx.res.setStatus(200);
            ctx.res.setCharacterEncoding("UTF-8");
            ctx.res.setContentType(MimeType.EVENT_STREAM.toString());
            ctx.res.addHeader(Header.CONNECTION, "close");
            ctx.res.addHeader(Header.CACHE_CONTROL, "no-cache");
            ctx.res.flushBuffer();

            ctx.req.startAsync(ctx.req, ctx.res);
            ctx.req.getAsyncContext().setTimeout(0);
            clientConsumer.accept(new SseClient(ctx));

            ctx.req.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException { /* not needed */
                }
                @Override
                public void onError(AsyncEvent event) throws IOException {
                    event.getAsyncContext().complete();
                }
                @Override
                public void onStartAsync(AsyncEvent event) throws IOException { /* not needed */
                }
                @Override
                public void onTimeout(AsyncEvent event) throws IOException {
                    event.getAsyncContext().complete();
                }
            });
            return;
        }

        getHandler.handle(ctx);
    }
}