package models;

public class GroupLayer {
	private Group group;
	private Layer layer;
	
	public GroupLayer(Group group, Layer layer) {
		this.group = group;
		this.layer = layer;
	}
	
	public Group getGroup() {
		return group;
	}
	
	public Layer getLayer() {
		return layer;
	}
}
