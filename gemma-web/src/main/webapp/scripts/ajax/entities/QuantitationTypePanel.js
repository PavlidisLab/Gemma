Ext.namespace("Gemma");

/**
 * Input description of a quantitation type - for dataset upload.
 * 
 * @author paul
 * @version $Id$
 */
Gemma.QuantitationTypePanel = Ext.extend(Ext.form.FieldSet, {

			autoHeight : true,
			title : "Quantitation type details",

			initComponent : function() {

				Ext.apply(this, {

							items : [
									// {
									// xtype : 'textfield',
									// id : 'qtype-name',
									// allowBlank : false,
									// value : 'Value',
									// width : 200,
									// name : "Name",
									// fieldLabel : "Name",
									// emptyText : "Examples: RMA, MAS5, log2 ratio"
									// }, {
									// xtype : 'textfield',
									// id : 'qtype-description',
									// width : 300,
									// name : "Description",
									// fieldLabel : "Description",
									// emptyText : 'Enter a brief description of the measurement'
									// },
									{
								xtype : 'checkbox',
								id : 'qtype-isratio',
								name : 'Ratio',
								fieldLabel : "Ratios?",
								boxLabel : "Check the box if the expression values are ratios",
								tooltip : 'Check if your input values are ratios'
							}, {
								xtype : 'checkbox',
								id : 'qtype-islogged',
								fieldLabel : 'Log transformed?',
								boxLabel : "Check the box if the expression values are on a log scale (log2 assumed)",
								tooltip : 'Check if your data are on a log scale'
							}

							]

						});

				Gemma.QuantitationTypePanel.superclass.initComponent.call(this);
			}

		});