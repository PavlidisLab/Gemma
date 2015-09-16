Ext.namespace( "Gemma" );

/**
 * Remote-mode combo-box that queries for characteristics with type-ahead.
 * 
 * @class Gemma.CharacteristicCombo
 * @extends Ext.form.ComboBox
 * @version $Id$
 */
Gemma.CharacteristicCombo = Ext
   .extend(
      Ext.form.ComboBox,
      {

         loadingText : "Searching...",
         minChars : 2,
         selectOnFocus : true,
         listWidth : 350,
         taxonId : null, // set on initialization
         name : 'characteristicCombo',
         lazyInit : false,
         tpl : new Ext.XTemplate(
            '<tpl for="."><div ext:qtip="{description}" style="font-size:11px" class="x-combo-list-item {style}">{value}</div></tpl>' ),

         /**
          * @Override
          */
         initComponent : function() {

            Ext.apply( this, {

               record : Ext.data.Record.create( [ {
                  name : "id",
                  type : "int"
               }, {
                  name : "value",
                  type : "string"
               }, {
                  name : "valueUri",
                  type : "string"
               }, {
                  name : "categoryUri",
                  type : "string"
               }, {
                  name : "alreadyPresentInDatabase",
                  type : "boolean"
               }, {
                  name : "numTimesUsed",
                  type : "int"
               }, {
                  name : "category",
                  type : "string"
               }, {
                  name : "description",
                  mapping : "this",
                  convert : this.getHover.createDelegate( this )
               }, {
                  name : "style",
                  mapping : "this",
                  convert : this.getStyle.createDelegate( this )
               } ] )
            } );

            Ext.apply( this, {
               store : new Ext.data.Store( {
                  proxy : new Ext.data.DWRProxy( {
                     apiActionToHandlerMap : {
                        read : {
                           dwrFunction : AnnotationController.findTerm,
                           getDwrArgsFunction : function( request ) {
                              // This is only used when directly calling 'load', which is not
                              // usual but can be used for tests.
                              // NOTE decided not to pass this.taxonId.
                              return [ request.params[0], this.characteristic.categoryUri, null ];
                           }.createDelegate( this )
                        }
                     }
                  } ),
                  reader : new Ext.data.ListRangeReader( {
                     id : "id"
                  }, this.record ),
                  remoteSort : true
               } )
            } );

            Gemma.CharacteristicCombo.superclass.initComponent.call( this );

            this.characteristic = {
               category : null,
               categoryUri : null,
               value : null,
               valueUri : null
            };

            this.on( "select", function( combo, record, index ) {
               this.characteristic.value = record.data.value;
               this.characteristic.valueUri = record.data.valueUri;
               /*
                * The addition of '\t' is a complete hack to workaround an extjs limitation. It's to make sure extjs
                * knows we want it to detect a change. See bug 1811
                */
               combo.setValue( record.data.value + "\t" );
            } );

         },

         /**
          * This is used when doing live-search - called by 'doQuery'.
          * 
          * @Override so we can have more than one parameter passed.
          */
         getParams : function( query ) {
            return [ query, this.characteristic.categoryUri, this.taxonId ];
         },

         /**
          * 
          * @return {}
          */
         getCharacteristic : function() {

            /*
             * check to see if the user has typed anything in the combo box (rather than selecting something); if they
             * have, remove the URI from the characteristic and update its value, so we end up with a plain text. See
             * note about hack '\t' above.
             */
            if ( this.getValue() != this.characteristic.value + "\t" ) {
               this.characteristic.value = this.getValue();
               this.characteristic.valueUri = null;
            }

            /*
             * if we don't have a valueUri or categoryUri set, don't return URI fields or a VocabCharacteristic will be
             * created when we only want a Characteristic...
             */
            return (this.characteristic.valueUri !== null || this.characteristic.categoryUri !== null) ? this.characteristic
               : {
                  category : this.characteristic.category,
                  value : this.characteristic.value
               };
         },

         /**
          * 
          * @param {}
          *           value
          * @param {}
          *           valueUri
          * @param {}
          *           category
          * @param {}
          *           categoryUri
          */
         setCharacteristic : function( value, valueUri, category, categoryUri ) {
            this.characteristic.value = value;
            this.characteristic.valueUri = valueUri;
            this.characteristic.category = category;
            this.characteristic.categoryUri = categoryUri;
            this.setValue( value );
         },

         /**
          * 
          * @param {}
          *           category
          * @param {}
          *           categoryUri
          */
         setCategory : function( category, categoryUri ) {
            this.characteristic.category = category;
            this.characteristic.categoryUri = categoryUri;
         },

         getHover : function( value, record ) {
            var k = (record.valueUri && record.valueUri != null && record.valueUri != "") ? record.valueUri
               : (record.category && record.category != null && record.category != "") ? record.category : '';

            if ( record.numTimesUsed > 0 ) {
               return k + " (" + record.numTimesUsed + ")";
            }

            return k;
         },
         getStyle : function( value, record ) {
            var isUsed = record.alreadyPresentInDatabase ? 'used' : 'unused';
            var hasURI = (record.valueUri && record.valueUri != null && record.valueUri != "") ? "WithUri" : "NoUri";
            return isUsed + hasURI;
         }

      } );
