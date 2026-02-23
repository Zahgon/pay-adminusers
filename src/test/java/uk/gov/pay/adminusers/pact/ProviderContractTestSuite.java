package uk.gov.pay.adminusers.pact;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import java.util.Map;

import static java.lang.String.format;

/**
 * Runs the provider contract tests of the consumer named by the {@code CONSUMER} system property, or those of all
 * consumers when it is not set.
 */
@Suite
@SelectClasses({ConnectorQueueMessageContractTest.class, ProductsUIContractTest.class, SelfServiceContractTest.class})
public class ProviderContractTestSuite {

    private static final Map<String, Class<?>> CONTRACT_TESTS = Map.of(
            "connector", ConnectorQueueMessageContractTest.class,
            "products-ui", ProductsUIContractTest.class,
            "selfservice", SelfServiceContractTest.class);

    static class ConsumerSelection implements ExecutionCondition {

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            String consumer = System.getProperty("CONSUMER");
            if (consumer == null || consumer.isBlank()) {
                return ConditionEvaluationResult.enabled("Running all contract tests.");
            }
            if (!CONTRACT_TESTS.containsKey(consumer)) {
                throw new RuntimeException(format("Error running provider contract tests. ${CONSUMER} system property was %s.", consumer));
            }
            Class<?> testClass = context.getRequiredTestClass();
            return CONTRACT_TESTS.get(consumer).isAssignableFrom(testClass)
                    ? ConditionEvaluationResult.enabled(format("Running %s-connector contract tests only.", consumer))
                    : ConditionEvaluationResult.disabled(format("Running %s-connector contract tests only.", consumer));
        }
    }
}
