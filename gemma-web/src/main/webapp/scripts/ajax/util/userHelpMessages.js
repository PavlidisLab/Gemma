
Ext.namespace('Gemma.HelpText','Gemma.StatusText');

// TT = tooltip
Gemma.EvidenceCodes = {
	expText: 'Inferred from Experiment',
	expTT: 'An experimental assay has been located in the cited reference, whose results indicate a gene association (or non-association) to a phenotype.',
	icText: 'Inferred by Curator',
	icTT: 'The association between the gene and phenotype is not supported by any direct evidence, but can be reasonably inferred by a curator. This includes annotations from animal models or cell cultures.',
	tasText: 'Traceable Author Statement',
	tasTT: 'The gene-to-phenotype association is stated in a review paper or a website (external database) with a reference to the original publication.',
    iepText: 'Inferred from Expression Pattern',
    iepTT: 'The association between the gene and phenotype is inferred from the timing or location of expression of a gene.',
    impText: 'Inferred from Mutant Phenotype',
    impTT: 'The association between the gene and phenotype is inferred based on differences in the function, process, or cellular localization between two different alleles of the corresponding gene.',
    igiText: 'Inferred from Genetic Interaction',
    igiTT: 'The association between the gene and phenotype is inferred based on a mutation in another gene.',
	EXP: 'Inferred from Experiment',
	IC: 'Inferred by Curator',
	TAS: 'Traceable Author Statement',
	IDA: 'Direct Assay',
	IPI: 'Physical Interaction',
	IMP: 'Mutant Phenotype',
	IGI: 'Genetic Interaction',
	IEP: 'Expression Pattern',
	ISS: 'Sequence or Structural Similarity',
	ISO: 'Sequence Orthology',
	ISA: 'Sequence Alignment',
	ISM: 'Sequence Model',
	IGC: 'Genomic Context',
	IBA: 'Biological aspect of Ancestor',
	IBD: 'Biological aspect of Descendant',
	IKR: 'Key Residues',
	IRD: 'Rapid Divergence',
	RCA: 'Reviewed Computational Analysis',
	NAS: 'Non-traceable Author Statement',
	ND: 'No biological Data available',
	IEA: 'Electronic Annotation',
	NR: 'Not Recorded'
};

