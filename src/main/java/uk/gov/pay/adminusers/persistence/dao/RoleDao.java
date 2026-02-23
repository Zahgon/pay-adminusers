package uk.gov.pay.adminusers.persistence.dao;


import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.persistence.entity.RoleEntity;

import jakarta.persistence.EntityManager;
import java.util.Optional;

/**
 * Not extending the generic JpaDao purposefully.
 * <p>This is to avoid inheriting persistence methods such as <b>persist, merge, etc</b>
 * which should never be used with ReadOnly RoleEntity.
 * </p>
 *     @see uk.gov.pay.adminusers.persistence.entity.RoleEntity
 */
@Repository
@Transactional
public class RoleDao {

    private final EntityManager entityManager;
    public RoleDao(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    public Optional<RoleEntity> findByRoleName(RoleName roleName) {

        String query = "SELECT r FROM RoleEntity r " +
                "WHERE r.roleName = :roleName";

        return entityManager
                .createQuery(query, RoleEntity.class)
                .setParameter("roleName", roleName)
                .getResultList().stream().findFirst();
    }
}
