Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';
/**
 * 
 * Panel containing the most interesting info about an experiment. Used as one tab of the EE page
 * 
 * pass in the experiment id as 'eeid'
 * 
 * @class Gemma.ExpressionExperimentDetails
 * @extends Ext.Panel
 * 
 */

Gemma.ExpressionExperimentQuantitationTypeGrid = Ext.extend( Ext.grid.GridPanel, {
   border : false,
   viewConfig : {
      forceFit : true
   },
   stripeRows : true,
   eeid : null, // needs to be set in configs on creation
   store : new Ext.data.SimpleStore( {
      fields : Ext.data.Record.create( [ {
         name : "id"
      }, {
         name : "expressionExperimentId"
      }, {
         name : "name"
      }, {
         name : "description"
      }, {
         name : "isRecomputedFromRawData"
      }, {
         name : "isBatchCorrected"
      }, {
         name : "generalType"
      }, {
         name : "isBackground"
      }, {
         name : "isBackgroundSubtracted"
      }, {
         name : "isMaskPreferred"
      }, {
         name : "isNormalized"
      }, {
         name : "isPreferred"
      }, {
         name : "isRatio"
      }, {
         name : "representation"
      }, {
         name : "scale"
      }, {
         name : "type"
      }, {
         name : "vectorType"
      } ] ),
      sortInfo : {
         field : 'name',
         direction : 'ASC'
      }
   } ),
   colModel : new Ext.grid.ColumnModel( {
      defaults : {
         sortable : true,
         renderer : function( value, metadata, record, rowIndex, colIndex, store ) {
            metadata.attr = 'ext:qtip="' + value + '"';
            if ( value === true ) {
               return "yes";
            }
            if ( value === false ) {
               return "no";
            }
            return value;
         }
      },
      columns : [ {
         id : 'name',
         header : "Name",
         dataIndex : "name",
         tooltip : 'Name',
         renderer : function( value, metadata, record, rowIndex, colIndex, store ) {
            var downloadQuantitationUrl;
            if ( record.data.vectorType === 'ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector' ) {
               downloadQuantitationUrl = Gemma.CONTEXT_PATH + '/rest/v2/datasets/' + record.data.expressionExperimentId + '/data/processed?download=true';
            } else if ( record.data.vectorType === 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector' ) {
               downloadQuantitationUrl = Gemma.CONTEXT_PATH + '/rest/v2/datasets/' + record.data.expressionExperimentId + '/data/raw?quantitationType=' + record.data.id + '&download=true';
            } else if ( record.data.vectorType === 'ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector' ) {
               downloadQuantitationUrl = Gemma.CONTEXT_PATH + '/rest/v2/datasets/' + record.data.expressionExperimentId + '/data/singleCell?quantitationType=' + record.data.id + '&download=true';
            }
            if ( downloadQuantitationUrl ) {
               return '<a href="' + downloadQuantitationUrl + '">' + value + "</a>";
            } else {
               return value;
            }
         }
      }, {
         id : 'desc',
         header : "Description",
         tooltip : "Description",
         dataIndex : 'description',
         width : 200
      }, {
         id : 'isRecomput',
         header : "Recompu?",
         tooltip : 'Is recomputed from raw data?',
         dataIndex : 'isRecomputedFromRawData',
         width : 40
      }, {
         id : 'isbatchcorr',
         header : "Batch corr?",
         tooltip : "Is batch corrected?",
         dataIndex : 'isBatchCorrected',
         width : 80
      }, {
         id : 'ispref',
         header : "Pref?",
         tooltip : 'Is Preferred?',
         dataIndex : 'isPreferred',
         width : 40
      }, {
         id : 'isratio',
         header : "Ratio?",
         tooltip : "Is Ratio?",
         dataIndex : 'isRatio',
         width : 45
      }, {
         id : 'isback',
         header : "Bkgrd?",
         tooltip : 'Is Background?',
         dataIndex : 'isBackground',
         width : 50
      }, {
         id : 'isbacksub',
         header : "Bkgrd Sub.?",
         tooltip : "Is Background Subtracted?",
         dataIndex : 'isBackgroundSubtracted',
         width : 80
      }, {
         id : 'isnorm',
         header : "Norm?",
         dataIndex : 'isNormalized',
         tooltip : 'Is Normalized?',
         width : 50
      }, {
         id : 'gentyp',
         header : "General Type",
         dataIndex : 'generalType'
      }, {
         id : 'type',
         header : "Type",
         tooltip : "Type",
         dataIndex : 'type'
      }, {
         id : 'repre',
         header : "Representation",
         tooltip : "Representation",
         dataIndex : 'representation'
      }, {
         id : 'scale',
         header : "Scale",
         tooltip : "Scale",
         dataIndex : 'scale'
      } /*
          * ,{ id: 'ismaskpref', header: "Is Mask Preferred", dataIndex: 'isMaskPreferred', sortable: true },
          */]
   } ),

   /**
    * @memberOf ExpressionExperimentQuantitationTypeGrid
    */
   initComponent : function() {
      Gemma.ExpressionExperimentQuantitationTypeGrid.superclass.initComponent.call( this );

      /*
       * Do this on render so if it's a tab, the load call doesn't happen until tab is switched to
       */
      this.on( 'render', function() {
         if ( !this.loadMask ) {
            this.loadMask = new Ext.LoadMask( this.getEl(), {
               msg : "Loading ..."
            } );
         }
         this.loadMask.show();
         var store = this.store;
         ExpressionExperimentController.loadQuantitationTypes( this.eeid, function( qts ) {
            for (var j = 0; j < qts.length; j++) {
               if ( this.getStore().find( "id", qts[j].id ) < 0 ) {
                  var Constructor = this.store.recordType;
                  var record = new Constructor( qts[j] );
                  this.getStore().add( [ record ] );
               }
            }
            this.loadMask.hide();
         }.createDelegate( this ) );
      }, this );

   }

} );
