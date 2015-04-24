package at.yawk.httpserver;

import java.util.logging.Logger;

/**
 * @author yawkat
 */
public final class Configuration {
    Logger logger = Logger.getLogger(HttpServer.class.getName());
    int readBufferSize = 4096;
    int writeBufferSize = 4096;

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public Configuration setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public Configuration setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
        return this;
    }

    public Logger getLogger() {
        return logger;
    }

    public Configuration setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }
}
