package uk.gov.pay.adminusers.utils;

/**
 * The names under which values are put into the logging context and into structured log entries.
 */
public final class LoggingKeys {

    public static final String MDC_REQUEST_ID_KEY = "x_request_id";
    public static final String SERVICE_EXTERNAL_ID = "service_external_id";
    public static final String USER_EXTERNAL_ID = "user_external_id";
    public static final String GATEWAY_ACCOUNT_ID = "gateway_account_id";
    public static final String PAYMENT_EXTERNAL_ID = "payment_external_id";
    public static final String LEDGER_EVENT_TYPE = "ledger_event_type";
    public static final String SQS_MESSAGE_ID = "sqs_message_id";
    public static final String METHOD = "method";
    public static final String URL = "url";
    public static final String HTTP_STATUS = "status_code";
    public static final String RESPONSE_TIME = "response_time";

    private LoggingKeys() {
    }
}
