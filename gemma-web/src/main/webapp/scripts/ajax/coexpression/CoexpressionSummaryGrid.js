/**
 * Shows the summary of the coexpression search results.
 * 
 * @class Ext.Gemma.CoexpressionSummaryGrid
 * @extends Ext.Gemma.GemmaGridPanel
 */
Ext.Gemma.CoexpressionSummaryGrid = Ext.extend(Ext.Gemma.GemmaGridPanel, {

	editable : false,
	title : 'Search Summary',
	width : 350,
	height : 250,

	autoExpandColumn : 'key',

	initComponent : function() {

		var columns = [{
			header : 'Group',
			dataIndex : 'group'
		}, {
			id : 'key',
			header : '',
			dataIndex : 'key',
			align : 'right'
		}];

		for (var i = 0; i < this.genes.length; ++i) {
			columns.push({
				header : this.genes[i].officialSymbol,
				dataIndex : this.genes[i].officialSymbol,
				align : 'right'
			});
		}

		var fields = [{
			name : 'sort',
			type : 'int'
		}, {
			name : 'group',
			type : 'string'
		}, {
			name : 'key',
			type : 'string'
		}];
		for (var i = 0; i < this.genes.length; ++i) {
			fields.push({
				name : this.genes[i].officialSymbol,
				type : 'int'
			});
		}

		Ext.apply(this, {
			columns : columns,
			store : new Ext.data.GroupingStore({
				reader : new Ext.data.ArrayReader({}, fields),
				groupField : 'group',
				data : this.transformData(this.genes, this.summary),
				sortInfo : {
					field : 'sort',
					direction : 'ASC'
				}
			}),
			view : new Ext.grid.GroupingView({
				enableGroupingMenu : false,
				enableNoGroups : false,
				hideGroupedColumn : true,
				showGroupName : false
			})
		});

		Ext.Gemma.CoexpressionSummaryGrid.superclass.initComponent.call(this);

	},

	transformData : function(genes, summary) {

		var datasetsAvailable = [0, "Datasets",
				"<span ext:qtip='How many data sets met your criteria'>Available</span>"];
		var datasetsTested = [
				1,
				"Datasets",
				"<span ext:qtip='How many data sets had the query gene available for analysis'>Query gene testable</span>"];
		var linksFound = [
				2,
				"Links",
				"<span ext:qtip='Total number of links (may show the number including those not meeting your stringency threshold)'>Found</span>"];
		var linksPositive = [
				3,
				"Links",
				"<span ext:qtip='How many genes were considered positively correlated with the query'>Met stringency (+)</span>"];
		var linksNegative = [
				4,
				"Links",
				"<span ext:qtip='How many genes were considered negatively correlated with the query'>Met stringency (-)</span>"];

		for (var i = 0; i < genes.length; ++i) {
			var thisSummary = summary[genes[i].officialSymbol] || {};
			datasetsAvailable.push(thisSummary.datasetsAvailable);
			datasetsTested.push(thisSummary.datasetsTested);
			linksFound.push(thisSummary.linksFound);
			linksPositive.push(thisSummary.linksMetPositiveStringency);
			linksNegative.push(thisSummary.linksMetNegativeStringency);
		}

		return [datasetsAvailable, datasetsTested, linksFound, linksPositive,
				linksNegative];
	}

});
