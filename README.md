# Whydah-SPAProxyService



![Build Status](https://jenkins.capraconsulting.no/buildStatus/icon?job=Whydah-SPAProxyService) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)  [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Whydah-SPAProxyService/badge.svg)](https://snyk.io/test/github/Cantara/Whydah-OAuth2Service)


The SPAProxyService is an optinal Whydah module to support whydah application sessions for single-page applications. This is the backend/API support module for the Whydah SPA javascript npm library.


* use:  https://spaproxy.cantara.no/proxy/health
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

  "applicationSessionStatistics": {
        "Whydah-SSOLoginWebApp" : 2 
     }
}
```

### Flow documentation
1. First ordered list item
2. Another item
⋅⋅* Unordered sub-list. 

 1. use the ../load/myapp redirect flow
 ⋅⋅* Looks for SSO session
 ..* Create 302-redirect with two secrets
 ..** secret 1 is the code=xxx param on the redirect-URI on the Location URL in the 302 response
 ..** secret 2 is in an embedded cookie in the 302-request
 2. if you are unable to get/process the cookie
 .. * do a js XREF request to  ../load/myapp/ping
 .. ** this will pick up the cookie, and retuen it in a json response
 3. to get the secret (initial application ticket) fo a secret1 XOR secret2   
 4. if the user is recognized, use the ticket from the 302-request and call /{secret}/get_token_from_ticket/{ticket}
 .. * You will get a JWT token back with the user roles for your application
 5. to log inn from username/password in your SPA, call /{secret}/authenticate_user/
 .. * You will get a JWT token back with the user roles for your application
 
 
#### Example for /proxy/health 
```json
{
  "username": "myUserName",
  "password": "myPassword"
}
```
