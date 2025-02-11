package com.guh.audit.daos;

import com.guh.audit.entities.JbpmModule;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Stateless
public class JbpmModuleDAO extends GenericDAO<JbpmModule> {
    @PersistenceContext(unitName = "auditPU")
    private EntityManager em;

    public JbpmModuleDAO() {
        super(JbpmModule.class);
    }

    public JbpmModule findByName(String name) {
        try {
            return em.createQuery("SELECT m FROM JbpmModule m WHERE m.name = :name", JbpmModule.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
