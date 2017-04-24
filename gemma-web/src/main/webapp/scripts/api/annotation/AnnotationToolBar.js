Ext.namespace('Gemma');

/**
 * Gemma.AnnotationToolBar constructor... annotationGrid is the grid that contains the annotations. config is a hash
 * with the following options:
 *
 * @param createHandler :
    *           a function with arguments (characteristic, id, callback ) where characteristic is the new characteristic to
 *           add, id is the 'owner' and callback is the function to be called when the characteristic has been added if
 *           this argument is not present, there will be no create button in the toolbar
 *
 * @param deleteHandler :
    *           a function with arguments ( ids, callback ) where ids is an array of characteristic ids to remove and
 *           callback is the function to be called when the characteristics have been removed if this argument is not
 *           present, there will be no delete button in the toolbar
 *
 * @param saveHandler :
    *           a function with arguments ( characteristics, callback ) where characteristics is an array of
 *           characteristics to update and callback is the function to be called when the characteristics have been
 *           updated if this argument is not present, there will be no save button in the toolbar
 */

Gemma.AnnotationToolBar = Ext.extend(Ext.Toolbar, {

    taxonId: null,
    showValidateButton: false,
    catNotNull: false,
    termNotNull: false,


    /**
     * @memberOf Gemma.AnnotationToolBar
     */
    initComponent: function () {

        if (this.annotationGrid.editable && !this.saveHandler) {
            this.saveHandler = CharacteristicBrowserController.updateCharacteristics;
        }

        var charComboOpts = {
            emptyText: 'Enter term',
            width: 140,
            taxonId: this.taxonId
        };

        if (this.charComboWidth) {
            charComboOpts.width = this.charComboWidth;
        }
        var categoryComboOpts = {
            emptyText: "Select a category",
            width: 130
        };
        if (this.categoryComboWidth) {
            categoryComboOpts.width = this.categoryComboWidth;
        }

        this.charCombo = new Gemma.CharacteristicCombo(charComboOpts);

        this.categoryCombo = new Gemma.CategoryCombo(categoryComboOpts);

        this.categoryCombo.on("change", function (combo) {
            if (combo.value) {
                this.charCombo.setCategory(combo.selectedTerm.term, combo.selectedTerm.uri);
                this.catNotNull = true;
                if (this.termNotNull) this.createButton.enable();
            } else {
                this.charCombo.setCategory(null, null);
                this.catNotNull = false;
                this.createButton.disable();
            }
        }, this);

        this.charCombo.on("change", function (combo) {
            if (combo.value) {
                this.termNotNull = true;
                if (this.catNotNull) this.createButton.enable();
            } else {
                this.termNotNull = false;
                this.createButton.disable();
            }
        }, this);

        this.descriptionField = new Ext.form.TextField({
            allowBlank: true,
            invalidText: "Enter a description",
            blankText: "Add a simple description",
            emptyText: "Description",
            width: 75
        });

        var panel = this;

        if (this.createHandler) {
            this.createButton = new Ext.Toolbar.Button({
                text: "create",
                tooltip: "Adds the new annotation",
                disabled: true,
                handler: function () {
                    var characteristic = panel.charCombo.getCharacteristic();
                    if (panel.addDescription) {
                        characteristic.description = panel.descriptionField.getValue();
                    }
                    panel.annotationGrid.loadMask.show();
                    panel.createHandler(characteristic, panel.annotationGrid.refresh.createDelegate(panel.annotationGrid));
                    panel.charCombo.reset();
                    panel.descriptionField.reset();
                    panel.termNotNull = false;
                    panel.createButton.disable();
                },
                scope: this
            });
        }

        if (this.deleteHandler) {
            this.deleteButton = new Ext.Toolbar.Button({
                text: "delete",
                tooltip: "Removes the selected annotation",
                disabled: true,
                handler: function () {
                    panel.deleteButton.disable();
                    panel.annotationGrid.loadMask.show();
                    panel.deleteHandler(
                        panel.annotationGrid.getSelectedIds(),
                        panel.annotationGrid.refresh.createDelegate(panel.annotationGrid));
                },
                scope: this
            });

        }

        if (this.saveHandler) {

            this.saveButton = new Ext.Toolbar.Button({
                text: "save",
                tooltip: "Saves the updated annotations",
                disabled: true,
                handler: function () {
                    panel.annotationGrid.loadMask.show();
                    panel.saveHandler(
                        panel.annotationGrid.getEditedCharacteristics(),
                        panel.annotationGrid.refresh.createDelegate(panel.annotationGrid));
                    panel.saveButton.disable();
                },
                scope: this
            });

            this.annotationGrid.on("afteredit", function (model) {
                panel.saveButton.enable();
            }.createDelegate(this, null, null));
        }
        Gemma.AnnotationToolBar.superclass.initComponent.call(this);
    },

    afterRender: function (l, r) {
        this.add(this.categoryCombo);
        this.addSpacer();
        this.add(this.charCombo);
        this.addSpacer();

        if (this.addDescription) {
            this.add(this.descriptionField);
        }

        if (this.createHandler) {
            this.add(this.createButton);
        }
        if (this.deleteHandler) {
            this.add(this.deleteButton);
        }
        if (this.saveHandler) {
            this.add(this.saveButton);
        }

        this.addFill();

        Gemma.AnnotationToolBar.superclass.afterRender.call(this, l, r);
    }

});