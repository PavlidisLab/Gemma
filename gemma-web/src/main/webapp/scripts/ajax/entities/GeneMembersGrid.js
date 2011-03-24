
/*
 * Widget for displaying a list of genes, with cofigurable column sets. 
 * 
 * Version : $Id$ Author : luke, paul
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
 * Adjust columns displayed using "columnSet" config (values can be "reduced" (default) or "full")
 * if "full": symbol, description, species and 'in list' boolean are shown
 * if "reduced" (or any other value): only symbol and description are shown 
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
			stripeRows: true,
			changeMade: false,
//			bubbleEvents: ['geneListModified'],
			geneGroupId: null, 
			/*
			 * columnSet can be "reduced" or "full", 
			 * if "reduced": only symbol and description are shown
			 * if "full": symbol, description, species and 'in list' boolean are shown
			 */
			columnSet:"reduced",
			
			viewConfig : {
				forceFit : true,
				emptyText : "Multiple genes can be listed here"
			},
			autoScroll : true,
			
			autoExpandColumn : 'desc',

			showRemoveColumn : function(){
				// if config is set for "full" column model, show more columns
				this.getColumnModel().setHidden(this.getColumnModel().getIndexById("remove"),false);
			},

			getFullColumnModel : function(){
				// if config is set for "full" column model, show more columns
				this.getColumnModel().setHidden(this.getColumnModel().getIndexById("taxon"),false);
				this.getColumnModel().setHidden(this.getColumnModel().getIndexById("inList"),false);
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
							for (i; i < genes.length; i++) {
								geneData.push([genes[i].id, genes[i].taxonScientificName, genes[i].officialSymbol,
										genes[i].officialName]);
							}
							/*
							 * FIXME this can result in the same gene listed twice. This is taken care of at the server
							 * side but looks funny.
							 */
							this.getStore().loadData(geneData);
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
					 header:'Actions'
					//,autoWidth:false
					//,hideMode:'display'
					,keepSelection:true
					,actions:[{
						 iconCls:'icon-cross'
						,tooltip:'Remove gene'
					}]
					,callbacks:{
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
							buttons : [{
										id : 'done-selecting-button',
										text : "Done",
										handler : this.done,
										scope : this
									}, {
										id : 'cancel-selecting-button',
										text : "Cancel",
										handler : this.cancel,
										scope : this
									}],
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
												},{
													name : 'inList',
													type : 'boolean',
													defaultValue: true
												}],
										sortInfo : {
											field : 'officialSymbol',
											direction : 'ASC'
										}
									}),
							colModel: new Ext.grid.ColumnModel({
										defaults: {
											sortable: true
										},
										columns: [
										  {header: 'Symbol',
											toolTip: 'Gene symbol',
											dataIndex: 'officialSymbol',
											width: 75,
											renderer: function(value, metadata, record, row, col, ds){
												return String.format(
												"<a target='_blank' href='/Gemma/gene/showGene.html?id={0}'>{1}</a><br><span style=\"font-color:grey\">{2}</span> ", 
												record.data.id, record.data.officialSymbol,record.data.officialName);
											}
										}/*,{header: 'Name',
											id: 'desc',
											toolTip: 'Gene name',
											dataIndex: 'officialName'
										}*/,{id : 'taxon',
												toolTip : 'Gene\'s Taxon',
												header : 'Taxon',
												dataIndex : 'taxon',
												hidden: true
										},{id : 'inList',
												toolTip : 'Marks whether this gene is present in one of your lists',
												header : 'In List(s)',
												dataIndex : 'inList',
												hidden: true
										}, this.action]
									}),
							plugins:[this.action]
				}); 
				
				// add columns dependent on columnSet config
				if(this.columnSet==="full"){
					console.log("in columnSet=\"full\"");
					Ext.apply(this, this.getFullColumnModel());
				}
				

				Gemma.GeneGrid.superclass.initComponent.call(this);

				this.addEvents('addgenes', 'removegenes', 'geneListModified');

				this.getStore().on("remove", function() {
							this.fireEvent("removegenes");
							this.changesMade = true;
						}, this);

				this.getStore().on("add", function() {
							this.fireEvent("addgenes");
							this.changesMade = true;
						}, this);

				this.on("keypress", function(e) {
							if (!this.getTopToolbar().disabled && e.getCharCode() === Ext.EventObject.DELETE) {
								this.removeGene();
							}
						}, this);
				
				// load genes stored in genes var, which can either be an array or comma separated list of gene ids 
				if (this.genes) {
					var genes = this.genes instanceof Array ? this.genes : this.genes.split(",");
					this.loadGenes(genes);
				}

			},//eo initComponent

			removeGene : function() {
				var selected = this.getSelectionModel().getSelections();
				var i = 0;
				for (i; i < selected.length; i++) {
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
					},{
						name : 'inList',
						type : 'boolean',
						defaultValue: true
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
			 * NOTE: NEED TO OVERRIDE THIS METHOD IN GENE CHOOSER PANEL B/C IT SHOULD GRAB ID OF GENE IN TOOLBAR TOO
			 * 
			 * @return {} list of all geneids currently held in the grid
			 */
			getGeneIds : function() {
				var ids = [];
				var all = this.getStore().getRange();
				var i = 0;
				for ( i; i < all.length; ++i) {
					ids.push(all[i].data.id);
				}
				return ids;
			},

			/**
			 * 
			 * NOTE: NEED TO OVERRIDE THIS METHOD IN GENE CHOOSER PANEL B/C IT SHOULD GRAB ID OF GENE IN TOOLBAR TOO
			 * 
			 * gene = {id, officialSymbol, officialName, taxon, inList flag}
			 * 
			 * @return [] array of genes objects currently held in the grid
			 */
			getGenes : function() {
				var genes = [];
				var all = this.getStore().getRange();
				var i = 0;
				for ( i; i < all.length; ++i) {
					genes.push(all[i].data);
				}
				return genes;
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
					this.newGroupName = "Gene group created: "+(new Date()).toString();
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
					this.newGroupDescription = "Temporary gene group saved "+(new Date()).toString(); 
				} 					
				
				// if user is not logged in, only saving to session is available
				if (!Ext.get('hasUser').getValue()) {
						this.saveToSession();
				}else{
					// if geneGroupId is null, then there was no group to start with
					// if user has made any changes, a new gene set will be created
					if(!this.geneGroupId || this.geneGroupId === null){
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
							if(this.selectedGeneGroup!==null && this.selectedGeneGroup.type.indexOf('user')>=0){
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
				
					
					//this.saveToSession(name, description);
					// update the component that create this grid (ex search widget) with the new gene ids
					// send geneIDs for testing, eventually will always pass group id (session or db bound)
					this.fireEvent('geneListModified', this.getGeneIds(), this.newGroupName);
					
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
Ext.reg('geneMembersGrid', Gemma.GeneMembersGrid);

