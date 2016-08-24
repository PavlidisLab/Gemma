Ext.namespace( 'Gemma' );
/**
 * Used in the showBioAssaysFromExpressionExperiment.html page
 * 
 * @class Gemma.BioAssayGrid
 * @extends Gemma.GemmaGridPanel
 * @version $Id$
 * @author Paul
 */
Gemma.BioAssayGrid = Ext
   .extend(
      Gemma.GemmaGridPanel,
      {
         collapsible : false,
         loadMask : true,
         defaults : {
            autoScroll : true
         },
         detectedOutlierIds : [],
         height : 500,
         width : 800,
         autoScroll : true,

         autoExpandColumn : 'description',

         record : Ext.data.Record.create( [ {
            name : "id",
            type : "int"
         }, {
            name : "name",
            type : "string"
         }, {
            name : "description",
            type : "string"
         }, {
            name : "outlier",
            type : "boolean"
         }, {
            name : "userFlaggedOutlier",
            type : "boolean"
         }, {
            name : "predictedOutlier",
            type : "boolean"
         } ] ),

         initComponent : function() {

            Ext.apply( this, {
               tbar : new Ext.Toolbar( {
                  items : [ {
                     xtype : 'button',
                     text : 'Save outlier changes',
                     id : 'bioassay-outlier-save-button',
                     handler : function( b, e ) {

                        // FIXME set this up so we can revert outliers as well.
                        var outliers = [];
                        this.store.each( function( record, id ) {
                           if ( record.get( 'userFlaggedOutlier' ) ) {
                              outliers.push( record.get( 'id' ) );
                           }
                        } );

                        if ( outliers.length > 0 ) {
                           Ext.getCmp( 'eemanager' ).markOutlierBioAssays( outliers );
                        }
                     }.createDelegate( this )
                  } ]
               } ),
               store : new Ext.data.Store( {
                  proxy : new Ext.data.DWRProxy( {
                     apiActionToHandlerMap : {
                        read : {
                           dwrFunction : BioAssayController.getBioAssays
                        }
                     },
                     getDwrArgsFunction : function( request, recordDataArray ) {
                        if ( request.options.params && request.options.params instanceof Array ) {
                           return request.options.params;
                        }
                        return [ this.eeId ];
                     }
                  } ),
                  reader : new Ext.data.ListRangeReader( {
                     id : "id"
                  }, this.record )
               } )
            } );

            Ext.apply( this, {
               columns : [ {
                  id : 'name',
                  header : "Name",
                  dataIndex : "name",
                  tooltip : "Name of the bioassay",
                  scope : this,
                  // width : 80,
                  width : 0.15,
                  sortable : true,
                  renderer : this.nameRenderer
               }, {
                  id : 'description',
                  header : "Description",
                  dataIndex : "description",
                  tooltip : "The descriptive name of the assay, usually supplied by the submitter",
                  // width : 120,
                  width : 0.45,

                  scope : this,
                  sortable : true,
                  renderer : this.descRenderer,
               } ]
            } );

            var isAdmin = Gemma.SecurityManager.isAdmin();

            if ( isAdmin ) {
               /*
                * CheckColumn::onMouseDown() sets the outlier status in the record.
                */
               outlierChx = new Ext.ux.grid.CheckColumn( {
                  header : "Mark outlier",
                  dataIndex : 'userFlaggedOutlier',
                  tooltip : 'Check to indicate this sample is an outlier',
                  width : 0.15
               } );

               this.columns.push( {
                  header : "Is outlier",
                  dataIndex : "id",
                  renderer : this.isOutlierRender,
                  width : 0.15
               } );

               this.columns.push( outlierChx );
               this.plugins = [ outlierChx ]; // needed to allow editing.
            }

            var me = this;

            Gemma.BioAssayGrid.superclass.initComponent.call( this );

            this.getStore().on( "load", function( store, records, options ) {
               this.doLayout.createDelegate( this );
            }, this );

            if ( this.eeId ) {
               this.getStore().load( {
                  params : [ this.eeId ]
               } );
            }

         },

         /**
          * @memberOf Gemma.BioAssayGrid
          */
         nameRenderer : function( value, metadata, record, row, col, ds ) {
            return "<a  title=\"Show details of this bioassay\" style='cursor:pointer' href=\"/Gemma/bioAssay/showBioAssay.html?id="
               + record.get( 'id' ) + "\">" + record.get( 'name' ) + "</a>";
         },

         descRenderer : function( value, metadata, record, row, col, ds ) {
            var color = 'black';

            if ( record.get( 'outlier' ) ) {
               color = 'red';
               return " <font color='" + color + "'>Removed as an outlier;" + record.get( 'name' ) + "</font>";
            }

            if ( record.get( 'predictedOutlier' ) ) {
               color = 'red';
               return "<font color='" + color + "'>Predicted outlier; " + record.get( 'name' ) + "</font>";
            }

            return record.get( 'name' );

         },

         isOutlierRender : function( value, metadata, record, row, col, ds ) {
            if ( record.get( 'outlier' ) ) {
               return "<img title=\"Is an outlier\" src=\"/Gemma/images/icons/stop.png\"/>";
            }
            return "";
         },

      } );