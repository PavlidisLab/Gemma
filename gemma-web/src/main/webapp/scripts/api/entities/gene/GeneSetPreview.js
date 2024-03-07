/**
 *
 * @author thea
 *
 */
Ext.namespace('Gemma');

/**
 *
 * Displays a small number of elements from the set with links to the set's page and to an editor. This is messed
 * up (misnamed?) because it manages the preview, but also deals with keeping track of the actual gene set via the
 * _addToPreviewedSet. The actual preview is shown in previewContent.
 *
 * @class Gemma.GeneSetPreview
 * @xtype Gemma.GeneSetPreview
 */
Gemma.GeneSetPreview = Ext.extend(Gemma.SetPreview, {

    /**
     * Fetch some genes as examples.
     *
     * @private
     *
     * @param {Number[]}
     *           ids an array of geneIds to use to populate preview
     * @memberOf Gene.GeneSetPreview
     */
    _loadGenePreviewFromIds: function (ids, message) {

        // load some genes to display
        var limit = (ids.length < this.preview_size) ? ids.length : this.preview_size;
        var previewIds = ids.slice(0, limit);
        GenePickerController.getGenes(previewIds, {
            callback: function (genes) {
                this.loadPreview(genes, ids.length, message);
                // this.updateTitle(); // this is extra.
                this.fireEvent('previewLoaded', genes);
            }.createDelegate(this),
            errorHandler: Gemma.genericErrorHandler
        });
    },

    reset: function () {
        this.resetPreview();
        this.previewContent.setTitle(null);
        this.setSelectedSetValueObject(null);
    },

    /**
     * @public update the contents of the gene preview box with a given geneSet.
     *
     * @param {GeneValueSetObject[]}
     *           geneSet populate preview with members
     * @param {String}
     *           message an extra message to show.
     */
    loadGenePreviewFromGeneSet: function (geneSet, message) {

        var ids = geneSet.geneIds;

        if (ids.length > 0) {
            this._loadGenePreviewFromIds(ids, message);
        } else if (geneSet.id > 0) {
            // fetch from server.
            GeneSetController.getGenesInGroup.apply(this, [geneSet.id, this.preview_size, {
                callback: function (genes) {
                    this.loadPreview(genes, this.selectedSetValueObject.size, message);
                    // this.updateTitle(); // this is extra.
                    this.fireEvent('previewLoaded', genes);
                }.createDelegate(this),
                errorHandler: Gemma.genericErrorHandler
            }]);
        } else {
            alert("Could not load");
        }

    },

    /**
     * This is called by SetPreview.setSelectedSetValueObject - a little odd.
     */
    updateTitle: function () {
        // debugger;
        var selectedSet = this.selectedSetValueObject;

        if (!selectedSet) {
            this.previewContent.setTitle('<span>' + 'No selection'
                + '</span> &nbsp;&nbsp;<span style="font-weight:normal">(0 genes)');
            return;
        }

        var size = selectedSet.size > 0 ? selectedSet.size : selectedSet.geneIds.length;

        if (selectedSet instanceof DatabaseBackedGeneSetValueObject) {

            name = "<a target=\"_blank\" href=\"" + Gemma.LinkRoots.geneSetPage + selectedSet.id + '">' + selectedSet.name
                + '</a>';

        } else if (selectedSet instanceof PhenotypeGroupValueObject) {

            name = "<a target=\"_blank\" href=\"" + Gemma.LinkRoots.phenotypePage + selectedSet.phenotypeName + '">'
                + selectedSet.name + ": " + selectedSet.description + '</a>';

        } else if (selectedSet instanceof GOGroupValueObject) {
            name = selectedSet.name + ": " + selectedSet.description;
        } else if (selectedSet instanceof PhenotypeGroupValueObject) {
            name = selectedSet.name + ": " + selectedSet.description;
        } else {
            name = selectedSet.name;
        }

        this.previewContent.setTitle('<span style="font-size:1.2em">' + name
            + '</span> &nbsp;&nbsp;<span style="font-weight:normal">(' + size + ((size > 1) ? " genes)" : " gene)"));
    },

    /**
     * Given the current selection, when the user selects another result from the combo: we merge it in.
     * this from the preview proper.
     *
     * @private
     * @param combo
     * @param record
     * @param index
     * @returns
     */
    _addToPreviewedSet: function (combo, record, index) {

        var o = record.get('resultValueObject');

        if (o instanceof GeneValueObject) { // see valueObjectsInheritanceStructure.
            this._appendAndUpdate([o.id]); // note that we have the gene, so doing this by ID is a bit wasteful.
        } else if (o instanceof GeneSetValueObject) {
            if (o.geneIds && o.geneIds.length > 0) {
                /*
                 * We have the Gene IDs, no need to fetch them again.
                 */
                var newIds = o.geneIds;
                this._appendAndUpdate(newIds);

            } else {
                /*
                 * Add the genes from the set to the current set (not sure this case is used!)
                 */
                console.log("Warning: gene set on client without gene ids, but it has an id");
                GeneSetController.load(o.id, function (fetched) {
                    this._appendAndUpdate(o.geneIds);
                }.createDelegate(this));
            }
        } else {
            throw 'Cannot add to preview from this type of object: ' + o.constructor.name;
        }

    },

    /**
     * Given gene ids, add them to the current group. If the current group is already a 'temporary' one, then just add
     * them. If the current group is a database-backed one, make a session-bound group that is based on the original.
     *
     *
     * @private
     * @param geneIdsToAdd
     *           {Array}
     */
    _appendAndUpdate: function (geneIdsToAdd) {
        var allIds = this.selectedSetValueObject.geneIds;

        // don't add duplicates
        var added = false;
        for (var i = 0; i < geneIdsToAdd.length; i++) {
            if (allIds.indexOf(geneIdsToAdd[i]) < 0) {
                allIds.push(geneIdsToAdd[i]);
                added = true;
            }
        }

        this.withinSetGeneCombo.reset();
        this.withinSetGeneCombo.blur();

        if (!added) {
            return;
        }

        this._loadGenePreviewFromIds(allIds); // async

        /*
         * if the current selection is just a session group, don't create a new one.
         */
        if (this.selectedSetValueObject instanceof SessionBoundGeneSetValueObject) {

            var editedGroup = this.selectedSetValueObject;
            editedGroup.modified = true;
            editedGroup.geneIds = allIds;
            editedGroup.size = editedGroup.geneIds.length; // not really necessary

            GeneSetController.updateSessionGroup(editedGroup, {
                callback: function (geneSet) {
                    this.setSelectedSetValueObject(geneSet);
                    // this.updateTitle();

                    this.fireEvent('geneListModified', geneSet);
                    this.fireEvent('doneModification');

                }.createDelegate(this),
                errorHandler: Gemma.genericErrorHandler
            });

        } else /* the previous selection was another type of set - dbbound */{
            var editedGroup = new SessionBoundGeneSetValueObject();
            editedGroup.id = null;

            // Make a new set.
            if (!(this.selectedSetValueObject.name.lastIndexOf("Modification of:\n") === 0)) {
                var currentTime = new Date();
                var hours = currentTime.getHours();
                var minutes = currentTime.getMinutes();
                var time = '(' + hours + ':' + minutes + ') ';
                editedGroup.name = "Modification of:\n" + this.selectedSetValueObject.name;
                editedGroup.description = "You created this set by combining multiple items. Starting point was:\n"
                    + this.selectedSetValueObject.name + " (at " + time + ")";
            } else {
                editedGroup.name = this.selectedSetValueObject.name;
                editedGroup.description = this.selectedSetValueObject.description;
            }

            editedGroup.geneIds = allIds;
            editedGroup.taxonId = this.selectedSetValueObject.taxonId;
            editedGroup.taxonName = this.selectedSetValueObject.taxonName;
            editedGroup.size = editedGroup.geneIds.length;
            editedGroup.modified = true;
            editedGroup.isPublic = false;

            GeneSetController.addSessionGroup(editedGroup, true, {
                callback: function (geneSet) {
                    this.setSelectedSetValueObject(geneSet);
                    this.updateTitle();
                    this.fireEvent('geneListModified', geneSet);
                    this.fireEvent('doneModification');

                }.createDelegate(this),
                errorHandler: Gemma.genericErrorHandler
            });
        }
    },

    /**
     * @override
     */
    initComponent: function () {

        /*
         * Combo box hidden until the user selects some genes; this is how they add more genes using a search. They can
         * also click on "edit or save your set", which goes to the geneSelectionEditor.
         */
        this.withinSetGeneCombo = new Gemma.GeneAndGeneGroupCombo({
            width: 300,
            style: 'margin:10px',
            hideTrigger: true,
            taxonId: this.taxonId,
            emptyText: 'Add genes to your group'
        });
        this.withinSetGeneCombo.setTaxonId(this.taxonId);
        this.withinSetGeneCombo.on('select', this._addToPreviewedSet.createDelegate(this), this);

        /*
         * The gene set editor.
         */
        Ext.apply(this, {
            selectionEditor: new Gemma.GeneMembersSaveGrid({
                name: 'geneSelectionEditor',
                hideHeaders: true,
                width: 500,
                height: 500,
                frame: false
            }),
            defaultTpl: new Ext.Template('<div style="padding-bottom:7px;">'
                + '<a target="_blank" href="' + ctxBasePath + '/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName} '
                + '<span style="color:grey">({taxonCommonName})</span></div>'),

            defaultPreviewTitle: "Gene Selection Preview",

            addingCombo: this.withinSetGeneCombo

        });

        Gemma.GeneSetPreview.superclass.initComponent.call(this);

        this.selectionEditor.on('geneListModified', function (geneset) {
            // debugger;
            if (typeof geneset.geneIds !== 'undefined' && typeof geneset.name !== 'undefined') {
                this.setSelectedSetValueObject(geneset);
                this.updateTitle();
                this._loadGenePreviewFromIds(geneset.geneIds);
            }
            this.fireEvent('geneListModified', geneset);
        }, this);

    }

});

Ext.reg('Gemma.GeneSetPreview', Gemma.GeneSetPreview);