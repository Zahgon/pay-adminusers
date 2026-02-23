package uk.gov.pay.adminusers.persistence.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.pay.adminusers.persistence.entity.GovUkPayAgreementEntity;

import jakarta.persistence.EntityManager;
import java.util.Optional;

@Repository
@Transactional
public class GovUkPayAgreementDao extends JpaDao<GovUkPayAgreementEntity> {
    public GovUkPayAgreementDao(EntityManager entityManager) {
        super(entityManager, GovUkPayAgreementEntity.class);
    }
    
    public Optional<GovUkPayAgreementEntity> findByExternalServiceId(String externalServiceId) {

        String query = "SELECT agreement FROM GovUkPayAgreementEntity agreement " +
                "WHERE agreement.service.externalId  = :externalServiceId";

        return entityManager
                .createQuery(query, GovUkPayAgreementEntity.class)
                .setParameter("externalServiceId", externalServiceId)
                .getResultStream()
                .findFirst();
    }
}
