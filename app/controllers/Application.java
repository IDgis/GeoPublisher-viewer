package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Service;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {

    public Result index() {
    	List<Service> services = new ArrayList<Service>();
    	Service service1 = new Service("7854", "OV_B4");
    	Service service2 = new Service("4578", "H2O");
    	Service service3 = new Service("9521", "OV_B9");
    	Service service4 = new Service("3521", "OV_B5");
    	Service service5 = new Service("1854", "OV_B7");
    	Service service6 = new Service("7521", "Beveiligd");
    	Service service7 = new Service("4532", "O2");
    	Service service8 = new Service("2545", "OV_B8");
    	Service service9 = new Service("8545", "OV_B1");
    	Service service10 = new Service("3521", "OV_B6");
    	Service service11 = new Service("6574", "OV_B3");
    	Service service12 = new Service("5452", "OV_B2");
    	
    	services.add(service1);
    	services.add(service2);
    	services.add(service3);
    	services.add(service4);
    	services.add(service5);
    	services.add(service6);
    	services.add(service7);
    	services.add(service8);
    	services.add(service9);
    	services.add(service10);
    	services.add(service11);
    	services.add(service12);
    	
    	Collections.sort(services, (Service s1, Service s2) -> s1.getServiceNaam().compareTo(s2.getServiceNaam()));
    	
    	return ok(index.render(services));
    }
}