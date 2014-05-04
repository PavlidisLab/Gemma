Ext.namespace( 'Gemma' );

/**
 * When the example queries are ready to run, 'examplesReady' event is fired with three param: taxonId,
 * geneExampleRecord, experimentExampleRecord
 * 
 * @author thea
 * @version $Id$
 */
Gemma.AnalysisResultsSearchExamples = Ext.extend( Ext.Panel, {

   exampleQueries : {
      diffEx : [
                {
                   goId : "GO_0021766",
                   eeSetId : '6112',
                   taxonId : '1',
                   backupGeneIds : [ 175764, 57412, 33449, 22652, 172517, 365527, 154351, 164380, 163012, 36178,
                                    258329, 325340, 119501, 161166, 169774, 43145, 12948, 74699, 203063, 120960, 33479,
                                    322804, 88959, 12966, 7187, 136503, 33369, 57883, 73088, 174546, 74174, 57397,
                                    36158 ]
                },
                {
                   goId : "GO_0021879",
                   eeSetId : '6110',
                   taxonId : '2',
                   backupGeneIds : [ 500611, 534025, 574982, 633950, 550316, 534368, 537487, 574759, 556740, 583115,
                                    634211, 534401, 500595 ]
                } ],
      coex : [
              {
                 goId : "GO_0051302",
                 eeSetName : 'Master set for yeast', // don't need one any more
                 // eeSetId : '6223',
                 taxonId : '11',
                 backupGeneIds : [ 7678763, 7678783, 7676882, 7694443, 7685764, 7667629, 7672893, 7673265, 7686100,
                                  7697083, 7670169, 7692953 ]
              }, {
                 goId : "GO_0035418",
                 eeSetId : '737',// don't need this any more.
                 taxonId : '1',
                 backupGeneIds : [ 269935, 194669, 232747, 36104, 316763 ]
              } ]
   },
   isCollapsed : false,
   colspan : 4,
   cls : 'left-align-btn transparent-btn transparent-btn-link',

   /**
    * @memberOf Gemma.AnalysisResultsSearchExamples
    */
   initComponent : function() {

      Gemma.AnalysisResultsSearchExamples.superclass.initComponent.call( this );

      this.add( [ {
         ref : 'examplesTitle',
         tpl : 'Example Queries: <a href="javascript:void(0);">[ {sign} ]</a>',
         data : {
            sign : '-'
         },
         border : false,
         hidingExamples : false,
         listeners : {
            'render' : function() {
               this.body.on( 'click', function( e ) {
                  e.stopEvent();
                  this.fireEvent( 'toggleHideExamples' );
               }, this, {
                  delegate : 'a'
               } );
            },
            'toggleHideExamples' : {
               fn : this.collapseExamples,
               scope : this
            }
         }
      }, {
         ref : 'diffExExamples',
         border : false,
         hidden : !this.defaultIsDiffEx,
         items : [ {
            xtype : 'button',
            ref : 'diffExExample1',
            text : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx1Text,
            tooltip : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx1TT,
            listeners : {
               click : function() {
                  this.runExampleQuery( this.exampleQueries.diffEx[0] );
               },
               scope : this
            }

         }, {
            xtype : 'button',
            ref : 'diffExExample2',
            text : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx2Text,
            tooltip : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.diffEx2TT,
            listeners : {
               click : function() {
                  this.runExampleQuery( this.exampleQueries.diffEx[1] );
               },
               scope : this
            }

         } ]
      }, {
         ref : 'coexExamples',
         border : false,
         hidden : this.defaultIsDiffEx,
         items : [ {
            xtype : 'button',
            ref : 'coexExample1',
            text : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.coex1Text,
            tooltip : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.Examples.coex1TT,
            listeners : {
               click : function() {
                  this.runExampleQuery( this.exampleQueries.coex[0] );
               },
               scope : this
            }

         } ]
      } ] );

      this.doLayout();

   },

   showDiffExExamples : function() {
      this.diffExExamples.show();
      this.coexExamples.hide();
   },
   showCoexExamples : function() {
      this.diffExExamples.hide();
      this.coexExamples.show();
   },

   /**
    * hide the example queries
    */
   collapseExamples : function( show ) {

      if ( show === undefined ) {
         show = this.isCollapsed;
      }

      this.diffExExamples.diffExExample1.setVisible( show );
      this.diffExExamples.diffExExample2.setVisible( show );
      this.coexExamples.coexExample1.setVisible( show );
      // this.searchExamples.coexExamples.coexExample2.setVisible(show);
      if ( show ) {
         this.examplesTitle.update( {
            sign : '-'
         } );
         this.isCollapsed = false;
      } else {
         this.examplesTitle.update( {
            sign : '+'
         } );
         this.isCollapsed = true;
      }
   },

   eeSetCb : function( eeSet ) {

      ExpressionExperimentSetController.getExperimentIdsInSet( eeSet.id, {
         callback : function( expIds ) {

            eeSet.expressionExperimentIds = expIds;

            var record = new Gemma.ExperimentAndExperimentGroupComboRecord( {
               name : eeSet.name,
               description : eeSet.description,
               isGroup : true,
               size : expIds.length,
               taxonId : eeSet.taxonId,
               taxonName : eeSet.taxonName,
               memberIds : expIds,
               resultValueObject : eeSet,
               userOwned : false
            } );
            this.fireEvent( 'eeExampleReady', record );

         }.createDelegate( this )
      } );
   },

   /**
    * set the first experiment chooser to have chosen a set and show its preview
    * 
    * @param setId
    *           must be a valid id for a database-backed experimetn set
    */
   getExperimentSet : function( setId, setName ) {

      // make a ee set combo record for the db-backed experimentSetValueObject
      if ( setId ) {
         ExpressionExperimentSetController.load( setId, this.eeSetCb.createDelegate( this ) );
      } else if ( setName ) {
         ExpressionExperimentSetController.loadByName( setName, this.eeSetCb.createDelegate( this ) );
      } else {
         // panic?
      }
   },

   /**
    * set the gene chooser to have chosen a go group and show its preview
    * 
    * @param geneSetId
    *           must be a valid id for a database-backed gene group
    */
   getGOGeneSet : function( goName, taxonId, backupGeneIds ) {

      var myscope = this;

      // make a gene combo record for the db-backed experimentSetValueObject
      GenePickerController.getGeneSetByGOId( goName, taxonId, function( geneSet ) {

         // if the GO id failed to match a set, use the hard coded back up list of genes
         // (this might happen if Berkeleybop is down, see bug 2534)
         if ( geneSet === null ) {
            geneSet = myscope.makeSessionBoundGeneSet( backupGeneIds, taxonId, 'Backup gene list for ' + goName,
               'GO database unavailable, using backup list' );
         }

         var record = new Gemma.GeneAndGeneGroupComboRecord( {
            name : geneSet.name,
            description : geneSet.descrption,
            isGroup : true,
            size : geneSet.geneIds.length,
            taxonId : geneSet.taxonId,
            taxonName : geneSet.taxonName,
            memberIds : geneSet.geneIds,
            resultValueObject : geneSet,
            comboText : geneSet.name + ": " + geneSet.description,
            userOwned : false
         } );

         myscope.fireEvent( 'geneExampleReady', record );

      } );
   },

   // TODO this is duplicated code from GeneSearchAndPreview; should be combined somewhere!
   makeSessionBoundGeneSet : function( geneIds, taxonId, name, description ) {
      var newGeneSet = new SessionBoundGeneSetValueObject();
      newGeneSet.modified = false;
      newGeneSet.geneIds = geneIds;
      newGeneSet.taxonId = taxonId;
      newGeneSet.name = name; // 'From Symbol List';
      newGeneSet.description = description, // 'Group made from gene symbols entered.';
      newGeneSet.size = geneIds.length;
      newGeneSet.id = null;
      return newGeneSet;
   },

   runExampleQuery : function( exampleConfig ) {

      this.fireEvent( 'startingExample' );

      var goName = exampleConfig.goId;
      var eeSetId = exampleConfig.eeSetId;
      var eeSetName = exampleConfig.eeSetName;
      var taxonId = exampleConfig.taxonId;
      var backupGeneIds = exampleConfig.backupGeneIds;

      this.getExperimentSet( eeSetId, eeSetName );
      // set the gene chooser
      this.getGOGeneSet( goName, taxonId, backupGeneIds );

      var queryRun = false;
      var geneExampleReady = false;
      var eeExampleReady = false;
      this.on( 'geneExampleReady', function( record ) {
         geneExampleReady = true;
         this.geneExampleRecord = record;
         if ( eeExampleReady && !queryRun ) {
            queryRun = true;
            this.fireEvent( 'examplesReady', taxonId, this.geneExampleRecord, this.experimentExampleRecord );
         }
      } );

      this.on( 'eeExampleReady', function( record ) {
         eeExampleReady = true;
         this.experimentExampleRecord = record;
         if ( geneExampleReady && !queryRun ) {
            queryRun = true;
            this.fireEvent( 'examplesReady', taxonId, this.geneExampleRecord, this.experimentExampleRecord );
         }
      } );

   }

} );
