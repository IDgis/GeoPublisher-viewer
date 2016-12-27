package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
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
import views.html.servicelayer;
import views.html.layers;

/**
 * The main controller of the application.
 * 
 * @author Sandro
 *
 */
public class Application extends Controller {
	private @Inject Zookeeper zk; // force Zookeeper initialization
	private @Inject WSClient ws;
	private @Inject Configuration conf;
	private @Inject WebJarAssets webJarAssets;
	
	/**
	 * Fetches the names and titles of the workspaces and makes a service for each workspace. It retrieves the names 
	 * of all workspaces and then retrieves the titles (if available) of every workspace. Then returns a service with
	 * the title as the name of the service. If the title isn't available it inserts the name of the workspace as the
	 * name of the service.
	 * 
	 * @return The promise of the list of services.
	 */
	public Promise<List<Service>> getServicesList(String service) {
		String environment = conf.getString("viewer.environmenturl");
		String username = conf.getString("viewer.username");
		String password = conf.getString("viewer.password");
		
		// The protocol is omitted from the service urls to ensure that the 
		// client is using the same protocol to access the services as is used 
		// to retrieve this application.
		String url = environment.replaceFirst("(.*)//", "//");
		String workspacesSummary = environment + "rest/workspaces.xml";
		String version = "1.3.0";
		
		WSRequest workspacesSummaryRequest = ws.url(workspacesSummary).setAuth(username, password, WSAuthScheme.BASIC);
		
		return workspacesSummaryRequest.get().flatMap(responseWorkspacesSummary -> {
			Document bodyWorkspacesSummary = responseWorkspacesSummary.asXml();
			NodeList names = bodyWorkspacesSummary.getElementsByTagName("name");
			List<Promise<Service>> unsortedServicesList = new ArrayList<>();
			
			for(int i = 0; i < names.getLength(); i++) {
				String name = names.item(i).getTextContent();
				String workspaceSettings = environment + "rest/services/wms/workspaces/" + name + "/settings.xml";
				WSRequest workspaceSettingsRequest = ws.url(workspaceSettings).setAuth(username, password, WSAuthScheme.BASIC);
				
				if(!"".equals(service)) {
					if(service.equals(name)) {
						unsortedServicesList.add(workspaceSettingsRequest.get().map(responseWorkspaceSettings -> {
							Document bodyWorkspaceSettings = responseWorkspaceSettings.asXml();
							NodeList titles = bodyWorkspaceSettings.getElementsByTagName("title");
							
							return getService(titles, url, name, version);
						}));
					}
				} else {
					unsortedServicesList.add(workspaceSettingsRequest.get().map(responseWorkspaceSettings -> {
						Document bodyWorkspaceSettings = responseWorkspaceSettings.asXml();
						NodeList titles = bodyWorkspaceSettings.getElementsByTagName("title");
						
						return getService(titles, url, name, version);
					}));
				}
			}
			
			return Promise.sequence(unsortedServicesList).map(servicesList -> {
				Collections.sort(servicesList, (Service s1, Service s2) -> s1.getServiceName().compareToIgnoreCase(s2.getServiceName()));
				
				return servicesList;
			});
		});
	}
	
	/**
	 * 
	 * @param titles list of titles of service
	 * @param url url of environment services
	 * @param name name of service
	 * @param version version of WMS
	 * @return
	 */
	public Service getService(NodeList titles, String url, String name, String version) {
		
		for(int j = 0; j < titles.getLength(); j++) {
			/* Parent has to be 'wms' to pick the right 'title' node. */
			if(titles.item(j).getParentNode().getNodeName().equals("wms")) {
				return new Service(name, titles.item(0).getTextContent(), url + name + "/wms?", version);
			}
		}
		
		return new Service(name, name, url + name + "/wms?", version);
	}
	
	/**
	 * Fetches content from an url as an inputstream.
	 * 
	 * @param url the url to fetch
	 * @return The inputstream.
	 */
	public Promise<InputStream> getInputStream(String url) {
		// sets up request and parameters
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		Map<String, String[]> colStr = request().queryString();
		for (Map.Entry<String, String[]> entry: colStr.entrySet()) {
			for(String entryValue: entry.getValue()) {
				request = request.setQueryParameter(entry.getKey(), entryValue);
			}
		}
		
		// returns response as inputstream
		return request.get().map(response -> {
			return response.getBodyAsStream();
		});
	}
    
