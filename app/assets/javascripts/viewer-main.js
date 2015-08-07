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
			center: [720000, 6889099],
			zoom: 9
		});
		
		var map;
		var info = dom.byId('info');
		
		window.onload = function() {
	        var map = new ol.Map({
				view: view,
				layers: [
					new ol.layer.Tile({
						source: new ol.source.MapQuest({layer: 'osm'})
					}),
				],
				target: 'map'
			});
	        
	        function makeHttpObject() {
				try {return new XMLHttpRequest();}
				catch (error) {}
				try {return new ActiveXObject("Msxml2.XMLHTTP");}
				catch (error) {}
				try {return new ActiveXObject("Microsoft.XMLHTTP");}
				catch (error) {}
				throw new Error("Could not create HTTP request object.");
			}
	        
			map.on('singleclick', function(evt) {
	        	domAttr.set(info, 'innerHTML', '');
	        	var viewResolution = (view.getResolution());
	        	var sourceArray = [];
	        	
	        	var layerBebKomInOvrs = dojo.query('.bebKomInOvrs')[0];
	        	
	        	if(layerBebKomInOvrs) {
	        		if(domAttr.get(layerBebKomInOvrs, 'checked')) {
	        			sourceArray.push(bebKomInOvrsSource);
	        		}
	        	}
	        	
	        	for(var i = 0; i < sourceArray.length; ++i) {
	        		var url = sourceArray[i].getGetFeatureInfoUrl(
			        	evt.coordinate, viewResolution, 'EPSG:3857',
			        	{'INFO_FORMAT': 'text/html'}
			        );
	        		executeRequest(url);
	        	}
	        	
	        	function executeRequest(url) {
	        		var request = makeHttpObject();
	        		request.open("GET", url, true);
					request.send(null);
					request.onreadystatechange = function() {
						if (request.readyState == 4) {
							var previousHTML = domAttr.get(info, 'innerHTML');
							domAttr.set(info, 'innerHTML', previousHTML + request.responseText);
						}
					};
	        	}
			});
	        
			var viewsContainer = dom.byId('views-container');
			var svrLayerView = dom.byId('svr-layer-view');
			var svrLayerControl = dom.byId('svr-layer-control');
			
			var serviceExpand = on(win.doc, '.js-service-link:click', function(e) {
				var serviceId = domAttr.get(this.parentNode, 'data-service-id');
				var serviceNode = this.parentNode;
				
				if(this.dataset.serviceExpanded === "false") {
					xhr(jsRoutes.controllers.Application.allLayers(serviceId).url, {
						handleAs: "html"
					}).then(function(data){
						domConstruct.place(data, serviceNode);
					});
					this.dataset.serviceExpanded = "true";
				} else {
					domConstruct.destroy(query(this).siblings()[0]);
					this.dataset.serviceExpanded = "false";
				}
			});
			
			var layerExpand = on(win.doc, '.js-layer-link:click', function(e) {
				var serviceId = domAttr.get(query(this).closest(".js-service-id")[0], 'data-service-id');
				var layerId = domAttr.get(this.parentNode, 'data-layer-id');
				var layerNode = this.parentNode;
				
				if(this.dataset.layerExpanded === "false") {
					xhr(jsRoutes.controllers.Application.layers(serviceId, layerId).url, {
						handleAs: "html"
					}).then(function(data){
						domConstruct.place(data, layerNode);
					});
					this.dataset.layerExpanded = "true";
				} else {
					domConstruct.destroy(query(this).siblings()[0]);
					this.dataset.layerExpanded = "false";
				}
			});
			
			var layerCheck = on(win.doc, '.js-layer-check:change', function(e) {
				var layerName = domAttr.get(this, 'data-layer-name');
				var layerEndpoint = domAttr.get(this, 'data-layer-endpoint');
				var layerVersion = domAttr.get(this, 'data-layer-version');
	       		
				layerEndpoint = layerEndpoint.slice(0, -1);
				
	       		var layerImage = new ol.layer.Image({
		       		source: new ol.source.ImageWMS({
		    			url: layerEndpoint,
		    			params: {'LAYERS': layerName, 'VERSION': layerVersion, 'CRS': crs},
		    			serverType: serverType
		    		})
		    	});
	       		
	       		if(domAttr.get(this, 'checked')) {
	       			map.addLayer(layerImage);
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
		};
});