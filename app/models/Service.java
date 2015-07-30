package models;

public class Service {
	private String serviceId;
	private String serviceNaam;
	
	public Service(String serviceId, String serviceNaam) {
		this.serviceId = serviceId;	
		this.serviceNaam = serviceNaam;	
	}
	
	public String getServiceId() {
		return serviceId;
	}
	
	public String getServiceNaam() {
		return serviceNaam;
	}
}