/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.GeneSearchAndPreview = Ext.extend(Ext.Panel, {
	listModified : false,
	resetGenePreview : function() {
		Ext.DomHelper.overwrite(this.previewPart.genePreviewContent.body, {
					cn : ''
				});
		// this.genePreviewExpandBtn.disable().hide();
		// this.geneSelectionEditorBtn.disable().hide();
	},
	showGenePreview : function() {
		this.loadMask.hide();
		this.geneSelectionEditorBtn.enable();
		this.geneSelectionEditorBtn.show();
	},

	maskGenePreview : function() {
		if (!this.loadMask) {
			this.loadMask = new Ext.LoadMask(this.getEl(), {
						msg : "Loading Genes ..."
					});
		}
		this.loadMask.show();
	},
	launchGeneSelectionEditor : function() {

		if (!this.geneIds || this.geneIds === null || this.geneIds.length === 0) {
			return;
		}
		this.searchForm.getEl().mask();

		this.geneSelectionEditorWindow.show();

		this.geneSelectionEditor.loadMask = new Ext.LoadMask(this.geneSelectionEditor.getEl(), {
					msg : "Loading genes ..."
				});
		this.geneSelectionEditor.loadMask.show();
		Ext.apply(this.geneSelectionEditor, {
					geneGroupId : this.geneGroupId,
					selectedGeneGroup : this.selectedGeneOrGroupRecord,
					groupName : this.selectedGeneOrGroupRecord.name,
					taxonId : this.searchForm.getTaxonId(),
					taxonName : this.searchForm.getTaxonName()
				});
		this.geneSelectionEditor.loadGenes(this.geneIds, function() {
					this.geneSelectionEditor.loadMask.hide();
				}.createDelegate(this, [], false));
	},

	loadGeneOrGroup : function(record, query) {

		this.selectedGeneOrGroupRecord = record.data;

		var id = record.data.reference.id;
		var isGroup = record.get("isGroup");
		var type = record.get("type");
		var size = record.get("size");
		var reference = record.get("reference");
		var name = record.get("name");
		var taxonId = this.searchForm.getTaxonId();
		
		if (id === null) {
			var queryToGetSelected = "";
			if (name.match(/^GO_\d+/)) {
				queryToGetSelected = "taxon:"+taxonId+";GO:"+name;
			}else if(type === 'freeText' && name.indexOf(query)!=-1){
				queryToGetSelected = "taxon:"+taxonId+";query:"+query;
			}
			this.queryUsedToGetSessionGroup = queryToGetSelected;
		}

		var geneIds = [];

		// load preview of group if group was selected
		if (isGroup) {
			
			geneIds = record.get('memberIds');
			if (geneIds === null || geneIds.length === 0) {
				return;
			}
			this.loadGenes(geneIds);
			this.geneGroupId = id;
			this.searchForm.geneGroupId = id;

		}
		// load single gene if gene was selected
		else {
			this.selectedGeneOrGroupRecord.memberIds = [this.selectedGeneOrGroupRecord.reference.id];
			this.geneIds = [id];

			this.geneGroupId = null;
			this.searchForm.geneGroupId = null;

			// reset the gene preview panel content
			this.resetGenePreview();

			// update the gene preview panel content
			this.previewPart.genePreviewContent.update({
						officialSymbol : record.get("name"),
						officialName : record.get("description"),
						id : record.data.reference.id,
						taxonCommonName : record.get("taxonName")
					});
			this.updateTitle(this.selectedGeneOrGroupRecord.name, 1);
			this.geneSelectionEditorBtn.setText('0 more - Edit');
			this.geneSelectionEditorBtn.enable();
			this.geneSelectionEditorBtn.show();
		}
	},

	/**
	 * update the contents of the gene preview box and the this.geneIds value
	 * using a list of gene Ids
	 * 
	 * @param geneIds
	 *            an array of geneIds to use
	 */
	loadGenes : function(ids) {

		this.maskGenePreview();

		// store the ids
		this.geneIds = ids;
		this.searchForm.geneIds = ids;
		// load the preview
		this.loadGenePreview();

	},

	/**
	 * update the contents of the gene preview box with the this.geneIds value
	 * 
	 * @param geneIds
	 *            an array of geneIds to use
	 */
	loadGenePreview : function() {

		this.maskGenePreview();

		// grab ids to use
		ids = this.geneIds;

		// load some genes to display
		var limit = (ids.size() < this.searchForm.PREVIEW_SIZE) ? ids.size() : this.searchForm.PREVIEW_SIZE;
		var previewIds = ids.slice(0, limit);
		GenePickerController.getGenes(previewIds, function(genes) {

					// reset the gene preview panel content
					this.resetGenePreview();
					for (var i = 0; i < genes.size(); i++) {
						this.previewPart.genePreviewContent.update(genes[i]);
					}
					this.updateTitle(this.selectedGeneOrGroupRecord.name,ids.size());
				
					this.geneSelectionEditorBtn.setText('<a>' + (ids.size() - limit) + ' more - Edit</a>');
					this.showGenePreview();

					if (ids.size() === 1) {
						this.geneSelectionEditorBtn.setText('0 more - Edit');
						this.geneSelectionEditorBtn.enable().show();
					}
					this.previewPart.genePreviewContent.expand();

				}.createDelegate(this));

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
				if (i >= Gemma.MAX_GENES_PER_QUERY) {
					if (!warned) {
						Ext.Msg.alert("Too many genes", "You can only search up to " + 
							Gemma.MAX_GENES_PER_QUERY +
							" genes, some of your selections will be ignored.");
						warned = true;
					}
				}

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
				var mockGeneSet = {
					data : {
						memberIds : geneIds,
						taxonId : taxonId,
						reference : {
							type : null,
							id : null
						},
						name : 'From Symbol List',
						description : 'Group made from gene symbols entered.',
						size : geneIds.length
					}
				};
				this.selectedGeneOrGroupRecord = mockGeneSet.data;

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
				this.previewPart.genePreviewContent.setTitle("Gene Selection Preview (" + geneIds.length + ")");
				this.geneSelectionEditorBtn.setText('<a>' + (geneIds.length - genesToPreview.length)+
						 ' more - Edit</a>');
				this.showGenePreview();
				this.previewPart.genePreviewContent.show();
				this.previewPart.show();

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

		/**
		 * ***** GENE SELECTION EDITOR
		 * *****************************************************************
		 */
		this.geneSelectionEditor = new Gemma.GeneMembersGrid({
					// id: 'geneSelectionEditor',
					name : 'geneSelectionEditor',
					// hidden: 'true',
					hideHeaders : true,
					frame : false
				});

		this.geneSelectionEditor.on('geneListModified', function(newRecords) {
			var i;
			for (i = 0; i < newRecords.length; i++) { // should only be one
				if (typeof newRecords[i].geneIds !== 'undefined') {
					this.loadGenes(newRecords[i].geneIds);
					// update record
					this.selectedGeneOrGroupRecord = newRecords[i];
				}
				if (newRecords[i].name) {
					this.updateTitle(newRecords[i].name, newRecords[i].size);
				}
			}
				this.listModified = true;
			}, this);

		this.geneSelectionEditor.on('doneModification', function() {
					this.searchForm.getEl().unmask();
					this.geneSelectionEditorWindow.hide();
				}, this);

		this.geneSelectionEditorBtn = new Ext.LinkButton({
					handler : this.launchGeneSelectionEditor,
					scope : this,
					style : 'float:right;text-align:right; padding-right:10px; padding-bottom:5px',
					width : '200px',
					tooltip : "Edit your selection",
					hidden : true,
					disabled : true,
					ctCls : 'right-align-btn transparent-btn'
				});

		this.geneSelectionEditorWindow = new Ext.Window({
					// id : 'geneSelectionEditorWindow',
					// closeAction: 'hide',
					closable : false,
					layout : 'fit',
					items : this.geneSelectionEditor,
					title : 'Edit Your Gene Selection'
				});

		/**
		 * **** GENE PREVIEW
		 * ***************************************************************************
		 */
		this.previewPart = new Ext.Panel({
			border: true,
				hidden: true,
				forceLayout:true,
				hideBorders: true,
				bodyStyle: 'border-color:#B5B8C8', // hide border until a selection is made
				items: [{
					width : 322,
					ref: 'genePreviewContent',
					tpl : new Ext.Template('<div style="padding-bottom:7px;">'+
					'<a target="_blank" href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName} '+
					'<span style="color:grey">({taxonCommonName})</span></div>'),
					tplWriteMode : 'append', // use this to append to content when
					// calling update instead of replacing
					title : 'Gene Selection Preview',
					collapsible : true,
					forceLayout:true,
					tools:[{
						id:'delete',
						handler:function(event, toolEl, panel, toolConfig){
								this.searchForm.removeGeneChooser(this.id);
						}.createDelegate(this, [], true),
						qtip:'Remove this gene or group from your search'
					}],
					cls : 'unstyledTitle',
					bodyStyle:'padding:10px',
					hidden : false,
					listeners : {
						collapse : function() {
							this.geneSelectionEditorBtn.hide();
						}.createDelegate(this, [], true),
						expand : function() {
							this.geneSelectionEditorBtn.show();
						}.createDelegate(this, [], true)
					}
				}, this.geneSelectionEditorBtn]
		});
		this.updateTitle = function(name, size){
			this.previewPart.genePreviewContent.setTitle(
				'<span style="font-size:1.2em">'+name+
				'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' + size + ((size > 1)?" genes)":" gene)"));
		};
		this.collapsePreview = function(){
			this.geneSelectionEditorBtn.hide();
			if(typeof this.previewPart.genePreviewContent !== 'undefined'){
				this.previewPart.genePreviewContent.collapse(true);
			}
		};				
		
		this.removeBtn = new Ext.Button({
					icon : "/Gemma/images/icons/cross.png",
					cls : "x-btn-icon",
					tooltip : 'Remove this experiment or group from your search',
					hidden : true,
					handler : function() {
						this.fireEvent('removeGene');
					}.createDelegate(this, [], true)
				});

		this.helpBtn = new Ext.Panel({
			hidden : false,
			padding:'3px',
			html: '<img ext:qtip=\'Select a general group of genes or try searching for genes by symbol, '+
					'GO terms or keywords such as: schizophrenia, hippocampus etc.<br><br>'+
					'<b>Example: search for \"map kinase\" and select a GO group</b>\' ' +
					'src="/Gemma/images/icons/question_blue.png">'
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
				
		Gemma.GeneSearchAndPreview.superclass.initComponent.call(this);

	}

});

Ext.reg('geneSearchAndPreview', Gemma.GeneSearchAndPreview);