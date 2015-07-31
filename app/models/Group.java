package models;

import java.util.List;

public class Group {
	private String groupId;
	private String groupName;
	private List<Group> groupList;
	private List<Layer> layerList;
	
	public Group(String groupId, String groupName, List<Group> groupList, List<Layer> layerList) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.groupList = groupList;
		this.layerList = layerList;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public List<Group> getGroupList() {
		return groupList;
	}
	
	public List<Layer> getLayerList() {
		return layerList;
	}
}