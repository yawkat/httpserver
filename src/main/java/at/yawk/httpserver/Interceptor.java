package at.yawk.httpserver;

import java.io.IOException;

/**
 * @author yawkat
 */
public interface Interceptor {
    boolean read(Request request) throws IOException;

    boolean write(Request request) throws IOException;
}
