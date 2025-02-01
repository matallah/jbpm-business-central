package com.guh.audit.daos;

import com.guh.audit.entities.JbpmAuditTrail;

public class JbpmAuditTrailDAO extends GenericDAO<JbpmAuditTrail>{
    protected JbpmAuditTrailDAO(Class<JbpmAuditTrail> entityClass) {
        super(entityClass);
    }
}
