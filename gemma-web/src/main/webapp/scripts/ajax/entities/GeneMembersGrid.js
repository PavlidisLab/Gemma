
/*
 * Widget for displaying a list of genes, with cofigurable column sets.
 * 
 * Version : $Id$
 * Author : luke, paul
 */
Ext.namespace('Gemma');

/**
 * The maximum number of genes we allow users to put in at once.
 * 
 * @type Number
 */
Gemma.MAX_GENES_PER_QUERY = 1000;

/**
 * Table of genes with toolbar for searching.
 * 
 * Adjust columns displayed using "columnSet" config (values can be "reduced"
 * (default) or "full") if "full": symbol, description, species and 'in list'
 * boolean are shown if "reduced" (or any other value): only symbol and
 * description are shown
 * 
 * 
 * @class GeneGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.GeneMembersGrid = Ext.extend(Ext.grid.GridPanel, {

	collapsible : false,
	autoWidth : true,
	stateful : false,
	frame : true,
	layout : 'fit',
	width : 400,
	height : 250,
	stripeRows : true,
	changeMade : false,
	// bubbleEvents: ['geneListModified'],
	geneGroupId : null,
	loggedId : null,
	/*
	 * columnSet can be "reduced" or "full", if "reduced": only symbol and
	 * description are shown if "full": symbol, description, species and 'in
	 * list' boolean are shown
	 */
	columnSet : "reduced",
	allowSaveToSession: true, // if false, user can only save to db

	viewConfig : {
		forceFit : true,
		emptyText : "Multiple genes can be listed here"
	},
	autoScroll : true,

	autoExpandColumn : 'desc',

	showRemoveColumn : function() {
		// if config is set for "full" column model, show more columns
		this.getColumnModel().setHidden(this.getColumnModel().getIndexById("remove"), false);
	},

	getFullColumnModel : function() {
		// if config is set for "full" column model, show more columns
		this.getColumnModel().setHidden(this.getColumnModel().getIndexById("taxon"), false);
		this.getColumnModel().setHidden(this.getColumnModel().getIndexById("inList"), false);
	},
	/**
	 * Add to table.
	 * 
	 * @param {}
	 *            geneIds
	 * @param {}
	 *            callback optional
	 * @param {}
	 *            args optional
	 */
	loadGenes : function(geneIds, callback, args) {
		if (!geneIds || geneIds.length === 0) {
			return;
		}

		GenePickerController.getGenes(geneIds, function(genes) {
					var geneData = [];
					var i = 0;
					for (i = 0; i < genes.length; i++) {
						geneData.push([genes[i].id, genes[i].taxonScientificName, genes[i].officialSymbol,
								genes[i].officialName]);
					}
					/*
					 * FIXME this can result in the same gene listed twice. This
					 * is taken care of at the server side but looks funny.
					 */
					this.getStore().loadData(geneData);
					if (callback) {
						callback(args);
					}
				}.createDelegate(this));
	},

	// input window for creation of new groups
	detailsWin : new Gemma.GeneSetDetailsDialog({
				id : 'geneDetailsWin',
				hidden : true
			}),
	addGenes : function(data) { // for adding from combo
				if (!data) {
					return;
				}
				this.selectedGeneOrGroupRecord = data;

				var id = data.reference.id;
				var isGroup = data.isGroup;
				var reference = data.reference;
				var name = data.name;
		
				var taxonId = data.taxonId;
				var taxonName = data.taxonName;
						
				var geneIdsToAdd = [];
				// load preview of group if group was selected
				if (isGroup) {
					geneIdsToAdd = data.memberIds;
				}else{
					geneIdsToAdd = [id];
				}
				if (!geneIdsToAdd || geneIdsToAdd === null || geneIdsToAdd.length === 0) {
					return;
				}
				
				GenePickerController.getGenes(geneIdsToAdd, function(genes) {

					for (var j = 0; j < genes.size(); j++) {
						if (this.getStore().find("id", genes[j].id) < 0) {
							var Constructor = this.store.recordType;
							var record = new Constructor(genes[j]);
							this.getStore().add([record]);
						}
					}
					/* maybe should notify user with text at bottom that 'x experiments have been added'
					this.experimentPreviewContent.setTitle(
						'<span style="font-size:1.2em">'+this.experimentCombo.getRawValue()+'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' + ids.size() + " experiments)");
					this.experimentSelectionEditorBtn.setText('<a>' + (ids.size() - limit) + ' more - Edit</a>');
					*/

				}.createDelegate(this));
				
				
			},
	/*
	 * set the taxon for this grid and for the toolbar to control what can be added from combo
	 */
	setTaxonId: function(taxonId){
		this.taxonId = taxonId;
		Ext.apply(this.getTopToolbar().geneCombo, {
			taxonId: taxonId
		});
	},
	initComponent : function() {
		Ext.apply(this, {
			tbar: new Gemma.GeneAndGroupAdderToolbar({
				geneGrid : this
			})
		});
		// Create RowActions Plugin
		this.action = new Ext.ux.grid.RowActions({
					header : 'Actions',
					// ,autoWidth:false
					// ,hideMode:'display'
					keepSelection : true,
					actions : [{
								iconCls : 'icon-cross',
								tooltip : 'Remove gene'
							}],
					callbacks : {
						'icon-cross' : function(grid, record, action, row, col) {
						}
					}
				});

		// dummy action event handler - just outputs some arguments to console
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

		this.detailsWin.on("commit", function(args) {
					this.newGroupName = args.name;
					this.newGroupDescription = args.description;
					this.createInDatabase();
				}.createDelegate(this));

		// function to deal with user choice of what to do after editing an
		// existing group
		this.editedExistingGroup = function(btn) {
			if (btn === 'no') { // no is don't save
				this.createInSession();
			} else if (btn === 'ok') { // ok is save
				this.updateDatabase();
			} else if (btn === 'yes') { // yes is save as
				this.detailsWin.name = '';
				this.detailsWin.description = '';
				this.detailsWin.show();
			} else {
				return;
			}
		}.createDelegate(this);
		this.saveButton = new Ext.Button({
					// id: 'save-selection-button-g',
					text : "Save",
					handler : this.save,
					scope : this,
					disabled : false
				});
		this.doneButton = new Ext.Button({
					// id: 'done-selecting-button-g',
					text : "Done",
					handler : this.done,
					scope : this,
					disabled : true
				})
		this.exportButton = new Ext.Button({
					// id: 'done-selecting-button-g',
					text : "Export",
					handler : this.exportToTxt,
					scope : this,
					disabled : false
				});
		// add save button if user isn't logged in
		
		if (Ext.get('hasUser').getValue() && this.allowSaveToSession) {
			Ext.apply(this, {
						buttons : [this.saveButton, this.exportButton, this.doneButton, {
									text : "Cancel",
									handler : this.cancel,
									scope : this
								}]
					});
		}else if (Ext.get('hasUser').getValue()) {
			Ext.apply(this, {
						buttons : [this.saveButton,this.exportButton,  {
									text : "Cancel",
									handler : this.cancel,
									scope : this
								}]
					});
		}else if( this.allowSaveToSession ) {
			Ext.apply(this, {
						buttons : [this.doneButton, this.exportButton, {
									text : "Cancel",
									handler : this.cancel,
									scope : this
								}]
					});
		}else{
			Ext.apply(this, {
						buttons : [this.exportButton, {
									xtype:'panel',
									html:'Sorry, you must be logged in to save this selection.'}, {
									text : "Cancel",
									handler : this.cancel,
									scope : this
								}]
					});
		}

		Ext.apply(this, {
			store : new Ext.data.SimpleStore({
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
								}, {
									name : 'inList',
									type : 'boolean',
									defaultValue : true
								}],
						sortInfo : {
							field : 'officialSymbol',
							direction : 'ASC'
						}
					}),
			colModel : new Ext.grid.ColumnModel({
				defaults : {
					sortable : true
				},
				columns : [{
					header : 'Symbol',
					toolTip : 'Gene symbol',
					dataIndex : 'officialSymbol',
					width : 75,
					renderer : function(value, metadata, record, row, col, ds) {
						return String
								.format(
										"<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a><br>"+
										"<span style='font-color:grey; white-space:normal !important;'>{2}</span> ",
										record.data.id, record.data.officialSymbol, record.data.officialName);
					}
				}/*
					 * ,{header: 'Name', id: 'desc', toolTip: 'Gene name',
					 * dataIndex: 'officialName' }
					 */, {
					id : 'taxon',
					toolTip : 'Gene\'s Taxon',
					header : 'Taxon',
					dataIndex : 'taxon',
					hidden : true
				}, {
					id : 'inList',
					toolTip : 'Marks whether this gene is present in one of your lists',
					header : 'In List(s)',
					dataIndex : 'inList',
					hidden : true
				}, this.action]
			}),
			plugins : [this.action]
		});

		// add columns dependent on columnSet config
		if (this.columnSet === "full") {
			Ext.apply(this, this.getFullColumnModel());
		}

		Gemma.GeneGrid.superclass.initComponent.call(this);

		this.addEvents('addgenes', 'removegenes', 'geneListModified');

		this.on('doneModification', function() {
			this.changesMade = false;
				// this.saveButton.disable();
				 this.doneButton.disable();
			});

		this.getStore().on("remove", function() {
					this.fireEvent("removegenes");
					this.changesMade = true;
					this.saveButton.enable();
					this.doneButton.enable();
				}, this);

		this.getStore().on("add", function() {
					this.fireEvent("addgenes");
					this.changesMade = true;
					this.saveButton.enable();
					this.doneButton.enable();
				}, this);

		this.on("keypress", function(e) {
					if (!this.getTopToolbar().disabled && e.getCharCode() === Ext.EventObject.DELETE) {
						this.removeGene();
					}
				}, this);

		// load genes stored in genes var, which can either be an array or comma
		// separated list of gene ids
		if (this.genes) {
			var genes = this.genes instanceof Array ? this.genes : this.genes.split(",");
			this.loadGenes(genes);
		}

	},// eo initComponent

	removeGene : function() {
		var selected = this.getSelectionModel().getSelections();
		var i;
		for (i = 0; i < selected.length; i++) {
			this.getStore().remove(selected[i]);
		}
		this.getSelectionModel().selectLastRow();
	},

	record : Ext.data.Record.create([{
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
			}, {
				name : 'inList',
				type : 'boolean',
				defaultValue : true
			}]),

	addGene : function(gene) {
		if (!gene) {
			return;
		}

		if (this.getStore().find("id", gene.id) < 0) {
			var Constructor = this.record;
			var record = new Constructor(gene);
			this.getStore().add([record]);
		}
	},

	/**
	 * 
	 * NOTE: NEED TO OVERRIDE THIS METHOD IN GENE CHOOSER PANEL B/C IT SHOULD
	 * GRAB ID OF GENE IN TOOLBAR TOO
	 * 
	 * @return {} list of all geneids currently held in the grid
	 */
	getGeneIds : function() {
		var ids = [];
		var all = this.getStore().getRange();
		var i = 0;
		for (i = 0; i < all.length; ++i) {
			ids.push(all[i].data.id);
		}
		return ids;
	},

	/**
	 * 
	 * NOTE: NEED TO OVERRIDE THIS METHOD IN GENE CHOOSER PANEL B/C IT SHOULD
	 * GRAB ID OF GENE IN TOOLBAR TOO
	 * 
	 * gene = {id, officialSymbol, officialName, taxon, inList flag}
	 * 
	 * @return [] array of genes objects currently held in the grid
	 */
	getGenes : function() {
		var genes = [];
		var all = this.getStore().getRange();
		var i = 0;
		for (i = 0; i < all.length; ++i) {
			genes.push(all[i].data);
		}
		return genes;
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
			this.newGroupName = "Gene group created: " + (new Date()).toString();
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
			this.newGroupDescription = "Temporary experiment group saved " + (new Date()).toString();
		}
	},

	/**
	 * When user clicks done, just save to session
	 */
	done : function() {
		
		this.createDetails();
		this.createInSession();
	},
	
	exportToTxt : function(){
		// TODO
		
	},

	/**
	 * When user clicks 'save', figure out what kind of save to do
	 */
	save : function() {

		// get name and description set up
		this.createDetails();

		// save button should only be visible if user is not logged in, but just
		// to be safe:
		if (!Ext.get('hasUser').getValue()) {
			Ext.Msg.alert("Not logged in", "You cannot save this list because you are not logged in, "+
							" however, your list will be available temporarily.");
			this.createInSession();
		} else {

			// if geneGroupId is null, then there was no group to start with
			// if user has made any changes, a new gene set will be created
			if (!this.geneGroupId || this.geneGroupId === null) {
				// ask user if they want to save changes
				this.editedExistingGroup('yes'); // yes means 'save as'

			} else {// if this is an edit of an existing gene group, give
				// options to create or edit

				// if group being edited is session-bound, only offer to save to
				// database
				if (this.selectedGeneGroup !== null
						&& (this.selectedGeneGroup.type.indexOf('Session') >= 0 || this.selectedGeneGroup.type
								.indexOf('session') >= 0)) {

					this.editedExistingGroup('yes'); // yes means 'save as'
				}

				// if group of genes being edited belongs to the user, ask if
				// they want to save changes
				else if (this.selectedGeneGroup !== null
						&& (this.selectedGeneGroup.type.indexOf('user') >= 0 || this.selectedGeneGroup.type
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
								fn : this.editedExistingGroup,
								icon : Ext.MessageBox.QUESTION
							});
				} else {
					this.editedExistingGroup('yes'); // yes means 'save as'
				}
			}
		}

	},
	createInSession : function() {

		var ids = this.getGeneIds();
		var editedGroup;
		if(this.selectedGeneGroup === null || typeof this.selectedGeneGroup === 'undefined' ){ //group wasn't made before launching 
			editedGroup = {
				reference: {
					id : null,
					type : null
				}
			};
		}else{
			editedGroup = this.selectedGeneGroup; 
			// reference has the right type already
		}

		editedGroup.name = this.newGroupName;
		editedGroup.description = this.newGroupDescription;
		editedGroup.geneIds = ids;
		editedGroup.memberIds = ids;
		editedGroup.type = 'usergeneSetSession';

		GeneSetController.addSessionGroups(
				[editedGroup], // returns datasets added
				function(geneSets) {
					// should be at least one datasetSet
					if (geneSets === null || geneSets.length === 0) {
						// TODO error message
						return;
					} else {
						var newRecordData = geneSets;
						this.fireEvent('geneListModified', newRecordData);
						this.fireEvent('doneModification');
					}
				}.createDelegate(this));

	},
	createInDatabase : function() {
		if (typeof this.selectedGeneGroup !== 'undefined') {

			var ids = this.getGeneIds();

			var editedGroup = this.selectedGeneGroup; // reference has the
			// right type already
			editedGroup.name = this.newGroupName;
			editedGroup.description = this.newGroupDescription;
			editedGroup.geneIds = ids;
			editedGroup.memberIds = ids;
			editedGroup.type = 'usergeneSet';

			GeneSetController.create(
					[editedGroup], // returns datasets added
					function(geneSets) {
						// should be at least one datasetSet
						if (geneSets === null || geneSets.length === 0) {
							// TODO error message
							return;
						} else {
							var newRecordData = geneSets;
							this.fireEvent('geneListModified', newRecordData);
							this.fireEvent('doneModification');
						}
					}.createDelegate(this));
		}

		this.fireEvent('doneModification');

	},
	updateDatabase : function() {

		var groupId = this.selectedGeneGroup.reference.id;
		this.newGroupName = this.groupName;
		var geneIds = this.getGeneIds();

		GeneSetController.updateMembers(groupId, geneIds, function(msg) {
					this.selectedGeneGroup.memberIds = geneIds;
					this.selectedGeneGroup.geneIds = geneIds;

					this.fireEvent('geneListModified', this.selectedGeneGroup);
					this.fireEvent('doneModification');
				}.createDelegate(this));
	}
});
Ext.reg('geneMembersGrid', Gemma.GeneMembersGrid);


