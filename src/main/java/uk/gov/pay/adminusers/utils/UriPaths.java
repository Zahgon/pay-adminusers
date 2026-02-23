package uk.gov.pay.adminusers.utils;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds URLs by appending path segments to a base URL, with exactly one {@code /} between each.
 */
public final class UriPaths {

    private UriPaths() {
    }

    public static String append(String baseUrl, String... segments) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        for (String segment : segments) {
            if (!segment.isEmpty()) {
                builder.path("/").path(segment);
            }
        }
        return builder.build().toUriString();
    }
}
