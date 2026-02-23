package uk.gov.pay.adminusers.app.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import uk.gov.pay.adminusers.app.healthchecks.DatabaseHealthCheck;
import uk.gov.pay.adminusers.app.metrics.DatabaseMetricsService;
import uk.gov.pay.adminusers.client.RestClientFactory;
import uk.gov.pay.adminusers.client.ledger.service.LedgerService;
import uk.gov.pay.adminusers.queue.sqs.SqsQueueService;
import uk.gov.pay.adminusers.service.LinksBuilder;
import uk.gov.pay.adminusers.service.NotificationService;
import uk.gov.pay.adminusers.service.NotifyClientProvider;
import uk.gov.pay.adminusers.utils.json.ObjectMapperFactory;
import uk.gov.pay.adminusers.web.JsonMessageConverter;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.InstantSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The service's own beans that are not discovered by component scanning: settings, the clock, the JSON mapper,
 * the clients of GOV.UK Notify, ledger and SQS (closed with the application context) and the health and metric
 * registries.
 */
@Configuration(proxyBeanMethods = false)
public class AdminUsersServiceConfiguration {

    private static final String SERVICE_METRICS_NODE = "adminusers";
    private static final int METRICS_COLLECTION_PERIOD_SECONDS = 30;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return ObjectMapperFactory.newObjectMapper();
    }

    @Bean
    public JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new JsonMessageConverter(objectMapper);
    }

    @Bean
    public InstantSource instantSource() {
        return InstantSource.system();
    }

    @Bean
    public LinksConfig linksConfig(AdminUsersConfig configuration) {
        return configuration.getLinks();
    }

    @Bean
    public LinksBuilder linksBuilder(AdminUsersConfig configuration) {
        return new LinksBuilder(configuration.getBaseUrl());
    }

    @Bean
    public GoogleAuthenticatorConfig googleAuthenticatorConfig(AdminUsersConfig configuration) {
        SecondFactorAuthConfiguration secondFactorAuthConfig = configuration.getSecondFactorAuthConfiguration();
        return new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(secondFactorAuthConfig.getValidTimeWindows())
                .setTimeStepSizeInMillis(secondFactorAuthConfig.getTimeWindowInMillis())
                .build();
    }

    @Bean
    public NotifyClientProvider notifyClientProvider(AdminUsersConfig configuration) {
        return new NotifyClientProvider(configuration.getNotifyConfiguration());
    }

    @Bean
    public NotificationService notificationService(NotifyClientProvider notifyClientProvider,
                                                   AdminUsersConfig configuration, MetricRegistry metricRegistry) {
        return new NotificationService(
                notifyClientProvider,
                configuration.getNotifyConfiguration(),
                configuration.getNotifyDirectDebitConfiguration(),
                metricRegistry);
    }

    @Bean(destroyMethod = "close")
    public SqsClient sqsClient(AdminUsersConfig configuration) {
        SqsConfig sqsConfig = configuration.getSqsConfig();
        SqsClientBuilder clientBuilder = SqsClient.builder();
        if (sqsConfig.isNonStandardServiceEndpoint()) {
            AwsBasicCredentials basicAWSCredentials = AwsBasicCredentials
                    .create(sqsConfig.getAccessKey(), sqsConfig.getSecretKey());
            clientBuilder
                    .credentialsProvider(StaticCredentialsProvider.create(basicAWSCredentials))
                    .endpointOverride(URI.create(sqsConfig.getEndpoint()))
                    .region(Region.of(sqsConfig.getRegion()));
        } else {
            // uses AWS SDK's DefaultAWSCredentialsProviderChain to obtain credentials
            clientBuilder.region(Region.of(sqsConfig.getRegion()));
        }
        return clientBuilder.build();
    }

    @Bean
    public SqsQueueService sqsQueueService(SqsClient sqsClient, AdminUsersConfig configuration) {
        return new SqsQueueService(
                sqsClient,
                configuration.getSqsConfig().getMessageMaximumWaitTimeInSeconds(),
                configuration.getSqsConfig().getMessageMaximumBatchSize());
    }

    @Bean(destroyMethod = "close")
    public HttpClient ledgerHttpClient(AdminUsersConfig configuration) {
        return RestClientFactory.buildHttpClient(configuration.getRestClientConfig());
    }

    @Bean
    public RestClient ledgerRestClient(HttpClient ledgerHttpClient) {
        return RestClientFactory.buildClient(ledgerHttpClient);
    }

    @Bean
    public LedgerService ledgerService(RestClient ledgerRestClient, AdminUsersConfig configuration) {
        return new LedgerService(ledgerRestClient, configuration.getLedgerBaseUrl());
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public HealthCheckRegistry healthCheckRegistry(DataSourceProperties dataSourceProperties) {
        HealthCheckRegistry registry = new HealthCheckRegistry();
        registry.register("deadlocks", new ThreadDeadlockHealthCheck());
        registry.register("database", new DatabaseHealthCheck(dataSourceProperties));
        return registry;
    }

    @Bean
    public DatabaseMetricsService databaseMetricsService(DataSourceProperties dataSourceProperties,
                                                         MetricRegistry metricRegistry) {
        return new DatabaseMetricsService(dataSourceProperties, metricRegistry, SERVICE_METRICS_NODE);
    }

    /**
     * Collects the database's statistics as metrics every {@value #METRICS_COLLECTION_PERIOD_SECONDS} / 2 seconds.
     */
    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService metricsCollector(DatabaseMetricsService metricsService) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(metricsService::updateMetricData, 0, METRICS_COLLECTION_PERIOD_SECONDS / 2,
                TimeUnit.SECONDS);
        return executor;
    }
}
