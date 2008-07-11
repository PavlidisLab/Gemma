/**
 * 
 */
Gemma.CoexpressionGridRowExpander = Ext.extend(Ext.grid.RowExpander, {

	expandedElements : [],

	beforeExpand : function(record, body, rowIndex) {
		if (this.fireEvent('beforeexpand', this, record, body, rowIndex) !== false) {

			var gene = record.data.foundGene;

			var bodyEl = new Ext.Element(body);

			Ext.DomHelper.overwrite(bodyEl, "");

			var supportingDsGridEl = bodyEl.createChild({});
			supportingDsGridEl.addClass("x-hide-display");

			var diffExGridEl = bodyEl.createChild({});
			diffExGridEl.addClass("x-hide-display");

			var supporting = this.getSupportingDatasetRecords(record);

			var dsGrid = new Gemma.ExpressionExperimentGrid({
				records : supporting,
				width : 750,
				renderTo : supportingDsGridEl
			});

			dsGrid.getStore().load();

			var diffExGrid = new Gemma.DiffExpressionExperimentGrid({
				geneId : gene.id,
				threshold : 0.01,
				width : 750,
				renderTo : diffExGridEl
			});

			var tabPanel = new Ext.TabPanel({
				renderTo : bodyEl,
				layoutOnTabChange : true,
				width : 750,
				activeTab : 0,
				// layout : 'fit',
				items : [{
					title : "Supporting datasets",
					contentEl : supportingDsGridEl
				}, {
					title : "Differential expression of " + gene.officialSymbol,
					contentEl : diffExGridEl,
					loaded : false,
					listeners : {
						"activate" : {
							fn : function() {
								if (!this.loaded) {
									diffExGrid.getStore().load({
										params : [gene.id, 0.01]
									});
								}
								this.loaded = true;
							}
						}
					}

				}]

			});

			// Keep mouse events from propagating to the parent grid. See ExtJS
			// forums topic "nested grids problem" (242878).
			dsGrid.getEl().swallowEvent(['mouseover', 'mousedown', 'click', 'dblclick']);
			diffExGrid.getEl().swallowEvent(['mouseover', 'mousedown', 'click', 'dblclick']);

			return true;
		}

		return false;
	},

	getSupportingDatasetRecords : function(record) {
		var ids = record.data.supportingExperiments;
		var supporting = [];
		var ind = 0;
		// this is quite inefficient, but probably doesn't matter.
		for (var i = 0; i < ids.length; ++i) {
			var id = ids[i];
			for (var j = 0; j < this.grid.datasets.length; j++) {
				var index = this.grid.datasets[j].id;
				if (index === id) {
					supporting.push(this.grid.datasets[j]);
					break;
				}
			}
		}
		return supporting;
	},

	clearCache : function() {
		for (var i = 0; i < this.expandedElements.length; ++i) {
			if (this.expandedElements[i]) {
				for (var j = 0; j < this.expandedElements[i].length; ++j) {
					// grid.destroy() seems to be broken...
					try {
						this.expandedElements[i][j].destroy();
					} catch (e) {
					}
				}
				this.expandedElements[i] = null;
			}
		}
	}
});

/*
 * instance methods...
 */
