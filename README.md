
# jBPM

## Overview

We need to log all user actions in a secure database to monitor and analyze cybersecurity-related activities.

## Prerequisites

Ensure you have the following prerequisites:
- A working environment of the project with the specified directory structure.
- Java Development Kit (JDK) set up properly.
- Access to the source code directories mentioned in the paths below.

## Integration Steps

### 1. Projects

Add the following code at the end of the specified classes in the mapping libraries `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`:

**Affected JARs:**
- `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`
- `kie-wb-common-services-backend-7.75.0-20241022.122359-92-sources.jar`
- 
**Affected Classes:**
- `org.guvnor.structure.backend.repositories.RepositoryServiceImpl.java` in `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`
- `org.guvnor.structure.backend.organizationalunit.config.SpaceConfigStorageImpl.java` in `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`
- `org.kie.workbench.common.services.backend.builder.service.BuildServiceHelper.java` in `kie-wb-common-services-backend-7.75.0-20241022.122359-92-sources.jar`

```java
@EJB
private AuditActionService auditActionService;

@Inject
private HttpServletRequest request;

@Inject
private User loggedInUser;

// Add this new method to the class
private Map<String, String> getCurrentUserInfo() {
    Map<String, String> userInfo = new LinkedHashMap<>();

    try {
        // Get username
        String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

        // Get session ID
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "no-session";

        // Get IP address
        String ipAddress = getClientIpAddress(request);

        userInfo.put("username", username);
        userInfo.put("sessionId", sessionId);
        userInfo.put("ipAddress", ipAddress);

        logger.debug("User info collected - username: {}, sessionId: {}, ipAddress: {}",
                username, sessionId, ipAddress);

    } catch (Exception e) {
        logger.error("Error getting user information", e);
        userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
    }

    return userInfo;
}

private String getClientIpAddress(HttpServletRequest request) {
    String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : HEADERS_TO_CHECK) {
        String ip = request.getHeader(header);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // If it's a comma separated list, take the first IP
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }
    }

    return request.getRemoteAddr();
}
```

#### 1.1 Add New Project

Adapt the following method in `org.guvnor.structure.backend.repositories.RepositoryServiceImpl.java` in Jar `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`:

```java
@Override
public Repository createRepository(final OrganizationalUnit organizationalUnit,
                                   final String scheme,
                                   final String alias,
                                   final RepositoryEnvironmentConfigurations repositoryEnvironmentConfigurations,
                                   final Collection<Contributor> contributors) throws RepositoryAlreadyExistsException {

    try {
        repositoryEnvironmentConfigurations.setSpace(organizationalUnit.getName());

        Space space = spacesAPI.getSpace(organizationalUnit.getName());
        String newAlias = createFreshRepositoryAlias(alias, space);

        final Repository repository = createRepository(scheme,
                newAlias,
                new Space(organizationalUnit.getName()),
                repositoryEnvironmentConfigurations,
                contributors);
        if (organizationalUnit != null && repository != null) {
            organizationalUnitService.addRepository(organizationalUnit, repository);
        }
        metadataStore.write(newAlias,
                (String) repositoryEnvironmentConfigurations.getOrigin(),
                false);
        Map<String, String> currentUserInfo = getCurrentUserInfo();
        if (currentUserInfo.containsKey("error")) {
            logger.error("Failed to log project action: {}", currentUserInfo.get("error"));
        } else {
            // Capture variables for async logging
            String repoAlias = newAlias;
            String spaceName = space.getName();
            String username = currentUserInfo.get("username");
            AuditActionService.ActionType actionType = AuditActionService.ActionType.INSERT;
            String ipAddress = currentUserInfo.get("ipAddress");
            String sessionId = currentUserInfo.get("sessionId");

            // Execute audit logging asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    this.auditActionService.logProjectAction(
                            repoAlias,
                            spaceName,
                            username,
                            actionType,
                            ipAddress,
                            sessionId
                    );
                } catch (Exception e) {
                    logger.error("Failed to asynchronously log project action", e);
                }
            });
        }
        return repository;
    } catch (final Exception e) {
        logger.error("Error during create repository", e);
        throw ExceptionUtilities.handleException(e);
    }
}
```
#### 1.2 Edit Existing Project

Adapt the following method in `org.guvnor.structure.backend.organizationalunit.config.SpaceConfigStorageImpl.java` in Jar `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`:

