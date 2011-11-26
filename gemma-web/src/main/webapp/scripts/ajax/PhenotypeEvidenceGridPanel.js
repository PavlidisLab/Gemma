Ext.namespace('Gemma');

var evidenceGridDefaultTitle = "Evidence";

function getPudmedAnchor(pudmedUrl) {
    return '<a target="_blank" href="' +
        pudmedUrl +
        '"><img ext:qtip="Go to PubMed (in new window)"  src="/Gemma/images/pubmed.gif" width="47" height="15" /></a>';
}

function getExternalDatabaseAnchor(databaseName, url, useDatabaseIcon) {
	var html = '<a target="_blank" href="' + url + '">';
	if (useDatabaseIcon) {
		html += '<img ext:qtip="Go to ' + databaseName + ' (in new window)" src="/Gemma/images/logo/' + databaseName + '.gif" />';		
	} else {
		html += url;
	}
	
	html += '</a>';
	
	return html;
}

function getRowExpanderHtml(record) {
	switch (record.className) {
		case 'DiffExpressionEvidenceValueObject' :
			return record.description;
		
		case 'ExperimentalEvidenceValueObject' :
			var html = '';

        	if (record.primaryPublicationCitationValueObject != null) {
        		html += '<p><b>Primary Publication</b>: ' +
        			record.primaryPublicationCitationValueObject.citation + ' ' +
        			getPudmedAnchor(record.primaryPublicationCitationValueObject.pubmedURL) + '</p>';
        	}

			var relPub = record.relevantPublicationsValueObjects;
        	if (relPub != null && relPub.length > 0) {
        		html += '<p><b>Relevant Publication</b>: ';
        		
				for (var i = 0; i < relPub.length; i++) {
					html += relPub[i].citation + ' ' + getPudmedAnchor(relPub[i].pubmedURL);
					
					if (i < relPub.length - 1) {
						html += " | ";
					}
				}
				html += '</p>';
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

				html += '<p>';
				Ext.iterate(expCharMap, function(key, value) {
				  html += '<b>' + key + "</b>: " + value + '<br />';
				});
				html += '</p>';
        	}
			
			return html + '<p><b>Note</b>: ' + record.description + '</p>';

		case 'ExternalDatabaseEvidenceValueObject' : 
			var html = '';

        	if (record.databaseEntryValueObject != null) {
        		var databaseName = record.databaseEntryValueObject.externalDatabase.name; 
        		
        		html += '<p><b>Database Name</b>: ' + databaseName + '<br />';
        			
        		html += '<b>Link</b>: ' + getExternalDatabaseAnchor(record.databaseEntryValueObject.externalDatabase.name, record.externalUrl, false) + '</p>';
        	}

			return html + '<p><b>Note</b>: ' + record.description + '</p>';

		case 'GenericEvidenceValueObject' : 
			return record.description;

		case 'LiteratureEvidenceValueObject' : 
			var html = '';

        	if (record.citationValueObject != null) {
        		html += '<p><b>Publication</b>: ' +
        			record.citationValueObject.citation + ' ' +
        			getPudmedAnchor(record.citationValueObject.pubmedURL) + '</p>';
        	}

			return html + '<p><b>Note</b>: ' + record.description + '</p>';

		case 'UrlEvidenceValueObject' : 
			return record.description;

		default :
			return ''; 
	}
}

function getLinkOutHtml(record) {
	switch (record.className) {
		case 'DiffExpressionEvidenceValueObject' :
			break;
		
		case 'ExperimentalEvidenceValueObject' :
        	if (record.primaryPublicationCitationValueObject != null) {
				return getPudmedAnchor(record.primaryPublicationCitationValueObject.pubmedURL);
			}
			break;

		case 'ExternalDatabaseEvidenceValueObject' :
        	if (record.databaseEntryValueObject != null) {
        		return getExternalDatabaseAnchor(record.databaseEntryValueObject.externalDatabase.name, record.externalUrl, true);
        	}
			break;

		case 'GenericEvidenceValueObject' : 
			break;

		case 'LiteratureEvidenceValueObject' : 
        	if (record.citationValueObject != null) {
        		return getPudmedAnchor(record.citationValueObject.pubmedURL);
        	}
			break;

		case 'UrlEvidenceValueObject' : 
			break;

		return ''; 
	}
}

function getTypeColumnHtml(record) {
	switch (record.className) {
		case 'DiffExpressionEvidenceValueObject' :
			return 'Differential Expression';
		
		case 'ExperimentalEvidenceValueObject' :
			var experimentValues = '';
			var experimentCharacteristics = record.experimentCharacteristics;
			
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
		
        	return 'Experimental' + experimentValues;

		case 'ExternalDatabaseEvidenceValueObject' :
			var databaseName = '';
        	if (record.databaseEntryValueObject != null) {
        		databaseName += ' [ ' + record.databaseEntryValueObject.externalDatabase.name + ' ] ';
        	}

			return 'External Database' + databaseName;

		case 'GenericEvidenceValueObject' : 
			return 'Comment';

		case 'LiteratureEvidenceValueObject' : 
			return 'Literature';

		case 'UrlEvidenceValueObject' : 
			return 'Url';

		default :
			return ''; 
	}
}


