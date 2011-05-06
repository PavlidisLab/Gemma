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
	queryText:'',

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
	loadExperiments: function(eeIds, callback, args){
		if (!eeIds || eeIds.length === 0) {
			return;
		}
		
		ExpressionExperimentController.loadExpressionExperiments(eeIds, function(ees){
			var eeData = [];
			var i;
			for (i = 0; i < ees.length; ++i) {
				eeData.push([ees[i].id, ees[i].shortName, ees[i].name, ees[i].arrayDesignCount, ees[i].bioAssayCount]);
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
	detailsWin: new Gemma.GeneSetDetailsDialog({
		id: 'experimentDetailsWin',
		hidden: true
	}),


	initComponent : function() {
		
		Ext.apply(this.detailsWin, {
			title: 'Provide or edit experiment group details'
			//,suggestedName: this.queryText,
			//suggestedDescription: 'Edited results of search for: "' + this.queryText + '". Created: ' + (new Date()).toString()
		});
		
		// Create RowActions Plugin
			 	this.action = new Ext.ux.grid.RowActions({
					 header:'Actions',
					keepSelection:true,
					actions:[{
						 iconCls:'icon-cross',
						tooltip:'Remove experiment'
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
					},
					//You can cancel the action by returning false from this event handler.
					beforeaction:function(grid, record, action, row, col) {
						if(grid.getStore().getCount()==1 && action==='icon-cross'){
							return false;	
						}
						return true;
					}
				});
							
				this.detailsWin.on("commit", function(args) {
										this.newGroupName = args.name;
										this.newGroupDescription = args.description;
										this.createInDatabase();
									}.createDelegate(this));
										
				// function to deal with user choice of what to do after editing an existing group
				this.editedExistingGroup = function(btn){
											if (btn === 'no') { // no is don't save
												this.saveToSession();
											}
											else 
												if (btn === 'ok') { // ok is save
													this.updateDatabase();
												}
												else 
													if (btn === 'yes') { // yes is save as
														this.detailsWin.name = this.groupName;
														this.detailsWin.description = 'Edited search results for: "' + this.groupName + '". Created: ' + (new Date()).toString();
														
														this.detailsWin.show();
													}
													else {
														return;
													}
										}.createDelegate(this);
				this.saveButton = new Ext.Button({
							id: 'save-selection-button-e',
							text: "Save",
							handler: this.save,
							scope: this,
							disabled: false
						});
				this.doneButton = new Ext.Button({
							id: 'done-selecting-button-e',
							text: "Done",
							handler: this.done,
							scope: this,
							disabled: false
						});
				// add buttons only if haven't been added already
				if(!this.buttons || this.buttons === null || this.buttons===[]){
					// add save button if user isn't logged in
					if (Ext.get('hasUser').getValue()) {
						Ext.apply(this, {
							buttons: [this.saveButton, this.doneButton, 
							{
								id: 'cancel-selecting-button-e',
								text: "Cancel",
								handler: this.cancel,
								scope: this
							}]
						});
					}
					else {
						Ext.apply(this, {
							buttons: [this.doneButton,
							 {
								id: 'cancel-selecting-button-e',
								text: "Cancel",
								handler: this.cancel,
								scope: this
							}]
						});
					}
				}
				
				Ext.apply(this, {
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
												"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={0}'>{1}</a><br><span style=\"font-color:grey\">{2}</span> ", 
												record.data.id, record.data.shortName,record.data.name);
											},
											sortable : true
										},  this.action]
									}),
							plugins:[this.action]
				}); 

		Gemma.ExpressionExperimentMembersGrid.superclass.initComponent.call(this);

		this.on('doneModification', function(){
			this.changesMade = false;
			//this.saveButton.disable();
			//this.doneButton.disable();
		});

		this.getStore().on("remove", function() {
							this.changesMade = true;
							this.saveButton.enable();
							this.doneButton.enable();
						}, this);

		this.getStore().on("add", function() {
							this.changesMade = true;
							this.saveButton.enable();
							this.doneButton.enable();
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
			 * Sets ups name and description for new group
			 */
			createDetails: function(){
												
				// if name for new group wasn't passed from parent component, make one up
				if(!this.groupName || this.groupName === null || this.groupName === ''){
					this.newGroupName = "Experiment group created: "+(new Date()).toString();
				} else{
					// adding time to end of session-bound group titles in case it's not unique
					var currentTime = new Date();
					var hours = currentTime.getHours();
					var minutes = currentTime.getMinutes();
					if (minutes < 10) {minutes = "0" + minutes;}
					this.newGroupName = '('+hours+':'+minutes+')';
					this.newGroupName += ' Edited \''+this.groupName+'\' group';
				}
								
				// if description for new group wasn't passed from parent component, make one up
				if(!this.newGroupDescription || this.newGroupDescription === null){
					this.newGroupDescription = "Temporary experiment group saved "+(new Date()).toString(); 
				} 		
			},

			/**
			 * When user clicks done, just save to session 
			 */
			done: function(){
				// if user hasn't made any changes, just close the window
				//if(!this.changesMade){
				//	this.fireEvent('doneModification');
				//	return;
				//}
				this.createDetails();
				this.saveToSession();
			},

			/**
			 * When user clicks 'save', figure out what kind of save to do
			 */
			save : function() {
				
				// if user hasn't made any changes, just close the window
				//if(!this.changesMade){
				//	this.fireEvent('doneModification');
				//	return;
				//}
				
				// if the user hasn't made any changes, save anyway
				
				
			
				// get name and description set up
				this.createDetails();				
									
				// save button should only be visible if user is logged in, but just to be safe:
				if (!Ext.get('hasUser').getValue()) {
						Ext.Msg.alert("Not logged in", "You cannot save this list because you are not logged in, "+
											" however, your list will be available temporarily.");
						this.saveToSession();
				}else{
					
					// if geneGroupId is null, then there was no group to start with
					// if user has made any changes, a new gene set will be created
					if(!this.experimentGroupId || this.experimentGroupId === null){
						//ask user if they want to save changes
								this.editedExistingGroup('yes'); // yes means 'save as'
						
					}else{// if this is an edit of an existing gene group, give options to create or edit
					
						// if group being edited is session-bound, only offer to save to database
							if (this.selectedExperimentGroup !== null && 
									(this.selectedExperimentGroup.type.indexOf('Session') >= 0 ||
									 this.selectedExperimentGroup.type.indexOf('session') >= 0)) {
								
								this.editedExistingGroup('yes'); // yes means 'save as'
							}
					
						// if group of genes being edited belongs to the user, ask if they want to save changes
							else if (this.selectedExperimentGroup !== null && 
									(this.selectedExperimentGroup.type.indexOf('user') >= 0 ||
									 this.selectedExperimentGroup.type.indexOf('User') >= 0)) {
								//ask user if they want to save changes
								Ext.Msg.show({
								   title:'Save Changes?',
								   msg: 'You have edited an existing group, '+
											'would you like to save your changes?<br>'+
											'(Unsaved lists are available until you log out.)',
								   buttons: {ok:'Save', yes:'Save As...', no:'Don\'t save'},
								   //buttons: {yes:'Save As...', no:'Don\'t save'},
								   fn: this.editedExistingGroup,
								   icon: Ext.MessageBox.QUESTION
								});
							}else{
								this.editedExistingGroup('yes'); // yes means 'save as'
							}
					}
				} 

					
				
			},
			saveToSession : function() {
				var editedGroup = this.selectedExperimentGroup; // reference has the right type already
				editedGroup.name = this.newGroupName;
				editedGroup.description = this.newGroupDescription;
				editedGroup.expressionExperimentIds = this.getEEIds();
				editedGroup.memberIds = this.getEEIds();
				editedGroup.type = 'userexperimentSetSession';
						
				ExpressionExperimentSetController.addSessionGroups([editedGroup], //returns datasets added
					function(datasetSets){
						// should be at least one datasetSet
						if (datasetSets === null || datasetSets.length === 0) {
							// TODO error message
							return;
						}
						else {
							var newRecordData = datasetSets;
							this.fireEvent('experimentListModified', newRecordData);
							this.fireEvent('doneModification');
						}
				}.createDelegate(this));
				
			},
		createInDatabase : function() {
			if(typeof this.selectedExperimentGroup !== 'undefined'){
				
				var editedGroup = this.selectedExperimentGroup; // reference has the right type already
				editedGroup.name = this.newGroupName;
				editedGroup.description = this.newGroupDescription;
				editedGroup.expressionExperimentIds = this.getEEIds();
				editedGroup.memberIds = this.getEEIds();
				editedGroup.type = 'userexperimentSet';
						
				ExpressionExperimentSetController.create([editedGroup], //returns datasets added
					function(datasetSets){
						// should be at least one datasetSet
						if (datasetSets === null || datasetSets.length === 0) {
							// TODO error message
							return;
						}
						else {
							var newRecordData = datasetSets;
							datasetSets[0].type = 'userexperimentSet';
							this.fireEvent('experimentListModified', newRecordData);
							this.fireEvent('doneModification');
						}
				}.createDelegate(this));
			}
		
		this.fireEvent('doneModification');
		
	},
	updateDatabase : function() {
		
		var groupId = this.selectedExperimentGroup.reference.id;
		this.newGroupName = this.groupName;
		var eeIds = this.getEEIds();
		
		ExpressionExperimentSetController.updateMembers(groupId, eeIds, function(msg){
			this.selectedExperimentGroup.memberIds = eeIds;
			this.selectedExperimentGroup.eeIds = eeIds;
			
			this.fireEvent('experimentListModified', this.selectedExperimentGroup);
			this.fireEvent('doneModification');
		}.createDelegate(this));
		
		this.changesMade = false;
	}
});



