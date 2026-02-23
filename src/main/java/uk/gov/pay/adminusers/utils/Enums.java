package uk.gov.pay.adminusers.utils;

/**
 * Looks up enum constants the forgiving way the API always has.
 */
public final class Enums {

    private Enums() {
    }

    /**
     * @return the constant whose name matches the value once blanks are removed, {@code -} and {@code .} are read as
     * {@code _} and case is ignored, else the constant whose {@code toString()} matches the value ignoring case,
     * else {@code null}
     */
    public static Enum<?> fromStringFuzzy(String value, Enum<?>[] constants) {
        String text = value
                .replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .replace('-', '_')
                .replace('.', '_');
        for (Enum<?> constant : constants) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        for (Enum<?> constant : constants) {
            if (constant.toString().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        return null;
    }
}
