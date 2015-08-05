package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import models.Layer;
import nl.idgis.ogc.client.wms.WMSCapabilitiesParser;
import nl.idgis.ogc.client.wms.WMSCapabilitiesParser.ParseException;
import nl.idgis.ogc.wms.WMSCapabilities;
import play.Logger;
import play.Routes;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.capabilitieswarning;
import views.html.index;
import views.html.layers;

public class Application extends Controller {
	
	private @Inject play.Application application;
	
	private Map<String, Layer> layerMap = new HashMap<String, Layer>();
	
	public Application() {
		List<Layer> layerList1 = Arrays.asList(
			new Layer("1548", "Bebouwde kommen in Overijssel", Collections.emptyList()),
			new Layer("4548", "Bebouwde kommen rondom Overijssel", Collections.emptyList()),
			new Layer("3654", "Bodemgebruik in Overijssel (1996)", Collections.emptyList())
		);
		
		List<Layer> layerList2 = Arrays.asList(
			new Layer("2895", "Nationale parken Weerribben-Wieden en Sallandse Heuvelrug", Collections.emptyList()),
	    	new Layer("9521", "Bodemgebruik in Overijssel (1993)", Collections.emptyList()),
	    	new Layer("7412", "Grenzen waterschappen in Overijssel (vlak)", Collections.emptyList())
		);
		
		List<Layer> layerList3 = Arrays.asList(
			new Layer("4387", "Grens projectgebied Vecht Regge", Collections.emptyList()),
	    	new Layer("5912", "Gebiedskenmerken stedelijke laag", Collections.emptyList())
		);
		
		List<Layer> layerList4 = Arrays.asList(
			new Layer("9513", "Grenzen waterschappen in Overijssel (lijn)", Collections.emptyList()),
		    new Layer("1545", "Projecten in Overijssel", Collections.emptyList())
		);
    	
		List<Layer> layerList5 = Arrays.asList(
			new Layer("7522", "Natuur", layerList1),
	    	new Layer("3564", "Gewassen", layerList3)
		);
		
		List<Layer> layerList6 = Arrays.asList(
			new Layer("7523", "Natuur2", layerList1),
	    	new Layer("1554", "Water", layerList4)
		);
		
		List<Layer> layerList7 = Arrays.asList(
			new Layer("8754", "Grenzen", layerList2),
	    	new Layer("1555", "Water2", layerList5)
		);
    	
    	List<Layer> layerListAl = Arrays.asList(
			new Layer("3333", "Algemeen1", layerList5),
	    	new Layer("2222", "Algemeen2", layerList6),
	    	new Layer("1111", "Algemeen3", layerList7)	
    	);
    	
    	Collections.sort(layerList1, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	Collections.sort(layerList2, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	Collections.sort(layerList3, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	Collections.sort(layerList4, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	Collections.sort(layerList5, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	Collections.sort(layerList6, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	Collections.sort(layerList7, (Layer l1, Layer l2) -> l1.getLayerName().compareTo(l2.getLayerName()));
    	
    	for(Layer layer: layerList1) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerList2) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerList3) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerList4) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerList5) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerList6) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerList7) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    	
    	for(Layer layer: layerListAl) { 
    		layerMap.put(layer.getLayerId(), layer); 
    	}
    }
    
    public WMSCapabilities getWMSCapabilities(String serviceId) throws ParseException {
    	InputStream capabilities = application.resourceAsStream ("wmscapabilities.xml");
    	
    	try {
    		return WMSCapabilitiesParser.parseCapabilities(capabilities);
    	} catch(ParseException e) {
    		Logger.error("An exception occured during parsing of a capabilities document: ", e);
    		throw new ParseException ("Error parsing capabilities document", e);
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
    	List<WMSCapabilities.Service> servicesList = null;
    	WMSCapabilities capabilities = null;
    	
    	try {
    		capabilities = getWMSCapabilities("1234");
    		servicesList = Arrays.asList(
    			capabilities.serviceIdentification()
    	    );
    	} catch(ParseException e) {
    		Logger.error("An exception occured during parsing of a capabilities document: ", e);
    	}
    	
    	return ok(index.render(servicesList, capabilities));
    }
    
    public Result allLayers() {    	
    	List<WMSCapabilities.Layer> layerList = new ArrayList<>();
    	WMSCapabilities capabilities = null;
    	
    	try {
    		capabilities = getWMSCapabilities("1234");
    		Collection<WMSCapabilities.Layer> collectionLayers = capabilities.allLayers();
    		for(WMSCapabilities.Layer layer : collectionLayers) {
    			layerList.add(layer);
    		}
    		layerList.remove(0);
    	} catch(ParseException e) {
    		Logger.error("An exception occured during parsing of a capabilities document: ", e);
    	}
    	
    	return ok(layers.render(layerList));
    }
    
    public Result layers(String layerId) {    	
    	List<WMSCapabilities.Layer> layerList = new ArrayList<>();
    	WMSCapabilities capabilities = null;
    	
    	try {
    		capabilities = getWMSCapabilities("1234");
    		WMSCapabilities.Layer layer = capabilities.layer(layerId);
    		layerList = layer.layers();
    	} catch(ParseException e) {
    		Logger.error("An exception occured during parsing of a capabilities document: ", e);
    	}
    	
    	return ok(layers.render(layerList));
    }
    
    public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
            controllers.routes.javascript.Application.allLayers(),
            controllers.routes.javascript.Application.layers()
        )).as ("text/javascript");
    }
}