Gemma.StatusText = {
	Loading: {
		arrayDesigns: "Loading array designs...",
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
	UnsavedChanges:{
		title: "Unsaved changes",
		text: "There are unsaved changes. Do you want to continue without saving?"
	},
	Deletion: {
		title: 'Comfirm Deletion',
		text: "Are you sure you want to delete this {0}? This cannot be undone."
	},
	DuplicateName: {
		title: "Duplicate Name",
		text: "Please provide a previously unused name for the group"
	},
	BrowserWarnings: {
		ie8: 'Advanced differential expression visualizations are not available in your browser (Internet Explorer 8). We suggest upgrading to  ' +
					'<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>, ' +
					'<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or ' +
					'<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.',
		ieNot8: 'This page may display improperly in older versions of Internet Explorer(IE). Please upgrade to ' +
						'<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">IE 9</a>, ' +
						'<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a> or ' +
						'<a href="http://www.google.com/chrome/" target="_blank">Chrome</a>.' +
						' If you are running IE 9 and you see this message, please make sure you are not in compatibility mode. ',
		generic: 'This page may not display properly in all browsers. (The \"canvas\" element is requried.)' +
						' Please switch to ' +
						'<a href="http://www.mozilla.com/en-US/firefox/new/" target="_blank">Firefox</a>,' +
						'<a href="http://www.google.com/chrome/" target="_blank">Chrome</a> or' +
						'<a href="http://windows.microsoft.com/en-US/internet-explorer/downloads/ie" target="_blank">Internet Explorer 9</a>.',
						
		// TODO this one should be looked into and maybe updated
		ie7: 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 8 or newer.'
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
		text: "You cannot save an empty group. No changes have been saved.<br>"+
				" Add experiments to group {0} or delete it."
	}
};

Gemma.HelpText.WidgetDefaults = {
	// name of widget, exactly
	AjaxLogin_AjaxLoginWindow:{
		passwordHintLink: "Forgot your password?",
		registerButton: "Need an account? Register",
		invalidLogin: 'Invalid Username/Password'
	},
	AjaxLogin_AjaxRegister:{
		successTitle :"Registration Successful",
		successText : "A confirmation email was sent. Please check your mail and click the link it contains"
	},
	AnalysisResultsSearchNonWidget:{
		CoexpressionTutorial: {
			supportColumnTitle: 'Support',
			supportColumnText:  'The number in the support column corresponds to the amount of dataset support.  Positive support is denoted by green text. Negative support is denoted by red text',
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
	AnalysisResultsSearchForm:{
		trimmingWarningTitle: "Warning",
		trimmingWarningText: "You are using {0} for your search. " +
					"Searching for more than {1}"+
					" can take some time to load and can slow down your interactions with the search results. " +
					"We suggest you cancel this search and refine your selections or let us trim your query.",
		autoTrimmingText: "You can only search up to {0} {1}. Please note that your list of {1} has been trimmed automatically.",
		taxonModeTT: "Searches are limited to one taxon, if you want to change the taxon, click the reset button.",
		Examples: {
			diffEx1Text : "Hippocampus development & autism (human)",
			diffEx1TT: 'Search for differential expression patterns in ten experiments studying autism spectrum disorder based on '+
								'genes from the &quot;hippocampus development&quot; GO group (human)',
								
			diffEx2Text: "Forebrain neuron differentiation in fetal mice (mouse)",
			diffEx2TT: 'Search for differential expression patterns in of genes from the &quot;forebrain neuron '+
								'differentiation&quot; GO group in experiments using fetal/embryonic mouse samples on the GPL1261 platform. (mouse)',
								
			coex1Text: "Regulation of cell division (yeast)",
			coex1TT: 'Search for coexpression patterns in thirty-three experiments based on genes in the &quot;regulation of cell division&quot; '+
						'GO group (yeast)',
						
			coex2Text: "Protein localization to the synapse (human)",
			coex2TT: 'Search for coexpression patterns in human brain experiments based on genes the &quot;Protein localization to the synapse&quot;'+
						' GO group (human)',
							
		}
	},
	AnnotationGrid: {
		parentLinkDescription: "The 'owner' of this annotation. May be hidden due to security.",
		taggingHelpTitle: "Help with tagging",
		taggingHelpText: "Select a 'category' for the term; then enter a term, "
							+ "choosing from existing terms if possible. "
							+ "Click 'create' to save it. You can also edit existing terms;"
							+ " click 'save' to make the change stick, or 'delete' to remove a selected tag."
	},
	ArrayDesignsNonPagingGrid:{
		emptyText: 'Either you didn\'t select any array designs, or you don\'t have permissions to view the ones you chose.',
		actionsColumnTT: 'Regenerate this report or delete orphaned designs (designs that aren\'t used by any experiments in Gemma)',
		isMergedTT: "merged: this design was created by merging others",
		isMergeeTT: "mergee: this design was merged with others to create a new design",
		isSubsumedTT: "subsumed: all the sequences in this design are covered by another",
		isSubsumerTT: "subsumer: this design \'covers\' one or more others in that it contains all their sequences",
		hideOrhpansTT: "Click to show/hide array designs that aren't used by any experiments in Gemma",
		hideTroubledTT: "Click to show/hide array designs that are troubled"
	},
	CoexpressionGrid :{
		stringencySpinnerTT: "Add/remove genes with more/less dataset support that confirms coexpression",
		myDataButtonTT: "Click to show/hide results containing only my data"
	},
	CytoscapePanel:{
		extendNodeText:'Extend Selected Nodes',
		extendNodeTT: 'Extend the graph by finding new results for selected genes',
		searchWithSelectedText: 'Search with Selected Nodes',
		searchWithSelectedTT: 'Start a new search with selected nodes',
		stringencySpinnerTT: 'Add/remove genes with more/less dataset support that confirms coexpression',
		widgetHelpTT: 'Click here for documentation on how to use this visualizer.',
		saveAsImageTT: 'Open a window with this graph as a PNG image',
		saveAsGraphMLTT: 'Open a window with this graph as GraphML data',
		saveAsXGMMLTT: 'Open a window with this graph as a XGMML data',
		saveAsSIFTT: 'Open a window with this graph as a SIF data',
		saveAsSVGTT: 'Open a window with this graph as a SVG data',
		refreshLayoutText: 'Refresh Layout',
		refreshLayoutTT: 'Refresh the layout of the graph',
		nodeDegreeEmphasisText: 'Specificity Emphasis',
		nodeDegreeEmphasisTT: 'Specificity is represented by the darkness of a gene node. The higher the specificity of a gene is'+
								', the darker it is and the less it shows coexpression with all other genes in that taxon',
		lowStringencyWarning: 'Lowering the stringency to this level will run a new search to retrieve low stringency results. '+
							'Low stringency results may not be meaningful for the number of datasets your are searching in.',
		exportPNGWindowTitle: 'Right-click the image and save the image as file.',
		exportGraphMLWindowTitle: 'GraphML data',
		exportXGMMLWindowTitle: 'XGMML data',
		exportSIFWindowTitle: 'SIF data',
		exportSVGWindowTitle: 'SVG data',
		searchStatusTitle: 'Status of Search',
		searchStatusTooMany: 'Too Many Genes Selected. Max number of selected genes is {0}',
		searchStatusTooManyReduce: 'Too many Query Genes. A max of {0} query genes allowed. Click Yes to continue search with reduced query genes',
		searchStatusTooFew: 'No Genes Selected',
		searchStatusNoExtraSelectedForExtend: 'You must select a non-query gene to extend',
		searchStatusNoMoreResults: 'No more results found for this gene',
		compressGraphTT: 'Compress the graph so that the nodes are closer together',
		compressGraphText: 'Compress Graph',
		unCompressGraphText: 'Uncompress Graph',
		nodeLabelsTT: 'Turn node labels on/off',
		nodeLabelsText: 'Node Labels On',
		noNodeLabelsText: 'Node Labels Off',
		invalidStringencyWarning: 'You cannot lower the stringency below 2. Please select a different stringency',
		newSearchOrReturnToCurrentStringencyOption: 'You are viewing the data on the table at a lower stringency than the current graph data stringency.'+
			'Would you like use the current lowest graph stringency of {0} or search for new graph data at stringency {1}?'
		
	},
	DatasetGroupEditor:{
		widgetTT: "Use this tool to create and edit groups of datasets. "+
				"You can modify a built-in group by making a copy (clone) and editing the copy",
		helpURL: Gemma.HOST + "faculty/pavlidis/wiki/display/gemma/Dataset+chooser"
	},
	DatasetGroupGridPanel:{
		protectedTT: "Protected; cannot have members changed, usually applies to automatically generated groups."
	},
	DiffExpressionGrid:{
		// skipped this one, only had column TTs
	},
	EEDetailsVisualizationWidget:{
		visualizaButtonTT: "Click to display data for selected genes, or a 'random' selection of data from this experiment",
		instructions: 'Use the search fields to find individual genes, or groups of genes. ' +
						'Gene group searches work for GO terms and other groups in Gemma. ' +
						'To create groups use the <a href=\"/Gemma/geneGroupManager.html\">gene group manager</a>.' +
						' Click "show" to view the data for those genes. ' +
						'Note that when viewing gene groups, not all genes in the group are necessarily in the data set.',
		GoButtonText: {
			random: 'Visualize \'random\' genes',
			one: 'Visualize 1 gene',
			multiple: 'Visualize {0} genes'
		},
		StatusText: {
			random: 'Visualizing 20 \'random\' genes.',
			one: 'Visualizaing selected gene.',
			multiple: 'Visualizaing selected genes. Note that not all genes are necessarily in the data set.'
		}
	},
	EEManager:{
		customiseDiffExHelpTitle : 'Processed vector analysis',
		customiseDiffExHelpText: 'Choose which factors to include in the model. If you choose only one, the analysis'+
								' will be a t-test or one-way-anova. If you choose two factors, you might be able to include interactions. '+
								'If you choose three or more, interactions will not be estimated. You can also choose to analyze different '+
								'parts of the data sets separately, by splitting it up according to the factors listed. The analysis is then '+
								'done independently on each subset.'
	},
	ExperimentalDesignUpload:{
		instructions: '<p>Experimental design submission works in two phases. ' +
							'First you must upload your design file (file format instructions' +
							' <a target="_blank" href="' +
							Gemma.HOST +
							'faculty/pavlidis/wiki/display/gemma/Experimental+Design+Upload">here</a>). ' +
							'Then click "submit". If your file format is invalid or does not match the properties of the ' +
							'experiment the design is intended for, you will see an error message.</p>'
	},
	ExperimentalFactorAddWindow: {
		descriptionEmptyText: "A short phrase such as 'control vs. drug'",
		descriptionUnique: "Description must be unique among factors",
		continuousCheckboxTT: "Check if the factor is a measurement that can take arbitrary numerical values. If in doubt leave this unchecked.",
	},
	ExperimentalFactorChooserPanel:{
		helpTitle: "Help for factor choose",
		helpText: "The meta-analysis can only use one factor per study. Experiments that have more"
								+ " than one factor will be shown here (or view all experiments)."
								+ " Click on the factor field to get a menu for choosing among multiple possibilities. Use the 'hinting' "
								+ "button to choose the type of factor most useful to you, to save manual work. For more help see <a target='_blank' "
								+ "href='" + Gemma.WIKI
								+ "Dataset+chooser#Datasetchooser-TheGemmaexperimentalfactorchooser'>this page</a>",
		noResultsTitle: "No results",
		noResultsText: "Sorry, there are no differential expression analyses for the data sets you selected."
	},
	ExperimentalFactorToolbar:{
		deleteFactorWarningTitle: "Deleting Factor(s)",
		deleteFactorWarningText: 'Are you sure? This cannot be undone. Any associated differential expression analyses will be deleted as well.'
	},
	ExperimentPagingGrid:{
		emptyText: 'Either you didn\'t select any experiments, or you don\'t have permissions to view the ones you chose.'
	},
	ExperimentSearchAndPreview:{
		widgetHelpTT: 'Select a group of experiments or try searching for experiments by name, '+
					' or keywords such as: schizophrenia, hippocampus, GPL96 etc.<br><br>'+
					'<b>Example: search for Alzheimer\'s and select all human experiments'
	},
	ExpressionDataUpload:{
		instructions: '<ul class="plainList" >' 
					+ '<li>Is your data available on GEO? If so, it is probably already loaded for you. '
					+ 'Check <a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html">here</a>.</li>'
					+ '<li>Complete all sections of the form, upload your data file (compress it first to speed things up)'
					+ ', and click "Validate data"; once validation is complete you will be able to click "Submit data".</li>'
					+ '<li>Most of the descriptive text you enter can be modified later. '
					+ 'The taxon, array design and the data themselves cannot easily be altered after submission.</li>'
					+ '<li>For help with the file data file format, see '
					+ '<a target="_blank" href="/Gemma/static/expressionExperiment/upload_help.html">this page</a>.</li> '
					+ '<li>The probe identifiers in your file must match those in the array design on record.</li>'
					+ '<li>If you used more than one array type in your study, there may be a "combined" array that will take care of your case. If not, let us know.</li>'
					+ '<li>Problems? Questions? Please <a href="mailto:gemma@ubic.ca">contact us</a></li></ul>',
		loadingGEOWarningTitle: 'GEO Dataset Check',
		loadingGEOWarningText: 'It looks like you\'re trying to load a GEO dataset. ' +
												'Please check that it is not already loaded in Gemma. ' +
												'\nTry looking <a href="/Gemma/expressionExperiment/showAllExpressionExperiments.html">here</a>.'
	},
	ExpressionExperimentDetails: {
		statusMultiplePreferredQuantitationTypes : 'This experiment has multiple \'preferred\' quantitation types. ' +
            'This isn\'t necessarily a problem but is suspicious.',
		statusMultipleTechnologyTypes : 'This experiment seems to mix array designs with different technology types.',
		profileDownloadTT: 'Tab-delimited data file for this experiment. ' +
                    'The filtered version corresponds to what is used in most Gemma analyses, removing some probes. Unfiltered includes all probes'
	},
	ExpressionExperimentGrid:{
		// only column TTs, skipped
	},
	ExpressionExperimentListView:{
		// only column TTs, skipped
	},
	FactorValueGrid:{
		deleteFactorValueWarningTitle: "Deleting Factor Value(s)",
		// see deleteFactorWarningText
		deleteFactorValueWarningText: 'Are you sure? This cannot be undone. Any associated differential expression analyses will be deleted as well.'
		
	},
	GeneAllenBrainAtlasImages:{
		helpTT: "Below is a sampling of in situ hybridization from the Allen Brain Atlas. Beside is a link to the Allen Brain Atlas"
	},
	
	GeneDetails:{
		probesTT: 'Number of probes for this gene on expression platforms in Gemma',
		assocExpTT: 'Experiments that specifically involve manipulation (or naturally-occurring variation/mutations) of this gene as part of their design.',
		multifuncTT : 'Overall multifunctionality is a function of both the number of GO terms and the sizes of the groups. Values range from 0-1 where 1 indicates the highest multifunctionality',
		nodeDegreeTT : 'Relative measure estimating how &quot;hubby&quot; this gene is, based on coexpression. Values range from 0-1 where 1 indicates the highest number of associations.'
	},
	
	GeneImportPanel:{
		instructions: "Type or paste in gene symbols, one per line, up to {0}. (Note that searches are "+
							"limited to a smaller number of genes.)"
	},
	GeneMembersSaveGrid:{
		saveAsTT: 'Save your selection as a new group.',
		saveTT: 'Save your selection permanently.',
		doneTT: 'Return to search using your edited list. (Selection will be kept temporarily.)',
		exportTT: 'Get a plain text version of this list'
	},
	GeneSearchAndPreview: {
		instructions: 'Select a general group of genes or try searching for genes by symbol, '+
					'GO terms or keywords such as: schizophrenia, hippocampus etc.<br><br>'+
					'<b>Example: search for "map kinase" and select a GO group</b>',
		symbolListButtonInstructions: "Select multiple genes with a list of symbols or NCBI IDs",
		inexactFromList: '<div style="padding-bottom:7px;color:red;">Not all symbols had exact matches ('+
							'<a onmouseover="this.style.cursor=\'pointer\'" ' +
							'onclick="Ext.Msg.alert(\'Query Result Details\',\'<br>{0}{1}'+
							'\');" style="color: red; text-decoration: underline;">details</a>)</div>'
	},
	ManageGroups:{
		groupInUseErrorText: 'Cannot delete group while permissions are set. Uncheck all checkboxes in the panel to the right, save your changes and try again.',
		groupInUseErrorTitle: 'Group in use'
	},
	MetaheatmapApplication:{
		noGenesSelectedTitle: "No Genes Selected",
		noGenesSelectedText: "Selection cannot be saved because no genes have been selected. "+
								"To select genes, hold down &quot;Ctrl&quot; and click on gene symbols.",
		noDatasetsSelectedTitle: "No Experiments Selected",
		noDatasetsSelectedText: "Selection cannot be saved because no experiments have been selected. "+
								"To select experiments, hold down &quot;Ctrl&quot; and click on condition labels.",
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
			instructions: 'This tutorial will point out some features of the differential expression search interface.' //TODO // Click the "next" and "previous" buttons to navigate between tips and click the "X" to close the tutorial.'
		}
	},
	PhenotypeEvidenceGridPanel:{
		specificallyRelatedTT: 'Marks evidence related specifically to your phenotype search',
		negativeEvidenceTT: 'Denotes evidence against association'
	},
	PhenotypePanel:{
		setupErrorTitle: 'Error in Gemma.PhenotypePanel',
		setupErrorText:'If you are using PhenotypePanel inside of Gemma, <b>phenotypeStoreProxy</b>, <b>geneStoreProxy</b>, <br />' +
    			'<b>evidenceStoreProxy</b>, <b>geneColumnRenderer</b> and <b>createPhenotypeAssociationHandler</b><br />' +
    			' should not be set in config. Otherwise, all of them should be set.<br />' +
    			'',
    	modifyPhenotypeAssociationOutsideOfGemmaTitle: "Add new phenotype association", 
		modifyPhenotypeAssociationOutsideOfGemmaText: "To add, edit or remove gene-phenotype associatons, please go to the <a target='_blank' href='http://www.chibi.ubc.ca/Gemma/phenotypes.html'>Gemma website</a>."
	},
	PhenotypeAssociationForm: {
		ErrorMessage: {
			// The followings are from ValidateEvidenceValueObject.
		    userNotLoggedIn: 'You have not logged in. Please log in to try again.',
		    accessDenied: 'Your access is denied.',
		    lastUpdateDifferent: 'This evidence has been modified by someone else. Please reload the page to view the updated version.',
		    evidenceNotFound: 'This evidence cannot be found because it has been removed by someone else.<br />Please reload the page to view the latest phenotype association.',
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
		noGroupsToShareWith: 'You cannot share this entity because you do not belong to any user groups.'+
					'<br>Would you like to <a href="/Gemma/manageGroups.html">create one</a>? ',
		publicWarning: 'Please note: setting the read permissions to "public" means that all ' +
				'users of Gemma (registered and anonymous) will be able to view this entity and use it in analyses.'
	},
	Tutorial:{
		ControlPanel: {
			instructions: 'This tutorial will point out some features of this page. Click the "next" and "previous" buttons to navigate between tips and click the "X" to close the tutorial.'
		}
	},
	VisualizationWithThumbsPanel: {
		browserWarning: "Plots use a feature of HTML 5 that runs in IE via emulation unless you have Chrome Frame installed. "+
					"Firefox, Chrome, Safari and Opera will be faster too.'>"+
					"Too slow in Explorer? Try <a href='http://www.google.com/chromeframe/"
	}
};

	/* where would this go? (from AuditTrailGrid.js), could be useful elsewhere
	 * AddAuditEventDialog:{
		types: {
			CommentedEvent: 'Comment',
			TroubleStatusFlagEvent: 'Other (generic) Trouble',
			ExperimentalDesignTrouble: 'Experimental Design Trouble',
			OutlierSampleTrouble: 'Outlier sample',
			OKStatusFlagEvent: 'OK flag (clear Trouble)',
			ValidatedFlagEvent: 'Validated flag',
			ValidatedQualityControl: 'QC validated',
			ValidatedAnnotations: 'Tags validated',
			ValidatedExperimentalDesign: 'Experimental design validated'
		}
	},*/
