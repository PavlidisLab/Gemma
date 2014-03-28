/**
 * Panel for selecting experiments
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

Gemma.MetaAnalysisSelectExperimentPanel = Ext.extend( Gemma.WizardTabPanelItemPanel, {
   title : 'Select experiments',
   nextButtonText : 'Select factors',
   initComponent : function() {
      var experimentSearchAndPreview;

      var contentPanel = new Ext.Panel( {
         border : false,
         // Must define taxonChanged function because ExperimentSearchAndPreview requires it.
         taxonChanged : function( taxonId, taxonName ) {
         }
      } );
      var initContentPanel = function() {
         contentPanel.removeAll();

         experimentSearchAndPreview = new Gemma.ExperimentSearchAndPreview( {
            width : 884,
            searchForm : contentPanel,
            listeners : {
               madeFirstSelection : function() {
                  nextButton.enable();
                  resetButton.enable();
               }
            }
         } );
         contentPanel.add( experimentSearchAndPreview );
         contentPanel.doLayout();
      };

      var nextButton = this.createNextButton();
      nextButton.disable();

      var resetButton = new Ext.Button( {
         disabled : true,
         icon : '/Gemma/images/icons/arrow_refresh_small.png',
         margins : '0 0 0 10',
         text : 'Reset',
         handler : function( button, eventObject ) {
            initContentPanel();
            nextButton.disable();
            resetButton.disable();
         }
      } );

      initContentPanel();

      Ext.apply( this, {
         border : false,
         items : [ contentPanel, {
            border : false,
            layout : {
               type : 'hbox',
               padding : '10 0 0 10'
            },
            items : [ nextButton, resetButton ]
         } ],
         getSelectedExperimentOrExperimentSetValueObject : function() {
            return experimentSearchAndPreview.getSelectedExperimentOrExperimentSetValueObject();
         }
      } );

      Gemma.MetaAnalysisSelectExperimentPanel.superclass.initComponent.call( this );
   }
} );
