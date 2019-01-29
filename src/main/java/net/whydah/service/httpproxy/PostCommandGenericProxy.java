package net.whydah.service.httpproxy;

import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PostCommandGenericProxy extends HystrixCommand<Response> {
    private static final String GROUP_KEY = "GetCommandGenericProxy";
    final private URI uri;
    final private MultivaluedMap<String, String> headers;
    final private String requestBody;
    private final String contentType;


    public PostCommandGenericProxy(ProxySpecification specification, MultivaluedMap<String, String> headers) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(specification.getCommand_timeout_milliseconds()))
        );
        specification.resolveVariables(Collections.emptyMap(),Collections.emptyMap(), Collections.emptyMap());
        this.uri = URI.create(specification.getCommand_url());
        this.headers = headers;
        this.requestBody = specification.getCommand_template();
        this.contentType = specification.getCommand_contenttype();
    }


    @Override
    protected Response run() throws Exception {
        URL url = uri.toURL();
        HttpRequest httpRequest = HttpRequest.post(url);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequest.header(entry.getKey(), value);
            }
        }
        httpRequest.acceptEncoding("identity");
        httpRequest.contentType(contentType);
        try {
            return executePost(httpRequest, requestBody);
        } finally {
            httpRequest.disconnect();
        }
    }

    private Response executePost(final HttpRequest request, String body) {
        request.send(body);
        int responseCode = request.code();
        String responseBody = request.body();

        Map<String, List<String>> headers = request.headers();

        Response.ResponseBuilder responseBuilder = Response.status(responseCode)
                .entity(responseBody);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                if (entry.getKey() != null && value != null) {
                    responseBuilder.header(entry.getKey(), value);
                }
            }
        }

        return responseBuilder.build();
    }
}
