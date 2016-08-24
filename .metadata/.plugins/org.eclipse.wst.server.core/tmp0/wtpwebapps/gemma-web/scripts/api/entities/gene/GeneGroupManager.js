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
 * Provides functionallity for creating and managing Gene groups inside of Gemma.
 * 
 * @author klc
 * @version $Id$
 */

/**
 * Interface with two panels: one for the groups, one for the group members.
 * 
 * @class Gemma.GeneGroupManager
 * @extends Ext.Panel
 */

Gemma.GeneGroupManager = Ext.extend( Ext.Panel, {

   id : "gene-manager-panel",
   layout : 'hbox',
   layoutConfig : {
      align : 'stretch'
   },
   title : "Gene Group Manager",

   /**
    * @memberOf Gemma.GeneGroupManager
    */
   initComponent : function() {
      this.geneChooserPanel = new Gemma.GeneGroupMemberPanelClassic( {
         split : true,
         flex : 1
      } );

      this.geneGroupPanel = new Gemma.GeneGroupPanel( {
         tbar : new Gemma.GeneGroupEditToolbar(),
         flex : 1,
         viewConfig : {
            forceFit : true
         }
      } );

      Ext.apply( this.geneChooserPanel.getTopToolbar().taxonCombo, {
         stateId : "",
         stateful : false,
         stateEvents : []
      } );

      // todo add widget for searching for gene groups (or go terms)
      Ext.apply( this, {
         items : [ this.geneGroupPanel, this.geneChooserPanel ]
      } );

      Gemma.GeneGroupManager.superclass.initComponent.call( this );

      /*
       * Remove a gene: update gene group record locally
       */
      this.geneChooserPanel.getStore().on( 'remove', function( store, record, index ) {
         this.dirtySet( store );
      }, this );

      /*
       * Add a gene: update gene group record locally
       */
      this.geneChooserPanel.getStore().on( 'add', function( store, records, index ) {
         this.dirtySet( store );
         this.geneChooserPanel.resetKeepTaxon();
      }, this );

      /*
       * After the gene panel loads, unmask
       */
      this.geneChooserPanel.getStore().on( 'load', function() {
         this.getEl().unmask();
      }, this );

      this.geneGroupPanel.getSelectionModel().on( 'rowselect', function( model, rowindex, record ) {
         // keep user from messing up interface while we load (e.g., switching rows)
         this.getEl().mask();
         if ( record.get( 'geneIds' ).length === 0 ) {
            this.geneChooserPanel.getStore().removeAll();
            this.geneChooserPanel.lockInTaxon( record.data.taxonId );
            this.getEl().unmask();
         } else {
            if ( !record.phantom ) {
               this.geneChooserPanel.showGeneGroup( record );
            } else {
               this.geneChooserPanel.loadGenes( record.get( 'geneIds' ) );
            }
         }

      }, this );
   },

   /**
    * Copy gene information from the gene grid over to the gene group.
    * 
    * @param store
    *           the store from the gene list panel.
    */
   dirtySet : function( store ) {
      store.clearFilter( false );
      // collect current ids from store
      var currentIds = [];
      store.each( function( r ) {
         currentIds.push( r.get( 'id' ) );
      }, this );

      var rec = this.geneGroupPanel.getSelectionModel().getSelected();

      if ( rec ) {
         rec.set( 'geneIds', currentIds );
         rec.set( 'size', currentIds.length );
      }
   }
} );
