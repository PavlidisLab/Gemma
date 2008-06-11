/*
 * Gemma.DiffExpressionGridRowExpander constructor...
 */
Gemma.DiffExpressionGridRowExpander = function(config) {

	this.expandedElements = [];

	this.grid = config.grid;

	var superConfig = {};

	for (property in config) {
		superConfig[property] = config[property];
	}
	Gemma.DiffExpressionGridRowExpander.superclass.constructor.call(this,
			superConfig);

};

/*
 * instance methods...
 */
Ext.extend(Gemma.DiffExpressionGridRowExpander, Ext.grid.RowExpander, {

	beforeExpand : function(record, body, rowIndex) {
		if (this.fireEvent('beforeexpand', this, record, body, rowIndex) !== false) {

			// don't do it twice.
			if (this.expandedElements[rowIndex]) {
				return true;
			}

			this.expandedElements[rowIndex] = [];

			var bodyEl = new Ext.Element(body);

			bodyEl.addClass('diffExpressionGridRowExpanded'); // layout.css

			var diffExGridEl = bodyEl.createChild({});
			diffExGridEl.addClass("x-hide-display");

			var tabPanel = new Ext.TabPanel({
				renderTo : bodyEl,
				activeTab : 0,
				items : [{
					title : "Supporting Datasets",
					contentEl : diffExGridEl
				}]
			});

			this.expandedElements[rowIndex].push(tabPanel);

			var supporting = this.getSupportingDatasetRecords(record);

			var diffExGrid = new Gemma.DiffExpressionExperimentGrid({
				records : supporting,
				pageSize : 10,
				width : 800,
				renderTo : diffExGridEl
			});

			diffExGrid.getStore().load({
				params : {
					start : 0,
					limit : 10
				}
			});

			/*
			 * var loadMask = new Ext.LoadMask( diffExGridEl, { removeMask :
			 * true, store : diffExGrid.getStore() } ); loadMask.show();
			 */

			// Keep mouse events from propogating to the parent grid. See ExtJS
			// forums topic "nested grids problem" (242878).
			diffExGrid.getEl().swallowEvent(['mouseover', 'mousedown', 'click',
					'dblclick']);

			return true;
		}
		return false;
	},

	getSupportingDatasetRecords : function(record) {
		return record.data.probeResults;
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
