# Whydah-SPAProxyService

![Build Status](https://jenkins.capraconsulting.no/buildStatus/icon?job=Whydah-SPAProxyService) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)  [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Whydah-SPAProxyService/badge.svg)](https://snyk.io/test/github/Cantara/Whydah-SPAProxyService)


The SPAProxyService is an optional Whydah module to support Whydah application sessions for Single-Page Applications (SPAs).
This module is the backend/API support module for the Whydah SPA JavaScript NPM Library.


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

### SPA SSO Flow 

1. use the ../load/{myapp} redirect flow
    * Looks for SSO session
    * Create 302-redirect with two secrets
      * secret 1 is the code=xxx param on the redirect-URI on the Location URL in the 302 response
      * secret 2 is in an embedded cookie in the 302-request
2. (Optional) if you are unable to get/process the cookie
    * do a js XREF request to  ../load/{myapp}/ping
      * this will pick up the cookie, and return it in a json response
3. to get the secret (initial application ticket) fo a secret1 XOR secret2
4. if the user is recognized, use the ticket from the 302-request and call /{secret}/get_token_from_ticket/{ticket}
    * You will get a JWT token back with the user roles for your application
5. to log inn from username/password in your SPA, call /{secret}/authenticate_user/
    * You will get a JWT token back with the user roles for your application
 
 
To make {myapp} with, it has to be configured in the system, with a redirect URI pointing to your SPA application.
 
#### Example for POST /api/{secret}/authenticate_user/  Json user credentials
```json
{
  "username": "myUserName",
  "password": "myPassword"
}
```

#### Example for POST /get_token_from_ticket/{ticket}   return JWT token
```text
~~~~~~~~~ JWT Header ~~~~~~~
JWT Header : {"alg":"RS256"}
~~~~~~~~~ JWT Body ~~~~~~~
JWT Body : {"sub":"useradmin","jti":"8bdf8ad8-b7af-4561-93be-58d420c3ea54","iss":"","aud":"","iat":1521489954,"userticket":"27eefeff-606d-4e58-9177-63373f36e6d4","exp":1521509954}
```

### Generic HTTP Proxy support
There might be cases where you wish to expose endpoints belonging to other applications through SPAProxy.

The following endpoints are used to proxy requests based on a `ProxySpecification`:
 * `../generic/{secret}/{userTokenId/{proxySpecificationName}`
 * `../generic/{secret}/{proxySpecificationName}`

The first endpoint expects a usedTokenId in the path, while the second expects a valid JWT Bearer token in the Authorization header.

Currently only `GET` requests are supported, while `POST` support is planned.

Example configuration get-sts-validate.json:

```
{
  "command_url": "#securitytokenservice#applicationTokenId/validate",
  "command_contenttype": "application/json",
  "command_http_authstring": "",
  "command_http_post": false,
  "command_timeout_milliseconds": 2000,
  "command_template": "",
  "command_replacement_map": {},
  "command_response_map": {}
}
```

The `proxySpecificationName` is derived from the file-name pattern: `{HTTPVerb}-{proxySpecificationName}.json`.

In the example above clients may call: GET `../generic/{secret}/{userTokenId/sts-validate`.
SPAProxy will replace call the url: `#securitytokenservice#applicationTokenId/validate`.
`#securitytokenservice` is replaced by `securitytokenservice` from `application.properties`.
`#applicationTokenId` is replaced by the applicationTokenId corresponding to the clients `{secret}`.
The example would execute a GET to STS with the clients applicationTokenId to the validate endpoint.
The response from STS is proxied to the client.


In addition to the two replacement variables above, it is possible to use:
`#logonservice` which will be replaced by `logonservice` from `application.properties`
`#userTokenId` which will be replaced by the `{userTokenId}` provided by the client through path, or JWT.

SPAProxy will execute the GET in a Hystrix command. `command_timeout_milliseconds` determine the Hystrix command timeout.

By default SPAProxy will look for ProxySpecification files in the `proxy-specifications/` directory.
The location may be overridden by setting `proxy.specification.directory`


### Forwarding query params
The SPA Proxy will forward all query params by default. It will not forwards those that are specified in the setting `proxy.queryparams.disallowed`.