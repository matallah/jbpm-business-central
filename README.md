# Project Name

## Overview

This project integrates `FileUploadFilter` for managing file uploads and `FormsDocumentServlet` for handling form documents. This document outlines the steps to integrate both components into your project.

## Prerequisites

Ensure you have the following prerequisites:

- A working environment of the project with the specified directory structure.
- Java Development Kit (JDK) set up properly.
- Access to the source code directories mentioned in the paths below.

## Integration Steps

### 1. FileUploadFilter Integration

#### 1.1 File Placement

Place the `FileUploadFilter.class` in the following directory:

```
kie-wb-distributions\business-central-parent\business-central-webapp\src\main\java\org\kie\bc\client
```

#### 1.2 Modify web.xml Configuration

In the `web.xml` file located at:

```
kie-wb-distributions\business-central-parent\business-central-webapp\src\main\webapp\WEB-INF
```

Add the following configuration:

```xml
<filter>
    <filter-name>FileUploadFilter</filter-name>
    <filter-class>org.kie.bc.client.FileUploadFilter</filter-class>
</filter>

<filter-mapping>
    <filter-name>FileUploadFilter</filter-name>
    <url-pattern>*</url-pattern>
</filter-mapping>

<context-param>
    <param-name>allowedExtensions</param-name>
    <param-value>xml,pdf</param-value>
</context-param>
```

- `FileUploadFilter` is the name of the filter being configured.
- `filter-class` should point to the full class name: `org.kie.bc.client.FileUploadFilter`.
- `url-pattern` is set to `*`, meaning the filter will apply to all incoming requests.
- `allowedExtensions` defines the allowed file types (in this case, `xml` and `pdf`).


Ensure that the class is in the correct package and directory for it to be utilized in the project.

## Testing the Integration

After performing the integration steps, follow these instructions to test:

1. **File Upload**: Test uploading files (with extensions `xml` and `pdf`) to confirm that the `FileUploadFilter` is correctly processing and allowing only the specified file types.
2. **Document Handling**: Ensure that `FormsDocumentServlet` is handling document-related requests as expected.