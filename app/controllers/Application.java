package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import models.Service;
import nl.idgis.ogc.client.wms.WMSCapabilitiesParser;
import nl.idgis.ogc.client.wms.WMSCapabilitiesParser.ParseException;
import nl.idgis.ogc.wms.WMSCapabilities;
import play.Logger;
import play.Routes;
import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.capabilitieswarning;
import views.html.index;
import views.html.layers;

public class Application extends Controller {
	private @Inject WSClient ws;
	
	private List<Service> servicesList;
	
	public Application() {
		servicesList = Arrays.asList(
	    	new Service("1234", "B0 - Referentie", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B0/wms?", "1.3.0"), 
	    	new Service("2345", "B3 - Water", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B3/wms?", "1.3.0"),
	    	new Service("3456", "B6 - Economie en landbouw", "http://acc-staging-services.geodataoverijssel.nl/geoserver/OV_B6/wms?", "1.3.0"),
	    	new Service("4567", "Stedelijk_gebied", "http://staging-services.geodataoverijssel.nl/geoserver/B04_stedelijk_gebied/wms?", "1.3.0"),
	    	new Service("5678", "Bestuurlijke grenzen", "http://staging-services.geodataoverijssel.nl/geoserver/B14_bestuurlijke_grenzen/wms?", "1.3.0")
	    );
    }
	
	public InputStream getWMSCapabilitiesBody(String url) {
		WSRequest request = ws.url(url).setFollowRedirects(true).setRequestTimeout(10000);
		
		Map<String, String[]> colStr = request().queryString();
		
		for (Map.Entry<String, String[]> entry: colStr.entrySet()) {
			for(String entryValue: entry.getValue()) {
				request = request.setQueryParameter(entry.getKey(), entryValue);
			}
		}
		
		Promise<WSResponse> response = request.get();
		
		InputStream inputStream = null;
		inputStream = response.get(10000).getBodyAsStream();
		
		return inputStream;
	}
    
    public WMSCapabilities getWMSCapabilities(Service service) throws ParseException {
    	InputStream capabilities = getWMSCapabilitiesBody(service.getEndpoint() + "version=" + service.getVersion() + "&service=wms&request=GetCapabilities");
    	
    	try {
    		return WMSCapabilitiesParser.parseCapabilities(capabilities);
    	} catch(ParseException e) {
    		Logger.error("An exception occured during parsing of a capabilities document: ", e);
    		throw e;
    	} finally {
    		try {
    			capabilities.close();
    		} catch(IOException io) {
    			Logger.error("An exception occured during closing of the capabilities stream.", io);
    		}
    	}
    }
    
    public Result getErrorWarning(String capWarning) {
    	return ok(capabilitieswarning.render(capWarning));
    }
    
    public Result index() {
    	return ok(index.render(servicesList));
    }
    
	public Result allLayers(String serviceId) {    	
    	List<WMSCapabilities.Layer> layerList = new ArrayList<>();
    	Service service = null;
    	
    	try {
    		for(Service service2 : servicesList) {
    			WMSCapabilities capabilities = null;
    			if(serviceId.equals(service2.getServiceId())) {
    				capabilities = getWMSCapabilities(service2);
    				
    				Collection<WMSCapabilities.Layer> collectionLayers = capabilities.allLayers();
    	    		for(WMSCapabilities.Layer layer : collectionLayers) {
    	    			layerList.add(layer);
    	    		}
    	    		layerList.remove(0);
    	    		
    	    		layerList = crsCheck(layerList);
    	    		service = service2;
    			}
    		}
    	} catch(ParseException e) {
    		return getErrorWarning("De lagen op dit niveau konden niet worden opgehaald.");
    	}
    	
    	return ok(layers.render(layerList, service));
    }
    
    public Result layers(String serviceId, String layerId) {    	
    	List<WMSCapabilities.Layer> layerList = new ArrayList<>();
    	Service service = null;
    	
    	try {
    		for(Service service2 : servicesList) {
    			WMSCapabilities capabilities = null;
    			if(serviceId.equals(service2.getServiceId())) {
    				capabilities = getWMSCapabilities(service2);
    				
    				WMSCapabilities.Layer layer = capabilities.layer(layerId);
    				layerList = layer.layers();
    				
    				layerList = crsCheck(layerList);
    				service = service2;
    			}
    		}
    	} catch(ParseException e) {
    		return getErrorWarning("De lagen op dit niveau konden niet worden opgehaald.");
    	}
    	
    	return ok(layers.render(layerList, service));
    }
    
    public List<WMSCapabilities.Layer> crsCheck(List<WMSCapabilities.Layer> layerList) {
    	for(WMSCapabilities.Layer layerChild : layerList) {
    		if(!layerChild.supportsCRS("EPSG:28992")) {
				layerList.remove(layerChild);
			}
		}
    	
    	return layerList;
    }
    
    public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
            controllers.routes.javascript.Application.allLayers(),
            controllers.routes.javascript.Application.layers()
        )).as ("text/javascript");
    }
}