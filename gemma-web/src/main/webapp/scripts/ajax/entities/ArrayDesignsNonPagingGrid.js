Ext.namespace('Gemma');


Gemma.ArrayDesignsStore = Ext.extend(Ext.data.Store, {
	reader: new Ext.data.JsonReader({
		idProperty: "id", // same as default,
		// used by store to set its sortInfo
		sortInfo: {
			field: "name",
			direction: "ASC"
		},
		fields: [{
			name: "id",
			type: "int"
		}, {
			name: "name",
			type: "string"
		}, {
			name: "shortName",
			type: "string"
		}, {
			name: "taxon",
			type: "string"
		},  {
			name: "expressionExperimentCount"
		}, {
			name: "summaryTable"
		}, {
			name: "lastSequenceUpdate",
			dateFormat: "timestamp",
			type: "date"
		}, {
			name: "lastRepeatMask",
			dateFormat: "timestamp",
			type: "date"
		}, {
			name: "lastSequenceAnalysis",
			dateFormat: "timestamp",
			type: "date"
		}, {
			name: "lastGeneMapping",
			dateFormat: "timestamp",
			type: "date"
		}, {
			name: "color",
			type: "string"
		}, {
			name: "isMergee"
		}, {
			name: "isMerged"
		}, {
			name: "isSubsumed"
		}, {
			name: "isSubsumer"
		}, {
			name: "troubled"
		}, {
			name: "troubleEvent"
		}, {
			name: "troubleEventDate",
			convert: function(v, record){
				if(record.troubleEvent && record.troubleEvent.date){
					return record.troubleEvent.date;
				}
				return null;
			},
			dateFormat: "timestamp",
			type: "date"
		}, {
			name: "statusArray",
			convert: function(v, record){
				return [record.troubled, record.isMerged, record.isMergee, record.isSubsumed, record.isSubsumer];
			},
			sortDir: 'DESC',
			sortType: function(value){
				var i;
				var count = 0;
				for (i = 0; i < value.length; i++) {
					if (value[i]) {
						count++;
					}
				}
				return count;
			}
		},{
			name: "designElementCount",
			defaultValue:'[not avail.]',
			useNull: true,
			convert: function(v, record){
				if (v === null) {
					return '<span style="color:grey">[not avail.]</span>';
				}
				return v;
			}
		},{
			name: "numProbeSequences",
			defaultValue:'[not avail.]',
			useNull: true,
			convert: function(v, record){
				if (v === null) {
					return '<span style="color:grey">[not avail.]</span>';
				}
				return v;
			}
		},{
			name: "numProbeAlignments",
			defaultValue:'[not avail.]',
			useNull: true,
			convert: function(v, record){
				if (v === null) {
					return '<span style="color:grey">[not avail.]</span>';
				}
				return v;
			}
		},{
			name: "numProbesToKnownGenes",
			defaultValue:'[not avail.]',
			useNull: true,
			convert: function(v, record){
				if (v === null) {
					return '<span style="color:grey">[not avail.]</span>';
				}
				return v;
			}
		},{
			name: "numGenes",
			defaultValue:'[not avail.]',
			useNull: true,
			convert: function(v, record){
				if (v === null) {
					return '<span style="color:grey">[not avail.]</span>';
				}
				return v;
			}
		},{
			name: "dateCached",
		},{
			name: 'cannotBeDeleted',
			convert: function(v,record){
				if(record.expressionExperimentCount === 0 && !record.isMerged && !record.isMergee){
					return false;
				}
				return true;
			}
		}]
	})
});

