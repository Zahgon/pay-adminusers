package uk.gov.pay.adminusers.persistence.dao;

import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Optional;

@Transactional
public abstract class JpaDao<T> {

    /* default */ final EntityManager entityManager;
    private final Class<T> persistenceClass;

    /* default */ JpaDao(EntityManager entityManager, Class<T> persistenceClass) {
        this.entityManager = entityManager;
        this.persistenceClass = persistenceClass;
    }

    public void persist(final T object) {
        entityManager.persist(object);
    }

    public void remove(T object) {
        if (entityManager.contains(object)) {
            entityManager.remove(object);
        } else {
            T mergedObject = entityManager.merge(object);
            entityManager.remove(mergedObject);
        }
    }

    public <ID> Optional<T> findById(final ID id) {
        return Optional.ofNullable(entityManager.find(persistenceClass, id));
    }

    public T merge(final T object) {
        return entityManager.merge(object);
    }
}
