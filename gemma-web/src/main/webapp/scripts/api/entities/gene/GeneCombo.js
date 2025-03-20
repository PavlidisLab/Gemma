Ext.namespace( 'Gemma' );

/**
 * Live search field for genes.
 * 
 * @class Gemma.GeneCombo
 * @extends Ext.form.ComboBox
 */
Gemma.GeneCombo = Ext
   .extend(
      Ext.form.ComboBox,
      {

         name : 'genecombo',
         displayField : 'comboText',
         valueField : 'id',
         width : 140,// default.
         listWidth : 450, // ridiculously large so IE displays it properly
         // (usually)
         enableKeyEvents : true,
         loadingText : 'Searching...',
         emptyText : "Search for a gene",
         minChars : 1,
         selectOnFocus : true,
         mode : 'remote', // default = remote
         queryDelay : 800, // default = 500
         lastQuery : null,

         stickyTaxon : true, // this controls whether the taxon of the first
         // selection
         // from this box should be remembered for subsequent searches

         actualTextOfLastQuery : '',

         record : Ext.data.Record.create( [ {
            name : "id",
            type : "int"
         }, {
            name : "taxonId"
         }, {
            name : "taxonScientificName"
         }, {
            name : "officialSymbol",
            type : "string"
         }, {
            name : "officialName",
            type : "string"
         }, {
            name : "comboText",
            type : "string",
            convert : function( v, record ) {
               return record.officialSymbol + " (" + record.taxonCommonName + ")";
            }
         } ] ),

         /**
          * @memberOf Gemma.GeneCombo
          */
         initComponent : function() {

            var template = new Ext.XTemplate(
               '<tpl for="."><div style="font-size:11px" class="x-combo-list-item" ext:qtip="{officialName}'
                  + ' ({[values.taxonScientificName]})"> {officialSymbol} {officialName} ({[values.taxonScientificName]})</div></tpl>' );

            Ext.apply( this, {
               tpl : template,
               store : new Ext.data.Store( {
                  proxy : new Ext.data.DWRProxy( GenePickerController.searchGenes ),
                  reader : new Ext.data.ListRangeReader( {
                     id : "id"
                  }, this.record ),
                  sortInfo : {
                     field : "comboText",
                     dir : "ASC"
                  }
               } )
            } );

            Gemma.GeneCombo.superclass.initComponent.call( this );

            this.addEvents( 'genechanged' );

            this.store.on( "datachanged", function() {
               if ( this.store.getCount() === 0 ) {
                  this.fireEvent( "invalid", "No matching genes" );
               }
            }, this );

            this.on( 'keyup', function() {
               this.actualTextOfLastQuery = '';
            }, this );

            /** *** start of query queue fix **** */
            // this makes sure that when older searches return AFTER newer searches,
            // the newer results aren't bumped
            // this needs the lastQuery property to be initialised as null
            // note that is some other code in this file requried as well, it is
            // marked
            this.getStore().on( 'beforeload', function( store, options ) {
               this.records = this.store.getRange();
            }, this );

            this.getStore().on( 'load', function( store, records, options ) {
               var query = (options.params) ? options.params[0] : null;
               if ( (query === null && this.lastQuery !== null) || (query !== '' && query !== this.lastQuery) ) {
                  store.removeAll();
                  store.add( this.records );
                  if ( this.records === null || this.records.length === 0 ) {
                     this.doQuery( this.lastQuery );
                  }
               } else {
                  this.records = this.store.getRange();
               }
            }, this );
            /** *** end of query queue fix **** */
         },

         onSelect : function( record, index ) {
            Gemma.GeneCombo.superclass.onSelect.call( this, record, index );
            if ( !this.selectedGene || record.data.id !== this.selectedGene.id ) {
               this.setGene( record.data );
               // 'select' event is also fired by superclass.onSelect.call
               this.fireEvent( 'select', this, this.selectedGene );
               // use event 'selectSingle' to get event as fired by super
               this.fireEvent( 'selectSingle', this, record );
            }
            this.actualTextOfLastQuery = this.lastQuery;
         },

         reset : function() {
            Gemma.GeneCombo.superclass.reset.call( this );
            delete this.selectedGene;
            this.lastQuery = null;

            if ( this.tooltip ) {
               this.tooltip.destroy();
            }
         },

         /**
          * Parameters for AJAX call.
          * 
          * @param {}
          *           query
          * @return {}
          */
         getParams : function( query ) {
            // don't want (taxon) in search query (we added that for clarity in the
            // combo text box)
            if ( this.actualTextOfLastQuery ) {
               query = this.actualTextOfLastQuery;
            }
            if ( this.stickyTaxon ) {
               return [ query, this.taxon ? this.taxon.id : -1 ];
            } else {
               return [ query, -1 ];
            }

         },

         getGene : function() {
            if ( this.getRawValue() === '' ) {
               return null;
            }
            return this.selectedGene;
         },

         setGene : function( gene ) {
            if ( this.tooltip ) {
               this.tooltip.destroy();
            }
            if ( gene ) {
               this.selectedGene = gene;
               this.taxon = {
                  id : gene.taxonId,
                  commonName : gene.taxonCommonName,
                  scientificname : gene.taxonScientificName
               };
               this.tooltip = new Ext.ToolTip( {
                  target : this.getEl(),
                  html : String.format( '{0} ({1})', gene.officialName || "no description", gene.taxonScientificName )
               } );
            }
         },

         getTaxon : function() {
            return this.taxon;
         },

         setTaxon : function( taxon ) {
            if ( !this.taxon || this.taxon.id !== taxon.id ) {
               this.taxon = taxon;
               this.reset();

               /*
                * this is to make sure we always search again after a taxon change, in case the user searches for the
                * same gene. Otherwise Ext just keeps the old results.
                */
               this.lastQuery = null;

            }
         }

      } );

