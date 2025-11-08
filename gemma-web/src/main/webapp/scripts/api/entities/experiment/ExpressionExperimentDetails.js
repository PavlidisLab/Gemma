Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

const NUMBER_FORMATTER = Intl.NumberFormat();

let htmlEncode = Ext.util.Format.htmlEncode;

function formatNumber( n ) {
    return NUMBER_FORMATTER.format( n );
}

/**
 *
 * Panel containing the most interesting info about an experiment. Used as one tab of the EE page
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Ext.Panel
 *
 */
Gemma.ExpressionExperimentDetails = Ext
    .extend(
        Ext.Panel,
        {

            dirtyForm: false,
            listeners: {
                leavingTab: function () {
                    if (this.editModeOn && this.dirtyForm) {
                        return confirm("You are still in edit mode. Your unsaved changes will be discarded when you switch tabs. Do you want to continue?");
                    }
                    return true;
                },
                tabChanged: function () {
                    this.fireEvent('toggleEditMode', false);
                }
            },

            /**
             * @memberOf Gemma.ExpressionExperimentDetails
             */
            renderArrayDesigns: function (ee) {
                const arrayDesigns = ee.arrayDesigns;
                let result = '';
                for ( let i = 0; i < arrayDesigns.length; i++ ) {
                    const ad = arrayDesigns[i];
                    result = result + '<a href="' + Gemma.CONTEXT_PATH + '/arrays/showArrayDesign.html?id=' + ad.id + '">' + ad.shortName
                        + '</a> - ' + ad.name;

                    if (arrayDesigns[i].troubled) {
                        result = result + ' <i class="red fa fa-exclamation-triangle fa-lg" ext:qtip="'
                            + arrayDesigns[i].troubleDetails + '"></i>';
                    }

                    if (arrayDesigns[i].blackListed) {
                        result = result + ' <i class="black fa fa-exclamation-triangle fa-lg" ext:qtip="Blacklisted platform"></i>';
                    }

                    if (i < arrayDesigns.length - 1) {
                        result = result + "<br/>";
                    }

                    if (ee.isRNASeq) {
	                    result = result + "&nbsp;(RNA-seq)"
                    }

                    if (ee.originalPlatforms.length > 0) {
	                     result = result + "<br/>As originally submitted: ";
                        for ( let j = 0; j < ee.originalPlatforms.length; j++ ) {
                            const op = ee.originalPlatforms[j];
                            result = result + '<a href="' + Gemma.CONTEXT_PATH + '/arrays/showArrayDesign.html?id=' + op.id + '>' + op.shortName + '</a> - ' + op.name;
		                     if (j < ee.originalPlatforms.length - 1) {
		                         result = result + ', '
                             }
	                     }
                    }
                }

                if (ee.lastArrayDesignUpdateDate) {
                    result += "<div class='dark-gray v-padded'>The last time a platform associated with this experiment was updated: "
                        + Gemma.Renderers.dateTimeRenderer(ee.lastArrayDesignUpdateDate) + "</div>"
                }

                return result;
            },
            renderCoExpressionLinkCount: function (ee) {

                if (!ee.hasCoexpressionAnalysis) {
                    return "Unavailable"; // analysis not run.
                }

                const downloadCoExpressionDataLink = String.format(
                   "<span style='cursor:pointer'  ext:qtip='Download all co-expression  data in a tab-delimited format.'  "
                   + "onClick='Gemma.ExpressionExperimentDataFetch.fetchCoExpressionData({0})' > &nbsp; <i class='fa fa-download'></i>  &nbsp; </span>",
                    ee.id);

                return "Available" + downloadCoExpressionDataLink;
            },

            renderSourceDatabaseEntry: function (ee) {
                if ( ee.externalDatabase === 'GEO' ) {
                    let acc = ee.accession;
                    acc = acc.replace( /\.[1-9]$/, '' );
                    // in case of multi-species.
                    const logo = Gemma.CONTEXT_PATH + '/images/logo/geoTiny.png';
                    return '<a target="_blank" href="https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=' + acc
                       + '"><img src="' + logo + '" alt="GEO logo"/></a>';

                } else if ( ee.externalDatabase === 'ArrayExpress' ) {
                    const logo = Gemma.CONTEXT_PATH + '/images/logo/arrayExpressTiny.png';
                    return '<a target="_blank" href="https://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession='
                       + ee.accession + '"><img src="' + logo + '" alt="ArrayExpress logo"/></a>';
                } else {
                    return "Direct upload";
                }
            },

            /**
             * Link for samples details page.
             *
             */
            renderSamples: function (ee) {
                let result = formatNumber( ee.bioAssayCount );
                if (this.editable) {
                    result = result
                        + '&nbsp;&nbsp<a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id='
                       + ee.id
                       + '"><img ext:qtip="View the details of the samples" src="' + Gemma.CONTEXT_PATH + '/images/icons/magnifier.png" alt="Loupe icon"/></a>';
                }
                return '' + result; // hack for possible problem with extjs 3.1 - bare
                // number not displayed, coerce to string.
            },

            /**
             *
             * @param {ExpressionExperimentDetailsValueObject} ee
             * @returns {string}
             */
            renderSingleCellMetadata : function( ee ) {
                let result = '';
                if ( ee.singleCellDimension !== null ) {
                    result += '<b>Number of cells:</b> ' + formatNumber( ee.singleCellDimension.numberOfCells ) + '<br/>';
                    if ( ee.singleCellDimension.cellTypeAssignments.length > 0 ) {
                        result += '<b>Cell type assignments:</b>';
                        result += '<ul style="padding-left: 20px;">'
                        for ( let cta of ee.singleCellDimension.cellTypeAssignments ) {
                            result += '<li>'
                            const name = cta.name || cta.protocol?.name || 'Unnamed';
                            if ( ee.hasCellBrowser && cta.id in ee.cellBrowserCellTypeAssignmentMetaNamesMap ) {
                                const metaId = ee.cellBrowserCellTypeAssignmentMetaNamesMap[cta.id]
                                result += ' <a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showCellBrowser.html?id=' + ee.id + '&meta=' + encodeURIComponent( metaId ) + '" target="_blank" ext:qtip="View ' + htmlEncode( name ) + ' in Cell Browser.">' + htmlEncode( name ) + '</a>' + ':';
                            } else {
                                result += '<b>' + htmlEncode( name ) + ':</b> '
                            }
                            result += ' <span ext:qtip="Possible values: ' + htmlEncode( cta.cellTypes.map( ct => ct.value ).join( ', ' ) ) + '.">' + cta.cellTypes.length + ' cell types</span>';
                            // result += '<span>' + cta.cellTypes.map( ct => ct.value ).join( ', ' ) + '</span>';
                            // if ( cta.protocol !== null ) {
                            //     result += ' <b>Protocol:</b> ' + cta.protocol.name;
                            // }
                            if ( cta.numberOfAssignedCells < ee.singleCellDimension.numberOfCells ) {
                                result += '; ' + formatNumber( cta.numberOfAssignedCells ) + ' cells are assigned a value';
                            }
                            if ( cta.preferred ) {
                                result += ' <b ext:qtip="This cell type annotation is marked as preferred and will be used for creating pseudo-bulks.">[Preferred]</b>';
                            }
                            result += ' <a href="' + Gemma.CONTEXT_PATH + '/rest/v2/datasets/' + ee.id + '/cellTypeAssignment?quantitationType=' + ee.singleCellQuantitationType.id + '&cellTypeAssignment=' + cta.id + '&download=true" ext:qtip="Download ' + htmlEncode( name ) + ' in a tab-delimited format.">Download</a>';
                            result += '</li>';
                        }
                        result += '</ul>'
                    }
                    if ( ee.singleCellDimension.cellLevelCharacteristics.length > 0 ) {
                        result += '<b>Cell-level characteristics:</b>';
                        result += '<ul style="padding-left: 20px;">'
                        for ( let clc of ee.singleCellDimension.cellLevelCharacteristics ) {
                            result += '<li>'
                            const name = clc.name || clc.category || 'Unnamed';
                            result += '<b>' + htmlEncode( name ) + ':</b>';
                            result += ' ' + '<span ext:qtip="Possible values: ' + htmlEncode( clc.characteristics.map( ct => ct.value ).join( ', ' ) ) + '.">' + formatNumber( clc.characteristics.length ) + ' values</span>';
                            if ( clc.numberOfAssignedCells < ee.singleCellDimension.numberOfCells ) {
                                result += '; ' + formatNumber( clc.numberOfAssignedCells ) + ' cells are assigned a value';
                            }
                            if ( ee.hasCellBrowser && clc.id in ee.cellBrowserCellLevelCharacteristicsMetaNamesMap ) {
                                const metaId = ee.cellBrowserCellLevelCharacteristicsMetaNamesMap[clc.id]
                                result += ' <a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showCellBrowser.html?id=' + ee.id + '&meta=' + encodeURIComponent( metaId ) + '" target="_blank"  ext:qtip="View ' + htmlEncode( name ) + ' in Cell Browser.">View</a>';
                            }
                            // TODO: result += ' <a href="' + Gemma.CONTEXT_PATH + '/rest/v2/datasets/' + ee.id + '/cellLevelCharacteristics?quantitationType=' + ee.singleCellQuantitationType.id + '&cellLevelCharacteristics=' + clc.id + '&download=true" ext:qtip="Download ' + htmlEncode( name ) + ' in a tab-delimited format.">Download</a>';
                            result += '</li>';
                        }
                        result += '</ul>'
                    }
                    if ( ee.hasCellBrowser ) {
                        result += '<a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showCellBrowser.html?id=' + ee.id + '" target="_blank" ext:qtip="View ' + ee.shortName + ' in Cell Browser.">View in Cell Browser</a>'
                        result += ' '
                    }
                    result += '<a href="' + Gemma.CONTEXT_PATH + '/rest/v2/datasets/' + ee.id + '/singleCellDimension' + '?quantitationType=' + ee.singleCellQuantitationType.id + '&download=true" target="_blank" ext:qtip="Download all single-cell annotations for ' + htmlEncode( ee.shortName ) + ' in a tab-delimited format. This include all cell-type assignments and cell-level characteristics.">Download all single-cell annotations</a>'
                }
                return result;
            },

            renderStatus: function (ee) {

                let result = '';

                const isUserLoggedIn = (Ext.get( 'hasUser' ) && Ext.get( 'hasUser' ).getValue() === 'true');

                if (isUserLoggedIn) {
                    result = result
                        + '<span class="ee-status-badge outline">' + Gemma.SecurityManager.getSecurityLink(
                            'ubic.gemma.model.expression.experiment.ExpressionExperiment', ee.id, ee.isPublic,
                            ee.isShared, ee.userCanWrite, null, null, null, ee.userOwned) + "</span>";
                }

                if (ee.needsAttention === true) {
                    result = result + Gemma.GEEQ.getStatusBadge('exclamation-circle', 'gold', 'in curation', 'The curation of this experiment is not done yet, so the quality and suitability scores may change significantly.')
                }
                if (ee.geeq !== null) {
                    result = result + Gemma.GEEQ.getGeeqBadges(ee.geeq.publicQualityScore, ee.geeq.publicSuitabilityScore);
                }

                if (ee.troubled) {
                    result = result + Gemma.GEEQ.getStatusBadge('exclamation-triangle', 'red', 'unusable',
                        ee.troubleDetails)
                }

                if (ee.hasMultiplePreferredQuantitationTypes) {
                    result = result + Gemma.GEEQ.getStatusBadge('exclamation-triangle', 'orange', 'multi-QT',
                        Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.statusMultiplePreferredQuantitationTypes)
                }

                if (ee.hasMultipleTechnologyTypes) {
                    result = result + Gemma.GEEQ.getStatusBadge('exclamation-triangle', 'orange', 'multi-Tech',
                        Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.statusMultipleTechnologyTypes)
                }

                result = result + Gemma.GEEQ.getBatchInfoBadges(ee);

                if (ee.reprocessedFromRawData) {
                    result = result + Gemma.GEEQ.getStatusBadge('cog', 'gray-blue', 'reprocessed',
                        Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.dataReprocessed)
                } else {
                    result = result + Gemma.GEEQ.getStatusBadge('cloud-download', 'gray-blue', 'external',
                        Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.dataExternal)
                }


                return   result ? result : "No flags"; // returning a panel with help causes layout problems.


            },

            linkAnalysisRenderer: function (ee) {
                const id = ee.id;
                const runUrl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
                   + id
                   + '-eemanager\').doLinks('
                   + id
                   + ')"><img src="' + Gemma.CONTEXT_PATH + '/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></span>';
                if (ee.dateLinkAnalysis) {
                    const type = ee.linkAnalysisEventType;
                    let color = "#000";
                    let suggestRun = true;
                    let qtip = 'ext:qtip="OK"';
                    if ( type === 'FailedLinkAnalysisEvent' ) {
                        color = 'red';
                        qtip = 'ext:qtip="Failed"';
                    } else if ( type === 'TooSmallDatasetLinkAnalysisEvent' ) {
                        color = '#CCC';
                        qtip = 'ext:qtip="Too small"';
                        suggestRun = false;
                    }

                    return '<span style="color:' + color + ';" ' + qtip + '>'
                       + Gemma.Renderers.dateRenderer( ee.dateLinkAnalysis ) + '&nbsp;' + (suggestRun ? runUrl : '');
                } else {
                    return '<span style="color:#3A3;">Needed</span>&nbsp;' + runUrl;
                }

            },

            missingValueAnalysisRenderer: function (ee) {
                const id = ee.id;
                const runUrl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
                   + id
                   + '-eemanager\').doMissingValues('
                   + id
                   + ')"><img src="' + Gemma.CONTEXT_PATH + '/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></span>';

                /*
                 * Offer missing value analysis if it's possible (this might need tweaking).
                 */
                if ( ee.technologyType !== 'ONE COLOR' && ee.technologyType !== 'NONE' && ee.hasEitherIntensity ) {

                    if (ee.dateMissingValueAnalysis) {
                        const type = ee.missingValueAnalysisEventType;
                        let color = "#000";
                        const suggestRun = true;
                        let qtip = 'ext:qtip="OK"';
                        if ( type === 'FailedMissingValueAnalysisEvent' ) {
                            color = 'red';
                            qtip = 'ext:qtip="Failed"';
                        }

                        return '<span style="color:' + color + ';" ' + qtip + '>'
                            + Gemma.Renderers.dateRenderer(ee.dateMissingValueAnalysis) + '&nbsp;'
                           + (suggestRun ? runUrl : '');
                    } else {
                        return '<span style="color:#3A3;">Needed</span>&nbsp;' + runUrl;
                    }

                } else {
                    return '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>';
                }
            },

            processedVectorCreateRenderer: function (ee) {
                const id = ee.id;
                const runUrl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
                   + id
                   + '-eemanager\').doProcessedVectors('
                   + id
                   + ')"><img src="' + Gemma.CONTEXT_PATH + '/images/icons/control_play_blue.png" alt="preprocess" title="preprocess"/></span>';

                if (ee.dateProcessedDataVectorComputation) {
                    const type = ee.processedDataVectorComputationEventType;
                    let color = "#000";

                    const suggestRun = true;
                    let qtip = 'ext:qtip="OK"';
                    if ( type === 'FailedProcessedVectorComputationEvent' ) {
                        color = 'red';
                        qtip = 'ext:qtip="Failed"';
                    }

                    return '<span style="color:' + color + ';" ' + qtip + '>'
                        + Gemma.Renderers.dateRenderer(ee.dateProcessedDataVectorComputation) + '&nbsp;'
                       + (suggestRun ? runUrl : '');
                } else {
                    return '<span style="color:#3A3;">Needed</span>&nbsp;' + runUrl;
                }
            },
            diagnosticsRenderer: function (ee) {
                const id = ee.id;
                const runUrl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
                   + id
                   + '-eemanager\').doDiagnostics('
                   + id
                   + ')"><img src="' + Gemma.CONTEXT_PATH + '/images/icons/control_play_blue.png" alt="diagnostics" title="diagnostics"/></span>';
// we don't have an appropriate date/event for this.
//               if (ee.dateProcessedDataVectorComputation) {
//                   var type = ee.processedDataVectorComputationEventType;
//                   var color = "#000";
//
//                   var suggestRun = true;
//                   var qtip = 'ext:qtip="OK"';
//                   if (type == 'FailedProcessedVectorComputationEvent') {
//                       color = 'red';
//                       qtip = 'ext:qtip="Failed"';
//                   }
//
//                   return '<span style="color:' + color + ';" ' + qtip + '>'
//                       + Gemma.Renderers.dateRenderer(ee.dateProcessedDataVectorComputation) + '&nbsp;'
//                       + (suggestRun ? runurl : '');
//               } else {
                return '<span style="color:#3A3;">Create/Update</span>&nbsp;' + runUrl;
             //  }
           },
            renderProcessedExpressionVectorCount: function (e) {
                return e.processedExpressionVectorCount ? formatNumber( e.processedExpressionVectorCount ) : '[count not available]';
            },
            renderEESets: function (eeSets) {
                eeSets.sort(function (a, b) {
                    const A = a.name.toLowerCase();
                    const B = b.name.toLowerCase();
                    if (A < B)
                        return -1;
                    if (A > B)
                        return 1;
                    return 0;
                });
                const eeSetLinks = [];
                for ( let i = 0; i < eeSets.length; i++ ) {
                    if (eeSets[i] && eeSets[i].name && eeSets[i].id) {
                        eeSetLinks
                            .push(' <a target="_blank" href="' + Gemma.CONTEXT_PATH + '/expressionExperimentSet/showExpressionExperimentSet.html?id='
                                + eeSets[i].id + '">' + eeSets[i].name + '</a>');
                    }
                }
                if (eeSetLinks.length === 0) {
                    eeSetLinks.push('Not currently a member of any experiment group');
                }
                return eeSetLinks;
            },

            renderOtherParts: function (e) {
                let h = "";

                if (e.otherParts && e.otherParts.length > 0) {
                    for ( let i = 0; i < e.otherParts.length; i++ ) {
                        const s = e.otherParts[i];
                        h = h + ' <a href="' + Gemma.CONTEXT_PATH + '/expressionExperiment/showExpressionExperiment.html?id='
                            + s.id + '">' + s.shortName +'</a>'
                    }
                } else {
                    h = "None";
                }

                return new Ext.Panel({
                    border: false,
                    html: h,
                    listeners: {
                        'afterrender' : function() {
                            window.jQuery('#otherPartsHelp').qtip({
                                content: "If this experiment was originally part of a larger study, other parts that are retained in the system are listed here.",
                                style: {
                                    name: 'cream'
                                }
                            });
                        }
                    }
                });

            },

            initComponent: function () {

                this.panelId = this.getId();
                Gemma.ExpressionExperimentDetails.superclass.initComponent.call(this);

                // this.editable && this.admin may also have been set in component configs

                const e = this.experimentDetails;
                let currentDescription = e.description;
                let currentName = e.name;
                let currentShortName = e.shortName;
                let currentPubMedId = (e.primaryCitation) ? e.primaryCitation.pubmedAccession : '';
                let currentPrimaryCitation = e.primaryCitation;
                const manager = new Gemma.EEManager( {
                    editable : this.editable,
                    id : e.id + '-eemanager'
                } );
                this.manager = manager;

                /* PUB MED REGION */

                const pubMedDisplay = new Ext.Panel( {
                    xtype: 'panel',
                    fieldLabel: 'Publication',
                    baseCls: 'x-plain-panel',
                    style: 'padding-top:5px',
                    tpl: new Ext.XTemplate(Gemma.Common.tpl.pubmedLink.complex),
                    data: {
                        pubAvailable: (currentPrimaryCitation) ? 'true' : 'false',
                        primaryCitationStr: (currentPrimaryCitation) ? currentPrimaryCitation.citation : '',
                        pubmedURL: (currentPrimaryCitation) ? currentPrimaryCitation.pubmedURL : '',
                        PMID: (currentPrimaryCitation) ? currentPrimaryCitation.pubmedAccession : ''
                    },
                    listeners: {
                        'toggleEditMode': function (editOn) {
                            this.setVisible(!editOn);
                        }
                    }
                });
                const pubMedIdField = new Ext.form.NumberField( {
                    xtype: 'numberfield',
                    allowDecimals: false,
                    minLength: 1,
                    maxLength: 9,
                    allowNegative: false,
                    emptyText: (this.isAdmin || this.editable) ? 'Enter pubmed id' : 'Not Available',
                    width: 100,
                    value: currentPubMedId,
                    enableKeyEvents: true,
                    bubbleEvents: ['changeMade'],
                    listeners: {
                        'keyup' : function( field ) {
                            if (field.isDirty()) {
                                field.fireEvent('changeMade', field.isValid());
                            }
                        },
                        scope: this

                    }
                });
                const pubMedDelete = {
                    xtype: 'button',
                    text: 'Clear',
                    icon: Gemma.CONTEXT_PATH + '/images/icons/cross.png',
                    tooltip: 'Remove this experiment\'s association with this publication',
                    bubbleEvents: ['changeMade'],
                    handler: function () {
                        pubMedIdField.setValue('');
                        field.fireEvent('changeMade', true);
                    },
                    scope: this
                };
                const pubMedForm = new Ext.Panel( {
                    fieldLabel: 'Publication',
                    xtype: 'panel',
                    layout: 'hbox',
                    hidden: true,// hide until edit mode is activated
                    padding: 3,
                    items: [pubMedIdField, pubMedDelete],
                    listeners: {
                        'toggleEditMode': function (editOn) {
                            this.setVisible(editOn);
                            this.doLayout();
                        }
                    }
                });

                /*
                 * This is needed to make the annotator initialize properly.
                 */
                new Gemma.CategoryCombo({});

                const taggerUrl = "<span style='cursor:pointer' onClick=\"return Ext.getCmp('" + e.id + "-eemanager')" +
                   ".tagger(" + e.id + "," + e.taxonId + "," + this.editable + ", null)\" >" +
                   "<i class='gray-blue fa fa-tags fa-lg -fa-fw' ext:qtip='add/view tags'></i></span>";

                const tagView = new Gemma.AnnotationDataView( {
                    readParams : [ {
                        id : e.id,
                        classDelegatingFor : "ExpressionExperiment"
                    } ]
                } );

                manager.on('tagsUpdated', function () {
                    tagView.store.reload();
                });

                manager.on('done', function () {
                    /*
                     * After a process that requires refreshing the page.
                     */
                    window.location.reload();
                });

                manager.on('reportUpdated', function (data) {
                    const ob = data[0];
                    let k = Ext.get( 'coexpressionLinkCount-region' );
                    Ext.DomHelper.overwrite(k, {
                        html: ob.coexpressionLinkCount
                    });
                    k.highlight();
                    k = Ext.get('processedExpressionVectorCount-region');
                    Ext.DomHelper.overwrite(k, {
                        html: ob.processedExpressionVectorCount
                    });
                    k.highlight();
                }, this);

                manager.on('differential', function () {
                    window.location.reload(true);
                });

                const save = function() {
                    if ( !this.saveMask ) {
                        this.saveMask = new Ext.LoadMask( this.getEl(), {
                            msg : "Saving ..."
                        } );
                    }
                    this.saveMask.show();
                    const shortName = shortNameField.getValue();
                    const description = descriptionArea.getValue();
                    const name = nameArea.getValue();
                    const newPubMedId = pubMedIdField.getValue();

                    const entity = {
                        entityId : e.id
                    };

                    if ( shortName !== currentShortName ) {
                        entity.shortName = shortName;
                    }

                    if ( description !== currentDescription ) {
                        entity.description = description;
                    }

                    if ( name !== currentName ) {
                        entity.name = name;
                    }

                    if ( !newPubMedId ) {
                        entity.pubMedId = currentPubMedId;
                        entity.removePrimaryPublication = true;
                    } else if ( newPubMedId !== currentPubMedId ) {
                        entity.pubMedId = newPubMedId;
                        entity.removePrimaryPublication = false;
                    } else {
                        entity.removePrimaryPublication = false;
                    }
                    // returns ee details object
                    ExpressionExperimentController.updateBasics( entity, function( data ) {

                        shortNameField.setValue( data.shortName );
                        nameArea.setValue( data.name );
                        descriptionArea.setValue( data.description );
                        pubMedIdField.setValue( data.pubmedId );
                        pubMedDisplay.update( {
                            pubAvailable : (data.pubmedId) ? 'true' : 'false',
                            primaryCitation : (data.primaryCitation) ? data.primaryCitation.citation : '',
                            pubmedURL : (data.primaryCitation) ? data.primaryCitation.pubmedURL : '',
                            PMID : (data.primaryCitation) ? currentPrimaryCitation.pubmedAccession : ''
                        } );

                        currentShortName = data.shortName;
                        currentName = data.name;
                        currentDescription = data.description;
                        currentPubMedId = (data.primaryCitation) ? data.primaryCitation.pubmedAccession : '';
                        currentPrimaryCitation = data.primaryCitation;

                        this.dirtyForm = false;
                        this.saveMask.hide();

                    }.createDelegate( this ) );

                }.createDelegate( this );

                const descriptionArea = new Ext.form.TextArea( {
                    allowBlank: true,
                    resizable: true,
                    readOnly: true,
                    disabled: false,
                    growMin: 1,
                    growMax: 150,
                    growAppend: '',
                    grow: true,
                    disabledClass: 'disabled-plain',
                    fieldClass: '',
                    emptyText: 'No description provided',
                    enableKeyEvents: true,
                    bubbleEvents: ['changeMade'],
                    listeners: {
                        'keyup' : function( field ) {
                            if (field.isDirty()) {
                                field.fireEvent('changeMade', field.isValid());
                            }
                        },
                        'toggleEditMode': function (editOn) {
                            this.setReadOnly(!editOn);
                            if (editOn) {
                                this.removeClass('x-bare-field');
                            } else {
                                this.addClass('x-bare-field');
                            }
                        }
                    },
                    style: 'width: 100%; background-color: #fcfcfc; border: 1px solid #cccccc;',
                    value: currentDescription
                });

                const shortNameField = new Ext.form.TextField( {
                    enableKeyEvents: true,
                    allowBlank: false,
                    grow: true,
                    disabledClass: 'disabled-plain',
                    readOnly: true,
                    // disabled: true,
                    style: 'font-weight: bold; font-size:1.4em; height:1.5em; color:black',
                    bubbleEvents: ['changeMade'],
                    listeners: {
                        'keyup' : function( field ) {
                            if (field.isDirty()) {
                                field.fireEvent('changeMade', field.isValid());
                            }
                        },
                        'toggleEditMode': function (editOn) {
                            this.setReadOnly(!editOn);
                            // this.setDisabled(!editOn);
                            if (editOn) {
                                this.removeClass('x-bare-field');
                            } else {
                                this.addClass('x-bare-field');
                            }
                        }
                    },
                    value: currentShortName
                });

                const nameArea = new Ext.form.TextArea( {
                    allowBlank: false,
                    grow: true,
                    // growMin: 22,
                    growAppend: '',
                    readOnly: true,// !this.editable,
                    cls: 'disabled-plain',
                    emptyText: 'No description provided',
                    enableKeyEvents: true,
                    bubbleEvents: ['changeMade'],
                    editOn: false,
                    listeners: {
                        'keyup' : function( field ) {
                            if (field.isDirty()) {
                                field.fireEvent('changeMade', field.isValid());
                            }
                        },
                        'focus': function (field) {
                            if (!field.editOn) {
                                this.removeClass('x-form-focus');
                            }
                        },
                        'toggleEditMode': function (editOn) {
                            this.setReadOnly(!editOn);
                            this.editOn = editOn;
                            if (editOn) {
                                this.removeClass('x-bare-field');
                            } else {
                                this.addClass('x-bare-field');
                            }
                        }
                    },
                    style: 'font-weight: bold; font-size:1.3em; width:100%',
                    value: currentName
                });

                const resetEditableFields = function() {
                    shortNameField.setValue(currentShortName);
                    nameArea.setValue(currentName);
                    descriptionArea.setValue(currentDescription);
                    pubMedIdField.setValue(currentPubMedId);
                    saveBtn.disable();
                    cancelBtn.disable();
                };

                const editBtn = new Ext.Button( {
                    // would like to use on/off slider or swtich type control here
                    text: 'Start editing',
                    editOn: false,
                    disabled: !this.editable,
                    handler : function() {
                        this.fireEvent('toggleEditMode', true);
                    },
                    scope: this
                });
                const cancelBtn = new Ext.Button( {
                    text: 'Cancel',
                    disabled: true,
                    toolTip: 'Reset all fields to saved values',
                    handler: function () {
                        this.fireEvent('toggleEditMode', false);
                    },
                    scope: this
                });

                const saveBtn = new Ext.Button( {
                    text: 'Save',
                    disabled: true,
                    handler: function () {
                        save();
                        this.fireEvent('toggleEditMode', false);
                    },
                    scope: this
                });
                const editEEButton = new Ext.Button( {
                    text: 'More edit options',
                    icon: Gemma.CONTEXT_PATH + '/images/icons/wrench.png',
                    toolTip: 'Go to editor page for this experiment',
                    disabled: !this.editable,
                    handler: function () {
                        window.open(Gemma.CONTEXT_PATH + '/expressionExperiment/editExpressionExperiment.html?id='
                            + this.experimentDetails.id);
                    },
                    scope: this
                });
                const deleteEEButton = new Ext.Button( {
                    text: 'Delete Experiment',
                    icon: Gemma.CONTEXT_PATH + '/images/icons/cross.png',
                    toolTip: 'Delete the experiment from the system',
                    disabled: !this.editable,
                    handler: function () {
                        manager.deleteExperiment(this.experimentDetails.id, true);
                    },
                    scope: this
                });

                this.on('toggleEditMode', function (editOn) {
                    // is there a way to make this even propagate to all children automatically?
                    this.editModeOn = editOn; // needed to warn user before tab change
                    editBtn.setText((editOn) ? 'Editing mode on' : 'Start editing');
                    editBtn.setDisabled(editOn);
                    nameArea.fireEvent('toggleEditMode', editOn);
                    descriptionArea.fireEvent('toggleEditMode', editOn);
                    shortNameField.fireEvent('toggleEditMode', editOn);
                    pubMedForm.fireEvent('toggleEditMode', editOn);
                    pubMedDisplay.fireEvent('toggleEditMode', editOn);
                    resetEditableFields();
                    saveBtn.setDisabled(!editOn);
                    cancelBtn.setDisabled(!editOn);
                    if (!editOn) {
                        resetEditableFields();
                        this.dirtyForm = false;
                    }
                });

                this.on('changeMade', function (wasValid) {
                    // enable save button
                    saveBtn.setDisabled(!wasValid);
                    cancelBtn.setDisabled(!wasValid);
                    this.dirtyForm = true;

                });
                const basics = new Ext.Panel(
                    {
                        ref: 'fieldPanel',
                        collapsible: false,
                        bodyBorder: false,
                        frame: false,
                        baseCls: 'x-plain-panel',
                        bodyStyle: 'padding:10px',
                        defaults: {
                            bodyStyle: 'vertical-align:top;',
                            baseCls: 'x-plain-panel',
                            fieldClass: 'x-bare-field'
                        },
                        tbar: new Ext.Toolbar({
                            hidden: !this.editable,
                            items: [editBtn, ' ', saveBtn, ' ', cancelBtn, '-', editEEButton, '-', deleteEEButton]
                        }),
                        items: [
                            shortNameField,
                            nameArea,
                            {
                                layout: 'form',
                                labelWidth: 140,
                                labelAlign: 'right',
                                labelSeparator: ':',
                                labelStyle: 'font-weight:bold;',
                                flex: 1,
                                defaults: {
                                    border: false
                                },
                                items: [
                                    {
                                        fieldLabel: "Taxon",
                                        html: e.taxon
                                    },
                                    {
                                        fieldLabel : 'Tags&nbsp;' + taggerUrl,
                                        items: [tagView]
                                    },
                                    {
                                        fieldLabel: 'Experiment Groups',
                                        html: this.renderEESets(e.expressionExperimentSets).join(',')
                                    },
                                    {
                                        fieldLabel: 'Samples',
                                        html: this.renderSamples(e),
                                        width: 60
                                    },
                                    {
                                        fieldLabel : 'Single Cell Metadata',
                                        html : this.renderSingleCellMetadata( e ),
                                        hidden : !e.isSingleCell
                                    },
                                    {
                                        fieldLabel: 'Profiles',
                                        // id: 'processedExpressionVectorCount-region',
                                        html: '<div id="downloads"> '
                                        + this.renderProcessedExpressionVectorCount(e)
                                        + '&nbsp;&nbsp;'
                                           + '<i>Downloads:</i> &nbsp;&nbsp; <span class="link"  ext:qtip="Download the filtered data in a tab-delimited format." onClick="Gemma.ExpressionExperimentDataFetch.fetchData(true,'
                                        + e.id
                                        + ', \'text\', null, null)">Filtered</span> &nbsp;&nbsp;'
                                           + '<span class="link" ext:qtip="Download the unfiltered data in a tab-delimited format." onClick="Gemma.ExpressionExperimentDataFetch.fetchData(false,'
                                        + e.id
                                        + ', \'text\', null, null)">Unfiltered</span> &nbsp;&nbsp;'
                                        + '<i class="qtp fa fa-question-circle fa-fw"></i>'
                                        + '</div>',
                                        width: 400,
                                        listeners: {
                                            'afterrender' : function() {
                                                window.jQuery('#downloads').find('i')
                                                    .qtip(
                                                        {
                                                            content: Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.profileDownloadTT,
                                                            style: {
                                                                name: 'cream'
                                                            }
                                                        });
                                            }
                                        }
                                    }, {
                                        fieldLabel: 'Platforms',
                                        html: this.renderArrayDesigns(e),
                                        width: 600
                                    }
                                    /* hidden temporarily
                                    , {
                                        fieldLabel: 'Coexpr. Links',
                                        html: this.renderCoExpressionLinkCount(e),
                                        width: 80
                                    }*/
                                    , {
                                        fieldLabel: 'Differential Expr. Analyses (<a href="https://pubmed.ncbi.nlm.nih.gov/33599246/">ANOVA</a>)',
                                        items: new Gemma.DifferentialExpressionAnalysesSummaryTree({
                                            experimentDetails: e,
                                            editable: this.editable,
                                            listeners: {
                                                'analysisDeleted': function () {
                                                    this.fireEvent('experimentDetailsReloadRequired');
                                                },
                                                scope: this
                                            }
                                        })
                                    },

                                    {
                                        fieldLabel: 'Status' /*+ '&nbsp;<i id="statusHelp" class="qtp fa fa-question-circle fa-fw"></i>'*/,
                                        baseCls: 'status-bcls',
                                        html: this.renderStatus(e)
                                    }]
                            },
                            descriptionArea,
                            {
                                layout: 'form',
                                labelWidth: 140,
                                labelAlign: 'right',
                                labelSeparator: ':',
                                labelStyle: 'font-weight:bold;',
                                flex: 1,
                                defaults: {
                                    border: false
                                },
                                items: [
                                    pubMedDisplay,
                                    pubMedForm,
                                    {
                                        fieldLabel: 'Source',
                                        html: this.renderSourceDatabaseEntry(e)
                                    },
                                    {
                                        fieldLabel: 'Other parts' + '&nbsp;<i id="otherPartsHelp" class="qtp fa fa-question-circle fa-fw"></i>',
                                        items: this.renderOtherParts(e)
                                    }
                                ]
                            },
                        ]
                    });

                this.add(basics);

                // adjust when user logs in or out
                Gemma.Application.currentUser.on( "logIn", function() {
                    const appScope = this;
                    ExpressionExperimentController.canCurrentUserEditExperiment(this.experimentDetails.id, {
                        callback: function (editable) {
                            // console.log(this);
                            appScope.adjustForIsEditable(editable);
                        },
                        scope: appScope
                    });

                }, this);
                Gemma.Application.currentUser.on("logOut", function () {
                    this.adjustForIsEditable(false);
                    // TODO reset widget if experiment is private!
                }, this);

                this.doLayout();
                this.fireEvent("ready");

            }, // end of initComponent
            adjustForIsEditable: function (editable) {
                this.fieldPanel.getTopToolbar().setVisible(editable);
            }
        });
