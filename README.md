
---

# README

## Requirements
- Java 1.8
- Maven
- Business Central WAR file

## Steps

1. **Build the Project**
   - Open terminal.
   - Navigate to the project folder.
   - Run: 
     ```bash
     mvn clean install
     ```
   - Find the JAR in `target/`.

2. **Deploy the JAR**
   - Locate `business-central.war`.
   - Extract it: 
     ```bash
     unzip business-central.war -d business-central
     ```
   - Copy JAR to `WEB-INF/lib`:
     ```bash
     cp target/<jar-name>.jar business-central/WEB-INF/lib/
     ```
   - Optional: Re-create WAR:
     ```bash
     cd business-central
     zip -r ../business-central.war .
     ```

3. **Deploy WAR**
   - Deploy `business-central.war` to your server.

---
