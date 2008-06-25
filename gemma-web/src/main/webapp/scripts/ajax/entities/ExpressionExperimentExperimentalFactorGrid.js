Ext.namespace('Gemma');

/**
 * A grid holding the experiments and their associated factors.
 * 
 * @author keshav
 * @version $Id: ExpressionExperimentExperimentalFactorGrid.js,v 1.1 2008/06/23
 *          23:51:30 keshav Exp $
 * @class Gemma.ExperimentalFactorGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.ExpressionExperimentExperimentalFactorGrid = Ext.extend(
		Ext.grid.PropertyGrid, {

			title : "Experimental Factors Per Experiment",
			loadMask : {
				msg : 'Loading factors ...'
			},
			pageSize : 25,
			collapsible : true,
			editable : false,
			autoHeight : true,
			width : 600,
			style : 'margin-bottom: 1em;',

			initComponent : function() {
//				if (this.pageSize) {
//					Ext.apply(this, {
//						store : new Gemma.PagingDataStore({
//							proxy : new Ext.data.MemoryProxy([]),
//							reader : new Ext.data.ListRangeReader({
//								id : "id"
//							}, this.record),
//							pageSize : this.pageSize
//						})
//					});
//					Ext.apply(this, {
//						bbar : new Gemma.PagingToolbar({
//							pageSize : this.pageSize,
//							store : this.store
//						})
//					});
//				} else {
//					Ext.apply(this, {
//						store : new Ext.data.Store({
//							proxy : new Ext.data.MemoryProxy(this.record),
//							reader : new Ext.data.ListRangeReader({},
//									this.record)
//						})
//					});
//				}

				var source = [];
				var customEditors = [];
				var d;
				for (i in this.data) {
					var d = this.data[i];
					if (d.expressionExperiment) {
						customEditors[d.expressionExperiment.name] = new Ext.grid.GridEditor(new Ext.form.ComboBox({
							store : new Ext.data.SimpleStore(d.experimentalFactors),
							typeAhead : true,
							displayField : 'name',
							selectOnFocus : true,
							triggerAction : 'all',
							mode : 'local'
						}));
						source[d.expressionExperiment.name] = d.experimentalFactors[0].name;
					};
				};

				Ext.apply(this, {
					source : source,
					customEditors : customEditors
				});

				Gemma.ExpressionExperimentExperimentalFactorGrid.superclass.initComponent
						.call(this);

				this.originalTitle = this.title;
			}

		});