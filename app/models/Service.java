package models;

public class Service {
	private String serviceId;
	private String serviceName;
	private String url;
	private String domain;
	
	public Service(String serviceId, String serviceName, String domain, String url) {
		this.serviceId = serviceId;	
		this.serviceName = serviceName;
		this.url = url;
		this.domain = domain;
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getDomain() {
		return domain;
	}
}