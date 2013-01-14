/**
 * It displays evidence linked to the current phenotypes and gene. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

// evidenceStoreProxy should be overridden if used outside of Gemma. 
Gemma.PhenotypeEvidenceGridPanel = Ext.extend(Ext.grid.GridPanel, {
	storeAutoLoad: false,
	storeSortInfo: { field: 'containQueryPhenotype', direction: 'DESC' },
	evidenceStoreProxy: null,
	evidencePhenotypeColumnRenderer: null,
	displayPhenotypeAsLink: false, // It will be ignored if evidencePhenotypeColumnRenderer is specified. 
	allowCreateOnlyWhenGeneSpecified: true,
	displayEvidenceCodeFullName: false,
	title: 'Evidence',
    autoScroll: true,
    stripeRows: true,
	loadMask: true,
	disableSelection: true,
    viewConfig: {
        forceFit: true,
		deferEmptyText: false,
		emptyText: 'No gene has been selected.'
    },
	hasRelevanceColumn: true,
	extraColumns: null,
    currentPhenotypes: null,
    currentGene: null,
	deferLoadToRender: false,
	showDialogViewBibliographicReferenceOutsideOfGemma: function() {
		Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.viewBibliographicReferenceOutsideOfGemmaTitle,
			Gemma.HelpText.WidgetDefaults.PhenotypePanel.viewBibliographicReferenceOutsideOfGemmaText);
	},
	hasUserLoggedIn: function() {
		return Ext.get("hasUser") != null && Ext.get("hasUser").getValue();
	},
	hasAdminLoggedIn: function() {
		return Ext.get("hasAdmin") != null && Ext.get("hasAdmin").getValue();
	},
    initComponent: function() {
		var RELEVANCE_COLUMNS_START_INDEX = 1;

   		var DEFAULT_TITLE = this.title; // A constant title that will be used when we don't have current gene.
   		
		var metaAnalysisUtilities = new Gemma.MetaAnalysisUtilities();
		var DEFAULT_THRESHOLD = metaAnalysisUtilities.getDefaultThreshold();

   		var phenotypeAssociationFormWindow;

		var metaAnalysisCache = [];
   		
   		var loggedInColumns = [
   			{ columnId: 'owner', isAdminColumn: true },
   			{ columnId: 'lastUpdated', isAdminColumn: false },
   			{ columnId: 'adminLinks', isAdminColumn: false }
   		];
   		var setColumnsVisible = function(isAdmin, isVisible) {
			var columnModel = this.getColumnModel();

			Ext.each(loggedInColumns, function(column, index) {
				if ((!isVisible) ||
					(column.isAdminColumn && isAdmin) ||
				    (!column.isAdminColumn)) {
					columnModel.setHidden(columnModel.getIndexById(column.columnId), !isVisible);
				} else {
					columnModel.setHidden(columnModel.getIndexById(column.columnId), true);
				}
			});
   		}.createDelegate(this);
   		
		if (!Gemma.isRunningOutsideOfGemma()) {   		
	   		// Show Admin column after user logs in. 
			Gemma.Application.currentUser.on("logIn", 
				function(userName, isAdmin) {
					setColumnsVisible(isAdmin, true);
				},
				this);
			   		
	   		// Hide Admin column after user logs out. 
			Gemma.Application.currentUser.on("logOut", 
				function() {	
					setColumnsVisible(false, false);					
				},
				this);
		}
		
		var generateLink = function(methodWithArguments, imageSrc, description, width, height) {
			return '<span class="link" onClick="return Ext.getCmp(\'' + this.getId() + '\').' + methodWithArguments +
						'"><img src="' + imageSrc + '" alt="' + description + '" ext:qtip="' + description + '" ' +
						((width && height) ?
							'width="' + width + '" height="' + height + '" ' :
							'') +
						'/></span>';
			
		}.createDelegate(this);
   		
		var generatePublicationLinks = function(pudmedId, pubmedUrl) {
			var anchor = '';
			if (pudmedId != null) {
				var imageSrc = '/Gemma/images/icons/magnifier.png';
				var size = 12;
				
				if (Gemma.isRunningOutsideOfGemma()) {
					anchor += generateLink('showDialogViewBibliographicReferenceOutsideOfGemma();', imageSrc, 'View Bibliographic Reference', size, size);
				} else {
					var description = 'Go to Bibliographic Reference (in new window)';
					anchor += '<a target="_blank" href="/Gemma/bibRef/searchBibRefs.html?pubmedID=' +
			        	pudmedId +
			        	'"><img src="' + imageSrc + '" alt="' + description + '" ext:qtip="' + description + '" width="' + size + '" height="' + size + '" /></a>';
				}		        	
			}
		    return anchor + (new Ext.Template( Gemma.Common.tpl.pubmedLink.simple )).apply({
		    	pubmedURL: pubmedUrl
		    });
		}.createDelegate(this);
		
		var convertToExternalDatabaseAnchor = function(databaseName, url, useDatabaseIcon) {
			var html = '<a target="_blank" href="' + url + '">';
			if (useDatabaseIcon) {
				html += '<img ext:qtip="Go to ' + databaseName + ' (in new window)" ' + 
							'src="/Gemma/images/logo/' + databaseName + '.gif" alt="' + databaseName + '" />';		
			} else {
				html += url;
			}
			
			html += '</a>';
			
			return html;
		};
		
		var showPhenotypeAssociationFormWindow = function(action, data) {
			if (!phenotypeAssociationFormWindow || (phenotypeAssociationFormWindow && phenotypeAssociationFormWindow.isDestroyed)) {
				phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
				this.relayEvents(phenotypeAssociationFormWindow, ['phenotypeAssociationChanged']);
			}

			phenotypeAssociationFormWindow.showWindow(action, data);
		}.createDelegate(this);

    	var createPhenotypeAssociationButton = new Ext.Button({
			disabled: this.allowCreateOnlyWhenGeneSpecified ?
				(this.currentGene == null) :
				false,
			handler: Gemma.isRunningOutsideOfGemma() ?
				function() {
					Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.modifyPhenotypeAssociationOutsideOfGemmaTitle,
						Gemma.HelpText.WidgetDefaults.PhenotypePanel.modifyPhenotypeAssociationOutsideOfGemmaText);
				} :
				function() {
					showPhenotypeAssociationFormWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE,
						{
							gene: this.currentGene,
							phenotypes: this.currentPhenotypes
						});
				},
			scope: this,
			icon: "/Gemma/images/icons/add.png",
			tooltip: "Add new phenotype association"
    	});
		   
    	
		MyRowExpander = Ext.extend(Ext.grid.RowExpander, {
			getRowClass: function(record, index, rowParams, store) {
				if (record.data.homologueEvidence) {	
					rowParams.tstyle += 'color: gray;';
				} else {
					rowParams.tstyle += 'color: black;';
				}
				
				return this.superclass().getRowClass.call(this, record, index, rowParams, store);
			},
			enableCaching: false, // It needs to be false. Otherwise, its content will not be updated after grid's data is changed.
			lazyRender: false, // It needs to be false. Otherwise, after grid's data is changed, all rows will be collapsed even though the expand button ("+" button) shows the correct state.
			// Use class="x-grid3-cell-inner" so that we have padding around the description.
		    tpl: new Ext.Template(
		        '<div class="x-grid3-cell-inner" style="white-space: normal;">{rowExpanderText}</div>'
		    )
		});
		var rowExpander = new MyRowExpander();    	
    	
		if (this.evidencePhenotypeColumnRenderer == null) {
			this.evidencePhenotypeColumnRenderer = {
				fn: function(value, metadata, record, rowIndex, colIndex, store) {
					var phenotypesHtml = '';
					for (var i = 0; i < value.length; i++) {
						if (this.displayPhenotypeAsLink) {
							phenotypesHtml += String.format('<a target="_blank" href="/Gemma/phenotypes.html?phenotypeUrlId={0}&geneId={2}" ext:qtip="Go to Phenotype Page (in new window)">{1}</a>',
								value[i].urlId, value[i].value, record.data.geneId);
						} else {
							if (value[i].child || value[i].root) {					
								phenotypesHtml += '<span style="font-weight: bold; color: red;">' +
														value[i].value +
												  '</span>';
							} else {
								phenotypesHtml += value[i].value;
							}
						}
					
						phenotypesHtml += '<br />';
					}					
					return phenotypesHtml;
				},
				scope: this
			};
		}

		var getGeneLink = this.getGeneLink;
		
		var getFormWindowData = function(id) {
			var record = this.getStore().getById(id);
			var evidenceClassName = record.data.className; 
		
			var data = null;
			
			if (evidenceClassName === 'LiteratureEvidenceValueObject') {
				data = {
					pubMedId: record.data.citationValueObject.pubmedAccession
				};
			} else if (evidenceClassName === 'ExperimentalEvidenceValueObject') {
				data = {
					primaryPubMedId: record.data.primaryPublicationCitationValueObject != null ?
										record.data.primaryPublicationCitationValueObject.pubmedAccession :
										null,
					// Assume we have at most one other PubMed Id.
					secondaryPubMedId: record.data.relevantPublicationsCitationValueObjects != null &&
								 	   record.data.relevantPublicationsCitationValueObjects.length > 0 ?
											record.data.relevantPublicationsCitationValueObjects[0].pubmedAccession :
											null,
					experimentCharacteristics: record.data.experimentCharacteristics
				};
			}
			
			if (data != null) {
				data.evidenceId = record.data.id;
				data.gene = {
					id: record.data.geneId,
					ncbiId: record.data.geneNCBI,
					officialSymbol: record.data.geneOfficialSymbol,
					officialName: record.data.geneOfficialName,
					taxonCommonName: record.data.taxonCommonName,
					// As of 2012-06-27, experiment tag's value combo box depends on taxon id.
					// Elodie and Nicolas said we did not have to set it. So, it is set to null.
					taxonId: null  
				};
				data.phenotypes = record.data.phenotypes;
				data.evidenceClassName = evidenceClassName;
				data.isNegativeEvidence = record.data.isNegativeEvidence;
				data.description = record.data.description;
				data.evidenceCode = record.data.evidenceCode;
				data.lastUpdated = record.data.lastUpdated;
			}
			
			return data;
		}.createDelegate(this);	

		var showLoadMask = function(msg) {
			if (!this.loadMask) {
				this.loadMask = new Ext.LoadMask(this.getEl());
			}
			this.loadMask.msg = msg 
				? msg 
				: "Loading ...";
				
			this.loadMask.show();
		}.createDelegate(this);
		
		var hideLoadMask = function() {
			this.loadMask.hide();
		}.createDelegate(this);
		
		var showMetaAnalysisWindow = function(metaAnalysis, analysisName, numGenesAnalyzed) {
			metaAnalysis.name = analysisName;
			metaAnalysis.numGenesAnalyzed = numGenesAnalyzed;

			var viewMetaAnalysisWindow = new Gemma.MetaAnalysisWindow({
				title: 'View Meta-analysis for ' + analysisName,
				metaAnalysis: metaAnalysis,
				defaultQvalueThreshold: DEFAULT_THRESHOLD
			});  
			viewMetaAnalysisWindow.show();
		};

		var showViewEvidenceWindow = function(metaAnalysis, storeId, metaAnalysisId) {
			var record = this.getStore().getById(storeId);
			if (record != null) {
				metaAnalysis.name = record.data.geneDifferentialExpressionMetaAnalysisSummaryValueObject.name;
				metaAnalysis.numGenesAnalyzed = record.data.geneDifferentialExpressionMetaAnalysisSummaryValueObject.numGenesAnalyzed;
	
				var viewEvidenceWindow = new Gemma.MetaAnalysisEvidenceWindow({
					metaAnalysisId: metaAnalysisId,
					metaAnalysis: metaAnalysis,
					showActionButton: record.data.evidenceSecurityValueObject.currentUserHasWritePermission,				
					title: 'View Neurocarta evidence for ' + record.data.geneDifferentialExpressionMetaAnalysisSummaryValueObject.name,
					diffExpressionEvidence: record.data.geneDifferentialExpressionMetaAnalysisSummaryValueObject.diffExpressionEvidence,
					modal: false,
					listeners: {
						evidenceRemoved: function() {
							this.store.reload();
							this.fireEvent('phenotypeAssociationChanged');
						},
						scope: this
					}
				});
				viewEvidenceWindow.show();
			}
		};

		var processMetaAnalysis = function(id, errorDialogTitle, callback, args) {
			var metaAnalysisFound = metaAnalysisCache[id];
			if (metaAnalysisFound) {
				// Put metaAnalysisFound at the beginning of args.
				args.splice(0, 0, metaAnalysisFound);

				callback.apply(this, args);
			} else {
				showLoadMask();
				DiffExMetaAnalyzerController.findDetailMetaAnalysisById(id, function(baseValueObject) {
					hideLoadMask();				
					
					if (baseValueObject.errorFound) {						
						Gemma.alertUserToError(baseValueObject,	errorDialogTitle);
					} else {
						metaAnalysisCache[id] = baseValueObject.valueObject;

						// Put metaAnalysisFound at the beginning of args.
						args.splice(0, 0, metaAnalysisCache[id]); 
						
						callback.apply(this, args);
					}			
				}.createDelegate(this));
			}
		}.createDelegate(this);

		var evidenceStore = new Ext.data.Store({
			autoLoad: this.storeAutoLoad,
			proxy: this.evidenceStoreProxy == null ?
						new Ext.data.DWRProxy({
					        apiActionToHandlerMap: {
				    	        read: {
				        	        dwrFunction: GeneController.loadGeneEvidence,
				            	    getDwrArgsFunction: function(request){
				            	    	return [
											request.params['taxonId'],
				            	    		request.params['showOnlyEditable'],
				            	    		request.params["geneId"],
					            	    	request.params["phenotypeValueUris"]
				            	    	];
					                }
				    	        }
					        }
				    	}) :
				    	this.evidenceStoreProxy,
		    reader: new Ext.data.JsonReader({
				idProperty: 'id',		    	
		        fields: [
			        // for EvidenceValueObject
					'id', 'className', 'description', 'evidenceCode', 'evidenceSecurityValueObject',
		        	'evidenceSource', 'isNegativeEvidence', 'lastUpdated', 'phenotypes', 'containQueryPhenotype',
		        	// for GroupEvidenceValueObject
		        	'literatureEvidences',
					// for DiffExpressionEvidenceValueObject
		        	'geneDifferentialExpressionMetaAnalysisSummaryValueObject', 'selectionThreshold', 'numEvidenceFromSameMetaAnalysis',
		        	// for ExperimentalEvidenceValueObject
		 			'experimentCharacteristics', 'primaryPublicationCitationValueObject',
		 			'relevantPublicationsCitationValueObjects',
		 			// for LiteratureEvidenceValueObject
		 			'citationValueObject',
					// for showing homologues' evidence
					'geneId', 'geneNCBI', 'geneOfficialSymbol', 'geneOfficialName', 'taxonCommonName', 'homologueEvidence',
		            {
						name: 'rowExpanderText',
						convert: function(value, record) {
							var descriptionHtml = '';
							
							switch (record.className) {
								case 'DiffExpressionEvidenceValueObject' :
									descriptionHtml += '<p>';

									descriptionHtml += '<b>Name</b>: ' + record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.name + ' ' +
										generateLink(
											'eval(\'processMetaAnalysis(' +
												record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.id + ', ' +
												'\\\'Cannot view meta-analysis\\\', ' +
												'showMetaAnalysisWindow, ' +
												'[ \\\'' + record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.name + '\\\', ' +
													record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.numGenesAnalyzed + ' ])\');',   
											'/Gemma/images/icons/magnifier.png', 'View included result sets and results', 10, 10) +
										' (' + record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.numResultSetsIncluded + ' result sets included; ' +
										record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.numGenesAnalyzed + ' genes analyzed)<br />';

									descriptionHtml += '<b>q-value threshold</b>: ' + record.selectionThreshold + ' (' + record.numEvidenceFromSameMetaAnalysis + ' genes ' +
										generateLink(
											'eval(\'processMetaAnalysis(' +
												record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.id + ', ' +
												'\\\'Cannot view Neurocarta evidence\\\', ' +
												'showViewEvidenceWindow, ' +
												'[ ' + record.id + ', ' + record.geneDifferentialExpressionMetaAnalysisSummaryValueObject.id + ' ])\');',   
											'/Gemma/images/icons/magnifier.png', 'View Neurocarta evidence', 10, 10) + ')<br />';

									descriptionHtml += '<b>p-value</b>: ' + record.metaPvalue.toExponential(2) + '; ' +
													   '<b>q-value</b>: ' + record.metaQvalue.toExponential(2) + '; ' +
													   '<b>Direction</b>: ' + metaAnalysisUtilities.generateDirectionHtml(record.upperTail) + '<br />';

									descriptionHtml += '</p>';
									break;
								
								case 'ExperimentalEvidenceValueObject' :
						        	if (record.primaryPublicationCitationValueObject != null) {
						        		descriptionHtml += '<p><b>Primary Publication</b>: ' +
						        			record.primaryPublicationCitationValueObject.citation + ' ' +
						        			generatePublicationLinks(record.primaryPublicationCitationValueObject.pubmedAccession,
						        				record.primaryPublicationCitationValueObject.pubmedURL) + '</p>';
						        	}
						
									var relPub = record.relevantPublicationsCitationValueObjects;
						        	if (relPub != null && relPub.length > 0) {
						        		descriptionHtml += '<p><b>Relevant Publication</b>: ';
						        		
										for (var i = 0; i < relPub.length; i++) {
											descriptionHtml += relPub[i].citation + ' ' +
												generatePublicationLinks(relPub[i].pubmedAccession, relPub[i].pubmedURL);
											
											if (i < relPub.length - 1) {
												descriptionHtml += " | ";
											}
										}
										descriptionHtml += '</p>';
						        	}
						        	
									var expChar = record.experimentCharacteristics;
						        	if (expChar != null && expChar.length > 0) {
										var expCharMap = new Object();
										for (var i = 0; i < expChar.length; i++) {
											if (expCharMap[expChar[i].category] == null) {
												expCharMap[expChar[i].category] = expChar[i].value;	
											} else {
												expCharMap[expChar[i].category] += " | " + expChar[i].value;
											}
											  
										}
						
										descriptionHtml += '<p>';
										Ext.iterate(expCharMap, function(key, value) {
										  descriptionHtml += '<b>' + key + "</b>: " + value + '<br />';
										});
										descriptionHtml += '</p>';
						        	}
									
									break;
						
								case 'GenericEvidenceValueObject' : 
									break;
						
								case 'GroupEvidenceValueObject' : 
									descriptionHtml += '<p>';
									
									for (var i = 0; i < record.literatureEvidences.length; i++) {
										descriptionHtml += '<b>Publication ' + (i + 1) + '</b>: ' +
											record.literatureEvidences[i].citationValueObject.citation + ' ' +
											generatePublicationLinks(
												record.literatureEvidences[i].citationValueObject.pubmedAccession,
												record.literatureEvidences[i].citationValueObject.pubmedURL) + '<br />';
									}
									
									descriptionHtml += '</p>';
									break;
						
								case 'LiteratureEvidenceValueObject' : 
						        	if (record.citationValueObject != null) {
						        		descriptionHtml += '<p><b>Publication</b>: ' +
						        			record.citationValueObject.citation + ' ' +
						        			generatePublicationLinks(
						        				record.citationValueObject.pubmedAccession,
						        				record.citationValueObject.pubmedURL) + '</p>';
						        	}
									break;
						
								case 'UrlEvidenceValueObject' : 
									break;
							}
							
							if (record.evidenceSource != null && record.evidenceSource.externalDatabase.name != null && record.evidenceSource.externalUrl != null) {
								var databaseName = record.evidenceSource.externalDatabase.name;
								descriptionHtml += '<p><b>Evidence Source</b>: ' + databaseName + ' ' + convertToExternalDatabaseAnchor(databaseName, record.evidenceSource.externalUrl, false) + '</p>'; 
							}
						
							if (record.description != null && record.description !== '') {
								descriptionHtml += '<p><b>Note</b>: ' + record.description + '</p>';
							}

							if (record.homologueEvidence) {
								var geneLink = getGeneLink ?
									getGeneLink(record.geneId) :
									'/Gemma/gene/showGene.html?id=' + record.geneId;
								
								descriptionHtml += String.format("<p><b>*</b> Inferred from homology with the {0} gene {1} " +
									"<a target='_blank' href='" + geneLink + "' ext:qtip='Go to {1} Details (in new window)'>" +
										"<img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/>" + 
									"</a></p>",
								record.taxonCommonName, record.geneOfficialSymbol);
							}
							
							return descriptionHtml;
						}
		            }
		        ]
		    }),
			sortInfo: this.storeSortInfo
		});

		var columns = [
			rowExpander,
			{
				header: "Phenotypes",
				dataIndex: 'phenotypes',
				width: 0.35,
				renderer: this.evidencePhenotypeColumnRenderer,					
				sortable: false
			},
			{
				header: "Type",
				dataIndex: 'className',
				width: 1,
				renderer: function(value, metadata, record, rowIndex, colIndex, store) {
					var externalSource = '';
				
					if (record.data.evidenceSource != null && record.data.evidenceSource.externalDatabase != null) {
						externalSource = 'External Source [' + record.data.evidenceSource.externalDatabase.name + ']';
					}
					
					var typeColumnHtml = '';
					
					switch (value) {
						case 'DiffExpressionEvidenceValueObject' :
							typeColumnHtml = 'Differential Expression Meta-analysis';
							break;
						
						case 'ExperimentalEvidenceValueObject' :
							var experimentValues = '';
							var experimentCharacteristics = record.data.experimentCharacteristics;
							
				        	if (experimentCharacteristics != null) {
								for (var i = 0; i < experimentCharacteristics.length; i++) {
									if (experimentCharacteristics[i].category == 'Experiment') {
										experimentValues += experimentCharacteristics[i].value + " | "; 
									}
								}
								if (experimentValues.length > 0) {
									experimentValues = ' [ ' + experimentValues.substr(0, experimentValues.length - 3) + ' ]'; // remove ' | ' at the end
								}
				        	}
						
				        	typeColumnHtml = 'Experimental' + experimentValues;
							break;
				
						case 'GenericEvidenceValueObject' :
							if (record.data.evidenceSource == null) {
								typeColumnHtml = 'Note';
							} else {
								typeColumnHtml = externalSource;
							}
							break;
				
						case 'GroupEvidenceValueObject' : 
							typeColumnHtml = '<b>' + record.data.literatureEvidences.length + 'x</b> Literature';
							break;

						case 'LiteratureEvidenceValueObject' : 
							typeColumnHtml = 'Literature';
							break;
				
						case 'UrlEvidenceValueObject' : 
							typeColumnHtml = 'Url';
							break;
					}
				
					if (value !== 'GenericEvidenceValueObject' && externalSource !== '') {
						typeColumnHtml += ' from ' + externalSource;
					}
				
					typeColumnHtml = 
						(record.data.isNegativeEvidence ? 
							"<img ext:qwidth='200' ext:qtip='" +
								Gemma.HelpText.WidgetDefaults.PhenotypeEvidenceGridPanel.negativeEvidenceTT+
								"' src='/Gemma/images/icons/thumbsdown.png' height='12'/> " :
							"") +
						typeColumnHtml;
					
					return '<span style="white-space: normal;">' + typeColumnHtml +'</span>';		
				},
				sortable: true
			},
			{
				header: "Evidence Code",
				dataIndex: 'evidenceCode',
				width: 0.33,
	            renderer: {
	            	fn: function(value, metadata, record, rowIndex, colIndex, store) {
						var columnRenderer;
						if (record.data.homologueEvidence) {
							columnRenderer = '<span ext:qwidth="200" ext:qtip="' + 
								'Inferred from homology with the ' + record.data.taxonCommonName + ' gene ' + record.data.geneOfficialSymbol + '.' + 
								'">' +
								'Inferred from ' + record.data.geneOfficialSymbol + ' [' + record.data.taxonCommonName + ']' + '</span>';
						} else {
							var evidenceCodeInfo = Gemma.EvidenceCodeInfo[value];
							var qtipInfo = Gemma.EvidenceCodeInfo.getQtipInfo(value, evidenceCodeInfo);
		            	
							columnRenderer = '<span ext:qwidth="' + qtipInfo.width + '" ext:qtip="' + qtipInfo.text + '">' +
								(this.displayEvidenceCodeFullName ?
									evidenceCodeInfo.name : 
									value) +
								'</span>';
						}
						return columnRenderer;
	            	},
	            	scope: this
	            },
				sortable: true
			},
			{
				header: "Link Out",
				dataIndex: 'evidenceSource',
				width: 0.2,
				renderer: function(value, metadata, record, rowIndex, colIndex, store) {
					var linkOutHtml = '';
				
					switch (record.data.className) {
						case 'DiffExpressionEvidenceValueObject' :
							break;
						
						case 'ExperimentalEvidenceValueObject' :
				        	if (record.data.primaryPublicationCitationValueObject != null) {
								linkOutHtml += generatePublicationLinks(null,
									record.data.primaryPublicationCitationValueObject.pubmedURL);
							}
							break;
				
						case 'GenericEvidenceValueObject' : 
							break;
				
						case 'GroupEvidenceValueObject' :
							var pubmedURL = record.data.literatureEvidences[0].citationValueObject.pubmedURL;
							for (var i = 1; i < record.data.literatureEvidences.length; i++) {
								pubmedURL += ',' + record.data.literatureEvidences[i].citationValueObject.pubmedAccession;
							}
							
							linkOutHtml += generatePublicationLinks(null, pubmedURL);
							break;
				
						case 'LiteratureEvidenceValueObject' : 
				        	if (record.data.citationValueObject != null) {
				        		linkOutHtml += generatePublicationLinks(null,
				        			record.data.citationValueObject.pubmedURL);
				        	}
							break;
				
						case 'UrlEvidenceValueObject' : 
							break;
					}
				
					if (value != null && value.externalDatabase.name != null && value.externalUrl != null) {
						if (linkOutHtml !== '') {
							linkOutHtml += ' ';
						}
						linkOutHtml += convertToExternalDatabaseAnchor(value.externalDatabase.name, value.externalUrl, true);
					}
					
					return '<span style="white-space: normal;">' + linkOutHtml +'</span>';	
				},	
				sortable: false
			},
			{ 
				header: 'Owner',
				id: 'owner', // Used by my function setColumnsVisible() to locate this column
				dataIndex: 'evidenceSecurityValueObject',
				width: 0.15,
				renderer: function(value, metadata, record, rowIndex, colIndex, store) {
					return value.owner;					
				},
				hidden: !this.hasAdminLoggedIn(),
				sortable: true
			},
			{ 
				header: 'Updated',
				id: 'lastUpdated', // Used by my function setColumnsVisible() to locate this column
				dataIndex: 'lastUpdated',
				width: 0.15,
				renderer: function(value, metadata, record, rowIndex, colIndex, store) {
					return new Date(value).format("y/M/d");
				},
				hidden: !this.hasUserLoggedIn(),
				sortable: true
			},
			{
				header: 'Admin',
				id: 'adminLinks', // Used by my function setColumnsVisible() to locate this column
				width: 0.3,
	            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
	            	var adminLinks = '';
	            	
	            	// Don't display anything if this column is hidden because users can still show this column manually.
					if (!this.hidden && record.data.className !== 'DiffExpressionEvidenceValueObject') {		            	
						adminLinks += Gemma.SecurityManager.getSecurityLink(
							'ubic.gemma.model.association.phenotype.PhenotypeAssociationImpl',
							record.data.id,
							record.data.evidenceSecurityValueObject.public,
							record.data.evidenceSecurityValueObject.shared,
							record.data.evidenceSecurityValueObject.currentUserIsOwner,
							null,
							null,
							'Phenotype Association', // Evidence name for the title in Security dialog.
							record.data.evidenceSecurityValueObject.currentUserIsOwner); 

						if ((record.data.className === 'LiteratureEvidenceValueObject' ||						
						 	 record.data.className === 'ExperimentalEvidenceValueObject') &&
	            		    record.data.evidenceSecurityValueObject.currentUserHasWritePermission &&
							record.data.evidenceSource == null) {
		            		adminLinks += ' ' +
								generateLink('showCreateWindow(' + record.data.id + ');', '/Gemma/images/icons/add.png', 'Clone evidence') + ' ' +			            		
            					generateLink('showEditWindow(' + record.data.id + ');', '/Gemma/images/icons/pencil.png', 'Edit evidence') + ' ' +
								generateLink('removeEvidence(' + record.data.id + ');', '/Gemma/images/icons/cross.png', 'Remove evidence');
	            		}
	            	}
	            	
					return adminLinks;
	            },
				hidden: !this.hasUserLoggedIn(),
				sortable: true
			}
		];

		if (this.hasRelevanceColumn) {
			columns.splice(RELEVANCE_COLUMNS_START_INDEX, 0, 
				{
					header: '<img style="vertical-align: bottom;" ext:qwidth="198" ext:qtip="'+
							Gemma.HelpText.WidgetDefaults.PhenotypeEvidenceGridPanel.specificallyRelatedTT+
							'" width="16" height="16" src="/Gemma/images/icons/bullet_red.png">',
					dataIndex: 'containQueryPhenotype',
					width: 0.12,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						if (value == true) {
							return this.header;
						} else {
							return '';
						}
		            },
		            hidden: !this.hasRelevanceColumn,
					sortable: true
				}
			);
		}

		if (this.extraColumns) {
			Ext.each(this.extraColumns, function(columnInfo, columnInfoIndex) {
				Ext.each(columnInfo.columns, function(column, columnIndex) {
					// The number of items to be removed is 0.
					columns.splice(columnInfo.startIndex + columnIndex, 0, column);
				});
			});
		}

		Ext.apply(this, {
			store: evidenceStore,
			plugins: rowExpander,
			columns: columns,
			tbar: [
				createPhenotypeAssociationButton
			],
			eval: function(request) {
				eval(request);
			},			
		    setCurrentData: function(currentFilters, currentPhenotypes, currentGene) {
		    	this.currentPhenotypes = currentPhenotypes;

				createPhenotypeAssociationButton.setDisabled(currentGene == null);
		    	
				if (currentGene != null) {
					this.setTitle("Evidence for " + currentGene.officialSymbol);
					
					this.currentGene = currentGene;
			    	
					var phenotypeValueUris = [];
					for (var i = 0; i < currentPhenotypes.length; i++) {
						phenotypeValueUris.push(currentPhenotypes[i].valueUri);
					}
								    	
					evidenceStore.reload({
						params: {
							taxonId: currentFilters.taxonId,
							showOnlyEditable: currentFilters.showOnlyEditable,
							geneId: currentGene.id,
							phenotypeValueUris: phenotypeValueUris		    			
						}
					});
				} else {
					this.currentGene = null;

					this.setTitle(DEFAULT_TITLE);					
					evidenceStore.removeAll();
				}                	
		    },
			loadGene: function(geneId) {
				// Defer the call. Otherwise, the loading mask does not show.
				Ext.defer(
					function() {
				    	evidenceStore.reload({
				    		params: {
								taxonId: null,
								showOnlyEditable: false,
				    			geneId: geneId
				    		}
				    	});
					},
					1,
					this);
			},
			showCreateWindow: function(id) {			
				var data = getFormWindowData(id);
			
				if (data != null) {
					// Because we are creating new evidence, they should be null.
					data.evidenceId = null;
					data.lastUpdated = null;
					
					showPhenotypeAssociationFormWindow(Gemma.PhenotypeAssociationForm.ACTION_CREATE, data);
				}
			},			
			showEditWindow: function(id) {			
				var data = getFormWindowData(id);
			
				if (data != null) {
					showPhenotypeAssociationFormWindow(Gemma.PhenotypeAssociationForm.ACTION_EDIT, data);
				}
			},			
			removeEvidence: function(id) {
				Ext.MessageBox.confirm('Confirm',
					'Are you sure you want to remove this evidence?',
					function(button) {
						if (button === 'yes') {
							PhenotypeController.removePhenotypeAssociation(id, function(validateEvidenceValueObject) {
								if (validateEvidenceValueObject == null) {
									this.fireEvent('phenotypeAssociationChanged');
								} else {
									if (validateEvidenceValueObject.evidenceNotFound) {
										// We still need to fire event to let listeners know that it has been removed.
										this.fireEvent('phenotypeAssociationChanged');
										Ext.Msg.alert('Evidence already removed', 'This evidence has already been removed by someone else.');
									} else {
										Ext.Msg.alert('Cannot remove evidence', Gemma.convertToEvidenceError(validateEvidenceValueObject).errorMessage,
											function() {
												if (validateEvidenceValueObject.userNotLoggedIn) {
													Gemma.AjaxLogin.showLoginWindowFn();
												}
											}
										);
									}
								}
							}.createDelegate(this));
						}
					},
					this);
			}			
		});

		Gemma.PhenotypeEvidenceGridPanel.superclass.initComponent.call(this);		
		
		if (!this.deferLoadToRender) {
			if (this.currentGene != null && this.currentGene.id != '') {
				this.loadGene(this.currentGene.id);
			}
			
		} else {
			this.on('render', function(){
				if (this.currentGene != null && this.currentGene.id != '') {
					this.loadGene(this.currentGene.id);
				}
			});
		}
    }
});