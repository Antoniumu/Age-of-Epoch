package joptsimple.internal;

import java.util.Collection;

public final class Strings {
    public static final String EMPTY = "";
    public static String join(Collection<String> pieces, String separator) {
        return pieces == null ? "" : String.join(separator, pieces);
    }
    public static String join(String[] pieces, String separator) {
        return pieces == null ? "" : String.join(separator, pieces);
    }
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
