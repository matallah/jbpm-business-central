package com.guh.audit.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "security_permission_changed_value")
@Getter @Setter
public class SecurityPermissionChangedValue implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String oldValue;
    private String newValue;

    @ManyToOne
    private JbpmAuditTrail auditTrail;
}
