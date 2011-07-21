
Ext.namespace('Gemma');

/**
 * Grid to display ExpressionExperiment group members and allow the user to remove and add members.
 */
Gemma.ExpressionExperimentMembersGrid = Ext.extend(Gemma.GemmaGridPanel, {

	/*
	 * Do not set header : true here - it breaks it.
	 */
	collapsible : false,
	readMethod : ExpressionExperimentController.loadExpressionExperiments.createDelegate(this, [], true),

	autoExpandColumn : 'name',

	stripeRows : true,
	changeMade : false,
	editable : true,
	stateful : false,
	layout : 'fit',
	width : 450,
	height : 500,
	viewConfig : {
		forceFit : true
	},
	queryText : '',
	addExperiments: true,
	taxonId: null,
	allowSaveToSession:true, // controls presence of 'done' button

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
						eeData.push([ees[i].id, ees[i].shortName, ees[i].name, ees[i].arrayDesignCount,
								ees[i].bioAssayCount]);
					}
					/*
					 * FIXME this can result in the same gene listed twice. This
					 * is taken care of at the server side but looks funny.
					 */
					this.getStore().loadData(eeData);
					if (callback) {
						callback(args);
					}
					this.fireEvent('experimentsLoaded');
				}.createDelegate(this));
	},
	addExperiments : function(data) { // maybe this won't work b/c combo returns search objects?
				if (!data) {
					return;
				}
				this.selectedExperimentOrGroupRecord = data;

				var id = data.reference.id;
				var isGroup = data.isGroup;
				var reference = data.reference;
				var name = data.name;
		
				var taxonId = data.taxonId;
				var taxonName = data.taxonName;
						
				var eeIdsToAdd = [];
				// load preview of group if group was selected
				if (isGroup) {
					eeIdsToAdd = data.memberIds;
				}else{
					eeIdsToAdd = [id];
				}
				if (!eeIdsToAdd || eeIdsToAdd === null || eeIdsToAdd.length === 0) {
					return;
				}
				
				ExpressionExperimentController.loadExpressionExperiments(eeIdsToAdd, function(ees) {

					for (var j = 0; j < ees.size(); j++) {
						if (this.getStore().find("id", ees[j].id) < 0) {
							var Constructor = this.store.recordType;
							var record = new Constructor(ees[j]);
							this.getStore().add([record]);
						}
					}
					/* maybe should notify user with text at bottom that 'x experiments have been added'
					this.experimentPreviewContent.setTitle(
						'<span style="font-size:1.2em">'+this.experimentCombo.getRawValue()+'</span>'+
						' &nbsp;&nbsp;<span style="font-weight:normal">(' + ids.size() + " experiments)");
					this.experimentSelectionEditorBtn.setText('<a>' + (ids.size() - limit) + ' more - Edit</a>');
					*/

				}.createDelegate(this));
				
				
			},

	/*
	 * set the taxon for this grid and for the toolbar to control what can be added from combo
	 */
	setTaxonId: function(taxonId){
		this.taxonId = taxonId;
		Ext.apply(this.getTopToolbar().eeCombo, {
			taxonId: taxonId
		});
	},

	initComponent : function() {

		Ext.apply(this, {
			tbar: new Gemma.ExperimentAndGroupAdderToolbar({
				eeGrid : this
			})
		});

		// Create RowActions Plugin
		this.action = new Ext.ux.grid.RowActions({
					header : 'Actions',
					keepSelection : true,
					actions : [{
								iconCls : 'icon-cross',
								tooltip : 'Remove experiment'
							}],
					callbacks : {
						'icon-cross' : function(grid, record, action, row, col) {
						}
					}
				});

		this.action.on({
					action : function(grid, record, action, row, col) {
						if (action === 'icon-cross') {
							this.changeMade = true;
							grid.getStore().remove(record);
						}
					},
					// You can cancel the action by returning false from this
					// event handler.
					beforeaction : function(grid, record, action, row, col) {
						if (grid.getStore().getCount() == 1 && action === 'icon-cross') {
							return false;
						}
						return true;
					}
				});


		// function to deal with user choice of what to do after editing an
		// existing group
		this.editedExistingGroup = function(btn) {
			if (btn === 'no') { // no is cancel 
				//this.saveToSession();
			} else if (btn === 'ok') { // ok is save
				this.updateDatabase();
			} else if (btn === 'yes') { // yes is save as
			
				// input window for creation of new groups
				var detailsWin = new Gemma.GeneSetDetailsDialog({
					title: 'Provide or edit experiment group details'
				});
				detailsWin.on("hide", function(args){
					this.close();
				});
				detailsWin.on("commit", function(args){
					this.newGroupName = args.name;
					this.newGroupDescription = args.description;
					this.createInDatabase();
				}, this);
				
				detailsWin.name = this.groupName;
				detailsWin.description = 'Edited search results for: "' + this.groupName + '". Created: ' +
				(new Date()).toString();
				
				detailsWin.show();
			}
			else {
				return;
			}
		}.createDelegate(this);

		this.saveButton = new Ext.Button({
			text: "Save",
			handler: this.saveBtnHandler,
			qtip: 'Save your selection before returning to search.',
			scope: this,
			disabled: false
		});
		this.doneButton = new Ext.Button({
			text: "Done",
			handler: this.done,
			qtip: 'Return to search using your edited list. (Selection will be kept temporarily.)',
			scope: this,
			disabled: true
		});
		this.exportButton = new Ext.Button({
			text: "Export",
			qtip: 'Get a plain text version of this list',
			handler: this.exportToTxt,
			scope: this,
			disabled: false
		});
	
		Ext.apply(this, {
		
			setButtonVisibilities: function(){
				
				this.saveButton.show();
				
				if (this.allowSaveToSession) {
					this.doneButton.show();
				}
				else {
					this.doneButton.hide();
				}				
			},
			buttons: [this.saveButton, this.doneButton, this.exportButton, {
				text: "Cancel",
				handler: this.cancel,
				scope: this
			}]
		});
		this.setButtonVisibilities();

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
			colModel : new Ext.grid.ColumnModel({
				defaults : {
					sortable : true
				},
				columns : [{
					id : 'shortName',
					header : "Dataset",
					dataIndex : "shortName",
					renderer : function(value, metadata, record, row, col, ds) {
						return String
								.format(
									"<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={0}'>{1}</a>"+
									"<br><span style='font-color:grey;white-space:normal !important;'>{2}</span> ",
									record.data.id, record.data.shortName, record.data.name);
					},
					sortable : true
				}, this.action]
			}),
			plugins : [this.action]
		});

		
		this.ajaxLogin = null;
		this.ajaxRegister = null;
		
		Gemma.ExpressionExperimentMembersGrid.superclass.initComponent.call(this);

		this.on('doneModification', function() {
			this.changesMade = false;
				// this.saveButton.disable();
				 this.doneButton.disable();
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
		
				
		this.on('experimentsLoaded',function(){
			if (this.selectedExperimentGroup && this.selectedExperimentGroup.reference) {
				ExpressionExperimentSetController.canCurrentUserEditGroup(this.selectedExperimentGroup.reference, function(response){
					var dataMsg = Ext.util.JSON.decode(response);
					if (!dataMsg.userCanEditGroup || !dataMsg.groupIsDBBacked) {
						this.saveButton.setText("Save As");
					}
				}.createDelegate(this));
			}
		});

	}, //EO init

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
	cancel : function() {
		this.fireEvent('doneModification');
	},

	/**
	 * Sets ups name and description for new group
	 */
	createDetails : function() {

		// if name for new group wasn't passed from parent component, make one
		// up
		if (!this.groupName || this.groupName === null || this.groupName === '') {
			this.newGroupName = "Experiment group created: " + (new Date()).toString();
		} else {
			// adding time to end of session-bound group titles in case it's not
			// unique
			var currentTime = new Date();
			var hours = currentTime.getHours();
			var minutes = currentTime.getMinutes();
			if (minutes < 10) {
				minutes = "0" + minutes;
			}
			this.newGroupName = '(' + hours + ':' + minutes + ')';
			this.newGroupName += ' Edited \'' + this.groupName + '\' group';
		}

		// if description for new group wasn't passed from parent component,
		// make one up
		if (!this.newGroupDescription || this.newGroupDescription === null) {
			this.newGroupDescription = "Temporary experiment group created " + (new Date()).toString();
		}
	},
	
	login: function(){
		window.open("/Gemma/login.jsp");
		//var win = new Ext.Window({
		//items:[new Gemma.LoginPanel({})]});
		//win.show();
	},

	/**
	 * When user clicks done, just save to session
	 */
	done : function() {

		this.createDetails();
		this.saveToSession();
	},
		
	exportToTxt : function(){
		// make download link
		var downloadLink = String.format("/Gemma/expressionExperiment/downloadExpressionExperimentList.html?e={0}", this.getEEIds());
		window.open(downloadLink);
	},

	
	launchRegisterWidget : function(){
		if (this.ajaxRegister == null){
			
			//Check to see if another register widget is open (rare case but possible)
			var otherOpenRegister = Ext.getCmp('_ajaxRegister');				
				
			//if another register widget is open, fire its event to close it and destroy it before launching this one
			if (otherOpenRegister!=null){
				otherOpenRegister.fireEvent("register_cancelled");
			}	
			
			
			
			this.ajaxRegister = new Gemma.AjaxRegister({					
					name : 'ajaxRegister',									
					closable : false,
					//closeAction : 'hide',													
					title : 'Please Register'
				
					
				});			
			
			this.ajaxRegister.on("register_cancelled",function(){
				
				this.ajaxRegister.destroy();
				this.ajaxRegister = null;
				this.getEl().unmask();				
				
			},this);
			
			this.ajaxRegister.on("register_success",function(){
				
				this.ajaxRegister.destroy();
				this.ajaxRegister = null;
				this.getEl().unmask();				
				
			},this);
			
						
			}
		this.getEl().mask();	
		this.ajaxRegister.show();
	},
	
	
	/**
	 * When user clicks 'save', check if they are logged in or not, then in the callback, call saveAfterCheck
	 */
	saveBtnHandler : function() {
				
		Ext.Ajax.request({
         	url : '/Gemma/ajaxLoginCheck.html',
            method: 'GET',                  
            success: function ( response, options ) {			
					
                    var dataMsg = Ext.util.JSON.decode(response.responseText);                    
                    var link = Ext.getDom('footer-login-link');
                    var loggedInAs = Ext.getDom('footer-login-status');
                    var hasuser = Ext.getDom('hasUser');
                    
                    if (dataMsg.success){
						link.href="/Gemma/j_spring_security_logout";
						link.innerHTML="Logout"; 
						loggedInAs.innerHTML="Logged in as: "+dataMsg.user;
						hasuser.value= true;
						this.loggedInSaveHandler();
					}
                    else{
                    	link.href="/Gemma/login.jsp";
						link.innerHTML="Login";
						loggedInAs.innerHTML=" ";
						hasuser.value= "";
						this.promptLoginForSave();                      	
                    }
            },
            failure: function ( response, options ) {   
				this.promptLoginForSave();  
            },
            scope: this,
            disableCaching: true
       });
	},
	
	promptLoginForSave: function(){
		if (this.ajaxLogin == null) {
		
			//Check to see if another login widget is open (rare case but possible)
			var otherOpenLogin = Ext.getCmp('_ajaxLogin');
			
			//if another login widget is open, fire its event to close it and destroy it before launching this one
			if (otherOpenLogin != null) {
				otherOpenLogin.fireEvent("login_cancelled");
			}
			
			
			this.ajaxLogin = new Gemma.AjaxLogin({
				name: 'ajaxLogin',
				closable: false,
				//closeAction : 'hide',													
				title: 'Please login to use this function'
			
			
			});
			
			
			this.ajaxLogin.on("login_success", function(){
				this.getEl().unmask();
				this.ajaxLogin.destroy();
				this.ajaxLogin = null;
				this.saveBtnHandler();
				
				
			}, this);
			
			this.ajaxLogin.on("register_requested", function(){
			
				this.getEl().unmask();
				this.ajaxLogin.destroy();
				this.ajaxLogin = null;
				this.launchRegisterWidget();
				
				
			}, this);
			
			this.ajaxLogin.on("login_cancelled", function(){
			
				this.ajaxLogin.destroy();
				this.ajaxLogin = null;
				this.getEl().unmask();
				
			}, this);
			
			}
			this.getEl().mask();
			this.ajaxLogin.show();
		
		
	},
	loggedInSaveHandler : function () {
		
		// get name and description set up
		this.createDetails();
		
		// check if user is editing a non-existant or session-bound group
		
		// check if group is db-backed and whether current user has editing priveleges
		if(this.selectedExperimentGroup && this.selectedExperimentGroup.reference){
			
			// if group is db-bound and user has editing privileges, they can either save or save as
			// in all other cases, user can only save as
			ExpressionExperimentSetController.canCurrentUserEditGroup(this.selectedExperimentGroup.reference, function(response){
				var dataMsg = Ext.util.JSON.decode(response);
				if(dataMsg.userCanEditGroup && dataMsg.groupIsDBBacked){
					// ask user if they want to save changes
					Ext.Msg.show({
								title : 'Save Changes?',
								msg : 'You have edited an <b>existing group</b>, '+
										'how would you like to save your changes?<br>',
								buttons : {
									ok : 'Save over',
									yes : 'Save as...',
									no : 'Cancel'
								},
								fn : function(btnId){
									if(btnId === 'ok'){
										this.saveHandler();
									}else if(btnId === 'yes'){
										this.saveAsHandler();
									}else if(btnId === 'no'){
										// just close the prompt
									}
								},
								scope:this,
								icon : Ext.MessageBox.QUESTION
							});
				}else{
					this.saveAsHandler();
				}
			}.createDelegate(this));
			
		}else{
			// if reference is null, then there was no group to start with
			// only save option is to save as
			this.saveAsHandler();
		}
	},
	saveAsHandler: function(){
		// input window for creation of new groups
		var detailsWin = new Gemma.GeneSetDetailsDialog({
			title: 'Provide or edit experiment group details'
		});
		detailsWin.on("hide", function(args){
			this.close();
		});
		detailsWin.on("commit", function(args){
			this.newGroupName = args.name;
			this.newGroupDescription = args.description;
			this.createInDatabase();
		}, this);
		
		detailsWin.name = this.groupName;
		detailsWin.description = 'Edited search results for: "' + this.groupName + '". Created: ' +
		(new Date()).toString();
		
		detailsWin.show();
	},
	saveHandler: function(){
		this.updateDatabase();
	},
	saveToSession : function() {
		var editedGroup;
		if (this.selectedGeneGroup === null || typeof this.selectedGeneGroup === 'undefined') {
			//group wasn't made before launching 
			editedGroup = {
				reference: {
					id: null,
					type: null
				}
			};
		}
		else {
			editedGroup = this.selectedGeneGroup;
		// reference has the right type already
		}
			
		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.expressionExperimentIds = this.getEEIds();
		editedGroup.memberIds = this.getEEIds();
		editedGroup.taxonId = this.taxonId;
		editedGroup.type = 'userexperimentSetSession';

		ExpressionExperimentSetController.addSessionGroups(
				[editedGroup], // returns datasets added
				function(datasetSets) {
					// should be at least one datasetSet
					if (datasetSets === null || datasetSets.length === 0) {
						// TODO error message
						return;
					} else {
						datasetSets[0].type = 'userexperimentSetSession';
						var newRecordData = datasetSets;
						this.fireEvent('experimentListModified', newRecordData);
						this.fireEvent('doneModification');
					}
				}.createDelegate(this));

	},
	createInDatabase: function(){
		var editedGroup;
		if (this.selectedGeneGroup === null || typeof this.selectedGeneGroup === 'undefined') {
			//group wasn't made before launching 
			editedGroup = {
				reference: {
					id: null,
					type: null
				}
			};
		}
		else {
			editedGroup = this.selectedGeneGroup;
		// reference has the right type already
		}
		
		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.expressionExperimentIds = this.getEEIds();
		editedGroup.memberIds = this.getEEIds();
		editedGroup.type = 'userexperimentSet';
		
		ExpressionExperimentSetController.create([editedGroup], // returns datasets added
 			function(datasetSets){
				// should be at least one datasetSet
				if (datasetSets === null || datasetSets.length === 0) {
					// TODO error message
					return;
				}
				else {
					datasetSets[0].type = 'userexperimentSet';
					var newRecordData = datasetSets;
					this.fireEvent('experimentListModified', newRecordData);
					this.fireEvent('doneModification');
				}
			}.createDelegate(this));
		
		
		this.fireEvent('doneModification');
		
	},
	updateDatabase : function() {
		var groupId = this.selectedExperimentGroup.reference.id;
		this.newGroupName = this.groupName;
		var eeIds = this.getEEIds();

		ExpressionExperimentSetController.updateMembers(groupId, eeIds, function(msg) {
					this.selectedExperimentGroup.memberIds = eeIds;
					this.selectedExperimentGroup.eeIds = eeIds;
					this.selectedExperimentGroup.expressionExperimentIds = eeIds;

					this.fireEvent('experimentListModified', [this.selectedExperimentGroup]);
					this.fireEvent('doneModification');
				}.createDelegate(this));
	}
});

