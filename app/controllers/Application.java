package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Group;
import models.GroupLayer;
import models.Layer;
import models.Service;
import models.ServiceGroup;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {

    public Result index() {
    	List<Service> services = new ArrayList<Service>();
    	List<ServiceGroup> serviceGroup = new ArrayList<ServiceGroup>();
    	List<GroupLayer> groupLayer = new ArrayList<GroupLayer>();
    	
    	Service service1 = new Service("7854", "OV_B4");
    	Service service2 = new Service("4578", "H2O");
    	Service service3 = new Service("7521", "Beveiligd");
    	
    	services.add(service1);
    	services.add(service2);
    	services.add(service3);
    	
    	Collections.sort(services, (Service s1, Service s2) -> s1.getServiceName().compareTo(s2.getServiceName()));
    	
    	Group group1 = new Group("7522", "Natuur");
    	Group group2 = new Group("8754", "Grenzen");
    	Group group3 = new Group("3564", "Gewassen");
    	Group group4 = new Group("1554", "Water");
    	
    	ServiceGroup serviceGroup1 = new ServiceGroup(service1, group1);
    	ServiceGroup serviceGroup2 = new ServiceGroup(service1, group2);
    	ServiceGroup serviceGroup3 = new ServiceGroup(service2, group1);
    	ServiceGroup serviceGroup4 = new ServiceGroup(service2, group4);
    	ServiceGroup serviceGroup5 = new ServiceGroup(service3, group3);
    	ServiceGroup serviceGroup6 = new ServiceGroup(service3, group4);
    	
    	serviceGroup.add(serviceGroup1);
    	serviceGroup.add(serviceGroup2);
    	serviceGroup.add(serviceGroup3);
    	serviceGroup.add(serviceGroup4);
    	serviceGroup.add(serviceGroup5);
    	serviceGroup.add(serviceGroup6);
    	
    	Collections.sort(serviceGroup, (ServiceGroup sg1, ServiceGroup sg2) -> sg1.getGroup().getGroupName().compareTo(sg2.getGroup().getGroupName()));
    	
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
    	
    	GroupLayer groupLayer1 = new GroupLayer(group1, layer1);
    	GroupLayer groupLayer2 = new GroupLayer(group1, layer2);
    	GroupLayer groupLayer3 = new GroupLayer(group1, layer4);
    	GroupLayer groupLayer4 = new GroupLayer(group1, layer9);
    	GroupLayer groupLayer5 = new GroupLayer(group2, layer3);
    	GroupLayer groupLayer6 = new GroupLayer(group2, layer8);
    	GroupLayer groupLayer7 = new GroupLayer(group2, layer6);
    	GroupLayer groupLayer8 = new GroupLayer(group3, layer5);
    	GroupLayer groupLayer9 = new GroupLayer(group3, layer7);
    	GroupLayer groupLayer10 = new GroupLayer(group4, layer10);
    	
    	groupLayer.add(groupLayer1);
    	groupLayer.add(groupLayer2);
    	groupLayer.add(groupLayer3);
    	groupLayer.add(groupLayer4);
    	groupLayer.add(groupLayer5);
    	groupLayer.add(groupLayer6);
    	groupLayer.add(groupLayer7);
    	groupLayer.add(groupLayer8);
    	groupLayer.add(groupLayer9);
    	groupLayer.add(groupLayer10);
    	
    	Collections.sort(groupLayer, (GroupLayer gl1, GroupLayer gl2) -> gl1.getLayer().getLayerName().compareTo(gl2.getLayer().getLayerName()));
    	
    	return ok(index.render(services, serviceGroup));
    }
}