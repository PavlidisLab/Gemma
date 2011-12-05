/*
 * A text field that searches Gemma for data sets. It supports two modes: filtering, in which a starting set of datasets
 * are provided, and finding, in which the Gemma database is simply searched and all results returned.
 */

Ext.namespace('Gemma');

/*
 * Constructor...
 */
Gemma.DatasetSearchField = function(config) {

	this.eeIds = [];
	this.filterFrom = []; // starting set.

	Gemma.DatasetSearchField.superclass.constructor.call(this, config);

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
Ext.extend(Gemma.DatasetSearchField, Ext.form.TriggerField, {

			loadingText : Gemma.StatusText.Searching.generic,
			emptyText : 'Enter search term',

			initComponent : function() {
				Gemma.DatasetSearchField.superclass.initComponent.call(this);
				this.addEvents('beforesearch', 'aftersearch');

				if (this.initQuery) {
					this.setValue(this.initQuery);
					this.findDatasets();
				}
			},

			// defined in typo.css
			triggerClass : 'x-go-trigger',

			setFilterFrom : function(filterFrom) {
				this.filterFrom = filterFrom;
			},

			initEvents : function() {
				Gemma.DatasetSearchField.superclass.initEvents.call(this);
				var queryTask = new Ext.util.DelayedTask(this.findDatasets, this);
				this.el.on("keyup", function(e) {
							if (e.getCharCode() === Ext.EventObject.ENTER) {
								queryTask.delay(5);
							}
						});
			},

			filterDatasets : function() {
				var params = [this.getValue(), this.taxon ? this.taxon.id : -1];
				params.push(this.filterFrom);

			},

			findDatasets : function() {
				// If there is no taxon
				if (!this.taxon) {
					Ext.Msg.alert("Sorry", "Please select a taxon first");
					return;
				}

				var params = [this.getValue(), this.taxon ? this.taxon.id : -1];
				if (this.fireEvent('beforesearch', this, params) !== false) {
					this.lastParams = params;
					ExpressionExperimentController.find(params[0], params[1], this.foundDatasets.createDelegate(this));
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

			setTaxon : function(taxon) {
				if (taxon === undefined) {
					return;
				}
				if (taxon.id) {
					this.taxon = taxon;
				} else {
					this.taxon = {
						id : taxon
					};
				}
			},

			onTriggerClick : function(e) {
				this.findDatasets();
			}

		});

Ext.reg('datasetsearchfield', Gemma.DatasetSearchField);