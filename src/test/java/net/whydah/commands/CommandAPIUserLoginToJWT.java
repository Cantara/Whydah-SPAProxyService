package net.whydah.commands;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.commands.basecommands.MyBaseHttpPostHystrixCommand;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Random;

public class CommandAPIUserLoginToJWT  extends MyBaseHttpPostHystrixCommand<String> {

    String contentType= MediaType.APPLICATION_JSON;
    static Random r = new Random();
    String payload="";
    String secret="";


    public CommandAPIUserLoginToJWT(String url,String secret, String payload) {
        super(URI.create(url), "CommandAPIUserLoginToJWT" + r.nextInt(100),3000);
        this.payload=payload;
        this.secret=secret;
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
        return ""+secret+"/authenticate_user";
    }


    @Override
    protected String dealWithResponse(String responseBody) {
        //return "200" + ":" + super.dealWithResponse(response);
        return responseBody;
//        return super.dealWithResponse(response);
    }

}
