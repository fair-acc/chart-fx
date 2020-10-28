package de.gsi.microservice.concepts.cmwlight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Obtain device info from the directory server
 */
public class DirectoryLightClient {
    public static final String GET_DEVICE_INFO = "get-device-info";
    public static final String GET_SERVER_INFO = "get-server-info";
    // private static final String SUPPORTED_CHARACTERS = "\\.\\-\\+_a-zA-Z0-9";
    // private static final String NAME_REGEX = "[a-zA-Z0-9][" + SUPPORTED_CHARACTERS + "]*";
    // private static final String CLIENT_INFO_SUPPORTED_CHARACTERS = "\\x20-\\x7E"; // ASCII := {32-126}
    static final String ERROR_STRING = "ERROR";

    private static final String HOST_PORT_SEPARATOR = ":";

    static final String NOT_BOUND_LOCATION = "*NOT_BOUND*";
    // static final String UNKNOWN_SERVER = "*UNKNOWN*";
    private static final String CLIENT_INFO = "UNKNOWN";
    private static final String VERSION = "2.0.0";
    private final String nameserver;
    private final int nameserverPort;

    public DirectoryLightClient(final String... nameservers) throws DirectoryClientException {
        if (nameservers.length != 1) {
            throw new DirectoryClientException("only one nameserver supported at the moment");
        }
        final String[] hostport = nameservers[0].split(HOST_PORT_SEPARATOR);
        if (hostport.length != 2) {
            throw new DirectoryClientException("nameserver address has wrong format: " + nameservers[0]);
        }
        nameserver = hostport[0];
        nameserverPort = Integer.parseInt(hostport[1]);
    }

    /**
     * Build the request message to query a number of devices
     *
     * @param devices The devices to query information for
     * @return The request message to send to the server
     **/
    private String getDeviceMsg(final List<String> devices) {
        final StringBuilder sb = new StringBuilder();
        sb.append(GET_DEVICE_INFO).append("\n");
        sb.append("@client-info ").append(CLIENT_INFO).append("\n");
        sb.append("@version ").append(VERSION).append("\n");
        // msg.append("@prefer-proxy\n");
        // msg.append("@direct ").append(this.properties.directServers.getValue()).append("\n");
        // msg.append("@domain ");
        // for (Domain domain : domains) {
        //     msg.append(domain.getName());
        //     msg.append(",");
        // }
        // msg.deleteCharAt(msg.length()-1);
        // msg.append("\n");
        for (final String dev : devices) {
            sb.append(dev).append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }

    // /**
    //  * Build the request message to query a number of servers
    //  *
    //  * @param servers The servers to query information for
    //  * @return The request message to send to the server
    //  **/
    // private String getServerMsg(final List<String> servers) {
    //     final StringBuilder sb = new StringBuilder();
    //     sb.append(GET_SERVER_INFO).append("\n");
    //     sb.append("@client-info ").append(CLIENT_INFO).append("\n");
    //     sb.append("@version ").append(VERSION).append("\n");
    //     // msg.append("@prefer-proxy\n");
    //     // msg.append("@direct ").append(this.properties.directServers.getValue()).append("\n");
    //     // msg.append("@domain ");
    //     // for (Domain domain : domains) {
    //     //     msg.append(domain.getName());
    //     //     msg.append(",");
    //     // }
    //     // msg.deleteCharAt(msg.length()-1);
    //     // msg.append("\n");
    //     for (final String dev : servers) {
    //         sb.append(dev).append('\n');
    //     }
    //     sb.append('\n');
    //     return sb.toString();
    // }

    /**
     * Query Server information for a given list of devices.
     *
     * @param devices The devices to query information for
     * @return a list of device information for the queried devices
     **/
    public List<Device> getDeviceInfo(final List<String> devices) throws DirectoryClientException {
        final ArrayList<Device> result = new ArrayList<>();
        try (final Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(nameserver, nameserverPort));
            final PrintWriter writer = new PrintWriter(socket.getOutputStream());
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer.write(getDeviceMsg(devices));
            writer.flush();
            // read query result, one line per requested device or ERROR followed by error message
            while (true) {
                final String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                if (line.equals(ERROR_STRING)) {
                    final String errorMsg = bufferedReader.lines().collect(Collectors.joining("\n")).strip();
                    throw new DirectoryClientException(errorMsg);
                }
                result.add(parseDeviceInfo(line));
            }
        } catch (IOException e) {
            throw new DirectoryClientException("Nameserver error: ", e);
        }
        return result;
    }

    private Device parseDeviceInfo(final String line) throws DirectoryClientException {
        String[] tokens = line.split(" ");
        if (tokens.length < 2) {
            throw new DirectoryClientException("Malformed reply line: " + line);
        }
        if (tokens[1].equals(NOT_BOUND_LOCATION)) {
            throw new DirectoryClientException("Requested device not bound: " + tokens[0]);
        }
        final ArrayList<Map<String, String>> servers = new ArrayList<>();
        for (int j = 2; j < tokens.length; j++) {
            final HashMap<String, String> server = new HashMap<>();
            servers.add(server);
            final String[] servertokens = tokens[j].split("#");
            server.put("protocol", servertokens[0]);
            int k = 1;
            while (k + 3 < servertokens.length) {
                if (servertokens[k + 1].equals("string")) {
                    final int length = Integer.parseInt(servertokens[k + 2]);
                    final String value = URLDecoder.decode(servertokens[k + 3], Charset.defaultCharset());
                    if (length == value.length()) {
                        server.put(servertokens[k], value);
                    } else {
                        throw new DirectoryClientException("Error parsing string: " + servertokens[k] + "(" + length + ") = " + value);
                    }
                    k += 4;
                } else if (servertokens[k + 1].equals("int") || servertokens[k + 1].equals("long")) {
                    server.put(servertokens[k], servertokens[k + 2]);
                    k += 3;
                } else {
                    throw new DirectoryClientException("Error parsing argument: " + k + ": " + Arrays.toString(servertokens));
                }
            }
        }
        return new Device(tokens[0], tokens[1], servers);
    }

    public static class Device {
        public final String name;
        private final String deviceClass;
        public final List<Map<String, String>> servers;

        public Device(final String name, final String deviceClass, final List<Map<String, String>> servers) {
            this.name = name;
            this.deviceClass = deviceClass;
            this.servers = servers;
        }

        @Override
        public String toString() {
            return "Device{"
                    + "name='" + name + '\'' + ", deviceClass='" + deviceClass + '\'' + ", servers=" + servers + '}';
        }
    }

    public static class DirectoryClientException extends Exception {
        public DirectoryClientException(final String errorMsg) {
            super(errorMsg);
        }
        public DirectoryClientException(final String errorMsg, final Exception cause) {
            super(errorMsg, cause);
        }
    }
}
