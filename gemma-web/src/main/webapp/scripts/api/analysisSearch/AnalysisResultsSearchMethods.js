Ext.namespace( 'Gemma' );

/**
 * Global setting for what we use as the minimum stringency (and often, the default)
 */
Gemma.MIN_STRINGENCY = 1;

Ext.onReady( function() {
   Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY = Ext.get( "coexpressionSearch.maxGenesPerQuery" ) ? Ext.get(
      "coexpressionSearch.maxGenesPerQuery" ).getValue() : 500;
} );

// max suggested number of elements to use for a diff ex viz query
// FIXME get this from the env. - add to userStatusVariables.jsp.
Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY = 100;
Gemma.MAX_EXPERIMENTS_CO_DIFF_EX_VIZ_QUERY = 100000; // effectively no limit
Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY = 100;

/**
 * These methods are used to run a search from the AnalysisResultsSearchForm; note that it does not actually contain the
 * calls to the server-side methods.
 * 
 * It's an Ext.util.Observable so that it can fire events (needed to keep form and UI in sync with steps of searching)
 * 
 * @author thea
 * @version $Id$
 */
Gemma.AnalysisResultsSearchMethods = Ext.extend( Ext.util.Observable, {
   taxonId : null,
   // defaults for coexpression
   DEFAULT_STRINGENCY : Gemma.MIN_STRINGENCY,
   DEFAULT_useMyDatasets : false,
   DEFAULT_queryGenesOnly : false,

   // defaults for differential expression
   // using Gemma.DEFAULT_THRESHOLD, Gemma.MIN_THRESHOLD, Gemma.MAX_THRESHOLD (defined elsewhere)

   /**
    * 
    * @param {GeneSetValueObject[]}
    *           geneSetValueObjects
    * @param {ExperimentSetValueObject[]}
    *           experimentSetValueObjects
    * @memberOf Gemma.AnalysisResultsSearchMethods
    */
   searchCoexpression : function( geneSetValueObject, experimentSetValueObject ) {
      // check
      if ( this.checkSetsEmpty( geneSetValueObject, experimentSetValueObject ) ) {
         return;
      }

      // register sets with session on the server
      var me = this;
      this.registerSetsIfNeeded( geneSetValueObject, experimentSetValueObject ).then( function done( sets ) {
         me.fireEvent( 'beforesearch' );
         var searchCommand = me.getCoexpressionSearchCommand( sets.geneSet, sets.experimentSet );
         var errorMessage = me.validateCoexSearch( searchCommand );
         if ( errorMessage === "" ) {
            me.fireEvent( 'coexpression_search_query_ready', searchCommand );
         } else {
            me.fireEvent( 'error', errorMessage );
         }

      }, function error( reason ) {
         me.fireEvent( 'error', "Error dealing with gene/experiment groups: " + reason );
      } );
   },

   /**
    * 
    * @param {GeneSetValueObject}
    *           geneSetValueObject
    * @param {ExperimentSetValueObject}
    *           experimentSetValueObject
    */
   searchDifferentialExpression : function( geneSetValueObject, experimentSetValueObject ) {
      // check inputs
      if ( this.checkSetsEmpty( geneSetValueObject, experimentSetValueObject ) ) {
         return;
      }

      /**
       * Verify that experiments and genes are not empty. If there are too many experiments or genes, warn the user and
       * offer to trim the sets.
       */

      // Check if counts exceed maximums
      var geneCount = Gemma.AnalysesSearchUtils.getGeneCount( geneSetValueObject );
      var experimentCount = Gemma.AnalysesSearchUtils.getExperimentCount( experimentSetValueObject );

      if ( geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY
         || experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY ) {
         // trim (optional)
         Gemma.AnalysesSearchUtils.showTrimInputDialogWindow( Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY, geneCount,
            geneSetValueObject, Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY, experimentCount, experimentSetValueObject,
            this );
      } else {
         // skip trimming
         this.startDifferentialExpressionSearch( geneSetValueObject, experimentSetValueObject );
      }
   },

   /**
    * @private
    * @param geneSetValueObject
    * @param experimentSetValueObject
    */
   startDifferentialExpressionSearch : function( geneSetValueObject, experimentSetValueObject ) {
      var scope = this;
      this.registerSetsIfNeeded( geneSetValueObject, experimentSetValueObject ).then( function done( sets ) {
         scope.fireEvent( 'beforesearch' );
         var query = scope.getDataForDiffVisualization( sets.geneSet, sets.experimentSet );
         scope.fireEvent( 'differential_expression_search_query_ready', null, query );
      }, function error( reason ) {

      } );
   },

   /**
    * Verify we are ready for query. FIXME Rename this...
    * 
    * @private
    * 
    * fires searchAborted event
    * 
    * @param geneSetValueObjects
    * @param experimentSetValueObjects
    * @return true if we still need more parameters set, false if we're good to go.
    */
   checkSetsEmpty : function( geneSetValueObject, experimentSetValueObject ) {

      if ( Gemma.AnalysesSearchUtils.isGeneSetEmpty( geneSetValueObject ) ) {

         /*
          * That's okay if they don't have too many experiments
          */
         if ( !Gemma.AnalysesSearchUtils.isExperimentSetEmpty( experimentSetValueObject )
            && experimentSetValueObject.size < 10 /* FIXME */) {
            return false;
         }

         Ext.Msg.alert( "Error", "Choose Genes or Experiments (or both)" );
         this.fireEvent( 'searchAborted' );
         return true;
      }

      if ( Gemma.AnalysesSearchUtils.isExperimentSetEmpty( experimentSetValueObject ) ) {

         if ( geneSetValueObject.size > 500 ) {
            Ext.Msg.alert( "Error", "Too many genes" );
            this.fireEvent( 'searchAborted' );
            return true;
         }

         if ( Gemma.AnalysesSearchUtils.isGeneSetEmpty( geneSetValueObject ) ) {
            Ext.Msg.alert( "Error", "You must select either some genes or some experiments." );
            return true;
         }

         return false;
      }

      if ( Gemma.AnalysesSearchUtils.isGeneSetEmpty( geneSetValueObject ) ) {

         if ( Gemma.AnalysesSearchUtils.isExperimentSetEmpty( experimentSetValueObject ) ) {
            Ext.Msg.alert( "Error", "You must select either some genes or some experiments." );
            this.fireEvent( 'searchAborted' );
            return true;
         }
      }
      return false;
   },

   /**
    * @private
    * @return {Promise}
    */
   registerSetsIfNeeded : function( geneSet, experimentSet ) {
      var registerGeneSetsPromise = Gemma.SessionSetsUtils.registerGeneSetIfNotRegistered( geneSet );
      var registerExperimentSetsPromise = Gemma.SessionSetsUtils.registerExperimentSetIfNotRegistered( experimentSet );

      // Promises are cool. Read up on them if you don't know what they are.
      var promise = RSVP.all( [ registerGeneSetsPromise, registerExperimentSetsPromise ] ).then(
         function wrapResults( results ) {
            // results are in the same order as promises were listed
            return {
               'geneSet' : results[0],
               'experimentSet' : results[1]
            };
         } );
      return promise;
   },

   /**
    * @private Construct the coexpression command object from the form, to be sent to the server.
    * 
    * @return {Object} CoexpressionSearchCommand
    */
   getCoexpressionSearchCommand : function( geneSetValueObject, experimentSetValueObject ) {

      var geneIds = null;
      if ( geneSetValueObject != null ) {
         geneIds = Gemma.AnalysesSearchUtils.getGeneIds( geneSetValueObject );
      }

      var eeIds = null;
      if ( experimentSetValueObject != null ) {
         eeIds = Gemma.AnalysesSearchUtils.getExperimentIds( experimentSetValueObject );
      }

      var coexpressionSearchCommand = {
         geneIds : geneIds,
         eeIds : eeIds,
         stringency : this.DEFAULT_STRINGENCY, // FIXME let user change this.
         useMyDatasets : this.DEFAULT_useMyDatasets, // FIXME let user change this
         queryGenesOnly : this.DEFAULT_queryGenesOnly, // FIXME let user change this
         taxonId : this.taxonId,
         eeSetName : null,
         eeSetId : experimentSetValueObject != null ? experimentSetValueObject.id : null,
         geneSetId : geneSetValueObject != null ? geneSetValueObject.id : null
      };
      return coexpressionSearchCommand;
   },

   /**
    * TODO: move this out of here
    * 
    * @return searchStringency
    */
   getLastCoexpressionSearchCommand : function() {
      return this.lastCSC;
   },

   /**
    * @private Do some more (mostly useless or unreachable) checks before running the coexpression search
    * @private
    * @param {Object}
    *           coexSearchCommand
    */
   validateCoexSearch : function( coexSearchCommand ) {
      if ( coexSearchCommand.queryGenesOnly
         && (coexSearchCommand.geneIds == null || coexSearchCommand.geneSet.size < 2) ) {
         return "You must select more than one query gene to use 'search among query genes only'";
      } else if ( coexSearchCommand.geneSetId == null
         && (!coexSearchCommand.geneIds || coexSearchCommand.geneIds.length === 0) ) {

         if ( (coexSearchCommand.eeIds === null || coexSearchCommand.eeIds.length == 0)
            && coexSearchCommand.eeSetId == null && coexSearchCommand.eeSetName == null ) {
            return "Specify some genes and/or some experiments";
         }
      } else if ( coexSearchCommand.stringency < Gemma.MIN_STRINGENCY ) {
         return "Minimum stringency is " + Gemma.MIN_STRINGENCY;
         // allow leaving thsi empty
         // } else if ( coexSearchCommand.eeIds && coexSearchCommand.eeIds.length < 1 ) {
         // / return "There are no datasets that match your search terms";
         // } else if ( !coexSearchCommand.eeIds && !coexSearchCommand.eeSetId ) {
         // return "Please select an analysis. Taxon, gene(s), and scope must be specified.";
      } else if ( coexSearchCommand.geneIds.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY ) {
         // if trying to search for more than the allowed limit of genes -- show warning
         // and trim the gene Ids
         // FIXME we can relax this if "my genes only"
         coexSearchCommand.queryGenesOnly = true;
         this.fireEvent( 'warning', "Complete Coexpression searches are limited to "
            + Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY
            + " query genes. Results from your query have been limited to coexpression between query genes.<br>" );
         return "";
      } else {
         return "";
      }
   },

   /**
    * @private
    * @param geneSetValueObject
    * @param experimentSetValueObject
    * @return
    */
   getDataForDiffVisualization : function( geneSetValueObject, experimentSetValueObject ) {

      var data = {
         experimentSetValueObject : experimentSetValueObject,
         geneSetValueObject : geneSetValueObject,
         taxonId : this.taxonId,
         taxonName : this.taxonName,
         pvalue : 0.01
      };
      return data;
   },

   /**
    * @private
    */
   initComponent : function() {
      Gemma.AnalysisResultsSearchMethods.superclass.initComponent.call( this );
   },

   /**
    * @private
    * @param configs
    */
   constructor : function( configs ) {
      if ( typeof configs !== 'undefined' ) {
         Ext.apply( this, configs );
      }
      Gemma.AnalysisResultsSearchMethods.superclass.constructor.call( this );
   }
} );