Gemma.ArrayDesignsNonPagingGrid = Ext.extend(Ext.grid.GridPanel, {
    //width: 1000,
    autoScroll: true,
    stripeRows: true,
    rowExpander: true,
    emptyText: 'Either you didn\'t select any experiments, or you don\'t have permissions to view the ones you chose.',
    viewConfig: {
        forceFit: true
    },
    myPageSize: 50,
    title: 'Array Designs',
	totalCount: 0,
    
    
	loadArrayDesigns: function( adIds ){
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Loading Array Designs ..."
					});
		}
		this.loadMask.show();
		ArrayDesignController.loadArrayDesignsForShowAll(adIds, function(arrayDesigns){
				this.loadMask.hide();
				this.getStore().loadData(arrayDesigns);
				this.setTitle(arrayDesigns.length + ((arrayDesigns.length === 1) ? " Array Design" : " Array Designs"));
				this.totalCount = arrayDesigns.length;
			}.createDelegate(this));
	},
    initComponent: function(){
	
		this.showAll = !(document.URL.indexOf("?") > -1 && (document.URL.indexOf("id=") > -1));
		this.idSubset = null;
		var filterById = false;
		this.showOrphans = false;
		this.showMergees = false;
		if (!this.showAll) {
			var subsetDetails = document.URL.substr(document.URL.indexOf("?") + 1);
			var param = Ext.urlDecode(subsetDetails);
			if (param.id) {
				this.idSubset = param.id.split(',');
				filterById = true;
			}if (param.showOrph) {
				this.showOrphans = param.showOrph;
			}if (param.showMerg) {
				this.showMergees = param.showMerg;
			}
		}
		
		var pageStore = new Gemma.ArrayDesignsStore({		//lastOptions: {params: {start: 0, limit: myPageSize}}
		});
		pageStore.on('load', function(store, records, options){
			/*console.log(records[0]);
			console.log(records[1]);
			console.log(records[2]);
			console.log(records[3]);
			console.log(records[4]);
			console.log(records[5]);*/
		}, this);
		Ext.apply(this, {
			store: pageStore
		});
		
		// Create RowActions Plugin
		this.action = new Ext.ux.grid.RowActions({
			header: 'Actions',
			dataIndex:'actions',
			tooltip: 'Regenerate this report or delete orphaned designs (designs that aren\'t used by any experiments in Gemma)',
			// ,autoWidth:false
			// ,hideMode:'display'
			keepSelection: true,
			actions: [{
				iconCls: 'icon-refresh',
				tooltip: 'Refresh'
			}, {
				iconCls: 'icon-cross',
				tooltip: 'Delete array design',
				hideIndex: 'cannotBeDeleted'// hide if == true
			}],
			callbacks: {
				'icon-cross': function(grid, record, action, row, col){
				}
			}
		});
		
		this.action.on({
			action: function(grid, record, action, row, col){
				if (action === 'icon-cross') {
					Ext.Msg.confirm("Confirm Deletion", "Are you sure you want to delete this array design? This cannot be undone.", function(btnId){
						if (btnId === 'yes') {
							
							var callParams = [];
							callParams.push({
										id : record.id
									});
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, false);
											k.on('done', function(payload) {
												window.location.reload();
											});
										}.createDelegate(this)
									});
						
							ArrayDesignController.remove.apply(this, callParams);

						}
					});
				}
				else 
					if (action === 'icon-refresh') {
						updateArrayDesignReport(record.id); // function in arrayDesign.js
					}
			},
			// You can cancel the action by returning false from this
			// event handler.
			beforeaction: function(grid, record, action, row, col){
				return true;
			}
		});
		
		rowExpander = new Ext.grid.RowExpander({
			tpl: '<p>Probes: <b>{designElementCount}</b></p>' +
			'<p>With sequences: <b>{numProbeSequences}</b> <span style="color:grey">(Number of probes with sequences)</span></p>' +
			'<p>With align: <b>{numProbeAlignments}</b> <span style="color:grey">(Number of probes with at least one genome alignment)</span></p>' +
			'<p>Mapped to genes: <b>{numProbesToKnownGenes}</b> <span style="color:grey">(Number of probes mapped to known genes (including predicted and anonymous locations))</span></p>' +
			'<p>Unique genes: <b>{numGenes}</b> <span style="color:grey">(Number of unique genes represented on the array)</span></p>' +
			'<p> (as of {dateCached})</p>',
		});
		Ext.apply(this, {
			plugins: [this.action, rowExpander],
			colModel: new Ext.grid.ColumnModel({
				defaults: {
					sortable: true
				},
				columns: [			/*{ // for testing
			 id:'id',
			 header: "db id",
			 dataIndex: 'id',
			 sortable:true,
			 width: 0.1 //viewConfig.forceFit resizes based on relative widths
			 },*/
				rowExpander, {
					id: 'name',
					header: "Array Name",
					dataIndex: 'name',
					width: 0.3, //viewConfig.forceFit resizes based on relative widths,
					renderer: function(value, metaData, record, rowIndex, colIndex, store){
						return (value && record) ? '<a title="' + value + '" href="/Gemma/arrays/showArrayDesign.html?id=' +
						record.id +
						'">' +
						value +
						'</a>' : '';
					}
				}, {
					header: "Status",
					dataIndex: 'statusArray',
					width: 0.04,
					renderer: function(value, metaData, record, rowIndex, colIndex, store){
						var statusString = "";
						
						if (record.get('troubled')) {
							var te = record.get('troubleEvent');
							if (te) {
								var date = (record.get('troubleEventDate')) ? new Date(record.get('troubleEventDate')).format("Y-m-d") : '';
								var detail = (te && te.detail) ? '(' + te.detail + ')' : '';
								var user = (te && te.performer) ? ' by ' + te.performer + ': ' : '';
								statusString += '<img title="' + date + user + te.note + detail +
								'" src="/Gemma/images/icons/stop.png"/>&nbsp;';
							}
							else {
								statusString += '<img src="/Gemma/images/icons/stop.png"/>&nbsp;';
							}
						}
						if (record.get('isMerged')) {
							statusString += '<img title="merged: this design was created by merging others"' +
							' src="/Gemma/images/icons/merging_result.png"/>&nbsp;';
						}
						if (record.get('isMergee')) {
							statusString += '<img title="mergee: this design was merged with others to create a new design"' +
							' src="/Gemma/images/icons/merging_component.png"/>&nbsp;';
						}
						if (record.get('isSubsumed')) {
							statusString += '<img title="subsumed: all the sequences in this design are covered by another"' +
							' src="/Gemma/images/icons/subsumed.png"/>&nbsp;';
						}
						if (record.get('isSubsumer')) {
							statusString += '<img title="subsumer: this design "covers" one or more others in that it contains all their sequences"' +
							' src="/Gemma/images/icons/subsumer.png"/>';
						}
						
						return statusString;
					}
				}, {
					header: "Short Name",
					dataIndex: 'shortName',
					width: 0.07,
					renderer: function(value, metaData, record, rowIndex, colIndex, store){
						return '<span title="' + value + '">' + value + '</span>';
					}
				}, {
					header: "Taxon",
					dataIndex: 'taxon',
					width: 0.07
				}, {
					header: "Expts",
					dataIndex: 'expressionExperimentCount',
					width: 0.03,
					tooltip: 'Number of experiments in Gemma that use this design'
				}, {
					header: "Seq. Update",
					dataIndex: 'lastSequenceUpdate',
					width: 0.07,
					sortDir: 'DESC',
					xtype: 'datecolumn',
					format: 'Y-m-d'
				}, {
					header: "Rep. mask",
					dataIndex: 'lastRepeatMask',
					width: 0.07,
					sortDir: 'DESC',
					xtype: 'datecolumn',
					format: 'Y-m-d'
				}, {
					header: "Seq. Analysis",
					dataIndex: 'lastSequenceAnalysis',
					width: 0.07,
					sortDir: 'DESC',
					xtype: 'datecolumn',
					format: 'Y-m-d'
				}, {
					header: "Gene Mapping",
					dataIndex: 'lastGeneMapping',
					width: 0.07,
					sortDir: 'DESC',
					xtype: 'datecolumn',
					format: 'Y-m-d'
				}, {
					header: "Color",
					dataIndex: 'color',
					width: 0.03,
					renderer: function(value, metaData, record, rowIndex, colIndex, store){
						return (value === "ONECOLOR") ? "1" : (value === "TWOCOLOR") ? "2" : '<span title="' + value + '">' + value + '</span>';
					}
				}, this.action]
			})
		});
		
		this.getStore().addMultiFilter({
			name:'orphanFilter',
			active: !this.showOrphans,
			fn: function(record){
				return (record.get('expressionExperimentCount') && record.get('expressionExperimentCount') > 0);
			}
		});
		this.getStore().addMultiFilter({
			name:'mergeeFilter',
			active: !this.showMergees,
			fn: function(record){
				return !record.get('isMergee');
			}
		});
		this.getStore().addMultiFilter({
			name:'troubledFilter',
			active: false,
			fn: function(record){
				return !record.get('troubled');
			}
		});
		
		var textFilterFun = function(query){
			var value = new RegExp(Ext.escapeRe(query), 'i');
			return function(record){
				// go through every visible field, if it matches the query text
				// show the row
				  var fieldContents;
				  for (var field in record.data) {
				    fieldContents = record.data[field];
				    if(value.test(fieldContents)){
						return true;
					}
				}
				return false;
			}
		}
		
		Ext.apply(this, {
			tbar: new Ext.Toolbar({
				items: [{
					xtype: 'textfield',
					ref: 'searchInGrid',
					tabIndex: 1,
					enableKeyEvents: true,
					emptyText: 'Enter search term',
					listeners: {
						"keyup": {
							fn: function(){
								this.getTopToolbar().fieldClearBtn.enable();
								this.getStore().removeMultiFilter('textQueryFilter');
								this.getStore().addMultiFilter({
									name:'textQueryFilter',
									active: true,
									fn: textFilterFun(this.getTopToolbar().searchInGrid.getValue())
								});
								this.getStore().applyMultiFilters();
								
							},
							scope: this,
							options: {
								delay: 100
							}
						}
					}
				},{
					ref:'fieldClearBtn',
					disabled: true,
					tooltip: 'Clear your search',
					icon: '/Gemma/images/icons/cross.png',
					handler: function(){
						this.getTopToolbar().searchInGrid.reset();
						this.getStore().removeMultiFilter('textQueryFilter');
						this.getStore().applyMultiFilters();
						this.getTopToolbar().fieldClearBtn.disable();
						
					},
					scope: this
				},'-',{
					ref:'orphansToggle',
					text: 'Hide Orphans',
					tooltip: "Click to show/hide array designs that aren't used by any experiments in Gemma",
					handler: function(){
						if(this.getTopToolbar().orphansToggle.getText() === "Show Orphans"){
							
							this.getTopToolbar().orphansToggle.setText("Hide Orphans");
							this.showOrphans = true;
							this.getStore().deactivateMultiFilter('orphanFilter');
							this.getStore().applyMultiFilters();
							
						}else{
							
							this.getTopToolbar().orphansToggle.setText("Show Orphans");
							this.showOrphans = false;
							this.getStore().activateMultiFilter('orphanFilter');
							this.getStore().applyMultiFilters();
						}
						
					},
					scope:this
				},{
					ref:'mergeesToggle',
					text: 'Hide Mergees',
					tooltip: "Click to show/hide array designs that have been merged with others to create a new design",
					handler: function(){
						if(this.getTopToolbar().mergeesToggle.getText() === "Show Mergees"){
							
							this.getTopToolbar().mergeesToggle.setText("Hide Mergees");
							this.showMergees = true;
							this.getStore().deactivateMultiFilter('mergeeFilter');
							this.getStore().applyMultiFilters();
							
						}else{
							this.getTopToolbar().mergeesToggle.setText("Show Mergees");
							this.showMergees = false;
							this.getStore().activateMultiFilter('mergeeFilter');
							this.getStore().applyMultiFilters();
						}
						
					},
					scope:this
				},{
					ref:'troubledToggle',
					hidden: true,
					text: 'Hide Troubled',
					tooltip: "Click to show/hide array designs that are troubled",
					handler: function(){
						if(this.getTopToolbar().troubledToggle.getText() === "Show Troubled"){
							
							this.getTopToolbar().troubledToggle.setText("Hide Troubled");
							this.showTroubled = true;
							this.getStore().deactivateMultiFilter('troubledFilter');
							this.getStore().applyMultiFilters();
							
						}else{
							this.getTopToolbar().troubledToggle.setText("Show Troubled");
							this.showTroubled = false;
							this.getStore().activateMultiFilter('troubledFilter');
							this.getStore().applyMultiFilters();
						}
						
					},
					scope:this
				},'->',{
					ref:'ArrayDesignsSummaryWindowBtn',
					text: 'Array Designs Summary',
					cls: 'x-toolbar-standardbutton',
					hidden:true,
					handler: function(){
						if(Ext.WindowMgr.get('ArrayDesignsSummaryWindow')){
							Ext.WindowMgr.bringToFront('ArrayDesignsSummaryWindow');
						}else{
							new Gemma.ArrayDesignsSummaryWindow({id:'arrayDesignsSummaryWindow'}).show();
						}
					},
					scope:this
				}]
			})
		});
			
		Gemma.ArrayDesignsNonPagingGrid.superclass.initComponent.call(this);
		
		
		this.on('render', function(){
			this.loadArrayDesigns( this.idSubset );
			
		}, this);
		
		this.getStore().on('datachanged',function(store){
			this.setTitle(this.getStore().getCount()+" of "+this.totalCount+" Array Designs");
		},this);
		
		// if the user is an admin, show the status column
		var isAdmin = (Ext.getDom('hasAdmin')) ? Ext.getDom('hasAdmin').getValue() : false;
		this.adjustForIsAdmin(isAdmin);
		
		Gemma.Application.currentUser.on("logIn", function(userName, isAdmin){
			this.adjustForIsAdmin(isAdmin);
			// when user logs in, reload the grid in case they can see more experiments now
			// ex: troubled ADs are admin only
			//this.getStore().reload(this.getStore().lastOptions);
			
		}, this);
		Gemma.Application.currentUser.on("logOut", function(){
		
			// update the column model to hide the admin column
			this.adjustForIsAdmin(false);
			
			// when user logs out, reload the grid in case they can see fewer experiments now
			// ex: troubled ADs are admin only
			//this.getStore().reload(this.getStore().lastOptions);
			
		}, this);
		
	}, // end of initComponent
    // make changes based on whether user is admin or not
    adjustForIsAdmin: function(isAdmin){
		
        // if user is admin, update the column model to show the status column
		var colModel = this.getColumnModel()
		
        var index = this.getColumnModel().findColumnIndex('lastSequenceUpdate');
        colModel.setHidden(index, !isAdmin);
		
        index = this.getColumnModel().findColumnIndex('lastRepeatMask');
        colModel.setHidden(index, !isAdmin);
		
        index = this.getColumnModel().findColumnIndex('lastSequenceAnalysis');
        colModel.setHidden(index, !isAdmin);
		
        index = this.getColumnModel().findColumnIndex('lastGeneMapping');
        colModel.setHidden(index, !isAdmin);
		
        index = this.getColumnModel().findColumnIndex('color');
        colModel.setHidden(index, !isAdmin);
		
        index = this.getColumnModel().findColumnIndex('actions');
        colModel.setHidden(index, !isAdmin);
		
		if(!isAdmin){
			this.getStore().activateMultiFilter('troubledFilter');
			this.getStore().applyMultiFilters();
		}
		 
		this.getTopToolbar().troubledToggle.setVisible(isAdmin);
		this.getTopToolbar().ArrayDesignsSummaryWindowBtn.setVisible(isAdmin);
    }
});

