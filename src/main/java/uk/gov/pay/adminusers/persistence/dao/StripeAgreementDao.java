package uk.gov.pay.adminusers.persistence.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.pay.adminusers.persistence.entity.StripeAgreementEntity;

import jakarta.persistence.EntityManager;
import java.util.Optional;

@Repository
@Transactional
public class StripeAgreementDao extends JpaDao<StripeAgreementEntity> {
    public StripeAgreementDao(EntityManager entityManager) {
        super(entityManager, StripeAgreementEntity.class);
    }


    public Optional<StripeAgreementEntity> findByServiceExternalId(String serviceExternalId) {

        String query = "SELECT s FROM StripeAgreementEntity s " +
                "WHERE s.service.externalId = :serviceExternalId";

        return entityManager
                .createQuery(query, StripeAgreementEntity.class)
                .setParameter("serviceExternalId", serviceExternalId)
                .getResultStream()
                .findFirst();
    }
}
