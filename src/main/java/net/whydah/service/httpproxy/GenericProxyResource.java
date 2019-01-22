package net.whydah.service.httpproxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.auth.SPAKeyStoreRepository;
import net.whydah.sso.application.types.ApplicationToken;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Optional;

import static javax.ws.rs.core.Response.*;
import static net.whydah.service.httpproxy.GenericProxyResource.API_PATH;


@RestController
@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class GenericProxyResource {
    static final String API_PATH = "/httpproxy/generic";
    private static final Logger log = LoggerFactory.getLogger(GenericProxyResource.class);

    private static final String BEARER_TOKEN_PREFIX = "bearer";

    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;
    private final SPAKeyStoreRepository spaKeyStoreRepository;
    private final ProxySpecificationRepository proxySpecifications;

    private final String logonServiceBaseUrl;
    private final String securitytokenservice;

    @Autowired
    @Configure
    public GenericProxyResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository,
                                SPAKeyStoreRepository spaKeyStoreRepository, ProxySpecificationRepository proxySpecifications,
                                @Configuration("logonservice") String logonServiceBaseUrl,
                                @Configuration("securitytokenservice") String securitytokenservice) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
        this.spaKeyStoreRepository = spaKeyStoreRepository;
        this.proxySpecifications = proxySpecifications;
        this.logonServiceBaseUrl = logonServiceBaseUrl;
        this.securitytokenservice = securitytokenservice;
    }

    /**
     * Uses the userTokenId from path
     */
    @GET
    @Path("{secret}/{userTokenId}/{proxySpecificationName}")
    public Response getGeneric(@Context UriInfo uriInfo,
                               @Context HttpHeaders httpheaders,
                               @PathParam("secret") String secret,
                               @PathParam("userTokenId") String userTokenId,
                               @PathParam("proxySpecificationName") String proxySpecificationName) throws CloneNotSupportedException {

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        if (applicationToken == null) {
            log.warn("Unable to locate application session from secret, returning FORBIDDEN");
            return status(Status.FORBIDDEN).build();
        }
        log.debug("GET invoked with proxySpecificationName: {}", proxySpecificationName);
        Response response = proxyRequest(proxySpecificationName, applicationToken, userTokenId, httpheaders);
        return response;
    }

    /**
     * Retrieves the userTokenId from the provided JWT
     */
    @GET
    @Path("{secret}/{proxySpecificationName}")
    public Response getGenericWithJWT(@Context UriInfo uriInfo,
                                      @Context HttpHeaders httpheaders,
                                      @PathParam("secret") String secret,
                                      @PathParam("proxySpecificationName") String proxySpecificationName,
                                      @HeaderParam("Authorization") String authorizationHeader) throws CloneNotSupportedException {

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        if (applicationToken == null) {
            log.warn("Unable to locate application session from secret, returning FORBIDDEN");
            return status(Status.FORBIDDEN).build();
        }

        log.debug("GET invoked with proxySpecificationName: {}", proxySpecificationName);
        //read the authorization header
        String userTokenId;
        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(BEARER_TOKEN_PREFIX + " ")) {
            String jwt = authorizationHeader.substring(BEARER_TOKEN_PREFIX.length()).trim();
            userTokenId = spaKeyStoreRepository.getUserTokenIdFromJWT(jwt);
            if (userTokenId == null || userTokenId.isEmpty()) {
                return status(Status.FORBIDDEN).build();
            }
        } else {
            return status(Status.FORBIDDEN).build();
        }

        return proxyRequest(proxySpecificationName, applicationToken, userTokenId, httpheaders);
    }


    private Response proxyRequest(final String proxySpecificationName,
                                  final ApplicationToken applicationToken, final String userTokenId,
                                  final HttpHeaders httpheaders) throws CloneNotSupportedException {
        Optional<ProxySpecification> optionalSpecification = proxySpecifications.get(proxySpecificationName);
        if (!optionalSpecification.isPresent()) {
            log.info("ProxySpecification not found for targetName: {}, proxySpecificationName{}");
            return Response.status(Status.NOT_FOUND).build();
        }
        ProxySpecification specification = TemplateUtil.getCloneWithReplacements(
                optionalSpecification.get(), applicationToken.getApplicationTokenId(),
                userTokenId, logonServiceBaseUrl, securitytokenservice
        );

        Response response = new CommandGenericGetProxy(
                specification,
                httpheaders.getRequestHeaders()
        ).execute();

        return fromResponse(response)
                .header("Access-Control-Allow-Origin", credentialStore.findRedirectUrl(applicationToken.getApplicationName()))
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

}
