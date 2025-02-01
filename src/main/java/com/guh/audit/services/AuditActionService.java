package com.guh.audit.services;

import com.guh.audit.daos.JbpmAffectedModuleDAO;
import com.guh.audit.daos.JbpmAuditTrailDAO;
import com.guh.audit.daos.JbpmModuleDAO;
import com.guh.audit.daos.SecurityPermissionChangedValueDAO;
import com.guh.audit.entities.JbpmAffectedModule;
import com.guh.audit.entities.JbpmAuditTrail;
import com.guh.audit.entities.JbpmModule;
import com.guh.audit.entities.SecurityPermissionChangedValue;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
public class AuditActionService {

    @EJB
    private JbpmModuleDAO moduleDAO;

    @EJB
    private JbpmAffectedModuleDAO affectedModuleDAO;

    @EJB
    private JbpmAuditTrailDAO auditTrailDAO;

    @EJB
    private SecurityPermissionChangedValueDAO securityPermissionDAO;

    public enum ModuleType {
        LOGIN("login"),
        PROJECTS("Projects"),
        PAGES("Pages"),
        SERVERS("servers"),
        DATASOURCES("datasources"),
        DASHBUILDER("dashbuilder"),
        SECURITY_SETTINGS("security_settings");

        private final String moduleName;

        ModuleType(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getModuleName() {
            return moduleName;
        }
    }

    public enum ActionType {
        INSERT,UPDATE,DELETE
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logLoginAction(String username, String ipAddress, String sessionId) {
        logAction(
                ModuleType.LOGIN,
                username,
                ActionType.INSERT,
                String.format("user %s logged in", username),
                ipAddress,
                sessionId,
                null,
                null
        );
    }


    public void logProjectAction(String projectId, String spaceId, String username,
                                 ActionType actionType, String ipAddress, String sessionId) {
        String action;
        switch (actionType) {
            case INSERT:
                action = String.format("User %s created new project with id %s in space %s",
                        username, projectId, spaceId);
                break;
            case UPDATE:
                action = String.format("User %s modify %s in space %s",
                        username, projectId, spaceId);
                break;
            case DELETE:
                action = String.format("User %s deleted project with id %s in space %s",
                        username, projectId, spaceId);
                break;
            default:
                throw new IllegalArgumentException("Invalid action type for project");
        }

        logAction(ModuleType.PROJECTS, projectId, actionType, action, ipAddress, sessionId, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logProjectDeployment(String projectId, String spaceId, String username,
                                     String ipAddress, String sessionId) {
        String action = String.format("User %s deployed the project %s exists in space %s",
                username, projectId, spaceId);
        logAction(ModuleType.PROJECTS, projectId, ActionType.UPDATE, action, ipAddress, sessionId, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logPageAction(String pageName, String username, ActionType actionType,
                              String ipAddress, String sessionId) {
        String action;
        switch (actionType) {
            case INSERT:
                action = String.format("User %s created new page with name %s", username, pageName);
                break;
            case UPDATE:
                action = String.format("User %s modified page %s", username, pageName);
                break;
            case DELETE:
                action = String.format("User %s deleted page %s", username, pageName);
                break;
            default:
                throw new IllegalArgumentException("Invalid action type for page");
        }

        logAction(ModuleType.PAGES, pageName, actionType, action, ipAddress, sessionId, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logServerAction(String serverId, String username, ActionType actionType,
                                String ipAddress, String sessionId) {
        String action;
        switch (actionType) {
            case INSERT:
                action = String.format("User %s created new server with ip %s", username, serverId);
                break;
            case UPDATE:
                action = String.format("User %s updated server with ip %s", username, serverId);
                break;
            case DELETE:
                action = String.format("User %s deleted server with ip %s", username, serverId);
                break;
            default:
                throw new IllegalArgumentException("Invalid action type for server");
        }

        logAction(ModuleType.SERVERS, serverId, actionType, action, ipAddress, sessionId, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logDataSourceAction(String datasourceName, String datasourceType,
                                    String connectionURL, String username, ActionType actionType,
                                    String ipAddress, String sessionId) {
        String action;
        switch (actionType) {
            case INSERT:
                action = String.format("User %s created new %s with name %s connected to %s",
                        username, datasourceType, datasourceName, connectionURL);
                break;
            case DELETE:
                action = String.format("User %s deleted %s datasource", username, datasourceName);
                break;
            default:
                throw new IllegalArgumentException("Invalid action type for datasource");
        }

        logAction(ModuleType.DATASOURCES, datasourceName, actionType, action, ipAddress, sessionId, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logDashbuilderAction(String fileName, String username, String action,
                                     ActionType actionType, String ipAddress, String sessionId) {
        logAction(ModuleType.DASHBUILDER, fileName, actionType, action, ipAddress, sessionId, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void logSecuritySettingsAction(String name, String type, String username,
                                          String oldValue, String newValue,
                                          String ipAddress, String sessionId) {
        String affectedModuleId = String.format("%s-%s_security_settings", name, type);
        String action = String.format("User %s changed security settings on %s %s",
                username, type, name);

        logAction(ModuleType.SECURITY_SETTINGS, affectedModuleId, ActionType.UPDATE,
                action, ipAddress, sessionId, oldValue, newValue);
    }
    private void logAction(ModuleType moduleType, String affectedModuleId,
                           ActionType actionType, String action, String ipAddress,
                           String sessionId, String oldValue, String newValue) {
        // Update the implementation to use DAOs instead of repositories
        JbpmModule module = moduleDAO.findByName(moduleType.getModuleName());
        if (module == null) {
            module = new JbpmModule();
            module.setName(moduleType.getModuleName());
            module = moduleDAO.save(module);
        }

        JbpmAffectedModule affectedModule = new JbpmAffectedModule();
        affectedModule.setAffectedModuleId(affectedModuleId);
        affectedModule.setModule(module);
        affectedModule = affectedModuleDAO.save(affectedModule);

        JbpmAuditTrail auditTrail = new JbpmAuditTrail();
        auditTrail.setActionType(actionType.name());
        auditTrail.setAction(action);
        auditTrail.setIpAddress(ipAddress);
        auditTrail.setSessionId(sessionId);
        auditTrail.setAffectedModule(affectedModule);
        auditTrail = auditTrailDAO.save(auditTrail);

        if (oldValue != null || newValue != null) {
            SecurityPermissionChangedValue securityChanges = new SecurityPermissionChangedValue();
            securityChanges.setOldValue(oldValue);
            securityChanges.setNewValue(newValue);
            securityChanges.setAuditTrail(auditTrail);
            securityPermissionDAO.save(securityChanges);
        }
    }
}
