Ext.namespace( 'Gemma' );
/**
 * @class Gemma.ExperimentPagingStore
 * @extends Ext.data.Store
 * @param {Object}
 *           config
 */
Gemma.ExperimentPagingStore = Ext.extend( Ext.data.Store, {
   constructor : function( config ) {
      Gemma.ExperimentPagingStore.superclass.constructor.call( this, config );
   },

   /**
    * @memberOf Gemma.ExperimentPagingStore
    */
   initComponent : function() {
      Gemma.ExperimentPagingStore.superclass.initComponent.call( this );
   },
   paramNames : {
      start : 'start', // default The parameter name which specifies the start row
      limit : 'limit', // default The parameter name which specifies number of rows to return
      sort : 'sort', // default The parameter name which specifies the column to sort on
      dir : 'dir' // default The parameter name which specifies the sort direction
   },
   remoteSort : true,
   proxy : new Ext.data.DWRProxy( {
      apiActionToHandlerMap : {
         read : {
            dwrFunction : ExpressionExperimentController.browse,
            getDwrArgsFunction : function( request ) {
               var params = request.params;
               return [ params ];
            }
         }
      }
   } ),

   reader : new Ext.data.JsonReader( {
      root : 'records', // required.
      successProperty : 'success', // same as default.
      messageProperty : 'message', // optional
      totalProperty : 'totalRecords', // default is 'total'; optional unless
      // paging.
      idProperty : "id", // same as default,
      adjustedStart : 'adjustedStart',
      // used by store to set its sortInfo
      sortInfo : {
         field : "dateLastUpdated",
         direction : "DESC"
      },
      fields : [ {
         name : "id",
         type : "int"
      }, {
         name : "name",
         type : "string"
      }, {
         name : "shortName",
         type : "string"
      }, {
         name : "bioAssayCount",
         type : "int"
      }, {
         name : "hasCoexpressionAnalysis",
         type : "boolean"
      }, {
         name : "hasDifferentialExpressionAnalysis",
         type : "boolean"
      }, {
         name : "taxon",
         type : "string"
      }, {
         name : "dateCreated",
         type : "date",
         sortDir : 'DESC'
      }, {
         name : "troubled",
         sortDir : 'DESC'
      }, {
         name : "troubleDetails"
      }, {
         name : "dateLastUpdated",
         type : "date",
         sortDir : 'DESC'
      } ]
   } ),

   writer : new Ext.data.JsonWriter( {
      writeAllFields : true
   } )

} );
/**
 * @class Gemma.ExperimentPagingStoreSelectedIds
 * @extends Gemma.ExperimentPagingStore
 * @param {Object}
 *           config
 */
Gemma.ExperimentPagingStoreSelectedIds = Ext.extend( Gemma.ExperimentPagingStore, {

   initComponent : function() {
      Gemma.ExperimentPagingStoreSelectedIds.superclass.initComponent.call( this );
   },
   // overwrite proxy to use load instead of browse
   proxy : new Ext.data.DWRProxy( {
      apiActionToHandlerMap : {
         read : {
            dwrFunction : ExpressionExperimentController.browseSpecificIds,
            getDwrArgsFunction : function( request ) {
               var params = request.params;
               var ids = params.ids;
               // ids is not a field in the ListBatchCommand java object
               delete params.ids;
               return [ params, ids ];
            }
         }
      }
   } )
} );

/**
 * @class Gemma.ExperimentPagingStoreTaxon
 * @extends Gemma.ExperimentPagingStore
 * @param {Object}
 *           config
 */
Gemma.ExperimentPagingStoreTaxon = Ext.extend( Gemma.ExperimentPagingStore, {

   initComponent : function() {
      Gemma.ExperimentPagingStoreSelectedIds.superclass.initComponent.call( this );
   },
   // overwrite proxy to use load instead of browse
   proxy : new Ext.data.DWRProxy( {
      apiActionToHandlerMap : {
         read : {
            dwrFunction : ExpressionExperimentController.browseByTaxon,
            getDwrArgsFunction : function( request ) {
               var params = request.params;
               var taxonId = params.taxonId;
               // taxonId is not a field in the ListBatchCommand java object
               delete params.taxonId;
               return [ params, taxonId ];
            }
         }
      }
   } )
} );

