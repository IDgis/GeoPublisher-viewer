package controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Layer;
import models.Service;
import play.Routes;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import views.html.layers;
import views.html.services;

public class Application extends Controller {
	private static final int INIT_DEPTH = 1;
	
	private final List<Service> servicesListAll;
	private Map<String, Service> serviceMap = new HashMap<String, Service>();
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
			new Layer("7522", "Natuur", layerList1),
	    	new Layer("1554", "Water", layerList4)
		);
		
		List<Layer> layerList7 = Arrays.asList(
			new Layer("8754", "Grenzen", layerList2),
	    	new Layer("1554", "Water", layerList4)
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
    	
    	servicesListAll = Arrays.asList(
    		new Service("7854", "OV_B4", layerList5),
    		new Service("4578", "H2O", layerList7),
    		new Service("7521", "Beveiligd", layerList6)
    	);
    	
    	Collections.sort(servicesListAll, (Service s1, Service s2) -> s1.getServiceName().compareTo(s2.getServiceName()));
    	
    	for(Service service: servicesListAll) {
    		serviceMap.put(service.getServiceId(), service);
    	}
    }
	
    public Result index() {
    	return ok(index.render(servicesListAll));
    }
    
    public Result services(String serviceId) {
    	Service service= serviceMap.get(serviceId);
    	if(service == null) {
    		return notFound();
    	}
    	
    	return ok(services.render(service, INIT_DEPTH));
    }
    
    public Result layers(String layerId) {    	
    	Layer layer = layerMap.get(layerId);
    	if(layer == null) {
    		return notFound();
    	}
    	
    	return ok(layers.render(layer, INIT_DEPTH));
    }
    
    public Result jsRoutes() {
		return ok (Routes.javascriptRouter ("jsRoutes",
            controllers.routes.javascript.Application.services(),
            controllers.routes.javascript.Application.layers()
        )).as ("text/javascript");
    }
}