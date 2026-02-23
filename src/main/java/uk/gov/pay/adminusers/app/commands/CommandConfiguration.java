package uk.gov.pay.adminusers.app.commands;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;

/**
 * What the commands that run outside the server need: the service's settings, which must be complete and valid as
 * for the server, and the database to connect to.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AdminUsersConfig.class, DataSourceProperties.class})
class CommandConfiguration {
}