```java
@Override
public void saveBranchPermissions(final String branchName,
                                  final String projectIdentifier,
                                  final BranchPermissions branchPermissions) {
    objectStorage.write(buildBranchConfigFilePath(branchName,
                    projectIdentifier,
                    BRANCH_PERMISSIONS),
            branchPermissions);
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (currentUserInfo.containsKey("error")) {
        logger.error("Failed to log project action: {}", currentUserInfo.get("error"));
    } else {
        if (projectIdentifier != null && projectIdentifier.startsWith("git://")) {
            String[] parts = projectIdentifier.substring(6).split("/", 2);
            if (parts.length == 2) {
                // Capture variables for async logging
                String repoAlias = parts[0];
                String spaceName = parts[1];
                String username = currentUserInfo.get("username");
                AuditActionService.ActionType actionType = AuditActionService.ActionType.UPDATE;
                String ipAddress = currentUserInfo.get("ipAddress");
                String sessionId = currentUserInfo.get("sessionId");

                // Execute audit logging asynchronously
                CompletableFuture.runAsync(() -> {
                    try {
                        this.auditActionService.logProjectAction(
                                repoAlias,
                                spaceName,
                                username,
                                actionType,
                                ipAddress,
                                sessionId
                        );
                    } catch (Exception e) {
                        logger.error("Failed to asynchronously log project action", e);
                    }
                });
            }
        }
    }
}
```
#### 1.3 Deploy Project

Adapt the following method in `org.kie.workbench.common.services.backend.builder.service.BuildServiceHelper.java` in Jar `kie-wb-common-services-backend-7.75.0-20241022.122359-92-sources.jar`:

```java
public BuildResults localBuildAndDeploy(final Module module,
                                            final DeploymentMode mode,
                                            final boolean suppressHandlers) {
        final BuildResults[] result = new BuildResults[1];
        invokeLocalBuildPipeLine(module,
                                 suppressHandlers,
                                 mode,
                                 localBinaryConfig -> {
                                     result[0] = localBinaryConfig.getBuildResults();
                                     result[0].setRootPathURI(module.getRootPath().toURI());
                                 });
        String project = (module != null && module.getPom() != null && module.getPom().getName() != null)
                ? module.getPom().getName()
                : "";

        String space = (module != null && module.getPom() != null && module.getPom().getGav() != null && module.getPom().getGav().getGroupId() != null)
                ? module.getPom().getGav().getGroupId().replace("com.", "")
                : "";
        Map<String, String> currentUserInfo = getCurrentUserInfo();
        if (currentUserInfo.containsKey("error")) {
        } else {
            // Capture variables for async logging
            String repoAlias = project;
            String spaceName = space;
            String username = currentUserInfo.get("username");
            AuditActionService.ActionType actionType = AuditActionService.ActionType.DEPLOY;
            String ipAddress = currentUserInfo.get("ipAddress");
            String sessionId = currentUserInfo.get("sessionId");

            // Execute audit logging asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    this.auditActionService.logProjectAction(
                            repoAlias,
                            spaceName,
                            username,
                            actionType,
                            ipAddress,
                            sessionId
                    );
                } catch (Exception e) {
                }
            });
        }
        return result[0];
    }
```
#### 1.4 Delete Project

Adapt the following method in `org.guvnor.structure.backend.organizationalunit.config.SpaceConfigStorageImpl.java` in Jar `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`:

```java
@Override
public void removeRepository(final Space space,
                             final String alias) {

    spaceConfigStorage.getBatch(space.getName())
            .run(context -> {

                final Optional<org.guvnor.structure.organizationalunit.config.RepositoryInfo> config = findRepositoryConfig(space.getName(), alias);

                try {
                    OrganizationalUnit orgUnit = Optional
                            .ofNullable(organizationalUnitService.getOrganizationalUnit(space.getName()))
                            .orElseThrow(() -> new IllegalArgumentException(String
                                    .format("The given space [%s] does not exist.",
                                            space.getName())));
                    doRemoveRepository(orgUnit,
                            alias,
                            config,
                            repo -> repositoryRemovedEvent.fire(new RepositoryRemovedEvent(repo)),
                            true);

                    Map<String, String> currentUserInfo = getCurrentUserInfo();
                    if (currentUserInfo.containsKey("error")) {
                        logger.error("Failed to log project action: {}", currentUserInfo.get("error"));
                    } else {
                        // Capture variables for async logging
                        String spaceName = space.getName();
                        String username = currentUserInfo.get("username");
                        AuditActionService.ActionType actionType = AuditActionService.ActionType.DELETE;
                        String ipAddress = currentUserInfo.get("ipAddress");
                        String sessionId = currentUserInfo.get("sessionId");

                        // Execute audit logging asynchronously
                        CompletableFuture.runAsync(() -> {
                            try {
                                this.auditActionService.logProjectAction(
                                        alias,
                                        spaceName,
                                        username,
                                        actionType,
                                        ipAddress,
                                        sessionId
                                );
                            } catch (Exception e) {
                                logger.error("Failed to asynchronously log project action", e);
                            }
                        });
                    }
                } catch (final Exception e) {
                    logger.error("Error during remove repository", e);
                    throw new RuntimeException(e);
                }

                return null;
            });
}
```
### 2. Pages

