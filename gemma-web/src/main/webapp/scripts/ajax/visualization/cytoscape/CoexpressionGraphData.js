/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace('Gemma');

/**
 *
 * @param {CoexpressionMetaValueObject} searchResultsCytoscape
 * @param cytoscapeCoexCommand
 * @param queryGeneIds
 * @constructor
 */
Gemma.CoexpressionGraphData = function (searchResultsCytoscape, cytoscapeCoexCommand, queryGeneIds) {
    var maxStringency = cytoscapeCoexCommand.eeIds.length;

    function getTrimmedGraphData (graphSizeLimit, minStringency) {
        var coexpressionPairs = searchResultsCytoscape.knownGeneResults;

        return Gemma.CoexValueObjectUtil.trimGraphUpToRequestedSize(
            coexpressionPairs,
            queryGeneIds,
            minStringency,
            maxStringency,
            graphSizeLimit);
    }

    var maxEdgesLimit = 2000; //searchResultsCytoscape.maxEdges;

    var fullGraph = {
        geneResults: searchResultsCytoscape.knownGeneResults,
        trimStringency:  searchResultsCytoscape.nonQueryGeneTrimmedValue,
        size: searchResultsCytoscape.knownGeneResults.length
    };

    this.graphSizeOptions = [];

    this.getGraphData = function() {
        return this.graphSizeOptions;
    };

    this.getSmallestGraph = function() {
        return this.graphSizeOptions[this.graphSizeOptions.length-1].graph;
    };

    var startStringency;
    if (searchResultsCytoscape.nonQueryGeneTrimmedValue > 0) {
        this.isTrimmedOnBackend = true;
        startStringency = searchResultsCytoscape.nonQueryGeneTrimmedValue + 1;
    } else {
        this.isTrimmedOnBackend = false;
        startStringency = cytoscapeCoexCommand.stringency;
    }

    /*
     Extra trimming will be done on the front end to the
     user's chosen graph size(the default will be significantly smaller than the
     highest setting because older computers will have trouble with the highest
     setting).  This way if the user wants more edges, the data has already been
     sent to the front end and won't need a new call to the back end.
     */
    var targetNumberOfEdges = 0.5 * maxEdgesLimit;

    if ( fullGraph.size <= targetNumberOfEdges ) {
        this.graphSizeOptions.push( { label:'No edge number limit',
                                 graph: fullGraph } );
        return; // Ideal case, we are done.
    }

    // Trim graph to be at or below target number of edges.
    // This will be default display value,
    // but we allow user to go higher than that.
    var graph;

    if ( fullGraph.size <= maxEdgesLimit ) {
        graph = fullGraph;
        if (this.isTrimmedOnBackend) {
            this.graphSizeOptions.push({
                label: fullGraph.trimStringency + ' (' + fullGraph.size + " edges)",
                graph: fullGraph });
        } else {
            this.graphSizeOptions.push({
                label: 'No Trimming',
                graph: fullGraph});
        }
    } else {
        graph = getTrimmedGraphData( maxEdgesLimit, startStringency );
        this.graphSizeOptions.push({
            label: graph.trimStringency + ' (' + graph.size + " edges)",
            graph: graph});
    }

    // Keep going until maxStringency limit is reached or graph is small enough.
    var ratio = 0.75;
    while (graph.trimStringency < maxStringency && graph.size > targetNumberOfEdges && ratio >= 0.5) {
        if (graph.size > ratio * maxEdgesLimit) {
            graph = getTrimmedGraphData( ratio * maxEdgesLimit, startStringency );
            this.graphSizeOptions.push({
                label: graph.trimStringency + ' (' + graph.size + " edges)",
                graph: graph});
        }
        ratio = ratio - 0.25;
    }
};