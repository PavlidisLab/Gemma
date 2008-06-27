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

				var source = [];
				var customEditors = [];
				for (i in this.data) {
					var d = this.data[i];
					if (d.expressionExperiment) {

						var myData = [
								[d.experimentalFactors[0].id,
										d.experimentalFactors[0].name],
								[d.experimentalFactors[1].id,
										d.experimentalFactors[1].name]];

						var s = new Ext.data.SimpleStore({
							fields : [{
								name : 'id',
								type : 'int'
							}, {
								name : 'name',
								type : 'string'
							}]
						});
						s.loadData(myData);

						customEditors[d.expressionExperiment.name] = new Ext.grid.GridEditor(new Ext.form.ComboBox({
							store : s,
							typeAhead : true,
							displayField : 'name',
							selectOnFocus : true,
							triggerAction : 'all',
							mode : 'local'
						}));
						source[d.expressionExperiment.name] = d.experimentalFactors[0].name;
					};
				};

				var cm = Ext.grid.ColumnModel([{
					header : "Dataset",
					width : 60,
					sortable : true
				}, {
					header : "Factors",
					width : 60,
					sortable : false
				}]);

				Ext.apply(this, {
					source : source,
					customEditors : customEditors,
					colModel : cm
				});

				Gemma.ExpressionExperimentExperimentalFactorGrid.superclass.initComponent
						.call(this);

				this.originalTitle = this.title;
			}

		});