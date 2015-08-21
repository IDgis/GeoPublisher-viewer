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

public class Application extends Controller {
	private @Inject WSClient ws;
	
	public Promise<List<Service>> getServicesList() {
		String url = "http://staging-services.geodataoverijssel.nl/geoserver/";
		String workspacesSummary = url + "rest/workspaces.xml";
		String version = "1.3.0";
		
		WSRequest workspacesSummaryRequest = ws.url(workspacesSummary).setAuth("admin", "ijMonRic8", WSAuthScheme.BASIC);
		return workspacesSummaryRequest.get().flatMap(responseWorkspacesSummary -> {
			Document bodyWorkspacesSummary = responseWorkspacesSummary.asXml();
			NodeList names = bodyWorkspacesSummary.getElementsByTagName("name");
			
			List<Promise<Service>> unsortedServicesList = new ArrayList<>();
			for(int i = 0; i < names.getLength(); i++) {
				String name = names.item(i).getTextContent();
				String workspaceSettings = url + "rest/services/wms/workspaces/" + name + "/settings.xml";
				
				WSRequest workspaceSettingsRequest = ws.url(workspaceSettings).setAuth("admin", "ijMonRic8", WSAuthScheme.BASIC);
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
			
			unsortedServicesList.add(Promise.pure(new Service("test", "test", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B0/wms?", "1.3.0")));
			
			return Promise.sequence(unsortedServicesList).map(servicesList -> {
				Collections.sort(servicesList, (Service s1, Service s2) -> s1.getServiceName().compareToIgnoreCase(s2.getServiceName()));
				
				return servicesList;
			});
		});
	}
	
	public Promise<InputStream> getWMSCapabilitiesBody(String url) {
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
    
    public Promise<WMSCapabilities> getWMSCapabilities(Service service) {
    	Promise<InputStream> capabilities = getWMSCapabilitiesBody(service.getEndpoint() + "version=" + service.getVersion() + "&service=wms&request=GetCapabilities");
    	
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
    
    public Promise<Result> index() {
    	return getServicesList().map(servicesList -> ok(index.render(servicesList)));
    }
    
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
	
	private Result getErrorWarning(Throwable t) throws Throwable {
		if(t instanceof ParseException) {
			return getErrorWarning("De lagen op dit niveau konden niet worden opgehaald."); 
		} else if(t instanceof PromiseTimeoutException){
			return getErrorWarning("Het laden van de lagen op dit niveau heeft te lang geduurd.");
		} else {
			throw t;
		}
	}
	
	public Result getErrorWarning(String capWarning) {
    	return ok(capabilitieswarning.render(capWarning));
    }
	
	public Result getEmptyLayerMessage(String message) {
    	return ok(emptylayermessage.render(message));
    }
    
    public List<WMSCapabilities.Layer> crsCheck(List<WMSCapabilities.Layer> layerList) {
    	for(WMSCapabilities.Layer layer : layerList) {
    		if(!layer.supportsCRS("EPSG:28992")) {
				layerList.remove(layer);
			}
		}
    	
    	return layerList;
    }
    
    public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
            controllers.routes.javascript.Application.allLayers(),
            controllers.routes.javascript.Application.layers(),
            controllers.routes.javascript.GetFeatureInfoProxy.proxy()
        )).as ("text/javascript");
    }
}