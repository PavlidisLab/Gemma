/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

Gemma.CoexpressionDisplaySettings = Ext.extend( Ext.util.Observable, {

   // @private
   stringency : Gemma.MIN_STRINGENCY,

   // @private
   searchTextValue : "",

   // @private
   queryGenesOnly : false,

   // @private
   overlayGeneIds : [],

   getStringency : function() {
      return this.stringency;
   },

   /**
    * @memberOf Gemma.CoexpressionDisplaySettings
    */
   setStringency : function( newValue ) {
      if ( this.stringency !== newValue ) {
         this.stringency = newValue;
         this.fireEvent( "stringency_change", newValue );
      }
   },

   getSearchTextValue : function() {
      return this.searchTextValue;
   },

   setSearchTextValue : function( newValue ) {
      if ( this.searchTextValue !== newValue ) {
         this.searchTextValue = newValue;
         this.fireEvent( "search_text_change", newValue );
      }
   },

   getQueryGenesOnly : function() {
      return this.queryGenesOnly;
   },

   setQueryGenesOnly : function( newValue ) {
      if ( this.queryGenesOnly !== newValue ) {
         this.queryGenesOnly = newValue;
         this.fireEvent( "query_genes_only_change", newValue );
      }
   },

   getOverlayGeneIds : function() {
      return this.overlayGeneIds;
   },

   setOverlayGeneIds : function( newOverlayGeneIds ) {
      this.overlayGeneIds = newOverlayGeneIds;
      this.fireEvent( "gene_overlay", newOverlayGeneIds );
   },

   /**
    * @private
    * @param configs
    */
   constructor : function( configs ) {
      this.addEvents( [ 'stringency_change', 'search_text_change', 'query_genes_only_change', 'gene_overlay' ] );

      if ( typeof configs !== 'undefined' ) {
         Ext.apply( this, configs );
      }

      Gemma.CoexpressionDisplaySettings.superclass.constructor.call( this );
   }
} );
