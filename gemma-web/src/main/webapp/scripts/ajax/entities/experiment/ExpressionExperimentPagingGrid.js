Ext.namespace('Gemma');

Gemma.ExperimentPagingStore = Ext.extend(Ext.data.Store, {
	constructor: function(config){
        Gemma.ExperimentPagingStore.superclass.constructor.call(this, config);
    },
	initComponent: function(){
		Gemma.ExperimentPagingStore.superclass.initComponent.call(this);
	},
	paramNames: {
	    start : 'start',  // default The parameter name which specifies the start row
	    adjustedStart: 'adjustedStart', // start row based on records returned (needed for dealing with security filtering)
	    limit : 'limit',  // default The parameter name which specifies number of rows to return
	    sort : 'sort',    // default The parameter name which specifies the column to sort on
	    dir : 'dir'       // default The parameter name which specifies the sort direction
	},
    remoteSort: true,
    proxy: new Ext.data.DWRProxy({
        apiActionToHandlerMap: {
            read: {
                dwrFunction: ExpressionExperimentController.browse,
                getDwrArgsFunction: function(request){
                    var params = request.params;
                    return [params];
                }
            }
        }
    }),
    
    reader: new Ext.data.JsonReader({
        root: 'records', // required.
        successProperty: 'success', // same as default.
        messageProperty: 'message', // optional
        totalProperty: 'totalRecords', // default is 'total'; optional unless
        // paging.
        idProperty: "id", // same as default,
        adjustedStart: 'adjustedStart',
        // used by store to set its sortInfo
        sortInfo : {
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
            name: "bioAssayCount",
            type: "int"
        }, {
            name: "taxon",
			type: "string"
        }, {
            name: "dateCreated",
			type: "date"
		}
		, {
            name: "troubleFlag"
        }]
    }),
    
    writer: new Ext.data.JsonWriter({
        writeAllFields: true
    })

});

Gemma.ExperimentPagingStoreSelectedIds = Ext.extend(Gemma.ExperimentPagingStore, {

	initComponent: function(){
		Gemma.ExperimentPagingStoreSelectedIds.superclass.initComponent.call(this);
	},
	// overwrite proxy to use load instead of browse
    proxy: new Ext.data.DWRProxy({
        apiActionToHandlerMap: {
            read: {
                dwrFunction: ExpressionExperimentController.browseSpecificIds,
                getDwrArgsFunction: function(request){
                    var params = request.params;
					var ids = params.ids;
					// ids is not a field in the ListBatchCommand java object
					delete params.ids; 
                    return [params, ids];
                }
            }
        }
    })
});


Gemma.ExperimentPagingStoreTaxon = Ext.extend(Gemma.ExperimentPagingStore, {

	initComponent: function(){
		Gemma.ExperimentPagingStoreSelectedIds.superclass.initComponent.call(this);
	},
	// overwrite proxy to use load instead of browse
    proxy: new Ext.data.DWRProxy({
        apiActionToHandlerMap: {
            read: {
                dwrFunction: ExpressionExperimentController.browseByTaxon,
                getDwrArgsFunction: function(request){
                    var params = request.params;
					var taxonId = params.taxonId;
					// taxonId is not a field in the ListBatchCommand java object 
					delete params.taxonId;
                    return [params, taxonId];
                }
            }
        }
    })
});

