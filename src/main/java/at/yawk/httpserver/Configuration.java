package at.yawk.httpserver;

/**
 * @author yawkat
 */
public final class Configuration {
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
}
