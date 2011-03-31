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
	minChars : 2,
	selectOnFocus : true,
	mode : 'remote',
	queryDelay : 800, // default = 500
	listeners: {
                specialkey: function(formField, e){
                    // e.HOME, e.END, e.PAGE_UP, e.PAGE_DOWN,
                    // e.TAB, e.ESC, arrow keys: e.LEFT, e.RIGHT, e.UP, e.DOWN
                    if (e.getKey() === e.ENTER || e.getKey() === e.TAB || e.getKey() === e.RIGHT  || e.getKey() === e.DOWN ) {
                        this.expand();
                    }
					if (e.getKey() === e.ESC ) {
                        this.collapse();
                    }
                },
        		beforequery: function(qe){
            		delete qe.combo.lastQuery;
        		}
    },
	initComponent : function() {

		Ext.apply(this, {
					// format fields to show in combo, only show size in brakets if the entry is a group
					tpl: new Ext.XTemplate('<tpl for=".">' +
					'<tpl if="type==\'gene\'">' +
						'<div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" '+
						'ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>'+
					'<tpl if="type==\'geneSet\'">' +
					'	<div style="font-size:11px;background-color:#EBE3F6" class="x-combo-list-item" '+
						'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +
					'<tpl if="type==\'usersgeneSet\'">' +
					'	<div style="font-size:11px;background-color:#FFECEC" class="x-combo-list-item" '+
						'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'usersgeneSetSession\'">' +
					'	<div style="font-size:11px;background-color:#FFFFFF" class="x-combo-list-item" '+
						'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: <span style="color:red">Unsaved</span> {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'GOgroup\'">' +
					'	<div style="font-size:11px;background-color:#E3FBE9" class="x-combo-list-item" '+
						'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +
					'<tpl if="type==\'freeText\'">' +
					'	<div style="font-size:11px;background-color:#FFFFE3" class="x-combo-list-item" '+
						'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
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
								name: "taxonId",
								type: "int",
								defaultValue: "-1"
							},{
								name: "taxonName",
								type: "string",
								defaultValue: ""
								
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
				
		/***** start of query queue fix *****/
		// this makes sure that when older searches return AFTER newer searches, the newer results aren't bumped
		// this needs the lastQuery property to be initialised as null
		// note that is some other code in this file requried as well, it is marked
		this.getStore().on('beforeload', function(store, options){
			this.records = this.store.getRange();
		}, this);
		
		this.getStore().on('load', function(store, records, options){
			var query = ( options.params)? options.params[0]: null;
			if( (query === null && this.lastQuery !== null) || query !== this.lastQuery ){
				store.removeAll();
				store.add(this.records);
				if(this.records === null || this.records.length === 0){
					this.doQuery(this.lastQuery);
				}
			}else{
				this.records = this.store.getRange();
			}
		}, this);
		/***** end of query queue fix *****/
		
		this.on('focus', function(field){
			// if the text field is blank, show the automatically generated groups (like 'All human', 'All rat' etc)
			if(this.getValue() ===''){
				if(this.getTaxonId()){
					field.lastQuery = null; // needed for query queue fix
					// passing in taxon instead of taxonId breaks this call
					GenePickerController.searchGenesAndGeneGroups("", this.getTaxonId(),
						function(records) {
										this.getStore().loadData(records);
									}.createDelegate(this)
						);
				}
				/* only allowing taxon searches for now
				 * else{
					GenePickerController.searchGenesAndGeneGroups("", null,
						function(records) {
										this.getStore().loadData(records);
									}.createDelegate(this)
						);
				}*/
				
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
		var taxonId = this.getTaxonId();
		return [query, taxonId]; // default taxon is human
	},

	getGeneGroup : function() {
		if (this.getRawValue() === ''){
			return null;
		}
		return this.selectedGeneGroup;
	},

	setGeneGroup : function(combo, geneGroup, index) {
//		this.reset();
		this.selectedGeneGroup = geneGroup.data;
		this.lastQuery = null;

	},

	getTaxonId : function() {
		return this.taxonId;
	},

	setTaxonId : function(taxonId) {
		if(!taxonId){
			return;
		}
		if (!this.taxonId || this.taxonId !== taxonId) {
			this.taxonId = taxonId;
//			this.reset();

			/*
			 * this is to make sure we always search again after a taxon change, in case the user searches for the same
			 * gene. Otherwise Ext just keeps the old results.
			 */
			this.lastQuery = null;

		}
	}

});