package models;

import java.util.List;

public class Layer {
	private String layerId;
	private String layerName;
	private List<Layer> layerList;
	
	public Layer(String layerId, String layerName, List<Layer> layerList) {
		this.layerId = layerId;
		this.layerName = layerName;
		this.layerList = layerList;
	}
	
	public String getLayerId() {
		return layerId;
	}
	
	public String getLayerName() {
		return layerName;
	}
	
	public List<Layer> getLayerList() {
		return layerList;
	}
}