/*
 * GeneGrid GeneChooserToolbar. Widget for picking genes. Allows user to search for and select one or more genes from
 * the database. The selected genes are kept in a table which can be edited. This component is the top part of the
 * coexpression interface, but should be reusable.
 * 
 * Author : luke, paul
 */
Ext.namespace('Gemma');

/**
 * The maximum number of genes we allow users to put in at once.
 *
 * @type Number
 */
Gemma.MAX_GENES_PER_PASTE = 1000;

/**
 * Table of genes with toolbar for searching.
 *
 * @class GeneGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.GeneGrid = Ext.extend(Ext.grid.GridPanel, {

    collapsible: false,
    autoWidth: true,
    stateful: false,
    frame: true,
    title: "Genes",
    layout: 'fit',
    width: 400,
    height: 250,

    viewConfig: {
        forceFit: true,
        emptyText: "Multiple genes can be listed here"
    },
    autoScroll: true,
    columns: [
        {
            header: 'Symbol',
            toolTip: 'Gene symbol',
            dataIndex: 'officialSymbol',
            width: 75,
            sortable: true,
            renderer: function (value, metadata, record, row, col, ds) {
                return String.format("<a target='_blank' href='" + Gemma.CONTEXT_PATH + "/gene/showGene.html?id={0}'>{1}</a> ",
                    record.data.id, record.data.officialSymbol);
            }
        }, {
            id: 'desc',
            toolTip: 'Gene name',
            header: 'Name',
            dataIndex: 'officialName'
        }],
    autoExpandColumn: 'desc',

    /**
     * Add to table.
     *
     * @param {Array.Number}
     *           geneIds
     * @param {Function}
     *           callback
     * @param {Array}
     *           args
     */
    loadGenes: function (geneIds, callback, args) {
        if (!geneIds || geneIds.length === 0) {
            return;
        }

        GenePickerController.getGenes(geneIds, function (genes) {
            var geneData = [];
            for (var i = 0; i < genes.length; ++i) {
                geneData.push([genes[i].id, genes[i].taxonScientificName, genes[i].officialSymbol,
                    genes[i].officialName]);
            }
            /*
             * FIXME this can result in the same gene listed twice. This is taken care of at the server side but looks
             * funny.
             */
            this.getStore().loadData(geneData);

            if (callback) {
                callback(args);
            }
        }.createDelegate(this));
    },

    /**
     * Add geneValueObjects to grid.
     *
     * @param {Object}
     *           gvos must have fields for id, taxonScientificName, officialSymbol & officialName
     */
    addGeneValueObjects: function (gvos) {
        if (!gvos || gvos.length === 0) {
            return;
        }
        if (this.getEl()) {
            this.loadMask = new Ext.LoadMask(this.getEl(), {
                msg: Gemma.StatusText.Loading.genes,
                msgCls: 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
            });
            this.loadMask.show();
        }
        var geneData = [];
        for (var i = 0; i < gvos.length; ++i) {
            geneData.push([gvos[i].id, gvos[i].taxonScientificName, gvos[i].officialSymbol, gvos[i].officialName]);
        }
        this.getStore().loadData(geneData);
    },

    /**
     * @memberOf Gemma.GeneGrid
     */
    initComponent: function () {
        Ext.apply(this, {
            tbar: new Gemma.GeneChooserToolBar({
                geneGrid: this,
                extraButtons: this.extraButtons,
                style: "border: #a3bad9 solid 1px;"
            }),
            store: new Ext.data.SimpleStore({
                fields: [{
                    name: 'id',
                    type: 'int'
                }, {
                    name: 'taxon'
                }, {
                    name: 'officialSymbol',
                    type: 'string'
                }, {
                    name: 'officialName',
                    type: 'string'
                }],
                sortInfo: {
                    field: 'officialSymbol',
                    direction: 'ASC'
                }
            })
        });

        Gemma.GeneGrid.superclass.initComponent.call(this);

        this.addEvents('addgenes', 'removegenes');

        this.getTopToolbar().geneCombo.on("select", function () {
            this.fireEvent("addgenes");
        }, this);

        this.getStore().on("remove", function () {
            this.fireEvent("removegenes");
        }, this);

        this.getStore().on("add", function () {
            this.fireEvent("addgenes");
        }, this);

        this.on("keypress", function (e) {
            if (!this.getTopToolbar().disabled && e.getCharCode() === Ext.EventObject.DELETE) {
                this.removeGene();
            }
        }, this);

        // See http://www.extjs.com/learn/Tutorial:RelayEvents
        this.relayEvents(this.getTopToolbar(), ['ready', 'taxonchanged']);

        if (this.genes) {
            var genes = this.genes instanceof Array ? this.genes : this.genes.split(",");
            this.loadGenes(genes);
        }

    },

    removeGene: function () {
        var selected = this.getSelectionModel().getSelections();
        for (var i = 0; i < selected.length; ++i) {
            this.getStore().remove(selected[i]);
        }
        this.getSelectionModel().selectLastRow();
    },

    removeAllGenes: function () {
        this.getStore().removeAll();
    },

    record: Ext.data.Record.create([{
        name: 'id',
        type: 'int'
    }, {
        name: 'taxon'
    }, {
        name: 'officialSymbol',
        type: 'string'
    }, {
        name: 'officialName',
        type: 'string'
    }]),

    addGene: function (gene) {
        if (!gene) {
            return;
        }

        if (this.getStore().find("id", gene.id) < 0) {
            var Constructor = this.record;
            var record = new Constructor(gene);
            this.getStore().add([record]);
        }
    },

    /**
     * Given text, search Gemma for matching genes. Used to 'bulk load' genes from the GUI.
     *
     * @param {}
     *           e
     */
    getGenesFromList: function (e, taxon) {
        if (!taxon) {
            Ext.Msg.alert(Gemma.HelpText.CommonErrors.MissingInput.title,
                Gemma.HelpText.CommonErrors.MissingInput.text);
            return;
        }

        var loadMask = new Ext.LoadMask(this.getEl(), {
            msg: Gemma.StatusText.Loading.genes
        });
        loadMask.show();

        var taxonId = taxon.id;
        var text = e.geneNames;
        GenePickerController.searchMultipleGenes(text, taxonId, {
            callback: function (genes) {
                if (genes.length < 1 || (genes.length === 1 && genes[0] === null)) {
                    Ext.Msg.alert("Genes not found", "No genes matching your query and taxon found.");
                    loadMask.hide();
                    return;
                }
                var origLines = text.split(/[\r\n]+/).length;
                var nulls = origLines - genes.length;
                var geneData = [];
                var warned = false;
                var alreadyIn = 0;
                var totalIn = 0;
                for (var i = 0; i < genes.length; ++i) {
                    if (i >= Gemma.MAX_GENES_PER_QUERY) {
                        if (!warned) {
                            Ext.Msg.alert("Too many genes", "You can only search up to " + Gemma.MAX_GENES_PER_QUERY
                                + " genes, some of your selection will be ignored.");
                            warned = true;
                        }
                        break;
                    }

                    if (genes[i] === null) {
                        nulls = nulls + 1;
                    } else if (this.getStore().find("id", genes[i].id) < 0) {
                        geneData.push([genes[i].id, genes[i].taxonScientificName, genes[i].officialSymbol,
                            genes[i].officialName]);
                        totalIn++;
                    } else {
                        alreadyIn++;
                    }
                }
                this.getStore().loadData(geneData, true);
                loadMask.hide();
                if (alreadyIn > 0 || nulls > 0) {
                    var inStr = alreadyIn > 0 ? ("  " + alreadyIn + " out of " + origLines + "  genes skipped (already in the group). <br/>") : "";
                    var nullStr = nulls > 0 ? ("  " + nulls + " out of " + origLines + " gene identifiers not recognized (or do not exist on this taxon). ") : "";
                    Ext.Msg.alert("Genes loaded", totalIn + " out of " + origLines + " genes loaded. <br/>"
                        + inStr + nullStr);
                }

            }.createDelegate(this),

            errorHandler: function (e) {
                if (this.getEl && this.getEl()) {
                    this.getEl().unmask();
                }
                Ext.Msg.alert('There was an error', e);
            }
        });
    },

    getTaxonId: function () {
        return this.getTopToolbar().getTaxonId();
    },

    setGene: function (geneId, callback, args) {
        this.getTopToolbar().setGene(geneId, callback, args);
        this.fireEvent("addgenes", [geneId]);
    },

    /**
     *
     * @return {} list of all geneids currently held, including ones in the grid and possible one in the field.
     */
    getGeneIds: function () {
        var ids = [];
        var all = this.getStore().getRange();
        for (var i = 0; i < all.length; ++i) {
            ids.push(all[i].data.id);
        }
        var gene = this.getTopToolbar().geneCombo.getGene();
        if (gene) {
            for (var j = 0; j < ids.length; ++j) {
                // don't add twice.
                if (ids[j] === gene.id) {
                    return ids;
                }
            }
            ids.push(gene.id);
        }
        return ids;
    },

    taxonChanged: function (taxon) {
        this.getTopToolbar().taxonChanged(taxon, true);
    },

    // returns gene objects in an array
    // gene = {id, officialSymbol, officialName, taxon}
    getGenes: function () {

        var genes = [];
        var all = this.getStore().getRange();
        for (var i = 0; i < all.length; ++i) {
            genes.push(all[i].data);
        }
        var gene = this.getTopToolbar().geneCombo.getGene();
        if (gene) {
            for (var j = 0; j < genes.length; ++j) {
                // don't add twice.
                if (genes[j].id === gene.id) {
                    return genes;
                }
            }
            genes.push(gene);
        }
        return genes;

    }

});

