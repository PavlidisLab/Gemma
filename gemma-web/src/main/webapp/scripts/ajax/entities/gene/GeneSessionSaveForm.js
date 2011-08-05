/**
 * The input for coexpression searches. This form has two main parts: a GeneChooserPanel, and the coexpression search
 * parameters.
 * 
 * Coexpression search has three main settings, plus an optional part that appears if the user is doing a 'custom'
 * analysis: Stringency, "Among query genes" checkbox, and the "scope".
 * 
 * If scope=custom, a DatasetSearchField is shown.
 * 
 * @authors Luke, Paul, klc
 * 
 * @version $Id$
 */

Gemma.MIN_STRINGENCY = 2;

Gemma.GeneSessionSaveForm = Ext.extend(Ext.Panel, {

	layout : 'table',
	width : 390,
	height : 480,
	frame : true,
	stateful : true,
	stateEvents : ["beforesearch"],
	taxonComboReady : false,
	eeSetReady : false,

	// share state with main page...
	stateId : "Gemma.CoexpressionSearch",

	defaults : {
		collapsible : true,
		bodyStyle : "padding:10px"
	},

	
	
	
	
	/* CAM */
	doSaveDatabase : function() {
		
		var SessionStoreRecord = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "name",
				type : "string",
				convert : function(v, rec) {
					if (v.startsWith("GO")) {
						return rec.description;
					}
					return v;
				}
			}, {
				name : "description",
				type : "string",
				convert : function(v, rec) {
					if (rec.name.startsWith("GO")) {
						return rec.name;
					}
					return v;
				}

			}, {
				name : "publik",
				type : "boolean"
			}, {
				name : "size",
				type : "int"
			}, {
				name : "shared",
				type : 'boolean'
			}, {
				name : "currentUserHasWritePermission",
				type : 'boolean'
			},{
				name : "session",
				type : 'boolean'
			},{
				name : "geneIds"
			}]);
		
		
		
		var tempstore = new Ext.data.SimpleStore({
										fields : [{
													name : 'id',
													type : 'int'
												}, {
													name : 'taxon'
												}, {
													name : 'officialSymbol',
													type : 'string'
												}, {
													name : 'officialName',
													type : 'string'
												}],
										sortInfo : {
											field : 'officialSymbol',
											direction : 'ASC'
										}
									});
		
		var records = [];
		
		//make a copy of all the records in geneChooserPanel to a temporary store
		this.geneChooserPanel.store.each(
			function(r){
				records.push(r.copy());
			}
		);
		
		tempstore.add(records);
		
		sessionStore = new Gemma.GeneGroupStore();		
		
		var ids = [];
		
		tempstore.each(
			function(r){
				
				ids.push(r.get("id"));			
				
			}	
		);
		
		var rec = new SessionStoreRecord();
		rec.set("geneIds", ids);
		rec.set("size", ids.length);	
		rec.set("name", this.nameOfGroup.getValue());
		rec.set("description",this.description.getValue());
		
		sessionStore.add(rec);
		
		sessionStore.save();
		
		//GeneSetController.create(rec.data, function(records) {
		//							alert('hi');
		//						}.createDelegate(this));
		
	},
	
	
	saveToSession : function() {
		var SessionStoreRecord = Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "name",
				type : "string",
				convert : function(v, rec) {
					if (v.startsWith("GO")) {
						return rec.description;
					}
					return v;
				}
			}, {
				name : "description",
				type : "string",
				convert : function(v, rec) {
					if (rec.name.startsWith("GO")) {
						return rec.name;
					}
					return v;
				}

			}, {
				name : "publik",
				type : "boolean"
			}, {
				name : "size",
				type : "int"
			}, {
				name : "shared",
				type : 'boolean'
			}, {
				name : "currentUserHasWritePermission",
				type : 'boolean'
			},{
				name : "session",
				type : 'boolean'
			},{
				name : "geneIds"
			}]);
		
		
		
		var tempstore = new Ext.data.SimpleStore({
										fields : [{
													name : 'id',
													type : 'int'
												}, {
													name : 'taxon'
												}, {
													name : 'officialSymbol',
													type : 'string'
												}, {
													name : 'officialName',
													type : 'string'
												}],
										sortInfo : {
											field : 'officialSymbol',
											direction : 'ASC'
										}
									});
		
		var records = [];
		
		//make a copy of all the records in geneChooserPanel to a temporary store
		this.geneChooserPanel.store.each(
			function(r){
				records.push(r.copy());
			}
		);
		
		tempstore.add(records);
		
		sessionStore = new Gemma.SessionGeneGroupStore();		
		
		var ids = [];
		
		tempstore.each(
			function(r){
				
				ids.push(r.get("id"));			
				
			}	
		);
		
		var rec = new SessionStoreRecord();
		rec.set("id", '-1');
		rec.set("geneIds", ids);
		rec.set("size", ids.length);	
		rec.set("name", this.nameOfGroup.getValue());
		rec.set("description",this.description.getValue());
		
		sessionStore.add(rec);
		
		sessionStore.save();
				
			},

	handleError : function(msg, e) {
		// console.log(e); // this contains the full stack.
		Ext.DomHelper.overwrite("coexpression-messages", {
					tag : 'img',
					src : '/Gemma/images/icons/warning.png'
				});
		Ext.DomHelper.append("coexpression-messages", {
					tag : 'span',
					html : "&nbsp;&nbsp;" + msg
				});
		this.returnFromSearch({
					errorState : msg
				});

	},



	initComponent : function() {

		this.geneChooserPanel = new Gemma.GeneGrid({
					height : 400,
					width : 400,
					region : 'center',
					id : 'gene-chooser-panel'
				});
		
		this.nameOfGroup = new Ext.form.TextField({
			
			fieldLabel: 'Name of Group',
			region : 'south',
			width : 100,
			name: 'name'
			
		});
		
		
		
		this.description = new Ext.form.TextField({
			
			fieldLabel: 'Description',
			region : 'west',
			width : 100,
			name: 'description'
			
		});
		
		this.form = new Ext.form.FormPanel({
			items: [this.nameOfGroup, this.description],
			width:400
		});
		

		Ext.apply(this.geneChooserPanel.getTopToolbar().taxonCombo, {
					stateId : "",
					stateful : false,
					stateEvents : []
				});

		

		Ext.apply(this, {

			title : "Search configuration",
			items : [this.geneChooserPanel, this.form],
			buttons : [{
				text : "Save To Session",
				handler : this.saveToSession.createDelegate(this, [], false)
					// pass
					// no
					// parameters!
				},
				{
				text : "Save To Database",
				handler : this.doSaveDatabase.createDelegate(this, [], false)
					// pass
					// no
					// parameters!
				}
				
				
				]
		});
		Gemma.GeneSessionSaveForm.superclass.initComponent.call(this);
	
	}

});
