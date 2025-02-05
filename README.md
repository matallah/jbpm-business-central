Sure, I've adapted and enhanced your README file:

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

**Classes:**
- `org.guvnor.structure.backend.repositories.RepositoryServiceImpl.java`
- `org.guvnor.structure.backend.organizationalunit.config.SpaceConfigStorageImpl.java`

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

Adapt the following method in `org.guvnor.structure.backend.organizationalunit.config.SpaceConfigStorageImpl.java` in Jar `kie-wb-common-services-backend-7.75.0-20241022.122359-92-sources.jar
`:

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