Ext.namespace("Gemma");

Gemma.CoexValueObjectUtil = {

	// takes an array of CoexpressionValueObjectExts (knowngenes) and trims away
	// results based on stringency and currentQueryGeneIds
	trimKnownGeneResultsWithQueryGenes : function(knowngenes,
			currentQueryGeneIds, filterStringency) {

		// helper array to prevent duplicate nodes from being entered
		var graphNodeIds = [];

		var trimmedGeneResults = [];
		var i;

		var kglength = knowngenes.length;
		for (i = 0; i < kglength; i++) {

			// go in only if the query or known gene is contained in the
			// original query geneids AND the stringency is >= the filter
			// stringency
			if (((currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1 || (currentQueryGeneIds
					.indexOf(knowngenes[i].queryGene.id) !== -1)) && (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency))

			) {

				if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) === -1) {
					graphNodeIds.push(knowngenes[i].foundGene.id);
				}

				if (graphNodeIds.indexOf(knowngenes[i].queryGene.id) === -1) {
					graphNodeIds.push(knowngenes[i].queryGene.id);
				}

				trimmedGeneResults.push(knowngenes[i]);

			} // end if
		} // end for (<kglength)
		// we need to loop through again to add edges that we missed the first
		// time (because we were unsure whether both nodes would be in the
		// graph)
		for (i = 0; i < kglength; i++) {

			// if both nodes of the edge are in the graph, and it meets the
			// stringency threshold, and neither of the nodes are query
			// genes(because their edges have already been added)
			if (graphNodeIds.indexOf(knowngenes[i].foundGene.id) !== -1
					&& graphNodeIds.indexOf(knowngenes[i].queryGene.id) !== -1
					&& (knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency)
					&& currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) === -1
					&& currentQueryGeneIds.indexOf(knowngenes[i].queryGene.id) === -1) {

				trimmedGeneResults.push(knowngenes[i]);

			}

		} // end for (<kglength)

		var trimmed = {};

		trimmed.trimmedKnownGeneResults = trimmedGeneResults;

		trimmed.trimmedNodeIds = graphNodeIds;

		return trimmed;

	},

	trimKnownGeneResultsForReducedGraph : function(knowngenes,
			currentQueryGeneIds, currentStringency, stringencyTrimLimit,
			resultsSizeLimit) {

		var i;

		var displayTrimmedStringency;

		var returningGeneResults = knowngenes;
		var trimmedGeneResults = [];
		var h;

		for (h = currentStringency; h <= stringencyTrimLimit; h++) {

			var graphNodeIds = [];

			var kglength = knowngenes.length;
			for (i = 0; i < kglength; i++) {

				if (((currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1 || (currentQueryGeneIds
						.indexOf(knowngenes[i].queryGene.id) !== -1)))

				) {
					trimmedGeneResults.push(knowngenes[i]);

					// need to populate graphNodeIds appropriately in order to
					// correctly add 'my genes only' edges
					if (currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !== -1
							&& currentQueryGeneIds
									.indexOf(knowngenes[i].queryGene.id) !== -1) {
						graphNodeIds.push(knowngenes[i].foundGene.id);
						graphNodeIds.push(knowngenes[i].queryGene.id);
					} else if (currentQueryGeneIds
							.indexOf(knowngenes[i].foundGene.id) !== -1) {
						graphNodeIds.push(knowngenes[i].foundGene.id);
						if (knowngenes[i].posSupp > h
								|| knowngenes[i].negSupp > h) {
							graphNodeIds.push(knowngenes[i].queryGene.id);
						}

					} else if (currentQueryGeneIds
							.indexOf(knowngenes[i].queryGene.id) !== -1) {
						graphNodeIds.push(knowngenes[i].queryGene.id);
						if (knowngenes[i].posSupp > h
								|| knowngenes[i].negSupp > h) {
							graphNodeIds.push(knowngenes[i].foundGene.id);
						}
					}

				} // end if
			} // end for (<kglength)
			// we need to loop through again to add edges between non query
			// genes that meet the stringency threshold
			for (i = 0; i < kglength; i++) {

				if (currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) === -1
						&& currentQueryGeneIds
								.indexOf(knowngenes[i].queryGene.id) === -1
						&& (knowngenes[i].posSupp > h || knowngenes[i].negSupp > h)
						&& graphNodeIds.indexOf(knowngenes[i].foundGene.id) !== -1
						&& graphNodeIds.indexOf(knowngenes[i].queryGene.id) !== -1) {

					trimmedGeneResults.push(knowngenes[i]);

				}

			} // end for (<kglength)

			if (trimmedGeneResults.length < returningGeneResults.length) {
				displayTrimmedStringency = h;
			}

			returningGeneResults = trimmedGeneResults;

			if (trimmedGeneResults.length < resultsSizeLimit) {
				break;
			}

			trimmedGeneResults = [];

		}

		var returnObject = {};
		returnObject.geneResults = returningGeneResults;
		returnObject.trimStringency = displayTrimmedStringency;

		return returnObject;

	},

	trimKnownGeneResults : function(knowngenes, filterStringency) {
		var trimmedGeneResults = [];

		var i;
		var kglength = knowngenes.length;
		for (i = 0; i < kglength; i++) {

			// go in only if the query or known gene is contained in the
			// original query geneids AND the stringency is >= the filter
			// stringency
			if ((knowngenes[i].posSupp >= filterStringency || knowngenes[i].negSupp >= filterStringency)) {
				trimmedGeneResults.push(knowngenes[i]);
			} // end if
		} // end for (<kglength)

		return trimmedGeneResults;

	},

	getCurrentQueryGeneIds : function(queryGenes) {
		var currentQueryGeneIds = [];
		var qlength = queryGenes.length;

		// populate geneid array for complete graph
		var i;

		for (i = 0; i < qlength; i++) {

			if (currentQueryGeneIds.indexOf(queryGenes[i].id) === -1) {
				currentQueryGeneIds.push(queryGenes[i].id);
			}

		}

		return currentQueryGeneIds;
	},

	// for filtering results down to only results that involve geneids
	filterGeneResultsByGeneIds : function(geneIds, knowngenes) {

		var trimmedGeneResults = [];

		var kglength = knowngenes.length;
		var i;
		for (i = 0; i < kglength; i++) {

			// go in only if the query or known gene is contained in the
			// original query geneids
			if (geneIds.indexOf(knowngenes[i].foundGene.id) !== -1
					|| geneIds.indexOf(knowngenes[i].queryGene.id) !== -1)

			{

				trimmedGeneResults.push(knowngenes[i]);

			} // end if
		} // end for (<kglength)

		return trimmedGeneResults;
	},

	filterGeneResultsByGeneIdsMyGenesOnly : function(geneIds, knowngenes) {

		var trimmedGeneResults = [];

		var kglength = knowngenes.length;
		var i;
		for (i = 0; i < kglength; i++) {

			if (geneIds.indexOf(knowngenes[i].foundGene.id) !== -1
					&& geneIds.indexOf(knowngenes[i].queryGene.id) !== -1)

			{

				trimmedGeneResults.push(knowngenes[i]);

			} // end if
		} // end for (<kglength)

		return trimmedGeneResults;
	},

	combineKnownGeneResultsAndQueryGeneOnlyResults : function(kgResults,
			qgoResults) {

		// only one query gene will result in no qgoResults
		if (!qgoResults) {
			return kgResults;
		}

		var coexEdgeSet = [];

		var combinedResults = [];

		var kglength = kgResults.length;
		var i;

		for (i = 0; i < kglength; i++) {

			if (coexEdgeSet.indexOf(kgResults[i].foundGene.officialSymbol
					+ "to" + kgResults[i].queryGene.officialSymbol) == -1
					&& coexEdgeSet
							.indexOf(kgResults[i].queryGene.officialSymbol
									+ "to"
									+ kgResults[i].foundGene.officialSymbol) == -1) {

				combinedResults.push(kgResults[i]);
				coexEdgeSet.push(kgResults[i].foundGene.officialSymbol + "to"
						+ kgResults[i].queryGene.officialSymbol);
				coexEdgeSet.push(kgResults[i].queryGene.officialSymbol + "to"
						+ kgResults[i].foundGene.officialSymbol);

			}
		}

		var qgolength = qgoResults.length;
		for (i = 0; i < qgolength; i++) {
			if (coexEdgeSet.indexOf(qgoResults[i].foundGene.officialSymbol
					+ "to" + qgoResults[i].queryGene.officialSymbol) == -1
					&& coexEdgeSet
							.indexOf(qgoResults[i].queryGene.officialSymbol
									+ "to"
									+ qgoResults[i].foundGene.officialSymbol) == -1) {
				combinedResults.push(qgoResults[i]);
				coexEdgeSet.push(qgoResults[i].foundGene.officialSymbol + "to"
						+ qgoResults[i].queryGene.officialSymbol);
				coexEdgeSet.push(qgoResults[i].queryGene.officialSymbol + "to"
						+ qgoResults[i].foundGene.officialSymbol);
			}
		}

		return combinedResults;

	},

	// used by cytoscape panel to get gene node ids for highlighting
	filterGeneResultsByTextForNodeIds : function(text, knowngenes) {

		var genesMatchingSearch = [];

		var splitTextArray = text.split(",");

		var j;
		for (j = 0; j < splitTextArray.length; j++) {
			
			splitTextArray[j] = splitTextArray[j].replace(/^\s+|\s+$/g,'');
			
			if (splitTextArray[j].length < 2) continue;

			var value = new RegExp(Ext.escapeRe(splitTextArray[j]), 'i');

			var kglength = knowngenes.length;
			var i;
			for (i = 0; i < kglength; i++) {

				var foundGene = knowngenes[i].foundGene;

				var queryGene = knowngenes[i].queryGene;

				if (genesMatchingSearch.indexOf(foundGene.officialSymbol) !== 1) {

					if (value.test(foundGene.officialSymbol)
							|| value.test(foundGene.officialName)) {
						genesMatchingSearch.push(foundGene.officialSymbol);

					}

				}

				if (genesMatchingSearch.indexOf(queryGene.officialSymbol) !== 1) {

					if (value.test(queryGene.officialSymbol)
							|| value.test(queryGene.officialName)) {
						genesMatchingSearch.push(queryGene.officialSymbol);
					}

				}

			} // end for (<kglength)

		}

		return genesMatchingSearch;
	},

	// used by coexpressionGrid to grab results for exporting
	filterGeneResultsByText : function(text, knowngenes) {

		var value = new RegExp(Ext.escapeRe(text), 'i');
		var genesMatchingSearch = [];

		var kglength = knowngenes.length;
		var i;
		for (i = 0; i < kglength; i++) {

			if (value.test(knowngenes[i].foundGene.officialSymbol)
					|| value.test(knowngenes[i].queryGene.officialSymbol)
					|| value.test(knowngenes[i].foundGene.officialName)
					|| value.test(knowngenes[i].queryGene.officialName)) {
				genesMatchingSearch.push(knowngenes[i]);
			}

		} // end for (<kglength)

		return genesMatchingSearch;
	},

	getHighestResultStringencyUpToInitialDisplayStringency : function(
			knowngenes, initialDisplayStringency) {

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

}