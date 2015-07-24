require([
	'dojo/dom',
	'dojo/dom-construct',
	'dojo/io-query',
	'dojo/on',
	'dojo/_base/window',
	'dojo/dom-style',
	'dojo/dom-attr',
	'dojo/query',
    
	'dojo/NodeList-traverse',
	'dojo/domReady!'
	], function(dom, domConstruct, ioQuery, on, win, domStyle, domAttr, query) {
		var viewsContainer = dom.byId('views-container');
		
		var svrLayerView = dom.byId('svr-layer-view');
		var svrLayerControl = dom.byId('svr-layer-control');
		var serviceExpand = on(win.doc, '.js-service-link:click', function(e) {
			var serviceName = domAttr.get(dojo.query(this).children('.js-link-label')[0], 'innerHTML');
			var className = domAttr.get(this, 'class');
			
			if(className == 'js-service-link') {
				var ul = domConstruct.create('ul', {'id': 'js-ul-groups-' + serviceName});
				domConstruct.place(ul, this, 'after');
				
				for(var i = 1; i < 11; ++i) {
					var li = domConstruct.create('li');
					domAttr.set(li, 'class', 'js-group');
					var span = domConstruct.create('span');
					domAttr.set(span, 'class', 'js-group-link');
					var spanIcon = domConstruct.create('span');
					domAttr.set(spanIcon, 'class', 'glyphicon glyphicon-plus-sign js-link-icon');
					var spanLabel = domConstruct.create('span');
					domAttr.set(spanLabel, 'innerHTML', ' B' + [i]);
					domAttr.set(spanLabel, 'class', 'js-link-label');
					
					domConstruct.place(li, ul, 'last');
					domConstruct.place(span, li, 'last');
					domConstruct.place(spanIcon, span, 'last');
					domConstruct.place(spanLabel, span, 'last');
				}
				
				domAttr.set(this, 'class', 'js-service-link expanded');
			}
			
			if(className == 'js-service-link expanded') {
				domAttr.set(this, 'class', 'js-service-link');
				var groupSetToDel = dojo.query(this).next()[0];
				domConstruct.destroy(groupSetToDel);
			}
		});
		
		var groupExpand = on(win.doc, '.js-group-link:click', function(e) {
			var groupName= domAttr.get(dojo.query(this).children('.js-link-label')[0], 'innerHTML');
			var className = domAttr.get(this, 'class');
			
			if(className == 'js-group-link') {
				var ul = domConstruct.create('ul', {'id': 'js-ul-layers-' + groupName});
				domConstruct.place(ul, this, 'after');
				
				
				for(var i = 1; i < 11; ++i) {
					var li = domConstruct.create('li');
					domAttr.set(li, 'class', 'js-layer');
					
					var span = domConstruct.create('span');
					domAttr.set(span, 'class', 'js-layer-link');
					
					var inputCheckboxLabel = domConstruct.create('label');
					domAttr.set(inputCheckboxLabel, 'class', 'checkbox-inline');
					
					var inputCheckboxInput = domConstruct.create('input');
					domAttr.set(inputCheckboxInput, 'type', 'checkbox');
					domStyle.set(inputCheckboxInput, 'position', 'static');
					
					var spanLabel = domConstruct.create('span');
					if(i == 1) {
						domAttr.set(spanLabel, 'innerHTML', ' Bebouwde kommen in Overijssel');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check bebKomInOvrs');
					} else if(i == 2) {
						domAttr.set(spanLabel, 'innerHTML', ' Bebouwde kommen rondom Overijssel');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check bebKomRondOvrs');
					} else if(i == 3) {
						domAttr.set(spanLabel, 'innerHTML', ' Bodemgebruik in Overijssel (1993)');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check bodem1993');
					} else if(i == 4) {
						domAttr.set(spanLabel, 'innerHTML', ' Bodemgebruik in Overijssel (1996)');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check bodem1996');
					} else if(i == 5) {
						domAttr.set(spanLabel, 'innerHTML', ' Gebiedskenmerken stedelijke laag');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check gebStedLaag');
					} else if(i == 6) {
						domAttr.set(spanLabel, 'innerHTML', ' Grens projectgebied Vecht Regge');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check grensRegge');
					} else if(i == 7) {
						domAttr.set(spanLabel, 'innerHTML', ' Grenzen waterschappen in Overijssel (lijn)');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check grenzenWatLijn');
					} else if(i == 8) {
						domAttr.set(spanLabel, 'innerHTML', ' Grenzen waterschappen in Overijssel (vlak)');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check grenzenWatVlak');
					} else if(i == 9) {
						domAttr.set(spanLabel, 'innerHTML', ' Nationale parken Weerribben-Wieden en Sallandse Heuvelrug');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check weerribben');
					} else if(i == 10) {
						domAttr.set(spanLabel, 'innerHTML', ' Projecten in Overijssel');
						domAttr.set(inputCheckboxInput, 'class', 'js-layer-check projOvrs');
					}
					
					domAttr.set(spanLabel, 'class', 'js-link-label-checkbox');
					
					domConstruct.place(li, ul, 'last');
					domConstruct.place(span, li, 'last');
					domConstruct.place(inputCheckboxLabel, span, 'last');
					domConstruct.place(inputCheckboxInput, inputCheckboxLabel, 'last');
					domConstruct.place(spanLabel, span, 'last');
				}
				
				domAttr.set(this, 'class', 'js-group-link expanded');
			}
			
			if(className == 'js-group-link expanded') {
				domAttr.set(this, 'class', 'js-group-link');
				var layerSetToDel = dojo.query(this).next()[0];
				domConstruct.destroy(layerSetToDel);
			}
		});
});