/**
 * It displays evidence linked to the current phenotypes and gene. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

// Note: If you set hasStoreProxy to true and keep storeProxy as null, a default storeProxy will be created.
Gemma.PhenotypeEvidenceGridPanel = Ext.extend(Ext.grid.GridPanel, {
	title: 'Evidence',
    autoScroll: true,
    stripeRows: true,
	loadMask: true,
	disableSelection: true,
    viewConfig: {
        forceFit: true
    },
	hasStoreProxy: false, 
	storeProxy: null,
	hasRelevanceColumn: true,
    currentPhenotypes: null,
    currentGene: null,
	deferLoadToRender: false,
    initComponent: function() {
   		var DEFAULT_TITLE = this.title; // A constant title that will be used when we don't have current gene.

		var convertToPudmedAnchor = function(pudmedUrl) {
		    return '<a target="_blank" href="' +
		        pudmedUrl +
		        '"><img ext:qtip="Go to PubMed (in new window)" ' + 
		        'src="/Gemma/images/pubmed.gif" width="47" height="15" alt="PubMed" /></a>';
		};
		
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

    	var createPhenotypeAssociationButton = new Ext.Button({
			disabled: (this.currentGene == null),
			handler: function() {
				var phenotypeAssociationFormWindow = new Gemma.PhenotypeAssociationForm.Window();
				phenotypeAssociationFormWindow.showWindow(this.currentPhenotypes, this.currentGene);
			},
			scope: this,
			icon: "/Gemma/images/icons/add.png",
			tooltip: "Add new phenotype association"
    	});
		    	
		var rowExpander = new Ext.grid.RowExpander({
			// Use class="x-grid3-cell-inner" so that we have padding around the description.
		    tpl: new Ext.Template(
		        '<div class="x-grid3-cell-inner" style="white-space: normal;">{description}</div>'
		    )
		});

		if (this.evidencePhenotypeColumnRenderer == null) {
			this.evidencePhenotypeColumnRenderer = function(value, metadata, record, rowIndex, colIndex, store) {
				var phenotypesHtml = '';
				for (var i = 0; i < value.length; i++) {
					phenotypesHtml += value[i].value + '<br />';
				}					
				return phenotypesHtml;
		    }
		}
			
		var evidenceStore = new Ext.data.Store({
			proxy: this.hasStoreProxy ?
				(this.storeProxy == null ?
					new Ext.data.DWRProxy({
				        apiActionToHandlerMap: {
			    	        read: {
			        	        dwrFunction: GeneController.loadGeneEvidence,
			            	    getDwrArgsFunction: function(request){
			            	    	return [request.params["geneId"]];
				                }
			    	        }
				        }
			    	}) :
			    	this.storeProxy) :
			    null,
		    reader: new Ext.data.JsonReader({
		        fields: [
		        	'relevance', 'phenotypes', 'className', 'evidenceCode', 'evidenceSource', 'experimentCharacteristics',
		 			 'isNegativeEvidence', 'primaryPublicationCitationValueObject', 'citationValueObject',
		            {
						name: 'description',
						convert: function(value, record) {
							var descriptionHtml = '';
							
							switch (record.className) {
								case 'DiffExpressionEvidenceValueObject' :
									break;
								
								case 'ExperimentalEvidenceValueObject' :
									var descriptionHtml = '';
						
						        	if (record.primaryPublicationCitationValueObject != null) {
						        		descriptionHtml += '<p><b>Primary Publication</b>: ' +
						        			record.primaryPublicationCitationValueObject.citation + ' ' +
						        			convertToPudmedAnchor(record.primaryPublicationCitationValueObject.pubmedURL) + '</p>';
						        	}
						
									var relPub = record.relevantPublicationsValueObjects;
						        	if (relPub != null && relPub.length > 0) {
						        		descriptionHtml += '<p><b>Relevant Publication</b>: ';
						        		
										for (var i = 0; i < relPub.length; i++) {
											descriptionHtml += relPub[i].citation + ' ' + convertToPudmedAnchor(relPub[i].pubmedURL);
											
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
						
								case 'LiteratureEvidenceValueObject' : 
						        	if (record.citationValueObject != null) {
						        		descriptionHtml += '<p><b>Publication</b>: ' +
						        			record.citationValueObject.citation + ' ' +
						        			convertToPudmedAnchor(record.citationValueObject.pubmedURL) + '</p>';
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
							
							return descriptionHtml;
						}	
		            }
		        ]
		    }),
			sortInfo: {	field: 'relevance', direction: 'DESC' }
		});

		Ext.apply(this, {
			store: evidenceStore,
			plugins: rowExpander,
			columns:[
				rowExpander,
				{
					header: '<img style="vertical-align: bottom;" ext:qwidth="198" ext:qtip="'+
							Gemma.HelpText.WidgetDefaults.PhenotypeEvidenceGridPanel.specificallyRelatedTT+
							'" width="16" height="16" src="/Gemma/images/icons/bullet_red.png">',
					dataIndex: 'relevance',
					width: 0.12,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						if (value == 1) {
							return this.header;
						} else {
							return '';
						}
		            },
		            hidden: !this.hasRelevanceColumn,
					sortable: true
				},
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
								typeColumnHtml = 'Differential Expression';
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
					
						return (record.data.isNegativeEvidence ? 
									"<img ext:qwidth='200' ext:qtip='" +
										Gemma.HelpText.WidgetDefaults.PhenotypeEvidenceGridPanel.negativeEvidenceTT+
										"' src='/Gemma/images/icons/thumbsdown.png' height='12'/> " :
									"") +
								typeColumnHtml;						
					},
					sortable: true
				},
				{
					header: "Evidence Code",
					dataIndex: 'evidenceCode',
					width: 0.33,
		            renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						var evidenceCode = Gemma.decodeEvidenceCode(value);						
		            	
						return '<span ext:qwidth="200" ext:qtip="' + evidenceCode.tooltipText + '">' + evidenceCode.displayText + '</span>';
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
									linkOutHtml += convertToPudmedAnchor(record.data.primaryPublicationCitationValueObject.pubmedURL);
								}
								break;
					
							case 'GenericEvidenceValueObject' : 
								break;
					
							case 'LiteratureEvidenceValueObject' : 
					        	if (record.data.citationValueObject != null) {
					        		linkOutHtml += convertToPudmedAnchor(record.data.citationValueObject.pubmedURL);
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
						
						return '<span style="white-space: normal;">' + linkOutHtml +'</span>'					
					},	
					sortable: false
				}
			],
// TODO: The following codes have been commented out because the new feature "create phenotype association" is still being implemented.
//			tbar: [
//				createPhenotypeAssociationButton
//			],
		    setCurrentData: function(currentPhenotypes, currentGene, currentEvidence) {
		    	this.currentPhenotypes = currentPhenotypes;

				createPhenotypeAssociationButton.setDisabled(currentGene == null);
		    	
				if (currentGene != null && currentEvidence != null) {
					this.setTitle("Evidence for " + currentGene.officialSymbol);
					
			    	this.currentGene = {
			    		id: currentGene.id,
			    		ncbiId: currentGene.ncbiId,
			    		officialSymbol: currentGene.officialSymbol,
			    		officialName: currentGene.officialName,
			    		taxonCommonName: currentGene.taxonCommonName
			    	};
			    	
					for (var i = 0; i < currentEvidence.length; i++) {
						for (var j = 0; j < currentEvidence[i].phenotypes.length; j++) {
							if (currentEvidence[i].phenotypes[j].child || currentEvidence[i].phenotypes[j].root) {
						  		currentEvidence[i].phenotypes[j].value = '<span style="font-weight: bold; color: red;">' + currentEvidence[i].phenotypes[j].value + '</span>'; 
							}
						}
					}
					evidenceStore.loadData(currentEvidence);
				} else {
					this.currentGene = null;

					this.setTitle(DEFAULT_TITLE);					
					evidenceStore.removeAll();
				}                	
		    },
			loadGene: function(geneId) {
		    	evidenceStore.reload({
		    		params: {
		    			'geneId': geneId
		    		}
		    	});
			}
		});
		this.superclass().initComponent.call(this);
		
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