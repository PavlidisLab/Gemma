/*
 */
Ext.namespace('Gemma');

/**
 * 
 * Grid to display ExpressionExperiments. Author: Paul (based on Luke's CoexpressionDatasetGrid) $Id:
 * ExpressionExperimentGrid.js,v 1.13 2008/04/23 19:54:46 kelsey Exp $
 */
Gemma.ExpressionExperimentMembersGrid = Ext.extend(Gemma.GemmaGridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	collapsible : false,
	readMethod : ExpressionExperimentController.loadExpressionExperiments.createDelegate(this, [], true),

	autoExpandColumn : 'name',

	stripeRows: true,
	changeMade: false,
	editable : true,
	stateful : false,
	layout : 'fit',
	width : 450,
	height : 250,
	experimentGroupId: null, 
	viewConfig : {forceFit : true},

			/**
			 * Add to table.
			 * 
			 * @param {}
			 *            eeIds
			 * @param {}
			 *            callback optional
			 * @param {}
			 *            args optional
			 */
			loadExperiments : function(eeIds, callback, args) {
				if (!eeIds || eeIds.length === 0) {
					return;
				}

				ExpressionExperimentController.loadExpressionExperiments(eeIds, function(ees) {
							var eeData = [];
							var i;
							for (i = 0; i < ees.length; ++i) {
								eeData.push([ees[i].id, ees[i].shortName, ees[i].name, 
								ees[i].arrayDesignCount, ees[i].bioAssayCount]);
							}
							/*
							 * FIXME this can result in the same gene listed twice. This is taken care of at the server
							 * side but looks funny.
							 */
							this.getStore().loadData(eeData);
							if (callback) {
								callback(args);
							}
						}.createDelegate(this));
			},
				
			// input window for creation of new groups
			detailsWin : new Gemma.GeneSetDetailsDialog({
										hidden: true
				}),


	initComponent : function() {
		// Create RowActions Plugin
			 	this.action = new Ext.ux.grid.RowActions({
					 header:'Actions',
					//,autoWidth:false
					//,hideMode:'display'
					keepSelection:true,
					actions:[{
						 iconCls:'icon-cross',
						tooltip:'Remove gene'
					}],
					callbacks:{
						'icon-cross':function(grid, record, action, row, col) {
						}
					}
				});
				
				// dummy action event handler - just outputs some arguments to console
				this.action.on({
					action:function(grid, record, action, row, col) {
						if(action==='icon-cross'){
							this.changeMade = true;
							grid.getStore().remove(record);
						}
					}
					//You can cancel the action by returning false from this event handler.
					,beforeaction:function() {
					}
				});
							
				this.detailsWin.on("commit", function(args) {
										this.newGroupName = args.name;
										this.newGroupDescription = args.description;
										this.saveToDatabase();
									}.createDelegate(this));
										
				// function to deal with user choice of what to do after editing an existing group
				this.editedExistingGroup = function(btn){
											if (btn === 'no') { // no is don't save
												this.saveToSession();
											}
											else 
												if (btn === 'ok') { // ok is save
													this.saveToSession();
												}
												else 
													if (btn === 'yes') { // yes is save as
														this.detailsWin.name = '';
														this.detailsWin.description = '';
														this.detailsWin.show();
													}
													else {
														return;
													}
										}.createDelegate(this);
				
				Ext.apply(this, {
							buttons : [/*{
										id : 'save-button',
										text : "Save...",
										handler : this.save,
										scope : this
									},{
										id : 'done-selecting-button',
										text : "Done",
										handler : this.done,
										scope : this
									},*/ {
										id : 'cancel-selecting-button',
										text : "Cancel",
										handler : this.cancel,
										scope : this
									}],
							store : new Ext.data.SimpleStore({
										fields : [{
													name : "id",
													type : "int"
												}, {
													name : "shortName",
													type : "string"
												}, {
													name : "name",
													type : "string"
												}, {
													name : "arrayDesignCount",
													type : "int"
												}, {
													name : "bioAssayCount",
													type : "int"
												}],
										sortInfo : {
											field : 'shortName',
											direction : 'ASC'
										}
									}),
							colModel: new Ext.grid.ColumnModel({
										defaults: {
											sortable: true
										},
										columns : [
										{
											id : 'shortName',
											header : "Dataset",
											dataIndex : "shortName",
											tooltip : "The unique short name for the dataset, often the accession number from the originating source database. Click on the name to view the details page.",
											renderer: function(value, metadata, record, row, col, ds){
												return String.format(
												"<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a><br><span style=\"font-color:grey\">{2}</span> ", 
												record.data.id, record.data.shortName,record.data.name);
											},
											sortable : true
										}/*,  this.action*/]
									}),
							plugins:[this.action]
				}); 

		Gemma.ExpressionExperimentMembersGrid.superclass.initComponent.call(this);


		this.getStore().on("remove", function() {
							this.changesMade = true;
						}, this);

		this.getStore().on("add", function() {
							this.changesMade = true;
						}, this);
						
		this.getStore().on("load", function(store, records, options) {
					this.doLayout.createDelegate(this);
				}, this);

		if (this.eeids) {
			this.getStore().load({
						params : [this.eeids]
					});
		}

	},

	formatEE : function(value, metadata, record, row, col, ds) {
		// fixme: this is duplicated code.
		var eeTemplate = new Ext.XTemplate(
				'<tpl for="."><a target="_blank" title="{name}" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=',
				'{[values.sourceExperiment ? values.sourceExperiment : values.id]}"',
				' ext:qtip="{name}">{shortName}</a></tpl>');
		return eeTemplate.apply(record.data);
	},

	/**
	 * Return all the ids of the experiments shown in this grid.
	 */
	getEEIds : function() {
		var result = [];
		this.store.each(function(rec) {
					result.push(rec.get("id"));
				});
		return result;
	},

	isEditable : function() {
		return this.editable;
	},

	setEditable : function(b) {
		this.editable = b;
	},



			/**
			 * When user clicks cancel, just let parent know 
			 */
			cancel: function(){
				this.fireEvent('doneModification');
			},


			/**
			 * When user clicks 'done', deal with changes
			 */
			done : function() {
				
				// if user hasn't made any changes, just close the window
				if(!this.changesMade){
					this.fireEvent('doneModification');
					return;
				}
				
				// if name for new group wasn't passed from parent component, make one up
				if(!this.groupName || this.groupName === null || this.groupName === ''){
					this.newGroupName = "Experiment group created: "+(new Date()).toString();
				} else{
					this.newGroupName = 'Edited \''+this.groupName+'\' group';
					// adding time to end of session-bound group titles in case it's not unique
					var currentTime = new Date();
					var hours = currentTime.getHours();
					var minutes = currentTime.getMinutes();
					if (minutes < 10) {minutes = "0" + minutes;}
					this.newGroupName += ' ('+hours+':'+minutes+')';
				}
								
				// if description for new group wasn't passed from parent component, make one up
				if(!this.newGroupDescription || this.newGroupDescription === null){
					this.newGroupDescription = "Temporary experiment group saved "+(new Date()).toString(); 
				} 					
				
				// for now, just save to session
				this.saveToSession();
				this.fireEvent('experimentListModified', this.getEEIds(), this.newGroupName);
				this.fireEvent('doneModification');
				return;
				
				// if user is not logged in, only saving to session is available
				if (!Ext.get('hasUser').getValue()) {
						this.saveToSession();
				}else{
					// if geneGroupId is null, then there was no group to start with
					// if user has made any changes, a new gene set will be created
					if(!this.experimentGroupId || this.experimentGroupId === null){
						//ask user if they want to save changes
								Ext.Msg.show({
									title: 'Save Changes?',
									msg: 'Would you like to save your changes? <br>' +
									'(Unsaved lists are available until you log out.) ',
									buttons: {
										yes: 'Save As',
										no: 'Don\'t save'
									},
									fn: this.editedExistingGroup,
									icon: Ext.MessageBox.QUESTION
								});
						
					}else{// if this is an edit of an existing gene group, give options to create or edit
					
						// if group of genes being edited belongs to the user, ask if they want to save changes
							if(this.selectedExperimentGroup!==null && this.selectedExperimentGroup.type.indexOf('user')>=0){
								//ask user if they want to save changes
								Ext.Msg.show({
								   title:'Save Changes?',
								   msg: 'You have edited an existing group, '+
											'would you like to save your changes?'+
											'(Unsaved lists are available until you log out.) ',
								   buttons: {ok:'Save', yes:'Save As', no:'Don\'t save'},
								   fn: this.editedExistingGroup,
								   icon: Ext.MessageBox.QUESTION
								});
							}else{
								Ext.Msg.show({
									title: 'Save Changes?',
									msg: 'Would you like to save your changes? <br>' +
									'(Unsaved lists are available until you log out.) ',
									buttons: {
										yes: 'Save As',
										no: 'Don\'t save'
									},
									fn: this.editedExistingGroup,
									icon: Ext.MessageBox.QUESTION
								});
							}
					}
				} 
					// update the component that create this grid (ex search widget) with the new gene ids
					// send geneIDs for testing, eventually will always pass group id (session or db bound)
					this.fireEvent('experimentListModified', this.getEEIds(), this.newGroupName);
					
					this.fireEvent('doneModification');
					
				
			},
			saveToSession : function() {
				var name = this.newGroupName;
				var description = this.newGroupDescription;
		
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
				
				this.store.each(
					function(r){
						records.push(r.copy());
					}
				);
				
				tempstore.add(records);
				
				sessionStore = new Gemma.UserSessionGeneGroupStore();		
				
				var ids = [];
				
				tempstore.each(
					function(r){
						ids.push(r.get("id"));
					}	
				);
				
				var RecType = sessionStore.record;
				var rec = new RecType();
				rec.set("geneIds", ids);
				rec.set("size", ids.length);	
				rec.set("name", name);
				rec.set("description",description);
				rec.set("session", true);
				
				sessionStore.add(rec);
				
				sessionStore.save();
				
			},
		saveToDatabase : function() {
		var name = this.newGroupName;
		var description = this.newGroupDescription;
			
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
		this.store.each(
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
		
		var RecType = sessionStore.record;
		var rec = new RecType();
		rec.set("geneIds", ids);
		rec.set("size", ids.length);	
		rec.set("name", name);
		rec.set("description",description);
		rec.set("session", false);
		
		sessionStore.add(rec);
		
		sessionStore.save();
		
	}
});



