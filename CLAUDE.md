# Whydah-SPAProxyService

## Purpose
Backend proxy service that supports Whydah application sessions for Single-Page Applications (SPAs). Manages application token lifecycle on behalf of SPAs that cannot securely hold credentials client-side. Companion to the Whydah SPA JavaScript NPM Library.

## Tech Stack
- Language: Java 21
- Framework: Jersey 2.x, Jetty 9.x, Spring 5.x
- Build: Maven
- Key dependencies: Whydah-Admin-SDK, Hystrix, Jackson, Jersey

## Architecture
Standalone microservice that acts as a secure proxy between SPAs and Whydah's SecurityTokenService. Handles application session creation and renewal, SSO session detection, and secure code exchange. Uses a two-secret pattern for SPA authentication: one secret in the redirect URL code parameter, another in a secure cookie.

## Key Entry Points
- `/proxy/health` - Health and status endpoint
- `/proxy/load/{appName}` - SPA SSO flow entry point
- `pom.xml` - Maven coordinates: `net.whydah.service:Whydah-SPAProxyService`

## Development
```bash
# Build
mvn clean install

# Run
java -jar target/Whydah-SPAProxyService-*.jar
```

## Domain Context
SPA authentication infrastructure for the Whydah IAM ecosystem. Solves the challenge of secure authentication for browser-based single-page applications that cannot safely store application secrets.
