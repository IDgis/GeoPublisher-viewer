package models;

public class ServiceGroup {
	private Service service;
	private Group group;
	
	public ServiceGroup(Service service, Group group) {
		this.service = service;
		this.group = group;
	}
	
	public Service getService() {
		return service;
	}
	
	public Group getGroup() {
		return group;
	}
}
