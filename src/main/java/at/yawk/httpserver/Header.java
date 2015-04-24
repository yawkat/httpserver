package at.yawk.httpserver;

import java.util.Objects;

/**
 * @author yawkat
 */
public final class Header {
    final String key;
    final String value;

    public Header(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Header)) { return false; }
        Header header = (Header) o;
        return Objects.equals(key, header.key) &&
               Objects.equals(value, header.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
