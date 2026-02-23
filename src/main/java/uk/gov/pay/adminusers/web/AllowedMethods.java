package uk.gov.pay.adminusers.web;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The methods a resource allows, as the API lists them in {@code Allow} headers and OPTIONS answers: a hash set of
 * method names (clients see its iteration order) that always includes OPTIONS, and HEAD wherever GET is allowed
 * unless the list answers a request with a method the resource does not support.
 */
public final class AllowedMethods {

    private AllowedMethods() {
    }

    public static Set<String> forOptions(Collection<String> declared) {
        Set<String> allowed = forUnsupportedMethod(declared);
        if (allowed.contains("GET")) {
            allowed.add("HEAD");
        }
        return allowed;
    }

    public static Set<String> forUnsupportedMethod(Collection<String> declared) {
        Set<String> allowed = new HashSet<>();
        declared.stream().filter(method -> !"HEAD".equals(method) && !"OPTIONS".equals(method)).forEach(allowed::add);
        allowed.add("OPTIONS");
        return allowed;
    }
}
