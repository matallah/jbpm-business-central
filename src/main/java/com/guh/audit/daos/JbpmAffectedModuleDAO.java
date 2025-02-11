package com.guh.audit.daos;

import com.guh.audit.entities.JbpmAffectedModule;

import javax.ejb.Stateless;

@Stateless
public class JbpmAffectedModuleDAO extends GenericDAO<JbpmAffectedModule> {
    public JbpmAffectedModuleDAO() {
        super(JbpmAffectedModule.class);
    }
}
