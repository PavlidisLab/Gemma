Ext.namespace('Gemma');

Gemma.MIN_STRINGENCY = 2;

// max suggested number of elements to use for a diff ex viz query
Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY = 100;
Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY = 100;
Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY = 20; // this is a hard limit
Gemma.MAX_EXPERIMENTS_CO_DIFF_EX_VIZ_QUERY = 100000; // effectively no limit

/**
 * These methods are used to run a search from the AnalysisResultsSearchForm
 * 
 * It's an Ext.util.Observable so that it can fire events
 * (needed to keep form and UI in sync with steps of searching)
 *  
 * @author thea
 * @version $Id$
 */
Gemma.AnalysisResultsSearchMethods = Ext.extend(Ext.util.Observable, {
	taxonId: null,
	// defaults for coexpression
	DEFAULT_STRINGENCY : 2,
	DEFAULT_forceProbeLevelSearch : false,
	DEFAULT_useMyDatasets : false,
	DEFAULT_queryGenesOnly : false,
	// defaults for differential expression
	// using Gemma.DEFAULT_THRESHOLD, Gemma.MIN_THRESHOLD, Gemma.MAX_THRESHOLD (defined elsewhere)

	geneIds : [],
	geneGroupId : null, // keep track of what gene group has been selected
	experimentIds : [],

	hidingExamples: false,

    SearchType : {'COEXPRESSION':1, "DIFFERENTIAL_EXPRESSION":2},

    /**
    *
    * @param {GeneSetValueObject[]} geneSetValueObjects
    * @param {ExperimentSetValueObject[]} experimentSetValueObjects
    */
	searchCoexpression: function( geneSetValueObjects, experimentSetValueObjects ){
        // check
        if (!this.checkSetsNonEmpty ( geneSetValueObjects, experimentSetValueObjects )) {
            return;
        }

        // register sets with session on the server
        var me = this;
        this.registerSetsIfNeeded( geneSetValueObjects, experimentSetValueObjects ).then (
            function done (sets) {
                me.fireEvent('beforesearch');
                var searchCommand = me.getCoexpressionSearchCommand( sets.geneSets, sets.experimentSets );
                var errorMessage = me.validateCoexSearch( searchCommand );
                if (errorMessage === "") {
                    //me.lastCSC = searchCommand;
                    me.fireEvent('coexpression_search_query_ready', searchCommand);
                } else {
                    me.fireEvent('error', errorMessage);
                }
            },
            function error (reason) {
                me.fireEvent('error', "Error dealing with gene/experiment groups.");
            }
        );
    },

	/**
     *
	 * @param {GeneSetValueObject[]} geneSetValueObjects
	 * @param {ExperimentSetValueObject[]} experimentSetValueObjects
	 */
	searchDifferentialExpression: function( geneSetValueObjects, experimentSetValueObjects ){
        // check inputs
        if (!this.checkSetsNonEmpty ( geneSetValueObjects, experimentSetValueObjects )) {
            return;
        }

        /**
         * Verify that experiments and genes are not empty.
         * If there are too many experiments or genes, warn the user and offer to trim the sets.
         */

        // trim (optional)
        // Check if counts exceed maximums
        var geneCount = Gemma.AnalysesSearchUtils.getGeneCount( geneSetValueObjects );
        var experimentCount = Gemma.AnalysesSearchUtils.getExperimentCount( experimentSetValueObjects );

        if ( geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY ||
             experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY ) {

            Gemma.AnalysesSearchUtils.showTrimInputDialogWindow( geneCount, geneSetValueObjects,
                                experimentCount, experimentSetValueObjects,
                                this );
        }
        // skip trimming

        // register sets with session on the server (moved out)

        var scope = this;
        this.registerSetsIfNeeded(geneSetValueObjects, experimentSetValueObjects).then (
            function done (sets) {
                scope.fireEvent('beforesearch');
                var data = scope.getDataForDiffVisualization( sets.geneSets, sets.experimentSets );
                scope.fireEvent('differential_expression_search_query_ready', null, data);
            },
            function error (reason) {

            }
        );

        // pick search parameters (ok)

        // search
    },

    /**
     * @private
     *
     * fires searchAborted event
     *
     * @param geneSetValueObjects
     * @param experimentSetValueObjects
     */
    checkSetsNonEmpty : function ( geneSetValueObjects, experimentSetValueObjects ) {
        // Verify we have no empty sets.
        if ( Gemma.AnalysesSearchUtils.isGeneSetsNonEmpty( geneSetValueObjects ) ) {
            Ext.Msg.alert("Error", "Gene(s) must be selected before continuing.");
            this.fireEvent('searchAborted');
            return false;
        }
        if ( Gemma.AnalysesSearchUtils.isExperimentSetsNonEmpty( experimentSetValueObjects ) ) {
            Ext.Msg.alert("Error", "Experiment(s) must be selected before continuing.");
            this.fireEvent('searchAborted');
            return false;
        }
        return true;
    },

    /**
     *
     * @return {Promise}
     */
    registerSetsIfNeeded : function (geneSets, experimentSets) {
        var registerGeneSetsPromise = Gemma.SessionSetsUtils.registerGeneSetsIfNotRegistered( geneSets );
        var registerExperimentSetsPromise = Gemma.SessionSetsUtils.registerExperimentSetsIfNotRegistered( experimentSets );

        // Promises are cool. Read up on them if you don't know what they are.
        var promise = RSVP.all([
                        registerGeneSetsPromise,
                        registerExperimentSetsPromise
                      ])
            .then( function wrapResults( results ) {
                // results are in the same order as promises were listed
                return {'geneSets' : results[0], 'experimentSets' : results[1]};
            });
        return promise;
    },


    /**
     * @private
     *
     * Run the search with entity set value objects.
     * If any of the parameter sets have a null, undefined, or negative id
     * then they are not "saved" anywhere (in the db or in the session-bound set list).
     * Before the search runs, these sets are "saved" to the session-bound list.
     *
     * @param {GeneSetValueObject[]} geneSets
     * @param {ExperimentSetValueObject[]} experimentSets
     */
    doSearch : function (geneSets, experimentSets) {
        //TODO: finish migration, called from other places
    },

	/**
     * @private
	 * Construct the coexpression command object from the form, to be sent to
	 * the server.
	 * 
	 * @return {Object} CoexpressionSearchCommand
	 */
	getCoexpressionSearchCommand : function( geneSetValueObjects, experimentSetValueObjects ) {
        var geneIds = Gemma.AnalysesSearchUtils.getGeneIds(geneSetValueObjects);
        var eeIds = Gemma.AnalysesSearchUtils.getExperimentIds(experimentSetValueObjects);
        var stringency = this.decideCoexpressionSearchStringency( eeIds.length );

        var coexpressionSearchCommand = {
            geneIds : geneIds,
            eeIds : eeIds,
            stringency : stringency,
            forceProbeLevelSearch : this.DEFAULT_forceProbeLevelSearch,
            useMyDatasets : this.DEFAULT_useMyDatasets,
            queryGenesOnly : this.DEFAULT_queryGenesOnly,
            taxonId : this.taxonId,
            eeSetName : null,
            eeSetId : null
        };
        return coexpressionSearchCommand;
	},

	/**
     * TODO: move this out of here
     *
	 * @return searchStringency
	 */
	getLastCoexpressionSearchCommand : function() {
		return this.lastCSC;
	},

    /**
     * We pick appropriate search stringency based on number of experiments
     * (low stringency results aren't meaningful in large experiment sets).
     *
     * @private
     * @param numDatasets
     */
	decideCoexpressionSearchStringency : function( numDatasets ) {
		var k = 50;

		var searchStringency = 2;
		
		if (numDatasets > k) {
			searchStringency = 2 + Math.round(numDatasets / k);
		}
		
		if (searchStringency > 20) {
			searchStringency = 20;
		}

        searchStringency = Math.round( (3/4) * searchStringency );

        return searchStringency;
	},

	/**
	 * @private
     * Do some more checks before running the coexpression search
	 * @private
	 * @param {Object} coexSearchCommand
	 */
	validateCoexSearch : function (coexSearchCommand) {
		if (coexSearchCommand.queryGenesOnly && coexSearchCommand.geneIds.length < 2) {
			return "You must select more than one query gene to use 'search among query genes only'";
		} else if (!coexSearchCommand.geneIds || coexSearchCommand.geneIds.length === 0) {
			return "We couldn't figure out which gene you want to query. Please use the search functionality to find genes.";
		} else if (coexSearchCommand.stringency < Gemma.MIN_STRINGENCY) {
			return "Minimum stringency is " + Gemma.MIN_STRINGENCY;
		} else if (coexSearchCommand.eeIds && coexSearchCommand.eeIds.length < 1) {
			return "There are no datasets that match your search terms";
		} else if (!coexSearchCommand.eeIds && !coexSearchCommand.eeSetId) {
			return "Please select an analysis. Taxon, gene(s), and scope must be specified.";
		} else if (coexSearchCommand.geneIds.length > Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY ) {
			// if trying to search for more than the allowed limit of genes -- show warning
			// and trim the gene Ids
			coexSearchCommand.geneIds = coexSearchCommand.geneIds.slice(0, Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY );
			this.fireEvent('warning',"Coexpression searches are limited to " + Gemma.MAX_GENES_PER_CO_EX_VIZ_QUERY +
					" query genes. Your query has been trimmed.<br>");
			return "";
		} else {
			return "";
		}
	},

    /**
     * @private
     * @param geneSetValueObjects
     * @param experimentSetValueObjects
     * @return {{experimentSetValueObjects: *, geneSetValueObjects: *, geneNames: Array, datasetNames: Array, taxonId: *, taxonName: *, pvalue: number, datasetCount: number}}
     */
    getDataForDiffVisualization : function(geneSetValueObjects, experimentSetValueObjects) {
        var geneNames = [];
        var i;
        if (geneSetValueObjects.length > 0) {
            for (i = 0; i < geneSetValueObjects.length; i++) {
                geneNames.push(geneSetValueObjects[i].name);
            }
        }
        var experimentNames = [];
        var experimentCount = 0;
        if (experimentSetValueObjects.length > 0) {
            for (i = 0; i < experimentSetValueObjects.length; i++) {
                experimentNames.push(experimentSetValueObjects[i].name);
                experimentCount += experimentSetValueObjects[i].expressionExperimentIds.size();
            }
        }
        var data = {
            experimentSetValueObjects: experimentSetValueObjects,
            geneSetValueObjects: geneSetValueObjects,
            geneNames : geneNames,
            datasetNames : experimentNames,
            taxonId : this.taxonId,
            taxonName : this.taxonName,
            pvalue : 0.01,
            datasetCount : experimentCount
        };
        return data;
    },

    /**
     * @private
     */
	initComponent: function(){
		Gemma.AnalysisResultsSearchMethods.superclass.initComponent.call(this);
	},

    /**
     * @private
     * @param configs
     */
	constructor: function(configs){
		if(typeof configs !== 'undefined'){
			Ext.apply(this, configs);
		}
		Gemma.AnalysisResultsSearchMethods.superclass.constructor.call(this);
	}
});