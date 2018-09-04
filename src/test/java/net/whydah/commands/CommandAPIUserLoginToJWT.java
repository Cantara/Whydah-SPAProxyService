package net.whydah.commands;

import net.whydah.commands.basecommands.MyBaseHttpPostHystrixCommand;

import java.net.URI;
import java.util.Map;
import java.util.Random;

public class CommandAPIUserLoginToJWT extends MyBaseHttpPostHystrixCommand<String> {
    static Random r = new Random();

    String userName = "";
    String password = "";
    String secret = "";

    public CommandAPIUserLoginToJWT(String url, String secret, String userName, String password) {
        super(URI.create(url), "CommandAPIUserLoginToJWT" + r.nextInt(100),
                HystrixCommandTimeoutConfig.defaultTimeout);
        this.userName = userName;
        this.password = password;
        this.secret = secret;
    }

    @Override
    protected Map<String, String> getFormParameters() {
        Map<String, String> data = super.getFormParameters();
        data.put("username", userName);
        data.put("password", password);
        return data;
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
        return "/" + secret + "/authenticate_user";
    }


    @Override
    protected String dealWithResponse(String responseBody) {
        return responseBody;
    }
}
