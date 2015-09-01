package models;

import java.util.Objects;

/**
 * The model class to store information for the WMS.
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
	 * @param serviceId the id of the service, the id is unique and for technical purposes to recognize
	 * the service
	 * @param serviceName the name of the service, the name functions as a logical name so the user 
	 * can recognize it easily
	 * @param endpoint the endpoint of the service, the endpoint is an url and must end with a question
	 * mark or ampersand so the parameters can be inserted after the endpoint
	 * @param version the version of the WMS, it needs to be an official recognized version for a WMS
	 */
	public Service(String serviceId, String serviceName, String endpoint, String version) {
		this.serviceId = Objects.requireNonNull(serviceId);
		this.serviceName = Objects.requireNonNull(serviceName);
		this.endpoint= Objects.requireNonNull(endpoint);
		this.version = Objects.requireNonNull(version);
	}
	
	/**
	 * Fetches the id of the service. The id is unique and for technical purposes to recognize the service.
	 * 
	 * @return The id of the service.
	 */
	public String getServiceId() {
		return serviceId;
	}
	
	/**
	 * Fetches the name of the service. The name functions as a logical name so the user can recognize it 
	 * easily.
	 * 
	 * @return The name of the service.
	 */
	public String getServiceName() {
		return serviceName;
	}
	
	/**
	 * Fetches the endpoint of the service. The endpoint is an url and must end with a question mark or ampersand 
	 * so the parameters can be inserted after the endpoint.
	 * 
	 * @return The endpoint of the service.
	 */
	public String getEndpoint() {
		return endpoint;
	}
	
	/**
	 * Fetches the version of the WMS.
	 * 
	 * @return The version of the service.
	 */
	public String getVersion() {
		return version;
	}
}