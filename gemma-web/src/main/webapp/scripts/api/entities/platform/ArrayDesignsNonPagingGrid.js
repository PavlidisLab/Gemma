Ext.namespace( 'Gemma' );

Gemma.ArrayDesignsStore = Ext.extend( Ext.data.Store, {
   reader : new Ext.data.JsonReader( {
      idProperty : "id", // same as default,
      // used by store to set its sortInfo
      sortInfo : {
         field : "name",
         direction : "ASC"
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
         name : "taxon",
         type : "string"
      }, {
         name : "expressionExperimentCount"
      }, {
         name : "summaryTable"
      }, {
         name : "createDate",
         dateFormat : "timestamp",
         type : "date"
      }, {
         name : "lastSequenceUpdate",
         dateFormat : "timestamp",
         type : "date"
      }, {
         name : "lastRepeatMask",
         dateFormat : "timestamp",
         type : "date"
      }, {
         name : "lastSequenceAnalysis",
         dateFormat : "timestamp",
         type : "date"
      }, {
         name : "lastGeneMapping",
         dateFormat : "timestamp",
         type : "date"
      }, {
         name : "color",
         type : "string"
      }, {
         name : "isMergee"
      }, {
         name : "isMerged"
      }, {
         name : "isSubsumed"
      }, {
         name : "isSubsumer"
      }, {
         name : "troubled"
      }, {
         name : "isAffymetrixAltCdf"
      }, {
         name : "troubleDetails"
      }, {
         name : "blackListed",
         type: "boolean"
      }, {
         name : "needsAttention"
      }, {
         name : "curationNote"
      }, {
         name : "statusArray",
         convert : function( v, record ) {
            return [ record.troubled, record.blackListed, record.isMerged, record.isMergee, record.isSubsumed, record.isSubsumer, record.isAffymetrixAltCdf ];
         },
         sortDir : 'DESC',
         sortType : function( value ) {
            var i;
            var count = 0;
            for (i = 0; i < value.length; i++) {
               if ( value[i] ) {
                  // want sorting to also group by status type & put troubled at top
                  count += Math.pow( value.length - i, value.length - i );
               }
            }
            return count;
         }
      }, {
         name : "designElementCount",
         defaultValue : '[not avail.]',
         useNull : true,
         convert : function( v, record ) {
            if ( v === null ) {
               return '<span style="color:grey">[not avail.]</span>';
            }
            return v;
         }
      }, {
         name : "numProbeSequences",
         defaultValue : '[not avail.]',
         useNull : true,
         convert : function( v, record ) {
            if ( v === null ) {
               return '<span style="color:grey">[not avail.]</span>';
            }
            return v;
         }
      }, {
         name : "numProbeAlignments",
         defaultValue : '[not avail.]',
         useNull : true,
         convert : function( v, record ) {
            if ( v === null ) {
               return '<span style="color:grey">[not avail.]</span>';
            }
            return v;
         }
      }, {
         name : "numProbesToGenes",
         defaultValue : '[not avail.]',
         useNull : true,
         convert : function( v, record ) {
            if ( v === null ) {
               return '<span style="color:grey">[not avail.]</span>';
            }
            return v;
         }
      }, {
         name : "numGenes",
         defaultValue : '[not avail.]',
         useNull : true,
         convert : function( v, record ) {
            if ( v === null ) {
               return '<span style="color:grey">[not avail.]</span>';
            }
            return v;
         }
      }, {
         name : "dateCached"
      }, {
         name : 'cannotBeDeleted',
         convert : function( v, record ) {
            /*
             * FIXME: the non-merged constraint might not be necessary. We should definitely not delete mergees even if
             * they have 0 experiments.
             */
            return !(record.expressionExperimentCount === 0 && !record.isMerged && !record.isMergee && record.switchedExpressionExperimentCount === 0);
         }
      }, {
         name : 'switchedExpressionExperimentCount'
      } ]
   } )
} );

