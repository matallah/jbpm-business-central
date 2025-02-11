package com.guh.audit.daos;

import com.guh.audit.entities.JbpmAuditTrail;

import javax.ejb.Stateless;

@Stateless
public class JbpmAuditTrailDAO extends GenericDAO<JbpmAuditTrail>{
    public JbpmAuditTrailDAO() {
        super(JbpmAuditTrail.class);
    }
}
