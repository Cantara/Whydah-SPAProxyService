package net.whydah.commands;

import com.github.kevinsawicki.http.HttpRequest;

import java.awt.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.whydah.commands.basecommands.MyBaseHttpPostHystrixCommand;

import javax.ws.rs.core.MediaType;


public class CommandResolveTicketToJWT  extends MyBaseHttpPostHystrixCommand<String> {

    String contentType= MediaType.APPLICATION_JSON;
    static Random r = new Random();
    String payload="";
    String secret="";
    String ticket="";


    public CommandResolveTicketToJWT(String url,String secret, String ticket,String payload) {
        super(URI.create(url), "CommandResolveTicketToJWT" + r.nextInt(100), HystrixCommandTimeoutConfig.defaultTimeout);
        this.payload=payload;
        this.secret=secret;
        this.ticket=ticket;
    }


    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        //super.dealWithRequestBeforeSend(request);

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
        return "/"+secret+"/get_token_from_ticket/"+ticket;
    }


    @Override
    protected String dealWithResponse(String responseBody) {
        //return "200" + ":" + super.dealWithResponse(response);
        return responseBody;
//        return super.dealWithResponse(response);
    }

}