Add the following code at the end of the specified classes in the mapping libraries `uberfire-structure-backend-7.75.0-SNAPSHOT-sources.jar`:

**Affected JARs:**
- `uberfire-layout-editor-backend-7.75.0-20241022.114530-55-sources.jar`
-
**Affected Classes:**
- `org.uberfire.ext.layout.editor.impl.PerspectiveServicesImpl.java` in `uberfire-layout-editor-backend-7.75.0-20241022.114530-55-sources.jar`

```java
@EJB
private AuditActionService auditActionService;

@Inject
private HttpServletRequest request;

@Inject
private User loggedInUser;

// Add this new method to the class
private Map<String, String> getCurrentUserInfo() {
    Map<String, String> userInfo = new LinkedHashMap<>();

    try {
        // Get username
        String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

        // Get session ID
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "no-session";

        // Get IP address
        String ipAddress = getClientIpAddress(request);

        userInfo.put("username", username);
        userInfo.put("sessionId", sessionId);
        userInfo.put("ipAddress", ipAddress);

        logger.debug("User info collected - username: {}, sessionId: {}, ipAddress: {}",
                username, sessionId, ipAddress);

    } catch (Exception e) {
        logger.error("Error getting user information", e);
        userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
    }

    return userInfo;
}

private String getClientIpAddress(HttpServletRequest request) {
    String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : HEADERS_TO_CHECK) {
        String ip = request.getHeader(header);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // If it's a comma separated list, take the first IP
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }
    }

    return request.getRemoteAddr();
}
```

#### 2.1 Add New Page

Adapt the following method in `org.uberfire.ext.layout.editor.impl.PerspectiveServicesImpl.java` in Jar `uberfire-layout-editor-backend-7.75.0-20241022.114530-55-sources.jar`:

```java
@Override
public Path saveLayoutTemplate(Path perspectivePath, LayoutTemplate layoutTemplate, String commitMessage) {
    String layoutModel = layoutServices.convertLayoutToString(layoutTemplate);
    LayoutEditorModel plugin = new LayoutEditorModel(layoutTemplate.getName(), PluginType.PERSPECTIVE_LAYOUT, perspectivePath, layoutModel);
    pluginServices.saveLayout(plugin, commitMessage);
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = commitMessage.contains("check-in") ? AuditActionService.ActionType.INSERT : AuditActionService.ActionType.UPDATE;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logPageAction(
                        layoutTemplate.getName(),
                        username,
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }
    return perspectivePath;
}
```
#### 2.2 Edit Page
- Same above code of 2.1 Add New Page

#### 2.4 Delete Page

Adapt the following method in `org.uberfire.ext.layout.editor.impl.PerspectiveServicesImpl.java` in Jar `uberfire-layout-editor-backend-7.75.0-20241022.114530-55-sources.jar`:

```java
@Override
public void delete(Path path, String comment) {
    pluginServices.delete(path, comment);
    Plugin plugin = pluginServices.getPluginContent(path);
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.INSERT;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logPageAction(
                        plugin.getName(),
                        username,
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }
}
```
### 3. Servers

Add the following code at the end of the specified classes in the mapping libraries `kie-server-controller-impl-7.75.0-20241017.141353-49-sources.jar`:

**Affected JARs:**
- `kie-server-controller-impl-7.75.0-20241017.141353-49-sources.jar`
-
**Affected Classes:**
- `org.kie.server.controller.impl.service.SpecManagementServiceImpl.java` in `kie-server-controller-impl-7.75.0-20241017.141353-49-sources.jar`

