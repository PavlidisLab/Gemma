/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

Ext.namespace('Gemma');

Gemma.GeneGroupCombo = Ext.extend(Ext.form.ComboBox, {

	name : 'geneGroupcombo',
	displayField : 'name',
	valueField : 'id',
	width : 140,// default.
	listWidth : 450, // ridiculously large so IE displays it properly
	// (usually)

	loadingText : 'Searching...',
	emptyText : "Search for gene groups",
	minChars : 1,
	selectOnFocus : true,
	mode : 'remote', // default = remote
	queryDelay : 800, // default = 500

	initComponent : function() {

		var template = new Ext.XTemplate('<tpl for="."><div style="font-size:11px" class="x-combo-list-item" ext:qtip="{name} ({size})"> {name} {description} ({size})</div></tpl>');

		Ext.apply(this, {
					tpl : template,
					store : new Gemma.GeneGroupStore({
								proxy : new Ext.data.DWRProxy(GeneSetController.findGeneSetsByName),
								sortInfo : {
									field : "name",
									dir : "ASC"
								},
								autoLoad : false
							})
				});

		Gemma.GeneGroupCombo.superclass.initComponent.call(this); 

		this.store.on("datachanged", function() {
					if (this.store.getCount() === 0) {
						this.fireEvent("invalid", "No matching genes");
					}
				}, this);
	},

	onSelect : function(record, index) {
		Gemma.GeneGroupCombo.superclass.onSelect.call(this, record, index);
		if (!this.selectedGeneGroup || record.data.id != this.selectedGeneGroup.id) {
			this.setGeneGroup(record.data);
			this.fireEvent('select', this, this.selectedGeneGroup);
		}
	},

	reset : function() {
		Gemma.GeneGroupCombo.superclass.reset.call(this);
		delete this.selectedGeneGroup;
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
		return [query, this.taxon ? this.taxon.id : 1]; // default taxon is human
	},

	getGeneGroup : function() {
		if (this.getRawValue() == '')
			return null;
		return this.selectedGeneGroup;
	},

	setGeneGroup : function(geneGroup) {
		if (this.tooltip) {
			this.tooltip.destroy();
		}
		if (geneGroup) {
			this.selectedGeneGroup = geneGroup;
			// FIXME: gene group contains no taxon, taxon is in the gene members contained in the genegroup....
			this.taxon = geneGroup.taxon;
			this.tooltip = new Ext.ToolTip({
						target : this.getEl(),
						html : String.format('{0} ({1})', geneGroup.name, geneGroup.description)
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