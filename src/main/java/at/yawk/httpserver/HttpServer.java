package at.yawk.httpserver;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author yawkat
 */
public final class HttpServer implements Closeable {
    final Configuration configuration;

    private final Selector selector;
    private final List<Interceptor> interceptors = new ArrayList<>(4);

    public HttpServer() throws IOException {
        this(new Configuration());
    }

    public HttpServer(Configuration configuration) throws IOException {
        this.configuration = configuration;
        this.interceptors.add(new HttpInterceptor(configuration));

        this.selector = Selector.open();
    }

    public void appendInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    public SocketAddress bind() throws IOException {
        return bind(0);
    }

    public SocketAddress bind(int port) throws IOException {
        return bind(new InetSocketAddress(port));
    }

    public SocketAddress bind(SocketAddress address) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        channel.bind(address);
        return channel.getLocalAddress();
    }

    public void loop() throws IOException, InterruptedException {
        Thread thread = Thread.currentThread();
        while (true) {
            selector.select();
            if (thread.isInterrupted()) {
                throw new InterruptedException();
            }
            if (!selector.isOpen()) {
                break;
            }
            afterSelect();
        }
    }

    public void tick() throws IOException {
        if (selector.selectNow() != 0) {
            afterSelect();
        }
    }

    private void afterSelect() throws IOException {
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            int ops = key.readyOps();

            if ((ops & SelectionKey.OP_ACCEPT) != 0) {
                ServerSocketChannel channel = (ServerSocketChannel) key.channel();

                SocketChannel accepted = channel.accept();
                accepted.configureBlocking(false);
                SelectionKey registeredKey =
                        accepted.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                Request request = new Request(this, accepted);
                registeredKey.attach(request);

                iterator.remove();
            } else { // OP_READ and/or OP_WRITE

                Request request = (Request) key.attachment();
                if ((ops & SelectionKey.OP_READ) != 0) {
                    doRead(request);
                }
                if ((ops & SelectionKey.OP_WRITE) != 0) {
                    doWrite(request);
                }
                iterator.remove();
            }
        }
    }

    private void doRead(Request request) throws IOException {
        for (Interceptor interceptor : interceptors) {
            if (!interceptor.read(request)) {
                break;
            }
        }
    }

    void doWrite(Request request) throws IOException {
        // check if we can write yet
        if (!request.writable) { return; }

        for (Interceptor interceptor : interceptors) {
            if (!interceptor.write(request)) {
                break;
            }
        }
    }

    public void close() throws IOException {
        selector.close();
    }

}
