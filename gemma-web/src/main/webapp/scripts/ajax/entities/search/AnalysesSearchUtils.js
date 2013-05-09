Ext.namespace('Gemma');


// Add helper methods to gene and experiment sets.

//GeneSetValueObject.prototype.getGeneIds = function() {
//    var geneIds = [];
//    geneIds = geneIds.concat(this.geneIds);
//    return geneIds;
//};
//
//    ExpressionExperimentSetValueObject
//
Gemma.AnalysesSearchUtils = {

    /**
     * @return {Array}
     */
    getGeneIds : function(geneSetValueObjects) {
        var geneIds = [];
        var i;
        for (i = 0; i < geneSetValueObjects.length; i++) {
            var vo = geneSetValueObjects[i];
            if (vo instanceof GeneValueObject) {
                geneIds.push(vo.id);
            } else if (vo instanceof GeneSetValueObject) {
                geneIds = geneIds.concat(vo.geneIds);
            }
        }
        return geneIds;
    },

    /**
     * @return {Array}
     */
    getExperimentIds : function(experimentSetValueObjects) {
        var eeIds = [];
        var i;
        for (i = 0; i < experimentSetValueObjects.length; i++) {
            var vo = experimentSetValueObjects[i];
            if ( vo instanceof ExpressionExperimentValueObject) {
                eeIds.push(vo.id);
            } else if (vo instanceof ExpressionExperimentSetValueObject) {
                eeIds = eeIds.concat(vo.expressionExperimentIds);
            }
        }
        return eeIds;
    },

    /**
     * fires 'searchAborted' event
     * @private
     * @param {GeneSetValueObject[]} geneSetValueObjects
     * @return {boolean}
     */
    isGeneSetsNonEmpty: function (geneSetValueObjects) {
        // verify that the array is not empty AND doesn't have empty sets.
        var isEmpty = false;
        if (geneSetValueObjects.length === 0) {
            return true;
        }
        if (geneSetValueObjects[0].geneIds && geneSetValueObjects[0].geneIds.length === 0) {
            isEmpty = true;
        }

        // TODO: ensure that other sets are non-empty
        return isEmpty;
    },

    /**
     * fires 'searchAborted' event
     * @private
     * @param {ExperimentSetValueObject[]} experimentSetValueObjects
     * @return {boolean}
     */
    isExperimentSetsNonEmpty: function (experimentSetValueObjects) {
        if (experimentSetValueObjects.length === 0) {
            return true;
        }
        return false;
    },

    /**
     * @param {GeneSetValueObject[]} geneSetValueObjects
     */
    getGeneCount : function (geneSetValueObjects) {
        var geneCount = 0, i, vo;
        for (i = 0; i < geneSetValueObjects.length; i++) {
            vo = geneSetValueObjects[i];
            if (vo.geneIds) {
                geneCount += vo.geneIds.length;
            }
        }
        return geneCount;
    },

    getExperimentCount : function (experimentSetValueObjects) {
        var experimentCount = 0, i, vo;
        for (i = 0; i < experimentSetValueObjects.length; i++) {
            vo = experimentSetValueObjects[i];
            if(vo.expressionExperimentIds) {
                experimentCount += vo.expressionExperimentIds.length;
            }
        }
        return experimentCount;
    },

    /**
     * @param {GeneSetValueObject[]} valueObjects
     * @param {Number} max
     * @returns {Array} a subset of the param list of valueObjects, with one set potentially trimmed
     */
    trimGeneValueObjects: function (valueObjects, max) {
        var runningCount = 0;
        var i; var valObj;
        var trimmedValueObjects = [];
        for (i = 0; i < valueObjects.length; i++) {
            valObj = valueObjects[i];
            if (valObj.geneIds && (runningCount + valObj.geneIds.length) < max) {
                runningCount += valObj.geneIds.length;
                trimmedValueObjects.push(valObj);
            } else if (valObj.geneIds) {
                var trimmedIds = valObj.geneIds.slice(0, (max - runningCount));
                // clone the object so you don't effect the original
                var trimmedValObj = Object.clone(valObj);
                trimmedValObj.geneIds = trimmedIds;
                trimmedValObj.id = null;
                trimmedValObj.name = "Trimmed " + valObj.name;
                trimmedValObj.description = "Trimmed " + valObj.name+" for search";
                trimmedValObj.modified = true;
                trimmedValueObjects.push(trimmedValObj);
                return trimmedValueObjects;
            }
        }
        return trimmedValueObjects;
    },

    /**
     * @param {Object} valueObjects
     * @param {Number} max
     * @return {Array} a subset of the param list of valueObjects, with one set potentially trimmed
     */
    trimExperimentValObjs: function(valueObjects, max){
        var runningCount = 0;
        var i; var valObj;
        var trimmedValueObjects = [];
        for (i = 0; i < valueObjects.length; i++){
            valObj = valueObjects[i];
            if (valObj.expressionExperimentIds && (runningCount + valObj.expressionExperimentIds.length) < max) {
                runningCount += valObj.expressionExperimentIds.length;
                trimmedValueObjects.push( valObj );
            } else if (valObj.expressionExperimentIds) {
                var trimmedIds = valObj.expressionExperimentIds.slice(0, (max - runningCount));
                // clone the object so you don't affect the original
                var trimmedValObj = Object.clone(valObj);
                trimmedValObj.expressionExperimentIds = trimmedIds;
                trimmedValObj.id = null;
                trimmedValObj.name = "Trimmed " + valObj.name;
                trimmedValObj.description = "Trimmed " + valObj.name+" for search";
                trimmedValObj.modified = true;
                trimmedValueObjects.push(trimmedValObj);
                return trimmedValueObjects;
            }
        }
        return trimmedValueObjects;
    },

    constructMessages: function (maxNumGenes, geneCount, maxNumExperiments, experimentCount) {
        var stateText = "";
        var maxText = "";

        if (geneCount > maxNumGenes && experimentCount > maxNumExperiments) {
            stateText = geneCount + " genes and " + experimentCount + " experiments";
            maxText = maxNumGenes + " genes and " + maxNumExperiments + " experiments";
        } else if (experimentCount > maxNumExperiments) {
            stateText = experimentCount + " experiments";
            maxText = maxNumExperiments + " experiments";
        } else if (geneCount > maxNumGenes) {
            stateText = geneCount + " genes";
            maxText = maxNumGenes + " genes";
        }
        return {stateText: stateText, maxText: maxText};
    },

    showTrimInputDialogWindow: function (maxNumGenes, geneCount, geneSetValueObjects, maxNumExperiments, experimentCount, experimentSetValueObjects, handlerScope) {
        var handlers = {
            trim : function () {
                if (geneCount > Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY) {
                    geneSetValueObjects = Gemma.AnalysesSearchUtils.trimGeneValueObjects( geneSetValueObjects, Gemma.MAX_GENES_PER_DIFF_EX_VIZ_QUERY );
                }
                if (experimentCount > Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY) {
                    experimentSetValueObjects = Gemma.AnalysesSearchUtils.trimExperimentValObjs( experimentSetValueObjects, Gemma.MAX_EXPERIMENTS_PER_DIFF_EX_VIZ_QUERY );
                }

                this.startDifferentialExpressionSearch( geneSetValueObjects, experimentSetValueObjects );
                trimWindow.close();
            },
            notrim : function () {
                this.startDifferentialExpressionSearch( geneSetValueObjects, experimentSetValueObjects );
                trimWindow.close();
            },
            cancel : function () {
                this.fireEvent('searchAborted'); // clears loading mask
                trimWindow.close();
            },
            scope : handlerScope
        };

        // Construct text
        var messages = Gemma.AnalysesSearchUtils.constructMessages(maxNumGenes, geneCount, maxNumExperiments, experimentCount);
        var stateText = messages.stateText;
        var maxText = messages.maxText;

        Ext.getBody().mask();
        var trimWindow = new Ext.Window({
            width: 450,
            height: 200,
            closable: false,
            bodyStyle: 'padding:7px;background: white; font-size:1.1em',
            title: Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.trimmingWarningTitle,
            html: String.format(Gemma.HelpText.WidgetDefaults.AnalysisResultsSearchForm.trimmingWarningText, stateText, maxText),
            buttons: [
                {
                    text: 'Trim',
                    tooltip: 'Your query will be trimmed to ' + maxText,
                    handler: handlers.trim,
                    scope: handlers.scope
                }, {
                    text: 'Don\'t trim',
                    tooltip: 'Continue with your search as is',
                    handler: handlers.notrim,
                    scope: handlers.scope
                }, {
                    text: 'Cancel',
                    handler: handlers.cancel,
                    scope: handlers.scope
                }
            ]
        });
        trimWindow.show();
        trimWindow.on('close', function () {
            Ext.getBody().unmask();
        });

        return trimWindow;
    },

    /**
     * @param result
     * @param lastSearchCommand
     * @param displayedResults
     * @return {Gemma.ObservableCoexpressionSearchResults}
     */
    constructCoexpressionSearchData: function (result, lastSearchCommand, displayedResults) {
        var coexpressionSearchData = new Gemma.ObservableCoexpressionSearchResults({
            coexGridCoexCommand: lastSearchCommand,
            cytoscapeCoexCommand: Gemma.CytoscapePanelUtil.getCoexVizCommandFromCoexGridCommand(lastSearchCommand),
            coexGridResults: result
        });

        // Sometimes initial display stringency is set higher than a stringency we have results for, check this
        var highestResultStringency = Gemma.CoexValueObjectUtil.getHighestResultStringencyUpToInitialDisplayStringency(displayedResults,
            coexpressionSearchData.coexGridCoexCommand.displayStringency);

        if (coexpressionSearchData.coexGridCoexCommand.displayStringency > highestResultStringency) {
            coexpressionSearchData.coexGridCoexCommand.displayStringency = highestResultStringency;
            coexpressionSearchData.cytoscapeCoexCommand.displayStringency = highestResultStringency;
            coexpressionSearchData.cytoscapeCoexCommand.stringency = Gemma.CytoscapePanelUtil.restrictResultsStringency(highestResultStringency);
        }
        return coexpressionSearchData;
    }
};
