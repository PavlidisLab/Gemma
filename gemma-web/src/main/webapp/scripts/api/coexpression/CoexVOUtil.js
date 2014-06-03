Ext.namespace( "Gemma" );

/**
 * Utility methods for manipulating coexpression result value objects, and thus take as argument a collection of
 * CoexpressionValueObjectExts (results).
 */
Gemma.CoexVOUtil = {

   /**
    * takes an array of CoexpressionValueObjectExts (results) returns a set of all the geneIds contained in it
    * 
    * @static
    * @param results
    * @returns {Array}
    * @memberOf Gemma.CoexVOUtil
    */
   getAllGeneIds : function( results ) {

      var geneIdSet = [];

      if ( results == null ) {
         return geneIdSet;
      }

      for (var i = 0; i < results.length; i++) {
         geneIdSet.push( results[i].foundGene.id );
         geneIdSet.push( results[i].queryGene.id );
      }

      return geneIdSet.reduce( function( a, b ) {
         if ( a.indexOf( b ) < 0 )
            a.push( b );
         return a;
      }, [] );
   },

   /**
    * Keep links that involve any query genes (not necessarily between query genes) and which meet the filter
    * stringency.
    * 
    * @static
    * @param results
    * @param currentQueryGeneIds
    * @param filterStringency
    * @returns {array} the graphNodeIds to retain (at least some of which will be query genes)
    * @memberOf Gemma.CoexVOUtil
    */
   trimResultsForQueryGenes : function( results, currentQueryGeneIds, filterStringency ) {

      var graphNodeIds = [];

      for (var i = 0; i < results.length; i++) {
         // go in only if the query or known gene is contained in the
         // original query geneids AND the stringency is >= the filter
         // stringency
         var r = results[i];
         if ( r.support >= filterStringency
            && (currentQueryGeneIds.indexOf( r.foundGene.id ) !== -1 || currentQueryGeneIds.indexOf( r.queryGene.id ) !== -1) ) {

            graphNodeIds.push( r.foundGene.id );
            graphNodeIds.push( r.queryGene.id );

         }
      }

      return graphNodeIds;

   },

   /**
    * Utility method; Syntactic sugar.
    * 
    * @static
    * @param {Array}
    *           array
    * @param {}
    *           item
    * @returns {Boolean} true if the array contains the item; false otherwise.
    * @memberOf Gemma.CoexVOUtil
    */
   arContains : function( array, item ) {
      return array.indexOf( item ) !== -1;
   },

   /**
    * Cut the data provided down to a subset, such that approximately resultsSizeLimit edges are retained. The logic is:
    * <ol>
    * <li>Links that involve the query genes are examined first. We select such links until we reach resultsSizeLimit.
    * The stringency at which this occurs is the trimStringency. We then take more such links for that stringency.
    * <li>Links that involve genes included in the above are kept if they meet trimStringency, even if this means going
    * over resultsSizeLimit.
    * </ol>
    * 
    * @static
    * @param {Array}
    *           results CoexpressionValueObjectExts
    * @param {Array}
    *           currentQueryGeneIds
    * @param {integer}
    *           currentStringency
    * @param {integer}
    *           stringencyTrimLimit the maximum stringency we will test (= number of data sets queried)
    * @param {integer}
    *           resultsSizeLimit maximum number of results
    * @returns {Object} with geneResults and trimStringency values.
    * @memberOf Gemma.CoexVOUtil
    */
   trimResultsForReducedGraph : function( results, currentQueryGeneIds, currentStringency, stringencyTrimLimit,
      resultsSizeLimit ) {

      if ( results.length <= resultsSizeLimit ) {
         /*
          * We shouldn't be here.
          */
      }

      // this might already be sorted... but in case.
      results.sort( function compare( a, b ) {
         return (a.support > b.support) ? -1 : ((b.support > a.support) ? 1 : 0);
      } );

      // convert currentQueryGeneIds into a hashtable
      qgh = {};
      for (var j = 0; j < currentQueryGeneIds.length; j++) {
         qgh[currentQueryGeneIds[j]] = 1;
      }

      /*
       * First pass: keep all links that involve two query genes, or either query gene if it meets the stringency.
       */
      var maybe = [];
      var trimmedGeneResults = [];
      var graphNodeIds = {};
      var trimStringency = currentStringency;
      for (var i = 0; i < results.length; i++) {
         var r = results[i];

         if ( r.support < trimStringency ) {
            break;
         }

         var f = qgh[r.foundGene.id];
         var q = qgh[r.queryGene.id];

         if ( f || q ) {
            trimmedGeneResults.push( r );

            // need to populate graphNodeIds appropriately in order to
            // correctly add 'my genes only' edges
            if ( f && q ) {
               // really these should be prioritized.
               // ignore support when the link is between query genes.
               graphNodeIds[r.foundGene.id] = 1;
               graphNodeIds[r.queryGene.id] = 1;
            } else if ( f ) {
               graphNodeIds[r.foundGene.id] = 1;
               if ( r.support > trimStringency ) {
                  graphNodeIds[r.queryGene.id] = 1;
               }
            } else if ( q ) {
               graphNodeIds[r.queryGene.id] = 1;
               if ( r.support > trimStringency ) {
                  graphNodeIds[r.foundGene.id] = 1;
               }
            }

            if ( trimmedGeneResults >= resultsSizeLimit ) {
               trimStringency = r.support;
            }

         } else {
            maybe.push( r );
         }
      }

      /*
       * Second pass: get any remaining links between the non-query nodes involved in the above links, so long as they
       * meet the stringency.
       */
      for (var i = 0; i < maybe.length; i++) {
         var r = maybe[i];
         if ( r.support >= trimStringency && graphNodeIds[r.foundGene.id] && graphNodeIds[r.queryGene.id] ) {
            trimmedGeneResults.push( r );
         }
      } // end for (<results.length)

      console.log( "Trimmed: " + currentStringency + " to " + trimStringency );

      var returnObject = {};
      returnObject.geneResults = trimmedGeneResults;
      returnObject.trimStringency = trimStringency;
      return returnObject;

   },

   /**
    * Filter results at the given stringency.
    * 
    * @static
    * @param results
    * @param filterStringency
    * @returns {Array}
    * @memberOf Gemma.CoexVOUtil
    */
   trimResults : function( results, filterStringency ) {
      var trimmedGeneResults = [];
      for (var i = 0; i < results.length; i++) {
         if ( results[i].support >= filterStringency ) {
            trimmedGeneResults.push( results[i] );
         } // end if
      } // end for (<results.length)

      return trimmedGeneResults;

   },

   /**
    * Utility method.
    * 
    * @static
    * @param {Array}
    *           entities, must have an 'id' field. Assumed to be unique!
    * @returns {Array} ids of the entities
    * @memberOf Gemma.CoexVOUtil
    */
   getEntityIds : function( entities ) {
      var result = [];

      for (var i = 0; i < entities.length; i++) {
         result.push( entities[i].id );
      }
      return result;
   },

   /**
    * for filtering results down to only results that involve geneids; if the query or known gene is contained in the
    * original query geneids
    * 
    * @static
    * @param geneIds
    * @param results
    * @returns {Array}
    * @memberOf Gemma.CoexVOUtil
    */
   filterGeneResultsByGeneIds : function( geneIds, results ) {

      var trimmedGeneResults = [];

      for (var i = 0; i < results.length; i++) {

         var r = results[i];
         if ( this.arContains( geneIds, rr.foundGene.id ) || this.arContains( geneIds, r.queryGene.id ) ) {
            trimmedGeneResults.push( r );
         }
      }

      return trimmedGeneResults;
   },

   /**
    * Filter results so that they are only links among the given genes.
    * 
    * @static
    * @param geneIds
    * @param results
    * @returns {Array}
    * @memberOf Gemma.CoexVOUtil
    */
   filterGeneResultsByGeneIdsMyGenesOnly : function( geneIds, results ) {

      var trimmedGeneResults = [];

      for (var i = 0; i < results.length; i++) {
         var r = results[i];
         if ( geneIds.indexOf( r.foundGene.id ) !== -1 && geneIds.indexOf( r.queryGene.id ) !== -1 ) {
            trimmedGeneResults.push( r );
         }
      }

      return trimmedGeneResults;
   },

   /**
    * used by cytoscape panel to get gene node ids for highlighting
    * 
    * @static
    * @param text
    * @param results
    * @returns {Array}
    */
   filterGeneResultsByTextForNodeIds : function( text, results ) {

      var genesMatchingSearch = [];

      var splitTextArray = text.split( "," );

      var j;
      for (j = 0; j < splitTextArray.length; j++) {

         splitTextArray[j] = splitTextArray[j].replace( /^\s+|\s+$/g, '' );

         if ( splitTextArray[j].length < 2 )
            continue;

         var value = new RegExp( Ext.escapeRe( splitTextArray[j] ), 'i' );

         for (var i = 0; i < results.length; i++) {

            var foundGene = results[i].foundGene;
            var queryGene = results[i].queryGene;

            if ( genesMatchingSearch.indexOf( foundGene.officialSymbol ) !== 1 ) {

               if ( value.test( foundGene.officialSymbol ) || value.test( foundGene.officialName ) ) {
                  genesMatchingSearch.push( foundGene.officialSymbol );

               }

            }

            if ( genesMatchingSearch.indexOf( queryGene.officialSymbol ) !== 1 ) {

               if ( value.test( queryGene.officialSymbol ) || value.test( queryGene.officialName ) ) {
                  genesMatchingSearch.push( queryGene.officialSymbol );
               }

            }

         } // end for (<results.length)

      }

      return genesMatchingSearch;
   },

   /**
    * used by coexpressionGrid to grab results for exporting
    * 
    * @static
    * @param text
    * @param results
    * @returns {Array}
    */
   filterGeneResultsByText : function( text, results ) {

      var value = new RegExp( Ext.escapeRe( text ), 'i' );
      var genesMatchingSearch = [];

      for (var i = 0; i < results.length; i++) {

         if ( value.test( results[i].foundGene.officialSymbol ) || value.test( results[i].queryGene.officialSymbol )
            || value.test( results[i].foundGene.officialName ) || value.test( results[i].queryGene.officialName ) ) {
            genesMatchingSearch.push( results[i] );
         }

      } // end for (<results.length)

      return genesMatchingSearch;
   },

/**
 * Find a stringency, possibly lower than the initialDisplayStringency, to ensure we see some results.
 * 
 * @static
 * @param results
 * @param initialDisplayStringency
 *           should be the stringency used to query ... so this method isn't going to do anything.
 * @returns {number}
 */
// findMaximalStringencyToApply : function( results, initialDisplayStringency ) {
//
// var highestResultStringency = Gemma.MIN_STRINGENCY;
//
// for (var i = 0; i < results.length; i++) {
//
// if ( results[i].posSupp > highestResultStringency ) {
// highestResultStringency = results[i].posSupp;
// }
//
// if ( results[i].negSupp > highestResultStringency ) {
// highestResultStringency = results[i].negSupp;
// }
//
// if ( highestResultStringency <= initialDisplayStringency ) {
// // then we know we can use this stringency and see some results.
// return initialDisplayStringency;
// }
//
// }
//
// return Math.min( highestResultStringency, initialDisplayStringency );
// }
};