/**
 * toolbar for selecting experiments or experiment groups and adding them to a grid
 * if eeCombo.taxonId is set, then searches will be limited by taxon  
 */
Gemma.ExperimentAndGroupAdderToolbar = Ext.extend(Ext.Toolbar,{

	initComponent: function(){
	
		Gemma.ExperimentAndGroupAdderToolbar.superclass.initComponent.call(this);
		
		this.eeCombo = new Gemma.ExperimentAndExperimentGroupCombo({
			typeAhead: false,
			width: 300,
			emptyText: 'Search for an experiment or group to add',
			listeners: {
				'select': {
					fn: function(combo, rec, index){
						this.addButton.enable();
						if (rec.data.size === 1) {
							this.addButton.setText('Add 1 experiment');
						}
						else {
							this.addButton.setText('Add ' + rec.data.size + ' experiments');
						}
						
					}.createDelegate(this)
				}
			}
		});
		
		this.addButton = new Ext.Toolbar.Button({
			icon: "/Gemma/images/icons/add.png",
			cls: "x-btn-text-icon",
			tooltip: "Add selected experiment(s) to the list",
			text: 'Add',
			disabled: true,
			handler: function(){
				this.eeGrid.addExperiments(this.eeCombo.getExpressionExperimentGroup());
				this.eeCombo.reset();
				this.addButton.setText('Add');
				this.addButton.disable();
			}.createDelegate(this)
		});
		
	},
	afterRender: function(c, l){
		Gemma.ExperimentAndGroupAdderToolbar.superclass.afterRender.call(this, c, l);
		
		this.add(this.eeCombo, this.addButton);
		
	}
});
