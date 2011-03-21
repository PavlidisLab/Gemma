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

Gemma.GeneAndGeneGroupCombo = Ext.extend(Ext.form.ComboBox, {

	name : 'geneAndGeneGroupCombo',
	displayField : 'name',
	valueField : 'id',
	width : 160,
	listWidth : 450, // ridiculously large so IE displays it properly
	
	triggerAction: 'all', //run the query specified by the allQuery config option when the trigger is clicked
	allQuery:'', // loading of auto gen and user's sets handled in Controller when query = ''

	/*
	 * Whether the user's groups should show up right away.
	 */
	prepopulate : true,

	loadingText : 'Searching...',

	emptyText : "Search for genes or gene groups",
	minChars : 3,
	selectOnFocus : true,
	mode : 'remote',
	queryDelay : 800, // default = 500

	initComponent : function() {

		Ext.apply(this, {
					// format fields to show in combo, only show size in brakets if the entry is a group
					tpl: new Ext.XTemplate('<tpl for=".">' +
					'<tpl if="type==\'gene\'">' +
						'<div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" ext:qtip="{name}: {description}"><b>{name}</b>: {description}</div>' +
					'</tpl>'+
					'<tpl if="type==\'geneSet\'">' +
					'	<div style="font-size:11px;background-color:#FFFFE3" class="x-combo-list-item" ext:qtip="{name}: {description}"><b>{name}</b>: {description} ({size})</div>' +
					'</tpl>' +
					'<tpl if="type==\'usersgeneSet\'">' +
					'	<div style="font-size:11px;background-color:#FFECEC" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonCommonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonCommonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'usersgeneSetSession\'">' +
					'	<div style="font-size:11px;background-color:#FFECEC" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonCommonName})"><b>{name}</b>: <span style="color:red">Unsaved</span> {description} ({size}) <span style="color:grey">({taxonCommonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'GOgroup\'">' +
					'	<div style="font-size:11px;background-color:#E3FBE9" class="x-combo-list-item" ext:qtip="{name}: {description}"><b>{name}</b>: {description} ({size})</div>' +
					'</tpl>' +
					'<tpl if="type==\'freeText\'">' +
					'	<div style="font-size:11px;background-color:#F9EEFF" class="x-combo-list-item" ext:qtip="{name}: {description}"><b>{name}</b>: {description} ({size})</span></div>' +
					'</tpl>' +
					'</tpl>'),
					//tpl: new Ext.XTemplate('<tpl for=".">' +
					//'<div style="font-size:11px" class="x-combo-list-item" ext:qtip="{name} ({size})">{name} - {description}</div></tpl>' ),
					store:{
						reader : new Ext.data.ListRangeReader({
								id : "sessionId"
							}, Ext.data.Record.create([{
								name : "sessionId",
								type : "int"
							},{
								name : "id",
								type : "int"
							},{
								name : "name",
								type : "string"
							},{
								name : "description",
								type : "string"
							},{
								name: "isGroup",
								type: "boolean"
							},{
								name: "size",
								type: "int"
							},{
								name: "taxon"
							},{
								name: "type",
								type: "string"
							},{
								name: "memberIds",
								defaultValue: []
							}])),
					
						proxy : new Ext.data.DWRProxy(GenePickerController.searchGenesAndGeneGroups),
						autoLoad : false
					}
				});

		Gemma.GeneAndGeneGroupCombo.superclass.initComponent.call(this);

		this.on('select', this.setGeneGroup, this);
		
		this.on('focus', function(){
			// if the text field is blank, show the automatically generated groups (like 'All human', 'All rat' etc)
			if(this.getValue() ==''){
				if(this.getTaxon()){
					// passing in taxon instead of taxonId breaks this call
					GenePickerController.searchGenesAndGeneGroups("", this.getTaxon().id,
						function(records) {
										this.getStore().loadData(records);
									}.createDelegate(this)
						);
				}else{
					GenePickerController.searchGenesAndGeneGroups("", null,
						function(records) {
										this.getStore().loadData(records);
									}.createDelegate(this)
						);
				}
				
			}
		});

	},

	reset : function() {
		Gemma.GeneAndGeneGroupCombo.superclass.reset.call(this);
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
		var taxon = this.getTaxon();
		return [query, taxon ? taxon.id : 1]; // default taxon is human
	},

	getGeneGroup : function() {
		if (this.getRawValue() == '')
			return null;
		return this.selectedGeneGroup;
	},

	setGeneGroup : function(combo, geneGroup, index) {
//		this.reset();
		this.selectedGeneGroup = geneGroup.data;
		this.lastQuery = null;

	},

	getTaxon : function() {
		return this.taxon;
	},

	setTaxon : function(taxon) {
		if (!this.taxon || this.taxon.id != taxon.id) {
			this.taxon = taxon;
//			this.reset();

			/*
			 * this is to make sure we always search again after a taxon change, in case the user searches for the same
			 * gene. Otherwise Ext just keeps the old results.
			 */
			this.lastQuery = null;

		}
	}

});