Ext.reg('genechooser', Gemma.GeneGrid);

/**
 * Toolbar with taxon chooser and gene search field.
 *
 * @class Gemma.GeneChooserToolBar
 * @extends Ext.Toolbar
 */
Gemma.GeneChooserToolBar = Ext.extend(Ext.Toolbar, {

    name: "gctb",

    /**
     * Set value in combobox.
     *
     * @param {}
     *           geneId
     * @param {}
     *           callback
     * @param {}
     *           args
     *
     * @memberOf Gemma.GeneChooserToolBar
     */
    setGene: function (geneId, callback, args) {
        GenePickerController.getGenes([geneId], function (genes) {
            var g = genes[0];
            if (g) {
                this.geneCombo.setGene(g);
                this.geneCombo.setValue(g.officialSymbol);
                this.getStore().removeAll();
                this.addButton.enable();
            }
            if (callback) {
                callback(args);
            }
        }.createDelegate(this));
    },

    getTaxonId: function () {
        if (this.taxonCombo) {
            return this.taxonCombo.getValue();
        } else {
            return this.geneCombo.getTaxon().id;
        }
    },

    /**
     * Check if the taxon needs to be changed, and if so, update it for the genecombo and the taxonCombo.
     *
     * @param {}
     *           taxon
     */
    taxonChanged: function (taxon) {

        if (!taxon || (this.geneCombo.getTaxon() && this.geneCombo.getTaxon().id === taxon.id)) {
            return;
        }

        this.geneCombo.setTaxon(taxon);

        // clear any genes.
        var all = this.getStore().getRange();
        for (var i = 0; i < all.length; ++i) {
            if (all[i].get('taxonId') !== taxon.id) {
                this.getStore().remove(all[i]);
            }
        }

        this.taxonCombo.setTaxon(taxon);

        Gemma.EVENTBUS.fireEvent('taxonchanged', taxon.id);
        // this.fireEvent( "taxonchanged", taxon );
    },

    getTaxon: function () {
        return this.taxonCombo.getTaxon();
    },

    getGenesFromList: function (e) {
        this.geneGrid.getGenesFromList(e, this.getTaxon());
    },

    getStore: function () {
        return this.geneGrid.getStore();
    },

    initComponent: function () {

        Gemma.GeneChooserToolBar.superclass.initComponent.call(this);

        /*
         * The taxon combo and gene combo have to update each other. Also the taxon combo is stateful.
         */

        this.taxonCombo = new Gemma.TaxonCombo({
            isDisplayTaxonWithGenes: true,
            listeners: {
                'select': {
                    fn: function (cb, rec, index) {
                        this.taxonChanged(rec.data, false);
                    }.createDelegate(this)
                }
            }
        });

        this.geneCombo = new Gemma.GeneCombo({
            emptyText: 'Search for a gene',
            listeners: {
                'select': {
                    fn: function (combo, rec, index) {
                        if (rec.get) {
                            this.taxonCombo.setTaxon(rec.get("taxonId"));
                        } else {
                            this.taxonCombo.setTaxon(rec.taxonId);
                        }
                        this.addButton.enable();
                    }.createDelegate(this)
                }
            }
        });

        this.addButton = new Ext.Toolbar.Button({
            icon: Gemma.CONTEXT_PATH + "/images/icons/add.png",
            cls: "x-btn-icon",
            tooltip: "Add a gene to the list",
            disabled: true,
            handler: function () {
                this.geneGrid.addGene(this.geneCombo.getGene());
                this.geneCombo.reset();
                this.addButton.disable();
            }.createDelegate(this)
        });

        this.removeButton = new Ext.Toolbar.Button({
            icon: Gemma.CONTEXT_PATH + "/images/icons/subtract.png",
            cls: "x-btn-icon",
            tooltip: "Remove the selected gene from the list",
            disabled: true,
            handler: function () {
                this.geneGrid.removeGene();
                // this.removeButton.disable();
            }.createDelegate(this)
        });

        this.chooser = new Gemma.GeneImportPanel({
            listeners: {
                'commit': {
                    fn: this.getGenesFromList.createDelegate(this),
                    scope: this
                }
            }
        });

        this.multiButton = new Ext.Toolbar.Button({
            icon: Gemma.CONTEXT_PATH + "/images/icons/page_white_put.png",
            cls: "x-btn-icon",
            tooltip: "Import multiple genes",
            disabled: false,
            handler: function () {

                if (!this.getTaxon()) {
                    Ext.Msg.alert(Gemma.HelpText.CommonErrors.MissingInput.title,
                        Gemma.HelpText.CommonErrors.MissingInput.text);
                    return;
                }

                this.geneCombo.reset();
                this.addButton.enable();
                this.chooser.show();
            }.createDelegate(this, [], true)
        });

        /*
         * code down here has to be called after the super-constructor so that we know we're a grid...
         */
        this.geneGrid.getSelectionModel().on("selectionchange", function (model) {
            var selected = model.getSelections();
            if (selected.length > 0) {
                this.removeButton.enable();
            } else {
                this.removeButton.disable();
            }
        }.createDelegate(this));

        this.relayEvents(this.taxonCombo, ['ready']);
    },

    afterRender: function (c, l) {
        Gemma.GeneChooserToolBar.superclass.afterRender.call(this, c, l);

        this.add(this.taxonCombo);
        this.addSpacer();
        this.add(this.geneCombo, this.addButton);
        this.addSpacer();
        this.add(this.removeButton);
        this.addSpacer();
        this.add(this.multiButton);

        if (this.extraButtons) {
            for (var i = 0; i < this.extraButtons.length; i++) {
                this.addSpacer();
                this.add(this.extraButtons[i]);
            }
        }

    }

});

