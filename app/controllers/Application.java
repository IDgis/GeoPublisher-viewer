package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Group;
import models.Layer;
import models.Service;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.groups;
import views.html.index;
import views.html.layers;

public class Application extends Controller {
	private List<Service> servicesListAll = new ArrayList<Service>();
	private List<Group> groupsListAll = new ArrayList<Group>();
	
	private List<Layer> layerList1 = new ArrayList<Layer>();
	private List<Layer> layerList2 = new ArrayList<Layer>();
	private List<Layer> layerList3 = new ArrayList<Layer>();
	private List<Layer> layerList4 = new ArrayList<Layer>();
	
	private List<Group> groupList1 = new ArrayList<Group>();
	private List<Group> groupList2 = new ArrayList<Group>();
	private List<Group> groupList3 = new ArrayList<Group>();
	
	public Application() {
		Layer layer1 = new Layer("1548", "Bebouwde kommen in Overijssel");
    	Layer layer2 = new Layer("4548", "Bebouwde kommen rondom Overijssel");
    	Layer layer3 = new Layer("9521", "Bodemgebruik in Overijssel (1993)");
    	Layer layer4 = new Layer("3654", "Bodemgebruik in Overijssel (1996)");
    	Layer layer5 = new Layer("5912", "Gebiedskenmerken stedelijke laag");
    	Layer layer6 = new Layer("4387", "Grens projectgebied Vecht Regge");
    	Layer layer7 = new Layer("9513", "Grenzen waterschappen in Overijssel (lijn)");
    	Layer layer8 = new Layer("7412", "Grenzen waterschappen in Overijssel (vlak)");
    	Layer layer9 = new Layer("2895", "Nationale parken Weerribben-Wieden en Sallandse Heuvelrug");
    	Layer layer10 = new Layer("1545", "Projecten in Overijssel");
    	
    	Group group1 = new Group("7522", "Natuur", null, layerList1);
    	Group group2 = new Group("8754", "Grenzen", null, layerList2);
    	Group group3 = new Group("3564", "Gewassen", null, layerList3);
    	Group group4 = new Group("1554", "Water", groupList2, layerList4);
    	
    	layerList1.add(layer1);
    	layerList1.add(layer2);
    	layerList1.add(layer4);
    	layerList2.add(layer9);
    	layerList2.add(layer3);
    	layerList2.add(layer8);
    	layerList3.add(layer6);
    	layerList3.add(layer5);
    	layerList4.add(layer7);
    	layerList4.add(layer10);
    	
    	groupList1.add(group1);
    	groupList1.add(group3);
    	groupList2.add(group2);
    	groupList2.add(group3);
    	groupList3.add(group1);
    	groupList3.add(group4); 
    	
    	Service service1 = new Service("7854", "OV_B4", groupList1, null);
    	Service service2 = new Service("4578", "H2O", groupList3, layerList1);
    	Service service3 = new Service("7521", "Beveiligd", groupList2, null);
    	
    	/*****/
    	
    	servicesListAll.add(service1);
    	servicesListAll.add(service2);
    	servicesListAll.add(service3);
    	
    	Collections.sort(servicesListAll, (Service s1, Service s2) -> s1.getServiceName().compareTo(s2.getServiceName()));
    	
    	groupsListAll.add(group1);
    	groupsListAll.add(group2);
    	groupsListAll.add(group3);
    	groupsListAll.add(group4);
    	
    	Collections.sort(groupsListAll, (Group g1, Group g2) -> g1.getGroupName().compareTo(g2.getGroupName()));
    }
	
    public Result index() {
    	return ok(index.render(servicesListAll));
    }
    
    public Result groups(String serviceId) {
    	return ok(groups.render(servicesListAll, serviceId));
    }
    
    public Result layers(String groupId) {
    	return ok(layers.render(groupsListAll, groupId));
    }
}