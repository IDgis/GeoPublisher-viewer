package models;

import java.util.List;

public class Service {
	private String serviceId;
	private String serviceName;
	private List<Group> groupList;
	private List<Layer> layerList;
	
	public Service(String serviceId, String serviceName, List<Group> groupList, List<Layer> layerList) {
		this.serviceId = serviceId;	
		this.serviceName = serviceName;
		this.groupList = groupList;
		this.layerList = layerList;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public List<Group> getGroupList() {
		return groupList;
	}
	
	public List<Layer> getLayerList() {
		return layerList;
	}
}