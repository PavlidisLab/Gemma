Ext.namespace( 'Gemma' );

/**
 * Add helper methods to gene and experiment sets.
 * 
 */
Gemma.AnalysesSearchUtils = {

   /**
    * @static
    * @return {Array}
    * @memberOf Gemma.AnalysesSearchUtils
    */
   getGeneIds : function( geneSetValueObject ) {
      var geneIds = [];
      var vo = geneSetValueObject;
      if ( vo instanceof GeneValueObject ) {
         console.log( "got a sole gene" );
         geneIds.push( vo.id );
      } else if ( vo instanceof GeneSetValueObject ) {
         geneIds = geneIds.concat( vo.geneIds );
      }

      return geneIds;
   },

   /**
    * @static
    * @return {Array}
    */
   getExperimentIds : function( experimentSetValueObject ) {
      if ( !experimentSetValueObject ) {
         return [];
      }
      var eeIds = [];
      var vo = experimentSetValueObject;
      if ( vo instanceof ExpressionExperimentValueObject ) {
         console.log( "Got a sole experiment" );
         eeIds.push( vo.id );
      } else if ( vo instanceof ExpressionExperimentSetValueObject ) {
         eeIds = eeIds.concat( vo.expressionExperimentIds );
      }

      return eeIds;
   },

   /**
    * @static
    * @param {GeneSetValueObject[]}
    *           geneSetValueObjects
    * @return {boolean}
    */
   isGeneSetEmpty : function( geneSetValueObject ) {

      if ( geneSetValueObject == null ) {
         return true;
      }

      return geneSetValueObject.geneIds.length == 0 && geneSetValueObject.size === 0;

   },

   /**
    * @static
    * @param {ExperimentSetValueObject[]}
    *           experimentSetValueObjects
    * @return {boolean}
    */
   isExperimentSetEmpty : function( experimentSetValueObject ) {
      if ( experimentSetValueObject == null ) {
         return true;
      }

      return experimentSetValueObject.expressionExperimentIds.length == 0 && experimentSetValueObject.size == 0;
   },

   /**
    * @static
    * @param {GeneSetValueObject[]}
    *           geneSetValueObjects
    */
   getGeneCount : function( geneSetValueObject ) {
      return geneSetValueObject.size;
   },

   /**
    * @static
    * @param experimentSetValueObject
    * @returns
    */
   getExperimentCount : function( experimentSetValueObject ) {
      if ( !experimentSetValueObject ) {
         return -1;
      }
      return experimentSetValueObject.size;
   },

   /**
    * @static
    * @param {GeneSetValueObject ]}
    *           valueObject
    * @param {Number}
    *           max
    * @returns {Object} a subset
    */
   trimGeneValueObject : function( valObj, max ) {
      var trimmedIds = valObj.geneIds.slice( 0, max );
      // clone the object so you don't effect the original
      var trimmedValObj = jQuery.extend( {}, valObj );
      trimmedValObj.geneIds = trimmedIds;
      trimmedValObj.id = null;
      trimmedValObj.name = "Trimmed " + valObj.name;
      trimmedValObj.description = "Trimmed " + valObj.name + " for search";
      trimmedValObj.modified = true;
      return trimmedValueObj;

   },

   /**
    * @static
    * @param {ExpressionExperimentValueObject}
    *           valueObject
    * @param {Number}
    *           max
    * @return {Object} a subset
    */
   trimExperimentValObj : function( valObj, max ) {
      var trimmedIds = valObj.expressionExperimentIds.slice( 0, max );
      // clone the object so you don't affect the original
      var trimmedValObj = jQuery.extend( {}, valObj );
      trimmedValObj.expressionExperimentIds = trimmedIds;
      trimmedValObj.id = null;
      trimmedValObj.name = "Trimmed " + valObj.name;
      trimmedValObj.description = "Trimmed " + valObj.name + " for search";
      trimmedValObj.modified = true;
      return trimmedValObj;

   },

   /**
    * @static
    * @param maxNumGenes
    * @param geneCount
    * @param maxNumExperiments
    * @param experimentCount
    * @returns {___anonymous5593_5663}
    */
   constructMessages : function( maxNumGenes, geneCount, maxNumExperiments, experimentCount ) {
      var stateText = "";
      var maxText = "";

      if ( geneCount > maxNumGenes && experimentCount > maxNumExperiments ) {
         stateText = geneCount + " genes and " + experimentCount + " experiments";
         maxText = maxNumGenes + " genes and " + maxNumExperiments + " experiments";
      } else if ( experimentCount > maxNumExperiments ) {
         stateText = experimentCount + " experiments";
         maxText = maxNumExperiments + " experiments";
      } else if ( geneCount > maxNumGenes ) {
         stateText = geneCount + " genes";
         maxText = maxNumGenes + " genes";
      }
      return {
         stateText : stateText,
         maxText : maxText
      };
   },

   /**
    * @static
    * @param maxNumGenes
    * @param geneCount
    * @param geneSetValueObject
    * @param maxNumExperiments
    * @param experimentCount
    * @param experimentSetValueObject
    * @param handlerScope
    * @returns {Ext.Window}
    * @memberOf Gemma.AnalysesSearchUtils
    */
   showTrimInputDialogWindow : function( maxNumGenes, geneCount, geneSetValueObject, maxNumExperiments,
      experimentCount, experimentSetValueObject, handlerScope ) {
      var handlers = {
         trim : function() {
            if ( geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY ) {
               geneSetValueObject = Gemma.AnalysesSearchUtils.trimGeneValueObject( geneSetValueObject,
                  Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY );
            }
            if ( experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY ) {
               experimentSetValueObject = Gemma.AnalysesSearchUtils.trimExperimentValObj( experimentSetValueObject,
                  Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY );
            }

            this.startDifferentialExpressionSearch( geneSetValueObject, experimentSetValueObject );
            trimWindow.close();
         },
         notrim : function() {
            this.startDifferentialExpressionSearch( geneSetValueObject, experimentSetValueObject );
            trimWindow.close();
         },
         cancel : function() {
            this.fireEvent( 'searchAborted' ); // clears loading mask
            trimWindow.close();
         },
         scope : handlerScope
      };

      // Construct text
      var messages = Gemma.AnalysesSearchUtils.constructMessages( maxNumGenes, geneCount, maxNumExperiments,
         experimentCount );
      var stateText = messages.stateText;
      var maxText = messages.maxText;

      Ext.getBody().mask();
      var trimWindow = new Ext.Window( {
         width : 450,
         height : 200,
         closable : false,
         bodyStyle : 'padding:7px;background: white; font-size:1.1em',
         title : Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.trimmingWarningTitle,
         html : String.format( Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.trimmingWarningText, stateText,
            maxText ),
         buttons : [ {
            text : 'Trim',
            tooltip : 'Your query will be trimmed to ' + maxText,
            handler : handlers.trim,
            scope : handlers.scope
         }, {
            text : 'Don\'t trim',
            tooltip : 'Continue with your search as is',
            handler : handlers.notrim,
            scope : handlers.scope
         }, {
            text : 'Cancel',
            handler : handlers.cancel,
            scope : handlers.scope
         } ]
      } );
      trimWindow.show();
      trimWindow.on( 'close', function() {
         Ext.getBody().unmask();
      } );

      return trimWindow;
   }

};
