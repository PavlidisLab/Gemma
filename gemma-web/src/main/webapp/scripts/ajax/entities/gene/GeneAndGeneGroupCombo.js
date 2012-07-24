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

Gemma.GeneAndGeneGroupComboRecord = Ext.data.Record.create([
	{
		name: "name",
		type: "string"
	}, {
		name: "description",
		type: "string"
	}, {
		name: "isGroup",
		type: "boolean"
	}, {
		name: "size",
		type: "int"
	}, {
		name: "taxonId",
		type: "int",
		defaultValue: "-1"
	}, {
		name: "taxonName",
		type: "string",
		defaultValue: ""
	}, {
		name: "memberIds",
		defaultValue: []
	}, {
		name: "comboText",
		type: "string",
		convert: function(v, record){
			if (record.resultValueObject instanceof GOGroupValueObject) {
				return record.name + ": " + record.description;
			} else {
				return record.name;
			}
		}
	}, {
		name: "resultValueObject"
	}, {
		name: "userOwned",
		type: "boolean"
	}
]);


/**
 * combo to display search results for genes and gene groups (including session bound groups
 * and GO groups)
 * @class Gemma.GeneAndGeneGroupCombo
 */
Gemma.GeneAndGeneGroupCombo = Ext.extend(Ext.form.ComboBox, {

	name : 'geneAndGeneGroupCombo',
	displayField : 'comboText',
	width : 160,
	listWidth : 450, // ridiculously large so IE displays it properly
	lazyInit: false, //true to not initialize the list for this combo until the field is focused (defaults to true)
	triggerAction: 'all', //run the query specified by the allQuery config option when the trigger is clicked
	allQuery: '', // loading of auto gen and user's sets handled in Controller when query = ''

	enableKeyEvents : true,
	loadingText : 'Still searching...',

	emptyText : "Search genes by keyword",
	listEmptyText : 'No results found',
	listEmptyTextBlankQuery : 'Enter text to search for genes',
	minChars : 2,
	selectOnFocus : false,
	autoSelect: false,
	forceSelection: true,
	typeAhead: false,
	taxonId:null,
	hideTrigger:true,
	
	lastQuery: null, // used for query queue fix
	
	mode : 'remote',
	queryDelay : 800, // default = 500
	listeners: {
                specialkey: function(formField, e){
                    // e.HOME, e.END, e.PAGE_UP, e.PAGE_DOWN,
                    // e.TAB, e.ESC, arrow keys: e.LEFT, e.RIGHT, e.UP, e.DOWN
                    if ( e.getKey() === e.TAB || e.getKey() === e.RIGHT  || e.getKey() === e.DOWN ) {
                        this.expand();
                    }else if (e.getKey() === e.ENTER ) {
						if (this.getValue() && this.getValue() !== null) {
							this.doQuery(this.getValue(), true);
						} else {
							this.doQuery('', true);
						}
                    }else if (e.getKey() === e.ESC ) {
                        this.collapse();
                    }
                },
        		beforequery: function(qe){
            		delete qe.combo.lastQuery;
        		}
    },
	
	// overwrite ComboBox onLoad function to get rid of query text being selected after a search returns
	// (this was interfering with the query queue fix)
	// only change made is commented out line
    onLoad : function(){
        if(!this.hasFocus){
            return;
        }
        if(this.store.getCount() > 0 || this.listEmptyText){
            this.expand();
            this.restrictHeight();
            if(this.lastQuery == this.allQuery){
                if(this.editable){
                  //  this.el.dom.select();
                }

                if(this.autoSelect !== false && !this.selectByValue(this.value, true)){
                    this.select(0, true);
                }
            }else{
                if(this.autoSelect !== false){
                    this.selectNext();
                }
                if(this.typeAhead && this.lastKey != Ext.EventObject.BACKSPACE && this.lastKey != Ext.EventObject.DELETE){
                    this.taTask.delay(this.typeAheadDelay);
                }
            }
        }else{
            this.collapse();
        }

    }, // end onLoad overwrite
	
	/**
	 * Parameters for AJAX call.
	 * 
	 * @param {}
	 *            query
	 * @return {}
	 */
	getParams : function(query) {
		return [query, this.getTaxonId()]; // default taxon is human
	},
	
	initComponent: function(){
	
		var geneTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" ' +
							'ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} <span style="color:grey">({taxonName})</span></div>');
		var goGroupTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#E3FBE9" class="x-combo-list-item" ' +
							'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		var phenotypeGroupTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#E3FBE9" class="x-combo-list-item" ' +
							'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		var freeTxtTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#FFFFE3" class="x-combo-list-item" ' +
							'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		var modifiedSessionTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#FFFFFF" class="x-combo-list-item" ' +
									'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: <span style="color:red">Unsaved</span> {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		var userOwnedDbGeneSetTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#FFECEC" class="x-combo-list-item" ' +
										'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		// these last three are all the same for now 				
		var dbGeneSetTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#EBE3F6" class="x-combo-list-item" ' +
								'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		var sessionGeneSetTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#EBE3F6" class="x-combo-list-item" ' +
									'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		var defaultTpl = new Ext.XTemplate('<div style="font-size:11px;background-color:#EBE3F6" class="x-combo-list-item" ' +
							'ext:qtip="{name}: {description} ({size}) ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>');
		Ext.apply(this, {
			// format fields to show in combo, only show size in brakets if the entry is a group
			tpl: new Ext.XTemplate('<tpl for=".">' +
			'{[ this.renderItem(values) ]}' +
			'</tpl>', {
				renderItem: function(values){
					if (values.resultValueObject instanceof DatabaseBackedGeneSetValueObject) {
						if (values.userOwned) {
							return userOwnedDbGeneSetTpl.apply(values);
						} else {
							return dbGeneSetTpl.apply(values);
						}
					}else if (values.resultValueObject instanceof GOGroupValueObject) {
							return goGroupTpl.apply(values);
					}else if (values.resultValueObject instanceof PhenotypeGroupValueObject) {
							return phenotypeGroupTpl.apply(values);
					}else if (values.resultValueObject instanceof FreeTextGeneResultsValueObject) {
						return freeTxtTpl.apply(values);
					}else if (values.resultValueObject instanceof SessionBoundGeneSetValueObject) {
						if (values.resultValueObject.modified) {
							return modifiedSessionTpl.apply(values);
						}else {
							return sessionGeneSetTpl.apply(values);
						}
					}else if (values.resultValueObject instanceof GeneValueObject) {
						return geneTpl.apply(values);
					} 
					return defaultTpl.apply(values);
				}
			}),
			store: {
				reader: new Ext.data.ListRangeReader({}, Gemma.GeneAndGeneGroupComboRecord),
				proxy: new Ext.data.DWRProxy(GenePickerController.searchGenesAndGeneGroups),
				autoLoad: false
			}
		});
		
		Gemma.GeneAndGeneGroupCombo.superclass.initComponent.call(this);
		
		this.on('select', this.setGeneGroup, this);
		
		/***** start of query queue fix *****/
		// enableKeyEvents config required
		this.on('keypress', function(textfield, eventObj){
			// this is set to true when query returns
			this.displayingComboValueToQueryMatch = false;
		});
		this.getStore().on('load', function(store, records, options){
			var query = (options.params) ? options.params[0] : null;
			// if the query for which the store is returning is not the same as the last query made with the combo
			// clear these results and add the previous query's results
			
			if(this.getValue() !== query){
				
				// replace returned records with those of last matching query
				store.removeAll();
				if( this.prevQuery === this.getValue() ){
					store.add(this.prevRecords);
				}
				
				// removing records works to prevent wrong/old results from popping up, but this also 
				// removes the loading text which should be shown if there's a newer query that's still working
				
				// if a valid query has already returned, don't replace results with loading text
				// if the valid query hasn't returned yet, show loading text
				if(!this.displayingComboValueToQueryMatch){
					// --- from Combo.js to show loading text ---
					this.innerList.update(this.loadingText ?''+this.loadingText+'' : '');
			        this.restrictHeight();
			        this.selectedIndex = -1;
					// --- end of code from Combo.js ---
				}
				
			}else{
				this.displayingComboValueToQueryMatch = true;
				this.prevRecords = this.store.getRange();
				this.prevQuery = (options.params) ? options.params[0] : null;
								
				// special case for empty prompted record set, display more specific feedback
				if(this.store.getCount() === 0 && (this.prevQuery === '' || this.prevQuery === null) ){
					this.innerList.update(this.listEmptyTextBlankQuery ?''+this.listEmptyTextBlankQuery+'' : '');
				}
			}
			
		}, this);
		/***** end of query queue fix *****/
		
		this.on('focus', function(field){
			// if the text field is blank, show the automatically generated groups (like 'All human', 'All rat' etc)
			if (this.getValue() === '') {
			
				this.doQuery('', true);
				this.lastQuery = null; // needed for query queue fix
			//	field.lastQuery = null; // needed for query queue fix
			// passing in taxon instead of taxonId breaks this call
			/*	GenePickerController.searchGenesAndGeneGroups("", this.getTaxonId(),
			 function(records) {
			 this.getStore().loadData(records);
			 }.createDelegate(this)
			 );
			 */
			/* only allowing taxon searches for now
			 * else{
			 GenePickerController.searchGenesAndGeneGroups("", null,
			 function(records) {
			 this.getStore().loadData(records);
			 }.createDelegate(this)
			 );
			 }*/
			}
		}, this);
		
	}, // end of initComponent

	reset : function() {
		Gemma.GeneAndGeneGroupCombo.superclass.reset.call(this);
		delete this.selectedGeneGroup;
		this.lastQuery = null;

		if (this.tooltip) {
			this.tooltip.destroy();
		}
	},

	getGeneGroup : function() {
		if (this.getRawValue() === ''){
			return null;
		}
		return this.selectedGeneGroup;
	},
	
	getGeneIds : function() {
		if (this.selectedGeneGroup) {
			return this.selectedGeneGroup.memberIds;
		}
		return [];
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