Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = ctxBasePath + '/images/default/s.gif';

/**
 *
 * Used as one tab of the EE page - the "Admin" tab.
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Gemma.CurationTools
 *
 */
Gemma.ExpressionExperimentTools = Ext.extend( Gemma.CurationTools, {

   allowScoreOverride : false,
   experimentDetails : null,
   tbar : new Ext.Toolbar(),
   bconfFolded : true,
   beffFolded : true,
   qualFolded : true,
   suitFolded : true,

   /**
    * @memberOf Gemma.ExpressionExperimentTools
    */
   initComponent : function() {
      this.curatable = this.experimentDetails;
      this.auditable = {
         id : this.experimentDetails.id,
         classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperiment"
      };
      Gemma.ExpressionExperimentTools.superclass.initComponent.call( this );
      var manager = new Gemma.EEManager( {
         editable : this.editable
      } );
      manager.on( 'reportUpdated', function() {
         this.fireEvent( 'reloadNeeded' );
      }, this );

      var self = this;

      var eeRow = new Ext.Panel( {
         cls : 'ee-tool-row',
         defaults : {
            width : '100%',
            border : false,
            padding : 2
         }
      } );

      eeRow.add( {
         html : '<hr class="normal"/>'
      } );

      var refreshButton = new Ext.Button( {
         text : '<i title="Refresh preprocessing statistics" class="fa fa-refresh fa-fw"/>',
         cls : 'btn-refresh nobreak',
         tooltip : 'Refresh preprocessing statistics',
         handler : function() {
            manager.updateEEReport( this.experimentDetails.id );
         },
         scope : this
      } );

      var leftPanel = new Ext.Panel( {
         cls : 'ee-tool-left',
         defaults : {
            border : false,
            padding : 2
         }
      } );

      leftPanel.add( {cls : 'nobreak', html : '<h4>Preprocessing:</h4>'} );
      leftPanel.add( refreshButton );

      /* This does all preprocessing */
      leftPanel.add( this.processedVectorCreatePanelRenderer( this.experimentDetails, manager ) );

      leftPanel.add( this.diagnosticsPanelRenderer( this.experimentDetails, manager ) );
      leftPanel.add( this.batchPanelRenderer( this.experimentDetails, manager ) );

      var batchEffectPanel = this.batchEffectRenderer( this.experimentDetails, manager );
      var batchConfoundPanel = this.batchConfoundRenderer( this.experimentDetails, manager );

      leftPanel.add( {html : "<br/><h4>Batch effects:</h4>"} );
      if ( batchEffectPanel !== null ) leftPanel.add( batchEffectPanel );
      if ( batchConfoundPanel !== null ) leftPanel.add( batchConfoundPanel );

      leftPanel.add( {html : "<br/><h4>Analyses:</h4>"} );
      leftPanel.add( this.differentialAnalysisPanelRenderer( this.experimentDetails, manager ) );

      //  leftPanel.add(this.linkAnalysisPanelRenderer(this.experimentDetails, manager));

      eeRow.add( leftPanel );

      var rightPanel = new Ext.Panel( {
         cls : 'ee-tool-right',
         defaults : {
            border : false,
            padding : 2
         }
      } );

      if ( this.experimentDetails.geeq ) {
         if ( this.experimentDetails.geeq.otherIssues && this.experimentDetails.geeq.otherIssues.trim() ) {
            rightPanel.add( {
               html :
                  "<div class='gq-errors'>" +
                  "<span class='icon'><i class='red fa-exclamation-triangle fa'></i></span>" +
                  "<span class='msg'>" +
                  "   <p>There were some issues while scoring this experiment:</p>" +
                  "   <pre>" + this.experimentDetails.geeq.otherIssues + "</pre>" +
                  "</span>" +
                  "</div>" +
                  "<hr class='normal'/>"
            } )
         }
         rightPanel.add( this.qualityRenderer( this.experimentDetails, manager ) );
         rightPanel.add( this.suitabilityRenderer( this.experimentDetails, manager ) );
      } else {
         rightPanel.add( {
            html :
               '<h4>Quality / Suitability</h4>' +
               '<div>Quality and Suitability not calculated for this experiment</div>'
         } )
      }

      var gqRecalcButton = new Ext.Button( {
         text : "<i class='fa fa-refresh fa-fw'></i>Recalculate score and refresh page (takes a minute)",
         tooltip :
            'Runs full scoring. This usually takes around 1 minute to complete, but can take up to several minutes for large experiments.\n' +
            'Page will refresh after this task has been finished',
         cls : 'gq-btn btn-refresh gq-btn-recalc-all',
         handler : function( b, e ) {
            b.setText( "<i class='fa fa-refresh fa-fw fa-spin'></i>Recalculate score and refresh page (takes a minute)" );
            b.setDisabled( true );
            ExpressionExperimentController.runGeeq( self.experimentDetails.id, "all", {
               callback : function() {
                  window.location.reload();
               }
            } );
         },
         scope : this
      } );

      var recalcButtonWrap = new Ext.Panel( {
         cls : 'extjs-sucks',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      recalcButtonWrap.add( gqRecalcButton );
      rightPanel.add( recalcButtonWrap );

      eeRow.add( rightPanel );

      this.add( eeRow );
   },

   suitabilityRenderer : function( ee, mgr ) {
      var panel = new Ext.Panel( {
         defaults : {
            border : false,
            padding : 0
         },
         items : [ {
            html : '<h4>Suitability</h4>'
         } ]
      } );

      var sHead = new Ext.Panel( {
         cls : 'gq-head',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var suitExtra = this.suitExtraRendeder( ee );
      sHead.add( this.geeqRowRenderer( "Public suitability score", ee.geeq.publicSuitabilityScore,
         "This is the suitability score that is currently publicly displayed.", "", 2, null, suitExtra, true ) );
      if ( this.allowScoreOverride ) sHead.add( suitExtra );
      this.allowSuitInput( ee.geeq.manualSuitabilityOverride );


      panel.add( sHead );

      var sBody = new Ext.Panel( {
         cls : 'gq-body',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var detailsButtonWrap = new Ext.Panel( {
         cls : 'extjs-sucks',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var detailsButton = this.detailsButtonRenderer( sBody );

      detailsButtonWrap.add( detailsButton );
      panel.add( detailsButtonWrap );

      var sPubDesc =
         Number( ee.geeq.sScorePublication ) === -1 ? "Experiment has no publication, try filling it in." :
            "Experiment does have a publication filled in properly.";

      var sPlatfAmntDesc =
         Number( ee.geeq.sScorePlatformAmount ) === -1 ? "Experiment is on more than 2 platforms. Consider splitting the experiment." :
            Number( ee.geeq.sScorePlatformAmount ) === -0.5 ? "Experiment has 2 platforms. Consider splitting the experiment." :
               "Experiment is on a single platform.";

      var sPlatfTechDesc =
         Number( ee.geeq.sScorePlatformsTechMulti ) === -1 ? "Experiment has two or more platforms that use different technologies. Experiment should be split." : "" +
            "All used platforms use the same technology.";

      var sPlatfPopDesc =
         Number( ee.geeq.sScoreAvgPlatformPopularity ) === -1 ? "Platform(s) used (on average) by less than 10 experiments." :
            Number( ee.geeq.sScoreAvgPlatformPopularity ) === -0.5 ? "Platform(s) used (on average) by less than 20 experiments." :
               Number( ee.geeq.sScoreAvgPlatformPopularity ) === 0.0 ? "Platform(s) used (on average) by less than 50 experiments." :
                  Number( ee.geeq.sScoreAvgPlatformPopularity ) === 0.5 ? "Platform(s) used (on average) by less than 100 experiments." :
                     "Platform(s) used (on average) by at least 100 experiments.";

      var sPlatfSizeDesc =
         Number( ee.geeq.sScoreAvgPlatformSize ) === -1 ? "Platform has (or all platforms have on average) very low gene covrage." :
            Number( ee.geeq.sScoreAvgPlatformSize ) === -0.5 ? "Platform has (or all platforms have on average) low gene coverage." :
               Number( ee.geeq.sScoreAvgPlatformSize ) === 0.0 ? "Platform has (or all platforms have on average) moderate gene coverage." :
                  Number( ee.geeq.sScoreAvgPlatformSize ) === 0.5 ? "Platform has (or all platforms have on average) good gene coverage." :
                     "Platform has (or all paltforms have on average) excellent gene coverage.";

      var sSizeDesc =
         Number( ee.geeq.sScoreSampleSize ) === -1 ? "The experiment has less than 6 samples or more than 500 samples" :
            Number( ee.geeq.sScoreSampleSize ) === -0.3 ? "The experiment has less than 10 samples." :
               Number( ee.geeq.sScoreSampleSize ) === 0.0 ? "The experiment has less than 20 samples." : "The experiment has at least 20 samples.";

      var sRawDesc =
         Number( ee.geeq.sScoreRawData ) === -1 ? "Experiment has no raw data available (data are from external source). Try obtaining the raw data."
            : "We do have raw data available for this experiment.";

      var sMissErr =
         ee.geeq.noVectors === true ? "Experiment has no computed vectors, run the vector computation!" : "";
      var sMissDesc =
         ee.geeq.noVectors === true ? "There are no computed vectors." :
            Number( ee.geeq.sScoreMissingValues ) === -1 ? "Experiment has missing values. Try filling them in, ideally by obtaining raw data." :
               "There are no missing values.";

      sBody.add( this.geeqRowRenderer( 'Publication', ee.geeq.sScorePublication,
         "Checks whether the experiment has a publication.", sPubDesc ) );

      sBody.add( this.geeqRowRenderer( 'Platforms used', ee.geeq.sScorePlatformAmount,
         "The amount of platforms the experiment uses.", sPlatfAmntDesc ) );

      sBody.add( this.geeqRowRenderer( 'Platforms tech consistency', ee.geeq.sScorePlatformsTechMulti,
         "Punishes technology inconsistency of multi-platform experiments.", sPlatfTechDesc ) );

      sBody.add( this.geeqRowRenderer( 'Platforms usage', ee.geeq.sScoreAvgPlatformPopularity,
         "Depends on the popularity (experiments that use the platform) of the used platform. If there are multiple platforms," +
         "the popularity is averaged.", sPlatfPopDesc ) );

      sBody.add( this.geeqRowRenderer( 'Platforms size', ee.geeq.sScoreAvgPlatformSize,
         "Depends on the size (the number of elements) of the used platform. If there are multiple platforms, the" +
         "size is averaged.", sPlatfSizeDesc ) );

      sBody.add( this.geeqRowRenderer( 'Sample size', ee.geeq.sScoreSampleSize,
         "Depends on the experiments size (number of samples).", sSizeDesc ) );

      sBody.add( this.geeqRowRenderer( 'Raw data state', ee.geeq.sScoreRawData,
         "Checks whether there was raw data available for this experiment.", sRawDesc ) );

      sBody.add( this.geeqRowRenderer( 'Missing values', ee.geeq.sScoreMissingValues,
         "Checks whether the experiment has any missing values.", sMissDesc, 1, sMissErr ) );

      panel.add( sBody );
      if ( !sMissErr ) {
         sBody.hide();
      }
      return panel;
   },

   qualityRenderer : function( ee, mgr ) {
      var panel = new Ext.Panel( {
         defaults : {
            border : false,
            padding : 0
         },
         items : [ {
            html : '<h4>Quality</h4>'
         } ]
      } );

      var qHead = new Ext.Panel( {
         cls : 'gq-head',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var qualExtra = this.qualExtraRendeder( ee );
      qHead.add( this.geeqRowRenderer( "Public quality score", ee.geeq.publicQualityScore,
         "This is the quality score that is currently publicly displayed.", "", 2, null, qualExtra, true ) );
      if ( this.allowScoreOverride ) qHead.add( qualExtra );
      this.allowQualInput( ee.geeq.manualQualityOverride );

      panel.add( qHead );

      var qBody = new Ext.Panel( {
         cls : 'gq-body',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var detailsButtonWrap = new Ext.Panel( {
         cls : 'extjs-sucks',
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var detailsButton = this.detailsButtonRenderer( qBody );

      detailsButtonWrap.add( detailsButton );
      panel.add( detailsButtonWrap );

      var qOutlErr =
         Number( ee.geeq.corrMatIssues ) === 1 ? "The correlation matrix is empty!" :
            Number( ee.geeq.corrMatIssues ) === 2 ? "There are NaN values in the correlation matrix." :
               "";

      var qOutlierDesc =
         Number( ee.geeq.qScoreOutliers ) === -1 ? "There are detected, non-removed outliers. Removing detected outliers will improve the score." :
            "No outliers were detected.";

      var qPlatfTechMultiDesc =
         Number( ee.geeq.qScorePlatformsTech ) === -1 ? "The experiment is on a two-color platform." : "" +
            "The experiment is NOT on a two-color platform.";

      var qReplErr =
         Number( ee.geeq.replicatesIssues ) === 1 ? "There is no experimental design for this experiment" :
            Number( ee.geeq.replicatesIssues ) === 2 ? "There are no factor values" :
               Number( ee.geeq.replicatesIssues ) === 3 ? "All factor-value combinations have no replicates." :
                  Number( ee.geeq.replicatesIssues ) === 4 ? "The lowest replicate amount was 0 - this should be impossible, please report" :
                     "";

      // These thresholds are defined
      var qReplDesc =
         Number( ee.geeq.qScoreReplicates ) === -1 ? "There is a factor-value combination that has very few or no replicates." :
            Number( ee.geeq.qScoreReplicates ) === 0.0 ? "There is a factor-value combination that has moderately few replicates. " :
               "All factor-value combinations have a good number of replicates";

      var qBatchInfoDesc =
         Number( ee.geeq.qScoreBatchInfo ) === -1 ? "The experiment has no batch info. Try filling it in." : "" +
            "Batch information provided.";

      var qBatchEffErr =
         Number( ee.geeq.qScoreBatchInfo ) === -1 ? "There is no batch information" :
            Number( ee.geeq.QScoreBatchEffect ) === 0.0 && Number( ee.geeq.QScoreBatchConfound ) < 1 ? "Batch confound detected, batch effect detection skipped." :
               ee.geeq.batchCorrected === true ? "Data was batch-corrected." : "";

      var qBatchEffDesc =
         ee.geeq.manualBatchEffectActive === true ? "Manually set value, detected score was: " + ee.geeq.QScoreBatchEffect :
            Number( ee.geeq.qScoreBatchInfo ) === -1 ? "There were problems when checking for batch effect." :
               Number( ee.geeq.QScoreBatchEffect ) === -1 ? "Experiment has a batch effect; Try to batch-correct." :
                  Number( ee.geeq.QScoreBatchEffect ) === 0.0 && Number( ee.geeq.QScoreBatchConfound ) < 1 ? "Batch effect score defaults to 0 when data is confounded with batches." :
                     Number( ee.geeq.QScoreBatchEffect ) === 0.0 ? "The experiment has some evidence for a batch effect. Try to batch-correct." :
                        "Batch effect considered negligible"; // FIXME: this seems to not be working right when there is a confound; ee.geeq.qStoreBatchConfound is not defined?

      var qBatchConfErr =
         Number( ee.geeq.qScoreBatchInfo ) === -1 ? "There is no batch information" :
            "";

      var qBatchConfDesc =
         ee.geeq.manualBatchConfoundActive === true ? "Manually set value, detected score was: " + ee.geeq.QScoreBatchConfound :
            Number( ee.geeq.QScoreBatchConfound ) === -1 ? "Batch confound has been detected." :
               Number( ee.geeq.QScoreBatchConfound ) === 0.0 ? "There were problems when checking for batch confound." :
                  "The experiment does not seem to be confounded with the batches.";

      var bconfExtra = this.bconfExtraRendeder( ee );
      var beffExtra = this.beffExtraRendeder( ee );

      this.allowBconfRadios( ee.geeq.manualBatchConfoundActive );
      this.allowBeffRadios( ee.geeq.manualBatchEffectActive );

      qBody.add( this.geeqRowRenderer( '<span style="text-decoration: line-through">Mean sample corr.</span>', ee.geeq.qScoreSampleMeanCorrelation,
         "[Not included in final score] The actual mean correlation of samples.", "Not included in final score", 4, qOutlErr ) );

      qBody.add( this.geeqRowRenderer( '<span style="text-decoration: line-through">Sample corr. variance</span>', ee.geeq.qScoreSampleCorrelationVariance,
         "[Not included in final score] The actual variance of sample correlation.", "Not included in final score", 4, qOutlErr ) );

      qBody.add( this.geeqRowRenderer( 'Median sample corr.', ee.geeq.qScoreSampleMedianCorrelation,
         "The actual median correlation of samples.", "Included in the final score. Can be somewhat improved by removing outliers.", 4, qOutlErr ) );

      qBody.add( this.geeqRowRenderer( 'Outliers', ee.geeq.qScoreOutliers,
         "Depends on the presence of detected (non-removed) outliers. If there are any outliers, the score will be low.", qOutlierDesc, 1, qOutlErr ) );

      qBody.add( this.geeqRowRenderer( 'Platform technology', ee.geeq.qScorePlatformsTech,
         "Checks whether the experiments platform (any one, if there are multiple) is two-color.", qPlatfTechMultiDesc ) );

      qBody.add( this.geeqRowRenderer( 'Replicates', ee.geeq.qScoreReplicates,
         "Checks the replicate amount of all factor-value combinations, and takes the lowest one.", qReplDesc, 1, qReplErr ) );

      qBody.add( this.geeqRowRenderer( 'Batch info', ee.geeq.qScoreBatchInfo,
         "Checks whether the experiment has batch info available.", qBatchInfoDesc ) );

      qBody.add( this.geeqRowRenderer( 'Batch confound', ee.geeq.qScorePublicBatchConfound,
         "Checks whether the experimental data are confounded with batches. This value is the currently publicly displayed information.",
         qBatchConfDesc, 1, qBatchConfErr, bconfExtra ) );
      qBody.add( bconfExtra );

      qBody.add( this.geeqRowRenderer( 'Batch effect', ee.geeq.qScorePublicBatchEffect,
         "Checks the experimental data for a batch effect. This value is the currently publicly displayed information.",
         qBatchEffDesc, 1, qBatchEffErr, beffExtra ) );
      qBody.add( beffExtra );

      panel.add( qBody );
      if ( !qReplErr && !qOutlErr && !qBatchConfErr ) {
         qBody.hide();
      }
      return panel;
   },

   detailsButtonRenderer : function( panel ) {
      return new Ext.Button( {
         text : '<i class=\'fa fa-bars fa-fw\'></i> Show score breakdown and details',
         cls : 'gq-btn gq-btn-details',
         handler : function() {
            this.showPanel( panel, !panel.isVisible() )
         },
         scope : this
      } );
   },

   bconfExtraRendeder : function( ee ) {

      this.bconfFolded = !ee.geeq.manualBatchConfoundActive;

      var bconfExtra = new Ext.Panel( {
         cls : 'gq-extra' + (this.bconfFolded ? ' folded' : ''),
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var self = this;
      var foldButton = new Ext.Button( {
         text : '<i class="fa fa-chevron-down fa"></i>',
         cls : 'gq-btn',
         handler : function() {
            this.foldPanel( bconfExtra, self.bconfFolded = !self.bconfFolded );
         },
         scope : this
      } );

      bconfExtra.add( foldButton );

      bconfExtra.add( new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"></i>Re-score batch info',
         tooltip : 'Run geeq only for the batch info related scores (refreshes page).',
         handler : function( b, e ) {
            b.setText( "<i class='fa fa-refresh fa-fw fa-spin'></i>Re-score batch info" );
            b.setDisabled( true );
            ExpressionExperimentController.runGeeq( self.experimentDetails.id, "batch", {
               callback : function() {
                  window.location.reload();
               }
            } );
         },
         scope : this,
         cls : 'btn-refresh gq-subscore-refresh-btn'
      } ) );

      bconfExtra.add( new Ext.form.Checkbox( {
         xtype : 'checkbox',
         id : 'gq-bconf-override',
         boxLabel : 'Override:',
         hideLabel : false,
         checked : ee.geeq.manualBatchConfoundActive,
         handler : function( el, value ) {
            self.allowBconfRadios( value );
            ee.geeq.manualBatchConfoundActive = value;
            document.getElementById( 'bconf-notification' ).removeAttribute( "hidden" );
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : self.bconfNotifySaved
            } );
         }
      } ) );

      bconfExtra.add( new Ext.form.Radio( {
         xtype : 'radio',
         id : 'gq-bconf-override-value-true',
         name : 'gq-bconf-override-value',
         boxLabel : 'Confounded',
         hideLabel : false,
         checked : ee.geeq.manualHasBatchConfound,
         handler : function( el, value ) {
            ee.geeq.manualHasBatchConfound = value;
            document.getElementById( 'bconf-notification' ).removeAttribute( "hidden" );
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : self.bconfNotifySaved
            } );
         }
      } ) );

      bconfExtra.add( new Ext.form.Radio( {
         xtype : 'radio',
         id : 'gq-bconf-override-value-false',
         name : 'gq-bconf-override-value',
         boxLabel : 'Not confounded',
         hideLabel : false,
         checked : !ee.geeq.manualHasBatchConfound
      } ) );

      bconfExtra.add( {cls : 'gq-notif hidden', html : '<span id="bconf-notification" hidden>Saving</span>'} );

      return bconfExtra;
   },

   bconfNotifySaved : function() {
      var nr = document.getElementById( 'bconf-notification' );
      if ( nr ) {
         nr.setAttribute( "hidden", "true" );
      }
   },

   beffExtraRendeder : function( ee ) {

      this.beffFolded = !ee.geeq.manualBatchEffectActive;

      var beffExtra = new Ext.Panel( {
         cls : 'gq-extra' + (this.beffFolded ? ' folded' : ''),
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var self = this;
      var foldButton = new Ext.Button( {
         text : '<i class="fa fa-chevron-down fa"></i>',
         cls : 'gq-btn',
         handler : function() {
            this.foldPanel( beffExtra, self.beffFolded = !self.beffFolded );
         },
         scope : this
      } );

      beffExtra.add( foldButton );

      beffExtra.add( new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"></i>Re-score batch info',
         tooltip : 'Run geeq only for the batch info related scores (refreshes page).',
         handler : function( b, e ) {
            b.setText( "<i class='fa fa-refresh fa-fw fa-spin'></i>Re-score batch info" );
            b.setDisabled( true );
            ExpressionExperimentController.runGeeq( self.experimentDetails.id, "batch", {
               callback : function() {
                  window.location.reload();
               }
            } );
         },
         scope : this,
         cls : 'btn-refresh gq-subscore-refresh-btn'
      } ) );

      beffExtra.add( new Ext.form.Checkbox( {
         xtype : 'checkbox',
         id : 'gq-beff-override',
         boxLabel : 'Override:',
         hideLabel : false,
         checked : ee.geeq.manualBatchEffectActive,
         handler : function( el, value ) {
            self.allowBeffRadios( value );
            self.experimentDetails.geeq.manualBatchEffectActive = value;
            document.getElementById( 'beff-notification' ).removeAttribute( "hidden" );
            ExpressionExperimentController.setGeeqManualSettings( self.experimentDetails.id, self.experimentDetails.geeq, {
               callback : self.beffNotifySaved
            } );
         }
      } ) );

      beffExtra.add( new Ext.form.Radio( {
         xtype : 'radio',
         id : 'gq-beff-override-value-strong',
         name : 'gq-beff-override-value',
         boxLabel : 'Strong',
         hideLabel : false,
         checked : ee.geeq.manualHasStrongBatchEffect,
         handler : function( el, value ) {
            if ( !value ) return; // since we have 3 radios, we wil only process the one that was selected
            ee.geeq.manualHasStrongBatchEffect = value;
            ee.geeq.manualHasNoBatchEffect = !value;
            document.getElementById( 'beff-notification' ).removeAttribute( "hidden" );
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : self.beffNotifySaved
            } );
         }
      } ) );

      beffExtra.add( new Ext.form.Radio( {
         xtype : 'radio',
         id : 'gq-beff-override-value-weak',
         name : 'gq-beff-override-value',
         boxLabel : 'Weak',
         hideLabel : false,
         checked : !ee.geeq.manualHasStrongBatchEffect && !ee.geeq.manualHasNoBatchEffect,
         handler : function( el, value ) {
            if ( !value ) return; // since we have 3 radios, we wil only process the one that was selected
            ee.geeq.manualHasStrongBatchEffect = !value;
            ee.geeq.manualHasNoBatchEffect = !value;
            document.getElementById( 'beff-notification' ).removeAttribute( "hidden" );
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : self.beffNotifySaved
            } );
         }
      } ) );

      beffExtra.add( new Ext.form.Radio( {
         xtype : 'radio',
         id : 'gq-beff-override-value-none',
         name : 'gq-beff-override-value',
         boxLabel : 'No batch effect',
         hideLabel : false,
         checked : ee.geeq.manualHasNoBatchEffect,
         handler : function( el, value ) {
            if ( !value ) return; // since we have 3 radios, we wil only process the one that was selected
            ee.geeq.manualHasStrongBatchEffect = !value;
            ee.geeq.manualHasNoBatchEffect = value;
            document.getElementById( 'beff-notification' ).removeAttribute( "hidden" );
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : self.beffNotifySaved
            } );
         }
      } ) );

      beffExtra.add( {cls : 'gq-notif hidden', html : '<span id="beff-notification" hidden>Saving</span>'} );

      return beffExtra;
   },

   beffNotifySaved : function() {
      var nr = document.getElementById( 'beff-notification' );
      if ( nr ) {
         nr.setAttribute( "hidden", "true" );
      }
   },

   qualExtraRendeder : function( ee ) {

      this.qualFolded = !ee.geeq.manualQualityOverride;

      var qualExtra = new Ext.Panel( {
         cls : 'gq-extra' + (this.qualFolded ? ' folded' : ''),
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var self = this;
      var foldButton = new Ext.Button( {
         text : '<i class="fa fa-chevron-down fa"></i>',
         cls : 'gq-btn',
         handler : function() {
            this.foldPanel( qualExtra, self.qualFolded = !self.qualFolded );
         },
         scope : this
      } );

      qualExtra.add( foldButton );

      qualExtra.add( new Ext.Panel( {
            cls : 'gq-qual-warning',
            defaults : {
               border : false,
               padding : 0
            },
            items : [
               {
                  html :
                     "<div class='gq-errors'>" +
                     "<span class='icon'><i class='orange fa-exclamation-triangle fa'></i></span>" +
                     "<span class='msg'>" +
                     "   <p>Changing the score manually is a last resort measure, that should not be used on regular basis.</p>" +
                     "   <p>Please consult this step with your supervisor.</p>" +
                     "</span>" +
                     "</div>" +
                     "<hr class='normal'/>"
               }
            ]
         } )
      );

      qualExtra.add( new Ext.form.Checkbox( {
         xtype : 'checkbox',
         id : 'gq-qual-override',
         boxLabel : 'Override public score?',
         hideLabel : false,
         checked : ee.geeq.manualQualityOverride,
         handler : function( el, value ) {
            self.allowQualInput( value );
            ee.geeq.manualQualityOverride = value;
            if ( value ) ee.geeq.manualQualityScore = Number( document.getElementById( 'gq-qual-override-value' ).value );
         }
      } ) );

      var qval = (ee.geeq.manualQualityScore ? ee.geeq.manualQualityScore : ee.geeq.detectedQualityScore);
      qualExtra.add( {
         cls : "gq-override-value-wrap",
         html :
            "<input id='gq-qual-override-value' class='gq-override-value' type='number' step='0.1' min='-1' max='1' " +
            "   style='background-color: " + Gemma.scoreToColor( Number( qval ) ) + "'" +
            "   readonly " + (!ee.geeq.manualQualityOverride ? "disabled" : "") +
            "   value='" + qval +
            "'/> "
      } );

      qualExtra.add( new Ext.slider.SingleSlider( {
         id : 'gq-qual-override-value-slider',
         cls : 'gq-override-value-slider',
         name : 'gq-qual-override-value-slider',
         width : 200,
         value : ((ee.geeq.manualQualityScore ? ee.geeq.manualQualityScore : ee.geeq.detectedQualityScore) + 1) * 10,
         increment : 1,
         minValue : 0,
         maxValue : 20,
         hideLabel : true,
         clickToChange : true,
         listeners : {
            change : function( el, val ) {
               var nr = document.getElementById( 'gq-qual-override-value' );
               nr.value = (Math.round( val ) / 10 - 1).toFixed( 1 );
               nr.style.background = Gemma.scoreToColor( Number( nr.value ) );
               ee.geeq.manualQualityScore = nr.value;
            }
         }
      } ) );

      var saveButton = new Ext.Button( {
         text : '<i class="fa-cloud-upload fa"></i> Save changes',
         cls : 'gq-btn-save',
         handler : function( el, value ) {
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : function() {
                  window.location.reload();
               }
            } );
         },
         scope : this
      } );
      qualExtra.add( saveButton );

      return qualExtra;
   },

   suitExtraRendeder : function( ee ) {

      this.suitFolded = !ee.geeq.manualSuitabilityOverride;

      var suitExtra = new Ext.Panel( {
         cls : 'gq-extra' + (this.suitFolded ? ' folded' : ''),
         defaults : {
            border : false,
            padding : 0
         }
      } );

      var self = this;
      var foldButton = new Ext.Button( {
         text : '<i class="fa fa-chevron-down fa"></i>',
         cls : 'gq-btn',
         handler : function() {
            this.foldPanel( suitExtra, self.suitFolded = !self.suitFolded );
         },
         scope : this
      } );

      suitExtra.add( foldButton );

      suitExtra.add( new Ext.Panel( {
            cls : 'gq-suit-warning',
            defaults : {
               border : false,
               padding : 0
            },
            items : [
               {
                  html :
                     "<div class='gq-errors'>" +
                     "<span class='icon'><i class='orange fa-exclamation-triangle fa'></i></span>" +
                     "<span class='msg'>" +
                     "   <p>Changing the score manually is a last resort measure, that should not be used on regular basis.</p>" +
                     "   <p>Please consult this step with your supervisor.</p>" +
                     "</span>" +
                     "</div>" +
                     "<hr class='normal'/>"
               }
            ]
         } )
      );

      suitExtra.add( new Ext.form.Checkbox( {
         xtype : 'checkbox',
         id : 'gq-suit-override',
         boxLabel : 'Override public score?',
         hideLabel : false,
         checked : ee.geeq.manualSuitabilityOverride,
         handler : function( el, value ) {
            self.allowSuitInput( value );
            ee.geeq.manualSuitabilityOverride = value;
            if ( value ) ee.geeq.manualSuitabilityScore = Number( document.getElementById( 'gq-suit-override-value' ).value );
         }
      } ) );

      var sval = (ee.geeq.manualSuitabilityScore ? ee.geeq.manualSuitabilityScore : ee.geeq.detectedSuitabilityScore);
      suitExtra.add( {
         cls : "gq-override-value-wrap",
         html :
            "<input id='gq-suit-override-value' class='gq-override-value' type='number' step='0.1' min='-1' max='1' " +
            "   style='background-color: " + Gemma.scoreToColor( Number( sval ) ) + "'" +
            "   readonly " + (!ee.geeq.manualSuitabilityOverride ? "disabled" : "") +
            "   value='" + sval +
            "'/> "
      } );

      suitExtra.add( new Ext.slider.SingleSlider( {
         id : 'gq-suit-override-value-slider',
         cls : 'gq-override-value-slider',
         name : 'gq-suit-override-value-slider',
         width : 200,
         value : ((ee.geeq.manualSuitabilityScore ? ee.geeq.manualSuitabilityScore : ee.geeq.detectedSuitabilityScore) + 1) * 10,
         increment : 1,
         minValue : 0,
         maxValue : 20,
         hideLabel : true,
         clickToChange : true,
         listeners : {
            change : function( el, val ) {
               var nr = document.getElementById( 'gq-suit-override-value' );
               nr.value = (Math.round( val ) / 10 - 1).toFixed( 1 );
               nr.style.background = Gemma.scoreToColor( Number( nr.value ) );
               ee.geeq.manualSuitabilityScore = nr.value;
            }
         }
      } ) );

      var saveButton = new Ext.Button( {
         text : '<i class="fa-cloud-upload fa"></i> Save changes',
         cls : 'gq-btn-save',
         handler : function() {
            ExpressionExperimentController.setGeeqManualSettings( ee.id, ee.geeq, {
               callback : function() {
                  window.location.reload();
               }
            } );
         },
         scope : this
      } );
      suitExtra.add( saveButton );

      return suitExtra;
   },

   allowBeffRadios : function( allow ) {
      Ext.getCmp( 'gq-beff-override-value-strong' ).setDisabled( !allow );
      Ext.getCmp( 'gq-beff-override-value-weak' ).setDisabled( !allow );
      Ext.getCmp( 'gq-beff-override-value-none' ).setDisabled( !allow );
   },

   allowBconfRadios : function( allow ) {
      Ext.getCmp( 'gq-bconf-override-value-true' ).setDisabled( !allow );
      Ext.getCmp( 'gq-bconf-override-value-false' ).setDisabled( !allow );
   },

   allowQualInput : function( allow ) {
      Ext.getCmp( 'gq-qual-override-value-slider' ).setDisabled( !allow );
      var nr = document.getElementById( 'gq-qual-override-value' );
      if ( nr && !allow ) nr.setAttribute( "disabled", "true" );
      if ( nr && allow ) nr.removeAttribute( "disabled" );
   },

   allowSuitInput : function( allow ) {
      Ext.getCmp( 'gq-suit-override-value-slider' ).setDisabled( !allow );
      var nr = document.getElementById( 'gq-suit-override-value' );
      if ( nr && !allow ) nr.setAttribute( "disabled", "true" );
      if ( nr && allow ) nr.removeAttribute( "disabled" );
   },

   geeqRowRenderer : function( label, value, labelDesc, valueDesc, valDecimals, warning, extra, normalizeColor ) {
      if ( valDecimals === undefined ) valDecimals = 1;
      var valColor = normalizeColor ? Gemma.scoreToColorNormalized( Number( value ) ) : Gemma.scoreToColor( Number( value ) );
      var valNumber = Gemma.roundScore( value, valDecimals );
      var cls = valNumber < 0 ? "negative" : "positive";
      var html =
         '<div class="gq-row ' + (extra ? 'has-extra' : '') + ' " style="background-color: ' + valColor + '">' +
         '<i class="fa fa-info fa-fw" ' +
         '   title="' + labelDesc + '"> ' +
         '</i>' +
         '   <span class="gq-label" title="' + labelDesc + '">' + label + '</span>' +
         '   <span class="gq-value ' + cls + ' " title="' + valueDesc + '">' + valNumber + '</span>';
      if ( valueDesc ) {
         html += '<i class="fa fa-question fa-fw" title="' + valueDesc + '"></i>'
      }
      if ( warning ) {
         html += '<i class="fa fa-exclamation-triangle fa-fw" title="' + warning + '"></i>'
      }
      html += '</div>';

      return {
         html : html
      };
   },

   showPanel : function( panel, show ) {
      if ( show ) {
         panel.show();
      } else {
         panel.hide();
      }
   },

   foldPanel : function( panel, fold ) {
      if ( fold ) {
         panel.addClass( "folded" );
      } else {
         panel.removeClass( "folded" );
      }
   },

   /* batchInfoMissingRenderer: function (ee, mgr) {

        var panelBC = new Ext.Panel({
            layout: 'hbox',
            defaults: {
                border: false,
                padding: 2
            },
            items: []
        });

        var be = (ee.hasBatchInformation === false)
            ? {
                html: '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
                + Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.noBatchInfo
            }
            : {
                html: '<i class="green fa fa-check-square-o fa-lg" ></i>&nbsp;Experiment does have batch information'
            };

        panelBC.add(be);

        return panelBC;
    },*/

   batchEffectRenderer : function( ee, mgr ) {

      var panelBC = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : []
      } );

      if ( !ee.hasBatchInformation ) {
         return null;
      }

      if ( ee.batchEffect === "SINGLE_BATCH_SUCCESS" ) {
         panelBC.add( {
            html : getStatusBadge( 'cogs', 'green', 'single batch', "Samples were run in a single batch as far as we can tell" )
         } );

      } else {

         var hasBatchConfound = ee.batchConfound !== null && ee.batchConfound !== "";

         if ( hasBatchConfound ) {
            var be = {
               html : '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
                  + "Batch effect not determined due to confound."
            };
            panelBC.add( be );
         } else {
            var be = (ee.batchEffect !== null && ee.batchEffect !== "")
               ? {
                  html : '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
                     + ee.batchEffect
               }
               : {
                  html : '<i class="' + ((ee.hasBatchInformation === false) ? 'dark-gray' : 'green') +
                     ' fa fa-check-square-o fa-lg" ></i>&nbsp;' +
                     (ee.hasBatchInformation === false ? 'No batch info, can not check for batch effect' : 'Batch effect not detected')
               };

            panelBC.add( be );
         }

         var recalculateBCBtn = new Ext.Button( {
            text : '<i class="fa fa-refresh fa-fw"/>',
            tooltip : "Recalculate batch effect (refreshes page)",
            handler : function( b, e ) {
               ExpressionExperimentController.recalculateBatchEffect( ee.id, {
                  callback : function() {
                     window.location.reload();
                  }
               } );
               b.setText( '<i class="fa fa-refresh fa-fw fa-spin"/>' );
               b.setDisabled( true );
            },
            scope : this,
            cls : 'btn-refresh'
         } );

         panelBC.add( recalculateBCBtn );
      }
      return panelBC;
   },

   batchConfoundRenderer : function( ee, mgr ) {

      var panelBC = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : []
      } );

      if ( !ee.hasBatchInformation ) {
         var be = {
            html : '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;No batch information available'
         };
         panelBC.add( be );
         return panelBC;
      } else if ( ee.batchEffect === "SINGLE_BATCH_SUCCESS" ) {
         return null;
      }

      var be = (ee.batchConfound !== null && ee.batchConfound !== "")
         ? {
            html : '<i class="dark-yellow fa fa-exclamation-triangle fa-lg" ></i>&nbsp;'
               + ee.batchConfound
         }
         : {
            html : '<i class="' + ((ee.hasBatchInformation === false) ? 'dark-gray' : 'green') + ' fa fa-check-square-o fa-lg" ></i>&nbsp;Batch confound not detected'
         };

      panelBC.add( be );

      var recalculateBCBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Recalculate batch confound (refreshes page)',
         handler : function( b, e ) {
            ExpressionExperimentController.recalculateBatchConfound( ee.id, {
               callback : function() {
                  window.location.reload();
               }
            } );
            b.setText( '<i class="fa fa-refresh fa-fw fa-spin"/>' );
            b.setDisabled( true );
         },
         scope : this,
         cls : 'btn-refresh'
      } );


      return panelBC;
   },

   linkAnalysisPanelRenderer : function( ee, manager ) {
      var panel = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : [ {
            html : 'Link Analysis: '
         } ]
      } );
      var id = ee.id;
      var runBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Missing value computation (popup, refreshes page)',
         handler : manager.doLinks.createDelegate( this, [ id ] ),
         scope : this,
         cls : 'btn-refresh'
      } );
      if ( ee.dateLinkAnalysis ) {
         var type = ee.linkAnalysisEventType;
         var color = "#000";
         var suggestRun = true;
         var qtip = 'ext:qtip="Analysis was OK"';
         if ( type == 'FailedLinkAnalysisEvent' ) {
            color = 'red';
            qtip = 'ext:qtip="Analysis failed"';
         } else if ( type == 'TooSmallDatasetLinkAnalysisEvent' ) {
            color = '#CCC';
            qtip = 'ext:qtip="Dataset is too small"';
            suggestRun = false;
         }
         panel.add( {
            html : '<span style="color:' + color + ';" ' + qtip + '>'
               + Gemma.Renderers.dateRenderer( ee.dateLinkAnalysis )
         } );
         // disable through gui
//            if (suggestRun) {
//                panel.add(runBtn);
//            }
         return panel;
      } else {
         panel.add( {
            html : '<span style="color:#3A3;">May be eligible; perform via CLI</span>&nbsp;'
         } );
         // disable through gui
         // panel.add(runBtn);
         return panel;
      }

   },

   missingValueAnalysisPanelRenderer : function( ee, manager ) {
      var panel = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : [ {
            html : 'Missing values: '
         } ]
      } );
      var id = ee.id;
      var runBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Missing value computation (popup, refreshes page)',
         handler : manager.doMissingValues.createDelegate( this, [ id ] ),
         scope : this,
         cls : 'btn-refresh'
      } );
      /*
       * Offer missing value analysis if it's possible (this might need tweaking).
       */
      if ( ee.technologyType != 'ONECOLOR' && ee.technologyType != 'SEQUENCING' && ee.technologyType != 'GENELIST' && ee.hasEitherIntensity ) {

         if ( ee.dateMissingValueAnalysis ) {
            var type = ee.missingValueAnalysisEventType;
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if ( type == 'FailedMissingValueAnalysisEvent' ) {
               color = 'red';
               qtip = 'ext:qtip="Failed"';
            }

            panel.add( {
               html : '<span style="color:' + color + ';" ' + qtip + '>'
                  + Gemma.Renderers.dateRenderer( ee.dateMissingValueAnalysis ) + '&nbsp;'
            } );
            if ( suggestRun ) {
               panel.add( runBtn );
            }
            return panel;
         } else {
            panel.add( {
               html : '<span style="color:#3A3;">Needed</span>&nbsp;'
            } );
            //      panel.add(runBtn);
            return panel;
         }

      } else {

         panel
            .add( {
               html : '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>'
            } );
         return panel;
      }
   },

   processedVectorCreatePanelRenderer : function( ee, manager ) {
      var panel = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : [ {
            html : 'Preprocessing: '
         } ]
      } );
      var id = ee.id;
      var runBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Preprocess including PCA, correlation matrix and M-V (popup, refreshes page)',
         handler : manager.doProcessedVectors.createDelegate( this, [ id ] ),
         scope : this,
         cls : 'btn-refresh'
      } );
      if ( ee.dateProcessedDataVectorComputation ) {
         var type = ee.processedDataVectorComputationEventType;
         var color = "#000";

         var suggestRun = true;
         var qtip = 'ext:qtip="OK"';
         if ( type == 'FailedProcessedVectorComputationEvent' ) {
            color = 'red';
            qtip = 'ext:qtip="Failed"';
         }
         panel.add( {
            html : '<span style="color:' + color + ';" ' + qtip + '>'
               + Gemma.Renderers.dateRenderer( ee.dateProcessedDataVectorComputation ) + '&nbsp;'
         } );
         if ( suggestRun ) {
            panel.add( runBtn );
         }
         return panel;
      } else {
         panel.add( {
            html : '<span style="color:#3A3;">Needed</span>&nbsp;'
         } );
         panel.add( runBtn );
         return panel;
      }
   },

   differentialAnalysisPanelRenderer : function( ee, manager ) {
      var panel = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : [ {
            html : 'Differential Expression Analysis: '
         } ]
      } );

      if ( !ee.suitableForDEA ) {
         var color = "#000";
         panel.add( {
            html : '<span style="color:' + color + ';" ' + qtip + '>'
               + 'Not suitable' + '&nbsp;'
         } );
         return panel;
      }

      var id = ee.id;
      var runBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Differential expression analysis (popup, refreshes page)',
         handler : manager.doDifferential.createDelegate( this, [ id ] ),
         scope : this,
         cls : 'btn-refresh'
      } );


      if ( ee.numPopulatedFactors > 0 ) {
         if ( ee.dateDifferentialAnalysis ) {
            var type = ee.differentialAnalysisEventType;

            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if ( type == 'FailedDifferentialExpressionAnalysisEvent' ) {
               color = 'red';
               qtip = 'ext:qtip="Failed"';
            }
            panel.add( {
               html : '<span style="color:' + color + ';" ' + qtip + '>'
                  + Gemma.Renderers.dateRenderer( ee.dateDifferentialAnalysis ) + '&nbsp;'
            } );
            if ( suggestRun ) {
               panel.add( runBtn );
            }
            return panel;
         } else {

            panel.add( {
               html : '<span style="color:#3A3;">Needed</span>&nbsp;'
            } );
            panel.add( runBtn );
            return panel;
         }
      } else {

         panel.add( {
            html : '<span style="color:#CCF;">NA</span>'
         } );
         return panel;
      }
   },

   renderProcessedExpressionVectorCount : function( e ) {
      return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
   },

   /*
    * This really replaces the PCA panel - allows for refresh of the diagnostics (PCA, sample correlation and MV)
    */
   diagnosticsPanelRenderer : function( ee, manager ) {
      var panel = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : [ {
            html : 'Diagnostics (PCA, MV, Sample Corr, GEEQ): '
         } ]
      } );
      var id = ee.id;
      var runBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Update diagnostics (popup, refreshes page)',
         handler : manager.doDiagnostics.createDelegate( this, [ id, true ] ),
         scope : this,
         cls : 'btn-refresh'
      } );

      // Get date and info. Note that we don't have a date for the diagnostics all together, so this can be improved.
      if ( ee.datePcaAnalysis ) {
         var type = ee.pcaAnalysisEventType;

         var color = "#000";
         var qtip = 'ext:qtip="OK"';
         var suggestRun = true;

         if ( type == 'FailedPCAAnalysisEvent' ) {
            color = 'red';
            qtip = 'ext:qtip="Failed"';
         }
         panel.add( {
            html : '<span style="color:' + color + ';" ' + qtip + '>'
               + Gemma.Renderers.dateRenderer( ee.datePcaAnalysis ) + '&nbsp;'
         } );
      } else
         panel.add( {
            html : '<span style="color:#3A3;">Needed</span>&nbsp;'
         } );

      panel.add( runBtn );
      return panel;
   },

   // removed in place of general diagnostics one.
