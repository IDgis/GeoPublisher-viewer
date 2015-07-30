package models;

public class Group {
	private Service service;
	private String groupId;
	private String groupName;
	
	public Group(Service service, String groupId, String groupName) {
		this.service = service;
		this.groupId = groupId;
		this.groupName = groupName;
	}
	
	public Service getService() {
		return service;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String getGroupName() {
		return groupName;
	}
}