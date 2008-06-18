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
	layout : 'border',
	width : 800,
	height : 500,
	closeAction : 'hide',
	constrainHeader : true,

	onCommit : function() {
		this.fireEvent("factors-chosen");
		this.hide();
	},

	/*
	 * initialize this panel by adding 'things' to it, like the data-store,
	 * columns, buttons (and events for buttons), etc.
	 */
	initComponent : function() {

		this.efGrid = new Gemma.ExperimentalFactorGrid({
			title : "Experimental Factors Per Experiment",
			pageSize : 25
		});

		Ext.apply(this, {
			buttons : [{
				id : 'done-selecting-button',
				text : "Done",
				handler : this.onCommit.createDelegate(this),
				scope : this
			}]
		});

		this.addEvents({
			"factors-chosen" : true
		});

		this.add(this.efGrid);

		Gemma.ExperimentalFactorChooserPanel.superclass.initComponent
				.call(this);

	},

	/**
	 * Show the experiments and associated factors.
	 * 
	 * @param {}
	 *            config
	 */
	show : function(config) {
		this.populateFactors.createDelegate(this);
		Gemma.ExperimentalFactorChooserPanel.superclass.show.call(this);
	},

	/**
	 * Get factors and then pass the results to the callback.
	 * 
	 * @param {}
	 *            eeIds
	 */
	populateFactors : function(eeIds) {
		DifferentialExpressionSearchController.getFactors(eeIds, {
			callback : this.returnFromGetFactors.createDelegate(this),
			errorHandler : errorHandler
		});
	},

	/**
	 * When returning from getting the factors, load the data.
	 * 
	 * @param {}
	 *            result
	 */
	returnFromGetFactors : function(results) {
		this.loadMask.hide();
		efGrid.loadData(results);
		// this.fireEvent('factors-retrieved', this, results);
	}

// ,onRender : function(ct, position) {
		// Gemma.ExperimentalFactorChooserPanel.superclass.onRender.call(this,
		// ct,
		// position);
		//
		// var admin = dwr.util.getValue("hasAdmin");
		// }

		});