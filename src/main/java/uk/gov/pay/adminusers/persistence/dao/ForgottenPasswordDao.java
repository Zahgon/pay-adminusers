package uk.gov.pay.adminusers.persistence.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.persistence.entity.ForgottenPasswordEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
@Transactional
public class ForgottenPasswordDao extends JpaDao<ForgottenPasswordEntity> {

    private final Integer forgottenPasswordExpiryMinutes;

    protected ForgottenPasswordDao(EntityManager entityManager, AdminUsersConfig configuration) {
        super(entityManager, ForgottenPasswordEntity.class);
        this.forgottenPasswordExpiryMinutes = configuration.getForgottenPasswordExpiryMinutes();
    }

    public Optional<ForgottenPasswordEntity> findNonExpiredByCode(String code) {
        String query = "SELECT fp FROM ForgottenPasswordEntity fp " +
                "WHERE fp.code = :code AND fp.date >= :expiry";

        ZonedDateTime expiryDateTime = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(forgottenPasswordExpiryMinutes);

        return entityManager
                .createQuery(query, ForgottenPasswordEntity.class)
                .setParameter("code", code)
                .setParameter("expiry", expiryDateTime)
                .getResultList().stream().findFirst();
    }

    public int deleteForgottenPasswords(ZonedDateTime deleteRecordsBeforeDate) {
        String query = "DELETE FROM ForgottenPasswordEntity" +
                " WHERE createdAt < :deleteRecordsBeforeDate";

        return entityManager
                .createQuery(query)
                .setParameter("deleteRecordsBeforeDate", deleteRecordsBeforeDate)
                .executeUpdate();
    }
}
