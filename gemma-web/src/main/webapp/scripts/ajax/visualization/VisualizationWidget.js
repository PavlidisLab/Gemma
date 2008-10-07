Ext.namespace('Gemma');

/**
 * @extends Ext.data.Store
 */

Gemma.VisualizationStore = function(config) {

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
		var newDivName = "vis" + ee.shortName;
		var f = Flotr.draw(newDivName, flotrData);
	}

});

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationWindow',
	width : 800,
	height : 500,
	closeAction : 'hide',
	layout : 'fit',
	constrainHeader : true,
	title : "Visualization",

	initComponent : function() {

		this.store = new Gemma.VisualizationStore();

		// If there are any compile errors with the template the error will not make its way to the console.
		// Tried every combination i could think of to get the profile to display...
		// Can't seem to access the data even though its there...

		var tpl = new Ext.XTemplate('<tpl for="."><tpl for="ee">', '<div id ="{shortName}"> {shortName} </div>',
				'</tpl>', '{[ this.drawGraph(values.ee, values.profiles)]}', '</tpl>', {
					drawGraph : function(ee, profile) {
						console.log("hehe");
						flotrDraw(ee.shortName, profile);
//						Ext.DomHelper.overwrite(shortName, {
//							tag : 'h1',
//							html : "this is a test"
//						});
						return ee.shortName + ": " + profile;
					}
				});

		Ext.apply(this, {
			items : [new Ext.DataView({
				store : this.store,
				tpl : tpl,
				autoHeight : true,
				emptyText : 'No images to display',

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

					console.log(data);
					return data;
				}

			})
			// Items
			]
		});

		Gemma.VisualizationWindow.superclass.initComponent.call(this);

		// this.on("click", function(o) {
		// console.log("render:" + o);
		// o.store.each(function() {
		// console.log("in each")
		// var shortName = this.get("ee").shortName;
		// console.log(shortName);
		// Ext.DomHelper.overwrite(shortName, {
		// tag : 'h1',
		// html : "this is a test"
		// });

		// });

		// });

	},

	displayWindow : function(eeIds, geneIds) {

		var params = [];
		params.push(eeIds);
		params.push(geneIds);
		this.show();
		this.store.load({
			params : params,
			callback : function() {

			}.createDelegate(this)
		});

	},

	clearWindow : function() {
		//this.hide();
		// TODO: Clear the window of old graphs
	}

});

flotrDraw = function(divId, profiles) {
	console.log(divId);
	console.log(profiles);
	Flotr.draw(divId, profiles);
}
