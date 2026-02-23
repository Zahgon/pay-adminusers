package uk.gov.pay.adminusers.app.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.metrics.servlets.AdminServlet;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import uk.gov.pay.adminusers.web.ContainerErrorPages;

/**
 * The admin port (the management server on {@code ADMIN_PORT}): the operational menu with {@code /ping},
 * {@code /healthcheck} (the service's health checks), {@code /threads} and the metrics as JSON, and {@code /metrics}
 * in the Prometheus text format. Any other path is not found, with the servlet container's error page.
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class AdminContextConfiguration {

    @Bean
    public ServletContextInitializer adminServletRegistries(MetricRegistry metricRegistry,
                                                            HealthCheckRegistry healthCheckRegistry) {
        return servletContext -> {
            servletContext.setAttribute(MetricsServlet.METRICS_REGISTRY, metricRegistry);
            servletContext.setAttribute(HealthCheckServlet.HEALTH_CHECK_REGISTRY, healthCheckRegistry);
        };
    }

    @Bean
    public ServletRegistrationBean<AdminServlet> adminServlet() {
        return new ServletRegistrationBean<>(new AdminServlet(), "/*");
    }

    @Bean
    public ServletRegistrationBean<io.prometheus.client.servlet.jakarta.exporter.MetricsServlet> prometheusMetricsServlet(
            MetricRegistry metricRegistry) {
        CollectorRegistry collectorRegistry = CollectorRegistry.defaultRegistry;
        collectorRegistry.register(new DropwizardExports(metricRegistry));
        return new ServletRegistrationBean<>(
                new io.prometheus.client.servlet.jakarta.exporter.MetricsServlet(collectorRegistry), "/metrics");
    }

    @Bean
    public ContainerErrorPages adminContainerErrorPages() {
        return new ContainerErrorPages();
    }
}
