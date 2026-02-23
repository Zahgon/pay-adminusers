package uk.gov.pay.adminusers.service;

import uk.gov.pay.adminusers.model.ForgottenPassword;
import uk.gov.pay.adminusers.model.Invite;
import uk.gov.pay.adminusers.model.Link;
import uk.gov.pay.adminusers.model.Link.Rel;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.model.User;

import java.util.List;

import static uk.gov.pay.adminusers.utils.UriPaths.append;
import static uk.gov.pay.adminusers.resources.ForgottenPasswordResource.FORGOTTEN_PASSWORDS_RESOURCE;
import static uk.gov.pay.adminusers.resources.UserResource.USERS_RESOURCE;

public class LinksBuilder {

    private final String baseUrl;

    public LinksBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public User decorate(User user) {
        String uri = append(baseUrl, USERS_RESOURCE, user.getExternalId());
        Link selfLink = Link.from(Rel.SELF, "GET", uri);
        user.setLinks(List.of(selfLink));
        return user;
    }

    public Service decorate(Service service) {
        String uri = append(baseUrl, "/v1/api/services", String.valueOf(service.getExternalId()));
        Link selfLink = Link.from(Rel.SELF, "GET", uri);
        service.setLinks(List.of(selfLink));
        return service;
    }

    public ForgottenPassword decorate(ForgottenPassword forgottenPassword) {
        String uri = append(baseUrl, FORGOTTEN_PASSWORDS_RESOURCE, forgottenPassword.getCode());
        Link selfLink = Link.from(Rel.SELF, "GET", uri);
        forgottenPassword.setLinks(List.of(selfLink));
        return forgottenPassword;
    }

    public Invite decorate(Invite invite) {
        String uri = append(baseUrl, "/v1/api/invites", invite.getCode());
        Link selfLink = Link.from(Rel.SELF, "GET", uri);
        invite.getLinks().add(selfLink);
        return invite;
    }

    public Invite addUserLink(User user, Invite invite) {
        String uri = append(baseUrl, USERS_RESOURCE, user.getExternalId());
        Link userLink = Link.from(Rel.USER, "GET", uri);
        invite.getLinks().add(userLink);
        return invite;
    }
}
