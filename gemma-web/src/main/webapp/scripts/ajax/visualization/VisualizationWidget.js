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
Gemma.COLD_COLORS = ["#0033FF", "#6699FF", "#3399CC", "#336666", "#66CCCC", "#33FF99", "#339966", "#009900", "#66CC66",
		"#00CC00"];
Gemma.HOT_COLORS = ["#FFCC00", "#FF9900", "#FF6600", "#FF3300", "#CC3300", "#660000", "#FF0000", "#990033", "#FF3399",
		"#990099"];

// Tests if gene is in the array of genes. uses the name of the gene to resolove
// identity.
Gemma.geneContained = function(geneName, arrayOfGenes) {
	for (var i = 0; i < arrayOfGenes.size(); i++) {
		if (arrayOfGenes[i].name === geneName)
			return true;
	}
	return false;

};

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
	layout : 'border',
	constrainHeader : true,
	title : "Visualization",
	height : Gemma.ZOOM_PLOT_SIZE + 50,
	width : Gemma.ZOOM_PLOT_SIZE + Gemma.PLOT_SIZE + 100,

	initComponent : function() {
		// If there are any compile errors with the template the error will not
		// make its way to the console.
		// Tried every combination i could think of to get the profile to
		// display...
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

						this.zoomPanel.displayWindow(eevo, profiles);

					}.createDelegate(this)
				}
			},

			singleSelect : true,

			itemSelector : 'div.vizWrap',

			prepareData : function(data) {

				// Need to transform the cordinate data from an object to an
				// array for flotr
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;

					var geneNames = genes[0].name;
					for (var k = 1; k < genes.size(); k++) {
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

				data.profiles = flotrData;
				return data;
			}
		});

		this.thumbnailPanel = new Ext.Panel({
			title : 'query gene (red) with coexpressed gene (black)',
			region : 'west',
			split : true,
			width : Gemma.PLOT_SIZE + 50,
			collapsible : true,
			margins : '3 0 3 3',
			cmargins : '3 3 3 3',
			items : this.dv,
			autoScroll : true,
			html : {
				id : 'zoomLegend',
				tag : 'div',
				style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px; float:left;'
			}

		});

		this.zoomPanel = new Ext.Panel({

			region : 'center',
			split : true,
			width : Gemma.ZOOM_PLOT_SIZE,
			hight : Gemma.ZOOM_PLOT_SIZE,
			id : 'visualization-zoom-window',
			closeAction : 'destroy',
			bodyStyle : "background:white",
			constrainHeader : true,
			layout : 'fit',
			title : "Click thumbnail to zoom in",

			html : {
				id : 'graphzoompanel',
				tag : 'div',
				style : 'width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:' + Gemma.ZOOM_PLOT_SIZE + 'px;'
			},

			displayWindow : function(eevo, profiles) {

				this.setTitle("Visualization for genes in dataset:  " + eevo.shortName);

				// Change color to hot and cold variety
				var coldGeneName = profiles[0].genes[0].name;
				var coldIndex = 0, hotIndex = 0;

				var sortedHotProfile = [];
				
				// Add cold colors
				for (var i = 0; i < profiles.size(); i++) {
					if (Gemma.geneContained(coldGeneName, profiles[i].genes)) {
						profiles[i].color = Gemma.COLD_COLORS[coldIndex];
						coldIndex++;
					} else {
						profiles[i].color = Gemma.HOT_COLORS[hotIndex];
						hotIndex++;
					}
//					profiles[i].lines = {
//						//linewidth : 1 / profiles[i].genes.size()
//					}; // Vary line width size on probe specificity?

				}
			
				//sort array by gene
				profiles.sort(function(a,b){
					if ( (a.genes[0].name === b.genes[0].name))
						return 1;
					else return 0;
				});
				
				 var GRAPH_ZOOM_CONFIG = {
					xaxis : {
						noTicks : 0
					},
					yaxis : {
						noTicks : 0
					},
					grid : {
						labelMargin : 0
					// => margin in pixels
					// color : "white" //this turns the letters in the legend to white
					},
					shadowSize : 0,

					legend : {
						show : true,
						container : 'zoomLegend'
					// position : 'nw'
					// backgroundOpacity : 0.5
					}

				};

				//console.log()
				Flotr.draw($('graphzoompanel'), profiles, GRAPH_ZOOM_CONFIG);

			}

		});

		Ext.apply(this, {
			items : [this.thumbnailPanel, this.zoomPanel]
		});

		Gemma.VisualizationWindow.superclass.initComponent.call(this);

	},

	displayWindow : function(eeIds, queryGene, coexpressedGene) {

		this.setTitle("Visualization of query gene: " + queryGene.officialSymbol + " with coexpressed gene <b> "
				+ coexpressedGene.officialSymbol + "</b>");

		this.thumbnailPanel.setTitle(queryGene.officialSymbol + " (red) with " + coexpressedGene.officialSymbol
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
		// If there are any compile errors with the template the error will not
		// make its way to the console.
		// Tried every combination i could think of to get the profile to
		// display...
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

				// Need to transform the cordinate data from an object to an
				// array for flotr
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
