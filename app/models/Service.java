package models;

public class Service {
	private String serviceId;
	private String serviceName;
	
	public Service(String serviceId, String serviceName) {
		this.serviceId = serviceId;	
		this.serviceName = serviceName;	
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
}