```java
@EJB
private AuditActionService auditActionService;

@Inject
private HttpServletRequest request;

@Inject
private User loggedInUser;

// Add this new method to the class
private Map<String, String> getCurrentUserInfo() {
    Map<String, String> userInfo = new LinkedHashMap<>();

    try {
        // Get username
        String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

        // Get session ID
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "no-session";

        // Get IP address
        String ipAddress = getClientIpAddress(request);

        userInfo.put("username", username);
        userInfo.put("sessionId", sessionId);
        userInfo.put("ipAddress", ipAddress);

        logger.debug("User info collected - username: {}, sessionId: {}, ipAddress: {}",
                username, sessionId, ipAddress);

    } catch (Exception e) {
        logger.error("Error getting user information", e);
        userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
    }

    return userInfo;
}

private String getClientIpAddress(HttpServletRequest request) {
    String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : HEADERS_TO_CHECK) {
        String ip = request.getHeader(header);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // If it's a comma separated list, take the first IP
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }
    }

    return request.getRemoteAddr();
}
```

#### 3.1 Add new Server

Adapt the following method in `org.kie.server.controller.impl.service.SpecManagementServiceImpl.java` in Jar `kie-server-controller-impl-7.75.0-20241017.141353-49-sources.jar`:

```java
@Override
public Path saveLayoutTemplate(Path perspectivePath, LayoutTemplate layoutTemplate, String commitMessage) {
    String layoutModel = layoutServices.convertLayoutToString(layoutTemplate);
    LayoutEditorModel plugin = new LayoutEditorModel(layoutTemplate.getName(), PluginType.PERSPECTIVE_LAYOUT, perspectivePath, layoutModel);
    pluginServices.saveLayout(plugin, commitMessage);
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = commitMessage.contains("check-in") ? AuditActionService.ActionType.INSERT : AuditActionService.ActionType.UPDATE;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logPageAction(
                        layoutTemplate.getName(),
                        username,
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }
    return perspectivePath;
}
```
#### 3.2 Edit server configurations
- Same above code of 2.1 Add New Page

#### 3.4 Delete server configurations

Adapt the following method in `org.uberfire.ext.layout.editor.impl.PerspectiveServicesImpl.java` in Jar `uberfire-layout-editor-backend-7.75.0-20241022.114530-55-sources.jar`:

```java
@Override
public void delete(Path path, String comment) {
    pluginServices.delete(path, comment);
    Plugin plugin = pluginServices.getPluginContent(path);
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.INSERT;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logPageAction(
                        plugin.getName(),
                        username,
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }
}
```
### 5. DataSources

Add the following code at the end of the specified classes in the mapping libraries `kie-wb-common-datasource-mgmt-backend-7.74.1.Final.jar`:

**Affected JARs:**
- `kie-wb-common-datasource-mgmt-backend-7.74.1.Final.jar`
-
**Affected Classes:**
- `org.kie.workbench.common.screens.datasource.management.backend.service.AbstractDefEditorService.java` in `kie-wb-common-datasource-mgmt-backend-7.74.1.Final.jar`

```java
@EJB
private AuditActionService auditActionService;

@Inject
private HttpServletRequest request;

@Inject
private User loggedInUser;

// Add this new method to the class
private Map<String, String> getCurrentUserInfo() {
    Map<String, String> userInfo = new LinkedHashMap<>();

    try {
        // Get username
        String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

        // Get session ID
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "no-session";

        // Get IP address
        String ipAddress = getClientIpAddress(request);

        userInfo.put("username", username);
        userInfo.put("sessionId", sessionId);
        userInfo.put("ipAddress", ipAddress);

        logger.debug("User info collected - username: {}, sessionId: {}, ipAddress: {}",
                username, sessionId, ipAddress);

    } catch (Exception e) {
        logger.error("Error getting user information", e);
        userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
    }

    return userInfo;
}

private String getClientIpAddress(HttpServletRequest request) {
    String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : HEADERS_TO_CHECK) {
        String ip = request.getHeader(header);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // If it's a comma separated list, take the first IP
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }
    }

    return request.getRemoteAddr();
}
```

#### 5.1 Define new dataSource

Adapt the following method in `org.kie.workbench.common.screens.datasource.management.backend.service.AbstractDefEditorService.java` in `kie-wb-common-datasource-mgmt-backend-7.74.1.Final.jar`:

