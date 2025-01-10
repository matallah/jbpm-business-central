FileUploadFilter.java
kie-wb-distributions\business-central-parent\business-central-webapp\src\main\java\org\kie\bc\client

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


kie-wb-distributions\business-central-parent\business-central-webapp\src\main\webapp\WEB-INF



FormsDocumentServlet
kie-wb-common-forms-jbpm-integration-backend-7.75.0-SNAPSHOT-sources\org\kie\workbench\common\forms\jbpm\server\service\impl\documents\storage