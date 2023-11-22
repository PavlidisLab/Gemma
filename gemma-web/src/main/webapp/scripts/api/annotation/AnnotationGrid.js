Ext.namespace('Gemma');

/**
 * The 'Characteristic browser' grid, also used for the basic Annotation view.
 *
 * Gemma.AnnotationGrid constructor... div is the name of the div in which to render the grid. config is a hash with the
 * following options:
 *
 * readMethod : the DWR method that returns the list of AnnotationValueObjects ( e.g.:
 * ExpressionExperimentController.getAnnotation )
 *
 * readParams : an array of parameters that will be passed to the readMethod ( e.e.: [ { id:x,
 * classDelegatingFor:"ExpressionExperiment" } ] ) or a pointer to a function that will return the array of
 * parameters
 *
 * editable : if true, the annotations in the grid will be editable
 *
 * showParent : if true, a link to the parent object will appear in the grid
 *
 * noInitialLoad : if true, the grid will not be loaded immediately upon creation
 *
 *
 * writeMethod : function pointer to server side ajax call to add an annotation eg
 *
 * removeMethod :function pointer to server side ajax call to remove an annotation
 *
 * entId : the entity that the annotations belong to eg) eeId or bmId
 *
 */

/**
 * Shows tags in a simple row of links.
 *
 * @class Gemma.AnnotationDataView
 * @extends Ext.DataView
 */
Gemma.AnnotationDataView = Ext
    .extend(
        Ext.DataView,
        {

            readMethod: ExpressionExperimentController.getAnnotation,

            record: Ext.data.Record.create([{
                name: "id",
                type: "int"
            }, {
                name: "classUri"
            }, {
                name: "className"
            }, {
                name: "termUri"
            }, {
                name: "termUriEsc",
                mapping: "termUri",
                convert: function (v) {
                    return encodeURIComponent(v);
                }
            }, {
                name: "termName"
            }, {
                name: "evidenceCode"
            }, {
                name: "objectClass"
            }]),

            /**
             * @memberOf Gemma.AnnotationDataView
             */
            getReadParams: function () {
                return (typeof this.readParams == "function") ? this.readParams() : this.readParams;
            },

            /*
             * For display of tags on expressionexperimentdetails page
             * Make it easier to spot which ones come from BioMaterial, FactorValue
             */
            tpl: new Ext.XTemplate(
                '<tpl for=".">',
                '<span class="ann-wrap" ext:qtip="{className}" >',
                '<tpl if="objectClass == \'BioMaterial\'">',
                '<span class="fromBioMaterial">',
                '</tpl>',
                '<tpl if="objectClass == \'FactorValue\'">',
                '<span class="fromFactorValue">',
                '</tpl>',
                '<tpl if="objectClass == \'ExperimentTag\'">',
                '<span class="fromExperimentTag">',
                '</tpl>',
                '<a ext:qtip="{className} : {termUri} via {objectClass}" href="' + gemBrowUrl + '/#/q/{termUriEsc}" style="text-decoration:underline;">',
                '{termName}',
                '</a>',
                '</span>',
                '</span>&nbsp;&nbsp;',
                '</tpl>'),


            itemSelector: 'ann-wrap',
            emptyText: 'No tags',

            initComponent: function () {

                Ext.apply(this, {
                    store: new Ext.data.Store({
                        proxy: new Ext.data.DWRProxy(this.readMethod),
                        reader: new Ext.data.ListRangeReader({
                            id: "id"
                        }, this.record) ,
                        // to keep grouped by type
                        sortInfo: {
                            field: 'objectClass',
                            direction: 'ASC'
                        }
                    })
                });

                Gemma.AnnotationDataView.superclass.initComponent.call(this);

                this.store.load({
                    params: this.getReadParams()
                });
            }

        });

