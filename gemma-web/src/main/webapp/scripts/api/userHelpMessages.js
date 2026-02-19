Ext.namespace('Gemma.HelpText', 'Gemma.StatusText');

Gemma.CITATION = "Zoubarev, A., et al., Gemma: A resource for the re-use, sharing and meta-analysis of expression profiling data. Bioinformatics, 2012.";
Gemma.CITATION_DIRECTIONS = "If you use this file for your research, please cite: {0}" + Gemma.CITATION;
Gemma.PHENOCARTA_CITATION = "If you use this file for your research, please cite PhenoCarta (previously known as Neurocarta): Portales-Casamar, E., et al., Neurocarta: aggregating and sharing disease-gene relations for the neurosciences. BMC Genomics. 2013 Feb 26;14(1):129.";

(function() {
	Gemma.helpTip = function(selecter, message, theme) {
		return function(c) {
			window.jQuery(selecter).qtip({
				content: message,
				style: {
					name: theme ? theme : 'cream'
				}
			});
		};
	};
})();

// TT = tooltip
Gemma.EvidenceCodeInfo = {
	getQtipInfo: function(code, evidenceCodeInfo) {
		return {
			text: '<b>' + code + ': ' + evidenceCodeInfo.name + '</b><br />' + evidenceCodeInfo.description,
			width: 370
		};
	},
	EXP: {
		name: 'Inferred from Experiment',
		description: 'An experimental assay has been located in the cited reference, whose results indicate a gene association (or non-association) to a phenotype.'
	},
	IAGP: {
		name: 'Inferred from Association of Genotype and Phenotype',
		description: 'The association between the gene and phenotype is inferred based on association studies comparing case and control groups.'
	},
	IBA: {
		name: 'Inferred from Biological aspect of Ancestor',
		description: ''
	},
	IBD: {
		name: 'Inferred from Biological aspect of Descendant',
		description: ''
	},
	IC: {
		name: 'Inferred by Curator',
		description: 'The association between the gene and phenotype is not supported by any direct evidence, but can be reasonably inferred by a curator. This includes annotations from animal models or cell cultures.'
	},
	IDA: {
		name: 'Inferred from Direct Assay',
		description: ''
	},
	IEA: {
		name: 'Inferred from Electronic Annotation',
		description: ''
	},
	IED: {
		name: 'Inferred from Experimental Data',
		description: 'The association between the gene and phenotype is inferred based on association studies comparing case and control groups.'
	},
	IEP: {
		name: 'Inferred from Expression Pattern',
		description: 'The association between the gene and phenotype is inferred from the timing or location of expression of a gene.'
	},
	IGC: {
		name: 'Inferred from Genomic Context',
		description: ''
	},
	IGI: {
		name: 'Inferred from Genetic Interaction',
		description: 'The association between the gene and phenotype is inferred based on a mutation in another gene.'
	},
	IKR: {
		name: 'Inferred from Key Residues',
		description: ''
	},
	IMP: {
		name: 'Inferred from Mutant Phenotype',
		description: 'The association between the gene and phenotype is inferred based on differences in the function, process, or cellular localization between two different alleles of the corresponding gene.'
	},
	IPI: {
		name: 'Inferred from Physical Interaction',
		description: ''
	},
	IPM: {
		name: 'Inferred from Phenotype Manipulation',
		description: 'The association between the gene and phenotype is inferred based on phenotype manipulation in model organisms.'
	},
	IRD: {
		name: 'Inferred from Rapid Divergence',
		description: ''
	},
	ISA: {
		name: 'Inferred from Sequence Alignment',
		description: ''
	},
	ISM: {
		name: 'Inferred from Sequence Model',
		description: ''
	},
	ISO: {
		name: 'Inferred from Sequence Orthology',
		description: ''
	},
	ISS: {
		name: 'Inferred from Sequence or Structural Similarity',
		description: ''
	},
	NAS: {
		name: 'Non-traceable Author Statement',
		description: ''
	},
	ND: {
		name: 'No biological Data available',
		description: ''
	},
	NR: {
		name: 'Not Recorded',
		description: ''
	},
	QTM: {
		name: 'Quantitative Trait Measurement',
		description: 'The association between the gene and phenotype is inferred based on correlation between genotype and severity of the phenotype.'
	},
	RCA: {
		name: 'inferred from Reviewed Computational Analysis',
		description: ''
	},
	TAS: {
		name: 'Traceable Author Statement',
		description: 'The gene-to-phenotype association is stated in a review paper or a website (external database) with a reference to the original publication.'
	}
};