```java
public Path createGlobal(final D def) {
    checkNotNull("def",
            def);

    Path context = serviceHelper.getGlobalDataSourcesContext();
    Path newPath = create(def,
            context);

    fireCreateEvent(def);
    DataSourceDef dataSourceDef = (DataSourceDef) def;
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (currentUserInfo.containsKey("error")) {
    } else {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.INSERT;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");

        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logDataSourceAction(
                        dataSourceDef.getName(),
                        "JDBC Datasource",
                        dataSourceDef.getConnectionURL(),
                        username,
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception e) {
            }
        });
    }
    return newPath;
}

protected Path create(final D def,
                      final Path context) {
    checkNotNull("def",
            def);
    checkNotNull("context",
            context);

    if (def.getUuid() == null) {
        def.setUuid(UUIDGenerator.generateUUID());
    }

    String fileName = buildFileName(def);
    String content = serializeDef(def);

    final org.uberfire.java.nio.file.Path nioPath = Paths.convert(context).resolve(fileName);
    final Path newPath = Paths.convert(nioPath);

    if (ioService.exists(nioPath)) {
        throw new FileAlreadyExistsException(nioPath.toString());
    }

    try {
        ioService.startBatch(nioPath.getFileSystem());
        //create the file.
        ioService.write(nioPath,
                content,
                optionsFactory.makeCommentedOption(""));
        serviceHelper.getDefRegistry().setEntry(newPath,
                def);
    } catch (Exception e) {
        logger.error("It was not possible to create: " + def.getName(),
                e);
        ioService.endBatch();
        throw ExceptionUtilities.handleException(e);
    }

    try {
        //proceed with the deployment
        deploy(def,
                DeploymentOptions.create());
    } catch (Exception e1) {
        logger.error("It was not possible to create: " + def.getName(),
                e1);
        serviceHelper.getDefRegistry().invalidateCache(newPath);
        //the file was created, but the deployment failed.
        try {
            ioService.delete(nioPath);
        } catch (Exception e2) {
            logger.warn("Removal of orphan definition file failed: " + newPath,
                    e2);
        }
        throw ExceptionUtilities.handleException(e1);
    } finally {
        ioService.endBatch();
    }
    return newPath;
}
```

#### 5.2 Delete dataSource

Adapt the following method in `org.kie.workbench.common.screens.datasource.management.backend.service.AbstractDefEditorService.java` in `kie-wb-common-datasource-mgmt-backend-7.74.1.Final.jar`:

```java
public void delete(final Path path,
                   final String comment) {
    checkNotNull("path",
            path);
    D def = null;
    final org.uberfire.java.nio.file.Path nioPath = Paths.convert(path);
    if (ioService.exists(nioPath)) {
        String content = ioService.readAllString(Paths.convert(path));
        def = deserializeDef(content);
        Module module = moduleService.resolveModule(path);
        try {

            I deploymentInfo = readDeploymentInfo(def.getUuid());
            if (deploymentInfo != null) {
                unDeploy(deploymentInfo,
                        UnDeploymentOptions.forcedUnDeployment());
            }
            serviceHelper.getDefRegistry().invalidateCache(path);
            ioService.delete(Paths.convert(path),
                    optionsFactory.makeCommentedOption(comment));
            fireDeleteEvent(def,
                    module);
        } catch (Exception e) {
            throw ExceptionUtilities.handleException(e);
        }
    }

    DataSourceDef dataSourceDef = (DataSourceDef) def;
    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (currentUserInfo.containsKey("error")) {
    } else {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.DELETE;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");

        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logDataSourceAction(
                        dataSourceDef.getName(),
                        "JDBC Datasource",
                        dataSourceDef.getConnectionURL(),
                        username,
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception e) {
            }
        });
    }
}
```
Adapt the following method in `org.kie.workbench.common.screens.datasource.management.backend.service.AbstractDefEditorService.java` in `kie-wb-common-datasource-mgmt-backend-7.74.1.Final.jar`:
### 6. Dashbuilder

Add the following code at the end of the specified classes in the mapping libraries `dashbuilder-services-7.74.1.Final.jar`:

**Affected JARs:**
- `dashbuilder-services-7.74.1.Final.jar`
-
**Affected Classes:**
- `org.dashbuilder.transfer.DataTransferServicesImpl.java` in `dashbuilder-services-7.74.1.Final.jar`

```java
@EJB
private AuditActionService auditActionService;

@Inject
private HttpServletRequest request;

@Inject
private User loggedInUser;

// Add this new method to the class
private Map<String, String> getCurrentUserInfo() {
    Map<String, String> userInfo = new LinkedHashMap<>();

    try {
        // Get username
        String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

        // Get session ID
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "no-session";

        // Get IP address
        String ipAddress = getClientIpAddress(request);

        userInfo.put("username", username);
        userInfo.put("sessionId", sessionId);
        userInfo.put("ipAddress", ipAddress);

        logger.debug("User info collected - username: {}, sessionId: {}, ipAddress: {}",
                username, sessionId, ipAddress);

    } catch (Exception e) {
        logger.error("Error getting user information", e);
        userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
    }

    return userInfo;
}

private String getClientIpAddress(HttpServletRequest request) {
    String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : HEADERS_TO_CHECK) {
        String ip = request.getHeader(header);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // If it's a comma separated list, take the first IP
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }
    }

    return request.getRemoteAddr();
}
```

