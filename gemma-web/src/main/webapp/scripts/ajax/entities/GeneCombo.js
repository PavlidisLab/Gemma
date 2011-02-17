/*
 * @version : $Id$
 * 
 */
Ext.namespace('Gemma');

/**
 * Live search field for genes.
 * 
 * @class Gemma.GeneCombo
 * @extends Ext.form.ComboBox
 */
Gemma.GeneCombo = Ext.extend(Ext.form.ComboBox, {

	name : 'genecombo',
	displayField : 'officialSymbol',
	valueField : 'id',
	width : 140,// default.
	listWidth : 450, // ridiculously large so IE displays it properly
	// (usually)

	loadingText : 'Searching...',
	emptyText : "Search for a gene",
	minChars : 1,
	selectOnFocus : true,
	mode : 'remote', // default = remote
	queryDelay : 800, // default = 500

	record : Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "taxonId"
			}, {
				name : "taxonScientificName"
			}, {
				name : "officialSymbol",
				type : "string"
			}, {
				name : "officialName",
				type : "string"
			}]),

	initComponent : function() {

		var template = new Ext.XTemplate('<tpl for="."><div style="font-size:11px" class="x-combo-list-item" ext:qtip="{officialName} ({[values.taxonScientificName]})"> {officialSymbol} {officialName} ({[values.taxonScientificName]})</div></tpl>');

		Ext.apply(this, {
					tpl : template,
					store : new Ext.data.Store({
								proxy : new Ext.data.DWRProxy(GenePickerController.searchGenes),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, this.record),
								sortInfo : {
									field : "officialSymbol",
									dir : "ASC"
								}
							})
				});

		Gemma.GeneCombo.superclass.initComponent.call(this);

		this.addEvents('genechanged');

		this.store.on("datachanged", function() {
					if (this.store.getCount() === 0) {
						this.fireEvent("invalid", "No matching genes");
					}
				}, this);
	},

	onSelect : function(record, index) {
		Gemma.GeneCombo.superclass.onSelect.call(this, record, index);
		if (!this.selectedGene || record.data.id != this.selectedGene.id) {
			this.setGene(record.data);
			// 'select' event is also fired by superclass.onSelect.call
			this.fireEvent('select', this, this.selectedGene); 
			// use event 'selectSingle' to get event as fired by super
			this.fireEvent('selectSingle', this, record); 
		}
	},

	reset : function() {
		Gemma.GeneCombo.superclass.reset.call(this);
		delete this.selectedGene;
		this.lastQuery = null;

		if (this.tooltip) {
			this.tooltip.destroy();
		}
	},

	/**
	 * Parameters for AJAX call.
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	getParams : function(query) {
		return [query, this.taxon ? this.taxon.id : -1];
	},

	getGene : function() {
		if (this.getRawValue() == '')
			return null;
		return this.selectedGene;
	},

	setGene : function(gene) {
		if (this.tooltip) {
			this.tooltip.destroy();
		}
		if (gene) {
			this.selectedGene = gene;
			this.taxon = {
				id : gene.taxonId,
				commonName : gene.taxonCommonName,
				scientificname : gene.taxonScientificName
			};
			this.tooltip = new Ext.ToolTip({
						target : this.getEl(),
						html : String.format('{0} ({1})', gene.officialName || "no description",
								gene.taxonScientificName)
					});
		}
	},

	getTaxon : function() {
		return this.taxon;
	},

	setTaxon : function(taxon) {
		if (!this.taxon || this.taxon.id != taxon.id) {
			this.taxon = taxon;
			this.reset();

			/*
			 * this is to make sure we always search again after a taxon change, in case the user searches for the same
			 * gene. Otherwise Ext just keeps the old results.
			 */
			this.lastQuery = null;

		}
	}

});

// ===================================================//

/**
 * 
 * @class Gemma.GeneSearch
 * @extends Ext.FormPanel
 */

Gemma.GeneSearch = Ext.extend(Ext.FormPanel, {

			autoHeight : true,
			frame : true,
			stateEvents : ["beforesearch"],
			labelAlign : "top",
			width : 350,
			height : 30,
			buttonAlign : 'right',
			layout : 'fit',

			initComponent : function() {

				Gemma.GeneSearch.superclass.initComponent.call(this);

				this.geneCombo = new Gemma.GeneCombo({
							hiddenName : 'g',
							id : 'gene-combo',
							fieldLabel : 'Select a gene',
							enableKeyEvents : true
						});

				this.geneCombo.on("focus", this.clearMessages, this);

				var submitButtonHandler = function(object, event) {
					var msg = this.validateSearch(this.geneCombo.getValue());
					if (msg.length === 0) {

						if (typeof pageTracker != 'undefined') {
							pageTracker._trackPageview("/Gemma/gene/showGene");
						}
						document.location.href = String.format("/Gemma/gene/showGene.html?id={0}", this.geneCombo
										.getValue());
					} else {
						this.handleError(msg);
					}
				};

				var enterButtonPressed = function(object, event) {

					var keycode = event.getKey();
					if (keycode == 13) { // 13 = keycode for "enter" button

						var msg = this.validateSearch(this.geneCombo.getValue());
						if (msg.length === 0) {

							if (typeof pageTracker != 'undefined') {
								pageTracker._trackPageview("/Gemma/gene/showGene");
							}
							document.location.href = String.format("/Gemma/gene/showGene.html?id={0}", this.geneCombo
											.getValue());
						} else {
							this.handleError(msg);
						}

					}
				};

				this.geneCombo.on("keypress", enterButtonPressed.createDelegate(this));

				var submitButton = new Ext.Button({
							text : "Go",
							handler : submitButtonHandler.createDelegate(this)
						});

				this.add(this.geneCombo);
				this.addButton(submitButton);
			},

			validateSearch : function(gene) {
				if (!gene || gene.length === 0) {
					return "Please select a valid query gene";
				}
				return "";
			},

			handleError : function(msg, e) {
				Ext.DomHelper.overwrite("geneSearchMessages", {
							tag : 'img',
							src : '/Gemma/images/icons/warning.png'
						});
				Ext.DomHelper.append("geneSearchMessages", {
							tag : 'span',
							html : "&nbsp;&nbsp;" + msg
						});
			},

			clearMessages : function() {
				if (Ext.DomQuery.select("geneSearchMessages").length > 0) {
					Ext.DomHelper.overwrite("geneSearchMessages", {
								tag : 'h3',
								html : "Gene Query"
							});
				}
			}

		});
