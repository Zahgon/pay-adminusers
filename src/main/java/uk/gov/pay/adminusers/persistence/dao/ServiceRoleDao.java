package uk.gov.pay.adminusers.persistence.dao;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.pay.adminusers.persistence.entity.ServiceRoleEntity;

import jakarta.persistence.EntityManager;
import java.util.List;

@Repository
@Transactional
public class ServiceRoleDao extends JpaDao<ServiceRoleEntity> {
    /* default */ ServiceRoleDao(EntityManager entityManager) {
        super(entityManager, ServiceRoleEntity.class);
    }

    public List<ServiceRoleEntity> findServiceUserRoles(Integer serviceId) {
        return entityManager
                .createQuery("select sre from ServiceRoleEntity sre where sre.service.id = :serviceId")
                .setParameter("serviceId", serviceId)
                .getResultList();
    }
}