/**
 * toolbar for selecting genes or gene groups and adding them to a grid
 * if this.taxonId is set, then searches will be limited by taxon  
 */
Gemma.GeneAndGroupAdderToolbar = Ext.extend(Ext.Toolbar,{
		//name : "eeChooserTb",

			initComponent : function() {

				Gemma.GeneAndGroupAdderToolbar.superclass.initComponent.call(this);
				
				this.geneCombo = new Gemma.GeneAndGeneGroupCombo({
					typeAhead : false,
					width : 300,
					listeners : {
								'select' : {
									fn : function(combo, rec, index) {
										this.addButton.enable();
										if(rec.data.size === 1){
											this.addButton.setText('Add 1 gene');
										}else{
											this.addButton.setText('Add '+rec.data.size+' genes');
										}
										
									}.createDelegate(this)
								}
							}
				});

				this.addButton = new Ext.Toolbar.Button({
							icon : "/Gemma/images/icons/add.png",
							cls : "x-btn-text-icon",
							tooltip : "Add selected experiment(s) to the list",
							text: 'Add',
							disabled : true,
							handler : function() {
								this.geneGrid.addGenes(this.geneCombo.getGeneGroup());
								this.geneCombo.reset();
								this.addButton.setText('Add');
								this.addButton.disable();
							}.createDelegate(this)
						});

			},
			afterRender : function(c, l) {
				Gemma.GeneAndGroupAdderToolbar.superclass.afterRender.call(this, c, l);
				this.add(this.geneCombo, this.addButton);
			}
});
