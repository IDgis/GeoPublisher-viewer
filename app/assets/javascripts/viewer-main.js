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
		var origin = [-285401.920, 903401.920];
		var resolutions = [3440.64, 1720.32, 860.16, 430.08, 215.04, 107.52, 53.76, 26.88, 13.44, 6.72, 3.36, 1.68, 0.84, 0.42, 0.21];
		var extent = [-285401.92, 22598.08, 595401.9199999999, 903401.9199999999];
		var matrixIds0 = [];
		
		var i;
		
		for (i = 0; i < 15; i++) {
			matrixIds0[i] = 'EPSG:28992:' + i;
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
	    
	    var rd = ol.proj.get('EPSG:28992');		
	    
		var view = new ol.View({
			projection: rd,
			center: [230000, 501500],
			zoom: 10
		});
		
		var map;
		var info = dom.byId('info');
		
		window.onload = function() {
	        var map = new ol.Map({
				view: view,
				layers: [
					new ol.layer.Tile({
		    	  		source : new ol.source.WMTS({
							title: 'BRT Achtergrondkaart', 
				    	  	overlay: false,
				    	  	opacity: 0.8,
							extent: extent,
				    	  	attributions: [],
					        url: 'http://geodata.nationaalgeoregister.nl/wmts',
					        layer: 'brtachtergrondkaart',
					        matrixSet: 'EPSG:28992',
					        format: 'image/png',
					        projection: rd,
					        tileGrid: tileGrid0,
					        style: 'default'
						}),
						visible: true
		    	  	})
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
	        	var layerBebKomRondOvrs = dojo.query('.bebKomRondOvrs')[0];
	        	var layerBodem1993 = dojo.query('.bodem1993')[0];
	        	var layerBodem1996 = dojo.query('.bodem1996')[0];
	        	var layerGebStedLaag = dojo.query('.gebStedLaag')[0];
	        	var layerGrensRegge = dojo.query('.grensRegge')[0];
	        	var layerGrenzenWatLijn = dojo.query('.grenzenWatLijn')[0];
	        	var layerGrenzenWatVlak = dojo.query('.grenzenWatVlak')[0];
	        	var layerWeerribben = dojo.query('.weerribben')[0];
	        	var layerProjOvrs = dojo.query('.projOvrs')[0];
	        	
	        	if(layerBebKomInOvrs) {
	        		if(domAttr.get(layerBebKomInOvrs, 'checked')) {
	        			sourceArray.push(bebKomInOvrsSource);
	        		} if(domAttr.get(layerBebKomRondOvrs, 'checked')) {
	        			sourceArray.push(bebKomRondOvrsSource);
	        		} if(domAttr.get(layerBodem1993, 'checked')) {
	        			sourceArray.push(bodem1993Source);
	        		} if(domAttr.get(layerBodem1996, 'checked')) {
	        			sourceArray.push(bodem1996Source);
	        		} if(domAttr.get(layerGebStedLaag, 'checked')) {
	        			sourceArray.push(gebStedLaagSource);
	        		} if(domAttr.get(layerGrensRegge, 'checked')) {
	        			sourceArray.push(grensReggeSource);
	        		} if(domAttr.get(layerGrenzenWatLijn, 'checked')) {
	        			sourceArray.push(grenzenWatLijnSource);
	        		} if(domAttr.get(layerGrenzenWatVlak, 'checked')) {
	        			sourceArray.push(grenzenWatVlakSource);
	        		} if(domAttr.get(layerWeerribben, 'checked')) {
	        			sourceArray.push(weerribbenSource);
	        		} if(domAttr.get(layerProjOvrs, 'checked')) {
	        			sourceArray.push(projOvrsSource);
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
						console.log(request.readyState);
						if (request.readyState == 4) {
							var previousHTML = domAttr.get(info, 'innerHTML');
							console.log(url);
							domAttr.set(info, 'innerHTML', previousHTML + request.responseText);
						}
					};
	        	}
			});
	        
			var bebKomInOvrsSource = new ol.source.ImageWMS({
    			url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Bebouwde_kommen_in_Overijssel', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
			var bebKomInOvrs = new ol.layer.Image({
	       		source: bebKomInOvrsSource
	    	});
	       	
			var bebKomRondOvrsSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Bebouwde_kommen_rondom_Overijssel', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var bebKomRondOvrs = new ol.layer.Image({
	       		source: bebKomRondOvrsSource
	    	});
	       	
	       	var bodem1993Source = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Bodemgebruik_In_Overijssel_1993', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var bodem1993 = new ol.layer.Image({
	       		source: bodem1993Source
	    	});
	       	
	       	var bodem1996Source = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Bodemgebruik_Overijssel_1996', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var bodem1996 = new ol.layer.Image({
	       		source: bodem1996Source
	    	});
	       	
	       	var gebStedLaagSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Gebiedskenmerken_Stedelijke_laag', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var gebStedLaag = new ol.layer.Image({
	       		source: gebStedLaagSource
	    	});
	       	
	       	var grensReggeSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Grens_projectgebied_Vecht_Regge', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var grensRegge = new ol.layer.Image({
	       		source: grensReggeSource
	    	});
	       	
	       	var grenzenWatLijnSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Grenzen_waterschappen_in_Overijssel_lijn', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var grenzenWatLijn = new ol.layer.Image({
	       		source: grenzenWatLijnSource
	    	});
	       	
	       	var grenzenWatVlakSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Grenzen_waterschappen_in_Overijssel_vlak', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var grenzenWatVlak = new ol.layer.Image({
	       		source: grenzenWatVlakSource
	    	});
	       	
	       	var weerribbenSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Nationale_parken_Weerribben_Wieden_en_Sallandse_Heuvelrug', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var weerribben = new ol.layer.Image({
	       		source: weerribbenSource
	    	});
	       	
	       	var projOvrsSource = new ol.source.ImageWMS({
				url: 'http://support.idgis.eu/staging-services-ov/geoserver/OV_B0/wms',
    			params: {'LAYERS': 'B0_Projecten_in_Overijssel', 'VERSION': '1.1.0', 'CRS': 'EPSG:28992'},
    			serverType: 'geoserver'
    		});
	       	var projOvrs = new ol.layer.Image({
	       		source: projOvrsSource
	    	});
	       	
	       	var viewsContainer = dom.byId('views-container');
			
			var svrLayerView = dom.byId('svr-layer-view');
			var svrLayerControl = dom.byId('svr-layer-control');
			var serviceExpand = on(win.doc, '.js-service-link:click', function(e) {
				var serviceId = domAttr.get(this.parentNode, 'data-service-id');
				var serviceNode = this.parentNode;
				
				if(this.dataset.serviceExpanded === "false") {
					xhr(jsRoutes.controllers.Application.services(serviceId).url, {
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
			
			var groupExpand = on(win.doc, '.js-group-link:click', function(e) {
				
			});
			
	       	var layerCheck = on(win.doc, '.js-layer-check:change', function(e) {
				var classNameElement = domAttr.get(this, 'class');
				var classNameId = classNameElement.split(' ')[1];
	       		var layerList = dojo.query('.' + classNameId);
	       		
	       		for(var i = 0; i < layerList.length; ++i) {
	       			domAttr.set(layerList[i], 'checked', !!domAttr.get(this, 'checked'));
	       		}
	       		
	       		if(classNameId == 'bebKomInOvrs') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(bebKomInOvrs);
	       			} else {
						map.removeLayer(bebKomInOvrs);
					}
	       		}
	       		
	       		if(classNameId == 'bebKomRondOvrs') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(bebKomRondOvrs);
	       			} else {
						map.removeLayer(bebKomRondOvrs);
					}
	       		}
	       		
	       		if(classNameId == 'bodem1993') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(bodem1993);
	       			} else {
						map.removeLayer(bodem1993);
					}
	       		}
	       		
	       		if(classNameId == 'bodem1996') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(bodem1996);
	       			} else {
						map.removeLayer(bodem1996);
					}
	       		}
	       		
	       		if(classNameId == 'gebStedLaag') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(gebStedLaag);
	       			} else {
						map.removeLayer(gebStedLaag);
					}
	       		}
	       		
	       		if(classNameId == 'grensRegge') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(grensRegge);
	       			} else {
						map.removeLayer(grensRegge);
					}
	       		}
	       		
	       		if(classNameId == 'grenzenWatLijn') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(grenzenWatLijn);
	       			} else {
						map.removeLayer(grenzenWatLijn);
					}
	       		}
	       		
	       		if(classNameId == 'grenzenWatVlak') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(grenzenWatVlak);
	       			} else {
						map.removeLayer(grenzenWatVlak);
					}
	       		}
	       		
	       		if(classNameId == 'weerribben') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(weerribben);
	       			} else {
						map.removeLayer(weerribben);
					}
	       		}
	       		
	       		if(classNameId == 'projOvrs') {
	       			if(domAttr.get(this, 'checked')) {
	       				map.addLayer(projOvrs);
	       			} else {
						map.removeLayer(projOvrs);
					}
	       		}
	       		
	       		
			});
	    };
});