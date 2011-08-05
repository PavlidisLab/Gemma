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

Gemma.MyEXTGeneAndGeneGroupCombo = Ext.extend(Ext.form.ComboBox, {
			name : 'geneAndGeneGroupCombo',
			displayField : 'id',
			valueField : 'id',
			loadingText : 'Searching...',
			emptyText : "Choose or search for groups",
			selectOnFocus : true,
			queryDelay : 800, // default = 500
			width : 160,
			height: 50,
			listWidth : 450, // ridiculously large so IE displays it properly
			minChars : 3,
			resizable:true,
			listeners:{
				// before the combo box runs a query on its contents/store, populate its store from
				// the feeder stores
				beforequery:function(queryEventObj){
					var params = this.getParams(queryEventObj.query);
					queryEventObj.combo.getStore().loadFeeders(params);
				}
			},
	initComponent : function() {

		Ext.apply(this, {
					tpl: new Ext.XTemplate('<tpl for="."><div style="font-size:11px" class="x-combo-list-item" ext:qtip="{name} ({size}) {id}"> {name} - {description} ({size})</div></tpl>'),
					store: new Gemma.MyGeneAndGroupStore()
				});

		Gemma.MyEXTGeneAndGeneGroupCombo.superclass.initComponent.call(this);

		this.on('select', this.setGeneGroup, this);

		/*if (this.prepopulate) {
			this.on('focus', function() {
						GeneSetController.getUsersGeneGroups(false, this.taxon.id, function(records) {
									this.store.loadData(records);
								}.createDelegate(this));

					}.createDelegate(this), this, {
						scope : this,
						single : true
					});
		}*/

	},

	reset : function() {
		Gemma.MyEXTGeneAndGeneGroupCombo.superclass.reset.call(this);
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
		if (this.getRawValue() === ''){
			return null;		
		}
		return this.selectedGeneGroup;
	},

	setGeneGroup : function(combo, geneGroup, index) {
		// this.reset();
		this.selectedGeneGroup = geneGroup.data;
		this.tooltip = new Ext.ToolTip({
					target : this.getEl(),
					html : String.format('{0} ({1})', this.selectedGeneGroup.text, this.selectedGeneGroup.id)
				});
		this.lastQuery = null;

	},

	getTaxon : function() {
		return this.taxon;
	},

	setTaxon : function(taxon) {
		if (!this.taxon || this.taxon.id !== taxon.id) {
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

Gemma.MyGeneAndGroupStore = Ext.extend(Ext.data.Store,{
	proxy: new Ext.data.MemoryProxy(),
	reader : new Ext.data.ListRangeReader({
								id : "id"
							}, Ext.data.Record.create([{
								name : "id",
								type : "int"
							},{
								name : "name",
								type : "string"
							},{
								name : "description",
								type : "string"
							},{
								name: "size",
								type: "int"
							},{
								name: "geneIds"
							},{
								name : "taxonId"
							}, {
								name : "taxonScientificName"
							}, {
								name : "officialSymbol",
								type : "string"
							}, {
								name : "officialName",
								type : "string"
							}])),
					myGeneStore : new Ext.data.Store({
								proxy : new Ext.data.DWRProxy(GenePickerController.searchGenes),
								reader : new Ext.data.ListRangeReader({
											id : "id"
										}, Ext.data.Record.create([{
								name : "id",
								type : "int"
							},{
								name : "name",
								type : "string"
							},{
								name : "description",
								type : "string"
							},{
								name: "size",
								type: "int"
							},{
								name: "geneIds"
							},{
								name : "taxonId"
							}, {
								name : "taxonScientificName"
							}, {
								name : "officialSymbol",
								type : "string"
							}, {
								name : "officialName",
								type : "string"
							}])),
								sortInfo : {
									field : "officialSymbol",
									dir : "ASC"
								}
							}),
					myGeneGroupStore : new Ext.data.Store({
								reader : new Ext.data.ListRangeReader({
										id : "id"
									}, Ext.data.Record.create([{
								name : "id",
								type : "int"
							},{
								name : "name",
								type : "string"
							},{
								name : "description",
								type : "string"
							},{
								name: "size",
								type: "int"
							},{
								name: "geneIds"
							},{
								name : "taxonId"
							}, {
								name : "taxonScientificName"
							}, {
								name : "officialSymbol",
								type : "string"
							}, {
								name : "officialName",
								type : "string"
							}])),
							
								proxy : new Ext.data.DWRProxy(GeneSetController.findGeneSetsByName)
						}),
					loadFeeders: function(params){
						this.myGeneStore.load({
								params:params,
								callback: function(r, options, success){
									// add records to reader store
									this.add(r);
									this.fireEvent("datachanged",this);
								},
								scope:this, 
								add:false
							});
						this.myGeneGroupStore.load({
								params:params,
								callback: function(r, options, success){
									// add records to reader store
									this.add(r);
									this.fireEvent("datachanged",this);
								},
								scope:this, 
								add:false
							});
					}
});
