package uk.gov.pay.adminusers.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import uk.gov.pay.adminusers.app.config.RestClientConfig;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;

/**
 * Builds the client adminusers calls other GOV.UK Pay services with: TLS 1.2 unless internal HTTPS is disabled,
 * entities read with a plain object mapper, and the caller's request id passed on.
 */
public final class RestClientFactory {

    private static final String TLSV1_2 = "TLSv1.2";

    private RestClientFactory() {
    }

    public static HttpClient buildHttpClient(RestClientConfig clientConfig) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1);
        if (!clientConfig.isDisabledSecureConnection()) {
            try {
                SSLContext sslContext = SSLContext.getInstance(TLSV1_2);
                sslContext.init(null, null, null);
                httpClientBuilder = httpClientBuilder.sslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(format("Unable to find an SSL context for %s", TLSV1_2), e);
            }
        }
        return httpClientBuilder.build();
    }

    public static RestClient buildClient(HttpClient httpClient) {
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2HttpMessageConverter(new ObjectMapper()));
                })
                .requestInterceptor(new RestClientLoggingInterceptor())
                .build();
    }
}
