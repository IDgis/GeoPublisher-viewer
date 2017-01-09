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
		
		var wrapper = dom.byId('wrapper');
		var divLayerControlCnt = dom.byId('layer-control-container');
		var divViewCnt = dom.byId('viewer-container');
		var divView = dom.byId('srv-layer-view');
		var divControl = dom.byId('srv-layer-control');
		var divInfo = dom.byId('info-container');
		var info = dom.byId('info');
		var uncheck = dom.byId('uncheck-all');
		var map;
		
		var setWrapperHeight = domStyle.set(wrapper, 'height', window.innerHeight-130 + 'px');
		var setControlCntHeight = domStyle.set(divLayerControlCnt, 'height', window.innerHeight-130 + 'px');
		var setViewCntHeight = domStyle.set(divViewCnt, 'height', window.innerHeight-130 + 'px');
		var setControlHeight = domStyle.set(divControl, 'height', domStyle.get(divControl, 'height')-domStyle.get(uncheck, 'height') + 'px');
		
		var uncheckHeight = domStyle.get(uncheck, 'height');
		var setCntsHeight = on(window, 'resize', function(evt) {
			domStyle.set(divLayerControlCnt, 'height', window.innerHeight-130 + 'px');
			domStyle.set(divControl, 'height', '100%');
			domStyle.set(divControl, 'height', domStyle.get(divControl, 'height')-uncheckHeight + 'px');
			domStyle.set(wrapper, 'height', window.innerHeight-130 + 'px');
			
			if(domStyle.get(divInfo, 'display') === 'none') {
				domStyle.set(divViewCnt, 'height', window.innerHeight-130 + 'px');
			} else {
				domStyle.set(divViewCnt, 'height', window.innerHeight-130 + 'px');
				
				domStyle.set(divView, 'height', '75%');
				domStyle.set(divView, 'margin-bottom', '15px');
				domStyle.set(divInfo, 'height', '25%');
				domStyle.set(divInfo, 'height', domStyle.get(divInfo, 'height')-15 + 'px');
				
				map.updateSize();
			}
		});
		
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
	    
	    var achtergrond = new ol.layer.Tile({
    		opacity: 0.8,
    		extent: extent,
        	source : new ol.source.WMTS({
        		attributions: [],
        		url: '//geodata.nationaalgeoregister.nl/wmts',
        		layer: 'brtachtergrondkaartpastel',
        		matrixSet: crs,
        		format: 'image/png',
        		tileGrid: tileGrid0,
        		style: 'default'
        	}),
        	visible: true
        });
	    
	    var iconStyle = new ol.style.Style({
            image: new ol.style.Icon(({
                anchor: [0.5, 32],
                anchorXUnits: 'fraction',
                anchorYUnits: 'pixels',
                opacity: 0.75,
                src: jsRoutes.controllers.Assets.versioned('images/location.svg').url
            }))
        });
	    
	    var iconGeometry = new ol.geom.Point([220000, 499000]);
        var iconFeature = new ol.Feature({
            geometry: iconGeometry
        });
	    
	    var vectorSource = new ol.source.Vector({
            features: [iconFeature]
        });

        var vectorLayer = new ol.layer.Vector({
            source: vectorSource
        });
        
        iconFeature.setStyle(iconStyle);
	    
        var zoomControl = new ol.control.Zoom();
        
		map = new ol.Map({
			layers: [
		    	achtergrond
			],
			control: zoomControl,
			target: 'map',
			view: view
		});
		
		var closeFeatureInfoNode = dom.byId('map-full-view');
		var firstClick = 0;
		map.on('singleclick', function(evt) {
			domAttr.set(info, 'innerHTML', '');
        	var viewResolution = (map.getView().getResolution());
        	var layersArray = map.getLayers().getArray();
        	var serviceArray = query('.js-layer-check[type=checkbox]:checked').closest('.js-service-id');
        	var featureInfoTable = false;
        	var counter = 0;
        	
        	if(firstClick !== 0) {
        		map.removeLayer(vectorLayer);
        	}
        	map.addLayer(vectorLayer);
        	iconGeometry.setCoordinates(evt.coordinate);
        	firstClick++;
        	
        	domAttr.set(closeFeatureInfoNode, 'class', 'btn btn-default btn-xs');
        	
        	domStyle.set(divInfo, 'display', 'block');
			domStyle.set(divView, 'height', '75%');
			domStyle.set(divView, 'margin-bottom', '15px');
			domStyle.set(divInfo, 'height', '25%');
			domStyle.set(divInfo, 'height', domStyle.get(divInfo, 'height')-15 + 'px');
			
			map.updateSize();
        	
			if(serviceArray.length === 0) {
				domConstruct.place('<span><strong>Niets gevonden.</strong></span>', info);
			}
			
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
        		
        		xhr(jsRoutes.controllers.GetFeatureInfoProxy.proxy(url).url, {
					handleAs: "html"
				}).then(function(data) {
					var nfBoolean = data.indexOf('data-empty-feature-info="true"') > -1;
					counter++;
					
					if(!nfBoolean) {
						domConstruct.place(data, info);
						featureInfoTable = true;
					} else if(counter === serviceArray.length && featureInfoTable === false) {
						domConstruct.place(data, info);
					}
				});
        	});
		});
		
		
		var closeFeatureInfo = on(closeFeatureInfoNode, 'click', function(e) {
			domAttr.set(closeFeatureInfoNode, 'class', 'btn btn-default btn-xs disabled');
			domStyle.set(divInfo, 'display', 'none');
			domStyle.set(divView, 'height', '100%');
			domStyle.set(divView, 'margin-bottom', '0px');
			domStyle.set(divInfo, 'height', '0%');
			
			map.updateSize();
		});
        
		on(win.doc, '.js-service-link:click', function(e) {
			serviceExpand(this);
		});
		
		var serviceParamExist = domAttr.get(dom.byId('js-service-param-exist'), 'value');
		if(serviceParamExist !== '') {
			serviceExpand(query('.js-service-link')[0]);
		}
		
		function serviceExpand(node) {
			var serviceNode = node.parentNode;
			var serviceId = domAttr.get(serviceNode, 'data-service-id');
			var serviceIconNode = query(node).query('.js-link-icon')[0];

			if(node.dataset.serviceStatus === "none") {
				xhr(jsRoutes.controllers.Application.allLayers(serviceId).url, {
					handleAs: "html"
				}).then(function(data){
					domConstruct.place(data, serviceNode);
					domAttr.set(serviceIconNode, 'class', 'glyphicon glyphicon-minus-sign js-link-icon');
				});
				node.dataset.serviceStatus = "created";
			} else if(node.dataset.serviceStatus == "created") {
				domStyle.set(query(node).siblings()[0], 'display', 'none');
				domAttr.set(serviceIconNode, 'class', 'glyphicon glyphicon-plus-sign js-link-icon');
				node.dataset.serviceStatus = "hidden";
			} else {
				domStyle.set(query(node).siblings()[0], 'display', 'block');
				domAttr.set(serviceIconNode, 'class', 'glyphicon glyphicon-minus-sign js-link-icon');
				node.dataset.serviceStatus = "created";
			}
		}
		
		var layerExpand = on(win.doc, '.js-layer-link:click', function(e) {
			var serviceId = domAttr.get(query(this).closest(".js-service-id")[0], 'data-service-id');
			var layerId = domAttr.get(this.parentNode, 'data-layer-id');
			var layerNode = this.parentNode;
			var layerIconNode = query(this).query('.js-link-icon')[0];
			
			if(this.dataset.layerStatus === "none") {
				xhr(jsRoutes.controllers.Application.layers(serviceId, layerId).url, {
					handleAs: "html"
				}).then(function(data){
					domConstruct.place(data, layerNode);
					domAttr.set(layerIconNode, 'class', 'glyphicon glyphicon-minus-sign js-link-icon');
				});
				this.dataset.layerStatus = "created";
			} else if(this.dataset.layerStatus === "created") {
				domStyle.set(query(this).siblings()[0], 'display', 'none');
				domAttr.set(layerIconNode, 'class', 'glyphicon glyphicon-plus-sign js-link-icon');
				this.dataset.layerStatus = "hidden";
			} else {
				domStyle.set(query(this).siblings()[0], 'display', 'block');
				domAttr.set(layerIconNode, 'class', 'glyphicon glyphicon-minus-sign js-link-icon');
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
   				
   				map.removeLayer(map.getLayers().removeAt(indexElement));
				domAttr.set(this, 'data-layer-index', '');
				
				var checkedInputs = query('.js-layer-check:checked');
				array.forEach(checkedInputs, function(checkedInput) {
					if(domAttr.get(checkedInput, 'data-layer-index') > indexElement) {
						domAttr.set(checkedInput, 'data-layer-index', domAttr.get(checkedInput, 'data-layer-index') - 1);
					}
				});
			}
		});
		
		var uncheckAllNode = dom.byId('uncheck-all');
		var uncheckAll = on(uncheckAllNode, 'click', function(e) {
			map.getLayers().clear();
			map.addLayer(achtergrond);
			
			var checkedInputs = query('.js-layer-check:checked');
			array.forEach(checkedInputs, function(checkedInput) {
				domAttr.set(checkedInput, 'data-layer-index', '');
				domAttr.set(checkedInput, 'checked', false);
			});
		});
});