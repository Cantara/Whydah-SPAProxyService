# Whydah-SPAProxyService



![Build Status](https://jenkins.capraconsulting.no/buildStatus/icon?job=Whydah-SPAProxyService) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)  [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Whydah-SPAProxyService/badge.svg)](https://snyk.io/test/github/Cantara/Whydah-OAuth2Service)


The SPAProxyService is an optinal Whydah module to support whydah application sessions for single-page applications. This is the backend/API support module for the Whydah SPA javascript npm library.


* use:  https://spaproxy.cantara.no/proxy/load/My-SPA-Application


#### Example for /proxy/health 
```json
{
  "Status": "OK",
  "Version": "0.2.8-SNAPSHOT [Whydah-SPAProxyService - 10.30.1.233  fe80:0:0:0:cfc:f5ff:fedd:1770%eth0  10.30.1.233  0:0:0:0:0:0:0:1%lo  127.0.0.1]",
  "DEFCON": "DEFCON5",
  "STS": "https://whydahdev.cantara.no/tokenservice/",
  "UAS": "https://whydahdev.cantara.no/useradminservice/",
  "hasApplicationToken": "true",
  "hasValidApplicationToken": "true",
  "hasApplicationsMetadata": "true",
  "ConfiguredApplications": "14",
  "now": "2018-03-12T12:14:55.535Z",
  "running since": "2018-03-12T12:06:03.327Z",

  "applicationSessionStatistics": "{
  \"Whydah-SSOLoginWebApp\" : 2 }"
}
```