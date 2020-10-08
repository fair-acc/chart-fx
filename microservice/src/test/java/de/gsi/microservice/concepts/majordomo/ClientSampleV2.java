package de.gsi.microservice.concepts.majordomo;

import java.nio.charset.StandardCharsets;

import org.zeromq.ZMsg;

/**
 * Majordomo Protocol client example, asynchronous. Uses the mdcli API to hide
 * all MajordomoProtocol aspects
 */

public class ClientSampleV2 {
    private static final int N_SAMPLES = 1_000_000;
    public static void main(String[] args) {
        MajordomoClientV2 clientSession = new MajordomoClientV2("tcp://localhost:5555");
        final byte[] serviceBytes = "mmi.echo".getBytes(StandardCharsets.UTF_8);
        // final byte[] serviceBytes = "inproc.echo".getBytes(StandardCharsets.UTF_8);
        // final byte[] serviceBytes = "echo".getBytes(StandardCharsets.UTF_8);

        int count;
        long start = System.currentTimeMillis();
        for (count = 0; count < N_SAMPLES; count++) {
            final String requestMsg = "Hello world - async - " + count;
            clientSession.send(serviceBytes, requestMsg.getBytes(StandardCharsets.UTF_8));
        }
        long mark1 = System.currentTimeMillis();
        double diff1 = 1e-3 * (mark1 - start);
        System.err.printf("%d requests processed in %d ms -> %f op/s\n", count, mark1 - start, count / diff1);

        for (count = 0; count < N_SAMPLES; count++) {
            ZMsg reply = clientSession.recv();
            if (count < 10 || count % 100_000 == 0 || count >= (N_SAMPLES - 10)) {
                System.err.println("client iteration = " + count + " - received: " + reply);
            }
            if (reply != null) {
                reply.destroy();
            } else {
                break; // Interrupt or failure
            }
        }
        long mark2 = System.currentTimeMillis();
        double diff2 = 1e-3 * (mark2 - start);
        System.err.printf("%d requests/replies processed in %d ms -> %f op/s\n", count, mark2 - start, count / diff2);
        clientSession.destroy();
    }
}
