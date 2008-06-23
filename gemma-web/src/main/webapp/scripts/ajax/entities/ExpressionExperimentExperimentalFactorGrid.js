Ext.namespace('Gemma');

/**
 * A grid holding the experiments and their associated factors.
 * 
 * @author keshav
 * @version $Id$
 * @class Gemma.ExperimentalFactorGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.ExpressionExperimentExperimentalFactorGrid = Ext.extend(Gemma.GemmaGridPanel, {

	collapsible : true,
	editable : false,
	autoHeight : true,
	width : 600,
	style : 'margin-bottom: 1em;',

	/* set what the record should look like */
	record : Ext.data.Record.create([{
		name : "expressionExperiment",
		convert : function(e) {
			return e.shortName;
		}
	}, {
		name : "experimentalFactors"
	}]),

	initComponent : function() {
		if (this.pageSize) {
			Ext.apply(this, {
				store : new Gemma.PagingDataStore({
					proxy : new Ext.data.MemoryProxy([]),
					reader : new Ext.data.ListRangeReader({
						id : "id"
					}, this.record),
					pageSize : this.pageSize
				})
			});
			Ext.apply(this, {
				bbar : new Gemma.PagingToolbar({
					pageSize : this.pageSize,
					store : this.store
				})
			});
		} else {
			Ext.apply(this, {
				store : new Ext.data.Store({
					proxy : new Ext.data.MemoryProxy(this.records),
					reader : new Ext.data.ListRangeReader({}, this.record)
				})
			});
		}

		Ext.apply(this, {
			columns : [{
				id : 'expressionExperiment',
				dataIndex : "expressionExperiment",
				header : "Dataset",
				sortable : false
			}, {
				id : 'experimentalFactors',
				dataIndex : "experimentalFactors",
				header : "Experimental Factors",
				toolTip : "Factors for the dataset."

					// ,editor : new Ext.form.ComboBox({ typeAhead : true,
					// triggerAction : 'all', transform : 'light', lazyRender :
					// true, listClass : 'x-combo-list-small' }), sortable :
					// false, width : 75

					}]
		});

		Gemma.ExperimentalFactorGrid.superclass.initComponent.call(this);

		this.originalTitle = this.title;
	},

	/**
	 * Load the data.
	 * 
	 * @param {}
	 *            results
	 */
	loadData : function(results) {
		this.getStore().proxy.data = results;
		this.getStore().reload({
			resetPage : true
		});
		this.getView().refresh(true); // refresh column headers
	}

});