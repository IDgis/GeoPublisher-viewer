/* jshint -W099 */
/* jshint -W083 */
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

	'dojo/NodeList-traverse',
	'dojo/domReady!'
	], function(dom, ioQuery, on, win, query, domAttr, domConstruct, domStyle, xhr) {
		var crs = 'EPSG:28992';
		var serverType = 'geoserver';
		
		var origin = [-285401.920, 903401.920];
		var resolutions = [3440.64, 1720.32, 860.16, 430.08, 215.04, 107.52, 53.76, 26.88, 13.44, 6.72, 3.36, 1.68, 0.84, 0.42, 0.21];
		var extent = [-285401.92, 22598.08, 595401.9199999999, 903401.9199999999];
		var matrixIds0 = [];
		
		var i;
		var j;
		
		for (i = 0; i < 15; i++) {
			matrixIds0[i] = crs + ':' + i;
		}
		
		var tileGrid0 = new ol.tilegrid.WMTS({
			origin: origin,
			resolutions: resolutions,
			matrixIds: matrixIds0
		});
		
	    var matrixIds1 = [];
	    for (i = 0; i < 15; i++) {
	    	matrixIds1[i] = (i < 10 ? '0' : '') + i;
	    }
	    
	    var rd = ol.proj.get(crs);		
	    
		var view = new ol.View({
			projection: rd,
			center: [220000, 499000],
			zoom: 10
		});
		
		var map;
		var info = dom.byId('info');
		
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
		        		projection: rd,
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
        	
        	for(i = 0; i < serviceArray.length; i++) {
        		var checkedElementen = query(serviceArray[i]).query('.js-layer-check[type=checkbox]:checked');
        		var layerString = '';
        		
        		for(j = 0; j < checkedElementen.length; j++) {
        			if(j === 0) {
        				layerString = layerString.concat(checkedElementen[j].dataset.layerName);
        			} else {
        				layerString = layerString.concat(',', checkedElementen[j].dataset.layerName);
        			}
        		}
        		
        		var sourceLayer = new ol.source.ImageWMS({
    	    		url: checkedElementen[0].dataset.layerEndpoint,
    	    		params: {'LAYERS': layerString, 'VERSION': checkedElementen[0].dataset.layerVersion, 'CRS': crs},
    	    		serverType: serverType
    	    	});
        		
        		var url = sourceLayer.getGetFeatureInfoUrl(evt.coordinate, viewResolution, map.getView().getProjection(), {'INFO_FORMAT': 'text/html'});
        		
        		xhr(jsRoutes.controllers.Proxy.proxy(url).url, {
					handleAs: "html"
				}).then(function(data) {
					domConstruct.place(data, info);
				});
        	}
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
			
       		if(domAttr.get(this, 'checked')) {
       			map.addLayer(
       				new ol.layer.Image({
       					source: new ol.source.ImageWMS({
       			    		url: layerEndpoint,
       			    		params: {'LAYERS': layerName, 'VERSION': layerVersion, 'CRS': crs},
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
				for(var i = 0; i < checkedInputs.length; i++) {
					if(domAttr.get(checkedInputs[i], 'data-layer-index') > indexElement) {
						domAttr.set(checkedInputs[i], 'data-layer-index', domAttr.get(checkedInputs[i], 'data-layer-index') -1);
					}
				}
			}
		});
});