Ext.namespace( "Gemma.Search" );
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

Gemma.Search.GeneralSearchSimple = Ext.extend( Ext.Panel, {
   layout : 'hbox',
   height : 60,
   padding : 5,
   layoutConfig : {
      align : 'middle',
      pack : 'start'
   },
   defaults : {
      border : false,
      margins : 'margin-right: 5px;'
   },
   border : false,
   style : 'margin-left: 0px; border: solid 1px #B5B8C8;',
   enableKeyEvents : true,

   /**
    * @memberOf Gemma.Search.GeneralSearchSimple
    */
   initComponent : function() {
      Ext.apply( this, {
         items : [ {
            xtype : 'box',
            html : 'Search for',
            cls : 'front-page-header-text',
            margins : {
               top : 0,
               right : 10,
               bottom : 0,
               left : 20
            }
         }, {
            ref : 'queryField',
            xtype : 'textfield',
            emptyText : 'by keyword, ID...',
            listeners : {
               'specialkey' : function( field, e ) {
                  if ( e.getKey() == e.ENTER ) {
                     this.runSearch();
                  }
               },
               scope : this
            }
         }, {
            xtype : 'button',
            text : 'Go!',
            handler : this.runSearch,
            scope : this
         } ]
      } );
      Gemma.Search.GeneralSearchSimple.superclass.initComponent.call( this );
   },
   runSearch : function() {
      var searchURL = this.getSearchURL();
      window.location.href = searchURL;
   },
   getSearchURL : function() {
      var query = this.queryField.getValue();
      var url = Gemma.CONTEXT_PATH + "/searcher.html?query=" + query;
      return url;
   }
} );

Gemma.Search.GeneralSearchSimpleCombo = Ext.extend( Ext.form.ComboBox, {
   mode : 'local',
   triggerAction : 'all',
   disableKeyFilter : true,
   editable : false,
   forceSelection : true,
   lazyInit : false,
   value : 'experiments',
   boxLabel : 'Search for',
   store : new Ext.data.ArrayStore( {
      fields : [ 'myTextId', 'displayText' ],
      data : [ [ 'experiments', 'datasets' ] ,[ 'genes', 'genes' ] ]
   } ),
   valueField : 'myTextId',
   displayField : 'displayText',
   listeners : {
      scope : this,
      'select' : function( combo, record, index ) {
      }
   },
   searchOptionToLinkScopeLetters : {
      everything : [ "P", "G", "E", "A", "M", "H", "N", "B" ],
      genes : [ "G" ],
      experiments : [ "E" ]
   },
   /**
    * returns an array of letters that can be used to code for the scope in a searcher.html URL query
    */
   getURLScopeLetters : function() {
      var value = this.getValue();
      return this.searchOptionToLinkScopeLetters[value];
   }
} );
Ext.reg( 'search.GeneralSearchSimpleCombo', Gemma.Search.GeneralSearchSimpleCombo );