#### 6.1 Import dashbuilder

Adapt the following method in `org.dashbuilder.transfer.DataTransferServicesImpl.java` in Jar `dashbuilder-services-7.74.1.Final.jar`:

```java
@Override
public List<String> doImport() throws Exception {
    List<String> imported = new ArrayList<>();

    Path root = Paths.get(URI.create(new StringBuilder().append(SpacesAPI.Scheme.GIT)
            .append("://")
            .append(systemFS.getName())
            .append(File.separator)
            .toString()
            .replace("\\", "/")));

    String expectedPath = new StringBuilder().append(File.separator)
            .append(FILE_PATH)
            .append(File.separator)
            .append(IMPORT_FILE_NAME)
            .toString()
            .replace("\\", "/");

    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            if (!path.toString().equalsIgnoreCase(expectedPath)) {
                return FileVisitResult.CONTINUE;
            }

            try {
                imported.addAll(importFiles(path));
                return FileVisitResult.TERMINATE;

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return FileVisitResult.TERMINATE;
            }
        }
    });

    ioService.deleteIfExists(root.resolve(expectedPath));

    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.IMPORT;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logDashbuilderAction(
                        EXPORT_FILE_NAME,
                        username,
                        "",
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }

    return imported;
}
```

#### 6.2 Export dashbuilder related data

Adapt the following method in `org.dashbuilder.transfer.DataTransferServicesImpl.java` in Jar `dashbuilder-services-7.74.1.Final.jar`:

```java
@Override
public String doExport(DataTransferExportModel exportModel) throws java.io.IOException {
    String zipLocation = new StringBuilder().append(System.getProperty("java.io.tmpdir"))
            .append(File.separator)
            .append(FILE_PATH)
            .append(File.separator)
            .append(EXPORT_FILE_NAME)
            .toString()
            .replace("\\", "/");

    Predicate<Path> readmeFilter = p -> p.toString().toLowerCase().endsWith("readme.md");
    Predicate<Path> datasetsFilter = def -> true;
    Predicate<Path> pagesFilter = page -> true;
    boolean exportNavigation = true;

    if (!exportModel.isExportAll()) {
        datasetsFilter = filterDatasets(exportModel.getDatasetDefinitions());
        pagesFilter = filterPages(exportModel.getPages());
        exportNavigation = exportModel.isExportNavigation();
    }

    new File(zipLocation).getParentFile().mkdirs();
    FileOutputStream fos = new FileOutputStream(zipLocation);
    ZipOutputStream zos = new ZipOutputStream(fos);

    zipFileSystem(datasetsFS, zos, readmeFilter.or(datasetsFilter));
    zipFileSystem(perspectivesFS, zos, readmeFilter.or(pagesFilter));

    if (exportNavigation) {
        zipFileSystem(navigationFS, zos, p -> true);
    } else {
        zipFileSystem(navigationFS, zos, readmeFilter);
    }

    if (externalComponentLoader.isExternalComponentsEnabled()) {
        String componentsPath = externalComponentLoader.getExternalComponentsDir();

        if (componentsPath != null && exists(componentsPath)) {
            Path componentsBasePath = Paths.get(new StringBuilder().append(SpacesAPI.Scheme.FILE)
                    .append("://")
                    .append(componentsPath)
                    .toString());
            Predicate<String> pagesComponentsFilter = page -> exportModel.isExportAll() || exportModel.getPages().contains(page);
            layoutComponentsHelper.findComponentsInTemplates(pagesComponentsFilter)
                    .stream()
                    .map(c -> componentsBasePath.resolve(c))
                    .filter(Files::exists)
                    .forEach(c -> {
                        Path componentPath = componentsBasePath.resolve(c);
                        zipComponentFiles(componentsBasePath,
                                componentPath,
                                zos,
                                p -> true);
                    });
        }
    }

    zipFile(createVersionFile(), "VERSION", zos);

    zos.close();
    fos.close();

    moveZipToFileSystem(zipLocation, systemFS);

    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.EXPORT;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logDashbuilderAction(
                        EXPORT_FILE_NAME,
                        username,
                        "",
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }

    return new StringBuilder().append(SpacesAPI.Scheme.GIT)
            .append("://")
            .append(systemFS.getName())
            .append(File.separator)
            .append(FILE_PATH)
            .append(File.separator)
            .append(EXPORT_FILE_NAME)
            .toString()
            .replace("\\", "/");
}
```
### 7. Security

