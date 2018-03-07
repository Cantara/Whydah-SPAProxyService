package net.whydah.commands;

import com.github.kevinsawicki.http.HttpRequest;
import net.whydah.commands.basecommands.MyBaseHttpGetHystrixCommand;

import java.net.URI;
import java.util.Random;

public class CommandGetProxyResponse  extends MyBaseHttpGetHystrixCommand<String> {

    String contentType = "text/xml;charset=UTF-8";
    String httpAuthorizationString;
    static Random r = new Random();

    public CommandGetProxyResponse(String uri) {
        super(URI.create(uri),"CommandGetProxyResponse_" + r.nextInt(100),3000);
    }


    private String headerName(String header) {
        return header.substring(0, header.indexOf(":")).trim();
    }

    private String headerValue(String header) {
        return header.substring(header.indexOf(":") + 1, header.length()).trim();
    }
    @Override
    protected HttpRequest dealWithRequestBeforeSend(HttpRequest request) {
        // request= super.dealWithRequestBeforeSend(request);
            return request;
    }


    @Override
    protected String dealWithFailedResponse(String responseBody, int statusCode) {
        if (statusCode < 300 && statusCode >= 200) {
            return responseBody;
        }
        if (statusCode == 302) {
            log.info("ResponseBody: {}",responseBody);
            if (responseBody.contains("code")){
                responseBody = "" +
                        "{" +
//                        "\"responseBody\": \""+ responseBody+ "\", \n" +
                        "\"code\": \""+ responseBody.substring(responseBody.indexOf("code=") + 5, responseBody.indexOf("&ticket"))+ "\", \n" +
                        "\"cookievalue\": \""+ responseBody.substring(responseBody.indexOf("test=") + 5, responseBody.indexOf(";expires"))+ "\", \n" +
                        "\"ticket\": \""+ responseBody.substring(responseBody.indexOf("ticket=") + 7, responseBody.length()-2)+ "\"}" +
                        "";
            }
            return responseBody;
        }
        return "StatusCode:" + statusCode + ":" + responseBody;
    }

    @Override
    protected String dealWithResponse(String response) {
        //return "200" + ":" + super.dealWithResponse(response);
        return super.dealWithResponse(response);
    }


    @Override
    protected String getTargetPath() {
        return "";
    }
}
