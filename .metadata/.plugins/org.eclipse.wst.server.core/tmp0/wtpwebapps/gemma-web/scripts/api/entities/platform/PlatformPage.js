Ext.namespace( 'Gemma' );

Gemma.DIFF_THRESHOLD = 0.01;
Gemma.MAX_DIFF_RESULTS = 125;
/**
 * 
 * Top level container for all sections of platform info
 * 
 * To open the page at a specific tab, include ?tab=[tabName] suffix in the URL. Tab names are each tab's itemId.
 * 
 * @class Gemma.PlatformPage
 * @extends Ext.TabPanel
 * 
 */

Gemma.PlatformPage = Ext.extend( Ext.TabPanel, {

   defaults : {
      width : 850
   },
   deferredRender : true,
   listeners : {
      'tabchange' : function( tabPanel, newTab ) {
         newTab.fireEvent( 'tabChanged' );
      },
      'beforetabchange' : function( tabPanel, newTab, currTab ) {
         // if false is returned, tab isn't changed
         if ( currTab ) {
            return currTab.fireEvent( 'leavingTab' );
         }
         return true;
      }
   },

   /**
    * @memberOf Gemma.P
    */
   initComponent : function() {

      var platformId = this.platformId;
      var isAdmin = Ext.get( "hasAdmin" ).getValue() == 'true';

      Gemma.PlatformPage.superclass.initComponent.call( this );

      // DETAILS TAB
      var details = new Gemma.PlatformDetails( {
         title : 'Overview',
         itemId : 'details',
         platformId : platformId
      } );
      details.on( 'changeTab', function( tabName ) {
         this.setActiveTab( tabName );
      }, this );
      this.add( details );

      this.add( new Gemma.PlatformElementsPanel( {
         title : 'Elements',
         itemId : 'elements',
         loadOnlyOnRender : true,
         platformId : platformId
      } ) );

      this.add( new Gemma.ExpressionExperimentGrid( {
         title : 'Experiments',
         itemId : 'experiments',
         loadOnlyOnRender : true,
         platformId : platformId
      } ) );

      this.adjustForIsAdmin( isAdmin );

      // duplicated from GenePage, could refactor
      this.loadSpecificTab = (document.URL.indexOf( "?" ) > -1 && (document.URL.indexOf( "tab=" ) > -1));
      if ( this.loadSpecificTab ) {
         var param = Ext.urlDecode( document.URL.substr( document.URL.indexOf( "?" ) + 1 ) );
         if ( param.tab ) {
            if ( this.getComponent( param.tab ) != undefined ) {
               initialTab = param.tab;
            }
         }
      }

      // duplicated from experiment details
      Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {
         var appScope = this;
         appScope.adjustForIsAdmin( isAdmin );
      }, this );
      Gemma.Application.currentUser.on( "logOut", function() {
         this.adjustForIsAdmin( false, false );
      }, this );

      var initialTab = 'details';
      this.on( 'render', function() {
         this.setActiveTab( initialTab );
      } );
   },

   // hide/show 'refresh' link to admin tabs.
   adjustForIsAdmin : function( isAdmin ) {
      /* HISTORY TAB */
      if ( isAdmin && !this.historyTab ) {
         this.historyTab = new Gemma.AuditTrailGrid( {
            title : 'History',
            itemId : 'history',
            bodyBorder : false,
            collapsible : false,
            viewConfig : {
               forceFit : true
            },
            auditable : {
               id : this.platformId,
               classDelegatingFor : "ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"
            },
            loadOnlyOnRender : true
         } );
         this.add( this.historyTab );
      } else if ( this.historyTab ) {
         this.historyTab.setVisible( isAdmin );
      }

      /* ADMIN TOOLS TAB */
      if ( isAdmin && !this.toolTab ) {
         // this.toolTab = new Gemma.ExpressionExperimentTools( {
         // experimentDetails : this.experimentDetails,
         // title : 'Admin',
         // itemId : 'admin',
         // editable : isEditable,
         // listeners : {
         // 'reloadNeeded' : function() {
         // var myMask = new Ext.LoadMask( Ext.getBody(), {
         // msg : "Refreshing..."
         // } );
         // myMask.show();
         // var reloadToAdminTab = document.URL;
         // reloadToAdminTab = reloadToAdminTab.replace( /&*tab=\w*/, '' );
         // reloadToAdminTab += '&tab=admin';
         // window.location.href = reloadToAdminTab;
         //
         // }
         // }
         // } );
         // this.add( this.toolTab );
      } else if ( this.toolTab ) {
         // this.toolTab.setVisible( isAdmin );
      }
   }

} );
