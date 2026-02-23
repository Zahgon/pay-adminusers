package uk.gov.pay.adminusers.persistence.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.pay.adminusers.persistence.entity.InviteEntity;

import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class InviteDao extends JpaDao<InviteEntity> {
    protected InviteDao(EntityManager entityManager) {
        super(entityManager, InviteEntity.class);
    }

    public Optional<InviteEntity> findByCode(String code) {

        String query = "SELECT invite FROM InviteEntity invite " +
                "WHERE invite.code = :code";

        return entityManager
                .createQuery(query, InviteEntity.class)
                .setParameter("code", code)
                .getResultList().stream().findFirst();
    }

    public List<InviteEntity> findByEmail(String email) {

        String query = "SELECT invite FROM InviteEntity invite " +
                "WHERE invite.email = :email";

        return entityManager
                .createQuery(query, InviteEntity.class)
                .setParameter("email", email)
                .getResultList();
    }

    public List<InviteEntity> findAllByServiceId(String serviceId) {

        String query = "SELECT invite FROM InviteEntity invite " +
                "WHERE invite.service.externalId = :serviceId";

        return entityManager
                .createQuery(query, InviteEntity.class)
                .setParameter("serviceId", serviceId)
                .getResultList();
    }

    public int deleteInvites(ZonedDateTime deleteRecordsBeforeDate) {
        String query = "DELETE FROM InviteEntity ie" +
                " WHERE ie.date < :deleteRecordsBeforeDate";

        return entityManager
                .createQuery(query)
                .setParameter("deleteRecordsBeforeDate", deleteRecordsBeforeDate)
                .executeUpdate();
    }
}
