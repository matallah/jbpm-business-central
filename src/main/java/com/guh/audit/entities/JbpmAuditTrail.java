package com.guh.audit.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "jbpm_audit_trail")
@Getter @Setter
public class JbpmAuditTrail implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actionType;
    private String action;
    private String ipAddress;
    private String sessionId;

    @ManyToOne
    private JbpmAffectedModule affectedModule;
}
