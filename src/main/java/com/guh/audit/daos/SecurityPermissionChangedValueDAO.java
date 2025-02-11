package com.guh.audit.daos;

import com.guh.audit.entities.SecurityPermissionChangedValue;

import javax.ejb.Stateless;

@Stateless
public class SecurityPermissionChangedValueDAO extends GenericDAO<SecurityPermissionChangedValue> {
    public SecurityPermissionChangedValueDAO() {
        super(SecurityPermissionChangedValue.class);
    }

}
