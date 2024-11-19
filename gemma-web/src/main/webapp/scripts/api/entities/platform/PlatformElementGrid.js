Ext.namespace( 'Gemma' );

/**
 *
 * @class Gemma.PlatformElementGrid
 * @extends Ext.grid.GridPanel
 */
Gemma.PlatformElementGrid = Ext
   .extend(
      Ext.grid.GridPanel,
      {

         autoExpandColumn : 'platform',

         loadMask : {
            msg : "Loading ..."
         },

         viewConfig : {
            forceFit : true
         },

         arrayDesignId : null,
         paging : false,
         loadOnlyOnRender : false,

         /**
          * Show first batch of data.
          *
          * @param {Object}
          *           isArrayDesign
          * @param {Object}
          *           id
          */
         loadData : function() {
            if ( this.arrayDesignId ) {
               this.showArrayDesignProbes( this.arrayDesignId );
            } else if ( this.geneId ) {
               this.showGeneProbes( this.geneId );
            } else {
               this.showprobes( this.csIds );
            }

            // reset the toolbar.
            if ( this.paging ) {
               this.paging.getEl().unmask();
               this.paging.getEl().select( "input,a,button" ).each( function( e ) {
                  e.dom.disabled = false;
               } );
            }
         },

         arraylink : function( data, metadata, record, row, column, store ) {
            return "<a ext:qtip='" + record.get( "arrayDesignShortName" )
               + " Click to view platform details' href='" + Gemma.CONTEXT_PATH + "/arrays/showArrayDesign.html?id="
               + record.get( "arrayDesignId" ) + "'>" + record.get( "arrayDesignName" ) + "</a>";
         },

         // CompositeSequenceMapValueObject

         convertgps : function( d ) { // not used
            var r = "";
            for ( var gp in d) {
               r = r + d[gp].name + ",";
            }
            r = r.substr( 0, r.length - 1 );
            return r;
         },

         convertgenes : function( d ) {
            var r = "";
            var count = 0;
            for ( var g in d) {
               if ( d[g].id ) {
                  r = r
                     + "&nbsp;<a  title='View gene details (opens new window)' target='_blank' href='" + Gemma.CONTEXT_PATH + "/gene/showGene.html?id="
                     + d[g].id + "'>" + d[g].officialSymbol + "</a>,";
                  ++count;
               }
            }
            if ( count > 3 ) {
               r = "(" + count + ")" + r;
            }
            if ( r.length > 0 ) {
               r = r.substr( 0, r.length - 1 );// trim tailing comma.
            }
            return r;
         },

         sequencelink : function( data, metadata, record, row, column, store ) {
            if ( data === "null" ) {
               return "<a title='[unavailable]'>-</a>";
            }
            return data;
         },

         record : Ext.data.Record.create( [ {
            name : "compositeSequenceId",
            type : "int"
         }, {
            name : "compositeSequenceName",
            type : "string"
         }, {
            name : "arrayDesignShortName",
            type : "string"
         }, {
            name : "arrayDesignName",
            type : "string"
         }, {
            name : "arrayDesignId",
            type : "int"
         }, {
            name : "bioSequenceId",
            type : "int"
         }, {
            name : "bioSequenceName",
            type : "string"
         }, {
            name : "numBlatHits",
            type : "int"
         }, {
            name : "bioSequenceNcbiId",
            type : "string"
         }, {
            name : "genes"
         } ] ),

         /**
          * @memberOf Gemma.PlatformElementGrid
          */
         initComponent : function() {

            var reader = new Ext.data.ListRangeReader( {
               id : "compositeSequenceId"
            }, this.record );

            this.isArrayDesign = this.arrayDesignId != null;

            var proxy;
            if ( this.isArrayDesign ) {
               proxy = new Ext.data.DWRProxy( ArrayDesignController.getCsSummaries );
            } else if ( this.geneId ) {
               proxy = new Ext.data.DWRProxy( CompositeSequenceController.getGeneCsSummaries );
            } else {
               proxy = new Ext.data.DWRProxy( CompositeSequenceController.getCsSummaries );
            }

            proxy.on( "loadexception", this.handleLoadError.createDelegate( this ) );

            Ext
               .apply(
                  this,
                  {

                     columns : [
                                {
                                   sortable : true,
                                   id : 'platform',
                                   header : "Platform",
                                   width : 100,
                                   dataIndex : "arrayDesignShortName",
                                   renderer : this.arraylink.createDelegate( this ),
                                   tooltip : "Name of platform (click for details - leaves this page)"
                                },
                                {
                                   sortable : true,
                                   id : 'probe',
                                   header : "Element Name",
                                   width : 130,
                                   dataIndex : "compositeSequenceName",
                                   renderer : ( data, metadata, record ) => {
                                      return '<a href="' + Gemma.CONTEXT_PATH + '/compositeSequence/show.html?id=' + record.data.compositeSequenceId + '">' + data + '</a>';
                                   },
                                   tooltip : "Element or probe name"
                                },
                                {
                                   sortable : true,
                                   id : 'sequence',
                                   header : "Sequence",
                                   width : 130,
                                   dataIndex : "bioSequenceName",
                                   renderer : this.sequencelink.createDelegate( this ),
                                   tooltip : "Name of sequence, may be empty for non-alignment based annotations"
                                },
                                {
                                   sortable : true,
                                   id : 'hits',
                                   header : "#Hits",
                                   width : 50,
                                   dataIndex : "numBlatHits",
                                   tooltip : "Number of high-quality BLAT alignments; will be zero for non-alignment based annotations"

                                },
                                {
                                   sortable : true,
                                   id : 'genes',
                                   header : "Genes",
                                   width : 200,
                                   dataIndex : "genes",
                                   tooltip : "Genes this element is considered to assay; if there are more than 3, the total count is provided in parentheses",
                                   renderer : this.convertgenes.createDelegate( this )
                                } ],
                     store : new Ext.data.Store( {
                        proxy : proxy,
                        reader : reader
                     } ),
                     selModel : new Ext.grid.RowSelectionModel( {
                        singleSelect : true
                     } )

                  } );

            if ( this.isArrayDesign ) {
               Ext.apply( this, {
                  tbar : [ {
                     xtype : 'textfield',
                     name : 'search-field',
                     emptyText : 'Search for elements',
                     id : 'search-field',
                     listeners : {
                        'specialkey' : {
                           fn : function( f, e ) {
                              if ( e.getKey() == e.ENTER ) {
                                 this.search();
                              }
                           }.createDelegate( this ),
                           scope : this
                        }
                     },
                     width : 100
                  }, {
                     xtype : 'button',
                     name : 'Search',
                     text : 'Search',
                     tooltip : 'Search for elements on this platform',
                     id : 'search-button',
                     handler : this.search.createDelegate( this )

                  }, {
                     xtype : 'button',
                     name : 'Reset',
                     text : 'Reset',
                     id : 'reset-button',
                     tooltip : 'Return to full list',
                     handler : this.loadData.createDelegate( this )

                  } ]

               } );
            }

            Gemma.PlatformElementGrid.superclass.initComponent.call( this );

            this.getStore().on( 'load', function( d ) {
               this.fireEvent( 'select', d[0] );
            } );

            this.getSelectionModel().on( "rowselect", function() {
               var sm = this.getSelectionModel();
               this.fireEvent( 'select', sm.getSelected() );
            }.createDelegate( this ) );

            if ( !this.loadOnlyOnRender ) {
               this.loadData();
            } else {
               this.on( 'render', function() {
                  this.loadData();
               } );
            }

         },

         /**
          * Event handler for searches. Update the lower grid.
          *
          * @param {Object}
          *           event
          */
         search : function( event ) {
            if ( !this.isArrayDesign ) {
               return;
            }
            var id = this.arrayDesignId;
            var query = Ext.getCmp( 'search-field' ).getValue();

            // swap out table proxy, temporarily.
            var oldprox = this.getStore().proxy;
            this.getStore().proxy = new Ext.data.DWRProxy( CompositeSequenceController.search );

            this.getStore().load( {
               params : [ query, id ]
            } );
            this.getStore().proxy = oldprox;
         },

         showprobes : function( ids ) {
            // note how we pass the new array in directly, without wrapping it in an
            // object first. We're not returning an object, just a bare array.
            this.getStore().load( {
               params : [ ids ],
               callback : function( r, options, success, scope ) {
                  if ( success ) {
                     // Ext.DomHelper.overwrite( "messages", this.getCount() + " elements shown" );
                  } else {
                     Ext.Msg.alert( "Error", "There was an error." );
                  }
               }
            } );
         },

         showGeneProbes : function( geneId ) {
            this.getStore().load( {
               params : [ geneId ],
               callback : function( r, options, success, scope ) {
                  if ( success ) {
                     // Ext.DomHelper.overwrite( "messages", this.getCount() + " elements shown" );
                  } else {
                     Ext.Msg.alert( "Error", "There was an error." );
                  }
               }
            } );
         },

         showArrayDesignProbes : function( id ) {
            this.getStore().load( {
               params : [ {
                  id : id,
                  classDelegatingFor : "ArrayDesign"
               } ],
               callback : function( r, options, success, scope ) {
                  if ( !success ) {
                     Ext.Msg.alert( "Error", "There was an error." );
                  }
               }
            } );
         },

         handleLoadError : function( scope, b, message, exception ) {
            Ext.DomHelper.overwrite( "messages", {
               tag : 'img',
               src : Gemma.CONTEXT_PATH + '/images/iconWarning.gif'
            } );
            Ext.DomHelper.overwrite( "messages", {
               tag : 'span',
               html : "There was an error while loading data: " + exception
                  + "<br />. Try again or contact the webmaster."
            } );
         }

      } );

Ext.reg( 'probegrid', Gemma.PlatformElementGrid );