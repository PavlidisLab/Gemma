Gemma.GEEQ = {};

Gemma.GEEQ.scoreToColor = function( i ) {
   // normalize from [-1,1] to [0,1]
   i = i + 1;
   i = i / 2;
   // hsl red = 0° and green = 120°
   var hue = i * 120;
   return 'hsl(' + hue + ', 100%, 70%)';
}

Gemma.GEEQ.scoreToColorNormalized = function( i ) {
   // < 0.1 -> 0; > 0.6 -> 1; 0.3 -> middle;
   i = i * 1.6; // Puts 0.3 almost in the middle (close enough)

   // Cut off anything > 1
   i = i > 1 ? 1 : i;

   // Cut off negative values
   i = i < 0 ? 0 : i;

   // hsl red = 0° and green = 120°
   var hue = i * 120;
   return 'hsl(' + hue + ', 100%, 70%)';
}

Gemma.GEEQ.getStatusBadge = function( faIconClass, colorClass, title, qTip ) {
   return '<span class="ee-status-badge bg-' + colorClass + ' " ext:qtip="' + qTip + '" >' +
      '<i class=" fa fa-' + faIconClass + ' fa-lg"></i> ' + title + '</span>';
}

Gemma.GEEQ.getGeeqBadges = function( quality, suitability ) {
   var val = '';
   val = val +
      '<span class="ee-status-badge geeq-badge" style="background-color: ' + Gemma.GEEQ.scoreToColorNormalized( Number( quality ) ) + '" ' +
      'ext:qtip="Quality:&nbsp;' + Gemma.GEEQ.roundScore( quality, 1 ) + '<br/>' +
      'Quality refers to data quality, wherein the same study could have been done twice with the same technical parameters and in one case yield bad quality data, and in another high quality data." >' +
      Gemma.GEEQ.getGeeqIcon( Number( quality ) ) + "" +
      '</span>';

   var isUserAdmin = (Ext.get( 'hasAdmin' ) && Ext.get( 'hasAdmin' ).getValue() === 'true') ? true : false;
   if ( isUserAdmin ) {
      val = val + '<span class="ee-status-badge geeq-badge" style="background-color: ' + Gemma.GEEQ.scoreToColorNormalized( Number( suitability ) ) + '" ' +
         'ext:qtip="Suitability:&nbsp;' + Gemma.GEEQ.roundScore( suitability, 1 ) + '<br/>' +
         'Suitability refers to technical aspects which, if we were doing the study ourselves, we would have altered to make it optimal for analyses of the sort used in Gemma." >' +
         Gemma.GEEQ.getGeeqIcon( Number( suitability ) ) + "" +
         '</span>';
   }
   return val;
}

Gemma.GEEQ.getGeeqIcon = function( score ) {
   return "<i class='fa fa-lg " + Gemma.GEEQ.getSmileyCls( score ) + "'></i></span>";
}

Gemma.GEEQ.getGeeqIconColored = function( score ) { // PP removed  ext:qtip="Suitability:&nbsp;' + roundScore(score, 1) +
   return '' +
      '<span class="fa fa-stack">' +
      '   <i class="fa fa-lg fa-stack-1x fa-circle" style="color:' + Gemma.GEEQ.scoreToColorNormalized( Number( score ) ) + '"></i>' +
      '   <i class="fa fa-lg fa-stack-1x ' + Gemma.GEEQ.getSmileyCls( score ) + '"></i></span>'
      + '</span> '
}

/**
 * Translation of Quality or Suitability scores into emoticons. Thresholds are set here!
 * @param score
 * @returns {string}
 */
Gemma.GEEQ.getSmileyCls = function( score ) {
   return score > 0.45 ? "fa-smile-o" : score > 0.1 ? "fa-meh-o" : "fa-frown-o";
}

Gemma.GEEQ.roundScore = function( value, valDecimals ) {
   return (Math.round( Number( value ) * (Math.pow( 10, valDecimals )) ) / Math.pow( 10, valDecimals )).toFixed( valDecimals );
}

Gemma.GEEQ.getBatchInfoBadges = function( ee ) {
   var result = "";

   var hasBatchConfound = ee.batchConfound !== null && ee.batchConfound !== "";

   if ( hasBatchConfound ) {
      result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', 'batch confound',
         ee.batchConfound );
   }

   // batch status, shown whether we have batch information or not.
   if ( !hasBatchConfound && ee.batchEffect !== null ) {
      if ( ee.batchEffect === "SINGLETON_BATCHES_FAILURE" ) {
         result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', 'unable to batch', Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.noBatchesSingletons );
      } else if ( ee.batchEffect === "UNINFORMATIVE_HEADERS_FAILURE" || ee.batchEffect === "PROBLEMATIC_BATCH_INFO_FAILURE" ) {
         result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', 'no batch info', Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.noBatchesBadHeaders );
      } else if ( ee.batchEffect === "BATCH_CORRECTED_SUCCESS" ) { // ExpressionExperimentServiceImpl::getBatchEffectDescription()
         result = result + Gemma.GEEQ.getStatusBadge( 'cogs', 'green', 'batch corrected', ee.batchEffectStatistics )
      } else if ( ee.batchEffect === "NO_BATCH_EFFECT_SUCCESS" ) {
         // if there is also a batch confound, don't show this.
         if ( ee.batchConfound !== null && ee.batchConfound !== "" ) {
            // no-op.
         } else {
            result = result + Gemma.GEEQ.getStatusBadge( 'cogs', 'green', 'negligible batch effect', "Batch information is present, but the batch effect was considered below the threshold for warranting correction" )
         }
      } else if ( ee.batchEffect === "SINGLE_BATCH_SUCCESS" ) {
         result = result + Gemma.GEEQ.getStatusBadge( 'cogs', 'green', 'single batch', "Samples were run in a single batch as far as we can tell" );
      } else if ( ee.batchEffect === "NO_BATCH_INFO" ) {
         result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', 'no batch info', Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.noBatchInfo );
      } else if ( ee.batchEffect === "BATCH_EFFECT_FAILURE" ) {
         // FIXME I'm not sure we should use this. It just indicates there was a batch effect, but it wasn't corrected for some reason.
         result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', 'uncorrectable batch effect', "Batch effect may be present but could not be corrected: " + ee.batchConfound )
      } else if ( ee.batchEffect === "BATCH_EFFECT_UNDETERMINED_FAILURE" ) {
         result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', 'undetermined batch effect', 'Batch effect could not be determined.' );
      } else {
         // unsupported batch effect type
         result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'dark-yellow', ee.batchEffect, 'Some other batch effect situation' )
      }
   }

   if ( !ee.suitableForDEA ) {
      result = result + Gemma.GEEQ.getStatusBadge( 'exclamation-triangle', 'orange', 'unsuitable for diff. ex.',
         Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.statusUnsuitableForDEA );
   }

   return result;
}