//    /*
//     * Get the last date PCA was run, add a button to run PCA
//     */
//    pcaPanelRenderer: function (ee, manager) {
//        var panel = new Ext.Panel({
//            layout: 'hbox',
//            defaults: {
//                border: false,
//                padding: 2
//            },
//            items: [{
//                html: 'Principal Component Analysis: '
//            }]
//        });
//        var id = ee.id;
//        var runBtn = new Ext.Button({
//            text: '<i class="fa fa-refresh fa-fw"/>',
//            tooltip: 'Principal component analysis (popup, refreshes page)',
//            // See EEManger.js doPca(id, hasPca)
//            handler: manager.doPca.createDelegate(this, [id, true]),
//            scope: this,
//            cls: 'btn-refresh'
//        });
//
//        // Get date and info
//        if (ee.datePcaAnalysis) {
//            var type = ee.pcaAnalysisEventType;
//
//            var color = "#000";
//            var qtip = 'ext:qtip="OK"';
//            var suggestRun = true;
//
//            if (type == 'FailedPCAAnalysisEvent') {
//                color = 'red';
//                qtip = 'ext:qtip="Failed"';
//            }
//            panel.add({
//                html: '<span style="color:' + color + ';" ' + qtip + '>'
//                + Gemma.Renderers.dateRenderer(ee.datePcaAnalysis) + '&nbsp;'
//            });
//        } else
//            panel.add({
//                html: '<span style="color:#3A3;">Needed</span>&nbsp;'
//            });
//
//        panel.add(runBtn);
//        return panel;
//
//    },

   /*
    * Get the last date batch info was downloaded, add a button to download
    */
   batchPanelRenderer : function( ee, manager ) {
      var panel = new Ext.Panel( {
         layout : 'hbox',
         defaults : {
            border : false,
            padding : 2
         },
         items : [ {
            html : 'Batch Information: '
         } ]
      } );
      var id = ee.id;
      var hasBatchInformation = ee.hasBatchInformation;
      var technologyType = ee.technologyType;
      var runBtn = new Ext.Button( {
         text : '<i class="fa fa-refresh fa-fw"/>',
         tooltip : 'Batch information (popup, refreshes page)',
         // See EEManager.js doBatchInfoFetch(id)
         handler : manager.doBatchInfoFetch.createDelegate( this, [ id ] ),
         scope : this,
         cls : 'btn-refresh'
      } );

      // Batch info fetching not allowed for RNA seq and other non-microarray data
      if ( technologyType == 'NONE' ) {
         panel.add( {
            html : '<span style="color:#CCF; "ext:qtip="Not microarray data">' + 'NA' + '</span>&nbsp;'
         } );
         return panel;
      }

      // If present, display the date and info. If batch information exists without date, display 'Provided'.
      // If no batch information, display 'Needed' with button for GEO and ArrayExpress data. Otherwise, NA.
      if ( ee.dateBatchFetch ) {
         var type = ee.batchFetchEventType;

         var color = "#000";
         var qtip = 'ext:qtip="OK"';

         if ( type == 'FailedBatchInformationFetchingEvent' ) {
            color = 'red';
            qtip = 'ext:qtip="Failed"';
         } else if ( type === 'FailedBatchInformationMissingEvent' || type === 'BatchInformationMissingEvent' ) {
            color = '#CCC';
            qtip = 'ext:qtip="Batch information was not available"';
         }

         panel.add( {
            html : '<span style="color:' + color + ';" ' + qtip + '>'
               + Gemma.Renderers.dateRenderer( ee.dateBatchFetch ) + '&nbsp;'
         } );
         panel.add( runBtn );
      } else if ( hasBatchInformation ) {
         panel.add( {
            html : '<span style="color:#000;">Provided</span>'
         } );
      } else if ( ee.externalDatabase == "GEO" || ee.externalDatabase == "ArrayExpress" ) {
         panel.add( {
            html : '<span style="color:#3A3;">Needed</span>&nbsp;'
         } );
         panel.add( runBtn );
      } else
         panel.add( {
            html : '<span style="color:#CCF; "'
               + 'ext:qtip="Add batch information by creating a \'batch\' experiment factor">' + 'NA'
               + '</span>&nbsp;'
         } );

      return panel;
   }
} );
