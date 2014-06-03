/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace( 'Gemma' );

/**
 * 
 * @param coexpressionSearchData
 *           Gemma.CoexGraphData
 * @param cytoscapeCoexCommand
 * @returns {Gemma.CoexGraphData}
 */
Gemma.CoexGraphData = function( coexpressionSearchData, cytoscapeCoexCommand ) {

   this.originalResults = new Object();
   this.originalResults.geneResults = coexpressionSearchData.getCytoscapeResults();
   this.originalResults.trimStringency = coexpressionSearchData.getTrimStringency();
   this.originalResults.queryStringency = coexpressionSearchData.getQueryStringency();

   this.defaultSize = "medium";

   /*
    * Heuristic thresholds. MaxEdges is a server-side limit meant to keep huge graphs from coming down. Default is 850
    * (or some such; see CoexpressionMetaValueObject).
    */
   this.mediumGraphMaxSize = coexpressionSearchData.cytoscapeSearchResults.maxEdges * 0.75;
   this.smallGraphMaxSize = coexpressionSearchData.cytoscapeSearchResults.maxEdges * 0.5;

   this.largeGraphDataEnabled = false;
   this.mediumGraphDataEnabled = false;
   this.smallGraphDataEnabled = false;

   /**
    * Trim graph to match (more or less) the given resultsSizeLimit. See Gemma.CoexVOUtil.trimResultsForReducedGraph for
    * details.
    * 
    * @param {number}
    *           resultsSizeLimit
    */
   this.getTrimmedGraphData = function( resultsSizeLimit ) {

      // if this is a large 'query genes only ' search, then trimming the data is unnecessary
      if ( coexpressionSearchData.searchCommandUsed.queryGenesOnly ) {
         var returnObject = {};
         returnObject.geneResults = this.originalResults.geneResults;
         returnObject.trimStringency = this.originalResults.trimStringency;
         return returnObject;
      }

      /*
       * 
       */
      return Gemma.CoexVOUtil.trimResultsForReducedGraph( this.originalResults.geneResults,
         coexpressionSearchData.searchCommandUsed.geneIds, cytoscapeCoexCommand.stringency,
         this.originalResults.numDatasetsQueried, resultsSizeLimit );

   };

   /*
    * Establish the data for the different graph sizes.
    */
   if ( this.mediumGraphMaxSize > this.originalResults.geneResults.length ) {
      this.graphDataMedium = this.originalResults;
   } else {
      this.graphDataMedium = this.getTrimmedGraphData( this.mediumGraphMaxSize );
      if ( this.graphDataMedium && this.graphDataMedium.geneResults.length < this.originalResults.geneResults.length ) {
         this.mediumGraphDataEnabled = true;
         this.largeGraphDataEnabled = true;
      }
   }

   if ( this.graphDataMedium && this.smallGraphMaxSize > this.graphDataMedium.geneResults.length ) {
      this.graphDataSmall = this.graphDataMedium;
   } else {
      this.graphDataSmall = this.getTrimmedGraphData( this.smallGraphMaxSize );
      if ( this.graphDataMedium && this.graphDataSmall.geneResults.length < this.graphDataMedium.geneResults.length ) {
         this.smallGraphDataEnabled = true;
         this.mediumGraphDataEnabled = true;
      }
   }

   this.getGraphData = function( graphSize ) {

      if ( !graphSize ) {
         graphSize = this.defaultSize;
      }

      if ( graphSize === "medium" ) {
         return this.graphDataMedium;
      } else if ( graphSize === "small" ) {
         return this.graphDataSmall;
      }

      return this.originalResults;

   };

};