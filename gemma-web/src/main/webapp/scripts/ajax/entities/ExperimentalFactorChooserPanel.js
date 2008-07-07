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
	height : 250,
	closeAction : 'hide',
	constrainHeader : true,

	title : "Choose the factors to analyze in each experiment",
	eeFactorsMap : null,

	reset : function(eeSet) {
		if (this.currentEEset != null && eeSet != this.currentEEset) {
			//console.log("reset " + eeSet);
			this.eeFactorsMap = null;
		}
		this.currentEEset = eeSet;
	},

	onCommit : function() {

		if (!this.efGrid) {
			return;
		}
		this.eeFactorsMap = [];

		var eeFactorSource = this.efGrid.getSource();
		// var store = this.efGrid.getStore();

		for (var experimentName in eeFactorSource) {
			var factorName = eeFactorSource[experimentName];
			if (typeof factorName != 'string') {
				continue;
			}
			// console.log(experimentName + " --> " + factorName);
			// var eeIndex = store.find("name", experimentName);

			// locate the experiment in the data
			for (var i in this.data) {
				var rec = this.data[i];
				if (!rec.expressionExperiment) {
					continue;
				}
				var eeInfo = rec.expressionExperiment;

				var eeName = eeInfo.name;

				if (eeName != experimentName) {
					continue;
				}

				var eeId = eeInfo.id;
				var efInfo = rec.experimentalFactors;

				// locate the experimental factor.
				for (var j in efInfo) {
					var ef = efInfo[j];
					if (!ef.name) {
						continue;
					}
					var efName = ef.name;
					if (efName == factorName) {
						var efId = ef.id;
						this.eeFactorsMap.push({
							efId : efId,
							eeId : eeId
						});

						// console.log(eeId + " --> " + efId);
						break;
					}
				}

			}

		}

		this.fireEvent("factors-chosen", this.eeFactorsMap);
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

		this.addEvents("factors-chosen");

	},

	/**
	 * Show the experiments and associated factors.
	 * 
	 * @param {}
	 *            config
	 */
	show : function(eeIds) {
		Gemma.ExperimentalFactorChooserPanel.superclass.show.call(this);
		this.populateFactors(eeIds);
	},

	/**
	 * Get factors and then pass the results to the callback.
	 * 
	 * @param {}
	 *            eeIds
	 */
	populateFactors : function(eeIds) {
		if (!this.efGrid) {
			Ext.apply(this, {
				loadMask : new Ext.LoadMask(this.getEl(), {
					msg : "Loading factors ..."
				})
			});
			this.loadMask.show();
		} else {
			this.efGrid.loadMask.show();
		}
		DifferentialExpressionSearchController.getFactors(eeIds, {
			callback : this.returnFromGetFactors.createDelegate(this)
		});
	},

	/**
	 * When returning from getting the factors, load the data.
	 * 
	 * @param {}
	 *            result
	 */
	returnFromGetFactors : function(results) {
		this.data = results;
		var dataFromServer = {
			data : results
		};
		if (results.size() > 0) {
			if (this.efGrid) {
				this.remove(this.efGrid, true);
			} else {
				this.loadMask.hide();
			}
			this.efGrid = new Gemma.ExpressionExperimentExperimentalFactorGrid(dataFromServer);
			this.add(this.efGrid);
			this.doLayout();
		}
	}

});