Gemma.ArrayDesignsNonPagingGrid = Ext
   .extend(
      Ext.grid.GridPanel,
      {
         // width: 1000,
         autoScroll : true,
         stripeRows : true,
         rowExpander : null,
         emptyText : Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.emptyText,
         viewConfig : {
            forceFit : true
         },
         myPageSize : 50,
         title : 'Platforms',
         totalCount : 0,
         showOrphans : true,
         showMergees : true,
         showTroubled : true,

         /**
          * @memberOf Gemma.ArrayDesignsNonPagingGrid
          */
         loadArrayDesigns : function( adIds, extraCallback, extraCallbackParams ) {
            if ( !this.loadMask ) {
               this.loadMask = new Ext.LoadMask( this.getEl(), {
                  msg : Gemma.StatusText.Loading.arrayDesigns
               } );
            }
            this.loadMask.show();
            ArrayDesignController.loadArrayDesignsForShowAll( adIds, function( arrayDesigns ) {
               this.loadMask.hide();
               this.getStore().loadData( arrayDesigns );
               this.setTitle( arrayDesigns.length + ((arrayDesigns.length === 1) ? " Platform" : " Platforms") );
               this.totalCount = arrayDesigns.length;
               this.getStore().applyMultiFilters();
               if ( extraCallback )
                  extraCallback( extraCallbackParams );
            }.createDelegate( this ) );
         },
         initComponent : function() {

            this.showAll = !(document.URL.indexOf( "?" ) > -1 && (document.URL.indexOf( "id=" ) > -1));
            this.idSubset = null;
            var filterById = false;

            if ( !this.showAll ) {
               var subsetDetails = document.URL.substr( document.URL.indexOf( "?" ) + 1 );
               var param = Ext.urlDecode( subsetDetails );
               if ( param.id ) {
                  this.idSubset = param.id.split( ',' );
                  filterById = true;
               }
               if ( param.showOrph ) {
                  this.showOrphans = param.showOrph;
               }
               if ( param.showMerg ) {
                  this.showMergees = param.showMerg;
               }
            }

            Ext.apply( this, {
               store : new Gemma.ArrayDesignsStore()
            } );

            // Create RowActions Plugin
            this.action = new Ext.ux.grid.RowActions( {
               header : 'Actions',
               dataIndex : 'actions',
               tooltip : Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.actionsColumnTT,
               // ,autoWidth:false
               // ,hideMode:'display'
               keepSelection : true,
               actions : [ {
                  iconCls : 'icon-refresh',
                  tooltip : 'Refresh'
               }, {
                  iconCls : 'icon-cross',
                  tooltip : 'Delete platform',
                  hideIndex : 'cannotBeDeleted'// hide if == true
               } ],
               callbacks : {
                  'icon-cross' : function( grid, record, action, row, col ) {
                  }
               }
            } );

            this.action.on( {
               action : function( grid, record, action, row, col ) {
                  if ( action === 'icon-cross' ) {
                     Ext.Msg.confirm( Gemma.HelpText.CommonWarnings.Deletion.title, String.format(
                        Gemma.HelpText.CommonWarnings.Deletion.text, 'platform' ), function( btnId ) {
                        if ( btnId === 'yes' ) {

                           var callParams = [];
                           callParams.push( {
                              id : record.id
                           } );
                           callParams.push( {
                              callback : function( taskId ) {
                                 var task = new Gemma.ObservableSubmittedTask( {
                                    'taskId' : taskId
                                 } );
                                 task.on( 'task-completed', function( payload ) {
                                    window.location.reload();
                                 } );
                                 task.showTaskProgressWindow( {
                                    showLogButton : true
                                 } );
                              }.createDelegate( this )
                           } );

                           ArrayDesignController.remove.apply( this, callParams );

                        }
                     } );
                  } else if ( action === 'icon-refresh' ) {
                     updateArrayDesignReport( record.id, grid ); // function in arrayDesign.js
                  }
               },
               // You can cancel the action by returning false from this
               // event handler.
               beforeaction : function( grid, record, action, row, col ) {
                  return true;
               }
            } );

            this.rowExpander = new Ext.grid.RowExpander( {
               enableCaching : false,
               tpl : Gemma.Widget.tpl.ArrayDesignsNonPagingGrid.rowDetails
            } );

            this.on( 'reportUpdated', function( id ) {
               var extraCallback = function( arr ) {
                  var grid = arr[0];
                  var id = arr[1];
                  grid.rowExpander.collapseAll();
                  record = grid.getStore().getById( id );
                  grid.rowExpander.expandRow( grid.getStore().indexOf( record ) );
               };

               this.loadArrayDesigns( this.idSubset, extraCallback, [ this, id ] );

            } );

            var cellTips = new Ext.ux.plugins.grid.CellToolTips( [ {
               field : 'name',
               tpl : '{name}'
            }, {
               field : 'shortName',
               tpl : '{shortName}'
            } ] );
            Ext
               .apply(
                  this,
                  {
                     plugins : [ this.action, this.rowExpander, cellTips ],
                     colModel : new Ext.grid.ColumnModel(
                        {
                           defaults : {
                              sortable : true
                           },
                           columns : [/*
                                        * { // for testing id:'id', header: "db id", dataIndex: 'id', sortable:true,
                                        * width: 0.1 //viewConfig.forceFit resizes based on relative widths },
                                        */
                              this.rowExpander,
                              {
                                 id : 'name',
                                 header : "Platform Name",
                                 dataIndex : 'name',
                                 width : 0.3, // viewConfig.forceFit resizes based on relative widths,
                                 renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                    return (value && record) ? '<a target="_blank" href="' + ctxBasePath
                                       + '/arrays/showArrayDesign.html?id=' + record.id + '">' + value + '</a>' : '';
                                 }
                              },
                              {
                                 header : "Status",
                                 dataIndex : 'statusArray',
                                 width : 0.05,
                                 renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                    var statusString = "";

                                    if ( record.get( 'troubled' ) ) {
                                       statusString += '<i class="red fa fa-exclamation-triangle fa-lg" ext:qtip="'
                                          + record.get( 'troubleDetails' ) + '"></i>&nbsp;';
                                    }

                                    if ( record.get( 'blackListed' ) ) {
                                        statusString += '<i class="black fa fa-exclamation-triangle fa-lg" ext:qtip="Blacklisted"></i>&nbsp;';
                                    }

                                    if ( record.get( 'isMerged' ) ) {
                                       statusString += '<img title="'
                                          + Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.isMergedTT + '"'
                                          + ' src="' + ctxBasePath + '/images/icons/merging_result.png"/>&nbsp;';
                                    }
                                    if ( record.get( 'isMergee' ) ) {
                                       statusString += '<img title="'
                                          + Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.isMergeeTT + '"'
                                          + ' src="' + ctxBasePath + '/images/icons/arrow_merge.png"/>&nbsp;';
                                    }
                                    if ( record.get( 'isSubsumed' ) ) {
                                       statusString += '<img title="'
                                          + Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.isSubsumedTT + '"'
                                          + ' src="' + ctxBasePath + '/images/icons/subsumed.png"/>&nbsp;';
                                    }
                                    if ( record.get( 'isSubsumer' ) ) {
                                       statusString += '<img title="'
                                          + Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.isSubsumerTT + '"'
                                          + ' src="' + ctxBasePath + '/images/icons/subsumer.png"/>';
                                    }
                                    if (record.get('isAffymetrixAltCdf')) {
                                        statusString += '&nbsp;<i class="orange fa fa-exclamation-circle fa-lg" ext:qtip="'
                                            + "This platform is an alternative to a 'standard' gene-level Affymetrix probe layout. " +
                                            "Data sets using it will be switched to the canonical one when raw data are available." + '"></i>';
                                    }
                                    if (record.get('switchedExpressionExperimentCount') > 0) {
                                       statusString += '&nbsp;<i style="color:#3366cc" class="fa fa-exclamation-circle fa-lg" ext:qtip="'
                                       + "This platform was the original for " + record.get('switchedExpressionExperimentCount') + " experiments "
                                       + "that were switched to another, these are not part of the experiment count column" + '"></i>';
                                    }

                                    return statusString;
                                 }
                              },
                              {
                                 header : "Curation status",
                                 tooltip : "",
                                 dataIndex : 'needsAttention',
                                 sortable : true,
                                 width : 0.05,
                                 hidden : false,
                                 renderer : Gemma.Renderers.curationRenderer
                              },
                              {
                                 header : "Short Name",
                                 dataIndex : 'shortName',
                                 width : 0.07
                              },
                              {
                                 header : "Taxon",
                                 dataIndex : 'taxon',
                                 width : 0.07
                              },
                              {
                                 header : "Expts",
                                 dataIndex : 'expressionExperimentCount',
                                 width : 0.03,
                                 tooltip : 'Number of experiments in Gemma that use this design'
                              },
                              {
                                 header : "Created",
                                 dataIndex : 'createDate',
                                 width : 0.07,
                                 sortDir : 'DESC',
                                 xtype : 'datecolumn',
                                 format : 'Y-m-d'
                              },
                              {
                                 header : "Seq. Update",
                                 dataIndex : 'lastSequenceUpdate',
                                 width : 0.07,
                                 sortDir : 'DESC',
                                 xtype : 'datecolumn',
                                 format : 'Y-m-d'
                              },
                              {
                                 header : "Rep. mask",
                                 dataIndex : 'lastRepeatMask',
                                 width : 0.07,
                                 sortDir : 'DESC',
                                 xtype : 'datecolumn',
                                 format : 'Y-m-d'
                              },
                              {
                                 header : "Seq. Analysis",
                                 dataIndex : 'lastSequenceAnalysis',
                                 width : 0.07,
                                 sortDir : 'DESC',
                                 xtype : 'datecolumn',
                                 format : 'Y-m-d'
                              },
                              {
                                 header : "Gene Mapping",
                                 dataIndex : 'lastGeneMapping',
                                 width : 0.07,
                                 sortDir : 'DESC',
                                 xtype : 'datecolumn',
                                 format : 'Y-m-d'
                              },
                              {
                                 header : "Channels",
                                 dataIndex : 'color',
                                 width : 0.03,
                                 renderer : function( value, metaData, record, rowIndex, colIndex, store ) {
                                    return (value === "ONECOLOR") ? "1" : (value === "TWOCOLOR") ? "2"
                                       : '<span title="' + value + '">' + value + '</span>';
                                 }
                              }, this.action ]
                        } )
                  } );

            this.getStore().addMultiFilter( {
               name : 'orphanFilter',
               active : !this.showOrphans,
               fn : function( record ) {
                  return (record.get( 'expressionExperimentCount' ) && record.get( 'expressionExperimentCount' ) > 0);
               }
            } );
            this.getStore().addMultiFilter( {
               name : 'mergeeFilter',
               active : !this.showMergees,
               fn : function( record ) {
                  return !record.get( 'isMergee' );
               }
            } );
            this.getStore().addMultiFilter( {
               name : 'troubledFilter',
               active : false,
               fn : function( record ) {
                  return !record.get( 'troubled' );
               }

            } );
             this.getStore().addMultiFilter( {
                 name : 'affyAltFilter',
                 active : false,
                 fn : function( record ) {
                     return !record.get( 'isAffymetrixAltCdf' );
                 }

             } );

            var textFilterFun = function( query ) {
               var value = new RegExp( Ext.escapeRe( query ), 'i' );
               return function( record ) {
                  // go through every visible field, if it matches the query text
                  // show the row
                  var fieldContents;
                  for ( var field in record.data) {
                     fieldContents = record.data[field];
                     if ( value.test( fieldContents ) ) {
                        return true;
                     }
                  }
                  return false;
               };
            };

            Ext.apply( this, {
               clearFilter : function() {
                  this.getTopToolbar().searchInGrid.reset();
                  this.getStore().removeMultiFilter( 'textQueryFilter' );
                  this.getStore().applyMultiFilters();
                  this.getTopToolbar().fieldClearBtn.disable();
               },
               tbar : new Ext.Toolbar( {
                  items : [ {
                     xtype : 'textfield',
                     ref : 'searchInGrid',
                     tabIndex : 1,
                     enableKeyEvents : true,
                     emptyText : 'Enter search term',
                     listeners : {
                        "keyup" : {
                           fn : function() {
                              this.getTopToolbar().fieldClearBtn.enable();
                              this.getStore().removeMultiFilter( 'textQueryFilter' );
                              this.getStore().addMultiFilter( {
                                 name : 'textQueryFilter',
                                 active : true,
                                 fn : textFilterFun( this.getTopToolbar().searchInGrid.getValue() )
                              } );
                              this.getStore().applyMultiFilters();

                           },
                           scope : this,
                           options : {
                              delay : 100
                           }
                        }
                     }
                  }, {
                     ref : 'fieldClearBtn',
                     disabled : true,
                     tooltip : 'Clear your search',
                     icon : ctxBasePath + '/images/icons/cross.png',
                     handler : function() {
                        this.clearFilter();
                     },
                     scope : this
                  }, '-', {
                     ref : 'refreshButton',
                     text : 'Refresh',
                     icon : ctxBasePath + '/images/icons/arrow_refresh_small.png',
                     tooltip : 'Refresh the contents of this table',
                     handler : function() {
                        this.clearFilter();
                        this.loadArrayDesigns( this.idSubset );
                     },
                     scope : this
                  }, '->', '-', {
                     ref : 'orphansToggle',
                     boxLabel : 'Hide Orphans',
                     checked : !this.showOrphans,
                     xtype : 'checkbox',
                     style : 'margin-top:0px',
                     tooltip : Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.hideOrphansTT,
                     handler : function( checkbox, isChecked ) {
                        if ( !isChecked ) {

                           this.showOrphans = true;
                           this.getStore().deactivateMultiFilter( 'orphanFilter' );
                           this.getStore().applyMultiFilters();

                        } else {

                           this.showOrphans = false;
                           this.getStore().activateMultiFilter( 'orphanFilter' );
                           this.getStore().applyMultiFilters();
                        }

                     },
                     scope : this
                  }, '-', {
                     ref : 'mergedToggle',
                     boxLabel : 'Hide mergees',
                     checked : !this.showMergees,
                     hidden : false,
                     style : 'margin-top:0px',
                     xtype : 'checkbox',
                     tooltip : Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.hideMergeeTT,
                     handler : function( checkbox, isChecked ) {
                        if ( !isChecked ) {

                           this.showMergees = true;
                           this.getStore().deactivateMultiFilter( 'mergeeFilter' );
                           this.getStore().applyMultiFilters();

                        } else {
                           this.showMergees = false;
                           this.getStore().activateMultiFilter( 'mergeeFilter' );
                           this.getStore().applyMultiFilters();
                        }

                     },
                     scope : this
                  }, '-', {
                     ref : 'troubledToggle',
                     boxLabel : 'Hide Troubled',
                     checked : !this.showTroubled,
                     hidden : true,
                     style : 'margin-top:0px',
                     xtype : 'checkbox',
                     tooltip : Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.hideTroubledTT,
                     handler : function( checkbox, isChecked ) {
                        if ( !isChecked ) {

                           this.showTroubled = true;
                           this.getStore().deactivateMultiFilter( 'troubledFilter' );
                           this.getStore().applyMultiFilters();

                        } else {
                           this.showTroubled = false;
                           this.getStore().activateMultiFilter( 'troubledFilter' );
                           this.getStore().applyMultiFilters();
                        }

                     },
                     scope : this
                  }, '-', {
                      ref : 'affyAltToggle',
                      boxLabel : 'Hide Affy. Alts',
                      checked : !this.showOrphans,
                      xtype : 'checkbox',
                      style : 'margin-top:0px',
                      tooltip : Gemma.HelpText.WidgetDefaults.ArrayDesignsNonPagingGrid.hideAffyAltTT,
                      handler : function( checkbox, isChecked ) {
                          if ( !isChecked ) {

                              this.showOrphans = true;
                              this.getStore().deactivateMultiFilter( 'affyAltFilter' );
                              this.getStore().applyMultiFilters();

                          } else {

                              this.showOrphans = false;
                              this.getStore().activateMultiFilter( 'affyAltFilter' );
                              this.getStore().applyMultiFilters();
                          }

                      },
                      scope : this
                  }, '-', {
                     ref : 'ArrayDesignsSummaryWindowBtn',
                     text : 'Platforms Summary',
                     cls : 'x-toolbar-standardbutton',
                     hidden : true,
                     handler : function() {
                        if ( Ext.WindowMgr.get( 'ArrayDesignsSummaryWindow' ) ) {
                           Ext.WindowMgr.bringToFront( 'ArrayDesignsSummaryWindow' );
                        } else {
                           new Gemma.ArrayDesignsSummaryWindow( {
                              id : 'arrayDesignsSummaryWindow'
                           } ).show();
                        }
                     },
                     scope : this
                  } ]
               } )
            } );

            Gemma.ArrayDesignsNonPagingGrid.superclass.initComponent.call( this );

            this.on( 'render', function() {
               this.loadArrayDesigns( this.idSubset );
            }, this );

            this.getStore().on( 'datachanged', function( store ) {
               this.setTitle( this.getStore().getCount() + " of " + this.totalCount + " Platforms" );
            }, this );

            // if the user is an admin, show the status column
            var isAdmin = (Ext.get( 'hasAdmin' )) ? Ext.get( 'hasAdmin' ).getValue() : false;
            this.adjustForIsAdmin( isAdmin );

            Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {

               this.adjustForIsAdmin( isAdmin );

            }, this );
            Gemma.Application.currentUser.on( "logOut", function() {

               this.adjustForIsAdmin( false );

            }, this );

         }, // end of initComponent
         // make changes based on whether user is admin or not
         adjustForIsAdmin : function( isAdmin ) {

            // if user is admin, update the column model to show the status column
            var colModel = this.getColumnModel();

            var index = this.getColumnModel().findColumnIndex( 'lastSequenceUpdate' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'lastRepeatMask' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'lastSequenceAnalysis' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'lastGeneMapping' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'color' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'actions' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'needsAttention' );
            colModel.setHidden( index, !isAdmin );

            index = this.getColumnModel().findColumnIndex( 'createDate' ); // maybe we can expose this.
            colModel.setHidden( index, !isAdmin );

            if ( !isAdmin ) {
               this.getStore().activateMultiFilter( 'troubledFilter' );
               this.getStore().applyMultiFilters();
            }

            this.getTopToolbar().troubledToggle.setVisible( isAdmin );
            this.getTopToolbar().ArrayDesignsSummaryWindowBtn.setVisible( isAdmin );
         }
      } );

