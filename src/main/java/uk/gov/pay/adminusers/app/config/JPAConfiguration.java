package uk.gov.pay.adminusers.app.config;

public class JPAConfiguration {

    private String jpaLoggingLevel;
    private String sqlLoggingLevel;
    private String ddlGenerationOutputMode;
    private String queryResultsCache;
    private String cacheSharedDefault;

    public String getJpaLoggingLevel() {
        return jpaLoggingLevel;
    }

    public String getSqlLoggingLevel() {
        return sqlLoggingLevel;
    }

    public String getDdlGenerationOutputMode() {
        return ddlGenerationOutputMode;
    }

    public String getQueryResultsCache() {
        return queryResultsCache;
    }

    public String getCacheSharedDefault() {
        return cacheSharedDefault;
    }

    public void setJpaLoggingLevel(String jpaLoggingLevel) {
        this.jpaLoggingLevel = jpaLoggingLevel;
    }

    public void setSqlLoggingLevel(String sqlLoggingLevel) {
        this.sqlLoggingLevel = sqlLoggingLevel;
    }

    public void setDdlGenerationOutputMode(String ddlGenerationOutputMode) {
        this.ddlGenerationOutputMode = ddlGenerationOutputMode;
    }

    public void setQueryResultsCache(String queryResultsCache) {
        this.queryResultsCache = queryResultsCache;
    }

    public void setCacheSharedDefault(String cacheSharedDefault) {
        this.cacheSharedDefault = cacheSharedDefault;
    }
}
