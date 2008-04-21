/*
* Widget that allows user to search for and select one or more genes from the database. The selected genes are kept in a table which can be edited.
* This component is the top part of the coexpression interface, but should be reusable. 
*
* 
* Version : $Id$
* Author : luke
*/
Ext.namespace('Ext.Gemma');

/* Ext.Gemma.GeneChooserPanel constructor...
 * 	config is a hash with the following options:
 */
Ext.Gemma.GeneChooserPanel = function ( config ) {
	
	this.genes = config.genes; delete config.genes;
	this.showTaxon = config.showTaxon; delete config.showTaxon;
	
	var thisGrid = this;
	
	var taxonCombo;
	if ( this.showTaxon ) {
		taxonCombo = new Ext.Gemma.TaxonCombo( {
			emptyText : 'select a taxon',
			width : 120
		} );
		taxonCombo.on( "taxonchanged", function( combo, taxon ) {
			thisGrid.taxonChanged( taxon );
		}, this );
	}
	this.taxonCombo = taxonCombo;
	
	var geneCombo = new Ext.Gemma.GeneCombo( {
		emptyText : 'search for a gene'
	} );
	geneCombo.on( "select", function( combo, record, index ) {
		taxonCombo.setTaxonByScientificName(record.data.taxon);
		addButton.enable();
	} );
	this.geneCombo = geneCombo;
	
	var addButton = new Ext.Toolbar.Button( {
		icon : "/Gemma/images/icons/add.png",
		cls:"x-btn-icon",
		tooltip : "Add a gene to the list",
		disabled : true,
		handler : function() {
			var gene = geneCombo.getGene();
			if ( thisGrid.getStore().find( "id", gene.id ) < 0) {  
				var Constructor = Ext.Gemma.GeneCombo.getRecord();
				var record = new Constructor( gene );
				thisGrid.getStore().add( [ record ] );
			}
			geneCombo.reset();
			addButton.disable(); 
		}
	} );
	this.addButton = addButton;
	
	var removeButton = new Ext.Toolbar.Button( {
		icon : "/Gemma/images/icons/subtract.png",
		cls:"x-btn-icon",
		tooltip : "Remove the selected gene from the list",
		disabled : true,
		handler : function() {
			var selected = thisGrid.getSelectionModel().getSelections();
			for ( var i=0; i<selected.length; ++i ) {
				thisGrid.getStore().remove( selected[i] );
			}
			removeButton.disable(); 
		}
	} );
	
	var chooser = new Ext.Gemma.GeneImportPanel({
				el : 'coexpression-genes',
				handler : function() {
					this.chooser.hide();
					this.getGenesFromList(  );
				},
				scope : this
			});
	this.chooser = chooser;
	
	var multiButton = new Ext.Toolbar.Button( {
		icon : "/Gemma/images/icons/page_white_put.png",
		cls:"x-btn-icon",
		tooltip : "Import multiple genes",
		disabled : false,
		handler : function() {
			
			// show the multigene chooser
			chooser.show();
			geneCombo.reset();
			addButton.enable(); 
		}
	} );
	this.multiButton = multiButton;
	
	
	var tbarItems = this.showTaxon ? [ taxonCombo, new Ext.Toolbar.Spacer() ] : [];
	tbarItems.push (
		geneCombo,
		new Ext.Toolbar.Spacer(),
		addButton,
		new Ext.Toolbar.Spacer(),
		removeButton,
		new Ext.Toolbar.Spacer(),
		multiButton
	);
	
	var debugButton = new Ext.Toolbar.Button( {
		text : "debug",
		handler : function() {
			var selected = thisGrid.getSelectionModel().getSelections();
			for ( var i=0; i<selected.length; ++i ) {
				alert( selected[i] );
			}
		}
	} );
	
	/* establish default config options...
	 */
	var superConfig = {
		height : 200, 
		autoScroll : true,
		emptyText : "Genes will be listed here",
		tbar : tbarItems,
		store : new Ext.data.SimpleStore( {
			fields : [
				{ name: 'id', type: 'int' },
				{ name: 'taxon', type: 'string' },
				{ name: 'officialSymbol', type: 'string' },
				{ name: 'officialName', type: 'string' }
			],
			sortInfo : { field: 'officialSymbol', direction: 'ASC' }
		} ),
		columns : [
			{ header: 'Gene', dataIndex: 'officialSymbol', sortable: true },
		//	{ header: 'Taxon', dataIndex: 'taxon', sortable: true, hidden: this.showTaxon ? false : true },
			{ id: 'desc', header: 'Description', dataIndex: 'officialName' }
		],
		autoExpandColumn : 'desc'
	};

	/* apply user-defined config options and call the superclass constructor...
	 */
	for ( var property in config ) {
		superConfig[property] = config[property];
	}
 
	Ext.Gemma.GeneChooserPanel.superclass.constructor.call( this, superConfig );
	
	/* code down here has to be called after the super-constructor so that we
	 * know we're a grid...
	 */
	this.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 ) {
			removeButton.enable();
		} else {
			removeButton.disable();
		}
	} );
	
	if ( config.genes ) {
		var genes = config.genes instanceof Array ? config.genes : config.genes.split( "," );
		this.loadGenes( genes );
	}
	
	this.getStore().on( "load", function () {
		//this.autoSizeColumns();
		this.doLayout();
	}, this );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.GeneChooserPanel, Ext.Gemma.GemmaGridPanel, {

	autoSizeColumns: function() {
	    for (var i = 0; i < this.colModel.getColumnCount(); i++) {
    		this.autoSizeColumn(i);
	    }
	},
	
	getGenesFromList : function() {
		if (!this.chooser) {
			return;
		}
	 
		var taxonId = this.getTaxonId();
		var text = this.chooser.getGeneNames();
		GenePickerController.searchMultipleGenes( text, taxonId,
			function ( genes ) {
				var geneData = [];
				for ( var i=0; i<genes.length; ++i ) {
					geneData.push( [
						genes[i].id,
						genes[i].taxon.scientificName,
						genes[i].officialSymbol,
						genes[i].officialName
					] );
				}
				this.getStore().loadData( geneData, true );
			}.bind( this )
		);
		
	},

	autoSizeColumn: function(c) {
		var w = this.view.getHeaderCell(c).firstChild.scrollWidth;
		for (var i = 0, l = this.store.getCount(); i < l; i++) {
			w = Math.max(w, this.view.getCell(i, c).firstChild.scrollWidth);
		}
		this.colModel.setColumnWidth(c, w);
		return w;
	},
	
	loadGenes: function( geneIds, callback ) {
		GenePickerController.getGenes( geneIds,
			function ( genes ) {
				var geneData = [];
				for ( var i=0; i<genes.length; ++i ) {
					geneData.push( [
						genes[i].id,
						genes[i].taxon.scientificName,
						genes[i].officialSymbol,
						genes[i].officialName
					] );
				}
				this.getStore().loadData( geneData );
				if ( callback ) {
					callback();
				}
			}.bind( this )
		);
	},
	
	setGene: function( geneId, callback ) {
		GenePickerController.getGenes( [ geneId ],
			function ( genes ) {
				var g = genes[0];
				if ( g ) {					
					g.taxon = g.taxon.scientificName;				
					this.geneCombo.setGene( g );
					this.geneCombo.setValue( g.officialSymbol );
					this.getStore().removeAll();
					this.addButton.enable();
				}
				if ( callback ) {
					callback();
				}
			}.bind( this )
		);
	},
	
	getGeneIds : function () {
		var ids = [];
		var all = this.getStore().getRange();
		for ( var i=0; i<all.length; ++i ) {
			ids.push( all[i].data.id );
		}
		var gene = this.geneCombo.getGene();
		if ( gene ) {
			for ( var i=0; i<ids.length; ++i ) {
				if ( ids[i] == gene.id ) {
					return ids;
				}
			}
			ids.push( gene.id );
		}
		return ids;
	},
	
	getTaxonId : function () {
		if ( this.taxonCombo ) {
			return this.taxonCombo.getValue();
		}
	},
	
	taxonChanged : function ( taxon ) {
		this.taxonCombo.setTaxon( taxon );
		this.geneCombo.setTaxon( taxon );
		var all = this.getStore().getRange();
		for ( var i=0; i<all.length; ++i ) {
			if ( all[i].data.taxon != taxon.scientificName ) {
				this.getStore().remove( all[i] );
			}
		}
	}
	
} );

