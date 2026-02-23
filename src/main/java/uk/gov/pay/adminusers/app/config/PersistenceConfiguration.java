package uk.gov.pay.adminusers.app.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.DefaultTransactionStatus;
import uk.gov.service.payments.commons.model.SupportedLanguageJpaConverter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * The EclipseLink persistence unit {@code AdminUsersUnit} (the entities and converters of the persistence entity
 * package and the language converter) on the service's data source, with Spring-managed transactions. Every DAO
 * method runs in a transaction, joining the one of the service method that calls it if there is one; the entity
 * manager lives as long as the outermost transaction, so entities returned outside of one are detached.
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class PersistenceConfiguration {

    private static final String PERSISTENCE_UNIT = "AdminUsersUnit";
    private static final String ENTITY_PACKAGE = "uk.gov.pay.adminusers.persistence.entity";

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                       AdminUsersConfig configuration) {
        JPAConfiguration jpaConfiguration = configuration.getJpaConfiguration();
        Map<String, Object> properties = new HashMap<>();
        properties.put("eclipselink.logging.level", jpaConfiguration.getJpaLoggingLevel());
        properties.put("eclipselink.logging.level.sql", jpaConfiguration.getSqlLoggingLevel());
        properties.put("eclipselink.query-results-cache", jpaConfiguration.getCacheSharedDefault());
        properties.put("eclipselink.cache.shared.default", jpaConfiguration.getCacheSharedDefault());
        properties.put("eclipselink.ddl-generation.output-mode", jpaConfiguration.getDdlGenerationOutputMode());
        properties.put("eclipselink.session.customizer", AdminUsersSessionCustomiser.class.getCanonicalName());
        properties.put("eclipselink.weaving", "false");

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName(PERSISTENCE_UNIT);
        factory.setPackagesToScan(ENTITY_PACKAGE, SupportedLanguageJpaConverter.class.getPackageName());
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setJpaPropertyMap(properties);
        return factory;
    }

    @Bean
    public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    /**
     * A failure inside a joined transaction is the outermost transactional method's to handle: it rolls back if that
     * method ends with a runtime exception, and commits otherwise. A commit that fails (a constraint violated by the
     * changes flushed at the end of the transaction) fails with the persistence provider's own exception, which
     * names the database error.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory) {
            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                try {
                    super.doCommit(status);
                } catch (TransactionSystemException | DataAccessException e) {
                    if (e.getCause() instanceof PersistenceException persistenceException) {
                        throw persistenceException;
                    }
                    throw e;
                }
            }
        };
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        return transactionManager;
    }
}
