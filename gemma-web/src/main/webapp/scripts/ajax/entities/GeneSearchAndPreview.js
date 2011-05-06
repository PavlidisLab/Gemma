/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.GeneSearchAndPreview = Ext.extend(Ext.Panel, {

	resetGenePreview : function() {
		Ext.DomHelper.overwrite(this.genePreviewContent.body, {
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
					selectedGeneGroup : this.geneCombo.selectedGeneGroup,
					groupName : (this.geneCombo.selectedGeneGroup) ? this.geneCombo.selectedGeneGroup.name : null,
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
		var geneIds = [];

		// load preview of group if group was selected
		if (isGroup) {

			if (type === "GOgroup") {
				// if no taxon has been selected, warn user that this won't work
				if (!this.searchForm.getTaxonId() || isNaN(this.searchForm.getTaxonId())) {
					Ext.Msg.alert("Error", "You must select a taxon before selecting a GO group.");
					return;
				}
			}

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
			this.searchForm.geneIds = [id];

			this.geneGroupId = null;
			this.searchForm.geneGroupId = null;

			// reset the gene preview panel content
			this.resetGenePreview();

			// update the gene preview panel content
			this.genePreviewContent.update({
						officialSymbol : record.get("name"),
						officialName : record.get("description"),
						id : record.get("id"),
						taxonCommonName : record.get("taxonName")
					});
			this.genePreviewContent.setTitle("Gene Selection Preview (1)");
			this.geneSelectionEditorBtn.setText('0 more');
			this.geneSelectionEditorBtn.disable();
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
						this.genePreviewContent.update(genes[i]);
					}
					this.genePreviewContent.setTitle("Gene Selection Preview (" + ids.size() + ")");
					this.geneSelectionEditorBtn.setText('<a>' + (ids.size() - limit) + ' more - Edit</a>');
					this.showGenePreview();

					if (ids.size() === 1) {
						this.geneSelectionEditorBtn.setText('0 more');
						this.geneSelectionEditorBtn.disable();
						this.geneSelectionEditorBtn.show();
					}
					this.genePreviewContent.expand();

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

		if (!taxonId && this.searchForm.getTaxonId()) {
			taxonId = this.searchForm.getTaxonId();
		}

		if (isNaN(taxonId)) {
			Ext.Msg.alert("Missing information", "Please select an experiment or experiment group first.");
			return;
		}

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
						Ext.Msg.alert("Too many genes", "You can only search up to " + Gemma.MAX_GENES_PER_QUERY
										+ " genes, some of your selections will be ignored.");
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
					msgMany = queriesWithMoreThanOneResult.length
							+ ((queriesWithMoreThanOneResult.length === 1) ? " query" : " queries")
							+ "  returned more than one gene, all were added to the results: <br>";
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
					msgNone = queriesWithNoResults.length
							+ ((queriesWithNoResults.length === 1) ? " query" : " queries")
							+ " did not match any genes in Gemma:<br><br>";
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

					Ext.DomHelper.append(this.genePreviewContent.body, {
								cn : '<div style="padding-bottom:7px;color:red;">Warning: Not all symbols had exact matches ('
										+ '<a onclick="Ext.Msg.alert(\'Query Result Details\',\'Please note:<br><br>'
										+ msgMany
										+ msgNone
										+ '\');" style="color: red; text-decoration: underline;">details</a>)</div>'
							});
				}

				// write to the gene preview panel
				for (i = 0; i < genesToPreview.length; i++) {
					this.genePreviewContent.update(genesToPreview[i]);
				}
				this.genePreviewContent.setTitle("Gene Selection Preview (" + geneIds.length + ")");
				this.geneSelectionEditorBtn.setText('<a>' + (geneIds.length - genesToPreview.length)
						+ ' more - Edit</a>');
				this.showGenePreview();
				this.genePreviewContent.show();

				if (geneIds.size() <= 1) {
					this.geneSelectionEditorBtn.setText('0 more');
					this.geneSelectionEditorBtn.disable();
					this.geneSelectionEditorBtn.show();
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
					width : 282,
					hideTrigger : false,
					typeAhead : false,
					taxonId : this.taxonId,
					listEmptyText : 'Enter text to search for genes',
					listeners : {
						'select' : {
							fn : function(combo, record, index) {
								var query = combo.store.baseParams.query;
								this.loadGeneOrGroup(record, query);
								this.genePreviewContent.show();
								this.genePreviewContent.expand();

								// if this was the first time a selection was
								// made using this box
								if (combo.startValue === '' && this.newBoxTriggered === false) {
									this.fireEvent('madeFirstSelection');
									this.newBoxTriggered = true;
									this.helpBtn.hide();
									this.removeBtn.show();
									// this.relayEvents(this.experimentCombo,
									// ['select']);
								}
							},
							scope : this
						},
						'focus' : {
							fn : function(cb, rec, index) {
								if (this.searchForm.getSelectedExperimentRecords().length === 0) {
									Ext.Msg.alert("Missing information",
											"Please select an experiment or experiment group first.");
								}
							},
							scope : this
						}
					}
				});

		if (this.searchForm.getSelectedExperimentRecords().length === 0) {
			Ext.apply(this.geneCombo, {
				style : 'background:Gainsboro;' // gene choosing should
					// be disabled until an
					// experiment has been
					// chosen
				});
		}

		this.relayEvents(this.geneCombo, ['select']);

		this.symbolList = new Gemma.GeneImportPanel({
					listeners : {
						'commit' : {
							fn : this.getGenesFromList.createDelegate(this),
							scope : this
						}
					}
				});

		this.symbolListButton = new Ext.Button({
					icon : "/Gemma/images/icons/page_upload.png",
					cls : "x-btn-icon",
					tooltip : "Import multiple genes",
					disabled : false,
					handler : function() {

						if (!this.searchForm.getTaxonId() || isNaN(this.searchForm.getTaxonId())) {
							Ext.Msg.alert("Missing information",
									"Please select an experiment or experiment group first.");
							return;
						}

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
					height : 200,
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
					this.geneCombo.setRawValue(newRecords[i].name);
				}
				// this.geneCombo.setTaxon(newRecords[i].taxonId);
			}
				// this.geneCombo.getStore().reload();
			}, this);

		this.geneSelectionEditor.on('doneModification', function() {
					this.searchForm.getEl().unmask();
					this.geneSelectionEditorWindow.hide();
				}, this);

		this.geneSelectionEditorBtn = new Ext.LinkButton({
					handler : this.launchGeneSelectionEditor,
					scope : this,
					style : 'float:right;text-align:right; ',
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

		// use this.genePreviewContent.update("one line of gene text"); to write
		// to this panel
		this.genePreviewContent = new Ext.Panel({
			// height:100,
			width : 302,
			tpl : new Ext.Template('<div style="padding-bottom:7px;"><a target="_blank" href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName} <span style="color:grey">({taxonCommonName})</span></div>'),
			tplWriteMode : 'append', // use this to append to content when
			// calling update instead of replacing
			style : 'padding-top:7px;',
			title : 'Gene Selection Preview',
			collapsible : true,
			cls : 'unstyledTitle',
			hidden : true,
			listeners : {
				collapse : function() {
					this.geneSelectionEditorBtn.hide();
				}.createDelegate(this, [], true),
				expand : function() {
					this.geneSelectionEditorBtn.show();
				}.createDelegate(this, [], true)
			}
		});

		this.genePreviewExpandBtn = new Ext.Button({
					handler : function() {
						this.genePreviewExpandBtn.disable().hide();
						this.loadGenePreview();
					}.createDelegate(this, [], true),
					scope : this,
					style : 'float:right;',
					tooltip : "View your selection",
					hidden : true,
					disabled : true,
					icon : "/Gemma/images/plus.gif",
					cls : "x-btn-icon"
				});
		this.removeBtn = new Ext.Button({
					icon : "/Gemma/images/icons/cross.png",
					cls : "x-btn-icon",
					tooltip : 'Remove this experiment or group from your search',
					hidden : true,
					handler : function() {
						this.fireEvent('removeGene');
					}.createDelegate(this, [], true)
				});
		this.helpBtn = new Ext.Button({
			icon : "/Gemma/images/icons/questionMark16x16.png",
			cls : "x-btn-icon",
			tooltip : 'Select a general group of genes or try searching for genes by symbol, GO terms or keywords such as: schizophrenia, hippocampus etc.',
			hidden : false,
			handler : function() {
				Ext.Msg
						.alert(
								'Gene Selection Help',
								'Select a general group of genes or try searching for genes by symbol, GO terms or keywords such as: schizophrenia, hippocampus etc.');
			}
		});
		Ext.apply(this, {
					width : 335,
					frame : true,
					items : [{
								layout : 'hbox',
								items : [this.symbolListButton, this.geneCombo, this.removeBtn, this.helpBtn]
							}, this.genePreviewExpandBtn, this.genePreviewContent, this.geneSelectionEditorBtn]
				});
		Gemma.GeneSearchAndPreview.superclass.initComponent.call(this);

	}

});

Ext.reg('geneSearchAndPreview', Gemma.GeneSearchAndPreview);