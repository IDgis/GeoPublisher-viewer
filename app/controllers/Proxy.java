package controllers;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

public class Proxy extends Controller {
	
	@Inject WSClient ws;
	
	public Promise<Result> proxy(String url) {
		String completeUrl = "http://staging-services.geodataoverijssel.nl/" + url;
		WSRequest request = ws.url(completeUrl).setFollowRedirects(true).setRequestTimeout(10000);
		
		Map<String, String[]> colStr = request().queryString();
		
		for (Map.Entry<String, String[]> entry: colStr.entrySet()) {
			for(String entryValue: entry.getValue()) {
				request = request.setQueryParameter(entry.getKey(), entryValue);
			}
		}
		
		Promise<WSResponse> responsePromise = request.get();
		Promise<Result> resultPromise = responsePromise.map (response -> {
			Integer statusCode = response.getStatus();
			
			if(statusCode >= 500 && statusCode < 600) {
				return status(BAD_GATEWAY, response.asByteArray()).as(response.getHeader(CONTENT_TYPE));
			} else {
				return status(statusCode, response.asByteArray()).as(response.getHeader(CONTENT_TYPE));
			}
		});
		
		Promise<Result> recoveredPromise = resultPromise.recover ((Throwable throwable) -> {
			if (throwable instanceof TimeoutException) {
				return status (GATEWAY_TIMEOUT, throwable.getMessage ());
			} else {
				return status (BAD_GATEWAY, throwable.getMessage ());
			}
		});
		
		return recoveredPromise;
	}
}