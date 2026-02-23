package uk.gov.pay.adminusers.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import uk.gov.pay.adminusers.app.commands.DatabaseMigrationCommand;
import uk.gov.pay.adminusers.app.commands.DependentResourceWaitCommand;
import uk.gov.pay.adminusers.app.commands.MigrateToInitialDbState;

import java.util.List;

/**
 * Entry point of the adminusers service. The first arguments name the command to run, as they always have:
 * {@code server} runs the service, {@code db migrate} applies the Liquibase change log, {@code migrateToInitialDbState}
 * creates the initial (selfservice) database state if the database has none and {@code waitOnDependencies} waits
 * until the database accepts connections. A trailing configuration file argument is accepted and ignored: the service
 * is configured through environment variables only. Errors without an entity are rendered by the servlet container
 * itself, so Spring Boot's error controller stays off; migrations run only on request.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class, LiquibaseAutoConfiguration.class})
@ComponentScan(basePackages = "uk.gov.pay.adminusers", excludeFilters = {
        @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        // The commands run in contexts of their own.
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.gov\\.pay\\.adminusers\\.app\\.commands\\..*"),
        // The admin port's beans belong to the management context only.
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ManagementContextConfiguration.class)})
@ConfigurationPropertiesScan
public class AdminUsersApp {

    private static final String USAGE = "usage: java -jar pay-adminusers-0.1-SNAPSHOT-allinone.jar "
            + "{server | db migrate | migrateToInitialDbState | waitOnDependencies} [config file]";

    public static void main(String[] args) {
        List<String> arguments = List.of(args);
        if (startsWith(arguments, "server")) {
            SpringApplication.run(AdminUsersApp.class);
        } else if (startsWith(arguments, "db", "migrate")) {
            runToCompletion(DatabaseMigrationCommand.class);
        } else if (startsWith(arguments, "migrateToInitialDbState")) {
            runToCompletion(MigrateToInitialDbState.class);
        } else if (startsWith(arguments, "waitOnDependencies")) {
            runToCompletion(DependentResourceWaitCommand.class);
        } else {
            System.err.println(USAGE);
            System.exit(1);
        }
    }

    private static boolean startsWith(List<String> arguments, String... command) {
        return arguments.size() >= command.length && arguments.subList(0, command.length).equals(List.of(command));
    }

    private static void runToCompletion(Class<?> command) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(command)
                .web(WebApplicationType.NONE)
                .run();
        System.exit(SpringApplication.exit(context));
    }
}
