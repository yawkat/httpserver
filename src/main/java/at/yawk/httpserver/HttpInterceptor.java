package at.yawk.httpserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author yawkat
 */
final class HttpInterceptor implements Interceptor {
    private static final Charset CHARSET = StandardCharsets.US_ASCII;

    private final CharBuffer charBuffer;
    private final CharsetDecoder decoder = CHARSET.newDecoder();
    private final CharsetEncoder encoder = CHARSET.newEncoder();

    HttpInterceptor(Configuration configuration) {
        charBuffer = CharBuffer.allocate(Math.max(configuration.readBufferSize,
                                                  configuration.writeBufferSize));
    }

    @Override
    public boolean read(Request request) throws IOException {
        // in body, don't do anything
        if (request.readState >= State.IN_BODY) { return true; }

        while (true) {
            ByteBuffer rb = request.readBuffer;

            int read = request.channel.read(rb);
            if (read == 0) { // no more data on this pass
                break;
            }

            if (read == -1) {
                // interrupt
                request.closeFromError("EOF in headers");
                return false;
            }

            rb.flip();

            int lineStart = 0;
            boolean inCrLf = false;

            parseLoop:
            while (true) {
                // try to find a CrLf
                while (rb.position() < rb.limit()) {
                    byte b = rb.get();

                    if (!inCrLf || b != '\n') {
                        inCrLf = (b == '\r');
                        continue;
                    }

                    // found CrLf
                    int oldPosition = rb.position();

                    // check for empty line
                    if (lineStart == oldPosition - 2) {
                        if (request.readState == State.BEFORE_STATUS) {
                            // no status received, invalid request
                            request.closeFromError("No status received");
                        }
                        // headers end
                        request.readState = State.IN_BODY;
                        return true;
                    }

                    /////// DECODE LINE ///////

                    int oldLimit = rb.limit();

                    rb.position(lineStart);
                    rb.limit(oldPosition - 2);

                    decoder.decode(rb, charBuffer, true);
                    charBuffer.flip();
                    decoder.reset();
                    String line = charBuffer.toString();
                    charBuffer.clear();

                    rb.limit(oldLimit);
                    rb.position(oldPosition);
                    lineStart = oldPosition;

                    /////// PROCESS LINE ///////

                    if (request.readState == State.BEFORE_STATUS) {
                        int methodSeparatorIndex = line.indexOf(' ');
                        int pathSeparatorIndex = line.lastIndexOf(' ');

                        if (methodSeparatorIndex == -1 ||
                            methodSeparatorIndex == pathSeparatorIndex) {
                            // no two spaces in status line
                            request.closeFromError("Invalid status line");
                            return false;
                        }

                        request.method = line.substring(0, methodSeparatorIndex);
                        request.path = line.substring(methodSeparatorIndex + 1, pathSeparatorIndex);
                        request.protocol = line.substring(pathSeparatorIndex + 1);

                        request.readState = State.IN_HEADERS;
                    } else {
                        int separatorIndex = line.indexOf(": ");
                        if (separatorIndex == -1) {
                            // no ': ' in header
                            request.closeFromError("Invalid header line");
                            return false;
                        }

                        request.requestHeaders.add(new Header(
                                line.substring(0, separatorIndex),
                                line.substring(separatorIndex + 2)
                        ));
                    }

                    // try parsing another line
                    continue parseLoop;
                }

                // we need more data, can't read more lines from the same buf
                break;
            }
            if (inCrLf) {
                // move one back so we read the lf again
                rb.position(rb.position() - 1);
            }

            rb.compact();

            // even after removing all read data we are at the buffer cap, we won't be able
            // to read more data
            if (rb.limit() == rb.capacity()) {
                request.closeFromError("Read buffer too small for header line");
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean write(Request request) throws IOException {
        while (true) {
            // can't write all output, wait for another opportunity
            if (!flushWriteBuffer(request)) { return false; }

            if (request.finished) {
                // all data written, we're done!
                if (request.keepAlive) {
                    request.reset();
                } else {
                    request.forceDisconnect();
                }
                return false;
            }

            // headers done
            if (request.writeState >= State.IN_BODY) { return true; }

            if (request.writeState == State.BEFORE_STATUS) {
                charBuffer.append(request.protocol);
                charBuffer.append(' ');
                charBuffer.append(Integer.toString(request.statusCode));
                charBuffer.append(' ');
                charBuffer.append(request.statusMessage);

                request.writeState = State.IN_HEADERS;
            } else { // IN_HEADERS
                Header nextHeader = request.responseHeaders.poll();
                if (nextHeader == null) { // headers done
                    request.writeState = State.IN_BODY;
                    // we're still writing one CrLf below
                } else {
                    charBuffer.append(nextHeader.key);
                    charBuffer.append(nextHeader.value);
                }
            }
            charBuffer.append("\r\n");
            charBuffer.flip();
            encoder.encode(charBuffer, request.writeBuffer, true);
            charBuffer.clear();
        }
    }

    /**
     * @return true if the write buffer was emptied, false otherwise
     */
    private boolean flushWriteBuffer(Request request) throws IOException {
        ByteBuffer wb = request.writeBuffer;
        wb.flip();
        while (wb.position() != wb.limit()) { // we still got stuff to write
            int written = request.channel.write(wb);
            if (written == -1) {
                request.closeFromError("EOF during header write");
                return false;
            }
            if (written == 0) { // couldn't write everything
                wb.compact();
                return false;
            }
        }
        wb.clear();
        return true;
    }
}
