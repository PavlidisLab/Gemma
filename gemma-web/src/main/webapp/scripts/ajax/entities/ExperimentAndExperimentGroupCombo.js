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

Gemma.ExperimentAndExperimentGroupCombo = Ext.extend(Ext.form.ComboBox, {

	name : 'experimentAndExperimentGroupCombo',
	displayField : 'name',
	valueField : 'id',
	width : 160,
	listWidth : 450, // ridiculously large so IE displays it properly
	//lazyInit: false, //true to not initialize the list for this combo until the field is focused (defaults to true)
	triggerAction: 'all', //run the query specified by the allQuery config option when the trigger is clicked
	allQuery: '', // loading of auto gen and user's sets handled in Controller when query = ''

	loadingText : 'Searching...',

	emptyText : "Select or search for a scope",
	minChars : 3,
	selectOnFocus : false,
	autoSelect: false,
	forceSelection: true,
	mode : 'remote',
	queryDelay : 800, // default = 500
	/*listeners: {
		beforequery:function(queryEvent){
			// queryEvent has combo, query, forceAll and cancel fields
			//var store = queryEvent.combo.getStore();
			console.log("before query");
		},
		beforeexpand:function(combo){
			console.log("before expand");
		},
		beforecollapse:function(combo){
			console.log("before collapse");
		}
	},
	// overwrite expand, only proceed is beforeexpand returns true
	expand: function () {
        if(this.isExpanded() || !this.hasFocus){
            return;
        }
    	if (this.fireEvent("beforeexpand", this)) {
            Gemma.ExperimentAndExperimentGroupCombo.superclass.expand.apply(this);
        }
    },
	// overwrite collapse
	collapse: function () {
        if(!this.isExpanded() || !this.hasFocus){
            return;
        }
    	if (this.fireEvent("beforecollapse", this)) {
           Gemma.ExperimentAndExperimentGroupCombo.superclass.collapse.apply(this);
        }
    },*/
	

	initComponent : function() {

        this.addEvents("beforeexpand","beforecollapse"); // if beforeexpand returns false, expand is cancelled

		Ext.apply(this, {
					// format fields to show in combo, only show size in brakets if the entry is a group
					tpl: new Ext.XTemplate('<tpl for=".">' +
					'<tpl if="type==\'experiment\'">' +
						'<div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>'+
					'<tpl if="type==\'usersExperimentSet\'">' +
					'	<div style="font-size:11px;background-color:#FFECEC" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'experimentSetSession\'">' +
					'	<div style="font-size:11px;background-color:#FFFFFF" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>:  <span style="color:red">Unsaved</span> {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'experimentSet\'">' +
					'	<div style="font-size:11px;background-color:#EBE3F6" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +	
					'<tpl if="type==\'freeText\'">' +
					'	<div style="font-size:11px;background-color:#FFFFE3" class="x-combo-list-item" ext:qtip="{name}: {description} ({taxonName})"><b>{name}</b>: {description} ({size}) <span style="color:grey">({taxonName})</span></div>' +
					'</tpl>' +
					'</tpl>'),
				store:{
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
				
					proxy : new Ext.data.DWRProxy(ExpressionExperimentController.searchExperimentsAndExperimentGroups),
					autoLoad : false
				}
		});

		Gemma.ExperimentAndExperimentGroupCombo.superclass.initComponent.call(this);

		this.on('select', this.setExpressionExperimentGroup, this);

		this.on('focus', function(){
			// if the text field is blank, show the automatically generated groups (like 'All human', 'All rat' etc)
			if(this.getValue() ===''){
				ExpressionExperimentController.searchExperimentsAndExperimentGroups("",
					function(records) {
									this.getStore().loadData(records);
								}.createDelegate(this)
					);
			}
		});
	},

	reset : function() {
		Gemma.ExperimentAndExperimentGroupCombo.superclass.reset.call(this);
		delete this.selectedExpressionExperimentGroup;
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
		return [query];
	},

	getExpressionExperimentGroup : function() {
		if (this.getRawValue() === ''){
			return null;
		}
		return this.selectedExpressionExperimentGroup;
	},

	setExpressionExperimentGroup : function(combo, expressionExperimentGroup, index) {
		//this.reset();
		this.selectedExpressionExperimentGroup = expressionExperimentGroup.data;
		this.lastQuery = null;

	}

});