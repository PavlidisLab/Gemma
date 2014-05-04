Ext.namespace( "Gemma" );

/**
 * Provide a drop-down menu populated with array designs.
 * 
 * @class Gemma.ArrayDesignCombo
 * @extends Ext.form.ComboBox
 */
Gemma.ArrayDesignCombo = Ext
   .extend(
      Gemma.StatefulRemoteCombo,
      {

         displayField : 'name',
         valueField : 'id',
         editable : true,
         loadingText : "Loading ...",
         listWidth : 550,
         forceSelection : true,
         typeAhead : true,
         triggerAction : 'all',
         emptyText : 'Select a platform',

         stateId : 'Gemma.ArrayDesign',

         record : Ext.data.Record.create( [ {
            name : "id",
            type : "int"
         }, {
            name : "name",
            type : "string"
         }, {
            name : "description",
            type : "string"
         }, {
            name : "taxon"
         }, {
            name : "shortName",
            type : "string"
         }, {
            name : "troubled",
            type : "boolean"
         } ] ),

         /**
          * @memberOf Gemma.ArrayDesignCombo
          */
         initComponent : function() {

            var templ = new Ext.XTemplate(
               '<tpl for="."><div ext:qtip="{description}" class="x-combo-list-item"><tpl if="troubled"><img src="/Gemma/images/icons/stop.png" /></tpl>{shortName} - {name}</div></tpl>' );

            Ext.apply( this, {
               store : new Ext.data.Store( {
                  sortInfo : {
                     field : 'name',
                     direction : 'ASC'
                  },
                  proxy : new Ext.data.DWRProxy( ArrayDesignController.getArrayDesigns ),
                  reader : new Ext.data.ListRangeReader( {
                     id : "id"
                  }, this.record ),
                  remoteSort : false
               } ),
               tpl : templ
            } );

            Gemma.ArrayDesignCombo.superclass.initComponent.call( this );

            this.store.on( 'load', function() {
               this.taxonChanged( this.taxon );
            }, this );

            this.store.load( {
               params : [ [], false, true ],
               scope : this,
               add : false
            } );

            this.doQuery();

            this.addEvents( 'arrayDesignchanged' );
         },

         setValue : function( v ) {
            var changed = false;
            if ( this.getValue() !== v ) {
               changed = true;
            }

            // if setting to a filtered value, reset the filter.
            if ( changed && this.store.isFiltered() ) {
               this.store.clearFilter();
            }

            Gemma.ArrayDesignCombo.superclass.setValue.call( this, v );

            if ( changed ) {
               this.fireEvent( 'arrayDesignchanged', this.getArrayDesign() );
            }
         },

         getArrayDesign : function() {
            var ArrayDesign = this.store.getById( this.getValue() );
            return ArrayDesign;
         },

         taxonChanged : function( taxon ) {

            if ( taxon === undefined ) {
               return;
            }

            this.taxon = taxon;
            if ( this.getArrayDesign() && this.getArrayDesign().taxon && this.getArrayDesign().taxon.id !== taxon.id ) {
               this.reset();
            }
            this.applyFilter( taxon );
         },

         applyFilter : function( taxon ) {

            if ( taxon === undefined ) {
               return;
            }

            this.store.filterBy( function( record, id ) {
               if ( !record.data.taxon || record.data.taxon.indexOf( taxon.commonName ) === -1 ) {
                  return false;
               } else {
                  return true;
               }
            } );
         },

         clearCustom : function() {
            var rec = this.store.getById( -1 );
            if ( rec ) {
               this.store.remove( rec );
            }
         }

      } );

Ext.reg( 'ArrayDesigncombo', Gemma.ArrayDesignCombo );