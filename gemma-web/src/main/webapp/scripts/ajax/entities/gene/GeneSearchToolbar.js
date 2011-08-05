/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.GeneSearchToolbar = Ext.extend(Ext.Panel, {
	
	wrapSelectedInSetForReturn: false,
	
	loadGeneOrGroup : function(record, query) {

		this.selectedGeneOrGroup = record.data;

		var id = record.get("resultValueObject").id;
		var resultValueObject = record.get("resultValueObject");
		var isGroup = record.get("isGroup");
		var size = record.get("size");
		var name = record.get("name");
		var taxonName = record.get("taxonName");
		var taxonId = record.get("taxonId");
		
		/*For bookmarking*/
		if (id === null) {
			var queryToGetSelected = "";
			if (name.match(/^GO_\d+/)) {
				queryToGetSelected = "taxon:"+taxonId+";GO:"+name;
			}else if(resultValueObject instanceof FreeTextGeneResultsValueObject && name.indexOf(query)!=-1){
				queryToGetSelected = "taxon:"+taxonId+";query:"+query;
			}
			this.queryUsedToGetSessionGroup = queryToGetSelected;
		}
		
		if( wrapSelectedInSetForReturn && !(this.selectedGeneOrGroup instanceof GeneSetValueObject) ){
			var wrapper = new SessionBoundGeneSetValueObject();
			wrapper.modified = false;
			wrapper.name = name;
			wrapper.description = description;
			wrapper.size = 1;
			wrapper.geneIds = [id];
			wrapper.taxonId = taxonId;
			wrapper.taxonName = taxonName;
		}

	},

	/**
	 * Given text, search Gemma for matching genes. Used to 'bulk load' genes
	 * from the GUI.
	 * 
	 * @param {}
	 *            e
	 */
	getGenesFromList : function(e, taxonId) {
		var taxonName;
		if (!taxonId && this.searchForm.getTaxonId()) {
			taxonId = this.searchForm.getTaxonId();
			taxonName = this.searchForm.getTaxonName();
		}else{
			taxonId = this.symbolList._taxonCombo.getTaxon().id;
			taxonName = this.symbolList._taxonCombo.getTaxon().data.commonName;
		}

		if (isNaN(taxonId)) {
			Ext.Msg.alert("Missing information", "Please select a taxon.");
			return;
		}

		this.searchForm.taxonChanged(taxonId, taxonName);
		this.geneCombo.disable().hide();
		this.helpBtn.hide();
		this.symbolListButton.hide();
		this.removeBtn.setPosition(300,0);
		this.fireEvent('madeFirstSelection');
		this.doLayout();
								
		var loadMask = new Ext.LoadMask(this.getEl(), {
					msg : "Loading genes..."
				});
		loadMask.show();
		var text = e.geneNames;
		GenePickerController.searchMultipleGenesGetMap(text, taxonId, {

			callback : function(queryToGenes) {
				var i;
				var geneData = [];
				var warned = false;
				this.maskGenePreview();

				var geneIds = [];
				var genesToPreview = [];
				var queriesWithMoreThanOneResult = [];
				var queriesWithNoResults = [];
				var query;

				// for each query
				for (query in queryToGenes) {
					var genes = queryToGenes[query];
					// for each result of that query

					// if a query matched more than one result, store for
					// notifying user
					if (genes.length > 1) {
						queriesWithMoreThanOneResult.push(query);
					}

					// if a query matched no results, store for notifying user
					if (genes.length === 0) {
						queriesWithNoResults.push(query);
					}

					for (i = 0; i < genes.length; i++) {
						// store some genes for previewing
						if (genesToPreview.length < this.searchForm.PREVIEW_SIZE) {
							genesToPreview.push(genes[i]);
						}
						// store all ids
						geneIds.push(genes[i].id);
					}
				}

				this.searchForm.geneIds = geneIds;
				this.geneIds = geneIds;
				var newGeneSet = new SessionBoundGeneSetValueObject();
				newGeneSet.modified = false;
				newGeneSet.geneIds = geneIds;
				newGeneSet.taxonId = taxonId;
				newGeneSet.name = 'From Symbol List';
				newGeneSet.description = 'Group made from gene symbols entered.';
				newGeneSet.numExperiments = geneIds.length;
				newGeneSet.id = null;
				this.selectedGeneOrGroup = newGeneSet;

				// if some genes weren't found or some gene matches were
				// inexact,
				// prepare a msg for the user

				var msgMany = "";
				var msgNone = "";
				if (queriesWithMoreThanOneResult.length > 0) {
					msgMany = queriesWithMoreThanOneResult.length +
							((queriesWithMoreThanOneResult.length === 1) ? " query" : " queries") +
							"  returned more than one gene, all were added to the results: <br>";
					// for each query
					query = '';
					for (i = 0; i < queriesWithMoreThanOneResult.length; i++) {
						query = queriesWithMoreThanOneResult[i];
						msgMany += "<br> - <b>" + query + "</b> matched: ";
						genes = queryToGenes[query];
						// for each result of that query
						for (var j = 0; j < genes.length && j < 10; j++) {
							msgMany += genes[j].officialSymbol;
							msgMany += (j + 1 < genes.length) ? ", " : ".";
						}
						if (genes.length > 10) {
							msgMany += "...(" + genes.length - 20 + " more)";
						}
					}
					msgMany += '<br><br><br>';
				}
				if (queriesWithNoResults.length > 0) {
					msgNone = queriesWithNoResults.length +
							((queriesWithNoResults.length === 1) ? " query" : " queries") +
							" did not match any genes in Gemma:<br><br>";
					// for each query
					query = '';
					for (i = 0; i < queriesWithNoResults.length; i++) {
						query = queriesWithNoResults[i];
						msgNone += " - " + query + "<br>";
					}
				}

				// reset the gene preview panel content
				this.resetGenePreview();

				if (queriesWithMoreThanOneResult.length > 0 || queriesWithNoResults.length > 0) {

					Ext.DomHelper.append(this.previewPart.genePreviewContent.body, {
								cn : '<div style="padding-bottom:7px;color:red;">Not all symbols had exact matches ('+
										 '<a onmouseover="this.style.cursor=\'pointer\'" '+
										 'onclick="Ext.Msg.alert(\'Query Result Details\',\'<br>'+
										 msgMany+
										 msgNone+
										 '\');" style="color: red; text-decoration: underline;">details</a>)</div>'
							});
				}

				// write to the gene preview panel
				for (i = 0; i < genesToPreview.length; i++) {
					this.previewPart.genePreviewContent.update(genesToPreview[i]);
				}
				this.previewPart.genePreviewContent.setTitle("Gene Selection Preview (" + geneIds.length + " genes)");
				this.geneSelectionEditorBtn.setText((geneIds.length - genesToPreview.length)+
						 ' more - Edit');
				this.showGenePreview();
				this.previewPart.genePreviewContent.show();
				this.previewPart.show();

				if (geneIds.size() <= this.searchForm.PREVIEW_SIZE) {
					this.previewPart.moreIndicator.update('');
				}else{
					this.previewPart.moreIndicator.update('[...]');
				}

				if (geneIds.size() <= 1) {
					this.geneSelectionEditorBtn.setText('0 more - Edit');
					this.geneSelectionEditorBtn.enable().show();
				}

				loadMask.hide();

			}.createDelegate(this),

			errorHandler : function(e) {
				this.getEl().unmask();
				Ext.Msg.alert('There was an error', e);
			}
		});
		this.geneSelectionEditorBtn.show();
		this.fireEvent('select');
	},
	initComponent : function() {

		/**
		 * **** GENE COMBO
		 * *****************************************************************************
		 */
		/* this.geneCombo = new Gemma.MyEXTGeneAndGeneGroupCombo; */
		this.newBoxTriggered = false;
		this.geneCombo = new Gemma.GeneAndGeneGroupCombo({
			width: 282,
			hideTrigger: true,
			taxonId: this.taxonId
		});
		this.geneCombo.on('select', function(combo, record, index) {
								
								this.searchForm.taxonChanged(record.get("taxonId"), record.get("taxonName"));
					
								var query = combo.store.baseParams.query;
								this.loadGeneOrGroup(record, query);
								this.previewPart.genePreviewContent.show();
								this.previewPart.show();
								this.previewPart.genePreviewContent.expand();
								
								this.geneSelectionEditor.setTaxonId(record.get("taxonId"));

								// if this was the first time a selection was
								// made using this box
								if (combo.startValue === '' && this.newBoxTriggered === false) {
									this.fireEvent('madeFirstSelection');
									this.newBoxTriggered = true;
									this.helpBtn.hide();
									this.symbolListButton.hide();
									this.removeBtn.show();
								}
								combo.disable().hide();
								this.helpBtn.hide();
								this.symbolListButton.hide();
								this.removeBtn.setPosition(300,0);
								this.doLayout();
								
							},
							this
						);
					

		this.relayEvents(this.geneCombo, ['select']);

		this.symbolList = new Gemma.GeneImportPanel({
			height:300,
			showTaxonCombo: true,
			listeners: {
				'commit': {
					fn: this.getGenesFromList.createDelegate(this),
					scope: this
				},
				'show': {
					fn: function(){
						if (this.searchForm.getTaxonId() !== null && this.searchForm.getTaxonId() && typeof this.searchForm.getTaxonId() !== 'undefined') {
							this.symbolList._taxonCombo.setTaxonById(this.searchForm.getTaxonId());
							this.symbolList._taxonCombo.disable();
						}
					},
					scope:this
				}
			}
		});
		

		this.symbolListButton = new Ext.Button({
					icon : "/Gemma/images/icons/page_upload.png",
					cls : "x-btn-icon",
					tooltip : "Select multiple genes with a list of symbols or NCBI IDs",
					disabled : false,
					style:'padding-right:5px',
					handler : function() {
						this.geneCombo.reset();
						this.symbolList.show();
					}.createDelegate(this, [], true)
				});

		this.helpBtn = new Gemma.InlineHelpIcon({
			tooltipText:'Select a general group of genes or try searching for genes by symbol, '+
					'GO terms or keywords such as: schizophrenia, hippocampus etc.<br><br>'+
					'<b>Example: search for "map kinase" and select a GO group</b>'
		});
		Ext.apply(this, {
			width: 335,
			frame: false,
			border: false,
			hideBorders: true,
			items: [{
				layout: 'hbox',
				hideBorders: true,
				items: [this.symbolListButton, this.geneCombo, this.helpBtn]
			}, this.previewPart]
		});
				
		Gemma.GeneSearchToolbar.superclass.initComponent.call(this);

	}

});

Ext.reg('geneSearchToolbar', Gemma.GeneSearchToolbar);