Add the following code at the end of the specified classes in the mapping libraries `dashbuilder-services-7.74.1.Final.jar`:

**Affected JARs:**
- `uberfire-backend-server-7.75.0-20241022.112904-55-sources.jar`
-
**Affected Classes:**
- `org.uberfire.backend.server.authz.AuthorizationServiceImpl.java` in `uberfire-backend-server-7.75.0-20241022.112904-55-sources.jar`

```java
@EJB
private AuditActionService auditActionService;

@Inject
private HttpServletRequest request;

@Inject
private User loggedInUser;

// Add this new method to the class
private Map<String, String> getCurrentUserInfo() {
    Map<String, String> userInfo = new LinkedHashMap<>();

    try {
        // Get username
        String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

        // Get session ID
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "no-session";

        // Get IP address
        String ipAddress = getClientIpAddress(request);

        userInfo.put("username", username);
        userInfo.put("sessionId", sessionId);
        userInfo.put("ipAddress", ipAddress);

        logger.debug("User info collected - username: {}, sessionId: {}, ipAddress: {}",
                username, sessionId, ipAddress);

    } catch (Exception e) {
        logger.error("Error getting user information", e);
        userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
    }

    return userInfo;
}

private String getClientIpAddress(HttpServletRequest request) {
    String[] HEADERS_TO_CHECK = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    for (String header : HEADERS_TO_CHECK) {
        String ip = request.getHeader(header);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // If it's a comma separated list, take the first IP
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }
    }

    return request.getRemoteAddr();
}
```

#### 7.1 Change security settings on roles

Adapt the following method in `org.dashbuilder.transfer.DataTransferServicesImpl.java` in Jar `uberfire-backend-server-7.75.0-20241022.112904-55-sources.jar`:

```java
@Override
public List<String> doImport() throws Exception {
    List<String> imported = new ArrayList<>();

    Path root = Paths.get(URI.create(new StringBuilder().append(SpacesAPI.Scheme.GIT)
            .append("://")
            .append(systemFS.getName())
            .append(File.separator)
            .toString()
            .replace("\\", "/")));

    String expectedPath = new StringBuilder().append(File.separator)
            .append(FILE_PATH)
            .append(File.separator)
            .append(IMPORT_FILE_NAME)
            .toString()
            .replace("\\", "/");

    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            if (!path.toString().equalsIgnoreCase(expectedPath)) {
                return FileVisitResult.CONTINUE;
            }

            try {
                imported.addAll(importFiles(path));
                return FileVisitResult.TERMINATE;

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return FileVisitResult.TERMINATE;
            }
        }
    });

    ioService.deleteIfExists(root.resolve(expectedPath));

    Map<String, String> currentUserInfo = getCurrentUserInfo();
    if (!currentUserInfo.containsKey("error")) {
        // Capture variables for async logging
        String username = currentUserInfo.get("username");
        AuditActionService.ActionType actionType = AuditActionService.ActionType.IMPORT;
        String ipAddress = currentUserInfo.get("ipAddress");
        String sessionId = currentUserInfo.get("sessionId");
        // Execute audit logging asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                this.auditActionService.logDashbuilderAction(
                        EXPORT_FILE_NAME,
                        username,
                        "",
                        actionType,
                        ipAddress,
                        sessionId
                );
            } catch (Exception ignored) {
            }
        });
    }

    return imported;
}
```

#### 7.2 Change security settings on groups

Adapt the following method in `org.uberfire.backend.server.authz.AuthorizationServiceImpl.java` in Jar `uberfire-backend-server-7.75.0-20241022.112904-55-sources.jar`:

```java
@Override
public void savePolicy(AuthorizationPolicy policy) {
    // Retrieve old and new authorization policies
    AuthorizationPolicy oldPolicy = storage.loadPolicy();
    AuthorizationPolicy newPolicy = policy;

    // Save new policy and set it in the permission manager
    storage.savePolicy(newPolicy);
    permissionManager.setAuthorizationPolicy(newPolicy);

    // Create audit log JSON object
    JSONObject auditLog = new JSONObject();

    // Process roles and permissions for audit log
    processPermissionsAudit(oldPolicy, newPolicy, auditLog);

    // Log the audit details
    Logger.getLogger(getClass().getName()).info("Authorization Policy Audit: " + auditLog.toString());

    savedEvent.fire(new AuthorizationPolicySavedEvent(newPolicy));
}
```

Add the following methods into the same class

