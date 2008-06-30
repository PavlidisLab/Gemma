Ext.namespace('Gemma');

/**
 * User interface for selecting experimental factors.
 * 
 * @class Gemma.ExperimentalFactorChooserPanel
 * @extends Ext.Window
 * 
 * @author keshav
 * @version $Id: ExperimentalFactorChooserPanel.js,v 1.2 2008/06/17 21:36:04
 *          keshav Exp $
 */
Gemma.ExperimentalFactorChooserPanel = Ext.extend(Ext.Window, {
	id : 'factor-chooser',
	layout : 'fit',
	width : 800,
	height : 500,
	closeAction : 'hide',
	constrainHeader : true,

	onCommit : function() {

		if (this.efGrid) {
			var eeFactorsSelModel = this.efGrid.selModel;

			var experiments = [];
			var factors = [];

			var i = 0;
			while (eeFactorsSelModel.hasSelection()) {

				eeFactorsSelModel.select(i, 0);
				var experiment = eeFactorsSelModel.selection.record.data.name;
				experiments[i] = experiment;

				eeFactorsSelModel.select(i, 1);
				var selectedFactor = eeFactorsSelModel.selection.record.data.value;
				factors[i] = selectedFactor;

				i++;
			}

		}

		var eeFactorsMap = {
			experiments : experiments,
			factors : factors
		};

		this.fireEvent("factors-chosen", eeFactorsMap);
		this.hide();
	},

	/*
	 * initialize this panel by adding 'things' to it, like the data-store,
	 * columns, buttons (and events for buttons), etc.
	 */
	initComponent : function() {

		Ext.apply(this, {
			buttons : [{
				id : 'done-selecting-button',
				text : "Done",
				handler : this.onCommit.createDelegate(this),
				scope : this
			}]
		});

		Gemma.ExperimentalFactorChooserPanel.superclass.initComponent
				.call(this);

		this.addEvents({
			"factors-chosen" : true
		});
	},

	/**
	 * Show the experiments and associated factors.
	 * 
	 * @param {}
	 *            config
	 */
	show : function(eeIds) {
		this.populateFactors(eeIds);
	},

	/**
	 * Get factors and then pass the results to the callback.
	 * 
	 * @param {}
	 *            eeIds
	 */
	populateFactors : function(eeIds) {
		DifferentialExpressionSearchController.getFactors(eeIds, {
			callback : this.returnFromGetFactors.createDelegate(this)
				// ,errorHandler : errorHandler
				});
	},

	/**
	 * When returning from getting the factors, load the data.
	 * 
	 * @param {}
	 *            result
	 */
	returnFromGetFactors : function(results) {
		var dataFromServer = {
			data : results
		};
		if (results.size() > 0) {
			this.efGrid = new Gemma.ExpressionExperimentExperimentalFactorGrid(dataFromServer);
			this.add(this.efGrid);
			this.efGrid.doLayout();
			Gemma.ExperimentalFactorChooserPanel.superclass.show.call(this);
		}
	}

// ,onRender : function(ct, position) {
// Gemma.ExperimentalFactorChooserPanel.superclass.onRender.call(this,
// ct,
// position);
//
// var admin = dwr.util.getValue("hasAdmin");
// }

});