Gemma.StatusText = {
	Loading: {
		arrayDesigns: "Loading platforms...",
		experiments: "Loading experiments...",
		experimentFactors: "Loading factors",
		generic: "Loading...",
		genes: "Loading genes..."
	},
	processing: "Processing...",
	waiting: "Please wait",
	creating: "Creating new {0}",
	saving: "Saving...",
	deleting: "Deleting...",
	deletingSpecific: "Deleting {0}...",
	Searching: {
		generic: "Processing request...",
		analysisResults: "Processing request..."
	}
};
Gemma.HelpText.CommonWarnings = {
	LoseChanges: {
		title: 'Changes will be lost!',
		text: 'You have unsaved changes, are you sure you want to refresh?'
	},
	UnsavedChanges: {
		title: "Unsaved changes",
		text: "There are unsaved changes. Do you want to continue without saving?"
	},
	Deletion: {
		title: 'Confirm Deletion',
		text: "Are you sure you want to delete this {0}? This cannot be undone."
	},
	Redo: {
		title: 'Confirm redo',
		text: "Are you sure you want to redo this {0}? This cannot be undone."
	},
	RefreshStats: {
		title: 'Confirm refresh',
		text: "Are you sure you want to refresh statistics for this {0}? This cannot be undone."
	},
	ReIndexing: {
		title: 'Confirm Re-Indexing',
		text: "Are you sure you want to rebuild the {0} indicies? Old indices will be deleted."
	},
	DuplicateName: {
		title: "Duplicate Name",
		text: "Please provide a previously unused name for the group"
	},
	BrowserWarnings: {
		ie8: 'Advanced differential expression visualizations are not available in your browser (Internet Explorer 8). We suggest upgrading to  '
			+ '<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>, '
			+ '<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or '
			+ '<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.',
		ieNot8: 'This page may display improperly in older versions of Internet Explorer(IE). Please upgrade to '
			+ '<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">IE 9</a>, '
			+ '<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or '
			+ '<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.'
			+ ' If you are running IE 9 and you see this message, please make sure you are not in compatibility mode. ',
		generic: 'This page may not display properly in all browsers. (The \"canvas\" element is requried.)'
			+ ' Please switch to '
			+ '<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a>,'
			+ '<a href="http://www.google.com/chrome/" target="_blank">Chrome</a> or'
			+ '<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>.',

		ie7: 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 9 or newer.'
	},
	Timeout: {
		title: 'Gemma is Busy',
		text: "Gemma appears to be under heavy usage, please try your query again in a few minutes"
	}
};

Gemma.HelpText.CommonErrors = {
	MissingInput: {
		title: 'Missing information',
		taxon: "Please select a taxon"
	},
	InvalidForm: {
		title: "Submit Failed",
		text: "Form is not valid, check entries before clicking 'submit'"
	},
	EmptySet: {
		title: "Cannot save an empty group",
		text: "You cannot save an empty group. No changes have been saved.<br>"
			+ " Add experiments to group {0} or delete it."
	},
	accessDenied: 'Your access is denied.',
	objectAlreadyRemoved: 'This record cannot be found because it has been removed by someone else.<br />Please reload the page to view the latest records.',
	userNotLoggedIn: 'You are not logged in. Please log in to try again.',
	errorUnknown: 'System error has occurred. Please contact the system administrator for assistance.'
};

