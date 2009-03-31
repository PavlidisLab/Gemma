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

Gemma.SELECTED = 2; // Multiply the line thickness by this factor when it is selected in the legend
Gemma.LINE_THICKNESS = 1;
Gemma.ZOOM_LINE_THICKNESS = 2;
Gemma.PLOT_SIZE = 100;

Gemma.HOT_FADE_COLOR = "#FFDDDD";
Gemma.COLD_FADE_COLOR = "#DDDDDD";

Gemma.GRAPH_ZOOM_CONFIG = {
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
	},
	label : true

};
// Tests if gene is in the array of genes. uses the name of the gene to resolove
// identity.
Gemma.geneContained = function(geneName, arrayOfGenes) {
	for (var i = 0; i < arrayOfGenes.size(); i++) {
		if (arrayOfGenes[i].name === geneName)
			return true;
	}
	return false;

};

Gemma.graphSort = function(a, b) {

	// sorts data by importance 1st
	if (a.factor > b.factor) {
		return 1;
	} else if (a.factor < b.factor) {
		return -1;
	}

	// if equal importance than sort by gene name
	else {
		if (a.genes[0].name > b.genes[0].name) {
			return 1;
		} else if (a.genes[0].name < b.genes[0].name) {
			return -1;
		} else {
			return (a.labelID > b.labelID);
		}

	}
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

		
Gemma.HEATMAP_VIEW = false;

Gemma.getTemplate = function(){
		var template;
		if (Gemma.HEATMAP_VIEW){
			template =  new Gemma.HeatmapTemplate('<tpl for="."><tpl for="eevo">',
				'<div class="vizWrap" id ="{shortName}_vizwrap" style="float:left; padding: 10px"> <b> {shortName}  </b> <small> {[sprintf("%.35s",values.name)]} </small> </div>',
				'</tpl></tpl>')			
		}
		else{
			template = new Gemma.ProfileTemplate(
				'<tpl for="."><tpl for="eevo">',
				'<div class="vizWrap" id ="{shortName}_vizwrap" style="float:left; padding: 10px"> <b> {shortName}  </b> <small> {[sprintf("%.35s",values.name)]} </small> </div>',
				'</tpl></tpl>')			
		}
		
		return template;
	
}
		
Gemma.VisualizationWindow = function(config) {

	Ext.Window.constructor.call(this, config);

};

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
	
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'border',
	constrainHeader : true,
	stateful : false,
	title : "Visualization",
	height : Gemma.ZOOM_PLOT_SIZE, 
	width : 600,
	
	listeners : {
		show : {
			fn : function(window) {
				this.onLegendClick = function(event, component) {
					var probeId = event.getTarget().id;
					var record = window.dv.getSelectedRecords()[0];
					var profiles = record.get("profiles");

					// FIXME make legend bold so there is a user clue as to the line being bolded.

					for (var i = 0; i < profiles.size(); i++) {

						if (profiles[i].labelID == probeId) {
							if (profiles[i].selected) {
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth / Gemma.SELECTED;
								profiles[i].selected = false;
							} else {
								profiles[i].selected = true;
								profiles[i].lines.lineWidth = profiles[i].lines.lineWidth * Gemma.SELECTED;
							}
							break;
						}
					}

					window.zoomPanel.refreshWindow(profiles);

				};

				var zoomLegendDiv = Ext.get("zoomLegend");
				zoomLegendDiv.on('click', this.onLegendClick.createDelegate(this));

			}.createDelegate(this)
		}
	}
	
});