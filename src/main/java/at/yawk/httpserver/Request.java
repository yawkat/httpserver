package at.yawk.httpserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.*;

/**
 * @author yawkat
 */
public final class Request {
    final HttpServer server;
    final ByteChannel channel;

    private final Map<PropertyKey<?>, Object> properties = new HashMap<>();

    final List<Header> requestHeaders = new ArrayList<>();
    // queue here so we can remove sent headers
    final Queue<Header> responseHeaders = new ArrayDeque<>();

    final ByteBuffer readBuffer;
    final ByteBuffer writeBuffer;

    byte readState;
    byte writeState;

    String path;
    String method;
    String protocol;

    boolean writable;
    boolean finished;
    int statusCode;
    String statusMessage;

    boolean keepAlive = false;

    Request(HttpServer server, ByteChannel channel) {
        this.server = server;
        this.channel = channel;

        readBuffer = ByteBuffer.allocate(server.configuration.readBufferSize);
        writeBuffer = ByteBuffer.allocate(server.configuration.writeBufferSize);

        reset();
    }

    void reset() {
        properties.clear();
        requestHeaders.clear();
        responseHeaders.clear();
        readState = State.BEFORE_STATUS;
        writeState = State.BEFORE_STATUS;
        readBuffer.clear();
        writeBuffer.clear();
        writable = false;
        finished = false;
        statusCode = 200;
        statusMessage = "OK";
    }

    public ByteChannel getChannel() {
        return channel;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(PropertyKey<T> key) {
        return (T) properties.get(key);
    }

    public <T> void setProperty(PropertyKey<T> key, T value) {
        properties.put(key, value);
    }

    public List<Header> getRequestHeaders() {
        return requestHeaders;
    }

    public String getRequestHeader(String key) {
        for (Header header : requestHeaders) {
            if (header.key.equalsIgnoreCase(key)) {
                return header.value;
            }
        }
        return null;
    }

    public void addResponseHeader(String key, String value) {
        addResponseHeader(new Header(key, value));
    }

    public void addResponseHeader(Header header) {
        responseHeaders.add(header);
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void sendResponse() throws IOException {
        writable = true;
        server.doWrite(this);
    }

    public void finish() {
        finished = true;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    void closeFromError(String msg) throws IOException {
        server.configuration.logger.fine("Disconnecting client due to malformed request: " + msg);
        forceDisconnect();
    }

    void forceDisconnect() throws IOException {
        channel.close();
    }
}
