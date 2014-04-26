Ext.namespace( 'Gemma' );

/**
 * Responsible for conducting a coexpression search, and holds the results. Basically mimics a
 * CoexpressionMetaValueObject
 * 
 * @author cam
 * @class
 * @version $Id$
 */
Gemma.CoexpressionSearchData = Ext.extend( Ext.util.Observable, {

   // The original query
   searchCommandUsed : {}, // type CoexpressionSearchCommand

   // CoexpressionMetaValueObject; also contains a copy of the command, whic his the original used.
   searchResults : {},

   // not sure what this is for...
   cytoscapeSearchResults : {},

   cytoscapeResultsUpToDate : false,
   coexSearchTimeout : 420000,

   // 
   stringency : null,
   allGeneIdsSet : [],

   /**
    * @Override
    */
   initComponent : function() {
      this.searchCommandUsed.stringency = Gemma.CytoscapePanelUtil
         .restrictResultsStringency( this.searchCommandUsed.displayStringency );

      Gemma.CoexpressionSearchData.superclass.initComponent.call( this );
      this.addEvents( 'search-results-ready', 'complete-search-results-ready', 'search-error' );
   },

   constructor : function( configs ) {
      if ( typeof configs !== 'undefined' ) {
         Ext.apply( this, configs );
      }
      Gemma.CoexpressionSearchData.superclass.constructor.call( this );
   },

   getNumberOfDatasetsUsable : function() {
      return this.searchResults.numDatasetsQueried;
   },

   getOriginalQuerySettings : function() {
      return this.searchResults.searchSettings;
   },

   /**
    * 
    * @returns {Array}
    */
   getResults : function() {
      return this.searchResults.results;
   },

   /**
    * 
    * @returns {Array}
    */
   getQueryGenesOnlyResults : function() {
      /*
       * filter
       */
      var r = [];
      for ( var i = 0; i < this.searchResults.results.length; i++) {
         if ( this.searchResults.results[i].interQueryLink ) {
            r.push( this.searchResults.results[i] );
         }
      }
      return r;
   },

   /**
    * Stringency which was used to trim non-query results. FIXME
    * 
    * @returns
    */
   getNonQueryGeneTrimmedValue : function() {
      return this.cytoscapeSearchResults.nonQueryGeneTrimmedValue;
   },

   /**
    * The stringency that was applied on the server to trim the results.
    * 
    * @returns
    */
   getAppliedStringency : function() {
      return this.searchResults.appliedStringency;
   },

   /**
    * 
    * @returns
    */
   getCytoscapeResults : function() {
      return this.cytoscapeSearchResults.results;
   },

   setCytoscapeKnownGeneResults : function( results ) {
      this.cytoscapeSearchResults.results = results;
   },

   /**
    * 
    * @param query
    * @returns {Array}
    */
   getCytoscapeGeneSymbolsMatchingQuery : function( query ) {
      var results = this.cytoscapeSearchResults.results;
      var genesMatchingSearch = [];
      var queries = query.split( "," );
      var i, j;
      for (j = 0; j < queries.length; j++) {

         queries[j] = queries[j].replace( /^\s+|\s+$/g, '' );

         if ( queries[j].length < 2 ) { // too short
            continue;
         }
         var queryRegEx = new RegExp( Ext.escapeRe( queries[j] ), 'i' );

         for (i = 0; i < results.length; i++) {
            var foundGene = results[i].foundGene;
            var queryGene = results[i].queryGene;

            if ( genesMatchingSearch.indexOf( foundGene.officialSymbol ) !== 1 ) {
               if ( queryRegEx.test( foundGene.officialSymbol ) || queryRegEx.test( foundGene.officialName ) ) {
                  genesMatchingSearch.push( foundGene.id );
               }
            }

            if ( genesMatchingSearch.indexOf( queryGene.officialSymbol ) !== 1 ) {
               if ( queryRegEx.test( queryGene.officialSymbol ) || queryRegEx.test( queryGene.officialName ) ) {
                  genesMatchingSearch.push( queryGene.id );
               }
            }
         }
      }
      return genesMatchingSearch;
   },

   /**
    * 
    * @returns
    */
   getResultsStringency : function() {
      return this.stringency;
   },

   getQueryGeneIds : function() {
      return this.searchCommandUsed.geneIds;
   },

   getQueryGenes : function() {
      return this.searchResults.queryGenes;
   },

   /**
    * Return how many links the gene has genome-wide at the given stringency.
    * 
    * @param geneId
    * @param stringency
    */
   getNodeDegree : function( geneId, stringency ) {
      var s = this.searchResults.summaries[geneId];
      /*
       * s is a CoexpressionSummaryValueObject
       */
      var c = s.coexpNodeDegree;

      /*
       * n is a TreeMap ...
       */
      var n = c.nodeDegrees;
   },

   getTaxonId : function() {
      return this.searchCommandUsed.taxonId;
   },

   setSearchCommand : function( searchCommand ) {
      this.searchCommandUsed = searchCommand;
   },

   /**
    * Does the search using CoexpressionSearchController.doSearchQuickComplete; fires events to notify state e.g. when
    * results are ready (or error). The stringency should initially be that used for the first search (to populate the table)
    * 
    * @param newStringency
    * 
    */
   searchForCytoscapeDataWithStringency : function( newStringency ) {

      // if the original grid search was query genes only, it means that we already have the results we need
      if ( this.searchCommandUsed.queryGenesOnly ) {
         this.stringency = newStringency;
         this.fireEvent( 'search-started' );
         this.cytoscapeSearchResults = this.searchResults;
         this.searchCommandUsed.stringency = newStringency;
         this.cytoscapeResultsUpToDate = true;
         this.fireEvent( 'complete-search-results-ready', this.searchResults, {
            geneIds : this.searchCommandUsed.geneIds,
            eeIds : this.searchCommandUsed.eeIds,
            stringency : newStringency,
            useMyDatasets : false,
            queryGenesOnly : true,
            taxonId : this.searchCommandUsed.taxonId,
            eeSetName : null,
            eeSetId : null
         } );
         this.fireEvent( 'aftersearch' );
         return;

      }
      
      var searchStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency( newStringency );
      this.stringency = searchStringency;

      var geneIdsSubset = Gemma.CytoscapePanelUtil.restrictQueryGenesForCytoscapeQuery( this );

      var coexpressionSearchCommand = {
         geneIds : geneIdsSubset,
         eeIds : this.searchCommandUsed.eeIds,
         stringency : searchStringency,
         useMyDatasets : false,
         queryGenesOnly : true,
         taxonId : this.searchCommandUsed.taxonId,
         eeSetName : null,
         eeSetId : null
      };

      if ( geneIdsSubset.length < 2 ) {
         // There is a bug where if you can get a gene back in results but if you search for it by itself there are
         // no results(PPP2R1A human)
         this.cytoscapeSearchResults.results = [];
         this.fireEvent( 'complete-search-results-ready', this.cytoscapeSearchResults, coexpressionSearchCommand );
         return;
      }

      this.fireEvent( 'search-started' );

      /*
       * Do a search that fills in the edges among the genes already found.
       */
      CoexpressionSearchController.doSearchQuickComplete( coexpressionSearchCommand, this.searchCommandUsed.geneIds, {
         callback : function( results ) {
            this.cytoscapeSearchResults = results;
            this.searchCommandUsed.stringency = searchStringency;
            this.cytoscapeResultsUpToDate = true;
            this.fireEvent( 'complete-search-results-ready', results, coexpressionSearchCommand );
            this.fireEvent( 'aftersearch' );
         }.createDelegate( this ),
         errorHandler : function( result ) {
            this.fireEvent( 'search-error', result );
            this.fireEvent( 'aftersearch' );
         }.createDelegate( this ),
         timeout : this.coexSearchTimeout
      } );
   },

   /**
    * 
    * @param geneIds
    * @param queryGenesOnly
    */
   searchWithGeneIds : function( geneIds, queryGenesOnly ) {
      var coexpressionSearchCommand = {
         geneIds : geneIds,
         eeIds : this.searchCommandUsed.eeIds,
         stringency : Gemma.MIN_STRINGENCY,
         useMyDatasets : false,
         queryGenesOnly : queryGenesOnly,
         taxonId : this.searchCommandUsed.taxonId,
         eeSetName : null,
         eeSetId : null
      };
      this.search( coexpressionSearchCommand );
   },

   /**
    * Does a search using CoexpressionSearchController.doBackgroundCoexSearch
    * 
    * @param searchCommand
    */
   search : function( searchCommand ) {
      this.searchCommandUsed = searchCommand;
      var ref = this;
      ref.fireEvent( 'search-started' );
      CoexpressionSearchController.doBackgroundCoexSearch( searchCommand, {
         callback : function( taskId ) {
            var task = new Gemma.ObservableSubmittedTask( {
               'taskId' : taskId
            } );
            task.showTaskProgressWindow( {} );
            Ext.getBody().unmask();
            // results is a CoexpressionMetaValueObject
            task.on( 'task-completed', function( results ) {
               
               if ( results.errorState != null || results == null ) {
                  Ext.getBody().unmask();                    
                  ref.fireEvent( 'aftersearch', results.errorState, true );
                  ref.fireEvent( 'search-error', results.errorState );
               } else {
                   ref.searchResults = results;
                   ref.allGeneIdsSet = Gemma.CoexVOUtil.getAllGeneIds( ref.getResults() );
                   ref.cytoscapeResultsUpToDate = false;
                   ref.fireEvent( 'aftersearch' );
                   ref.fireEvent( 'search-results-ready' );
               }
               
            } );
            task.on( 'task-failed', function( error ) {
               ref.fireEvent( 'aftersearch', error, true );
            } );
            task.on( 'task-cancelling', function( error ) {
               ref.fireEvent( 'aftersearch', error, true );
            } );
         },
         errorHandler : function( error ) {
            ref.fireEvent( 'aftersearch', error );
            Ext.Msg.alert( Gemma.HelpText.CommonWarnings.Timeout.title, Gemma.HelpText.CommonWarnings.Timeout.text );
         } // sometimes got triggered without timeout
      } );
   }

} );