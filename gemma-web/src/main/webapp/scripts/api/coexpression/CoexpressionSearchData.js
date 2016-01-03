Ext.namespace( 'Gemma' );

/**
 * Responsible for conducting a coexpression search, and holds the results. Basically wraps a
 * CoexpressionMetaValueObject and encapsulates the search
 * 
 * @author cam
 * @class
 * @version $Id$
 */
Gemma.CoexpressionSearchData = Ext.extend( Ext.util.Observable, {

   // The original query, which could include gene ids, a gene set, experiment set, etc.
   searchCommandUsed : {}, // type CoexpressionSearchCommand. It gets set

   // CoexpressionMetaValueObject; also contains a copy of the command, which is the original used. It also has the
   // query gene ids.
   searchResults : {},

   // CoexpressionMetaValueObject, which might be filtered different for the graph view.
   cytoscapeSearchResults : {},

   cytoscapeResultsUpToDate : false,

   coexSearchTimeout : 420000, // ms - is this necessary?

   // ???
   allGeneIdsSet : [],

   /**
    * @Override
    * @memberOf Gemma.CoexpressionSearchData
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

   /**
    * 
    * @returns {Number}
    */
   getNumberOfDatasetsUsable : function() {
      return this.searchResults.numDatasetsQueried;
   },

   /**
    * 
    * @returns
    */
   getOriginalQuerySettings : function() {
      return this.searchResults.searchSettings;
   },

   /**
    * The stringency that was used on the server - may not be what the user requested
    * 
    * @returns {Number}
    */
   getQueryStringency : function() {
      return this.searchResults.queryStringency;
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
      for (var i = 0; i < this.searchResults.results.length; i++) {
         if ( this.searchResults.results[i].foundGene.isQuery ) {
            r.push( this.searchResults.results[i] );
         }
      }
      return r;
   },

   /**
    * The stringency that was applied on the server to trim the results - that is, the actual stringency used (be
    * careful)
    * 
    * @returns
    */
   getTrimStringency : function() {
      return this.searchResults.trimStringency;
   },

   /**
    * 
    * @returns {Array.CoexpressionValueObjectExt}
    */
   getCytoscapeResults : function() {
      return this.cytoscapeSearchResults.results;
   },

   /**
    * 
    * @param results
    *           {Array.CoexpressionValueObjectExt}
    */
   setCytoscapeResults : function( results ) {
      this.cytoscapeSearchResults.results = results; // this seems a bad idea...
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
      for (var j = 0; j < queries.length; j++) {

         queries[j] = queries[j].replace( /^\s+|\s+$/g, '' );

         if ( queries[j].length < 2 ) { // too short
            continue;
         }
         var queryRegEx = new RegExp( Ext.escapeRe( queries[j] ), 'i' );

         for (var i = 0; i < results.length; i++) {
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
    * FIXME This is redundant and confusing.
    * 
    * @returns
    */
   getResultsStringency : function() {
      return this.stringency;
   },

   /**
    * 
    * 
    * @returns
    */
   getQueryGeneIds : function() {
      // This might be empty if the query was a gene set
      // return this.searchCommandUsed.geneIds;

      var res = [];
      this.searchResults.queryGenes.forEach( function( gene ) {
         res.push( gene.id );
      } );
      return res;
   },

   getQueryGenes : function() {
      return this.searchResults.queryGenes;
   },

   getTaxonId : function() {
      return this.searchCommandUsed.taxonId;
   },

   /**
    * Does the search using CoexpressionSearchController.doSearchQuickComplete; fires events to notify state e.g. when
    * results are ready (or error). The stringency should initially be that used for the first search (to populate the
    * table)
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
         this.searchCommandUsed.stringency = newStringency; // FIXME WHY ARE WE CHANGING THIS HERE?
         this.cytoscapeResultsUpToDate = true;

         /*
          * last arg is the search settings
          */
         this.fireEvent( 'complete-search-results-ready', this.searchResults, {
            geneIds : this.searchCommandUsed.geneIds,
            eeIds : this.searchCommandUsed.eeIds, // IS THIS AVAILABLE?
            stringency : newStringency,
            useMyDatasets : false,
            queryGenesOnly : true,
            taxonId : this.searchCommandUsed.taxonId,
            eeSetName : this.searchCommandUsed.eeSetName,
            eeSetId : this.searchCommandUsed.eeSetId
         } );
         this.fireEvent( 'aftersearch' );
         return;

      }

      // otherwise...
      var searchStringency = Gemma.CytoscapePanelUtil.restrictResultsStringency( newStringency );
      this.stringency = searchStringency;

      var geneIdsSubset = Gemma.CytoscapePanelUtil.restrictQueryGenesForCytoscapeQuery( this );

      var coexpressionSearchCommand = {
         geneIds : geneIdsSubset,
         eeIds : this.searchCommandUsed.eeIds, // IS THIS AVAILABLE?
         stringency : searchStringency,
         useMyDatasets : false,
         queryGenesOnly : geneIdsSubset.length > 1, // important!
         taxonId : this.searchCommandUsed.taxonId,
         eeSetName : this.searchCommandUsed.eeSetName,
         eeSetId : this.searchCommandUsed.eeSetId
      };

      /*
       * Do a search that fills in the edges among the genes already found.
       */
      CoexpressionSearchController.doSearchQuickComplete( coexpressionSearchCommand, this.getQueryGeneIds(), {
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

      this.fireEvent( 'search-started' );
   },

   /**
    * Used when extending queries in the visualization.
    * 
    * @param geneIds
    * @param queryGenesOnly
    */
   searchWithGeneIds : function( geneIds, queryGenesOnly ) {
      var coexpressionSearchCommand = {
         geneIds : geneIds,
         eeIds : this.searchCommandUsed.eeIds,
         stringency : this.searchCommandUsed.stringency,
         useMyDatasets : this.searchCommandUsed.useMyDatasets,
         queryGenesOnly : queryGenesOnly,
         taxonId : this.searchCommandUsed.taxonId,
         eeSetName : this.searchCommandUsed.eeSetName,
         eeSetId : this.searchCommandUsed.eeSetId
      };
      this.search( coexpressionSearchCommand );
   },

   reset : function() {
      this.searchResults = {};
      this.allGeneIdsSet = []; // what is this for?
      this.cytoscapeSearchResults = {};
   },

   /**
    * Does a search using CoexpressionSearchController.doBackgroundCoexSearch
    * 
    * @param searchCommand
    */
   search : function( searchCommand ) {
      this.searchCommandUsed = searchCommand; // this is the only place this gets set. If Sets were used, the ids will
      // not be here.
      // console.log( "Query from client:" );
      // console.log( this.searchCommandUsed );
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
                  // console.log( "Server results:" )
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