	/**
	 * Parses the WMS capabilities of a service.
	 * 
	 * @param service the service to parse
	 * @return The promise of WMSCapabilities.
	 */
    public Promise<WMSCapabilities> getWMSCapabilities(Service service) {
    	// create request url
    	String environment = conf.getString("viewer.environmenturl");
    	String protocol = environment.substring(0, environment.indexOf("://") + 1);
    	String request = protocol + service.getEndpoint() + "version=" + service.getVersion() + "&service=wms&request=GetCapabilities";
    	
    	// gets the inputstream
    	Promise<InputStream> capabilities = getInputStream(request);
    	
    	// parses the capabilities
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
    public Promise<Result> index(String service) {
    	return getServicesList(service).map(servicesList -> ok(index.render(webJarAssets, servicesList)));
    }
    
    /**
     * Renders the view for displaying a specific layer
     * 
     * @param service the service of the WMS
     * @param layer the layer to display
     * @return the result of the html
     */
    public Promise<Result> renderLayer(String service, String layer) {
    	String environment = conf.getString("viewer.environmenturl");
    	String username = conf.getString("viewer.username");
		String password = conf.getString("viewer.password");
    	
		String url = environment.replaceFirst("(.*)//", "//");
    	String workspacesSummary = environment + "rest/workspaces.xml";
    	
    	WSRequest workspacesSummaryRequest = ws.url(workspacesSummary).setAuth(username, password, WSAuthScheme.BASIC);
    	
    	Service s = new Service(service, service, url + service + "/wms?", "1.3.0");
    	
    	String servicePrefix = service + ":";
    	int servicePrefixLength = servicePrefix.length();
    	
    	return workspacesSummaryRequest.get().flatMap(responseWorkspacesSummary -> {
			Document bodyWorkspacesSummary = responseWorkspacesSummary.asXml();
			NodeList names = bodyWorkspacesSummary.getElementsByTagName("name");
			
			int foundService = 0;
			for(int i = 0; i < names.getLength(); i++) {
				String name = names.item(i).getTextContent();
				if(service.equals(name)) {
					foundService++;
				}
			}
			
			if(foundService < 1) {
				return Promise.pure(notFound());
			}
			
			return getWMSCapabilities(s).map(capabilities -> {
	    		Collection<WMSCapabilities.Layer> collectionLayers = capabilities.layers();
	    		List<WMSCapabilities.Layer> layerList = new ArrayList<>();
				for(WMSCapabilities.Layer wmsLayer : collectionLayers) {
					layerList.addAll(wmsLayer.layers());
	    		}

				List<WMSCapabilities.Layer> finalLayerList = new ArrayList<>();
				recursiveLayerListing(layerList, finalLayerList);
				
				Iterator<WMSCapabilities.Layer> i = finalLayerList.iterator();
				while(i.hasNext()) {
					WMSCapabilities.Layer wmsLayer = i.next();
					String wmsLayerName = wmsLayer.name();
					String wmsLayerNameWithoutPrefix = null;
					String servicePrefixCheck = wmsLayerName.substring(0, servicePrefixLength);
					
					if(servicePrefix.equals(servicePrefixCheck)) {
						wmsLayerNameWithoutPrefix = wmsLayerName.substring(servicePrefixLength);
					} else {
						// do nothing
					}
					
					if(!layer.equals(wmsLayerName) && !layer.equals(wmsLayerNameWithoutPrefix)) {
						i.remove();
					}
				}
				
				if(finalLayerList.size() < 1) {
					return notFound();
				}
	    		
	    		return ok(servicelayer.render(webJarAssets, service, layer));
	    	});
    	});
    }
    
    public void recursiveLayerListing(List<WMSCapabilities.Layer> layers, 
    		List<WMSCapabilities.Layer> list) {
    	for(WMSCapabilities.Layer layer : layers) {
    		if(layer.layers().isEmpty()) {
    			list.add(layer);
    		} else {
    			recursiveLayerListing(layer.layers(), list);
    		}
    	}
    }
    
    /**
     * Fetches the immediate layers of a service who have a CRS of EPSG:28992.
     * 
     * @param serviceId the service id from the service whose immediate layers to fetch
     * @return The promise of the result of the response.
     */
	public Promise<Result> allLayers(String serviceId) {
		return getServicesList("").flatMap(servicesList -> {
    		for(Service service : servicesList) {
    			if(serviceId.equals(service.getServiceId())) {					
					return getWMSCapabilities(service).map(capabilities -> {
						// gets the rootlayer
						Collection<WMSCapabilities.Layer> collectionLayers = capabilities.layers();
						
						// loops through layers in rootlayer and adds them to a list
						List<WMSCapabilities.Layer> layerList = new ArrayList<>();
						for(WMSCapabilities.Layer layer : collectionLayers) {
        	    			layerList.addAll(layer.layers());
        	    		}
						
						// checks if each layer contains the EPSG:28992 CRS
						layerList = crsCheck(layerList);
						
						// sends response
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
	 * Fetches the immediate layers of a layer who have a CRS of EPSG:28992.
	 * 
	 * @param serviceId the service id from the service to select
	 * @param layerId the layer id from the layer whose immediate layers to fetch
	 * @return The promise of the result of the response.
	 */
	public Promise<Result> layers(String serviceId, String layerId) {
    	return getServicesList("").flatMap(servicesList -> {
    		for(Service service : servicesList) {
    			if(serviceId.equals(service.getServiceId())) {
    				return getWMSCapabilities(service).map(capabilities -> {
    					// gets a specific layer
    					WMSCapabilities.Layer layer = capabilities.layer(layerId);
    					
    					// adds all layers of the layer to a list
    					List<WMSCapabilities.Layer> layerList = new ArrayList<>();
						layerList = layer.layers();
						
						// checks if each layer contains the EPSG:28992 CRS
						layerList = crsCheck(layerList);
						
						// sends response
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
	 * Handles parse- and promisetimeoutexceptions. If another exception is thrown throws it again.
	 * 
	 * @param t an exception
	 * @return The result.
	 * @throws Throwable
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
	 * Renders an error message.
	 * 
	 * @param capWarning the text to show in the error message
	 * @return The result.
	 */
	public Result getErrorWarning(String capWarning) {
    	return ok(capabilitieswarning.render(capWarning));
    }
	
	/**
	 * Renders an info message.
	 * 
	 * @param message the text to show in the info message
	 * @return The result.
	 */
	public Result getEmptyLayerMessage(String message) {
    	return ok(emptylayermessage.render(message));
    }
    
	/**
	 * Checks if a layer has the CRS of EPSG:28992 and removes the layer from the list if that's not the case.
	 * 
	 * @param layerList the list of layers to check
	 * @return The new list of layers after checking.
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
     * @return The result.
     */
    public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
            controllers.routes.javascript.Assets.versioned(),
			controllers.routes.javascript.Application.allLayers(),
            controllers.routes.javascript.Application.layers(),
            controllers.routes.javascript.GetFeatureInfoProxy.proxy()
        )).as ("text/javascript");
    }
}