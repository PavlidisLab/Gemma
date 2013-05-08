Ext.namespace("Gemma");

Gemma.CoexValueObjectUtil = {

    // takes an array of CoexpressionValueObjectExts and trims away
    // results based on stringency and currentQueryGeneIds
    trimKnownGeneResultsWithQueryGenes: function (coexPairs, currentQueryGeneIds, filterStringency, keepQueryGeneEdges) {
        // helper array to prevent duplicate nodes from being entered
        var graphNodeIds = [];
        var _edgeSet = [];
        var trimmedCoexPairs = [];
        var i;

        function addGeneIfAbsent(gene) {
            if (graphNodeIds.indexOf(gene.id) === -1) {
                graphNodeIds.push(gene.id);
            }
        }

        function bothGenesPresent(coexPair) {
            return (graphNodeIds.indexOf(coexPair.foundGene.id) !== -1 &&
                graphNodeIds.indexOf(coexPair.queryGene.id) !== -1);
        }

        function atleastOneGeneIsQueryGene(coexPair) {
            return (currentQueryGeneIds.indexOf(coexPair.foundGene.id) !== -1 ||
                currentQueryGeneIds.indexOf(coexPair.queryGene.id) !== -1);
        }

        function bothAreNotQueryGenes(coexPair) {
            return (currentQueryGeneIds.indexOf(coexPair.foundGene.id) === -1 &&
                currentQueryGeneIds.indexOf(coexPair.queryGene.id) === -1);
        }

        function bothAreQueryGenes(coexPair) {
            return (currentQueryGeneIds.indexOf(coexPair.foundGene.id) !== -1 &&
                currentQueryGeneIds.indexOf(coexPair.queryGene.id) !== -1);
        }

        function meetsStringency(coexPair) {
            return (coexPair.posSupp >= filterStringency || coexPair.negSupp >= filterStringency);
        }

        function addCoexPairIfAbsent(coexPair) {
            if (!isEdgePresentInGraph(coexPair)) {
                trimmedCoexPairs.push(coexPair);

                // Edges going in the opposite direction are equivalent, so we keep track of both.
                _edgeSet.push(coexPair.foundGene.officialSymbol + "to" + coexPair.queryGene.officialSymbol);
                _edgeSet.push(coexPair.queryGene.officialSymbol + "to" + coexPair.foundGene.officialSymbol);
            }
        }

        function isEdgePresentInGraph (coexPair) {
            return (_edgeSet.indexOf(coexPair.queryGene.officialSymbol + "to" + coexPair.foundGene.officialSymbol) !== -1);
        }

        for (i = 0; i < coexPairs.length; i++) {
            // if either gene is part of query genes set AND edge meets stringency threshold
            // that means we only add nodes and edges around query genes
            if (keepQueryGeneEdges) {
                if (bothAreQueryGenes(coexPairs[i])) {
                    addGeneIfAbsent(coexPairs[i].foundGene);
                    addGeneIfAbsent(coexPairs[i].queryGene);

                    addCoexPairIfAbsent(coexPairs[i]);
                }
            }

            if (meetsStringency(coexPairs[i]) &&
                atleastOneGeneIsQueryGene(coexPairs[i])) {

                addGeneIfAbsent(coexPairs[i].foundGene);
                addGeneIfAbsent(coexPairs[i].queryGene);

                addCoexPairIfAbsent(coexPairs[i]);
            }
        }

        // Add edges between non-query genes
        // we need to loop through again to add edges that we missed the first
        // time (because we were unsure whether both nodes would be in the
        // graph)
        for (i = 0; i < coexPairs.length; i++) {

            // if both nodes of the edge are in the graph, and it meets the
            // stringency threshold, and neither of the nodes are query
            // genes(because their edges have already been added)
            if (bothGenesPresent(coexPairs[i]) &&
                meetsStringency(coexPairs[i]) &&
                bothAreNotQueryGenes(coexPairs[i])) {

                addCoexPairIfAbsent(coexPairs[i]);
            }
        }

        return {
            geneIds : graphNodeIds,
            coexpressionPairs: trimmedCoexPairs
        };

    },

    // Find stringency that trims graph up to a certain size.
    trimGraphUpToRequestedSize: function (coexpressionPairs, currentQueryGeneIds, currentStringency, stringencyTrimLimit, resultsSizeLimit) {
        var result;
        for (var stringency = currentStringency; stringency <= stringencyTrimLimit; stringency++) {
            var trimmed = Gemma.CoexValueObjectUtil.trimKnownGeneResultsWithQueryGenes(coexpressionPairs, currentQueryGeneIds, stringency, true);

            result = { geneResults: trimmed.coexpressionPairs,
                       trimStringency: stringency,
                       size: trimmed.coexpressionPairs.length };

            if (trimmed.coexpressionPairs.length < resultsSizeLimit) {
                return result;
            }
        }
        return result;
    },

    trimKnownGeneResults: function (knowngenes, filterStringency) {
        var trimmedGeneResults = [];
        var i;
        var kglength = knowngenes.length;
        for (i = 0; i < kglength; i++) {

            // go in only if the query or known gene is contained in the
            // original query geneids AND the stringency is >= the filter
            // stringency
            if ((knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency)) {
                trimmedGeneResults.push(knowngenes[i]);
            }
        }

        return trimmedGeneResults;
    },

    /**
     *
     *
     * @param knownGeneResults
     * @param queryGeneOnlyResults
     * @returns {*}
     */
    combineKnownGeneResultsAndQueryGeneOnlyResults: function (knownGeneResults, queryGeneOnlyResults) {
        var _edgeSet = [];
        var coexpressionPairs = [];
        var i, len;

        function addCoexPairIfAbsent(coexPair) {
            if (!isEdgePresentInGraph(coexPair)) {
                coexpressionPairs.push(coexPair);

                // Edges going in the opposite direction are equivalent, so we keep track of both.
                _edgeSet.push(coexPair.foundGene.officialSymbol + "to" + coexPair.queryGene.officialSymbol);
                _edgeSet.push(coexPair.queryGene.officialSymbol + "to" + coexPair.foundGene.officialSymbol);
            }
        }

        function isEdgePresentInGraph (coexPair) {
            return (_edgeSet.indexOf(
                coexPair.queryGene.officialSymbol + "to" + coexPair.foundGene.officialSymbol) !== -1);
        }

        for (i = 0, len = knownGeneResults.length; i < len; i++) {
            addCoexPairIfAbsent(knownGeneResults[i]);
        }
        for (i = 0, len = queryGeneOnlyResults.length; i < len; i++) {
            addCoexPairIfAbsent(queryGeneOnlyResults[i]);
        }
        return coexpressionPairs;
    },

    // used by coexpressionGrid to grab results for exporting
    filterGeneResultsByText: function (text, knowngenes) {
        var value = new RegExp(Ext.escapeRe(text), 'i');
        var genesMatchingSearch = [];
        var kglength = knowngenes.length;
        var i;
        for (i = 0; i < kglength; i++) {
            if (value.test(knowngenes[i].foundGene.officialSymbol) ||
                value.test(knowngenes[i].queryGene.officialSymbol) ||
                value.test(knowngenes[i].foundGene.officialName) ||
                value.test(knowngenes[i].queryGene.officialName)) {
                genesMatchingSearch.push(knowngenes[i]);
            }
        } // end for (<kglength)
        return genesMatchingSearch;
    },

    getHighestResultStringencyUpToInitialDisplayStringency: function (knowngenes, initialDisplayStringency) {
        var highestResultStringency = 2;
      var kglength = knowngenes.length;
        var i;
        for (i = 0; i < kglength; i++) {
            if (knowngenes[i].posSupp > highestResultStringency) {
                highestResultStringency = knowngenes[i].posSupp;
            }
            if (knowngenes[i].negSupp > highestResultStringency) {
                highestResultStringency = knowngenes[i].negSupp;
            }
            if (highestResultStringency >= initialDisplayStringency) {
                return initialDisplayStringency;
            }
        } // end for (<kglength)
        return highestResultStringency;
    }
};