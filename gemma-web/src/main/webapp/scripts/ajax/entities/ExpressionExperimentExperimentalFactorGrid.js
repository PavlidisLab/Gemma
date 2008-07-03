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
			editable : true,
			autoHeight : true,
			height : 400,
			style : 'margin-bottom: 1em;',

			initComponent : function() {

				var source = [];
				var customEditors = [];
				for (i in this.data) {
					var d = this.data[i];
					if (d.expressionExperiment) {

						var myData = [];

						for (j in d.experimentalFactors) {
							var f = d.experimentalFactors[j];
							if (f.id) {
								var row = [f.id, f.name];
								myData.push(row);
							}
						}

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

				Ext.apply(this, {
					source : source,
					customEditors : customEditors
				});

				Gemma.ExpressionExperimentExperimentalFactorGrid.superclass.initComponent
						.call(this);

				this.originalTitle = this.title;
			}

		});