Gemma.HelpText.WidgetDefaults = {
	// name of widget, exactly
	AjaxLogin_AjaxLoginWindow: {
		passwordHintLink: "Forgot your password?",
		registerButton: "Need an account? Register",
		invalidLogin: 'Invalid Username/Password'
	},
	AjaxLogin_AjaxRegister: {
		successTitle: "Registration Successful",
		successText: "A confirmation email was sent. Please check your mail and click the link it contains"
	},
	AnalysisResultsSearchNonWidget: {
		CoexpressionTutorial: {
			supportColumnTitle: 'Support',
			supportColumnText: 'The number in the support column corresponds to the amount of dataset support.  Positive support is denoted by green text. Negative support is denoted by red text',
			visualiseColumnTitle: 'Visualise',
			visualiseColumnText: 'what does this do',
			nodeDegreeColumnTitle: 'Specificity',
			nodeDegreeColumnText: 'Specificity measures the coexpression of the gene with all other genes in their taxon. Genes with higher specificity show less coexpression with all other genes in their taxon.',
			stringencyTitle: 'Stringency',
			stringencyText: 'Use the stringency control to add/remove genes with more/less dataset support that confirms coexpression',
			cytoNodeDegreeTitle: 'node deg 2nd',
			cytoNodeDegreeText: 'explain visualisation of node deg',
			saveTitle: 'Saving your results',
			saveText: 'explain formats',
			updateQueryTitle: 'new query from chart',
			updateQueryText: 'click and drag etc',
			visualizeTabTitle: 'Visualization Tab',
			visualizeTabText: 'Click the visualization tab to view your coexpression results as a gene network, click the Help button after the visualization has loaded for information on its features.'
		}
	},
	AnalysisResultsSearchForm: {
		trimmingWarningTitle: "Warning",
		trimmingWarningText: "You are using {0} for your search. " + "Searching for more than {1}"
			+ " can take some time to load and can slow down your interactions with the search results. "
			+ "We suggest you cancel this search and refine your selections or let us trim your query.",
		autoTrimmingText: "You can only search up to {0} {1}. Please note that your list of {1} has been trimmed automatically.",
		taxonModeTT: "Searches are limited to one taxon, if you want to change the taxon, click the reset button.",
		Examples: {
			diffEx1Text: "Hippocampus development & autism (human)",
			diffEx1TT: 'Search for differential expression patterns in ten experiments studying autism spectrum disorder based on '
				+ 'genes from the &quot;hippocampus development&quot; GO group (human)',

			diffEx2Text: "Forebrain neuron differentiation in fetal mice (mouse)",
			diffEx2TT: 'Search for differential expression patterns in of genes from the &quot;forebrain neuron '
				+ 'differentiation&quot; GO group in experiments using fetal/embryonic mouse samples on the GPL1261 platform. (mouse)',

			coex1Text: "Regulation of cell division (yeast)",
			coex1TT: 'Search for coexpression patterns in thirty-three experiments based on genes in the &quot;regulation of cell division&quot; '
				+ 'GO group (yeast)',

			coex2Text: "Protein localization to the synapse (human)",
			coex2TT: 'Search for coexpression patterns in human brain experiments based on genes the &quot;Protein localization to the synapse&quot;'
				+ ' GO group (human)'

		}
	},
	AnnotationGrid: {
		parentLinkDescription: "The 'owner' of this annotation. May be hidden due to security.",
		taggingHelpTitle: "Help with tagging",
		taggingHelpText: "Select a 'category' for the term; then enter a term, "
			+ "choosing from existing terms if possible. "
			+ "Click 'create' to save it. You can also edit existing terms;"
			+ " click 'save' to make the change stick, or 'delete' to remove a selected tag.",
		objectClassDescription: "How this annotation is associated. Indirect annotations are not editable here."
	},
	ArrayDesignsNonPagingGrid: {
		emptyText: 'Either you didn\'t select any platforms, or you don\'t have permissions to view the ones you chose.',
		actionsColumnTT: 'Regenerate this report or delete orphaned designs (designs that aren\'t used by any experiments in Gemma)',
		isMergedTT: "merged: this design was created by merging others",
		isMergeeTT: "mergee: this design was merged with others to create a new design",
		isSubsumedTT: "subsumed: all the sequences in this design are covered by another",
		isSubsumerTT: "subsumer: this design \'covers\' one or more others in that it contains all their sequences",
		hideOrphansTT: "Click to show/hide platforms that aren't used by any experiments in Gemma",
		hideAffyAltTT: "Click to show/hide platforms that are alternative Affymetrix layouts (generally not used in Gemma)",
		hideTroubledTT: "Click to show/hide platforms that are troubled",
		hideMergeeTT: "Click to show/hide platforms that are merged into others"
	},
	CoexpressionGrid: {
		stringencySpinnerTT: "Adjust the threshold for the evidence supporting coexpression",
		supportColumnTT: "Number of data sets supporting coexpression. Green indicates positive correlations, red negative. "
			+ "Thus 5/27 means the coexpression was supported by 5 out of 27 tested data sets.",
		specificityColumnTT: "Indicates how many other links these genes have at the same or higher level of support, genome-wide."
	},
	CytoscapePanel: {
		extendNodeText: 'Extend Selected Nodes',
		searchWithSelectedText: 'Search with Selected Nodes',
		applyGeneListOverlayText: 'Apply Gene List Overlay',
		clearGeneListOverlayText: 'Clear Gene List Overlay',
		stringencySpinnerTT: "Adjust the threshold for the evidence supporting coexpression",
		widgetHelpTT: 'Click here for documentation on how to use this visualizer.',
		zoomToFitText: 'Center graph',
		refreshLayoutText: 'Refresh Layout',
		nodeDegreeEmphasisText: 'Specificity Emphasis',
		nodeDegreeEmphasisTT: 'Specificity is represented by the shade of a gene node (darker=more specific). '
			+ 'Specificity indicates how many other links these genes have at the same or higher level of support, genome-wide.',
		lowStringencyWarning: 'Lowering the stringency further will trigger a new search to retrieve more results. '
			+ 'Low stringency results may not be meaningful for the number of datasets you are searching',
		exportPNGWindowTitle: 'Right-click the image and save the image as file.',
		exportGraphMLWindowTitle: 'GraphML data',
		exportXGMMLWindowTitle: 'XGMML data',
		exportSIFWindowTitle: 'SIF data',
		exportSVGWindowTitle: 'SVG data',
		searchStatusTitle: 'Status of Search',
		searchStatusTooMany: 'Max number of selected genes is {0} for a complete coexpression search, results will only display coexpression between query genes',
		searchStatusTooManyReduce: 'Total number of visible query genes and selected genes for complete coex search exceeds max of {0}. Click Yes to continue with search results that will only display coexpression between query genes',
		searchStatusTooFew: 'No Genes Selected',
		searchStatusNoExtraSelectedForExtend: 'You must select a non-query gene to extend',
		searchStatusNoMoreResults: 'No more results found for this gene',
		compressGraphText: 'Compress Graph',
		unCompressGraphText: 'Uncompress Graph',
		nodeLabelsText: 'Node Labels On',
		noNodeLabelsText: 'Node Labels Off',
		invalidStringencyWarning: 'You cannot lower the stringency below ' + Gemma.MIN_STRINGENCY
			+ '. Please select a different stringency',
		newSearchOrReturnToCurrentStringencyOption: 'A new query will be required to fetch the lower-stringency data.'
			+ ' Keep the current stringency of {0} or do a new search at stringency {1}?',
		graphSizeMenuTT: 'Due to browser performance limitations when rendering large graphs we have trimmed edges between non-query genes. Use the control'
			+ ' to adjust the trim stringency and change the number of edges in your graph',
		graphSizeMenuTT2: 'Due to browser performance limitations when rendering large graphs we have trimmed edges between non-query genes.'

	},
	DatasetGroupEditor: {
		widgetTT: "Use this tool to create and edit groups of datasets. "
			+ "You can modify a built-in group by making a copy (clone) and editing the copy",
		helpURL: Gemma.WIKI_URL + "/Dataset+chooser"
	},
	DatasetGroupGridPanel: {
		protectedTT: "Protected; cannot have members changed, usually applies to automatically generated groups."
	},
	EEDetailsVisualizationWidget: {
		visualizaButtonTT: "Click to display data for selected genes, or a 'random' selection of data from this experiment",
		instructions: 'Use the search fields to find individual genes, or groups of genes. '
			+ 'Gene group searches work for GO terms and other groups in Gemma. '
			+ 'To create groups use the <a href=\"' + Gemma.CONTEXT_PATH + '/geneGroupManager.html\">gene group manager</a>.'
			+ ' Click "show" to view the data for those genes. '
			+ 'Note that when viewing gene groups, not all genes in the group are necessarily in the data set.',
		GoButtonText: {
			random: 'Visualize 20 random genes',
			one: 'Visualize one gene',
			multiple: 'Visualize {0} genes'
		},
		StatusText: {
			random: 'Visualizing 20 random design elements.',
			one: 'Visualizing selected gene.',
			multiple: 'Visualizing selected genes. Note that not all genes are necessarily in the data set.',
			geneMatchCount: 'Found data for {0} of {1} genes.'
		}
	},
	EEManager: {
		customiseDiffExHelpTitle: 'Processed vector analysis',
		customiseDiffExHelpText: 'Choose which factors to include in the model. If you choose only one, the analysis'
			+ ' will be a t-test or one-way-anova. If you choose two factors, you might be able to include interactions. '
			+ 'If you choose three or more, interactions will not be estimated. You can also choose to analyze different '
			+ 'parts of the data sets separately, by splitting it up according to the factors listed. The analysis is then '
			+ 'done independently on each subset.'
	},
	ExperimentalDesignUpload: {
		instructions: '<p>Experimental design submission works in two phases. '
			+ 'First you must upload your design file (file format instructions' + ' <a target="_blank" href="'
			+ Gemma.WIKI_URL + '/Experimental+Design+Upload">here</a>). '
			+ 'Then click "submit". If your file format is invalid or does not match the properties of the '
			+ 'experiment the design is intended for, you will see an error message.</p>'
	},
	ExperimentalFactorAddWindow: {
		descriptionEmptyText: "A short phrase such as 'control vs. drug'",
		descriptionUnique: "Description must be unique among factors",
		continuousCheckboxTT: "Check if the factor is a measurement that can take arbitrary numerical values. If in doubt leave this unchecked.",
		makeFromExistingCharacteristicTT : "Optional: Select an existing 'raw' BioMaterial Characteristic to prepopulate the new factor with." +
			 "If you check 'continuous', the values will be intereprted as numbers."
	},
	// ExperimentalFactorChooserPanel : {
	// helpTitle : "Help for factor choose",
	// helpText : "The meta-analysis can only use one factor per study. Experiments that have more"
	// + " than one factor will be shown here (or view all experiments)."
	// + " Click on the factor field to get a menu for choosing among multiple possibilities. Use the 'hinting' "
	// + "button to choose the type of factor most useful to you, to save manual work. For more help see <a
	// target='_blank' "
	// + "href='" + Gemma.WIKI + "Dataset+chooser#Datasetchooser-TheGemmaexperimentalfactorchooser'>this page</a>",
	// noResultsTitle : "No results",
	// noResultsText : "Sorry, there are no differential expression analyses for the data sets you selected."
	// },
	ExperimentalFactorToolbar: {
		deleteFactorWarningTitle: "Deleting Factor(s)",
		deleteFactorWarningText: 'Are you sure? This cannot be undone. Any associated differential expression analyses will be deleted as well.'
	},
	ExperimentPagingGrid: {
		emptyText: 'Either you didn\'t select any experiments, or you don\'t have permissions to view the ones you chose.'
	},
	ExperimentSearchAndPreview: {
		widgetHelpTT: 'Select a group of experiments or try searching for experiments by name, '
			+ ' or keywords such as: schizophrenia, hippocampus, GPL96 etc.<br><br>'
			+ '<b>Example: search for Alzheimer\'s and select all human experiments.</b><br/><br/>Coexpression only: '
			+ 'Leave blank to search all eligible experiments (requires a gene constraint).'
	},
	ExpressionDataUpload: {
		instructions: '<ul class="plainList" >'
			+ '<li>Is your data available on GEO? If so, it is probably already loaded for you. '
			+ 'Check <a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showAllExpressionExperiments.html">here</a>.</li>'
			+ '<li>Complete all sections of the form, upload your data file (compress it first to speed things up)'
			+ ', and click "Validate data"; once validation is complete you will be able to click "Submit data".</li>'
			+ '<li>Most of the descriptive text you enter can be modified later. '
			+ 'The taxon, platform and the data themselves cannot easily be altered after submission.</li>'
			+ '<li>For help with the file data file format, see '
			+ '<a target="_blank" href="https://pavlidislab.github.io/Gemma/upload.html">this page</a>.</li> '
			+ '<li>The probe identifiers in your file must match those in the platform on record.</li>'
			+ '<li>If you used more than one platform in your study, there may be a "combined" platform that will take care of your case. If not, let us know.</li>'
			+ '<li>Problems? Questions? Please <a href="mailto:pavlab-support@msl.ubc.ca">contact us</a></li></ul>',
		loadingGEOWarningTitle: 'GEO Dataset Check',
		loadingGEOWarningText: 'It looks like you\'re trying to load a GEO dataset. '
			+ 'Please check that it is not already loaded in Gemma. '
			+ '\nTry looking <a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showAllExpressionExperiments.html">here</a>.'
	},
	ExpressionExperimentDetails: {
		statusMultiplePreferredQuantitationTypes: 'This experiment has multiple \'preferred\' quantitation types. '
			+ 'This isn\'t necessarily a problem but is suspicious.',
		statusMultipleTechnologyTypes: 'This experiment seems to mix platforms with different technology types.',
		profileDownloadTT: 'Tab-delimited data and experimental design files for this experiment. '
			+ 'The filtered version corresponds to what is used in most Gemma analyses, removing some probes/elements. Unfiltered includes all elements.',
		dataReprocessed: "Reprocessed from raw data by Gemma.",
		dataExternal: "Processed data are from external source, not reprocessed from raw by Gemma",
		noBatchInfo: "Information on sample batching was not available",
		noBatchesSingletons: "Gemma was unable to form a reasonable batching structure due to batch(es) with one sample",
		noBatchesBadHeaders: "Information on sample batching was not extractable from the available data",
		statusUnsuitableForDEA: "Data or experimental design not suitable for differential expression analysis."
	},
	ExpressionExperimentGrid: {
		// only column TTs, skipped
	},
	ExpressionExperimentListView: {
		// only column TTs, skipped
	},
	FactorValueGrid: {
		deleteFactorValueWarningTitle: "Deleting Factor Value(s)",
		// see deleteFactorWarningText
		deleteFactorValueWarningText: 'Are you sure? This cannot be undone. Any associated differential expression analyses will be deleted as well.'

	},
	GeneAllenBrainAtlasImages: {
		helpTT: "Below is a sampling of in situ hybridization from the Allen Brain Atlas. Beside is a link to the Allen Brain Atlas"
	},

	GeneDetails: {
		probesTT: 'Number of elements/probes for this gene on expression platforms in Gemma',
		assocExpTT: 'Experiments that specifically involve manipulation (or naturally-occurring variation/mutations) of this gene as part of their design.',
		multifuncTT: 'Overall multifunctionality is a function of both the number of GO terms (including parent terms) and '
			+ 'the sizes of the groups. We show the value relative to other genes for this taxon as a normalized rank. '
			+ 'Values range from 0-1 where 1 indicates the highest multifunctionality. '
			+ 'Note that genes with no GO terms have a non-zero multifuntionality rank because of ties.',
		nodeDegreeTT: 'How many coexpression links this gene has at varying levels of support (number of studies).',
		phenotypeTT: 'Phenotypes this gene has been found to be associated with. Click the links or go to the &quot;Phenotypes&quot; tab for more detail.'
	},

	GeneImportPanel: {
		instructions: "Type or paste in gene symbols, one per line, up to {0}. (Note that searches are "
			+ "limited to a smaller number of genes.)"
	},
	GeneMembersSaveGrid: {
		saveAsTT: 'Save your selection as a new group.',
		saveTT: 'Save your selection permanently.',
		doneTT: 'Return to search using your edited list. (Selection will be kept temporarily.)',
		exportTT: 'Get a plain text version of this list'
	},
	GeneSearchAndPreview: {
		instructions: 'Select a general group of genes or try searching for genes by symbol, '
			+ 'GO terms or keywords such as: schizophrenia, hippocampus etc.<br><br>'
			+ '<b>Example: search for "map kinase" and select a GO group</b>.<br/><br/>Coexpression only: Leave blank to search all genes (requires experiment constraint).',
		symbolListButtonInstructions: "Select multiple genes with a list of symbols or NCBI IDs",
		inexactFromList: '<div style="padding-bottom:7px;color:red;">Not all symbols had exact matches ('
			+ '<a onmouseover="this.style.cursor=\'pointer\'" '
			+ 'onclick="Ext.Msg.alert(\'Query Result Details\',\'<br>{0}{1}'
			+ '\');" style="color: red; text-decoration: underline;">details</a>)</div>'
	},
	ManageGroups: {
		groupInUseErrorText: 'Cannot delete group while permissions are set. Uncheck all checkboxes in the panel to the right, save your changes and try again.',
		groupInUseErrorTitle: 'Group in use'
	},
	MetaAnalysisManagerGridPanel: {
		ErrorTitle: {
			saveMetaAnalysisAsEvidence: 'Cannot save meta-analysis as Phenocarta evidence',
			removeMetaAnalysis: 'Cannot remove meta-analysis',
			viewMetaAnalysisDetail: 'Cannot view meta-analysis detail'
		},
		ErrorMessage: {
			evidenceExist: 'Please remove Phenocarta evidence before removing meta-analysis.'
		}
	},
	MetaAnalysisShowResultPanel: {
		ErrorTitle: {
			resultSetsNotAnalyzed: 'Cannot analyze result sets',
			resultSetsNotSaved: 'Cannot save result sets'
		},
		ErrorMessage: {
			resultSetsNotAnalyzed: 'Result sets cannot be analyzed.',
			resultSetsNotSaved: 'Result sets cannot be saved.'
		}
	},
	MetaAnalysisEvidenceWindow: {
		ErrorTitle: {
			removeEvidence: 'Cannot remove Phenocarta evidence'
		},
		ErrorMessage: {
			qvalueThresholdOutOfRange: 'q-value threshold should be greater than {0}, and less than or equal to {1}.'
		}
	},
	MetaheatmapApplication: {
		noGenesSelectedTitle: "No Genes Selected",
		noGenesSelectedText: "Selection cannot be saved because no genes have been selected. "
			+ "To select genes, hold down &quot;Ctrl&quot; and click on gene symbols.",
		noDatasetsSelectedTitle: "No Experiments Selected",
		noDatasetsSelectedText: "Selection cannot be saved because no experiments have been selected. "
			+ "To select experiments, hold down &quot;Ctrl&quot; and click on condition labels.",
		Tutorial: {
			searchResultsTitle: 'Search Results',
			searchResultsText: 'Your results are displayed as a heatmap of genes vs conditions. Fold change is shown with cell color and p values are encoded by opacity of the black inner rectangle. Hover over a cell for more details.',
			foldChangeTitle: 'Fold change and p value',
			foldChangeText: 'Use this button to show or hide p value.',
			colourLegendTitle: 'Color Legend',
			colourLegendText: 'View the color legend for the chart.',
			sortAndFilterTitle: 'Sort and Filter',
			sortAndFilterText: 'Change the layout of your data to clarify patterns.',
			downloadTitle: 'Download your Results',
			downloadText: 'Save an image of your chart or save a text version of the results.',
			instructions: 'This tutorial will point out some features of the differential expression search interface.'
			// //
			// Click
			// the
			// "next"
			// and
			// "previous"
			// buttons
			// to
			// navigate
			// between
			// tips
			// and
			// click
			// the
			// "X"
			// to
			// close
			// the
			// tutorial.'
		}
	},
	PhenotypeEvidenceGridPanel: {
		specificallyRelatedTT: 'Marks evidence related specifically to your phenotype search',
		negativeEvidenceTT: 'Denotes evidence against association'
	},
	PhenotypePanel: {
		noRecordEmptyText: 'No phenotype associations',
		noPhenotypeSelectedForGeneGridEmptyText: 'No phenotype has been selected.',
		noGeneSelectedForEvidenceGridEmptyText: 'No gene has been selected.',
		setupErrorTitle: 'Error in Gemma.PhenotypePanel',
		setupErrorText: 'If you are using PhenotypePanel inside of Gemma, <b>phenotypeStoreProxy</b>,<br />'
			+ '<b>geneStoreProxy</b>, <b>evidenceStoreProxy</b> and <b>getGeneLink</b><br />'
			+ 'should not be set in config. Otherwise, all of them should be set.<br />' + '',
		modifyPhenotypeAssociationOutsideOfGemmaTitle: "Add new phenotype association",
		modifyPhenotypeAssociationOutsideOfGemmaText: "To add, edit or remove gene-phenotype associatons, please go to the <a target='_blank' href='" + Gemma.CONTEXT_PATH + "/phenotypes.html'>Gemma website</a>.",
		viewBibliographicReferenceOutsideOfGemmaTitle: "View bibliographic reference",
		viewBibliographicReferenceOutsideOfGemmaText: "To view bibliographic reference, please go to the <a target='_blank' href='" + Gemma.CONTEXT_PATH + "/phenotypes.html'>Gemma website</a>.",
		filterMyAnnotationsOutsideOfGemmaTitle: "Filter by my annotations",
		filterMyAnnotationsOutsideOfGemmaText: "To filter by my annotations, please go to the <a target='_blank' href='" + Gemma.CONTEXT_PATH + "/phenotypes.html'>Gemma website</a>."
	},
	PhenotypeAssociationForm: {
		ErrorMessage: {
			// The followings are from ValidateEvidenceValueObject.
			userNotLoggedIn: 'You are not logged in. Please log in to try again.',
			accessDenied: 'Your access is denied.',
			lastUpdateDifferent: 'This evidence has been modified by someone else. Please reload the page to view the updated version.',
			evidenceNotFound: 'This evidence cannot be found because it has been removed by someone else.<br />Please reload the page to view the latest records.',
			pubmedIdInvalid: '{0} is not valid.',
			sameGeneAndPhenotypesAnnotated: 'An identical annotation (same gene and same phenotype(s)) already exists for this PubMed Id.',
			sameGeneAnnotated: 'An annotation for this gene already exists for this PubMed Id.',
			sameGeneAndOnePhenotypeAnnotated: 'A similar annotation (same gene and related phenotype(s)) already exists for this PubMed Id.',
			sameGeneAndPhenotypeChildOrParentAnnotated: 'A similar annotation (same gene and related phenotype(s)) already exists for this PubMed Id.',
			sameEvidenceFound: 'The same evidence already exists.',
			// The followings are NOT from ValidateEvidenceValueObject.
			errorUnknown: 'System error has occurred. Please contact the system administrator for assistance.',
			pubMedIdsDuplicate: '{0} and {1} should not be the same.',
			pubMedIdOnlyPrimaryEmpty: '{0} is required if you specify {1}.',
			phenotypesDuplicate: 'Phenotypes should not have duplicate.',
			experimentTagsDuplicate: 'Experiment tags should not have duplicate.'
		}
	},
	ProbeLevelDiffExGrid: {
		// skipped, only had column TTs
	},
	SecurityManager: {
		noGroupsToShareWith: 'You cannot share this entity because you do not belong to any user groups.'
			+ '<br>Would you like to <a href="' + Gemma.CONTEXT_PATH + '/manageGroups.html">create one</a>? ',
		publicWarning: 'Please note: setting the read permissions to "public" means that all '
			+ 'users of Gemma (registered and anonymous) will be able to view this entity and use it in analyses.'
	},
	Tutorial: {
		ControlPanel: {
			instructions: 'This tutorial will point out some features of this page. Click the "next" and "previous" buttons to navigate between tips and click the "X" to close the tutorial.'
		}
	},
	VisualizationWithThumbsPanel: {
		browserWarning: "Plots use a feature of HTML 5 that runs in IE via emulation unless you have Chrome Frame installed. "
			+ "Firefox, Chrome, Safari and Opera will be faster too.'>"
			+ "Too slow in Explorer? Try <a href='http://www.google.com/chromeframe/"
	}
};
