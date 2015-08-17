package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.w3c.dom.Document;

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
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

public class Application extends Controller {
	private @Inject WSClient ws;
	
	public Promise<List<Service>> getServicesList() {
		String url = "http://staging-services.geodataoverijssel.nl/geoserver/" + "rest/workspaces.xml";
		
		WSRequest request = ws.url(url).setAuth("admin", "ijMonRic8", WSAuthScheme.BASIC);
		return request.get().map(response -> {
			Document body = response.asXml();
			
			body.getElementsByTagName("workspace");
			
			List<Service> servicesList = Arrays.asList(
			    	new Service("1234", "B0 - Referentie", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B0/wms?", "1.3.0"), 
			    	new Service("2345", "B3 - Water", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B3/wms?", "1.3.0"),
			    	new Service("3456", "B6 - Economie en landbouw", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B6/wms?", "1.3.0"),
			    	new Service("4567", "Stedelijk_gebied", "http://staging-services.geodataoverijssel.nl/geoserver/B04_stedelijk_gebied/wms?", "1.3.0"),
			    	new Service("5678", "Bestuurlijke grenzen", "http://staging-services.geodataoverijssel.nl/geoserver/B14_bestuurlijke_grenzen/wms?", "1.3.0"),
			    	new Service("6789", "B4 - Natuur en milieu", "http://staging-services.geodataoverijssel.nl/geoserver/OV_B4/wms?", "1.3.0"));
			
			Collections.sort(servicesList, (Service s1, Service s2) -> s1.getServiceName().compareTo(s2.getServiceName()));
			
			return servicesList;			

			// todo: parsing
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
		
		try {
			Promise<WSResponse> response = request.get();
			return response.map(response2 -> {
				return response2.getBodyAsStream();
			});
		} catch(PromiseTimeoutException pte) {
			throw pte;
		}		
	}
    
    public Promise<WMSCapabilities> getWMSCapabilities(Service service) {
    	Promise<InputStream> capabilities = getWMSCapabilitiesBody(service.getEndpoint() + "version=" + service.getVersion() + "&service=wms&request=GetCapabilities");
    	
    	return capabilities.map(capabilitiesBody -> {
    		try {
        		return WMSCapabilitiesParser.parseCapabilities(capabilitiesBody);
        	} catch(ParseException e) {
        		Logger.error("An exception occured during parsing of a capabilities document: ", e);
        		throw e;
        	} finally {
        		try {
        			capabilitiesBody.close();
        		} catch(IOException io) {
        			Logger.error("An exception occured during closing of the capabilities stream.", io);
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
						
						return (Result)ok(layers.render(layerList, service));
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
						
						return (Result)ok(layers.render(layerList, service));
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
            controllers.routes.javascript.Proxy.proxy()
        )).as ("text/javascript");
    }
}