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
		var divViewCnt = dom.byId('viewer-container');
		var divView = dom.byId('srv-layer-view');
		var divInfo = dom.byId('info-container');
		var info = dom.byId('info');
		var map;
		
		var setWrapperHeight = domStyle.set(wrapper, 'height', window.innerHeight-130 + 'px');
		var setViewCntHeight = domStyle.set(divViewCnt, 'height', window.innerHeight-130 + 'px');
	    
	    var centerString = domAttr.get(dom.byId('js-app-centers'), 'value');
	    var centers = centerString.split(',');
	    
	    for(var centerInt = 0; centerInt < centers.length; centerInt++) {
	    	centers[centerInt] = parseInt(centers[centerInt], 10);
	    }
		
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
	    	extent: extent
	    });
	    
	    var view = new ol.View({
			projection: projection,
			center: centers,
			zoom: 5
		});
	    
	    var achtergrond = new ol.layer.Tile({
    		opacity: 0.8,
    		extent: extent,
        	source : new ol.source.WMTS({
        		attributions: [],
        		url: '//geodata.nationaalgeoregister.nl/tiles/service/wmts',
        		layer: 'opentopoachtergrondkaart',
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
		
		
		var layerEndpoint = domAttr.get(dom.byId('js-data-service-layer'), 'data-endpoint');
		var layerEndpointIndex = layerEndpoint.indexOf('/');
		var finalLayerEndpoint = layerEndpoint.substring(layerEndpointIndex);
		
		var service = domAttr.get(dom.byId('js-data-service-layer'), 'data-service');
		var layerName = domAttr.get(dom.byId('js-data-service-layer'), 'data-layer');
		
		
		map.addLayer(
			new ol.layer.Image({
				source: new ol.source.ImageWMS({
				   url: finalLayerEndpoint + service + '/wms?',
				   params: {'LAYERS': layerName, 'VERSION': '1.3.0'},
				   serverType: serverType
				})
			})
		);
		
		var closeFeatureInfoNode = dom.byId('map-full-view');
		var firstClick = 0;
		map.on('singleclick', function(evt) {
			domAttr.set(info, 'innerHTML', '');
        	var viewResolution = (map.getView().getResolution());
        	var layersArray = map.getLayers().getArray();
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
        	
			var sourceLayer = new ol.source.ImageWMS({
	    		url: finalLayerEndpoint + service + '/wms?',
	    		params: {'LAYERS': layerName, 'VERSION': '1.3.0', 'FEATURE_COUNT': '50'},
	    		serverType: serverType
	    	});
    		
    		var url = sourceLayer.getGetFeatureInfoUrl(evt.coordinate, viewResolution, map.getView().getProjection(), {'INFO_FORMAT': 'text/html'});
    		url += '&' + 'encoding=UTF-8';
    		
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
		
		
		var closeFeatureInfo = on(closeFeatureInfoNode, 'click', function(e) {
			domAttr.set(closeFeatureInfoNode, 'class', 'btn btn-default btn-xs disabled');
			domStyle.set(divInfo, 'display', 'none');
			domStyle.set(divView, 'height', '100%');
			domStyle.set(divView, 'margin-bottom', '0px');
			domStyle.set(divInfo, 'height', '0%');
			
			map.updateSize();
		});
});