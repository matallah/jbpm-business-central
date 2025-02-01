package com.guh.audit.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "jbpm_affected_module")
@Getter @Setter
public class JbpmAffectedModule implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String affectedModuleId;

    @ManyToOne
    private JbpmModule module;
}
