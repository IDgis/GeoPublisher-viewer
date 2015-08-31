package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import models.Service;
import nl.idgis.ogc.client.wms.WMSCapabilitiesParser;
import nl.idgis.ogc.client.wms.WMSCapabilitiesParser.ParseException;
import nl.idgis.ogc.wms.WMSCapabilities;
import play.Logger;
import play.Routes;
import play.Configuration;
import play.libs.F.Promise;
import play.libs.F.PromiseTimeoutException;
import play.libs.ws.WSAuthScheme;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.capabilitieswarning;
import views.html.emptylayermessage;
import views.html.index;
import views.html.layers;

/**
 * The main controller of the application.
 * 
 * @author Sandro
 *
 */
public class Application extends Controller {
	private @Inject WSClient ws;
	private @Inject Configuration conf;
	
	/**
	 * Fetches the names and titles of the workspaces and makes a service for each workspace. It retrieves the names 
	 * of all workspaces and then retrieves the titles (if available) of every workspace. Then returns a service with
	 * the title as the name of the service. If the title isn't available it inserts the name of the workspace as the
	 * name of the service.
	 * 
	 * @return The promise of the list of services.
	 */
	public Promise<List<Service>> getServicesList() {
		/* Retrieves data from configuration parameters. */
		String environment = conf.getString("viewer.environmenturl");
		String username = conf.getString("viewer.username");
		String password = conf.getString("viewer.password");
		
		/* Sets up URL prefix, complete URL for XML document and version for services. */
		String url = environment;
		String workspacesSummary = url + "rest/workspaces.xml";
		String version = "1.3.0";
		
		/* Sets up the request to the workspaces XML document with authorization settings. */
		WSRequest workspacesSummaryRequest = ws.url(workspacesSummary).setAuth(username, password, WSAuthScheme.BASIC);
		
		return workspacesSummaryRequest.get().flatMap(responseWorkspacesSummary -> {
			/* Gets the response as an XML object. */
			Document bodyWorkspacesSummary = responseWorkspacesSummary.asXml();
			
			/* Gets every name in XML document as a nodelist. */
			NodeList names = bodyWorkspacesSummary.getElementsByTagName("name");
			
			/* Makes a list of promises of a service. */
			List<Promise<Service>> unsortedServicesList = new ArrayList<>();
			
			/* 
			 * Retrieves for every name in previously mentioned nodelist the content of the name tag
			 * and sets up an URL with that name that points to the settings of the workspace. It sets up a request 
			 * with authorization settings and gets the response as an XML object. It retrieves the titles
			 * of that object as a nodelist and checks for every title if the parent equals to 'wms'. If that's
			 * the case it retrieves the content of that node and inserts that as a name in a service. If no title
			 * is found with a parent that equals to 'wms' it returns the previously mentioned name as the name in 
			 * the service.
			 */
			for(int i = 0; i < names.getLength(); i++) {
				String name = names.item(i).getTextContent();
				String workspaceSettings = url + "rest/services/wms/workspaces/" + name + "/settings.xml";
				
				WSRequest workspaceSettingsRequest = ws.url(workspaceSettings).setAuth(username, password, WSAuthScheme.BASIC);
				unsortedServicesList.add(workspaceSettingsRequest.get().map(responseWorkspaceSettings -> {
					Document bodyWorkspaceSettings = responseWorkspaceSettings.asXml();
					NodeList titles = bodyWorkspaceSettings.getElementsByTagName("title");
					
					for(int j = 0; j < titles.getLength(); j++) {
						if(titles.item(j).getParentNode().getNodeName().equals("wms")) {
							String title = titles.item(0).getTextContent();
							return new Service(name, title, url + name + "/wms?", version);
						}
					}
					
					return new Service(name, name, url + name + "/wms?", version);
				}));
			}
			
			/*
			 * Converts a list of promises of a service into a promise of a list of services. It
			 * orders the list of services alphabetically and returns the list of services.
			 */
			return Promise.sequence(unsortedServicesList).map(servicesList -> {
				Collections.sort(servicesList, (Service s1, Service s2) -> s1.getServiceName().compareToIgnoreCase(s2.getServiceName()));
				
				return servicesList;
			});
		});
	}
	
	/**
	 * Fetches content from an url.
	 * 
	 * @param url - the url to fetch.
	 * @return the inputstream.
	 */
	public Promise<InputStream> getInputStream(String url) {
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		
		Map<String, String[]> colStr = request().queryString();
		
		for (Map.Entry<String, String[]> entry: colStr.entrySet()) {
			for(String entryValue: entry.getValue()) {
				request = request.setQueryParameter(entry.getKey(), entryValue);
			}
		}
		
		return request.get().map(response -> {
			return response.getBodyAsStream();
		});
	}
    