/**
 * @class Gemma.ExperimentPagingGrid
 * @extends Ext.grid.GridPanel
 * @param {Object}
 *           config
 */
Gemma.ExperimentPagingGrid = Ext
   .extend(
      Ext.grid.GridPanel,
      {
         // width: 1000,
         loadMask : true,
         autoScroll : true,
         stripeRows : true,
         rowExpander : true,
         emptyText : Gemma.HelpText.WidgetDefaults.ExperimentPagingGrid.emptyText,
         viewConfig : {
            forceFit : true
         },
         myPageSize : 20,
         title : 'Expression Experiments',

         columns : [ /*
                      * { // for testing id:'id', header: "db id", dataIndex: 'id', sortable:true, width: 0.1
                      * //viewConfig.forceFit resizes based on relative widths },
                      */
            {
               id : 'name',
               header : "Dataset Name",
               dataIndex : 'name',
               sortable : true,
               width : 0.5, // viewConfig.forceFit resizes based on relative widths,
               renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                  return (value && record) ? '<a href="/Gemma/expressionExperiment/showExpressionExperiment.html?id='
                     + record.id + '" title="' + value + '">' + value + '</a>' : '';
               }
            },
            {
               header : "Updated",
               dataIndex : 'dateLastUpdated',
               sortable : true,
               width : 0.06,
               hidden : true,
               renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                  return (value) ? Ext.util.Format.date( value, "Y/m/d" ) : '?';
               }
            },
            {
               header : "Status",
               tooltip : "D=has differential expression analysis; C=has coexpression analysis",
               dataIndex : 'troubled',
               sortable : true,
               width : 0.05,
               hidden : false,
               renderer : function( value, metaData, record, rowIndex, colIndex, store ) {

                  if ( value ) {
                     return '<img title="' + record.get( 'troubleDetails' )
                        + '" src="/Gemma/images/icons/warning.png"/>';
                  }

                  var text = '';
                  if ( record.get( 'hasCoexpressionAnalysis' ) ) {
                     text = text + 'C&nbsp;';
                  }
                  if ( record.get( 'hasDifferentialExpressionAnalysis' ) ) {
                     text = text + 'D&nbsp;';
                  }

                  return text;
               }
            },
            {
               header : "Short Name",
               dataIndex : 'shortName',
               sortable : true,
               width : 0.1
            },
            {
               header : "Assay Count",
               dataIndex : 'bioAssayCount',
               sortable : true,
               width : 0.1,
               tooltip : "View bioassays",
               renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                  return (value && record) ? '<a title="View bioassays" href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id='
                     + record.id + '">' + value + '</a>'
                     : '';
               }
            }, {
               header : "Species",
               dataIndex : 'taxon',
               sortable : true,
               width : 0.1
            } ],

         /**
          * private
          * 
          * @return a configured Gemma.ExperimentPagingStoreSelectedIds
          */
         getIdsStore : function( ids ) {
            var pageSize = this.myPageSize;
            var pageStore = new Gemma.ExperimentPagingStoreSelectedIds( {
               autoLoad : {
                  params : {
                     start : 0,
                     limit : pageSize
                  }
               },
               baseParams : {
                  ids : ids
               }
            } );

            pageStore.setDefaultSort( 'dateLastUpdated' );

            return pageStore;
         },
         /**
          * Show the experiments indicated by the array of ids passed in
          * 
          * Steps:
          * <ul>
          * <li> create a store with the ids from the parameter pass in
          * <li> unbind the current store from the paging toolbar and bind Gemma.ExperimentPagingStoreSelectedIds
          * <li> reconfigure the grid with the new store </li>
          * 
          * @param {Object}
          *           eeIds
          * @memberOf Gemma.ExperimentPagingGrid
          */
         loadExperimentsById : function( eeIds ) {

            // I tried being fancy and only creating a new store if the current one
            // wasn't an id store, but it caused problems on page changes
            // (the first page loaded fine, but when you navigated to the next page, it
            // was populated with entries from the previous query)
            // Always need to create new store for new queries, it seems.

            var idsStore = this.getIdsStore( eeIds );
            // bind new store to paging toolbar
            this.getBottomToolbar().bind( idsStore );
            // reconfigure grid to use new store
            this.reconfigure( idsStore, this.getColumnModel() );

            this.setShowAsTextParams( null, eeIds );
            this.nowSubset();
         },

         loadExperimentsByPlatform : function( platformId ) {

            ExpressionExperimentController.loadExperimentsForPlatform( platformId, {
               callback : function( vos ) {

                  var idsStore = this.getIdsStore( eeIds );
                  // bind new store to paging toolbar
                  this.getBottomToolbar().bind( idsStore );
                  // reconfigure grid to use new store
                  this.reconfigure( idsStore, this.getColumnModel() );

                  this.setShowAsTextParams( null, eeIds );
                  this.nowSubset();
               }
            } );

         },

         /**
          * private
          * 
          * @return a configured Gemma.ExperimentPagingStoreTaxon
          */
         getTaxonStore : function( id ) {
            var pageSize = this.myPageSize;
            var pageStore = new Gemma.ExperimentPagingStoreTaxon( {
               autoLoad : {
                  params : {
                     start : 0,
                     limit : pageSize
                  }
               },
               baseParams : {
                  taxonId : id
               }
            } );

            this.setTitle( "Expression Experiments For Taxon" );
            pageStore.on( 'load', function( store, records, options ) {
               // problem for salmon because this will be child taxon, not parent
               // TODO use parent taxon, need to add it to object coming from back end
               if ( records[0] ) {
                  this.setTitle( "Expression Experiments For Taxon: " + records[0].get( 'taxon' ) );
               }
            }, this );

            return pageStore;
         },

         getDownloadStore : function() {
            var subsetDetails = document.URL.substr( document.URL.indexOf( "?" ) + 1 );
            var param = Ext.urlDecode( subsetDetails );
            var pageStore = null;
            if ( this.downloadAsTextTaxonId ) {
               pageStore = new Gemma.ExperimentPagingStoreTaxon( {
                  autoLoad : {
                     params : {
                        start : 0,
                        limit : 0
                     }
                  },
                  baseParams : {
                     taxonId : this.downloadAsTextTaxonId
                  }
               } );

            } else if ( this.downloadAsTextSpecificIds ) {
               pageStore = new Gemma.ExperimentPagingStoreSelectedIds( {
                  autoLoad : {
                     params : {
                        start : 0,
                        limit : 0
                     }
                  },
                  baseParams : {
                     ids : this.downloadAsTextSpecificIds
                  }
               } );

            } else if ( param.id ) {

               var idSubset = param.id.split( ',' );

               pageStore = new Gemma.ExperimentPagingStoreSelectedIds( {
                  autoLoad : {
                     params : {
                        start : 0,
                        limit : 0
                     }
                  },
                  baseParams : {
                     ids : idSubset
                  }
               } );

            } else {
               pageStore = new Gemma.ExperimentPagingStore( {
                  autoLoad : {
                     params : {
                        start : 0,
                        limit : 0
                     }
                  }
               } );
            }

            pageStore.on( 'load', function( store, records, options ) {

               var gses = "";

               store.each( function( ee ) {
                  gses += ee.get( 'shortName' ) + "\t" + ee.get( 'taxon' ) + "\t" + ee.get( 'bioAssayCount' ) + "\t"
                     + ee.get( 'name' ) + "\n";
               } );

               var popup = new Ext.Window( {
                  modal : true,
                  title : "You can copy this text",
                  html : gses,
                  height : 400,
                  width : 500,
                  autoScroll : true,
                  bodyCfg : {
                     tag : 'textarea',
                     style : 'background-color : white;font-size:smaller'
                  }
               } );
               popup.show();

               this.downloadAsTextButton.setDisabled( false );
               this.downloadAsTextButton.setText( "" );

            }, this );

            return pageStore;
         },
         /**
          * Show the experiments indicated by the array of ids passed in
          * 
          * Steps:
          * <ul>
          * <li>if necessary, unbind the current store from the paging toolbar and bind
          * Gemma.ExperimentPagingStoreSelectedIds
          * <li> reconfigure the store with the new store (if necessary)
          * <li> load the (new) store with the ids from the parameter pass in
          * </ul>
          * 
          * @param {Object}
          *           eeIds
          */
         loadExperimentsByTaxon : function( taxonId ) {

            // I tried being fancy and only creating a new store if the current one
            // wasn't a taxon store, but it caused problems on page changes
            // (the first page loaded fine, but when you navigated to the next page, it
            // was populated with entries from the previous query)
            // Always need to create new store for new queries, it seems.

            var store = this.getTaxonStore( taxonId );

            // bind new store to paging toolbar
            this.getBottomToolbar().bind( store );
            // reconfigure grid to use new store
            this.reconfigure( store, this.getColumnModel() );

            this.setShowAsTextParams( taxonId, null );
            this.nowSubset();

         },

         setShowAsTextParams : function( taxonId, specificIds ) {
            this.downloadAsTextTaxonId = taxonId;
            this.downloadAsTextSpecificIds = specificIds;

         },

         /**
          * Show a list of short names (GSE numbers) of all or selected experiments
          * 
          */
         showAsText : function() {

            // disable this and add loading text

            this.downloadAsTextButton.setDisabled( true );
            this.downloadAsTextButton.setText( "Loading" );

            // this will autoload the results and display a popup after load
            var downloadStore = this.getDownloadStore();

         },

         initComponent : function() {

            this.showAll = !(document.URL.indexOf( "?" ) > -1 && (document.URL.indexOf( "id=" ) > -1 || document.URL
               .indexOf( "taxonId=" ) > -1));
            this.idSubset = [];
            var myPageSize = this.myPageSize;
            var filterById = false;
            var filterByTaxon = false;
            if ( !this.showAll ) {
               var subsetDetails = document.URL.substr( document.URL.indexOf( "?" ) + 1 );
               var param = Ext.urlDecode( subsetDetails );
               if ( param.id ) {
                  this.idSubset = param.id.split( ',' );
                  filterById = true;
               } else if ( param.taxonId ) {
                  this.taxonId = param.taxonId;
                  filterByTaxon = true;
               } else {
                  this.showAll = true;
               }
            }
            var pageStore;
            if ( filterById ) {

               pageStore = this.getIdsStore( this.idSubset );

            } else if ( filterByTaxon ) {

               pageStore = this.getTaxonStore( this.taxonId );

            } else {
               this.setTitle( "All Expression Experiments" );
               pageStore = new Gemma.ExperimentPagingStore( {
                  autoLoad : {
                     params : {
                        start : 0,
                        limit : myPageSize
                     }
                  }
               } );
            }
            pageStore.setDefaultSort( 'dateLastUpdated' );

            pageStore.on( 'load', function( store, records, options ) {
               // console.log(records[0]);
            }, this );
            var eeCombo = new Gemma.ExperimentAndExperimentGroupCombo( {
               width : 310,
               hideTrigger : true
            } );
            eeCombo.on( "recordSelected", function( selectedGroup ) {
               if ( selectedGroup !== null ) {

                  this.showAll = false;
                  filterById = true;
                  // create a store that browses with selected ids
                  var ids = selectedGroup.memberIds;
                  this.loadExperimentsById( ids );
                  // ids is not security filtered
                  // var totalCount = this.getStore().getTotalCount();
                  this.setTitle( "Displaying set: &quot;" + selectedGroup.name + "&quot;" );

                  // problem: if the user reaches this grid with a URL like '[...]?taxonId=4' and then does a
                  // search, the taxon
                  // clause will still be in the URL
                  // so we need to refresh to clear the url, but if we do that, we lose the selected ids!
                  // as a quick fix in this one case, we'll use this alternative way that is bookmarkable but
                  // limited by URL length
                  // if(document.URL.indexOf("?") > 0){
                  // window.location =
                  // "/Gemma/expressionExperiment/showAllExpressionExperiments.html?id="+ids.join(',');
                  // }
               }
            }, this );
            var editMine = new Ext.Button( {
               text : 'Edit my datasets',
               cls : 'x-toolbar-standardbutton',
               handler : function() {
                  window.location = "/Gemma/expressionExperiment/showAllExpressionExperimentLinkSummaries.html";
               },
               hidden : true
            } );
            var subsetText = new Ext.BoxComponent( {
               xtype : 'box',
               html : 'Viewing subset',
               border : false,
               style : 'padding-right:10px;padding-left:10px',
               hidden : this.showAll
            } );
            var showAllButton = new Ext.Button( {
               text : 'Show All Experiments',
               cls : 'x-toolbar-standardbutton',
               handler : function() {
                  window.location = "/Gemma/expressionExperiment/showAllExpressionExperiments.html";
               },
               hidden : this.showAll
            } );
            var QClinkButton = new Ext.Button( {
               text : 'Experiment QC',
               cls : 'x-toolbar-standardbutton',
               handler : function() {
                  window.location = "/Gemma/expressionExperimentsWithQC.html";
               }
            } );

            // Button for downloading the shortNames of all or selected experiments as text
            var asTextButton = new Ext.Button( {
               icon : '/Gemma/images/icons/disk.png',
               handler : function() {
                  this.showAsText();
               }.createDelegate( this ),
               tooltip : "Download as text"
            } );

            this.downloadAsTextButton = asTextButton;
            this.downloadAsTextTaxonId = null;
            this.downloadAsTextSpecificIds = null;

            this.nowSubset = function() {
               subsetText.show();
               showAllButton.show();
            };
            var mybbar = new Ext.PagingToolbar( {
               store : pageStore, // grid and PagingToolbar using same
               // store
               displayInfo : true,
               pageSize : myPageSize,
               plugins : [ new Ext.ux.PageSizePlugin() ]
            } );
            Ext.apply( this, {
               store : pageStore,
               tbar : [ eeCombo, '->', subsetText, showAllButton, '-', asTextButton, '-', editMine, QClinkButton ],
               bbar : mybbar
            } );
            if ( this.rowExpander ) {
               Ext.apply( this, {
                  rowExpander : new Gemma.EEGridRowExpander( {
                     tpl : ""
                  } )
               } );
               this.columns.unshift( this.rowExpander );
               Ext.apply( this, {
                  plugins : this.rowExpander
               } );
            }
            Gemma.ExperimentPagingGrid.superclass.initComponent.call( this );

            // when user changes the grid's sorting, bring them back to the first page
            // this makes sense if they're on the 4th page, but may be annoying if the user is on the last page
            // since the prior is more common than the latter, I think this is a good move
            // this.on('sortchange', function(grid, sortinfo){
            // THIS DOESN'T WORK WELL BECAUSE THE GRID RELOADS WITH NEW SORT FIRST, THEN RELOADS WITH FIRST PAGE
            // grid.getBottomToolbar().moveFirst();
            // });

            // if the user is an admin, show the status column
            var isAdmin = (Ext.get( 'hasAdmin' )) ? Ext.get( 'hasAdmin' ).getValue() : false;
            this.adjustForIsAdmin( isAdmin );

            Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {
               this.adjustForIsAdmin( isAdmin );
               // when user logs in, reload the grid in case they can see more experiments now
               this.getStore().reload( this.getStore().lastOptions );

            }, this );
            Gemma.Application.currentUser.on( "logOut", function() {

               // update the column model to hide the status column
               this.adjustForIsAdmin( false );

               // when user logs out, reload the grid in case they can see fewer experiments now
               this.getStore().reload( this.getStore().lastOptions );

            }, this );

         }, // end of initComponent
         // make changes based on whether user is admin or not
         adjustForIsAdmin : function( isAdmin ) {
            if ( isAdmin ) {
               // if user is admin, update the column model to show the status column
               var index = this.getColumnModel().findColumnIndex( 'troubled' );
               this.getColumnModel().setHidden( index, !isAdmin );

               index = this.getColumnModel().findColumnIndex( 'dateLastUpdated' );
               this.getColumnModel().setHidden( index, !isAdmin );
            }

         }
      } );
