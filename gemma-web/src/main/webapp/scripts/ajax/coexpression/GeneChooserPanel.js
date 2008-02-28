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
			width : 150
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
		addButton.enable();
	} );
	this.geneCombo = geneCombo;
	
	var addButton = new Ext.Toolbar.Button( {
		text : "+",
		tooltip : "Add a gene to the list",
		disabled : true,
		handler : function() {
			var gene = geneCombo.getGene();
			var Constructor = Ext.Gemma.GeneCombo.getRecord();
			var record = new Constructor( gene );
			thisGrid.getStore().add( [ record ] );
			geneCombo.reset();
			addButton.disable();
		}
	} );
	
	var removeButton = new Ext.Toolbar.Button( {
		text : "-",
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
	
	var tbarItems = this.showTaxon ? [ taxonCombo, new Ext.Toolbar.Spacer() ] : [];
	tbarItems.push (
		geneCombo,
		new Ext.Toolbar.Spacer(),
		addButton,
		new Ext.Toolbar.Spacer(),
		removeButton
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
		autoHeight : true,
		autoScroll : true,
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
			{ header: 'Taxon', dataIndex: 'taxon', sortable: true, hidden: this.showTaxon ? false : true },
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
		this.autoSizeColumns();
		this.doLayout();
	}, this );
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.GeneChooserPanel, Ext.grid.GridPanel, {

	autoSizeColumns: function() {
	    for (var i = 0; i < this.colModel.getColumnCount(); i++) {
    		this.autoSizeColumn(i);
	    }
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

