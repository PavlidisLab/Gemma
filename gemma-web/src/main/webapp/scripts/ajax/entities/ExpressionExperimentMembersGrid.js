
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
			if (btn === 'no') { // no is don't save
				this.saveToSession();
			} else if (btn === 'ok') { // ok is save
				this.updateDatabase();
			} else if (btn === 'yes') { // yes is save as
			
				// input window for creation of new groups
				var detailsWin = new Gemma.GeneSetDetailsDialog({
					id: 'experimentDetailsWin',
					hidden: true,
					title: 'Provide or edit experiment group details'
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
			handler: this.save,
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
		})
		this.exportButton = new Ext.Button({
			text: "Export",
			qtip: 'Get a plain text version of this list',
			handler: this.exportToTxt,
			scope: this,
			disabled: false
		});
		this._noSavePanel = new Ext.Panel({
			layout: 'hbox',
			width: 250,
			border: false,
			bodyBorder: false,
			bodyStyle: 'background: transparent',
			padding: 0,
			defaults: {
				border: false,
				bodyBorder: false,
				bodyStyle: 'background: transparent',
				padding: 0
			},
			items: [{
				html: 'You must be ',
				style: 'padding-top:3px'
			}, new Ext.LinkButton({
				text: 'logged in',
				handler: this.login,
				scope: this
			}), {
				html: 'to save this selection.',
				style: 'padding-top:3px'
			}, {
				xtype: 'button',
				icon: '/Gemma/images/icons/arrow_refresh_small.png',
				qtip: 'Refresh your login state',
				handler: function(){
					// TODO re-evaluate what buttons they can see
					this.setButtonVisibilities();
					//console.log("logged in :"+Ext.get('hasUser').getValue());
					this.doLayout();
				},
				scope: this
			}],
		});
		Ext.apply(this, {
		
			setButtonVisibilities: function(){
				if (Ext.get('hasUser').getValue()) {
					this.saveButton.show();
				}
				else {
					this.saveButton.hide();
				}
				if (this.allowSaveToSession) {
					this.doneButton.show();
				}
				else {
					this.doneButton.hide();
				}
				if (!Ext.get('hasUser').getValue() && !this.allowSaveToSession) {
					this._noSavePanel.show();
				}
				else {
					this._noSavePanel.hide();
				}
			},
			buttons: [this.saveButton, this.doneButton, this._noSavePanel, this.exportButton, {
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

	/**
	 * When user clicks 'save', figure out what kind of save to do
	 */
	save : function() {

		// if the user hasn't made any changes, save anyway

		// get name and description set up
		this.createDetails();

		// save button should only be visible if user is logged in, but just to
		// be safe:
		if (!Ext.get('hasUser').getValue()) {
			Ext.Msg.alert("Not logged in", "You cannot save this list because you are not logged in, "+
							" however, your list will be available temporarily.");
			this.saveToSession();
		} else {

			// if geneGroupId is null, then there was no group to start with
			// if user has made any changes, a new gene set will be created
			if (!this.selectedExperimentGroup ||this.selectedExperimentGroup === null || 
					this.selectedExperimentGroup.reference === null || this.selectedExperimentGroup.reference.id === null) {
				// ask user if they want to save changes
				this.editedExistingGroup('yes'); // yes means 'save as'

			} else {// if this is an edit of an existing gene group, give
				// options to create or edit

				// if group being edited is session-bound, only offer to save to
				// database
				if (this.selectedExperimentGroup !== null
						&& (this.selectedExperimentGroup.type.indexOf('Session') >= 0 || this.selectedExperimentGroup.type
								.indexOf('session') >= 0)) {

					this.editedExistingGroup('yes'); // yes means 'save as'
				}

				// if group of genes being edited belongs to the user, ask if
				// they want to save changes
				else if (this.selectedExperimentGroup !== null
						&& (this.selectedExperimentGroup.type.indexOf('user') >= 0 || this.selectedExperimentGroup.type
								.indexOf('User') >= 0)) {
					// ask user if they want to save changes
					Ext.Msg.show({
								title : 'Save Changes?',
								msg : 'You have edited an existing group, '+
										 'would you like to save your changes?<br>'+
										 '(Unsaved lists are available until you log out.)',
								buttons : {
									ok : 'Save',
									yes : 'Save As...',
									no : 'Don\'t save'
								},
								// buttons: {yes:'Save As...', no:'Don\'t
								// save'},
								fn : this.editedExistingGroup,
								icon : Ext.MessageBox.QUESTION
							});
				} else {
					this.editedExistingGroup('yes'); // yes means 'save as'
				}
			}
		}

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
		editedGroup.type = 'userexperimentSetSession';

		ExpressionExperimentSetController.addSessionGroups(
				[editedGroup], // returns datasets added
				function(datasetSets) {
					// should be at least one datasetSet
					if (datasetSets === null || datasetSets.length === 0) {
						// TODO error message
						return;
					} else {
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
					var newRecordData = datasetSets;
					datasetSets[0].type = 'userexperimentSet';
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
