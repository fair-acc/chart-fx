package de.gsi.acc.remote;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.alibaba.fastjson.JSON;
import de.gsi.dataset.remote.MimeType;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class CipboardReceiverSample {
    //public static final String ENDPOINT_STATUS_IMG = "https://localhost:8443/clipboard/status.png";
    public static final String ENDPOINT_STATUS_SSE = "http://localhost:8080/clipboard/status";
    public static final String ENDPOINT_STATUS_IMG = "http://localhost:8080/clipboard/status.png";
    private static final Logger LOGGER = LoggerFactory.getLogger(CipboardReceiverSample.class);
    private static OkHttpClient okClient = new OkHttpClient();
    private static EventSource.Factory factory = EventSources.createFactory(okClient);
    //private final EventSource sseSource = newEventSource("https://localhost:8443/clipboard/status.png");
    private EventSource sseSource;

    private static final EventSourceListener EVENT_SOURCE_LISTENER = new EventSourceListener() {
        private final BlockingQueue<Object> events = new LinkedBlockingDeque<>();

        @Override
        public void onOpen(EventSource eventSource, Response response) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().log("[ES] onOpen");
            }
            System.err.println("event on  Open from " + eventSource + "\nresponse = " + response);
        }

        @Override
        public void onEvent(final EventSource eventSource, final String id, final String type, String data) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().log("[ES] onEvent");
                System.err.println("event from " + eventSource + " id = " + id + " type = " + type + " data = " + data);
            }
            //events.add(new Event(id, type, data));
        }

        @Override
        public void onFailure(EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().log("[ES] onFailure");
            }
            System.err.println("event onFailure from " + eventSource + "\nresponse = " + response);
            final ResponseBody body = response.body();
            try {
                throw new RuntimeException("onFailure response.body " + body.contentType() + " body content = " + body.string());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    };

    public CipboardReceiverSample() {
    }

    public void connectSse() {
        sseSource = newEventSource(ENDPOINT_STATUS_IMG);
    }

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
                return response.body().bytes();
            }

        } catch (IOException e) {
            LOGGER.atError().setCause(e);
            e.printStackTrace();
        }

        return new byte[0];
    }

    private static EventSource newEventSource(String path) {
        Request request = new Request.Builder().url(path).addHeader("Accept", MimeType.EVENT_STREAM.toString()).build();
        System.err.println("init sse to " + path + " request = " + request);
        return factory.newEventSource(request, EVENT_SOURCE_LISTENER);
    }

    public static void main(String[] args) throws InterruptedException {
        CipboardReceiverSample sample = new CipboardReceiverSample();
        sample.connectSse();

        while (true) {
            Thread.sleep(5000);
            LOGGER.atInfo().log("waiting for events");
            // poll data
            byte[] data = getByteArrayOkHTTP(ENDPOINT_STATUS_IMG, MimeType.JSON, false);
            if (data != null) {
                LOGGER.atInfo().addArgument(data.length).log("polled {} bytes");
            }
        }
    }
}
