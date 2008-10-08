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

Gemma.ProfileTemplate = Ext.extend(Ext.XTemplate, {

			overwrite : function(el, values, ret) {
				Gemma.ProfileTemplate.superclass.overwrite.call(this, el, values, ret);
				console.log(values);
				for (var i = 0; i < values.length; i++) {
					var record = values[i];
					var shortName = record.ee.shortName;
					console.log(shortName);

					var newDiv = Ext.DomHelper.append(shortName, {
								tag : 'div',
								id : shortName + "_vis",
								style : 'width:300px;height:300px;'
							});

					flotrDraw(newDiv, record.profiles);
				};
			}
		});

Gemma.VisualizationWindow = Ext.extend(Ext.Window, {
			id : 'VisualizationWindow',
			width : 800,
			height : 500,
			closeAction : 'destroy',
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
							store : new Gemma.VisualizationStore(),

							tpl : new Gemma.ProfileTemplate('<tpl for="."><tpl for="ee">',
									'<div id ="{shortName}" style="height:300px;width:300px;"> {shortName} </div>',
									'</tpl></tpl>'),

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
						});

				Ext.apply(this, {
							items : [this.dv]
						});

				Gemma.VisualizationWindow.superclass.initComponent.call(this);

			},

			displayWindow : function(eeIds, geneIds) {

				var params = [];
				params.push(eeIds);
				params.push(geneIds);
				this.show();
				this.dv.store.load({
							params : params
						});

			},

			clearWindow : function() {
				// this.hide();
				// TODO: Clear the window of old graphs
			}

		});

flotrDraw = function(divId, profiles) {
	console.log(divId);
	console.log(profiles);
	Flotr.draw(divId, profiles);
}
