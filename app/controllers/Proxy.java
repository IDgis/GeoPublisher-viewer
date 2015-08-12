package controllers;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

public class Proxy extends Controller {
	
	private static final ALogger logger = Logger.of(Proxy.class);
	
	@Inject WSClient ws;
	
	public Promise<Result> proxy(String url, String encoding) {
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		
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
				final String body = new String(response.asByteArray(), encoding);
						
				StringBuilder strBuilder = new StringBuilder(body);
				String strStyle = strBuilder.substring(body.indexOf("<style"), body.indexOf("</style>") + 8);
				String strTitle = strBuilder.substring(body.indexOf("<head"), body.indexOf("</head>") + 7);
				
				response().setContentType("text/html; charset=utf-8");
				
				return status(statusCode, body.replace(strStyle, "").replace(strTitle, "").replace("<br/>", ""), "UTF-8");
			}
		});
		
		
		
		Promise<Result> recoveredPromise = resultPromise.recover ((Throwable throwable) -> {
			if (throwable instanceof TimeoutException) {
				logger.error("Timeout when requesting: " + url, throwable);
				return status (GATEWAY_TIMEOUT, throwable.getMessage ());
			} else {
				logger.error("Error occured when requesting: " + url, throwable);
				return status (BAD_GATEWAY, throwable.getMessage ());
			}
		});
		
		return recoveredPromise;
	}
}