Gemma.ArrayDesignsSummaryWindow = Ext.extend(Ext.Window,{
	title: 'Summary for All Array Designs',
	shadow: false,
	loadText: function(){
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Loading Summary ..."
					});
		}
		this.loadMask.show();
		ArrayDesignController.loadArrayDesignsSummary(function(arrayDesignSummary){
			if(arrayDesignSummary === null){
				arrayDesignSummary = this.defaultData;
			}else{
				for(field in arrayDesignSummary){
					if(arrayDesignSummary[field] === null){
						arrayDesignSummary[field] = '<span style="color:grey">[Not avail.]</span>';
					}
				}
			}
			
			this.update(arrayDesignSummary);
			this.loadMask.hide();
		}.createDelegate(this));
	},
	tpl: '<a href="/Gemma/arrays/generateArrayDesignSummary.html" onclick="return confirm(\'Regenerate report for all platforms?\');">Regenerate this report</a><br><br>'+
			'<p>With sequences: <b>{numProbeSequences}</b> <span style="color:grey">(Number of probes with sequences)</span></p>' +
			'<p>With align: <b>{numProbeAlignments}</b> <span style="color:grey">(Number of probes with at least one genome alignment)</span></p>' +
			'<p>Mapped to genes: <b>{numProbesToKnownGenes}</b> <span style="color:grey">(Number of probes mapped to known genes (including predicted and anonymous locations))</span></p>' +
			'<p>Unique genes:<b>{numGenes}</b> <span style="color:grey">(Number of unique genes represented on the array)</span></p>' +
			'<p> (as of {dateCached})</p>',
	padding:7,
	defaultData:{
		numProbeSequences: '<span style="color:grey">[Not avail.]</span>',
		numProbeAlignments: '<span style="color:grey">[Not avail.]</span>',
		numProbesToKnownGenes: '<span style="color:grey">[Not avail.]</span>',
		numGenes: '<span style="color:grey">[Not avail.]</span>',
		dateCached: '<span style="color:grey">[Not avail.]</span>',
	},
	initComponent: function(){
		
		Gemma.ArrayDesignsSummaryWindow.superclass.initComponent.call(this);
		this.on('render', function(){
			this.loadText();
		}, this);
	}// end of initComponent
});