```java

    private void processPermissionsAudit(AuthorizationPolicy oldPolicy,
                                         AuthorizationPolicy newPolicy,
                                         JSONObject auditLog) {
        Set<Role> roles = oldPolicy.getRoles();

        for (Role role : roles) {
            JSONArray auditEntries = new JSONArray();
            Map<String, String> oldPermissions = extractPermissions(oldPolicy, role);
            Map<String, String> newPermissions = extractPermissions(newPolicy, role);

            // Combine the permission names from both old and new maps
            Set<String> allPermissionNames = new HashSet<>(oldPermissions.keySet());
            allPermissionNames.addAll(newPermissions.keySet());

            // Collect changed permissions
            for (String permissionName : allPermissionNames) {
                String oldAccess = oldPermissions.getOrDefault(permissionName, "NOT_SET");
                String newAccess = newPermissions.getOrDefault(permissionName, "NOT_SET");

                if (!oldAccess.equals(newAccess)) {
                    JSONObject entry = new JSONObject();
                    entry.put("permission", permissionName);
                    entry.put("oldAccess", oldAccess);
                    entry.put("newAccess", newAccess);
                    auditEntries.add(entry);
                }
            }

            if (!auditEntries.isEmpty()) {
                Map<String, String> currentUserInfo = getCurrentUserInfo();
                if (!currentUserInfo.containsKey("error")) {
                    String username = currentUserInfo.get("username");
                    String ipAddress = currentUserInfo.get("ipAddress");
                    String sessionId = currentUserInfo.get("sessionId");

                    // Prepare old and new JSON structures
                    JSONObject oldValue = new JSONObject();
                    JSONArray oldPermissionsArray = new JSONArray();
                    JSONObject newValue = new JSONObject();
                    JSONArray newPermissionsArray = new JSONArray();

                    // In JSON-simple there is no getJSONObject() so we cast the result of get(i)
                    for (int i = 0; i < auditEntries.size(); i++) {
                        JSONObject entry = (JSONObject) auditEntries.get(i);

                        JSONObject oldEntry = new JSONObject();
                        oldEntry.put("permission", entry.get("permission"));
                        oldEntry.put("access", entry.get("oldAccess"));
                        oldPermissionsArray.add(oldEntry);

                        JSONObject newEntry = new JSONObject();
                        newEntry.put("permission", entry.get("permission"));
                        newEntry.put("access", entry.get("newAccess"));
                        newPermissionsArray.add(newEntry);
                    }

                    oldValue.put("Permissions", oldPermissionsArray);
                    newValue.put("Permissions", newPermissionsArray);

                    // Set audit trail parameters
                    String actionType = AuditActionService.ActionType.UPDATE.toString();

                    // Async audit logging using CompletableFuture
                    CompletableFuture.runAsync(() -> {
                        try {
                            auditActionService.logSecuritySettingsAction(
                                    role.getName(),
                                    actionType,
                                    username,
                                    String.valueOf(oldValue),
                                    String.valueOf(newValue),
                                    ipAddress,
                                    sessionId
                            );
                        } catch (Exception e) {
                        }
                    });
                }
            }
        }
    }

    private Map<String, String> extractPermissions(AuthorizationPolicy policy, Role role) {
        Map<String, String> permissionsMap = new HashMap<>();
        Collection<Permission> permissionsList = policy.getPermissions(role).collection();

        for (Permission permission : permissionsList) {
            permissionsMap.put(permission.getName(), permission.getResult().toString());
        }

        return permissionsMap;
    }

    @EJB
    private AuditActionService auditActionService;

    @Inject
    private HttpServletRequest request;

    @Inject
    private User loggedInUser;

    // Add this new method to the class
    private Map<String, String> getCurrentUserInfo() {
        Map<String, String> userInfo = new LinkedHashMap<>();

        try {
            // Get username
            String username = loggedInUser != null ? loggedInUser.getIdentifier() : "unknown";

            // Get session ID
            HttpSession session = request.getSession(false);
            String sessionId = session != null ? session.getId() : "no-session";

            // Get IP address
            String ipAddress = getClientIpAddress(request);

            userInfo.put("username", username);
            userInfo.put("sessionId", sessionId);
            userInfo.put("ipAddress", ipAddress);

        } catch (Exception e) {
            userInfo.put("error", "Failed to retrieve user information: " + e.getMessage());
        }

        return userInfo;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] HEADERS_TO_CHECK = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : HEADERS_TO_CHECK) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                // If it's a comma separated list, take the first IP
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }

        return request.getRemoteAddr();
    }
```