
/**
 * @extends Ext.data.Store
 */
Gemma.VisaulizationStore = function(config) {

	this.record = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "ee"
	}, {
		name : "profiles"
	}]);

	// DEDVController.getDEDVForVisualization(experimentIds, geneIds, loadVisData);
	this.readMethod = DEDVController.getDEDVForVisualization;

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
Ext.extend(Gemma.VisualizationStore, Ext.data.Store, {

	loadVisData : function(data) {


			// Create a DIV for data.
			// var dh = Ext.DomHelper;
			var newDivName = "vis" + ee.shortName;
			// var newDiv = dh.append('coexpression-visualization', {
			// tag : 'div',
			// id : newDivName,
			// style : 'width:300px;height:300px;'
			// });
			var f = Flotr.draw(newDivName, flotrData);

		}

	}

);

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationPanel',
	layout : 'border',
	width : 800,
	height : 500,
	closeAction : 'hide',
	constrainHeader : true,
	title : "Visualization Panel",
	isAdmin : false,

	initComponent : function() {

		var store = new Gemma.VisaulizationStore();

		var tpl = new Ext.XTemplate({divName : function(eeName){return "vis" + eeName}},'<div id=this.divName(ee.shortname)>',
		'{[Flotr.draw(this.divName(ee.shortname), flotrData)]}');

		var panel = new Ext.Panel({

			id : 'images-view',

			frame : true,
			width : 535,
			autoHeight : true,
			collapsible : true,
			layout : 'fit',
			title : 'Visualization',

			items : new Ext.DataView({
				store : store,
				tpl : tpl,
				autoHeight : true,
				multiSelect : true,
				overClass : 'x-view-over',
				itemSelector : 'div.thumb-wrap',
				emptyText : 'No images to display',
				plugins : [new Ext.DataView.DragSelector(), new Ext.DataView.LabelEditor({
					dataIndex : 'name'
				})],

				prepareData : function(data) {

					// Need to transform the cordinate data from an object to an array for flotr
					var flotrData = [];
					var coordinateProfile = data.profiles;

					for (var i = 0; i < coordinateProfile.size(); i++) {
						var coordinateObject = coordinateProfile[i].points;
						var coordinateSimple = [];

						for (var j = 0; j < coordinateObject.size(); j++) {
							coordinateSimple.push([coordinateObject[j].x, coordinateObject[j].y]);
						}
						flotrData.push(coordinateSimple);
					}

					data.profiles = flotrData;
					return data;
				}

			})
		});

	},

	displayWindow : function(eeIds, geneIds) {

		var params = {};
		params.push(eeIds);
		params.push(geneIds);
		this.store.load(params);

	},

	clearWindow : function() {

		// Clear the window of old graphs
	}

});