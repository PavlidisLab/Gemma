Ext.namespace('Gemma');
/**
 * Gene go grid
 * @class Gemma.GeneGOGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.GeneGOGrid = Ext.extend(Gemma.GemmaGridPanel, {
	deferLoadToRender:false,
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
										dataIndex : "evidenceCode",
										renderer: function( value, metaData, record, rowIndex, colIndex, store ){
											return Gemma.EvidenceCodes[record.get('evidenceCode')];
										}
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
								
				if (!this.deferLoadToRender) {
					this.getStore().load({
						params: [this.geneid]
					})
				} else {
					this.on('render',function(){
						this.getStore().load({
							params: [this.geneid]
						})
					});
				}
			}

		});
Ext.reg('genegogrid',Gemma.GeneGOGrid);