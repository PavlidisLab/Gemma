Ext.namespace( 'Gemma' );


/**
 *
 * Top level container for all sections of expression experiment info Sections are: 1. Details (has editing tools) 2.
 * Experimental design 3. Expression visualisation 4. Diagnostics 5. Quantitation Types ? 6. History (admin only) 7.
 * Admin (running analyses)
 *
 *
 * To open the page at a specific tab, include ?tab=[tabName] suffix in the URL. Tab names are each tab's itemId.
 *
 * @class Gemma.ExpressionExperimentPage
 * @extends Ext.TabPanel
 *
 */
Gemma.ExpressionExperimentPage = Ext.extend( Ext.TabPanel, {

   height : 600,
   defaults : {
      autoScroll : true,
      width : 850
   },
   initialTab : 'details',
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

   checkURLforInitialTab : function() {
      this.loadSpecificTab = (document.URL.indexOf( "?" ) > -1 && (document.URL.indexOf( "tab=" ) > -1));
      if ( this.loadSpecificTab ) {
         var param = Ext.urlDecode( document.URL.substr( document.URL.indexOf( "?" ) + 1 ) );
         if ( param.tab ) {
            if ( this.getComponent( param.tab ) !== undefined ) {
               this.initialTab = param.tab;
            }
         }
      }
   },

   /**
    * @memberOf Gemma.ExpressionExperimentPage
    */
   initComponent : function() {

      var eeId = this.eeId;

      var isAdmin = Ext.get( "hasAdmin" ).getValue() == 'true';

      Gemma.ExpressionExperimentPage.superclass.initComponent.call( this );
      this.on( 'render', function() {
         if ( !this.loadMask ) {
            this.loadMask = new Ext.LoadMask( this.getEl(), {
               msg : Gemma.StatusText.Loading.generic,
               msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
            } );
         }
         this.loadMask.show();
      } );

      ExpressionExperimentController.loadExpressionExperimentDetails( eeId, {
         callback : function( experimentDetails ) {
            if ( experimentDetails === null ) throw "Experiment can not be accessed, please log in first.";
            this.initFromExperimentValueObject( experimentDetails, isAdmin );

            this.checkURLforInitialTab();
            this.setActiveTab( this.initialTab );
         }.createDelegate( this ),
         errorHandler : Gemma.Error.genericErrorHandler
      } );

      Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {
         var appScope = this;
         ExpressionExperimentController.canCurrentUserEditExperiment( this.experimentDetails.id, {
            callback : function( editable ) {
               // console.log(this);
               appScope.adjustForIsAdmin( isAdmin, editable );
            },
            scope : appScope
         } );

      }, this );

      Gemma.Application.currentUser.on( "logOut", function() {

         this.adjustForIsAdmin( false, false );

      }, this );
   },

   initFromExperimentValueObject : function( experimentDetails, isAdmin ) {

      /**
       * The ExpressionExperimentValueObject - see the Java object for details.
       *
       * The following is here to hide JS warnings for unresolved variables.
       * @param experimentDetails.id
       * @param experimentDetails.currentUserHasWritePermission
       * @param experimentDetails.currentUserIsOwner
       * @param experimentDetails.hasBatchInformation
       * @param experimentDetails.batchConfound
       * @param experimentDetails.batchEffect
       * @param experimentDetails.troubled
       * @param experimentDetails.troubleDetails
       * @param experimentDetails.reprocessedFromRawData
       * @param experimentDetails.QChtml
       * @param experimentDetails.hasMultiplePreferredQuantitationTypes
       * @param experimentDetails.hasMultipleTechnologyTypes
       * @param experimentDetails.externalDatabase
       * @param experimentDetails.coexpressionLinkCount
       * @param experimentDetails.bioAssayCount
       * @param experimentDetails.dateLinkAnalysis
       * @param experimentDetails.technologyType
       * @param experimentDetails.hasEitherIntensity
       * @param experimentDetails.dateMissingValueAnalysis
       * @param experimentDetails.dateProcessedDataVectorComputation
       * @param experimentDetails.dateDifferentialAnalysis
       * @param experimentDetails.differentialAnalysisEventType
       * @param experimentDetails.pubmedId
       * @param experimentDetails.expressionExperimentSets
       * @param experimentDetails.lastArrayDesignUpdateDate
       * @param experimentDetails.needsAttention
       * @param experimentDetails.geeq
       * @param experimentDetails.geeq.publicQualityScore
       * @param experimentDetails.geeq.publicSuitabilityScore
       * @param experimentDetails.isRNASeq
       */
      this.experimentDetails = experimentDetails;
      this.editable = experimentDetails.currentUserHasWritePermission || isAdmin;
      this.ownedByCurrentUser = experimentDetails.currentUserIsOwner;

      if ( this.loadMask ) {
         this.loadMask.hide();
      }

      // DETAILS TAB
      this.add( this.makeDetailsTab( experimentDetails ) );

      // EXPERIMENT DESIGN TAB
      this.add( this.makeDesignTab( experimentDetails ) );

      // VISUALISATION TAB
      this.add( this.makeVisualisationTab( experimentDetails, isAdmin ) );

      // DIAGNOSTICS TAB
      this.add( this.makeDiagnosticsTab( experimentDetails, isAdmin ) );

      this.adjustForIsAdmin( isAdmin, this.editable );

   },

   makeDetailsTab : function( experimentDetails ) {
      return new Gemma.ExpressionExperimentDetails( {
         title : 'Overview',
         itemId : 'details',
         id : 'ee-details-panel',
         experimentDetails : experimentDetails,
         editable : this.editable,
         owned : this.ownedByCurrentUser,
         admin : this.admin,
         listeners : {
            'experimentDetailsReloadRequired' : function() {
               var myMask = new Ext.LoadMask( Ext.getBody(), {
                  msg : "Refreshing..."
               } );
               myMask.show();
               window.location.reload( false ); // could do something fancier like reloading just
               // the component
            },
            scope : this
         }
      } );
   },

   makeDesignTab : function( experimentDetails ) {
      var batchInfo = '<div class="ed-batch-info">' + Gemma.GEEQ.getBatchInfoBadges( experimentDetails ) + '</div>';

      return {
         title : 'Experimental Design',
         tbar : [ {
            text : 'Show Details',
            itemId : 'design',
            tooltip : 'Go to the design details',
            icon : Gemma.CONTEXT_PATH + '/images/magnifier.png',
            handler : function() {
               window.open( Gemma.CONTEXT_PATH + "/experimentalDesign/showExperimentalDesign.html?eeid=" + experimentDetails.id );
            }
         } ],
         html : batchInfo + "<span style='font-size:smaller;padding:4px'>Continuous factors are not shown in this view</span> " + '<div id="eeDesignMatrix" style="height:100%">Loading...</div>',
         layout : 'absolute',
         listeners : {
            render : function() {
               Gemma.DesignMatrix.init( {
                  id : experimentDetails.id
               } );
            }
         }
      };
   },

   makeVisualisationTab : function( experimentDetails, isAdmin ) {
      var eeId = this.eeId;
      var title = "Data for a 'random' sampling of probes";
      var geneList = [];
      var downloadLink = String.format( Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}", eeId );
      var viz = new Gemma.VisualizationWithThumbsPanel( {
         thumbnails : false,
         downloadLink : downloadLink,
         params : [ [ eeId ], geneList ]
      } );
      viz.on( 'render', function() {
         viz.loadFromParam( {
            params : [ [ eeId ], geneList ]
         } );
      } );

      var geneTBar = new Gemma.VisualizationWidgetGeneSelectionToolbar( {
         eeId : eeId,
         visPanel : viz
         , taxonId : experimentDetails.taxonId
         // ,showRefresh : (isAdmin || this.editable)
      } );
      geneTBar.on( 'refreshVisualisation', function() {
         viz.loadFromParam( {
            params : [ [ eeId ], geneList ]
         } );
      } );
      return {
         items : viz,
         itemId : 'visualize',
         layout : 'fit',
         padding : 0,
         title : 'Visualize Expression',
         tbar : geneTBar
      };
   },


   makeDiagnosticsTab : function( experimentDetails, isAdmin ) {

      var metaRow = new Ext.Panel(
         {
            fieldLabel : 'Preprocessing metadata',
            id : 'metadata-row',
            border : false
         }
      );

      this.renderMetadata( experimentDetails, metaRow );

      return {
         title : 'Diagnostics',
         itemId : 'diagnostics',
         items : [
            //   this.refreshDiagnosticsBtn,
            {
               baseCls : 'x-plain-panel',
               bodyStyle : 'padding:10px',
               html : experimentDetails.QChtml,
               border : false
            },
            new Ext.Panel( {
               baseCls : 'x-plain-panel',
               bodyStyle : 'padding:10px',
               ref : 'metadataPanel',
               border : false,
               items : [
                  {
                     layout : 'form',
                     defaults : {
                        border : false
                     },
                     items : [
                        metaRow
                     ]
                  }
               ]
            } )
         ]
      };

   },

   renderMetadata : function( ee, metaRow ) {

      ExpressionExperimentDataFetchController.getMetadataFiles( ee.id, {
         callback : function( files ) {
            var result = "";
            var hasFiles = false;

            files.forEach( function( file ) {
               if ( file != null ) {
                  hasFiles = true;
                  result +=
                     "<div class='v-padded'>" +
                     "   <a target='_blank' href='" + Gemma.CONTEXT_PATH + "/getMetaData.html?eeId="
                     + ee.id + "&typeId=" + file.typeId + "' ext:qtip='Download file " + file.displayName
                     + "'><i class='gray-blue fa fa-download'></i> " + file.displayName + "</a>" +
                     "</div>";
               }
            } );


            if ( !hasFiles ) {
               result = "<span class='dark-gray'> Not available for this experiment </span>";
            }

            metaRow.html = result;
         }
      } );

      return "";
   },

   adjustForIsAdmin : function( isAdmin, isEditable ) {

      // QUANTITATION TYPES TAB
      if ( (isAdmin || isEditable) && !this.qtTab ) {
         this.qtTab = new Gemma.ExpressionExperimentQuantitationTypeGrid( {
            title : 'Quantitation Types',
            itemId : 'quantitation',
            eeid : this.experimentDetails.id
         } );
         this.add( this.qtTab );
      } else if ( this.qtTab ) {
         this.qtTab.setVisible( (isAdmin || isEditable) );
      }

      /* HISTORY TAB */
      if ( (isAdmin || isEditable) && !this.historyTab ) {
         this.historyTab = new Gemma.AuditTrailGrid( {
            title : 'History',
            itemId : 'history',
            bodyBorder : false,
            collapsible : false,
            viewConfig : {
               forceFit : true
            },
            auditable : {
               id : this.experimentDetails.id,
               classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperiment"
            },
            loadOnlyOnRender : true
         } );
         this.add( this.historyTab );
      } else if ( this.historyTab ) {
         this.historyTab.setVisible( (isAdmin || isEditable) );
      }

      /* ADMIN TOOLS TAB */
      if ( (isAdmin || isEditable) && !this.toolTab ) {
         this.toolTab = new Gemma.ExpressionExperimentTools( {
            experimentDetails : this.experimentDetails,
            title : 'Admin & Curation',
            itemId : 'admin',
            editable : isEditable,
            listeners : {
               'reloadNeeded' : function() {
                  var myMask = new Ext.LoadMask( Ext.getBody(), {
                     msg : "Refreshing..."
                  } );
                  myMask.show();
                  var reloadToAdminTab = document.URL;
                  reloadToAdminTab = reloadToAdminTab.replace( /&*tab=\w*/, '' );
                  reloadToAdminTab += '&tab=admin';
                  window.location.href = reloadToAdminTab;

               }
            }
         } );
         this.add( this.toolTab );
      } else if ( this.toolTab ) {
         this.toolTab.setVisible( (isAdmin || isEditable) );
      }
   }
} );

/**
 * Used to make the correlation heatmap clickable.
 *
 * See ExperimentQCTag.java
 * @param url    an URL to the image
 * @param width  the width of the image to generate, in pixels
 * @param height the height of the image to generate, in pixels
 */
Gemma.ExpressionExperimentPage.popupImage = function( url, width, height ) {
   url = url + "&nocache=" + Math.floor( Math.random() * 1000 );
   var b = new Ext.Window( {
      modal : true,
      stateful : false,
      resizable : true,
      autoScroll : true,
      height : height, // or false.
      width : width || 200,
      padding : 10,
      html : '<img src=\"' + url + '"\" />'
   } );
   b.show();
};
