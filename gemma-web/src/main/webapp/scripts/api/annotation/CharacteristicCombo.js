Ext.namespace( "Gemma" );

/**
 * Remote-mode combo-box that queries for characteristics with type-ahead.
 *
 * @class Gemma.CharacteristicCombo
 * @extends Ext.form.ComboBox
 */
Gemma.CharacteristicCombo = Ext.extend( Ext.form.ComboBox, {

   loadingText : "Searching...",
   minChars : 2,
   selectOnFocus : true,
   listWidth : 350,
   taxonId : null, // set on initialization
   name : 'characteristicCombo',
   lazyInit : false,
   tpl : new Ext.XTemplate(
      '<tpl for="."><div ext:qtip="{description}" style="font-size:11px" class="x-combo-list-item {style}">{value}{[this.trimUri(values.valueUri)]}</div></tpl>',
      {
         trimUri : function( uri ) {
            if ( uri != null && uri != "" ) {
                return ' - (' + uri.replace( /.+\//g, "" ) + ')';
            }
            return '';
         }
      } ),


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
            name : "valueDefinition",
            type : "string"
         }, {
            name : "description",
            mapping : "this",
            convert : this.getHover.createDelegate( this, null, false )
         }, {
            name : "style",
            mapping : "this",
            convert : this.getStyle.createDelegate( this, null, false )
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
                        return [ request.params[0], null ];
                     }.createDelegate( this, null, false )
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

      // FIXME do we use the category? since that's in a separate combo etc.
      this.characteristic = {
         category : null,
         categoryUri : null,
         value : null,
         valueUri : null,
         isSelection : false // fixme
      };

      this.on( "select", function( combo, record, index ) {

         var o = record.data.value;
         var sameValue = this.characteristic.value === o;

         this.characteristic.value = record.data.value;
         this.characteristic.valueUri = record.data.valueUri;
         this.characteristic.isSelection = true;

         // ensure this gets marked as dirty. I can't find a cleaner way.
         // but the value is only used for display.
         if (sameValue) {
            combo.setValue( record.data.value + " ");
         } else {
            combo.setValue( record.data.value );
         }
      } );

      // happens when we are doing free text or when combo loses focus
      this.on( "change", function( combo, newValue, oldValue ) {
         this.characteristic.value = newValue;
         if ( !this.characteristic.isSelection ) {
            // then user typed in free text. FIXME this doesn't have the desired effect
            this.characteristic.valueUri = null;
         }
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

   getCharacteristic : function() {
      return this.characteristic;
   },

   // setCharacteristic: function (value, valueUri, category, categoryUri) {
   //     this.characteristic.value = value;
   //     this.characteristic.valueUri = valueUri;
   //     this.characteristic.category = category;
   //     this.characteristic.categoryUri = categoryUri;
   //     this.setValue(value);
   // },

   clearCharacteristic : function() {
      this.characteristic.value = null;
      this.characteristic.valueUri = null;
      this.setValue( "" );
      // do not clear the category as it is probably still being displayed; it will be reset when the container updates it.
      //   this.characteristic.category = null;
      //   this.characteristic.categoryUri = null;
      this.setValue( "" );
   },

   /**
    * Used to store the category information when container's category combo changes. This is a bit weird but at least information is all in one place
    * @param category
    * @param categoryUri
    */
   setCategory : function( category, categoryUri ) {
      this.characteristic.category = category;
      this.characteristic.categoryUri = categoryUri;
   },

   getHover : function( value, record ) {
      var k = (record.valueUri && record.valueUri != null && record.valueUri != "") ? record.valueUri
         : (record.category && record.category != null && record.category != "") ? record.category : '';

      if ( record.numTimesUsed > 0 ) {
         k = k + " (" + record.numTimesUsed + ")";
      }

      if ( record.valueDefinition != null && record.valueDefinition != "" ) {
         k = k + "</br>[" + record.valueDefinition + "]"; /// may need more formatting
      }

      return k;
   },
   getStyle : function( value, record ) {
      var isUsed = record.alreadyPresentInDatabase ? 'used' : 'unused';
      var hasURI = (record.valueUri && record.valueUri != null && record.valueUri != "") ? "WithUri" : "NoUri";
      return isUsed + hasURI;
   }

} );
