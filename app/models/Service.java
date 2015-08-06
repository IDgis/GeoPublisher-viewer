package models;

public class Service {
	private String serviceId;
	private String serviceName;
	private String endpoint;
	private String version;
	
	public Service(String serviceId, String serviceName, String endpoint, String version) {
		this.serviceId = serviceId;	
		this.serviceName = serviceName;
		this.endpoint= endpoint;
		this.version = version;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public String getEndpoint() {
		return endpoint;
	}
	
	public String getVersion() {
		return version;
	}
}