package de.gsi.microservice.concepts;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

public class RestBehindRouterEvaluation {
    private static final byte[] ZERO_MQ_HEADER = { -1, 0, 0, 0, 0, 0, 0, 0, 7, 127 };
    public static void main(final String[] argv) {
        try (ZContext context = new ZContext()) {
            Socket tcpProxy = context.createSocket(SocketType.ROUTER);
            tcpProxy.setRouterRaw(true);
            Socket router = context.createSocket(SocketType.ROUTER);
            Socket stream = context.createSocket(SocketType.STREAM);

            if (!tcpProxy.bind("tcp://*:8080")) {
                throw new IllegalStateException("could not bind socket");
            }
            if (!router.bind("tcp://*:8081")) {
                throw new IllegalStateException("could not bind socket");
            }
            if (!stream.bind("tcp://*:8082")) {
                throw new IllegalStateException("could not bind socket");
            }

            final TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Socket dealer = context.createSocket(SocketType.DEALER);
                    dealer.setIdentity("dealer".getBytes(zmq.ZMQ.CHARSET));

                    System.err.println("clients sends request");
                    dealer.connect("tcp://localhost:8080");
                    dealer.send("Hello World");
                }
            };
            new Timer().schedule(timerTask, 2000);

            Poller poller = context.createPoller(2);
            poller.register(tcpProxy, Poller.POLLIN);
            poller.register(stream, Poller.POLLIN);

            while (!Thread.interrupted()) {
                if (poller.poll(1000) == -1) {
                    break; //  Interrupted
                }

                if (poller.pollin(0)) {
                    handleRouterSocket(tcpProxy);
                }
                if (poller.pollin(1)) {
                    handleStreamHttpSocket(stream, null);
                }
            }
        }
    }

    private static long bytesToLong(byte[] bytes) {
        long value = 0;
        for (final byte aByte : bytes) {
            value = (value << 8) + (aByte & 0xff);
        }
        return value;
    }

    private static ZFrame getConnectionID(final Socket socket) {
        // TODO: add further safe-guards if called for a socket with no data pending

        // Get [id, ] message on client connection.
        final ZFrame handle = ZFrame.recvFrame(socket);
        if (handle == null || bytesToLong(handle.getData()) == 0) {
            return null;
        }

        System.err.println("received ID = " + handle.toString()); //  Professional Logging(TM)
        if (!handle.hasMore()) {
            //  Close erroneous connection to browser
            handle.send(socket, ZFrame.MORE | ZFrame.REUSE);
            socket.send((byte[]) null, 0);
            return null;
        }

        // receive empty payload.
        final ZFrame emptyFrame = ZFrame.recvFrame(socket);
        if (emptyFrame == null || !emptyFrame.hasMore() || emptyFrame.size() == 0) {
            // received null frame
            //System.err.println("nothing received");
            return handle;
        }
        if (emptyFrame.hasMore() || emptyFrame.size() != 0) {
            System.err.println("did receive more " + emptyFrame);
            //  Close erroneous connection to browser
            //            handle.send(socket, ZFrame.MORE | ZFrame.REUSE);
            //            socket.send((byte[]) null, 0);
            return null;
        }

        return handle;
    }

    private static ZFrame getRequest(final Socket socket) {
        // TODO: add further safe-guards if called for a socket with no data pending

        // Get [id, ] message on client connection.
        final ZFrame handle = ZFrame.recvFrame(socket);
        if (handle == null || bytesToLong(handle.getData()) == 0) {
            return null;
        }

        System.err.println("received Request ID = " + handle.toString() + " - more = " + handle.hasMore()); //  Professional Logging(TM)
        if (!handle.hasMore()) {
            //  Close erroneous connection to browser
            handle.send(socket, ZFrame.MORE | ZFrame.REUSE);
            socket.send((byte[]) null, 0);
            return null;
        }

        // receive request
        return ZFrame.recvFrame(socket);
    }

    private static void handleRouterSocket(final Socket router) {
        System.err.println("### called handleRouterSocket");
        // Get [id, ] message on client connection.
        ZFrame handle;
        if ((handle = getConnectionID(router)) == null) {
            // did not receive proper [ID, null msg] frames
            return;
        }

        final ZFrame request = getRequest(router);
        if (request == null) {
            return;
        }
        if (Arrays.equals(ZERO_MQ_HEADER, request.getData())) {
            router.sendMore(handle.getData());
            router.send(ZERO_MQ_HEADER);
            System.err.println("received ZeroMQ message more = " + request.hasMore());

        } else {
            System.err.println("received other (HTTP) message");
            System.err.println("received request = " + request + " more? " + request.hasMore());
            return;
        }

        //        handleStreamHttpSocket(router, handle);
        // received router request
        while (request.hasMore()) {
            // receive message
            final byte[] message = router.recv(0);
            assert message != null;
            final boolean more = router.hasReceiveMore();
            System.err.println("router msg (" + (more ? "more" : "all ") + "): " + Arrays.toString(message) + "\n      - string: '" + new String(message) + "'");

            //handleStreamHttpSocket(router);
            // Broker it -- throws an exception (too naive implementation?)
            //stream.send(message, more ? ZMQ.SNDMORE : 0);
            if (!more) {
                break;
            }
        }
    }

    private static void handleStreamHttpSocket(Socket httpSocket, ZFrame handlerExt) {
        // Get [id, ] message on client connection.
        ZFrame handler = handlerExt;
        if (handler == null && (handler = getConnectionID(httpSocket)) == null) {
            // did not receive proper [ID, null msg] frames
            return;
        }

        // Get [id, playload] message.
        final ZFrame clientRequest = ZFrame.recvFrame(httpSocket);
        if (clientRequest == null || bytesToLong(clientRequest.getData()) == 0) {
            return;
        }
        if (!Arrays.equals(handler.getData(), clientRequest.getData())) {
            // header ID mismatch
            return;
        }
        if (!clientRequest.hasMore()) {
            //  Close erroneous connection to browser
            clientRequest.send(httpSocket, ZFrame.MORE | ZFrame.REUSE);
            httpSocket.send((byte[]) null, 0);
            return;
        }

        // receive playload message.
        ZFrame request = ZFrame.recvFrame(httpSocket);
        assert request != null;
        String header = new String(request.getData(), 0, request.size(),
                StandardCharsets.UTF_8);
        System.err.println("received client request header : '" + header); //  Professional Logging(TM)

        //  Send Hello World response
        final String URI = (header.length() == 0) ? "null" : header.split("\n")[0];
        clientRequest.send(httpSocket, ZFrame.MORE | ZFrame.REUSE);
        httpSocket.send("HTTP/1.0 200 OK\r\nContent-Type: text/plain\r\n\r\nHello, World!\nyou requested URI: " + URI);

        //  Close connection to browser -- normally exit
        clientRequest.send(httpSocket, ZFrame.MORE | ZFrame.REUSE);
        httpSocket.send((byte[]) null, 0);
    }
}
