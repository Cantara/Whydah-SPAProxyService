package net.whydah.service.httpproxy;

import com.github.kevinsawicki.http.HttpRequest;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CommandGenericGetProxy extends HystrixCommand<Response> {
    private static final String GROUP_KEY = "CommandGenericGetProxy";
    final private URI uri;
    final private MultivaluedMap<String, String> headers;


    public CommandGenericGetProxy(ProxySpecification specification, MultivaluedMap<String, String> headers) {
        super(com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(specification.getCommand_timeout_milliseconds()))
        );
        this.uri = URI.create(specification.getCommand_url());
        this.headers = headers;
    }


    @Override
    protected Response run() throws Exception {
        URL url = uri.toURL();
        HttpRequest httpRequest = HttpRequest.get(url);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequest.header(entry.getKey(), value);
            }
        }
        httpRequest.acceptEncoding("identity");

        try {
            return executeGet(httpRequest);
        } finally {
            httpRequest.disconnect();
        }
    }

    private Response executeGet(final HttpRequest request) {
        int responseCode = request.code();
        String body = request.body();

        Map<String, List<String>> headers = request.headers();

        Response.ResponseBuilder responseBuilder = Response.status(responseCode)
                .entity(body);
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
