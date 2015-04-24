package at.yawk.httpserver;

/**
 * @author yawkat
 */
@SuppressWarnings("unused") // we use the type parameter for type safety in the request
public final class PropertyKey<T> {
    private PropertyKey() {}

    public static <T> PropertyKey<T> create() {
        return new PropertyKey<>();
    }
}
