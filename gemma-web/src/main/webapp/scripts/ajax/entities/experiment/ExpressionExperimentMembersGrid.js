
Ext.namespace('Gemma');

/**
 * Grid to display ExpressionExperiment group members and allow the user to remove and add members.
 * 
 * Define selectedExpressionExperimentValueObject in config or with setSelectedExpressionExperimentValueObject(eesvo) to display an experiment group.
 * Use loadExperimentSet(eesvo) to display an experiment group after initialisation. 
 */
Gemma.ExpressionExperimentMembersGrid = Ext.extend(Ext.grid.GridPanel, {

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
	viewConfig : {
		forceFit : true
	},
	queryText : '',
	addExperiments: true,
	taxonId: null,
	allowSaveToSession:true, // controls action of 'ok' button (if false, will act as cancel)
	allowAdditions:true, // controls presence of top toolbar
	allowRemovals:true, // controls presence of 'remove experiment' buttons on every row
	sortableColumnsView:false, // controls whether the data appears in two columns or formatted into one
	hideOkCancel:false,
	showSeparateSaveAs: false, // show a 'save as' button in addition to a save button
	enableSaveOnlyAfterModification: false, // if save button is show, leave it disabled until an experiment is added or removed
	/**
	 * Set the expressionExperimentSetValueObject.
	 * (does not display the eesvo, use loadExperimentSet(eesvo) to do that)
	 * @param {Object} eesvo
	 */
	setSelectedExpressionExperimentValueObject: function(eesvo){
		this.selectedExpressionExperimentValueObject = eesvo;
	},
	/**
	 * same as getSelectedExperimentSetValueObject()
	 */
	getSelectedExpressionExperimentSetValueObject: function(){
		return this.selectedExpressionExperimentValueObject;
	},
	/**
	 * shorter name for getSelectedExpressionExperimentSetValueObject()
	 */
	getSelectedExperimentSet: function(){
		return this.getSelectedExpressionExperimentSetValueObject();
	},
	
	// used as 'interface' with geneMembersGrid
	loadSetValueObject : function(eesvo, callback, args) {
		this.loadExperimentSetValueObject(eesvo, callback, args);
	},
	/**
	 * Add to table.
	 * 
	 * @param {}
	 *            expressionExperimentSetValueObject
	 * @param {}
	 *            callback optional
	 * @param {}
	 *            args optional
	 */
	loadExperimentSetValueObject : function(eesvo, callback, args) {
		
		// update title
		this.setTitle("Edit your experiment selection, from group: \""+eesvo.name+"\"");
		// update this.selectedGeneSetValueObject
		this.setSelectedExpressionExperimentValueObject(eesvo);
		// update genes in grid
		this.loadExperiments(eesvo.expressionExperimentIds, callback, args);

	},
	// used as 'interface' with geneMembersGrid
	loadEntities : function(eeIds, callback, args) {
		this.loadExperiments(eeIds, callback, args);
	},
	/**
	 * Add to table.
	 * 
	 * @param {}
	 *            eeIds collection of experiment ids for experiments to display
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
			
			var taxonId = (ees[0]) ? ees[0].taxonId : -1;
			
			for (i = 0; i < ees.length; ++i) {
				eeData.push([ees[i].id, ees[i].shortName, ees[i].name, ees[i].arrayDesignCount, ees[i].bioAssayCount]);
				if (taxonId != ees[i].taxonId) {
					var taxonId = -1;
				}
			}
			if (taxonId != -1) {
				this.setTaxonId(taxonId);
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
	/**
	 * @param {Object} data eeSetValueObject
	 */
	addExperiments : function(eeSearchResult) {
				if (!eeSearchResult) {
					return;
				}

				var id = eeSearchResult.id;
				var name = eeSearchResult.name;
						
				var eeIdsToAdd = [];
				eeIdsToAdd = eeSearchResult.memberIds;
				
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

				}.createDelegate(this));
				
				
			},

	/*
	 * set the taxon for this grid and for the toolbar to control what can be added from combo
	 */
	setTaxonId: function(taxonId){
		this.taxonId = taxonId;
		if (this.getTopToolbar()) {
			Ext.apply(this.getTopToolbar().eeCombo, {
				taxonId: taxonId
			});
		}
		
	},

	initComponent : function() {
		
		var extraButtons = [];
		if(this.allowRemovals){
			var removeSelectedBtn = new Ext.Button({
				text: 'Remove Selected',
				icon: "/Gemma/images/icons/cross.png",
				hidden: true,
				handler: function(){
					var records = this.getSelectionModel().getSelections();
					this.getStore().remove(records);
					removeSelectedBtn.setVisible(false);
				},
				scope:this
			});
			extraButtons = [removeSelectedBtn];
			
			this.getSelectionModel().on('rowselect', function(selModel){
				removeSelectedBtn.setVisible(selModel.getCount() > 1);
			}, this);

		}
		
		if(this.allowAdditions){
			Ext.apply(this, {
				tbar: new Gemma.ExperimentAndGroupAdderToolbar({
					extraButtons: extraButtons,
					ref: 'eeAdderTBar',
					eeGrid : this
				})
			});
		}
		var columns = [];
		if(this.sortableColumnsView){
			Ext.apply(this, {
				hideHeaders:false
			});
			columns.push({
				header: "Short Name",
				dataIndex: "shortName",
				renderer: function(value, metadata, record, row, col, ds){
					return String.format("<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={0}'>{1}</a>" ,
						record.data.id, record.data.shortName);
				},
				sortable: true,
				width:40
			});
			columns.push({
				id:'name', // needed for autoExpand config
				header: "Name",
				dataIndex: "name",
				sortable: true,
				width:150
			});
		}else{
			columns.push({
				id: 'shortName',
				header: "Dataset",
				dataIndex: "shortName",
				renderer: function(value, metadata, record, row, col, ds){
					return String.format("<a target='_blank' href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={0}'>{1}</a>" +
					"<br><span style='font-color:grey;white-space:normal !important;'>{2}</span> ", record.data.id, record.data.shortName, record.data.name);
				},
				sortable: true
			});
		}
		
		if (this.allowRemovals) {
			// Create RowActions Plugin
			this.action = new Ext.ux.grid.RowActions({
				header: 'Remove',
				keepSelection: true,
				tooltip: 'Remove experiment',
				actions: [{
					iconCls: 'icon-cross',
					tooltip: 'Remove experiment'
				}],
				callbacks: {
					'icon-cross': function(grid, record, action, row, col){
					}
				}
			});
			
			this.action.on({
				action: function(grid, record, action, row, col){
					if (action === 'icon-cross') {
						this.changeMade = true;
						grid.getStore().remove(record);
					}
				},
				// You can cancel the action by returning false from this
				// event handler.
				beforeaction: function(grid, record, action, row, col){
					if (grid.getStore().getCount() == 1 && action === 'icon-cross') {
						return false;
					}
					return true;
				}
			});
			columns.push(this.action);
			Ext.apply(this, {
				plugins:[this.action]
			});
		}
		Ext.apply(this, {
			colModel: new Ext.grid.ColumnModel({
				defaults: {
					sortable: true
				},
				columns: columns
			})
		});

		this.saveAsButton = new Ext.Button({
			text: "Save As",
			handler: this.saveAsBtnHandler,
			qtip: 'Save your selection as a new set.',
			scope: this,
			disabled: !this.showSeparateSaveAs,
			hidden: !this.showSeparateSaveAs
		});
		this.saveButton = new Ext.Button({
			text: "Save...",
			handler: this.saveBtnHandler,
			qtip: 'Save your selection before returning to search.',
			scope: this,
			disabled: this.enableSaveOnlyAfterModification // defaults to false
		});
		this.okButton = new Ext.Button({
			text: "OK",
			handler: this.okHandler,
			scope: this,
			disabled: (!this.allowSaveToSession || this.hideOkCancel),
			hidden: (!this.allowSaveToSession || this.hideOkCancel)
		});	
		this.cancelButton = new Ext.Button({
			text: "Cancel",
			handler: this.cancel,
			scope: this,
			hidden: (this.allowSaveToSession || this.hideOkCancel),
			disabled: (this.allowSaveToSession || this.hideOkCancel)
		});
		this.exportButton = new Ext.Button({
			icon: "/Gemma/images/download.gif",
			tooltip: "Export to text",
			handler: this.exportToTxt,
			scope: this,
			disabled: false
		});	
	
		Ext.apply(this, {
			buttonAlign: 'left',
			buttons: [this.exportButton, '->',this.saveButton, this.saveAsButton, this.okButton, this.cancelButton]
		});
		this.saveButton.show();		

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
					})
		});

		
		this.ajaxLogin = null;
		this.ajaxRegister = null;
		
		Gemma.ExpressionExperimentMembersGrid.superclass.initComponent.call(this);

		this.on('doneModification', function() {
			this.changesMade = false;
		});

		this.getStore().on("remove", function(){
			this.changesMade = true;
			this.saveButton.enable();
		}, this);

		this.getStore().on("add", function(){
			this.changesMade = true;
			this.saveButton.enable();
		}, this);

		this.getStore().on("load", function(store, records, options) {
					this.doLayout.createDelegate(this);
				}, this);

		if(this.selectedExpressionExperimentSetValueObject){
			this.loadExperimentSet(this.selectedExpressionExperimentSetValueObject);
		} else if (this.eeids) {
			this.loadExperiments(this.eeids);
		}
		
				
		this.on('experimentsLoaded',function(){
			if (this.getSelectedExperimentSet()) {

				ExpressionExperimentSetController.canCurrentUserEditGroup(this.getSelectedExperimentSet(), function(response){
					var dataMsg = Ext.util.JSON.decode(response);
					if (!dataMsg.userCanEditGroup || !dataMsg.groupIsDBBacked) {
						
						// don't show two save as buttons
						if(this.showSeparateSaveAs){
							this.saveButton.hide().disable();
						}else{
							this.saveButton.setText("Save As");
						}
					}else{
						this.saveButton.setText("Save...");
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
		if ( !this.getSelectedExperimentSet() && (!this.groupName || this.groupName === null || this.groupName === '')) {
			this.newGroupName = "Experiment group created: " + (new Date()).toString();
		} else {
			var groupName = (this.getSelectedExperimentSet() && this.getSelectedExperimentSet().name)? this.getSelectedExperimentSet().name: this.groupName;
			
			// adding time to end of session-bound group titles in case it's not
			// unique
						var currentTime = new Date();
			var hours = currentTime.getHours();
			var minutes = currentTime.getMinutes();
			if (minutes < 10) {
				minutes = "0" + minutes;
			}
			var time = '(' + hours + ':' + minutes + ') ';
			
			// Use simple, non descriptive naming for now
			this.newGroupName  = time + "Custom Experiment Group";
			
			//var matches = groupName.match(/^\(\d+:\d+\)\s*/);
			//if ( matches !== null) {
			//	groupName = groupName.substring(matches[0].length, groupName.length);
			//	if (groupName.substring(0, prefix.length) === prefix &&
			//			groupName.substring(groupName.length - suffix.length, groupName.length) === suffix) {
			//		groupName = groupName.substring(prefix.length, groupName.length - suffix.length);
			//	}
			//}
			//this.newGroupName = time + ' Edited \'' + groupName + '\' group';
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
	okHandler: function(){
		// if user has made changes, save to session
		// if user hasn't made changes, just close
		if(this.changesMade && this.allowSaveToSession){
			this.prepareAndSaveToSession();
		}else{
			this.cancel();
		}
	},

	/**
	 * populate set details and save to session
	 */
	prepareAndSaveToSession : function() {
				
		// check if user is trying to save an empty set
		if(this.getStore().getRange() && this.getStore().getRange().length === 0){
			Ext.Msg.alert('Cannot use empty set', 'You are trying to use an empty set. '+
				'Please add some experiments and try again.');
			return;
		}
		
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
		
		SignupController.loginCheck(
	             {
	                callback: function(result){
	                	if (result.loggedIn){
	                		this.loggedInSaveHandler();	                		
	                	}
	                	else{
	                		this.promptLoginForSave('save'); 
	                	}
	                }.createDelegate(this)
	            });		
		
	},
	/**
	 * When user clicks 'save as', check if they are logged in or not, then in the callback, call saveAsHandler
	 */
	saveAsBtnHandler : function() {
		
		
		SignupController.loginCheck(
	             {
	                callback: function(result){
	                	if (result.loggedIn){
	                		this.createDetails();
							this.saveAsHandler();                		
	                	}
	                	else{
	                		this.promptLoginForSave('saveAs'); 
	                	}
	                }.createDelegate(this)
	            });
		
	},
	
	promptLoginForSave: function(save){
	
		//Check to see if another login widget is open (rare case but possible)
		var otherOpenLogin = Ext.getCmp('_ajaxLogin');
		
		//if another login widget is open, fire its event to close it and destroy it before launching this one
		if (otherOpenLogin != null) {
			otherOpenLogin.fireEvent("login_cancelled");
		}
		
		Gemma.AjaxLogin.showLoginWindowFn();
		
		Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
			Ext.getBody().unmask();
			if(save === 'save'){
				this.saveBtnHandler();
			}else{
				this.saveAsHandler();
			}
			
		}, this);
		
	},
	loggedInSaveHandler : function () {
				
		// check if user is trying to save an empty set
		if(this.getStore().getRange() && this.getStore().getRange().length === 0){
			Ext.Msg.alert('Cannot save empty set', 'You are trying to save an empty set. '+
				'Please add some experiments and try again.');
			return;
		}
		
		// get name and description set up
		this.createDetails();

		// check if user is editing a non-existent or session-bound group
		
		// check if group is db-backed and whether current user has editing priveleges
		if(this.getSelectedExperimentSet() ){
			
			// if group is db-bound and user has editing privileges, they can either save or save as
			// in all other cases, user can only save as
			ExpressionExperimentSetController.canCurrentUserEditGroup(this.getSelectedExperimentSet(), function(response){
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
			// only save option is to save as
			this.saveAsHandler();
		}
	},
	saveAsHandler: function(){
		// input window for creation of new groups
		var detailsWin = new Gemma.CreateSetDetailsWindow({
			title: 'Provide or edit experiment group details'
		});
		detailsWin.lockInTaxonId(this.taxonId, true);
		detailsWin.on("hide", function(args){
			this.close();
		});
		detailsWin.on("commit", function(args){
			this.newGroupName = args.name;
			this.newGroupDescription = args.description;
			this.newGroupPublik = args.publik;
			this.newGroupTaxon = args.taxon;
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
		editedGroup = new SessionBoundExpressionExperimentSetValueObject();
		editedGroup.id = null;	
		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.expressionExperimentIds = this.getEEIds();
		editedGroup.taxonId = this.taxonId;
		editedGroup.numExperiments = this.getEEIds().length;
		editedGroup.modified = true;
		editedGroup.publik = false;
		

		ExpressionExperimentSetController.addSessionGroups(
				[editedGroup], true, // returns datasets added
				function(newValueObjects) {
					// should be at least one datasetSet
					if (newValueObjects === null || newValueObjects.length === 0) {
						// TODO error message
						return;
					} else {
						this.fireEvent('experimentListModified', newValueObjects);
						this.fireEvent('doneModification');
					}
				}.createDelegate(this));

	},
	createInDatabase: function(){
		var editedGroup;
		if (this.getSelectedExperimentSet() === null || typeof this.getSelectedExperimentSet() === 'undefined' || 
				!(this.getSelectedExperimentSet() instanceof DatabaseBackedExpressionExperimentSetValueObject)) {
			//group wasn't made before launching 
			editedGroup = new DatabaseBackedExpressionExperimentSetValueObject();
		}
		else {
			editedGroup = Object.clone(this.getSelectedExperimentSet());
		}
		
		editedGroup.id = null;	
		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.expressionExperimentIds = this.getEEIds();
		editedGroup.numExperiments = this.getEEIds().length;
		editedGroup.publik = this.newGroupPublik;
		editedGroup.taxonId = (this.newGroupTaxon)? this.newGroupTaxon.id : this.taxonId;
		
		ExpressionExperimentSetController.create([editedGroup], // returns datasets added
 			function(newValueObjects){
				// should be at least one datasetSet
				if (newValueObjects === null || newValueObjects.length === 0) {
					// TODO error message
					return;
				}
				else {
					this.fireEvent('experimentListModified', newValueObjects);
					this.fireEvent('experimentListCreated', newValueObjects[0]);
					this.fireEvent('doneModification');
				}
			}.createDelegate(this));
		
		
		this.fireEvent('doneModification');
		
	},
	updateDatabase : function() {
		var id = this.getSelectedExperimentSet().id;
		var eeIds = this.getEEIds();

		ExpressionExperimentSetController.updateMembers(id, eeIds, function(msg) {
					this.getSelectedExperimentSet().expressionExperimentIds = eeIds;

					this.fireEvent('experimentListModified', [this.getSelectedExperimentSet()]);
					this.fireEvent('experimentListSavedOver');
					this.fireEvent('doneModification');
				}.createDelegate(this));
	}
});

/**
 * toolbar for selecting experiments or experiment groups and adding them to a grid
 * if eeCombo.taxonId is set, then searches will be limited by taxon  
 */
Gemma.ExperimentAndGroupAdderToolbar = Ext.extend(Ext.Toolbar,{

	extraButtons: [],
	initComponent: function(){
	
		Gemma.ExperimentAndGroupAdderToolbar.superclass.initComponent.call(this);
		
		this.eeCombo = new Gemma.ExperimentAndExperimentGroupCombo({
			typeAhead: false,
			width: 300,
			emptyText: 'Search for an experiment or group to add',
			listeners: {
				'select': {
					fn: function(combo, rec, index){
						this.addBtn.enable();
						if (rec.data.size === 1) {
							this.addBtn.setText('Add 1 experiment');
						}
						else {
							this.addBtn.setText('Add ' + rec.data.size + ' experiments');
						}
						
					}.createDelegate(this)
				}
			}
		});
		
		this.addBtn = new Ext.Toolbar.Button({
			icon: "/Gemma/images/icons/add.png",
			cls: "x-btn-text-icon",
			tooltip: "Add selected experiment(s) to the list",
			text: 'Add',
			disabled: true,
			handler: function(){
				this.eeGrid.addExperiments(this.eeCombo.getExpressionExperimentGroup());
				this.eeCombo.reset();
				this.addBtn.setText('Add');
				this.addBtn.disable();
			}.createDelegate(this)
		});
		
	},
	afterRender: function(c, l){
		Gemma.ExperimentAndGroupAdderToolbar.superclass.afterRender.call(this, c, l);
		this.add(this.eeCombo, this.addBtn);
		
		this.addButton(this.extraButtons);
		
	}
});
