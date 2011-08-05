Ext.namespace('Gemma');

Gemma.GeneGOGrid = Ext.extend(Gemma.GemmaGridPanel, {
	
	viewConfig : {
		forceFit : true
	},
			record : Ext.data.Record.create([{
						name : "id",
						type : "int"
					}, {
						name : "termUri"
					}, {
						name : "termName"
					}, {
						name : "evidenceCode"
					}]),

			golink : function(d) {
				var g = d.replace("_", ":");
				return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
						+ g + "'>" + g + "</a>";
			},

			initComponent : function() {
				Ext.apply(this, {
							columns : [{
										header : "ID",
										dataIndex : "termUri",
										renderer : this.golink
									}, {
										header : "Term",
										dataIndex : "termName"
									}, {
										header : "Evidence Code",
										dataIndex : "evidenceCode"
									}],

							store : new Ext.data.Store({
										proxy : new Ext.data.DWRProxy(GeneController.findGOTerms),
										reader : new Ext.data.ListRangeReader({
													id : "id"
												}, this.record),
										remoteSort : false
									})
						});

				Gemma.GeneGOGrid.superclass.initComponent.call(this);

				this.getStore().setDefaultSort('termUri');

				this.getStore().load({
							params : [this.geneid]
						});
			}

		});
Ext.reg('genegogrid',Gemma.GeneGOGrid);