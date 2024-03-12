/**
 * Displays summary of an experimental design.
 * 
 *
 * @author Luke, Paul
 * @type
 */
var DesignMatrix = {

   /**
    * @memberOf DesignMatrix
    */
   build : function( rows ) {

      var factors = rows[0].factors;
      var factorTypes = rows[0].factorTypes;
      var record = [];
      var columns = [];
      columns.push( {
         header : "Assays",
         dataIndex : "count",
         sortable : "true",
         tooltip : "How many assays are in this group"
      } );

      for (var i = 0; i < factors.length; ++i) {
         var fn = factors[i];
         record.push( {
            name : cleanName( fn ),
            type : factorTypes[i] === 'CONTINUOUS' ? "number" : "string"
         } );
         columns.push( {
            header : fn,
            dataIndex : cleanName( fn ),
            sortable : "true",
            tooltip : fn
         } );
      }

      record.push( {
         name : "count",
         type : "int"
      } );

      var designMatrixRow = Ext.data.Record.create( record );
      var cm = new Ext.grid.ColumnModel( columns );

      var data = [];
      for (var k = 0; k < rows.length; ++k) {
         data[k] = [];
         for (var j = 0; j < factors.length; ++j) {
            data[k][j] = rows[k].factorValueMap[factors[j]];
         }
         data[k][factors.length] = rows[k].count;
      }
      this.ds = new Ext.data.Store( {
         proxy : new Ext.data.MemoryProxy( data ),
         reader : new Ext.data.ArrayReader( {}, designMatrixRow ),
         remoteSort : false
      } );
      this.ds.load();

      var height = Ext.get( 'eeDesignMatrix' ).getHeight();
      Ext.DomHelper.overwrite( Ext.get( 'eeDesignMatrix' ), '' );
      this.grid = new Ext.grid.GridPanel( {
         ds : this.ds,
         cm : cm,
         title : "Experimental Design overview",
         renderTo : "eeDesignMatrix",
         height : height,
         viewConfig : {
            forceFit : true
         }
      } );
   },

   init : function( entityDelegator ) {
      ExpressionExperimentController.getDesignMatrixRows( entityDelegator, this.build );
   }
};

function cleanName( string ) {
   return string.replace( /[\\\/\"\'\s\(\),\.;\[\]]/g, "_" );
}

// complication is variable number of columns & record fields
Gemma.eeDesignMatrix = Ext.extend( Ext.grid.GridPanel, {
   constructor : function( config ) {
      this.configParam = config;
      Gemma.eeDesignMatrix.superclass.constructor.apply( this, arguments );
   },
   collapsible : true,
   title : "Experimental Design",
   height : 125,
   width : 600,
   cm : new Ext.grid.ColumnModel( [ {
      header : "Assays",
      dataIndex : "count",
      sortable : "true",
      tooltip : "How many assays are in this group"
   } ] ),
   viewConfig : {
      forceFit : true
   },

   /**
    * @memberOf Gemma.eeDesignMatirx
    */
   initComponent : function() {

      var record = [];
      record.push( {
         name : "count",
         type : "int"
      } );
      var columns = [];
      columns.push( {
         header : "Assays",
         dataIndex : "count",
         sortable : "true",
         tooltip : "How many assays are in this group"
      } );

      var designMatrixRow = Ext.data.Record.create( record );
      Ext.apply( this, {
         store : new Ext.data.Store( {
            proxy : new Ext.data.MemoryProxy( data ),
            reader : new Ext.data.ArrayReader( {}, designMatrixRow ),
            remoteSort : false
         } )
      } );

      Gemma.eeDesignMatrix.superclass.initComponent.apply( this );
      ExpressionExperimentController.getDesignMatrixRows( this.configParam, this.build );
   },
   build : function( rows ) {

      var factors = rows[0].factors;
      var record = [];
      var columns = [];
      columns.push( {
         header : "Assays",
         dataIndex : "count",
         sortable : "true",
         tooltip : "How many assays are in this group"
      } );

      for (var i = 0; i < factors.length; ++i) {
         var fn = factors[i];
         record.push( {
            name : cleanName( fn ),
            type : "string"
         } );
         columns.push( {
            header : fn,
            dataIndex : cleanName( fn ),
            sortable : "true",
            tooltip : fn
         } );
      }

      record.push( {
         name : "count",
         type : "int"
      } );

      // var designMatrixRow = Ext.data.Record.create(record);
      Ext.apply( this, {
         cm : new Ext.grid.ColumnModel( columns )
      } );

      var data = [];
      for (var k = 0; k < rows.length; ++k) {
         data[k] = [];
         for (var j = 0; j < factors.length; ++j) {
            data[k][j] = rows[k].factorValueMap[factors[j]];
         }
         data[k][factors.length] = rows[k].count;
      }

      this.store.loadData( data );

   }
} );