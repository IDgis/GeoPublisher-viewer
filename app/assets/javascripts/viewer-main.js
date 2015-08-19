/* jshint -W099 */
require([
	'dojo/dom',
	'dojo/io-query',
	'dojo/on',
	'dojo/_base/window',
	'dojo/query',
	'dojo/dom-attr',
	'dojo/dom-construct',
	'dojo/dom-style',
	'dojo/request/xhr',
	'dojo/_base/array',

	'dojo/NodeList-traverse',
	'dojo/domReady!'
	], function(dom, ioQuery, on, win, query, domAttr, domConstruct, domStyle, xhr, array) {
		var crs = 'EPSG:28992';
		var serverType = 'geoserver';
		
		var divLayerControlCnt = dom.byId('layer-control-container');
		var divViewCnt = dom.byId('viewer-container');
		
		var setLayerControlCntHeight = domStyle.set(divLayerControlCnt, 'height', window.innerHeight-130 + 'px');
		var setViewCntHeight = domStyle.set(divViewCnt, 'height', window.innerHeight-130 + 'px');
		
		var setCntsHeight = on(window, 'resize', function(evt) {
			domStyle.set(divLayerControlCnt, 'height', window.innerHeight-130 + 'px');
			domStyle.set(divViewCnt, 'height', window.innerHeight-130 + 'px');
		});
		
		var divView = dom.byId('srv-layer-view');
		var divInfo = dom.byId('info-container');
		var info = dom.byId('info');
		var map;
		
		var origin = [-285401.920, 903401.920];
		var resolutions = [3440.64, 1720.32, 860.16, 430.08, 215.04, 107.52, 53.76, 26.88, 13.44, 6.72, 3.36, 1.68, 0.84, 0.42, 0.21];
		var extent = [-285401.92, 22598.08, 595401.9199999999, 903401.9199999999];
		
		var matrixIds0 = [];
		matrixIds0 = array.map(resolutions, function(resolution) {
			var indexResolution = resolutions.indexOf(resolution);
			return crs + ':' + indexResolution;
		});
		
		var tileGrid0 = new ol.tilegrid.WMTS({
			origin: origin,
			resolutions: resolutions,
			matrixIds: matrixIds0
		});
		
	    var matrixIds1 = [];
	    matrixIds1 = array.map(resolutions, function(resolution) {
			var indexResolution = resolutions.indexOf(resolution);
			return (indexResolution < 10 ? '0' : '') + indexResolution;
		});
	    
	    var projection = new ol.proj.Projection({
	    	code: 'EPSG:28992',
	    	extent: [-285401.92, 22598.08, 595401.9199999999, 903401.9199999999]
	    });
	    
	    var view = new ol.View({
			projection: projection,
			center: [220000, 499000],
			zoom: 5
		});
		
		map = new ol.Map({
			layers: [
		    	new ol.layer.Tile({
		    		overlay: false,
		    		opacity: 0.8,
		    		extent: extent,
		        	source : new ol.source.WMTS({
		        		attributions: [],
		        		url: 'http://geodata.nationaalgeoregister.nl/wmts',
		        		layer: 'brtachtergrondkaart',
		        		matrixSet: crs,
		        		format: 'image/png',
		        		tileGrid: tileGrid0,
		        		style: 'default',
		        	}),
		        	visible: true
		        })
			],
			target: 'map',
			view: view
		});
		
		map.on('singleclick', function(evt) {
			domAttr.set(info, 'innerHTML', '');
        	var viewResolution = (map.getView().getResolution());
        	var layersArray = map.getLayers().getArray();
        	var serviceArray = query('.js-layer-check[type=checkbox]:checked').closest('.js-service-id');
        	
        	array.forEach(serviceArray, function(service) {
        		var checkedElementen = query(service).query('.js-layer-check[type=checkbox]:checked');
        		var layerString = '';
        		
        		array.forEach(checkedElementen, function(checkedElement) {
        			if(checkedElementen.indexOf(checkedElement) === 0) {
        				layerString = layerString.concat(checkedElement.dataset.layerName);
        			} else {
        				layerString = layerString.concat(',', checkedElement.dataset.layerName);
        			}
        		});
        		
        		var sourceLayer = new ol.source.ImageWMS({
    	    		url: checkedElementen[0].dataset.layerEndpoint,
    	    		params: {'LAYERS': layerString, 'VERSION': checkedElementen[0].dataset.layerVersion, 'FEATURE_COUNT': '50'},
    	    		serverType: serverType
    	    	});
        		
        		var encodingValue = 'UTF-8';
        		var url = sourceLayer.getGetFeatureInfoUrl(evt.coordinate, viewResolution, map.getView().getProjection(), {'INFO_FORMAT': 'text/html'});
        		url += '&' + 'encoding' + '=' + encodingValue;
        		
        		xhr(jsRoutes.controllers.Proxy.proxy(url).url, {
					handleAs: "html"
				}).then(function(data) {
					domConstruct.place(data, info);
					
					if(query('table.featureInfo')[0]) {
						domStyle.set(divView, 'height', '60%');
						domStyle.set(divView, 'margin-bottom', '15px');
						domStyle.set(divInfo, 'height', '35%');
						domStyle.set(divInfo, 'height', domStyle.get(divInfo, 'height')-15 + 'px');
					} else {
						domStyle.set(divView, 'height', '95%');
						domStyle.set(divView, 'margin-bottom', '0px');
						domStyle.set(divInfo, 'height', '0%');
					}
					
					map.updateSize();
				});
        	});
		});
        
		var serviceExpand = on(win.doc, '.js-service-link:click', function(e) {
			var serviceId = domAttr.get(this.parentNode, 'data-service-id');
			var serviceNode = this.parentNode;
			
			if(this.dataset.serviceStatus === "none") {
				xhr(jsRoutes.controllers.Application.allLayers(serviceId).url, {
					handleAs: "html"
				}).then(function(data){
					domConstruct.place(data, serviceNode);
				});
				this.dataset.serviceStatus = "created";
			} else if(this.dataset.serviceStatus == "created") {
				domStyle.set(query(this).siblings()[0], 'display', 'none');
				this.dataset.serviceStatus = "hidden";
			} else {
				domStyle.set(query(this).siblings()[0], 'display', 'block');
				this.dataset.serviceStatus = "created";
			}
		});
		
		var layerExpand = on(win.doc, '.js-layer-link:click', function(e) {
			var serviceId = domAttr.get(query(this).closest(".js-service-id")[0], 'data-service-id');
			var layerId = domAttr.get(this.parentNode, 'data-layer-id');
			var layerNode = this.parentNode;
			
			if(this.dataset.layerStatus === "none") {
				xhr(jsRoutes.controllers.Application.layers(serviceId, layerId).url, {
					handleAs: "html"
				}).then(function(data){
					domConstruct.place(data, layerNode);
				});
				this.dataset.layerStatus = "created";
			} else if(this.dataset.layerStatus === "created") {
				domStyle.set(query(this).siblings()[0], 'display', 'none');
				this.dataset.layerStatus = "hidden";
			} else {
				domStyle.set(query(this).siblings()[0], 'display', 'block');
				this.dataset.layerStatus = "created";
			}
		});
		
		var layerCheck = on(win.doc, '.js-layer-check:change', function(e) {
			var layerName = domAttr.get(this, 'data-layer-name');
			var layerEndpoint = domAttr.get(this, 'data-layer-endpoint');
			var layerVersion = domAttr.get(this, 'data-layer-version');
			
			var layerToAdd = new ol.source.ImageWMS({
		    		url: layerEndpoint,
		    		params: {'LAYERS': layerName, 'VERSION': layerVersion, 'CRS': 'EPSG:28922'},
		    		serverType: serverType
		    });
			
			if(domAttr.get(this, 'checked')) {
       			map.addLayer(
       					new ol.layer.Image({
       						source: new ol.source.ImageWMS({
       				    		url: layerEndpoint,
       				    		params: {'LAYERS': layerName, 'VERSION': '1.3.0'},
       				    		serverType: serverType
       						})
       					})	
       			);
       			domAttr.set(this, 'data-layer-index', map.getLayers().getLength() - 1);
   			} else {
				var indexElement = domAttr.get(this, 'data-layer-index');
   				
   				map.removeLayer(map.getLayers().removeAt(domAttr.get(this, 'data-layer-index')));
				domAttr.set(this, 'data-layer-index', '');
				
				var checkedInputs = query('.js-layer-check:checked');
				
				array.forEach(checkedInputs, function(checkedInput) {
					if(domAttr.get(checkedInput, 'data-layer-index') > indexElement) {
						domAttr.set(checkedInput, 'data-layer-index', domAttr.get(checkedInput, 'data-layer-index') -1);
					}
				});
			}
		});
});