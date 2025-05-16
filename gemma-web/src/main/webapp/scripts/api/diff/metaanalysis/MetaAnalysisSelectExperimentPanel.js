/**
 * Panel for selecting experiments
 * 
 * @author frances
 *
 */
Ext.namespace( 'Gemma' );

Gemma.MetaAnalysisSelectExperimentPanel = Ext.extend( Gemma.WizardTabPanelItemPanel, {
   title : 'Select experiments',
   nextButtonText : 'Select factors',
   experimentSearchAndPreview : null,

   getSelectedExperimentOrExperimentSetValueObject : function() {
      return this.experimentSearchAndPreview.getSelectedExpressionExperimentSetValueObject();
   },

   /**
    * @memberOf Gemma.MetaAnalysisSelectExperimentPanel
    */
   initComponent : function() {

      var contentPanel = new Ext.Panel( {
         border : false,
         // Must define taxonChanged function because ExperimentSearchAndPreview requires it. //???
         taxonChanged : function( taxonId, taxonName ) {
         }
      } );
      var initContentPanel = function() {
         contentPanel.removeAll();

         this.experimentSearchAndPreview = new Gemma.ExperimentSearchAndPreview( {
            width : 884,
            mode : 'diffex',
            searchForm : contentPanel,
            listeners : {
               madeFirstSelection : function() {
                  nextButton.enable();
                  resetButton.enable();
               }
            }
         } );
         contentPanel.add( this.experimentSearchAndPreview );
         contentPanel.doLayout();
      }.createDelegate( this );

      var nextButton = this.createNextButton();
      nextButton.disable();

      var resetButton = new Ext.Button( {
         disabled : true,
         icon : Gemma.CONTEXT_PATH + '/images/icons/arrow_refresh_small.png',
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

      } );

      Gemma.MetaAnalysisSelectExperimentPanel.superclass.initComponent.call( this );
   }
} );
