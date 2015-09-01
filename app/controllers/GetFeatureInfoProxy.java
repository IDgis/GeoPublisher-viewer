package controllers;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.emptyfeatureinfomessage;

/**
 * The controller to get the feature info.
 * 
 * @author Sandro
 *
 */
public class GetFeatureInfoProxy extends Controller {
	
	private static final ALogger logger = Logger.of(GetFeatureInfoProxy.class);
	
	@Inject WSClient ws;
	
	/**
	 * Fetches the html response of the get feature info call. It filters the HTML from the body tags of the response 
	 * and removes the br tags with an HTML parser. If the body is an empty string it returns a static message. If 
	 * the body isn't empty it returns the HTML of the body in UTF-8.
	 * 
	 * @param url the url to fetch
	 * @return The promise of the result of the response.
	 */
	public Promise<Result> proxy(String url) {
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		Map<String, String[]> requestParams = request().queryString();
		for (Map.Entry<String, String[]> entry: requestParams.entrySet()) {
			for(String entryValue: entry.getValue()) {
				request = request.setQueryParameter(entry.getKey(), entryValue);
			}
		}
		
		Promise<Result> resultPromise = request.get().map (response -> {
			Integer statusCode = response.getStatus();
			
			if(statusCode >= 500 && statusCode < 600) {
				return status(BAD_GATEWAY, response.asByteArray()).as(response.getHeader(CONTENT_TYPE));
			} else {
				// parses response, sets encoding from parameter because geoserver doesn't provide one in response
				String encodeValue = requestParams.get("encoding")[0];
				Document gfiDocument = Jsoup.parse(response.getBodyAsStream(), encodeValue, url);
				
				// modifies response
				gfiDocument.body().getElementsByTag("br").remove();
				
				// sends response
				response().setContentType("text/html; charset=utf-8");
				if(gfiDocument.body().html().isEmpty()) {
					return getEmptyFeatureInfo("Niets gevonden.");
				} else {
					return status(statusCode, gfiDocument.body().html(), "UTF-8");
				}
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
	
	/**
	 * Fetches an info message when a get feature info return is empty.
	 * 
	 * @param message the text to show in the info message
	 * @return The result.
	 */
	public Result getEmptyFeatureInfo(String message) {
    	return ok(emptyfeatureinfomessage.render(message));
    }
}