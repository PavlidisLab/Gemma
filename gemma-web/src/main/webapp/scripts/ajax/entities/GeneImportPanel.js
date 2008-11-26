/*
 * $Id$
 */

/**
 * Prompt for a list of gene symbols.
 * 
 * @class Gemma.GeneImportPanel
 * @extends Ext.Window
 */
Gemma.GeneImportPanel = Ext.extend(Ext.Window, {

			title : "Import multiple genes (one symbol per line, up to " + Gemma.MAX_GENES_PER_QUERY + ")",
			modal : true,
			layout : 'fit',
			stateful : false,
			autoHeight : true,
			width : 300,
			height : 200,
			closeAction : 'hide',
			easing : 3,

			onCommit : function() {
				this.hide();
				this.fireEvent("commit", {
							geneNames : Ext.getCmp('gene-list-text').getValue()
						});
			},

			initComponent : function() {

				this.addEvents({
							"commit" : true
						});

				/*
				 * Gemma.MAX_GENE_PER_QUERY is defined in GeneChooserPanel.js
				 */

				Ext.apply(this, {
							items : [{
										id : 'gene-list-text',
										xtype : 'textarea',

										fieldLabel : "Paste in gene symbols, one per line, up to "
												+ Gemma.MAX_GENES_PER_QUERY,
										width : 290
									}],
							buttons : [{
										text : 'Cancel',
										handler : this.hide.createDelegate(this, [], true)
									}, {
										text : 'OK',
										handler : this.onCommit,
										scope : this
									}, {
										text : 'Clear',
										handler : function() {
											Ext.getCmp('gene-list-text').setValue("");
										}
									}]
						});

				Gemma.GeneImportPanel.superclass.initComponent.call(this);
			}

		});
