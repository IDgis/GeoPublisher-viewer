package models;

import java.util.List;

public class Service {
	private String serviceId;
	private String serviceName;
	private List<Layer> layerList;
	
	public Service(String serviceId, String serviceName, List<Layer> layerList) {
		this.serviceId = serviceId;	
		this.serviceName = serviceName;
		this.layerList = layerList;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public List<Layer> getLayerList() {
		return layerList;
	}
}