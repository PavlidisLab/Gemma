/*
 * A text field that searches Gemma for data sets. It supports two modes:
 * filtering, in which a starting set of datasets are provided, and finding, in
 * which the Gemma database is simply searched and all results returned.
 */

Ext.namespace('Ext.Gemma');

/*
 * Constructor...
 */
Ext.Gemma.DatasetSearchField = function(config) {

	this.eeIds = [];
	this.filterFrom = []; // starting set.

	Ext.Gemma.DatasetSearchField.superclass.constructor.call(this, config);

	this.on('beforesearch', function(field, query) {
		if (this.loadMask) {
			this.loadMask.show();
		}
		this.addClass("x-loading");
		this.disable();
	});

	this.on('aftersearch', function(field, results) {
		if (this.loadMask) {
			this.loadMask.hide();
		}
		this.enable();
		this.removeClass("x-loading");
	});
};

/*
 * Type declaration
 */
Ext.extend(Ext.Gemma.DatasetSearchField, Ext.form.TriggerField, {

	loadingText : 'Searching...',
	emptyText : 'Enter search term',

	initComponent : function() {
		Ext.Gemma.DatasetSearchField.superclass.initComponent.call(this);

		this.addEvents('beforesearch', 'aftersearch');
	},

	triggerClass : 'x-go-trigger',

	setFilterFrom : function(filterFrom) {
		this.filterFrom = filterFrom;
	},

	initEvents : function() {
		Ext.Gemma.DatasetSearchField.superclass.initEvents.call(this);
		var queryTask = new Ext.util.DelayedTask(this.findDatasets, this);
		this.el.on("keyup", function(e) {
			if (e.getCharCode() == Ext.EventObject.ENTER) {
				queryTask.delay(5);
			}
		});
	},

	filterDatasets : function() {
		var params = [this.getValue(), this.taxon ? this.taxon.id : -1];
		params.push(this.filterFrom);
		if (this.lastParams
				&& (params[0] == this.lastParams[0] && params[1] == this.lastParams[1])) {
			return;
		}
		if (this.fireEvent('beforesearch', this, params) !== false) {
			this.lastParams = params;
			GeneLinkAnalysisManagerController.filterExpressionExperiments(
					params[0], params[1], params[2], this.foundDatasets
							.bind(this));
		}
	},

	findDatasets : function() {
		if (this.filtering) {
			this.filterDatasets();
		} else {
			// If there is no taxon
			if (!this.taxon) {
				Ext.Msg.alert("Sorry", "Please select a taxon first");
				return;
			}

			var params = [this.getValue(), this.taxon ? this.taxon.id : -1];
			if (this.lastParams
					&& (params[0] == this.lastParams[0] && params[1] == this.lastParams[1])) {
				return;
			}
			if (this.fireEvent('beforesearch', this, params) !== false) {
				this.lastParams = params;
				ExpressionExperimentController.find(params[0], params[1],
						this.foundDatasets.bind(this));
			}
		}
	},

	reset : function() {
		this.lastParams = null;
		this.value = "";
	},

	foundDatasets : function(results) {
		this.eeIds = results;
		this.fireEvent('aftersearch', this, results);
	},

	getEeIds : function() {
		return this.eeIds;
	},

	taxonChanged : function(taxon, doSearch) {
		this.taxon = taxon;
		if (doSearch) {
			this.findDatasets();
		}
	},

	onTriggerClick : function(e) {
		this.findDatasets();
	}

});

Ext.reg('datasetsearchfield', Ext.Gemma.DatasetSearchField);