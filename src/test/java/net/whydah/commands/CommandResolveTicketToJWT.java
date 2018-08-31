package net.whydah.commands;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.commands.basecommands.MyBaseHttpPostHystrixCommand;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Random;

public class CommandResolveTicketToJWT extends MyBaseHttpPostHystrixCommand<String> {
    static Random r = new Random();

    String contentType = MediaType.APPLICATION_JSON;
    String payload = "";
    String secret = "";
    String ticket = "";

    public CommandResolveTicketToJWT(String url, String secret, String ticket, String payload) {
        super(URI.create(url), "CommandResolveTicketToJWT" + r.nextInt(100),
                HystrixCommandTimeoutConfig.defaultTimeout);
        this.payload = payload;
        this.secret = secret;
        this.ticket = ticket;
    }

    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        return request.contentType(contentType).accept(contentType).send(this.payload);
    }

    @Override
    protected String dealWithFailedResponse(String responseBody, int statusCode) {
        if (statusCode < 300 && statusCode >= 200) {
            return responseBody;
        }
        return "StatusCode:" + statusCode + ":" + responseBody;
    }

    @Override
    protected String getTargetPath() {
        return "/" + secret + "/get_token_from_ticket/" + ticket;
    }

    @Override
    protected String dealWithResponse(String responseBody) {
        return responseBody;
    }
}
