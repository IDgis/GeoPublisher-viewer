package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import play.libs.ws.WSResponse;
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
 * @author Sandro Neumann
 *
 */
public class Application extends Controller {
	private @Inject WSClient ws;
	private @Inject Configuration conf;
	private @Inject WebJarAssets webJarAssets;
	
	/**
	 * Renders the index page.
	 * 
	 * @param serviceName the name of the service to be retrieved, if the string is empty all services will be returned
	 * @return the promise of the result
	 */
	public Promise<Result> index(String serviceName) {
		return fetchServicesList(serviceName).map(servicesList -> ok(index.render(webJarAssets, servicesList, serviceName)));
	}
	
	/**
	 * Renders the view for displaying a specific layer
	 * 
	 * @param serviceName the name of the service
	 * @param layerName the name of the layer to display
	 * @return the promise of the result
	 */
	public Promise<Result> renderLayer(String serviceName, String layerName) {
		String httpServiceEndpoint = conf.getString("httpServiceEndpoint");
		String servicesUrl = httpServiceEndpoint + "services";
		
		WSRequest servicesRequest  = ws.url(servicesUrl);
		
		return servicesRequest.get().flatMap(servicesResponse -> {
			Promise<List<Service>> servicesList = handleServicesResponse(servicesResponse, serviceName, false);
			
			return servicesList.flatMap(list -> {
				if(list.size() != 1) return Promise.pure(notFound());
				
				String servicePrefix = serviceName + ":";
				int servicePrefixLength = servicePrefix.length();
				
				return getWMSCapabilities(list.get(0)).map(capabilities -> {
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
						
						String servicePrefixCheck;
						if(servicePrefixLength > wmsLayerName.length()) {
							servicePrefixCheck = null;
						} else {
							servicePrefixCheck = wmsLayerName.substring(0, servicePrefixLength);
						}
						
						if(servicePrefix.equals(servicePrefixCheck)) {
							wmsLayerNameWithoutPrefix = wmsLayerName.substring(servicePrefixLength);
						} else {
							// do nothing
						}
						
						if(!layerName.equals(wmsLayerName) && !layerName.equals(wmsLayerNameWithoutPrefix)) {
							i.remove();
						}
					}
					
					if(finalLayerList.size() < 1) return notFound();
					
					return ok(servicelayer.render(webJarAssets, serviceName, layerName));
				});
			});
		});
	}
	
	/**
	 * Fetches the immediate layers of a service who have a CRS of EPSG:28992.
	 * 
	 * @param serviceId the service id from the service whose immediate layers to fetch
	 * @return the promise of the result
	 */
	public Promise<Result> allLayers(String serviceId) {
		return fetchServicesList("").flatMap(servicesList -> {
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
	 * @return the promise of the result
	 */
	public Promise<Result> layers(String serviceId, String layerId) {
		return fetchServicesList("").flatMap(servicesList -> {
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
	 * Fetches the available services.
	 * 
	 * @param serviceName the name of the service to be retrieved, if the string is empty all services will be returned
	 * @return a promise of the list of services
	 */
	public Promise<List<Service>> fetchServicesList(String serviceName) {
		String httpServiceEndpoint = conf.getString("httpServiceEndpoint");
		String servicesUrl = httpServiceEndpoint + "services";
		
		WSRequest servicesRequest = ws.url(servicesUrl);
		
		return servicesRequest.get().flatMap(servicesResponse -> {
			Promise<List<Service>> unsortedServicesList = handleServicesResponse(servicesResponse, serviceName, true);
			
			return unsortedServicesList.map(servicesList -> {
				List<Service> filteredServicesList = servicesList.stream()
					.filter(s -> s != null)
					.collect(Collectors.toList());
				
				Collections.sort(filteredServicesList, (Service s1, Service s2) -> s1.getServiceName().compareToIgnoreCase(s2.getServiceName()));
				
				return filteredServicesList;
			});
		});
	}
	
	/**
	 * Handles the service response and parses the JSON to a list of services.
	 * 
	 * @param servicesResponse the response of the url request
	 * @param serviceName the name of the service to be retrieved, if the string is empty all services will be returned
	 * @param index boolean that determines if the call originates from an index context
	 * @return a promise of the list of services
	 */
	private Promise<List<Service>> handleServicesResponse(WSResponse servicesResponse, String serviceName, boolean index) {
		String environment = conf.getString("viewer.environmentUrl");
		String url = environment.replaceFirst("(.*)//", "//");
		String version = "1.3.0";
		
		List<Service> servicesList = new ArrayList<>();
		
		try {
			JsonObject object = new JsonParser().parse(servicesResponse.getBody()).getAsJsonObject();
			
			JsonArray servicesArray = object.get("services").getAsJsonArray();
			
			servicesArray.forEach(serviceItem -> {
				JsonObject serviceObject = serviceItem.getAsJsonObject();
				String name = serviceObject.get("name").getAsString();
				String title = serviceObject.get("title").getAsString();
				
				Service service = getService(name, title, url, version);
				
				if(index) {
					if(!"".equals(serviceName)) {
						if(name != null && name.equals(serviceName)) servicesList.add(service);
					} else servicesList.add(service);
				} else {
					if(name != null && name.equals(serviceName)) servicesList.add(service);
				}
			});
		} catch(RuntimeException re) {
			re.printStackTrace();
		}
		
		return Promise.pure(servicesList);
	}
	
	/**
	 * Creates an service instance
	 * 
	 * @param name name of service
	 * @param title title of service
	 * @param url url of environment of service
	 * @param version version of service
	 * @return a new service instance
	 */
	public Service getService(String name, String title, String url, String version) {
		return new Service(name, title, url + name + "/wms?", version);
	}
	
	/**
	 * Parses the WMS capabilities of a service.
	 * 
	 * @param service the service to parse
	 * @return the promise of WMSCapabilities
	 */
	public Promise<WMSCapabilities> getWMSCapabilities(Service service) {
		// create request url
		String environment = conf.getString("viewer.environmentUrl");
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
	
	private void recursiveLayerListing(List<WMSCapabilities.Layer> layers, 
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
	 * Fetches content from an url as an inputstream.
	 * 
	 * @param url the url to fetch
	 * @return the inputstream
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
	 * Handles parse and promise timeout exceptions. If another exception is thrown it throws it again.
	 * 
	 * @param t an exception
	 * @return the result
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
	 * @param warning the text to show in the error message
	 * @return the result
	 */
	public Result getErrorWarning(String warning) {
		return ok(capabilitieswarning.render(warning));
	}
	
	/**
	 * Renders an info message.
	 * 
	 * @param message the text to show in the info message
	 * @return the result
	 */
	public Result getEmptyLayerMessage(String message) {
		return ok(emptylayermessage.render(message));
	}
	
	/**
	 * Checks if a layer has the CRS of EPSG:28992 and removes the layer from the list if that is not the case.
	 * 
	 * @param layerList the list of layers to check
	 * @return the new list of layers
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
	 * Makes specific controller methods available for use in JavaScript.
	 * 
	 * @return the result
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