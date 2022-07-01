package io.fair_acc.acc.remote;

import java.io.IOException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.remote.MimeType;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class CipboardReceiverSample {
    public static final String ENDPOINT_STATUS_SSE = "http://localhost:8080/clipboard/status";
    public static final String ENDPOINT_STATUS_IMG = "http://localhost:8080/clipboard/status.png";
    private static final Logger LOGGER = LoggerFactory.getLogger(CipboardReceiverSample.class);
    private static final OkHttpClient okClient = new OkHttpClient();
    private static final EventSource.Factory factory = EventSources.createFactory(okClient);

    private static final EventSourceListener EVENT_SOURCE_LISTENER = new EventSourceListener() {
        @Override
        public void onOpen(EventSource eventSource, Response response) {
            LOGGER.atInfo().addArgument(eventSource).addArgument(response).log("[ES] onOpen - event from '{}' and response '{}'");
        }

        @Override
        public void onEvent(final EventSource eventSource, final String id, final String type, String data) {
            LOGGER.atInfo().addArgument(eventSource).addArgument(id).addArgument(type).addArgument(data).log("[ES] onEvent - event from '{}' id = {} type = {} data = {}");
        }

        @Override
        public void onFailure(EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
            LOGGER.atInfo().addArgument(eventSource).addArgument(response).addArgument(response == null ? " null" : response.body()).log("[ES] onFailure - event onFailure from '{}' response = {} - body = '{}'");
        }
    };

    public static byte[] getByteArrayOkHTTP(final String path, final MimeType mimeType, boolean useGSON) {
        Request request = new Request.Builder().url(path).get().addHeader("Accept", mimeType.toString()).build();

        try (Response response = okClient.newCall(request).execute()) {
            switch (mimeType) {
            case TEXT:
            case HTML:
            case JSON:
            case XML:
                //                MyBinaryData json;
                //                if (useGSON) {
                //                    Reader reader = response.body().charStream();
                //                    json = gson.fromJson(reader, MyBinaryData.class);
                //                } else {
                //                    json = JSON.parseObject(response.body().bytes(), MyBinaryData.class);
                //                }
                //                return json.binaryData;
            case UNKNOWN:
            case BINARY:
            default:
                return response.body() != null ? response.body().bytes() : new byte[0];
            }

        } catch (IOException e) {
            LOGGER.atError().setCause(e);
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static EventSource newEventSource(final String path) {
        Request request = new Request.Builder().url(path).addHeader("Accept", MimeType.EVENT_STREAM.toString()).build();
        LOGGER.atInfo().addArgument(path).addArgument(request).log("init sse to '{}' request = {}");
        return factory.newEventSource(request, EVENT_SOURCE_LISTENER);
    }

    public static void main(String[] args) throws InterruptedException {
        final EventSource sseSource = newEventSource(ENDPOINT_STATUS_IMG); // need to keep a strong reference due to the listener
        assert sseSource != null;
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(5000); // NOPMD
            LOGGER.atInfo().log("waiting for events");
            // poll data
            byte[] data = getByteArrayOkHTTP(ENDPOINT_STATUS_IMG, MimeType.JSON, false);
            if (data != null) {
                LOGGER.atInfo().addArgument(data.length).log("polled {} bytes");
            }
        }
    }
}