Gemma.ExperimentPagingGrid = Ext.extend(Ext.grid.GridPanel, {
        //width: 1000,
        loadMask: true,
		autoScroll:true,
		stripeRows : true,
		rowExpander:true,
		emptyText:'Either you didn\'t select any experiments, or you don\'t have permissions to view the ones you chose.',
		viewConfig : {
			forceFit : true
		},
		myPageSize:50,
		title:'Expression Experiments',

        columns: [
			/*{ // for testing
                id:'id',
				header: "db id",
                dataIndex: 'id',
				sortable:true,
                width: 0.1 //viewConfig.forceFit resizes based on relative widths
            },*/{
                id:'name',
				header: "Dataset Name",
                dataIndex: 'name',
				sortable:true,
                width: 0.5, //viewConfig.forceFit resizes based on relative widths,
                renderer: function(value, metaData, record, rowIndex, colIndex, store){
					return (value && record)?'<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id='+record.id+'">'+value+'</a>':'';
				}
            }, {
				header: "Status",
                dataIndex: 'troubleFlag',
				sortable:true,
                width: 0.03,
				hidden: true,
                renderer: function(value, metaData, record, rowIndex, colIndex, store){
					return (value)?'<img title="'+value.detail+' ('+value.note+')" src="http://sandbox.chibi.ubc.ca/Gemma/images/icons/warning.png"/>':'';
				}
            }, {
                header: "Short Name",
                dataIndex: 'shortName',
				sortable:true,
                width: 0.1
            }, {
                header: "Assay Count",
                dataIndex: 'bioAssayCount',
				sortable:true,
                width: 0.1,
				tooltip:"View bioassays",
                renderer: function(value, metaData, record, rowIndex, colIndex, store){
					return (value && record)?'<a title="View bioassays" href="Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id='+record.id+'">'+value+'</a>':'';
				}
            }, {
                header: "Species",
                dataIndex: 'taxon',
				sortable:true,
                width: 0.1
            }],
		initComponent: function(){
			
            /*SIZING remove this when we use viewport*/
            var pageHeight = window.innerHeight !== null ? window.innerHeight : document.documentElement &&
            document.documentElement.clientHeight ? document.documentElement.clientHeight : document.body !== null ? document.body.clientHeight : null;
            var pageWidth = window.innerWidth !== null ? window.innerWidth : document.documentElement &&
            document.documentElement.clientWidth ? document.documentElement.clientWidth : document.body !== null ? document.body.clientWidth : null;
            var winPadding = 5;
            var minAppWidth = 300;
            var minAppHeight = 200;
            var adjPageWidth = ((pageWidth - winPadding) > minAppWidth) ? (pageWidth - winPadding - 60) : minAppWidth;
            // not sure why need extra -30 here and not below, but otherwise it doesn't fit 
            var adjPageHeight = ((pageHeight - winPadding) > minAppHeight) ? (pageHeight - winPadding - 30) : minAppHeight;
            // resize all elements with browser window resize
            Ext.EventManager.onWindowResize(function(width, height){
                // -50 so that window fits nicely
                var adjWidth = ((width - winPadding) > minAppWidth) ? (width - winPadding) : minAppWidth;
                var adjHeight = ((height - winPadding) > minAppHeight) ? (height - winPadding) : minAppHeight;
                this.setSize(adjWidth, adjHeight);
                this.doLayout();
            }, this);
			this.setSize(adjPageWidth, adjPageHeight);
			
			
			
			this.showAll= !(document.URL.indexOf("?") > -1 && (document.URL.indexOf("id=") > -1 || document.URL.indexOf("taxonId=") > -1));
			this.idSubset = [];
			var myPageSize = this.myPageSize;
			var filterById = false;
			var filterByTaxon = false;
			if(!this.showAll){
				var subsetDetails = document.URL.substr(document.URL.indexOf("?") + 1);
				var param = Ext.urlDecode(subsetDetails);
				if(param.id){
					this.idSubset = param.id.split(',');
					filterById = true;
				}else if(param.taxonId){
					this.taxonId = param.taxonId;
					filterByTaxon = true;
				}else{
					this.showAll = true;
				}
			}
			var pageStore;
			if (filterById) {
				this.setTitle(this.idSubset.length + " Expression Experiments");
				pageStore = new Gemma.ExperimentPagingStoreSelectedIds({
					autoLoad: {
						params: {
							start: 0,
							limit: myPageSize
						}
					},
					baseParams:{
						ids: this.idSubset
					}
				});
			}else if(filterByTaxon){
				pageStore = new Gemma.ExperimentPagingStoreTaxon({
					autoLoad: {
						params: {
							start: 0,
							limit: myPageSize
						}
					},
					baseParams:{
						taxonId:this.taxonId
					}
				});
				
				this.setTitle("Expression Experiments For Taxon");
				pageStore.on('load', function( store, records, options){
					// problem for salmon because this will be child taxon, not parent
					// TODO use parent taxon, need to add it to object coming from back end
					if(records[0]){
						this.setTitle("Expression Experiments For Taxon: "+records[0].get('taxon'));
					}
					
				},this);
			} else {
				this.setTitle("All Expression Experiments");
				pageStore = new Gemma.ExperimentPagingStore({
					autoLoad: {
						params: {
							start: 0,
							limit: myPageSize
						}
					}
				});
			}
			pageStore.setDefaultSort('name');
			
			pageStore.on('load', function( store, records, options){
					//console.log(records[0]);
				},this);
			var eeCombo = new Gemma.ExperimentAndExperimentGroupCombo({
					width : 310,
					hideTrigger: true
				});
			eeCombo.on("recordSelected", function(selectedGroup){
				if(selectedGroup !== null){
					
					this.showAll = false;
					filterById = true;
					// create a store that browses with selected ids
					var ids = selectedGroup.memberIds;
					this.setTitle(selectedGroup.name+": "+ids.length + " Expression Experiments");
					pageStore = new Gemma.ExperimentPagingStoreSelectedIds({
						autoLoad: {
							params: {
								start: 0,
								limit: myPageSize
							}
						},
						baseParams: {
							ids: ids
						}
					});
					
					// bind paging toolbar to new store
					this.getBottomToolbar().bind(pageStore);
					// bind new store to grid
					this.reconfigure(pageStore, this.getColumnModel());
					this.nowSubset();
					
					// problem: if the user reaches this grid with a URL like '[...]?taxonId=4' and then does a search, the taxon 
					// clause will still be in the URL
					// so we need to refresh to clear the url, but if we do that, we lose the selected ids!
					// as a quick fix in this one case, we'll use this alternative way that is bookmarkable but limited by URL length
					//if(document.URL.indexOf("?") > 0){
					//	window.location = "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="+ids.join(',');
					//} 
				}
			},this);
			var editMine = new Ext.Button({
				text: 'Edit my datasets',
				cls:'x-toolbar-standardbutton',
				handler: function(){
					window.location = "/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html";
				},
				hidden: true
			});
			var subsetText = new Ext.BoxComponent({
				xtype:'box',
				html: 'Viewing subset',
				border:false,
				style: 'padding-right:10px;padding-left:10px',
				hidden: this.showAll
			});
			var showAllButton = new Ext.Button({
				text: 'Show All',
				cls:'x-toolbar-standardbutton',
				handler: function(){
					window.location = "/Gemma/expressionExperiment/showAllExpressionExperiments.html";
				},
				hidden: this.showAll
			});
			this.nowSubset = function(){
				subsetText.show(); 
				showAllButton.show();
			};
			var mybbar = new Ext.PagingToolbar({
					store: pageStore, // grid and PagingToolbar using same
					// store
					displayInfo: true,
					pageSize: myPageSize
				});
			Ext.apply(this, {
				store: pageStore,
				tbar:[eeCombo,'-', subsetText, showAllButton,'->',editMine],
				bbar: mybbar
			});
			if (this.rowExpander) {
				Ext.apply(this, {
						rowExpander : new Gemma.EEGridRowExpander({
									tpl : ""
								})
					});
				this.columns.unshift(this.rowExpander);
				Ext.apply(this, {
						plugins : this.rowExpander
					});
			}
			Gemma.ExperimentPagingGrid.superclass.initComponent.call(this);

			// when user changes the grid's sorting, bring them back to the first page
			// this makes sense if they're on the 4th page, but may be annoying if the user is on the last page
			// since the prior is more common than the latter, I think this is a good move
			//this.on('sortchange', function(grid, sortinfo){
			// THIS DOESN'T WORK WELL BECAUSE THE GRID RELOADS WITH NEW SORT FIRST, THEN RELOADS WITH FIRST PAGE
			//	grid.getBottomToolbar().moveFirst();
			//});
			
			// if the user is an admin, show the status column
			var isAdmin = (Ext.getDom('hasAdmin'))?Ext.getDom('hasAdmin').getValue():false;
			if(isAdmin){
				var index = this.getColumnModel().findColumnIndex('troubleFlag');
				this.getColumnModel().setHidden(index, false);
			}

		}
    
});