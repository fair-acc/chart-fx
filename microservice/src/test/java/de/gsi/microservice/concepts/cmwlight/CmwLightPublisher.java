package de.gsi.microservice.concepts.cmwlight;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.zeromq.ZMQ;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import de.gsi.microservice.utils.WorkerThreadFactory;

/**
 * Sample implementation for subscription to multiple cmw servers and properties and publishing the
 * updates into a high performance ring buffer for further processing.
 */
public class CmwLightPublisher implements Runnable {
    private static final long TIMEOUT = 1000;
    private final Disruptor<CmwRbEntry> disruptor;
    private final Map<Integer, CmwLightClient> clients = new HashMap<>(); // poller index -> client
    private final ZMQ.Poller poller;
    private final ZMQ.Context context;
    private final RingBuffer<CmwRbEntry> rb;

    public CmwLightPublisher() {
        context = ZMQ.context(1);
        poller = context.poller();
        disruptor = new Disruptor<>(CmwRbEntry::new, 1024, new WorkerThreadFactory("cmw-disruptor"));
        rb = disruptor.getRingBuffer();
    }

    public void add(CmwLightClient client) {
        clients.put(poller.register(client.getSocket(), ZMQ.Poller.POLLIN), client);
    }

    @Override
    public void run() {
        disruptor.start();

        for (CmwLightClient client : clients.values()) {
            try {
                client.housekeeping(System.currentTimeMillis());
            } catch (CmwLightProtocol.RdaLightException e) {
                e.printStackTrace(); // todo: publish exception
            }
        }
        while (!Thread.interrupted()) {
            try {
                if (poller.poll(TIMEOUT) > 0) {
                    // get data from clients
                    for (Map.Entry<Integer, CmwLightClient> entry : clients.entrySet()) {
                        while (poller.pollin(entry.getKey())) {
                            final CmwLightClient client = entry.getValue();
                            final CmwLightMessage reply = client.receiveData();
                            if (reply != null && reply.requestType == CmwLightProtocol.RequestType.NOTIFICATION_DATA) {
                                rb.publishEvent((event, sequence, arg0) -> {
                                    event.type = "update";
                                    event.message = arg0;
                                    event.device = client.getAddress();
                                }, reply);
                            }
                            if (reply != null && (reply.requestType == CmwLightProtocol.RequestType.EXCEPTION || reply.requestType == CmwLightProtocol.RequestType.NOTIFICATION_EXC || reply.requestType == CmwLightProtocol.RequestType.SUBSCRIBE_EXCEPTION)) {
                                rb.publishEvent((event, sequence, arg0) -> {
                                    event.type = "exception";
                                    event.message = arg0;
                                    event.device = client.getAddress();
                                }, reply);
                            }
                        }
                    }
                } else {
                    // do housekeeping on clients
                    for (CmwLightClient client : clients.values()) {
                        client.housekeeping(System.currentTimeMillis());
                    }
                }
            } catch (CmwLightProtocol.RdaLightException e) {
                e.printStackTrace(); // todo: publish exception
            }
        }
    }

    private Disruptor<CmwRbEntry> getDisruptor() {
        return disruptor;
    }

    private ZMQ.Context getContext() {
        return context;
    }

    /**
     * Sample use of the publisher, subscribes to three different properties on two different devices.
     *
     * @param args the address and port of the cmw nameserver to use "[server]:[port]"
     * @throws DirectoryLightClient.DirectoryClientException
     */
    public static void main(String[] args) throws DirectoryLightClient.DirectoryClientException {
        final String nameserver = args[0]; // set cmw nameserver here
        final String device1 = "GSCD002";
        final String property = "AcquisitionDAQ";
        final String selector = "FAIR.SELECTOR.ALL";
        final String filter1a = "GS11MU2:Current_1@10Hz";
        final String filter1b = "GS11MU2:Voltage_1@10Hz";
        final String device2 = "GSCD005";
        final String filter2 = "GS02KQ1E:Current_1@10Hz";
        DirectoryLightClient directoryLightClient = new DirectoryLightClient(nameserver);
        final String address1 = directoryLightClient.getDeviceInfo(Collections.singletonList(device1)).get(0).servers.get(0).get("Address:");
        final String address2 = directoryLightClient.getDeviceInfo(Collections.singletonList(device2)).get(0).servers.get(0).get("Address:");
        CmwLightPublisher poller = new CmwLightPublisher();
        final CmwLightClient client1 = new CmwLightClient(address1, poller.getContext());
        client1.subscribe(device1, property + "fail", selector, Map.of("acquisitionModeFilter", 0, "channelNameFilter", filter1a));
        client1.subscribe(device1, property, selector, Map.of("acquisitionModeFilter", 0, "channelNameFilter", filter1b));
        poller.add(client1);
        final CmwLightClient client2 = new CmwLightClient(address2, poller.getContext());
        client2.subscribe(device2, property + "fail", selector, Map.of("acquisitionModeFilter", 0, "channelNameFilter", filter2));
        poller.add(client2);

        poller.getDisruptor().handleEventsWith((ev, seq, lastOfBatch) -> System.out.println(ev));

        poller.run();
    }

    private static class CmwRbEntry {
        public CmwLightMessage message;
        public String device;
        public String type;

        @Override
        public String toString() {
            return "CmwRbEntry{"
                    + "device='" + device + '\'' + ", type='" + type + '\'' + ", message=" + message + '}';
        }
    }
}
