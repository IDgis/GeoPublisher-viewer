package models;

public class Layer {
	private String layerId;
	private String layerName;
	
	public Layer(String layerId, String layerName) {
		this.layerId = layerId;
		this.layerName = layerName;
	}

	public String getLayerId() {
		return layerId;
	}

	public String getLayerName() {
		return layerName;
	}	
}
