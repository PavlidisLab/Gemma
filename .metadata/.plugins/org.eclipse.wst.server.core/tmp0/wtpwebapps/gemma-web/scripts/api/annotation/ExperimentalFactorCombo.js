/**
 * 
 */
Gemma.ExperimentalFactorCombo = Ext
   .extend(
      Ext.form.ComboBox,
      {

         // fixme only show disc.
         // if it differs from
         // name.
         displayField : 'name',
         tpl : new Ext.XTemplate(
            '<tpl for="."><div class="x-combo-list-item">{name} <tpl if="name != description && description" > ({description})</tpl></div></tpl>' ),
         listWidth : 250,

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
            name : "category",
            type : "string"
         }, {
            name : "categoryUri",
            type : "string"
         } ] ),

         valueField : "id",
         editable : false,
         mode : "local",
         triggerAction : "all",

         /**
          * @memberOf Gemma.ExperimentalFactorCombo
          */
         initComponent : function() {

            this.experimentalDesign = {
               id : this.edId,
               classDelegatingFor : "ExperimentalDesign"
            };

            Ext.apply( this, {
               store : new Ext.data.Store( {
                  proxy : new Ext.data.DWRProxy( ExperimentalDesignController.getExperimentalFactors ),
                  reader : new Ext.data.ListRangeReader( {
                     id : "id"
                  }, this.record ),
                  remoteSort : false,
                  sortInfo : {
                     field : "name"
                  }
               } )
            } );

            Gemma.ExperimentalFactorCombo.superclass.initComponent.call( this );

            this.store.load( {
               params : [ this.experimentalDesign ]
            } );
         }

      } );
