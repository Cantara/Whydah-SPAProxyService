package net.whydah.commands;

import com.github.kevinsawicki.http.HttpRequest;

import net.whydah.commands.basecommands.MyBaseHttpGetHystrixCommand;

import java.net.URI;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

public class CommandGetProxyResponse  extends MyBaseHttpGetHystrixCommand<String> {

	String contentType = "text/xml;charset=UTF-8";
	String httpAuthorizationString;
	static Random r = new Random();

	public CommandGetProxyResponse(String uri) {
		super(URI.create(uri),"CommandGetProxyResponse_" + r.nextInt(100), HystrixCommandTimeoutConfig.defaultTimeout);
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

			JSONObject obj;
			try {
				obj = new JSONObject(responseBody);
				String location = obj.getString("Location");
				String cookie = obj.getString("Cookie");

				if(location.contains("&ticket")) {
					responseBody = "" +
						"{" +
						"\"code\": \""+ location.substring(location.indexOf("code=") + 5, location.indexOf("&ticket"))+ "\"," +
						"\"cookievalue\": \""+ cookie.substring(cookie.indexOf("=") + 1, cookie.indexOf(";expires"))+ "\"," +
						"\"ticket\": \""+ location.substring(location.indexOf("ticket=") + 7, location.length())+  "\"" +
						"}";
				} else {
					responseBody = "" +
							"{" +
							"\"code\": \""+ location.substring(location.indexOf("code=") + 5)+ "\"," +
							"\"cookievalue\": \""+ cookie.substring(cookie.indexOf("=") + 1, cookie.indexOf(";expires"))+ "\"," +
							"\"ticket\": \""+ null +  "\"" +
							"}";
				}
			} catch (JSONException e) {

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