var evidenceStore = new Ext.data.Store({
    reader: new Ext.data.JsonReader({
        fields: [
        	'relevance', 'className', 'evidenceCode', 'phenotypes', 'isNegativeEvidence',
            {
				name: 'description',
				convert: function(value, record) {
					return getRowExpanderHtml(record);					
				}	
            },
            {
				name: 'type',
				convert: function(value, record) {
					return getTypeColumnHtml(record);					
				}	
            },
            {
				name: 'linkOut',
				convert: function(value, record) {
					return getLinkOutHtml(record);					
				}	
            }
        ]
    })
});
evidenceStore.setDefaultSort('relevance', 'desc');

Gemma.PhenotypeEvidenceRowExpander = Ext.extend(Ext.grid.RowExpander, {
    getRowClass : function(record, rowIndex, p, ds) {
        return this.superclass().getRowClass.call(this, record, rowIndex, p, ds) +
        	(record.data.isNegativeEvidence ? ' negative-annotation' : '');
    },
	// Use class="x-grid3-cell-inner" so that we have padding around the description.
    tpl: new Ext.Template(
        '<div class="x-grid3-cell-inner" style="white-space: normal;">{description}</div>'
    )
});
var rowExpander = new Gemma.PhenotypeEvidenceRowExpander();

Gemma.PhenotypeEvidenceGridPanel = Ext.extend(Ext.grid.GridPanel, {
    initComponent: function() {
		Ext.apply(this, {
		    autoScroll: true,
		    stripeRows: true,
			store: evidenceStore,
			loadMask: true,
			disableSelection: true,
		    viewConfig: {
		        forceFit: true
		    },
			plugins: rowExpander,
		    
			// grid columns
			columns:[
				rowExpander,
				{
					header: '<img style="vertical-align: bottom;" ext:qwidth="198" ext:qtip="The red dot marks evidence related specifically to your phenotype search." width="16" height="16" src="/Gemma/images/icons/bullet_red.png">',
					dataIndex: 'relevance',
					width: 0.12,
		            renderer: function(value) {
						if (value == 1) {
							return this.header;
						} else {
							return '';
						}
		            },
					sortable: true
				},
				{
					header: "Phenotypes",
					dataIndex: 'phenotypes',
					width: 0.35,
					sortable: false
				},
				{
					header: "Type",
					dataIndex: 'type',
					width: 1,
					renderer: function(value, metadata, record, rowIndex, colIndex, store) {
						return (record.data.isNegativeEvidence ? 
									"<img ext:qwidth='200' ext:qtip='This negative sign denotes evidence for a negative association.' src='/Gemma/images/icons/delete.png' height='16' width='16'/> " :
									"") +
								value;						
					},
					sortable: true
				},
				{
					header: "Evidence Code",
					dataIndex: 'evidenceCode',
					width: 0.33,
		            renderer: function(value) {
		            	var displayText = "";
		            	var tooltipText = "";
		            	
						switch (value) {
							case 'EXP' :
				            	displayText = 'Inferred from Experiment';
				            	tooltipText = 'An experimental assay has been located in the cited reference, whose results indicate a gene association (or non-association) to a phenotype.';
								break;
							case 'IC' :
				            	displayText = 'Inferred by Curator';
				            	tooltipText = 'The association between the gene and phenotype is not supported by any direct evidence, but can be reasonably inferred by a curator. This includes annotations from animal models or cell cultures.';
								break;
							case 'TAS' :
				            	displayText = 'Traceable Author Statement';
				            	tooltipText = 'The gene-to-phenotype association is stated in a review paper or a website (external database) with a reference to the original publication.';
								break;
							default :
								return value; 
						}
						
						return '<span ext:qwidth="200" ext:qtip="' + tooltipText + '">' + displayText + '</span>';
		            },
					sortable: true
				},
				{
					header: "Link Out",
					dataIndex: 'linkOut',
					width: 0.2,
					sortable: false
				}
			]
		});
		if (this.title == null) {
			this.title = evidenceGridDefaultTitle;
		}
		this.on('render', function() { 		
			if (this.evidencePhenotypeColumnRenderer == null) {
				this.evidencePhenotypeColumnRenderer = function(value) {
					var phenotypesHtml = '';
					for (var i = 0; i < value.length; i++) {
						phenotypesHtml += value[i].value + '<br />';
					}					
					return phenotypesHtml;
			    }
			}
			this.colModel.setRenderer(2, this.evidencePhenotypeColumnRenderer);	
		});

		Gemma.PhenotypeEvidenceGridPanel.superclass.initComponent.apply(this, arguments);
    },
    setProxy: function(proxy) {
		evidenceStore.proxy = proxy;
    },
	load: function() {
		evidenceStore.load();
	}, 
    loadData: function(data) {
		for (var i = 0; i < data.length; i++) {
			for (var j = 0; j < data[i].phenotypes.length; j++) {
				if (data[i].phenotypes[j].child || data[i].phenotypes[j].root) {
			  		data[i].phenotypes[j].value = '<span style="font-weight: bold; color: red;">' + data[i].phenotypes[j].value + '</span>'; 
				}
			}
		}

    	evidenceStore.loadData(data);
    },
    removeAll: function(silent) {
    	evidenceStore.removeAll(silent);
    },
	loadGene: function(geneId) {
	    	evidenceStore.reload({
	    		params: {
	    			'geneId': geneId
	    		}
	    	});
	    },
	setColumnHidden: function(index, hidden) {
		this.getColumnModel().setHidden(index, hidden);
	},
	setEvidencePhenotypeRenderer: function(evidencePhenotypeRenderer) {
		this.evidencePhenotypeRenderer = evidencePhenotypeRenderer;
	}
});