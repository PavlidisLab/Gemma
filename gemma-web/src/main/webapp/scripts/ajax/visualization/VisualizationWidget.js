Ext.namespace('Gemma');

/**
 * @extends Ext.data.Store
 */

Gemma.VisualizationStore = function(config) {

	this.record = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "eevo"
	}, {
		name : "profiles"
	}]);

	if (config && config.readMethod)
		this.readMethod = config.readMethod;
	else
		this.readMethod = DEDVController.getDEDVForCoexpressionVisualization;

	this.proxy = new Ext.data.DWRProxy(this.readMethod);

	this.reader = new Ext.data.ListRangeReader({
		id : "id"
	}, this.record);

	Gemma.VisualizationStore.superclass.constructor.call(this, config);

};

/**
 * 
 * @class Gemma.VisualizationStore
 * @extends Ext.data.Store
 */

Ext.extend(Gemma.VisualizationStore, Ext.data.Store, {});

Gemma.PLOT_SIZE = 100;
Gemma.ZOOM_PLOT_SIZE = 400;

Gemma.ProfileTemplate = Ext.extend(Ext.XTemplate, {

	graphConfig : {
		lines : {
			lineWidth : 1
		},
		xaxis : {
			noTicks : 0
		},
		yaxis : {
			noTicks : 0
		},
		grid : {
			color : "white"
		},
		shadowSize : 0,
		
		legend : {
			show : false
		}
	},

	overwrite : function(el, values, ret) {
		Gemma.ProfileTemplate.superclass.overwrite.call(this, el, values, ret);
		for (var i = 0; i < values.length; i++) {
			var record = values[i];
			var shortName = record.eevo.shortName;
			var newDiv = Ext.DomHelper.append(shortName + '_vizwrap', {
				tag : 'div',
				id : shortName + "_vis",
				style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px;'
			});

			// Must use prototype extraction here -- putting in newDiv fails.
			console.log(record.profiles);
			Flotr.draw($(shortName + "_vis"), record.profiles, this.graphConfig);
		}
	}
});