// ===================================================//

/**
 * 
 * @class Gemma.GeneSearch
 * @extends Ext.FormPanel
 */

Gemma.GeneSearch = Ext.extend( Ext.FormPanel, {

   autoHeight : true,
   frame : true,
   stateEvents : [ "beforesearch" ],
   labelAlign : "top",
   width : 350,
   height : 30,
   buttonAlign : 'right',
   layout : 'fit',
   stickyTaxon : true,

   /**
    * @memberOf Gemma.GeneSearch
    */
   initComponent : function() {

      Gemma.GeneSearch.superclass.initComponent.call( this );

      this.geneCombo = new Gemma.GeneCombo( {
         hiddenName : 'g',
         id : 'gene-combo',
         fieldLabel : 'Select a gene',
         enableKeyEvents : true,
         stickyTaxon : this.stickyTaxon
      } );

      this.geneCombo.on( "focus", this.clearMessages, this );

      var submitButtonHandler = function( object, event ) {
         var msg = this.validateSearch( this.geneCombo.getValue() );
         if ( msg.length === 0 ) {

            if ( typeof pageTracker !== 'undefined' ) {
               pageTracker._trackPageview( Gemma.CONTEXT_PATH + "/gene/showGene" );
            }
            document.location.href = String.format( Gemma.CONTEXT_PATH + "/gene/showGene.html?id={0}", this.geneCombo.getValue() );
         } else {
            this.handleError( msg );
         }
      };

      var enterButtonPressed = function( object, event ) {

         var keycode = event.getKey();
         if ( keycode === 13 ) { // 13 = keycode for "enter" button

            var msg = this.validateSearch( this.geneCombo.getValue() );
            if ( msg.length === 0 ) {

               if ( typeof pageTracker !== 'undefined' ) {
                  pageTracker._trackPageview( Gemma.CONTEXT_PATH + "/gene/showGene" );
               }
               document.location.href = String.format( Gemma.CONTEXT_PATH + "/gene/showGene.html?id={0}", this.geneCombo.getValue() );
            } else {
               this.handleError( msg );
            }

         }
      };

      this.geneCombo.on( "keypress", enterButtonPressed.createDelegate( this ) );

      var submitButton = new Ext.Button( {
         text : "Go",
         handler : submitButtonHandler.createDelegate( this )
      } );

      this.add( this.geneCombo );
      this.addButton( submitButton );
   },

   validateSearch : function( gene ) {
      if ( !gene || gene.length === 0 ) {
         return "Please select a valid query gene";
      }
      return "";
   },

   handleError : function( msg, e ) {
      Ext.DomHelper.overwrite( "geneSearchMessages", {
         tag : 'img',
         src : Gemma.CONTEXT_PATH + '/images/icons/warning.png'
      } );
      Ext.DomHelper.append( "geneSearchMessages", {
         tag : 'span',
         html : "&nbsp;&nbsp;" + msg
      } );
   },

   clearMessages : function() {
      if ( Ext.DomQuery.select( "geneSearchMessages" ).length > 0 ) {
         Ext.DomHelper.overwrite( "geneSearchMessages", {
            tag : 'h3',
            html : "Gene Query"
         } );
      }
   }

} );
