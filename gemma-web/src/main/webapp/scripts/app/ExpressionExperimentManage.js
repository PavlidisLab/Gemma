Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			var record = Ext.data.Record.create([{
						name : "id",
						type : "int"
					}, {
						name : "shortName",
						type : "string"
					}, {
						name : "name",
						type : "string"
					}, {
						name : "arrayDesignCount",
						type : "int"
					}, {
						name : "bioAssayCount",
						type : "int"
					}, {
						name : "externalUri",
						type : "string"
					}, {
						name : "description",
						type : "string"
					}, {
						name : "differentialExpressionAnalysisId",
						type : "string"
					}]);

			var store = new Ext.data.Store({
						proxy : new Ext.data.DWRProxy(ExpressionExperimentController.loadStatusSummaries),
						reader : new Ext.data.ListRangeReader({
									id : "id"
								}, record),
						remoteSort : false
					});
			store.load({
						params : [null]
					});

			var tpl = new Ext.XTemplate('<tpl for="."><div class="itemwrap" id="{shortName}">',
					'<p>{id} {name} {shortName} {externalUri} {[this.log(values.id)]}</p>', "</div></tpl>", {
						log : function(id) {
							console.log(id);
						}
					});

			var manager = new Ext.Panel({
						renderTo : 'eemanage',
						width : 200,
						autoHeight : true,
						layout : 'fit',
						items : [new Ext.DataView({
									store : store,
									tpl : tpl,
									itemSelector : 'itemwrap',
									autoHeight : true,
									emptyText : "No experiments to display"
								})]

					});

		});