/**
 * pop-up to put in multiple genes.
 *
 * @class Gemma.GeneImportPanel
 * @extends Ext.Window
 */
Gemma.GeneImportPanel = Ext.extend(Ext.Window, {

    title: "Import multiple genes (one symbol or NCBI id per line, up to " + Gemma.MAX_GENES_PER_PASTE + ")",
    modal: true,
    layout: 'fit',
    stateful: false,
    autoHeight: false,
    width: 350,
    height: 300,
    closeAction: 'hide',
    easing: 3,
    showTaxonCombo: false,

    /**
     * @memberOf Gemma.GeneImportPanel
     */
    onCommit: function () {
        if (this.showTaxonCombo
            && (typeof this._taxonCombo.getTaxon() === 'undefined' || isNaN(this._taxonCombo.getTaxon().id))) {
            this._taxonCombo.markInvalid("This field is required");
            return;
        }
        this.hide();

        var geneList = this._geneText.getValue();

        if (geneList.length > 0) {
            this.fireEvent("commit", {
                geneNames: geneList
            });

        }
    },

    initComponent: function () {

        this.addEvents({
            "commit": true
        });

        if (this.showTaxonCombo) {
            Ext.apply(this, {
                layout: 'form',
                width: 420,
                height: 400,
                padding: 10,
                items: [
                    {
                        xtype: 'taxonCombo',
                        ref: '_taxonCombo',
                        emptyText: 'Select a taxon (required)',
                        fieldLabel: 'Select a taxon',
                        width: 250,
                        msgTarget: 'side',
                        isDisplayTaxonWithGenes: true
                    },
                    {
                        xtype: 'textarea',
                        ref: '_geneText',
                        fieldLabel: String.format(Gemma.HelpText.WidgetDefaults.GeneImportPanel.instructions,
                            Gemma.MAX_GENES_PER_QUERY),
                        width: 250,
                        height: 290
                    }]
            });
        } else {
            Ext.apply(this, {
                items: [{
                    id: 'gene-list-text',
                    xtype: 'textarea',
                    ref: '_geneText',
                    fieldLabel: String.format(Gemma.HelpText.WidgetDefaults.GeneImportPanel.instructions,
                        Gemma.MAX_GENES_PER_QUERY),
                    width: 290,
                    height: 290
                }]
            });
        }
        Ext.apply(this, {
            buttons: [{
                text: 'OK',
                handler: this.onCommit,
                scope: this
            }, {
                text: 'Clear',
                scope: this,
                handler: function () {
                    this._geneText.setValue("");
                }
            }, {
                text: 'Cancel',
                handler: function () {
                    this.hide();
                }.createDelegate(this),
                scope: this
            }]
        });

        Gemma.GeneImportPanel.superclass.initComponent.call(this);
    }

});