Gemma.HeatmapTemplate = Ext.extend(Ext.XTemplate, {

	overwrite : function(el, values, ret) {
		Gemma.HeatmapTemplate.superclass.overwrite.call(this, el, values, ret);
		for (var i = 0; i < values.length; i++) {
			var record = values[i];
			var shortName = record.eevo.shortName;
			var newDiv = Ext.DomHelper.append(shortName + '_vizwrap', {
				tag : 'div',
				id : shortName + "_vis",
				style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px;'
			});

			Heatmap.draw($(shortName + "_vis"), record.profiles);
		}
	}
});

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationWindow',
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'fit',
	constrainHeader : true,
	title : "Visualization",
	autoScroll : true,
	zoomWindow : null,

	initComponent : function() {
		// If there are any compile errors with the template the error will not make its way to the console.
		// Tried every combination i could think of to get the profile to display...
		// Can't seem to access the data even though its there...

		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'No images to display',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore(),

			tpl : new Gemma.ProfileTemplate(
					'<tpl for="."><tpl for="eevo">',
					'<div class="vizWrap" id ="{shortName}_vizwrap" style="float:left; padding: 10px"> {shortName} </div>',
					'</tpl></tpl>'),

			listeners : {
				selectionchange : {
					fn : function(dv, nodes) {
						
						var record = dv.getRecords(nodes)[0];
						var eevo = record.get("eevo");
						var profiles = record.get("profiles");

						
						//FIXME closing the window needs to be done better
						//Multiple clicks can get the new window into a funny state (fixable only by a reload)
							
						zoomWindow = new Ext.Window({

							id : 'visualization-zoom-window',
							closeAction : 'destroy',
							bodyStyle : "background:white",
							constrainHeader : true,
							layout : 'fit',
							title : "Visualization Zoom In",


							html : {
								id : 'graphzoompanel',
								tag : 'div',
								style : 'width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:' + Gemma.ZOOM_PLOT_SIZE + 'px;'						
							},

							displayWindow : function(eevo, profiles) {

								this.setTitle("Visualization of: " + eevo.shortName);

								//Remove color
								for(var i = 0; i< profiles.size(); i++){
									profiles[i].color = null;
									
								}
								var graphZoomConfig =  {
													lines : {lineWidth : 2
												},
												xaxis : {
													noTicks : 0
												},
												yaxis : {
													noTicks : 0
												},
												grid : {
													//color : "white"  //this turns the letters in the legend  to white
												},
												shadowSize : 1,
											
												legend : {
													show: true,
													position : 'nw'
													//backgroundOpacity : 0.5
												}};
												
								
								Flotr.draw($('graphzoompanel'), profiles, graphZoomConfig);

							}

						});

						zoomWindow.show(nodes[0], function() {zoomWindow.displayWindow(eevo, profiles);});

					}
				}
			},

			singleSelect : true,

			itemSelector : 'div.vizWrap',

			prepareData : function(data) {

				// Need to transform the cordinate data from an object to an array for flotr
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;

					var geneNames = genes[0].name;
					for(var k = 1; k < genes.size(); k++){
							geneNames = geneNames + "," + genes[k].name;
					}

					var oneProfile = [];

					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}
					var plotConfig = {
						data : oneProfile,
						color : color,
						genes : genes,
						label : probe + "=>" + geneNames
					};

					flotrData.push(plotConfig);
				}

				var data

				data.profiles = flotrData;
				return data;
			}
		});

		Ext.apply(this, {
			items : [this.dv]
		});

		Gemma.VisualizationWindow.superclass.initComponent.call(this);

	},

	displayWindow : function(eeIds, queryGene, coexpressedGene) {

		this.setTitle("Visualization of: " + queryGene.officialSymbol + " (red) with " + coexpressedGene.officialSymbol
				+ " (black)");

		var params = [];
		params.push(eeIds);
		params.push(queryGene.id);
		params.push(coexpressedGene.id);
		this.show();
		this.dv.store.load({
			params : params
		});

	}

});

// -----------------------------------------------------
// FIXME: this could be abstracted out better
// -----------------------------------------------------

Gemma.VisualizationDifferentialWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationWindow',
	width : 800,
	height : 500,
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'fit',
	constrainHeader : true,
	title : "Visualization",

	initComponent : function() {
		// If there are any compile errors with the template the error will not make its way to the console.
		// Tried every combination i could think of to get the profile to display...
		// Can't seem to access the data even though its there...

		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'No images to display',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore({
				readMethod : DEDVController.getDEDVForVisualization
			}),

			tpl : new Gemma.ProfileTemplate(
					'<tpl for="."><tpl for="eevo">',
					'<div class="vizWrap" id ="{shortName}_vizwrap"  style="float:left; padding: 10px"> {shortName} </div>',
					'</tpl></tpl>'),

			listeners : {
				selectionchange : {
					fn : function(dv, nodes) {

					}
				}
			},

			overClass : 'x-view-over',
			singleSelect : true,
			itemSelector : 'div.vizWrap',
			prepareData : function(data) {

				// Need to transform the cordinate data from an object to an array for flotr
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;

					var oneProfile = [];

					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}
					var plotConfig = {
						data : oneProfile,
						color : color
					};

					flotrData.push(plotConfig);
				}

				var data

				data.profiles = flotrData;
				return data;
			}
		});

		Ext.apply(this, {
			items : [this.dv]
		});

		Gemma.VisualizationWindow.superclass.initComponent.call(this);

	},

	displayWindow : function(eeIds, gene) {

		this.setTitle("Visualization of: " + gene.officialSymbol);
		var params = [];
		params.push(eeIds);
		params.push([gene.id]);
		this.show();
		this.dv.store.load({
			params : params
		});

	}

});
