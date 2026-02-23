package uk.gov.pay.adminusers.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import uk.gov.pay.adminusers.exception.WebApplicationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AdminUsersExceptionsTest {

    @Test
    public void shouldCreateARoleUnavailabeException() {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.undefinedRoleException("non-existent-role");
        assertThat(undefinedRoleException.getStatus().value(), is(400));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getEntity();
        assertThat(entity.get("errors").get(0), is("role [non-existent-role] not recognised"));
    }

    @Test
    public void shouldCreateAConflictingUsernameException() {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.conflictingEmail("existing-user@example.com");
        assertThat(undefinedRoleException.getStatus().value(), is(409));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getEntity();
        assertThat(entity.get("errors").get(0), is("email [existing-user@example.com] already exists"));
    }

    @Test
    public void shouldCreateAnInternalServerErrorException() {
        WebApplicationException undefinedRoleException = AdminUsersExceptions.internalServerError("server error");
        assertThat(undefinedRoleException.getStatus().value(), is(500));
        Map<String, List<String>> entity = (Map<String, List<String>>) undefinedRoleException.getEntity();
        assertThat(entity.get("errors").get(0), is("server error"));
    }
}
