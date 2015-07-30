package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Group;
import models.Service;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {

    public Result index() {
    	List<Service> services = new ArrayList<Service>();
    	
    	Service service1 = new Service("7854", "OV_B4");
    	Service service2 = new Service("4578", "H2O");
    	Service service3 = new Service("7521", "Beveiligd");
    	
    	services.add(service1);
    	services.add(service2);
    	services.add(service3);
    	
    	Collections.sort(services, (Service s1, Service s2) -> s1.getServiceNaam().compareTo(s2.getServiceNaam()));
    	
    	List<Group> groups = new ArrayList<Group>();
    	
    	Group group1 = new Group(service1, "7522", "Natuur");
    	Group group2 = new Group(service2, "7522", "Natuur");
    	Group group3 = new Group(service1, "8754", "Grenzen");
    	Group group4 = new Group(service3, "3564", "Gewassen");
    	Group group5 = new Group(service2, "1554", "Water");
    	Group group6 = new Group(service3, "1554", "Water");
    	
    	groups.add(group1);
    	groups.add(group2);
    	groups.add(group3);
    	groups.add(group4);
    	groups.add(group5);
    	groups.add(group6);
    	
    	Collections.sort(groups, (Group g1, Group g2) -> g1.getGroupName().compareTo(g2.getGroupName()));
    	
    	return ok(index.render(services, groups));
    }
}