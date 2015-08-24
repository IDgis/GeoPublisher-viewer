package models;

/**
 * The model class to store information for the WMS service.
 * 
 * @author Sandro
 *
 */
public class Service {
	private String serviceId;
	private String serviceName;
	private String endpoint;
	private String version;
	
	/**
	 * The constructor for the service class.
	 * 
	 * @param serviceId - the id of the service.
	 * @param serviceName - the name of the service.
	 * @param endpoint - the endpoint of the service.
	 * @param version - the version of the service.
	 */
	public Service(String serviceId, String serviceName, String endpoint, String version) {
		this.serviceId = serviceId;	
		this.serviceName = serviceName;
		this.endpoint= endpoint;
		this.version = version;
	}
	
	/**
	 * Fetches the id of the service.
	 * 
	 * @return the string of the service id.
	 */
	public String getServiceId() {
		return serviceId;
	}
	
	/**
	 * Fetches the name of the service.
	 * 
	 * @return the string of the service name.
	 */
	public String getServiceName() {
		return serviceName;
	}
	
	/**
	 * Fetches the endpoint of the service.
	 * 
	 * @return the string of the service endpoint.
	 */
	public String getEndpoint() {
		return endpoint;
	}
	
	/**
	 * Fetches the version of the service.
	 * 
	 * @return the string of the version of the service.
	 */
	public String getVersion() {
		return version;
	}
}