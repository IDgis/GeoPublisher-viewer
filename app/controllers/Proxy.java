package controllers;

import java.util.Map;

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
		WSRequest request = ws.url(completeUrl).setFollowRedirects(true);
		
		Map<String, String[]> colStr = request().queryString();
		
		for (Map.Entry<String, String[]> entry: colStr.entrySet()) {
			for(String entryValue: entry.getValue()) {
				request.setQueryParameter(entry.getKey(), entryValue);
			}
		}
		
		Promise<WSResponse> responsePromise = request.get();
		Promise<Result> resultPromise = responsePromise.map (response -> ok(response.asByteArray()).as(response.getHeader(CONTENT_TYPE)));
		
		return resultPromise;
	}
}