/**
 * 
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

/**
 * 
 * Displays a small number of elements from the set with links to the set's page and to an editor
 * 
 * @class Gemma.GeneSetPreview
 * @xtype Gemma.GeneSetPreview
 */
Gemma.GeneSetPreview = Ext.extend(Gemma.SetPreview, {

	/**
	 * public
	 * update the contents of the gene preview box
	 *
	 * @param {Number[]} ids an array of geneIds to use to populate preview
	 */
	loadGenePreviewFromIds: function(ids){
	
		this.entityIds = ids;
		this.totalCount = ids.length;
		// load some genes to display
		var limit = (ids.size() < this.preview_size) ? ids.size() : this.preview_size;
		var previewIds = ids.slice(0, limit);
		GenePickerController.getGenes(previewIds, function(genes){
			//this.unmask();
			this.loadPreview(genes, ids.length);
		}.createDelegate(this));
	},
	
	
	/**
	 * public
	 * update the contents of the gene preview box
	 *
	 * @param {GeneValueSetObject[]} geneSet populate preview with members
	 */
	loadGenePreviewFromGeneSet: function(geneSet){
	
		var ids = geneSet.geneIds;
		this.entityIds = ids;
		// load some genes to display
		this.loadGenePreviewFromIds(ids);
		this.setSelectedSetValueObject(geneSet);
		
	},
	
	/**
	 * public
	 * update the contents of the gene preview box
	 *
	 * @param {GeneValueObject[]} genes an array of genes to use to populate preview
	 */
	loadGenePreviewFromGenes: function(genes){
	
		this.entityIds = [];
		Ext.each(genes, function(item, index, allitems){
			this.entityIds.push(item.id);
		}, this);
		
		this.totalCount = genes.length;
		// load some genes to display
		var limit = (genes.size() < this.preview_size) ? genes.size() : this.preview_size;
		var previewGenes = genes.slice(0, limit);
		this.loadPreview(previewGenes, genes.length);
		
	},
	
	/**
	 * public
	 * don't use params if you want to update name based on this.selectedEntityOrGroup.resultValueObject
	 * @param {Object} name
	 * @param {Object} size
	 */
	updateTitle: function(name, size){
	
		// if an gene set page exists for this set, make title a link 
		if (!name && this.selectedSetValueObject instanceof GeneSetValueObject) {
			size = this.selectedSetValueObject.geneIds.size();
			
			if (this.selectedSetValueObject instanceof DatabaseBackedGeneSetValueObject) {
			
				name = '<a target="_blank" href="/Gemma/geneSet/showGeneSet.html?id=' +
				this.selectedSetValueObject.id +
				'">' +
				this.selectedSetValueObject.name +
				'</a>'
				
			} else if (this.selectedSetValueObject instanceof GOGroupValueObject) {
				name = this.selectedSetValueObject.name + ": " + this.selectedSetValueObject.description;
			} else {
				name = this.selectedSetValueObject.name;
			}
		} else if (!name) {
			name = "Gene Selection Preview";
		}
		this.previewContent.setTitle('<span style="font-size:1.2em">' + name +
				'</span> &nbsp;&nbsp;<span style="font-weight:normal">(' +
				this.totalCount +
				((this.totalCount > 1) ? " genes)" : " gene)"));
	},
	
	initComponent: function(){
	
		Ext.apply(this, {
			selectionEditor: new Gemma.GeneMembersSaveGrid({
				name: 'geneSelectionEditor',
				hideHeaders: true,
				width: 500,
				height: 500,
				frame: false
			}),
			defaultTpl: new Ext.Template('<div style="padding-bottom:7px;">' +
			'<a target="_blank" href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName} ' +
			'<span style="color:grey">({taxonCommonName})</span></div>'),
			
			defaultPreviewTitle: "Gene Selection Preview"
		
		});
		Gemma.GeneSetPreview.superclass.initComponent.call(this);
		
		this.selectionEditor.on('geneListModified', function(newSets){
			var i;
			for (i = 0; i < newSets.length; i++) { // should only be one
				if (typeof newSets[i].geneIds !== 'undefined' && typeof newSets[i].name !== 'undefined') {
					this.loadGenePreviewFromIds(newSets[i].geneIds);
					this.setSelectedSetValueObject(newSets[i]);
					this.updateTitle();
				}
			}
			this.listModified = true;
			this.fireEvent('geneListModified', newSets);
		}, this);
		
	}
	
});

Ext.reg('Gemma.GeneSetPreview', Gemma.GeneSetPreview);