
Ext.namespace('Gemma.HelpText','Gemma.StatusText','Gemma.Widget');

// TT = tooltip

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
	Searching: {
		generic: "Searching...",
		analysisResults: "Searching for analysis results..."
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
		text: "Please provide a previously unused name for the set"
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
		title: "Cannot save an empty set",
		text: "You cannot save an empty set. No changes have been saved.<br>"+
				" Add experiments to set {0} or delete it."
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
		refreshLayoutText: 'Refresh Layout',
		refreshLayoutTT: 'Refresh the layout of the graph',
		nodeDegreeEmphasisText: 'Node Degree Emphasis',
		nodeDegreeEmphasisTT: 'Node degree is represented by the darkness of a gene node. The closer the node degree of a gene is'+
								' to 1, the lighter it is and the more that gene shows coexpression with all other genes in that taxon',
		lowStringencyWarning: 'Lowering the stringency to this level will run a new search to retrieve low stringency results. '+
							'Low stringency results may not be meaningful for the number of datasets your are searching in. Click Yes to Continue.',
		exportPNGWindowTitle: 'Right-click the image and save the image as file.',
		searchStatusTitle: 'Status of Search',
		searchStatusTooMany: 'Too Many Genes Selected. Max number of selected genes is {0}',
		searchStatusTooManyReduce: 'Too many Query Genes. A max of {0} query genes allowed. Click Yes to continue search with reduced query genes',
		searchStatusTooFew: 'No Genes Selected',
		searchStatusNoMoreResults: 'No more results found for this gene',
		
	},
	DatasetGroupEditor:{
		widgetTT: "Use this tool to create and edit groups of datasets. "+
				"You can modify a built-in set by making a copy (clone) and editing the copy",
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
		deleteFactorValueWarningTitle: "Deleting Factor(s)",
		deleteFactorValueWarningText: 'Are you sure? This cannot be undone. Any associated differential expression analyses will be deleted as well.'
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
		helpTT: "Below is a sampling of expression profile pictures from the allen brain atlas. Beside is a link to the allen brain atlas"
	},
	GeneDetails:{
		probesTT: 'Number of probes for this gene on expression platforms in Gemma'
	},
	GeneImportPanel:{
		instructions: "Type or paste in gene symbols, one per line, up to {0}. (Note that searches are "+
							"limited to a smaller number of genes.)"
	},
	GeneMembersSaveGrid:{
		saveAsTT: 'Save your selection as a new set.',
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
	MetaheatmapApplication:{
		noGenesSelectedTitle: "No Genes Selected",
		noGenesSelectedText: "Selection cannot be saved because no genes have been selected. "+
								"To select genes, hold down &quot;Ctrl&quot; and click on gene symbols.",
		noDatasetsSelectedTitle: "No Experiments Selected",
		noDatasetsSelectedText: "Selection cannot be saved because no experiments have been selected. "+
								"To select experiments, hold down &quot;Ctrl&quot; and click on condition labels."
	},
	PhenotypeEvidenceGridPanel:{
		specificallyRelatedTT: 'The red dot marks evidence related specifically to your phenotype search.',
		negativeEvidenceTT: 'This negative sign denotes evidence for a negative association.',
		EvidenceCodes: {
			expText: 'Inferred from Experiment',
			expTT: 'An experimental assay has been located in the cited reference, whose results indicate a gene association (or non-association) to a phenotype.',
			icText: 'Inferred by Curator',
			icTT: 'The association between the gene and phenotype is not supported by any direct evidence, but can be reasonably inferred by a curator. This includes annotations from animal models or cell cultures.',
			tasText: 'Traceable Author Statement',
			tasTT: 'The gene-to-phenotype association is stated in a review paper or a website (external database) with a reference to the original publication.'
		}
	},
	PhenotypePanel:{
		setupErrorTitle: 'Error in Gemma.PhenotypePanel',
		setupErrorText:'If you are using PhenotypePanel inside of Gemma,<br />' +
    			'<b>phenotypeStoreProxy</b>, <b>geneStoreProxy</b> and<br />' +
    			'<b>geneColumnRenderer</b> should not be set <br />' +
    			'in config. Otherwise, all of them should be set.',
		
	},
	PhenotypePanelSearchField:{
		setupErrorTitle: 'Error in Gemma.PhenotypePanelSearchField',
		setupErrorText: 'You should set all these configs: <b>getSelectionModel</b>, <b>getStore</b>, <b>filterFields</b> and <b>emptyText</b>.'
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

Gemma.Widget.tpl = {
	ArrayDesignsNonPagingGrid: {
		rowDetails: '<p>Probes: <b>{designElementCount}</b></p>' +
			'<p>With sequences: <b>{numProbeSequences}</b> <span style="color:grey">(Number of probes with sequences)</span></p>' +
			'<p>With align: <b>{numProbeAlignments}</b> <span style="color:grey">(Number of probes with at least one genome alignment)</span></p>' +
			'<p>Mapped to genes: <b>{numProbesToKnownGenes}</b> <span style="color:grey">(Number of probes mapped to known genes '+
					'(including predicted and anonymous locations))</span></p>' +
			'<p>Unique genes: <b>{numGenes}</b> <span style="color:grey">(Number of unique genes represented on the array)</span></p>' +
			'<p> (as of {dateCached})</p>'
	}
};