/**
 *
 * @class Gemma.AnnotationGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.AnnotationGrid = Ext.extend(Gemma.GemmaGridPanel, {

    // autoHeight : true,
    width: 500,
    height: 200,
    stateful: false,
    taxonId: null,
    name: 'AnnotationGrid',

    viewConfig: {
        enableRowBody: true,
        emptyText: 'No annotations',
        showDetails: false,
        getRowClass: function (record, index, p, store) {
            if (this.showDetails) {
                p.body = "<p class='characteristic-body' >" + String.format("From {0}", record.data.parentOfParentLink)
                    + "</p>";
            }

            if (this.grid.editable && !this.grid.showParent && record.get("objectClass") !== "ExperimentTag" ) {
                return 'disabled';
            } else {
                return '';
            }
        }
    },

    loadMask: {
        msg: Gemma.StatusText.processing,
        store: this.store
    },

    forceValidation: true, // always fire edit event even if text doesn't change - the url might have. But this
    // isn't good enough, ext has a limitation.

    useDefaultToolbar: true,

    record: Ext.data.Record.create([{
        name: "id",
        type: "int"
    }, {
        name: "objectClass"
    }, {
        name: "classUri"
    }, {
        name: "className"
    }, {
        name: "termUri"
    }, {
        name: "termName"
    }, {
        name: "parentLink"
    }, {
        name: "parentDescription"
    }, {
        name: "parentOfParentLink"
    }, {
        name: "parentOfParentDescription"
    }, {
        name: "evidenceCode"
    }]),

    parentStyler: function (value, metadata, record, row, col, ds) {
        return this.formatParentWithStyle(record.id, record.expanded, record.data.parentLink,
            record.data.parentDescription, record.data.parentOfParentLink, record.data.parentOfParentDescription);
        // return parentLink;
    },

    formatParentWithStyle: function (id, expanded, parentLink, parentDescription, parentOfParentLink,
                                     parentOfParentDescription) {
        var value;
        value = (parentLink ? (parentLink + "&nbsp;&nbsp;") : "") + (parentDescription ? parentDescription : "");

        if (parentOfParentLink) {
            value = value + "&nbsp;&laquo;&nbsp;" + parentOfParentLink;
        }

        // }
        return expanded ? value
            .concat(String.format("<div style='white-space: normal;'>{0}</div>", parentDescription)) : value;
    },

    termStyler: function (value, metadata, record, row, col, ds) {
        return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.data.termUri);
    },

    categoryStyler: function (value, metadata, record, row, col, ds) {
        return Gemma.GemmaGridPanel.formatTermWithStyle(value, record.data.classUri);
    },

    termUriStyler: function (value, metadata, record, row, col, ds) {
        if (record.get('termUri')) {
            return String.format("<a target='_blank' href='{0}'>{0}</a>", record.get('termUri'));
        }
        return '';
    },

    initComponent: function () {

        Ext.apply(this, {
            columns: [{
                header: "Category",
                dataIndex: "className",
                renderer: this.categoryStyler.createDelegate(this),
                sortable: true
            }, {
                header: "Term",
                dataIndex: "termName",
                renderer: this.termStyler.createDelegate(this),
                sortable: true
            }, {
                header: "Term URI",
                dataIndex: "termUri",
                hidden: true,
                renderer: this.termUriStyler.createDelegate(this),
                sortable: true
            }, {
                header: "Annotation belongs to:",
                dataIndex: "parentLink",
                renderer: this.parentStyler.createDelegate(this),
                tooltip: Gemma.HelpText.WidgetDefaults.AnnotationGrid.parentLinkDescription,
                hidden: this.showParent ? false : true,
                sortable: true
            }, {
                header: "Evidence",
                dataIndex: "evidenceCode",
                sortable: true
            }, {
                header: "From",
                dataIndex: "objectClass",
                sortable: true,
                hidden: this.showParent? true : false,
                tooltip: Gemma.HelpText.WidgetDefaults.AnnotationGrid.objectClassDescription
            }]

        });

        if (this.store == null) {
            Ext.apply(this, {
                store: new Ext.data.Store({
                    proxy: new Ext.data.DWRProxy(this.readMethod),
                    reader: new Ext.data.ListRangeReader({
                        id: "id"
                    }, this.record)
                })
            });
        }

        if (this.editable && this.useDefaultToolbar) {
            Ext.apply(this, {
                tbar: new Gemma.AnnotationToolBar({
                    annotationGrid: this,
                    showValidateButton: true,
                    createHandler: function (characteristic, callback) {
                        this.writeMethod(characteristic, this.entId, callback);
                    }.createDelegate(this),
                    deleteHandler: function (ids, callback) {
                        this.removeMethod(ids, this.entId, callback);
                    }.createDelegate(this),
                    taxonId: this.taxonId
                })
            });
        }
        //
        Gemma.AnnotationGrid.superclass.initComponent.call(this);

        this.getStore().setDefaultSort('className');

        this.loadMask.store = this.getStore();

        this.autoExpandColumn = this.showParent ? 2 : 1;

        this.getColumnModel().defaultSortable = true;

        if (this.editable) {

            /*
             * Display all the edit functions.
             */

            var CATEGORY_COLUMN = 0;
            var VALUE_COLUMN = 1;
            var VALUE_URI_COLUMN = 2;
            var PARENT_COLUMN = 3;
            var EVIDENCE_COLUMN = 4;

            // Category setup
            this.categoryCombo = new Gemma.CategoryCombo({
                lazyRender: true
            });
            var categoryEditor = new Ext.grid.GridEditor(this.categoryCombo);
            this.categoryCombo.on("select", function (combo, record, index) {
                categoryEditor.completeEdit();
            });
            this.getColumnModel().setEditor(CATEGORY_COLUMN, categoryEditor);

            // Value setup
            this.valueCombo = new Gemma.CharacteristicCombo({
                taxonId: this.taxonId
            });
            var valueEditor = new Ext.grid.GridEditor(this.valueCombo);
            this.valueCombo.on("select", function (combo, record, index) {
                valueEditor.completeEdit();
            });
            this.getColumnModel().setEditor(VALUE_COLUMN, valueEditor);

            // Evidence setup
            this.evidenceCombo = new Gemma.EvidenceCodeCombo({
                lazyRender: true
            });
            var evidenceEditor = new Ext.grid.GridEditor(this.evidenceCombo);
            this.evidenceCombo.on("select", function (combo, record, index) {
                evidenceEditor.completeEdit();
            });
            this.getColumnModel().setEditor(EVIDENCE_COLUMN, evidenceEditor);

            this.on("beforeedit", function (e) {
                var row = e.record.data;

                // this applies to the mini-view on experiment pages
                if (!this.showParent && e.record.get("objectClass") !== "ExperimentTag" ) {
                    return false;
                }

                var col = this.getColumnModel().getColumnId(e.column);
                if (col == VALUE_COLUMN) {
                    this.valueCombo.setCategory.call(this.valueCombo, row.className, row.classUri);
                }

                 return true;
            });

            this.on("afteredit", function(e) {
                if (e.column == VALUE_COLUMN) {
                    var c = this.valueCombo.getCharacteristic();
                    e.record.set("term", c.value);
                    e.record.set("termUri", c.valueUri);
                } else if (e.column == CATEGORY_COLUMN) {
                   e.record.set("className", this.categoryCombo.getTerm().term);
                   e.record.set("classUri", this.categoryCombo.getTerm().uri);
                }

            });

            if (this.getTopToolbar().deleteButton) {
                this.getSelectionModel().on("selectionchange", function (model) {
                    var selected = model.getSelections();
                    if (selected.length > 0) {
                        this.getTopToolbar().deleteButton.enable();
                    } else {
                        this.getTopToolbar().deleteButton.disable();
                    }
                }, this);
            }
        }

        this.on("celldblclick", function (grid, rowIndex, cellIndex) {
            var record = grid.getStore().getAt(rowIndex);
            var column = grid.getColumnModel().getColumnId(cellIndex);
            if (column == PARENT_COLUMN) {
                record.expanded = record.expanded ? 0 : 1;
                grid.getView().refresh(true);
            }
        }, this);

        if (!this.noInitialLoad) {
            this.getStore().load({
                params: this.getReadParams()
            });
        }
    },

    getReadParams: function () {
        return (typeof this.readParams == "function") ? this.readParams() : this.readParams;
    },

    getSelectedCharacteristics: function () {
        var selected = this.getSelectionModel().getSelections();
        var chars = [];
        for (var i = 0; i < selected.length; ++i) {
            var row = selected[i].data;
            chars.push(row);
        }
        return chars;
    },

    getEditedCharacteristics: function () {
        var chars = [];
        this.getStore().each(function (record) {
            if (record.dirty) {
                var row = record.data;
                chars.push(row);
            }
        }.createDelegate(this), this);
        return chars;
    },

    setEEId: function (id) {
        this.eeId = id;
    }

});