	/**
	 * Parses the WMS capabilities for a service.
	 * 
	 * @param service - the service to parse.
	 * @return the promise of WMSCapabilities.
	 */
    public Promise<WMSCapabilities> getWMSCapabilities(Service service) {
    	Promise<InputStream> capabilities = getInputStream(service.getEndpoint() + "version=" + service.getVersion() + "&service=wms&request=GetCapabilities");
    	
    	return capabilities.map(capabilitiesBody -> {
    		try {
        		return WMSCapabilitiesParser.parseCapabilities(capabilitiesBody);
        	} catch(ParseException e) {
        		Logger.error("An exception occured during parsing of the capabilities document from service " + service.getServiceId() + ": ", e);
        		throw e;
        	} finally {
        		try {
        			capabilitiesBody.close();
        		} catch(IOException io) {
        			Logger.error("An exception occured during closing of the capabilities stream from service " + service.getServiceId() + ": ", io);
        		}
        	}
    	});
    }
    
    /**
     * Renders the index page.
     * 
     * @return the promise of the result.
     */
    public Promise<Result> index() {
    	return getServicesList().map(servicesList -> ok(index.render(servicesList)));
    }
    
    /**
     * Fetches the immediate layers of a service.
     * 
     * @param serviceId - the service id from the service whose immediate layers to fetch.
     * @return the promise of the result of the HTML response.
     */
	public Promise<Result> allLayers(String serviceId) {
		return getServicesList().flatMap(servicesList -> {
    		for(Service service : servicesList) {
    			if(serviceId.equals(service.getServiceId())) {					
					return getWMSCapabilities(service).map(capabilities -> {
						Collection<WMSCapabilities.Layer> collectionLayers = capabilities.layers();
						
						List<WMSCapabilities.Layer> layerList = new ArrayList<>();
						for(WMSCapabilities.Layer layer : collectionLayers) {
        	    			layerList.addAll(layer.layers());
        	    		}
						
						layerList = crsCheck(layerList);
						
						if(layerList.isEmpty()) {
							return getEmptyLayerMessage("Geen lagen");
						} else {
							return (Result)ok(layers.render(layerList, service));
						}
					});
    			}
    		}
    		
    		return Promise.pure(notFound());
    	}).recover(this::getErrorWarning);
    }
	
	/**
	 * Fetches the immediate layers of a layer.
	 * 
	 * @param serviceId - the service id from the service to select.
	 * @param layerId - the layer id from the layer whose immediate layers to fetch.
	 * @return the promise of the result of the HTML response.
	 */
	public Promise<Result> layers(String serviceId, String layerId) {
    	return getServicesList().flatMap(servicesList -> {
    		for(Service service : servicesList) {
    			if(serviceId.equals(service.getServiceId())) {
    				return getWMSCapabilities(service).map(capabilities -> {
    					WMSCapabilities.Layer layer = capabilities.layer(layerId);
    					
    					List<WMSCapabilities.Layer> layerList = new ArrayList<>();
						layerList = layer.layers();
						layerList = crsCheck(layerList);
						
						if(layerList.isEmpty()) {
							return getEmptyLayerMessage("Geen lagen");
						} else {
							return (Result)ok(layers.render(layerList, service));
						}
					});
    			}
    		}
    		
    		return Promise.pure(notFound());
    	}).recover(this::getErrorWarning);
    }
	
	/**
	 * Handles parse- and promisetimeoutexceptions.
	 * 
	 * @param t - an exception.
	 * @return the result.
	 * @throws Throwable an exception.
	 */
	private Result getErrorWarning(Throwable t) throws Throwable {
		if(t instanceof ParseException) {
			return getErrorWarning("De lagen op dit niveau konden niet worden opgehaald."); 
		} else if(t instanceof PromiseTimeoutException){
			return getErrorWarning("Het laden van de lagen op dit niveau heeft te lang geduurd.");
		} else {
			throw t;
		}
	}
	
	/**
	 * Fetches an error message.
	 * 
	 * @param capWarning - the text to show in the error message.
	 * @return the result.
	 */
	public Result getErrorWarning(String capWarning) {
    	return ok(capabilitieswarning.render(capWarning));
    }
	
	/**
	 * Fetches an info message when a root layer is empty.
	 * 
	 * @param message - the text to show in the info message.
	 * @return the result.
	 */
	public Result getEmptyLayerMessage(String message) {
    	return ok(emptylayermessage.render(message));
    }
    
	/**
	 * Checks if a layer has the CRS of EPSG:28992 and removes the layer if that's not the case.
	 * 
	 * @param layerList - the list of layers to check.
	 * @return the new list of layers after checking.
	 */
    public List<WMSCapabilities.Layer> crsCheck(List<WMSCapabilities.Layer> layerList) {
    	for(WMSCapabilities.Layer layer : layerList) {
    		if(!layer.supportsCRS("EPSG:28992")) {
				layerList.remove(layer);
			}
		}
    	
    	return layerList;
    }
    
    /**
     * Makes specific controller methods available to use in JavaScript.
     * 
     * @return the result.
     */
    public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
            controllers.routes.javascript.Application.allLayers(),
            controllers.routes.javascript.Application.layers(),
            controllers.routes.javascript.GetFeatureInfoProxy.proxy()
        )).as ("text/javascript");
    }
}