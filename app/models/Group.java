package models;

public class Group {
	private String groupId;
	private String groupName;
	
	public Group(String groupId, String groupName) {
		this.groupId = groupId;
		this.groupName = groupName;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String getGroupName() {
		return groupName;
	}
}