Gemma.ArrayDesignsSummaryWindow = Ext
   .extend(
      Ext.Window,
      {
         title : 'Summary for All Platforms',
         shadow : false,
         loadText : function() {
            if ( !this.loadMask ) {
               this.loadMask = new Ext.LoadMask( this.getEl(), {
                  msg : "Loading Summary ..."
               } );
            }
            this.loadMask.show();
            ArrayDesignController.loadArrayDesignsSummary( function( arrayDesignSummary ) {
               if ( arrayDesignSummary === null ) {
                  arrayDesignSummary = this.defaultData;
               } else {
                  for (field in arrayDesignSummary) {
                     if ( arrayDesignSummary[field] === null ) {
                        arrayDesignSummary[field] = '<span style="color:grey">[Not avail.]</span>';
                     }
                  }
               }

               this.update( arrayDesignSummary );
               this.loadMask.hide();
            }.createDelegate( this ) );
         },
         tpl : '<a href="'
            + ctxBasePath
            + '/arrays/generateArrayDesignSummary.html" onclick="return confirm(\'Regenerate report for all platforms?\');">Regenerate this report</a><br><br>'
            + Gemma.Widget.tpl.ArrayDesignsNonPagingGrid.rowDetails,
         padding : 7,
         defaultData : {
            numProbeSequences : '<span style="color:grey">[Not avail.]</span>',
            numProbeAlignments : '<span style="color:grey">[Not avail.]</span>',
            numProbesToGenes : '<span style="color:grey">[Not avail.]</span>',
            numGenes : '<span style="color:grey">[Not avail.]</span>',
            dateCached : '<span style="color:grey">[Not avail.]</span>'
         },
         initComponent : function() {

            Gemma.ArrayDesignsSummaryWindow.superclass.initComponent.call( this );
            this.on( 'render', function() {
               this.loadText();
            }, this );
         }// end of initComponent
      } );