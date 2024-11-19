Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

/**
 * This provides a summary of the differential analyses done for a particular dataset/expression experiment. It is
 * structured as a tree with each analysis as a node and its result sets as its children
 *
 * @config experimentDetails {ExpressionExperimentValueObject} the expression experiment for which to display the
 *         analyses
 * @config editable {boolean} whether the user should be shown icons to delete analyses
 * @class Gemma.DifferentialExpressionAnalysesSummaryTree
 * @extends Ext.tree.TreePanel
 */

Gemma.DifferentialExpressionAnalysesSummaryTree = Ext
    .extend(
        Ext.tree.TreePanel,
        {
            animate: true,
            rootVisible: false,
            enableDD: false,
            cls: 'x-tree-noicon',
            singleClickExpand: true,
            lines: false,
            containerScroll: false,
            // panel
            autoScroll: true,
            border: false,
            layout: 'fit',
            root: {
                text: 'root'
            },

            listeners: {
                afterrender: function () {
                    this.drawPieCharts();
                },
                expandnode: function () {
                    this.drawPieCharts();
                }
            },

            /**
             * @memberOf Gemma.DifferentialExpressionAnalysisSummaryTree
             */
            initComponent: function () {

                this.ee = this.experimentDetails;

                // always editable by admin user
                if (this.editable === undefined && Ext.get("hasAdmin").getValue() === 'true') {
                    this.editable = true;
                }

                Gemma.DifferentialExpressionAnalysesSummaryTree.superclass.initComponent.call(this);
                this.build();
                new Ext.tree.TreeSorter(this, {
                    folderSort: false,
                    dir: 'ASC',
                    property: 'text'
                });
            },
            build: function () {
                var analyses = this.ee.differentialExpressionAnalyses;
                Ext.apply(this, {
                    contrastPercents: [], // for drawing charts
                    totalProbes: this.ee.processedExpressionVectorCount
                });

                // console.log("in build" +
                // this.ee.differentialExpressionAnalyses);
                // set the root node
                var root = new Ext.tree.TreeNode({
                    expanded: true,
                    id: 'diffExRoot',
                    text: 'root'
                });
                this.setRootNode(root);

                // just show "Not available" root if no analyses
                if (!analyses || analyses.length === 0) {
                    root.appendChild(new Ext.tree.TreeNode({
                        id: 'nodeNA',
                        expanded: false,
                        leaf: true,
                        text: 'Not Available'
                    }));
                    return;
                }

                // var subsetTracker = {}; // used to keep subset nodes
                // adjacent
                var nodeId = 0; // used to keep track of nodes and give
                // each a specific
                // div in which to draw a pie chart
                for (var j = 0; j < analyses.length; j++) {
                    var analysis = analyses[j];

                    var parentNode = null;
                    var parentText = null;
                    var interaction = 0;

                    // prepare subset text if applicable
                    var subsetText = '';
                    var subsetIdent = '';
                    var neighbourNode = null;
                    // FIXME. The analysis contains the subsetFactor as well.
                    if (analysis.subsetFactor) {
                        var subsetFactor = analysis.subsetFactor;
                        subsetText = this.getSubsetText(analysis);
                        subsetIdent = subsetFactor + analysis.resultSets.length;
                        // if a similar subset node has already been created, insert
                        // this node adjacent to it
                        neighbourNode = root.findChild('subsetIdent', subsetIdent);
                    }

                    // prepare download link
                    var downloadDiffDataLink = this.getDownloadLink(analysis);

                    // make node for analysis
                    parentNode = new Ext.tree.TreeNode({
                        id: 'node' + this.ee.id + '-' + (nodeId++),
                        expanded: true /* PP changed */,
                        singleClickExpand: true,
                        text: downloadDiffDataLink,
                        subsetIdent: subsetIdent,
                        leaf: false
                    });

                    // add node to tree
                    if (neighbourNode) {
                        root.insertBefore(parentNode, neighbourNode);
                    } else {
                        root.appendChild(parentNode);
                    }
                    var resultSet = null;
                    var analysisName = null;
                    var analysisNameExtra = null;
                    var nodeText = '';
                    var primaryFactorID = null;
                    // if analysis has only one result set, don't give
                    // it children and
                    // put all info in parent node
                    if (analysis.resultSets.length === 1) {
                        resultSet = analysis.resultSets[0];
                        // get experimental factor string and build analysis parent node text
                        analysisName = this.getFactorNameText(analysis, resultSet);
                        primaryFactorID = this.getPrimaryFactorID(analysis, resultSet);
                        analysisNameExtra = this.getFactorNameExtra(analysis, resultSet);
                        nodeText = '';
                        // if there's subset text, add baseline and
                        // links to it to
                        // maintain order
                        // FIXME
                        if (subsetText !== '') {
                            subsetText += this.getBaseline(resultSet);
                            subsetText += this.getActionLinks(resultSet, analysisName[0], this.ee.id,  primaryFactorID, nodeId);
                        } else {
                            nodeText += this.getBaseline(resultSet);
                            nodeText += this.getActionLinks(resultSet, analysisName[0], this.ee.id, primaryFactorID, nodeId);
                        }

                        parentText = '<b>'
                            + analysisName[0]
                            + (analysisNameExtra ? '</b>&nbsp<span style="color:grey;font-size:smaller;" ext:qtip="Values: '
                                + analysisNameExtra + '">(' + Ext.util.Format.ellipsis(analysisNameExtra, 35, true)
                                + ')</span>' : '') + nodeText;

                        /*
                         * How many levels were used.
                         */
                        parentNode.attributes.numberOfFactors = resultSet.experimentalFactors.length;
                        parentNode.attributes.analysisId = resultSet.analysisId;
                        parentNode.attributes.resultSetId = resultSet.resultSetId;
                        if (resultSet.experimentalFactors.length === 1 && analysis.factorValuesUsed[resultSet.experimentalFactors[0].id] !== undefined) {
                            parentNode.attributes.numberOfFactorValues = analysis.factorValuesUsed[resultSet.experimentalFactors[0].id].length;
                        }
                    }
                    // if analysis has >1 result set, create result set
                    // children
                    else {
                        for (var i = 0; i < analysis.resultSets.length; i++) {
                            resultSet = analysis.resultSets[i];

                            // get experimental factor string and build
                            // analysis parent
                            // node text
                            analysisName = this.getFactorNameText(analysis, resultSet);
                            analysisNameExtra = this.getFactorNameExtra(analysis, resultSet);
                            primaryFactorID = this.getPrimaryFactorID(analysis, resultSet);
                            var factor = analysisName[0];
                            interaction += analysisName[1];

                            // only grab factor name when 1 factor,
                            // otherwise will grab
                            // doubles from interaction
                            if (resultSet.experimentalFactors.length === 1) {
                                // keep factors in alpha order
                                parentText = (!parentText) ? factor : (factor < parentText) ? (factor + " & " + parentText)
                                    : (parentText + " & " + factor);
                            }

                            nodeText = '';
                            nodeText += this.getBaseline(resultSet);
                            nodeText += this.getActionLinks(resultSet, factor, this.ee.id, primaryFactorID,(nodeId + 1));

                            // make child nodes for each analysis and
                            // add them to parent
                            // factor node
                            var analysisNode = new Ext.tree.TreeNode(
                                {
                                    id: 'node' + this.ee.id + '-' + (nodeId++),
                                    expanded: true, // not obeyed?
                                    singleClickExpand: true,
                                    text: factor
                                    + (analysisNameExtra ? '&nbsp<span style="color:grey;font-size:smaller;" ext:qtip="Values: '
                                        + analysisNameExtra
                                        + '">('
                                        + Ext.util.Format.ellipsis(analysisNameExtra, 35, true)
                                        + ')</span>'
                                        : '') + nodeText,
                                    /*
                                     * How many levels were used.
                                     */
                                    numberOfFactors: resultSet.experimentalFactors.length,
                                    analysisId: resultSet.analysisId,
                                    resultSetId: resultSet.resultSetId,
                                    numberOfFactorValues: resultSet.experimentalFactors.length === 1 && analysis.factorValuesUsed[resultSet.experimentalFactors[0].id] ? analysis.factorValuesUsed[resultSet.experimentalFactors[0].id].length
                                        : null,

                                    leaf: true
                                });

                            // if this node is the interaction result
                            // set, it goes last

                            parentNode.appendChild(analysisNode);

                        }
                    }

                    // figure out type of a ANOVA
                    var numberOfFactors = 0;
                    for (i = 0; i < analysis.resultSets.length; i++) {
                        resultSet = analysis.resultSets[i];
                        // ignore the result sets where interactions are
                        // being looked at
                        if (resultSet.experimentalFactors.length === 1) {
                            numberOfFactors++;
                        }
                    }

                    var analysisDesc = this.getANOVAtypeText(numberOfFactors);
                    if (numberOfFactors === 1 || interaction <= 0) {
                        analysisDesc += ' on ';
                    } else {
                        analysisDesc += ' with interactions on ';
                    }

                    var deleteText = '';
                    var redoText = '';
                    var refreshStatsText = '';
                    if (this.editable) {
                        deleteText = this.getDeleteLink(analysis);
                        redoText = this.getRedoLink(analysis);
                        refreshStatsText = this.getRefreshStatsLink(analysis);
                    }

                    parentNode.setText(analysisDesc + parentText + subsetText + " " + parentNode.text + deleteText
                        + redoText + refreshStatsText);

                    // if this parent node has an interaction child,
                    // that child should
                    // go last among its siblings
                    var sorter = new Ext.tree.TreeSorter(this, {
                        dir: 'ASC',
                        property: 'text'
                    });
                    sorter.doSort(parentNode);
                }
            },

            getSubsetText: function (analysis) {
                var subsetText = '';
                if (analysis.subsetFactor) {
                    var subsetFactor = analysis.subsetFactor;
                    var subsetFactorValue = analysis.subsetFactorValue;
                    subsetText = '<span ext:qtip="Analysis was run by subsetting the data on the factor '
                        + subsetFactor.category + " (" + subsetFactor.description
                        + ") and selecting samples where the value was \'" + subsetFactorValue.factorValue + '\'">'
                        + " using a subset of the data (" + subsetFactor.category + " = " + analysis.subsetFactorValue.factorValue
                        + ')</span>';
                }
                return subsetText;
            },
            getDownloadLink: function (analysis) {
                // prepare download link
                return String
                    .format(
                        "<span class='link'"
                        + " onClick='Gemma.ExpressionExperimentDataFetch.fetchDiffExpressionData({0})' > " +
                        "<i class='gray-blue fa fa-cloud-download fa-lg' ext:qtip='Download all differential expression data for this analysis in a tab-delimited format'></i></span>",
                        analysis.id);
            },
            getANOVAtypeText: function (numberOfFactors) {

                var analysisDesc = '';
                // just being overly safe here
                switch (numberOfFactors) {
                    case 1:
                        analysisDesc = 'One-way ANOVA';
                        break;
                    case 2:
                        analysisDesc = 'Two-way ANOVA';
                        break;
                    case 3:
                        analysisDesc = 'Three-way ANOVA';
                        break;
                    case 4:
                        analysisDesc = 'Four-way ANOVA';
                        break;
                    case 5:
                        analysisDesc = 'Five-way ANOVA';
                        break;
                    case 6:
                        analysisDesc = 'Six-way ANOVA';
                        break;
                    default:
                        analysisDesc = 'n-way ANOVA';
                }
                return analysisDesc;
            },
            /**
             * get the number of probes that are differentially expressed and the number of 'up' and 'down' probes
             *
             * @return String text with numbers
             */
            getExpressionNumbers: function (resultSet, nodeId, showThreshold) {
                /* Show how many probes are differentially expressed; */

                var numbers = resultSet.numberOfDiffExpressedProbes + ' of ' + this.totalProbes
                    + ' probes were differentially expressed<br>';

                // if (resultSet.upregulatedCount != 0) {
                numbers += resultSet.upregulatedCount + "&nbsp;Up";
                // }

                // if (resultSet.downregulatedCount != 0) {
                numbers += ';&nbsp;' + resultSet.downregulatedCount + "&nbsp;Down";
                // }
                if (showThreshold) {
                    numbers += '. <br>Threshold value = ' + resultSet.threshold;
                    numbers += (resultSet.qValue) ? (', qvalue = ' + resultSet.qValue) : '';
                }

                // save number of up regulated probes for drawing as
                // chart after tree
                // has been rendered
                // if there are no up or down regulated probes, draw an
                // empty circle
                if (resultSet.numberOfDiffExpressedProbes === 0) {
                    this.contrastPercents[nodeId] = null;
                } else {
                    this.contrastPercents[nodeId] = {
                        // jawr doesn't like / starting a line, parens
                        // guard.
                        'up': (resultSet.upregulatedCount / this.totalProbes),
                        'down': (resultSet.downregulatedCount / this.totalProbes),
                        'diffExpressed': (resultSet.numberOfDiffExpressedProbes / this.totalProbes)
                    };
                }
                return numbers;
            },
            getBaseline: function (resultSet) {
                if (this.isContinuousFactor(resultSet))
                    return '';
                var base = '';
                if (resultSet.baselineGroup) {
                    base = ' with baseline&nbsp;=&nbsp;' + resultSet.baselineGroup.factorValue;
                }
                return base;
            },
            getDeleteLink: function (analysis) {
                var manager = new Gemma.EEManager({
                    id: "eemanager"
                });
                manager.on('deletedAnalysis', function () {
                    manager.updateEEReport(this.ee.id);
                }, this);
                manager.on('reportUpdated', function () {
                    this.fireEvent('analysisDeleted');
                }, this);
                return String.format("<span style='cursor:pointer' "
                    + "onClick='Ext.getCmp(&quot;eemanager&quot;).deleteExperimentAnalysis({0},{1},false)'>"
                    + "<i class='red fa fa-remove fa-lg fa-fw' ext:qtip='Delete this analysis'></i></span>", this.ee.id, analysis.id);
            },

            getRedoLink: function (analysis) {
                var manager = new Gemma.EEManager({
                    id: "eemanager"
                });
                manager.on('differential', function () {
                    manager.updateEEReport(this.ee.id);
                }, this);
                manager.on('reportUpdated', function () {
                    this.fireEvent('analysisRedone');
                }, this);
                return String.format("<span style='cursor:pointer' "
                    + "onClick='Ext.getCmp(&quot;eemanager&quot;).redoExperimentAnalysis({0},{1},false)'>"
                    + "<i class='green fa fa-refresh fa-lg fa-fw' ext:qtip='Re-run this analysis'></i></span>", this.ee.id, analysis.id);
            },

            /**
             * We can remove this, as it was just for helping deal with corrupt etc. data.
             * @param analysis
             * the analysis
             */
            getRefreshStatsLink: function (analysis) {
                var manager = new Gemma.EEManager({
                    id: "eemanager"
                });

                // maybe don't need to do this; also not the right event, perhaps.
                manager.on('differential', function () {
                    manager.updateEEReport(this.ee.id);
                }, this);
                manager.on('reportUpdated', function () {
                    this.fireEvent('analysisRedone');
                }, this);
                return String.format("<span class='fa-stack' style='cursor:pointer' "
                    + "onClick='Ext.getCmp(&quot;eemanager&quot;).refreshDiffExStats({0},{1},false)'>"
                    + "<i class='gold fa fa-circle fa-fw fa-lg fa-stack-1x' ext:qtip='Refresh the summary stats'></i>"
                    + "<i class='orange fa fa-refresh fa-fw fa-lg fa-stack-1x' ext:qtip='Refresh the summary stats'></i>"
                    + "</span>",
                    this.ee.id, analysis.id);
            },

            calculateChartId: function (eeId, nodeId) {
                // Because this tree can be used in another window, getId() is used so that the returned id is unique
                // across all opened windows.
                return this.getId() + 'Experiment' + eeId + 'Chart' + nodeId + 'Div';
            },
            getActionLinks: function ( resultSet, factorString, eeID, primaryFactorID, nodeId) {
                /* link for details */
                var numbers = this.getExpressionNumbers(resultSet, nodeId, true);
                var linkText = '&nbsp;'
                    + '<span class="link" onClick="Ext.Msg.alert(\'Differential Expression Specificity and Contrast Ratio\', \''
                    + numbers + '\')" ext:qtip=\"' + numbers + '\">' + '&nbsp;<canvas height=20 width=20 id="'
                    + this.calculateChartId(eeID, nodeId) + '"></canvas>';

                // if the number of up or downregulated probes is
                // less than 5% of the total number of differentially
                // expressed probes
                // (but not 0),
                // then insert text to highlight this

                var percentDifferentiallyExpressed = resultSet.upregulatedCount + resultSet.downregulatedCount;
                percentDifferentiallyExpressed /= this.totalProbes; // avoid
                // "/"
                // at
                // line
                // start!! Causes
                // compression
                // problems.

                if ((percentDifferentiallyExpressed < 0.05 && percentDifferentiallyExpressed > 0)) {
                    linkText += " ["
                        + ((Math.round(percentDifferentiallyExpressed * 100) === 0) ? "<1" : Math
                            .round(percentDifferentiallyExpressed * 100)) + "% diff. expr.]";
                }

                linkText += '</span>';
                /* provide link for visualization. */
                var tipText = "View top differentially expressed genes for &quot;" + factorString + "&quot;";
                linkText += '<span class="link" onClick="Gemma.DifferentialExpressionAnalysesSummaryTree.visualizeDiffExpressionHandler(\'' + eeID + '\',\''
                   + resultSet.resultSetId + '\',\'' + factorString
                   + '\', \'' + primaryFactorID + '\')">&nbsp;'
                   + "<i class='orange fa fa-area-chart fa-fw fa-lg' ext:qtip='" + tipText + "'></i></span>";

                var pValueDistImageSize = 16;
                var imageUrl = Gemma.CONTEXT_PATH + '/expressionExperiment/visualizePvalueDist.html?' + 'id=' + eeID + '&analysisId='
                    + resultSet.analysisId + '&rsid=' + resultSet.resultSetId;
                var placeholderImageUrl = imageUrl + '&size=' + pValueDistImageSize;
                // -8px -6px is used as background-position property because the image has gray border.
                var placeholderCss = 'cursor: pointer; display: inline-block;'
                   + 'width: ' + pValueDistImageSize + 'px;'
                   + 'height: ' + pValueDistImageSize + 'px;'
                   + 'background: url(' + placeholderImageUrl + ') no-repeat -8px -6px; margin: 0 3px;'
                var methodWithArguments = 'showPValueDistributionWindow(\'' + factorString.replaceAll( "'", "\\'" ) + '\', \'' + imageUrl.replaceAll( "'", "\\''" ) + '\');';

                linkText += '<div style="' + placeholderCss + '" '
                   + 'ext:qtip="Click to view the P-value distribution"'
                   + ' onClick="return Ext.getCmp(\'' + this.getId() + '\').' + methodWithArguments + '"></div>';

                return linkText;
            },
            /**
             * get experimental factor string and build analysis parent node text
             *
             * @private
             * @param {Object}
             *           resultSet
             * @return {[String,int]} an array with the first element being the factor text and the second a flag marking
             *         interaction
             */
            getFactorNameText: function (analysis, resultSet) {
                var factorText = '';
                var interaction = 0;
                if (resultSet.experimentalFactors === null || resultSet.experimentalFactors.length === 0) {
                    factorText = "n/a";
                } else {

                    for (var k = 0; k < resultSet.experimentalFactors.length; k++) {
                        var ef = resultSet.experimentalFactors[k];

                        if (ef.type === 'continuous') {
                            factorText = factorText + ef.name + ': ' + ef.description;
                        } else {
                            if ( k > 0 && k < resultSet.experimentalFactors.length ) {
                                factorText = factorText + "&nbsp;X&nbsp;";
                                interaction = interaction + 1;
                            }
                            factorText = factorText + ef.name;
                        }
                    }
                }
                return [factorText, interaction];
            },

            getPrimaryFactorID: function (analysis, resultSet) {
                if (resultSet.experimentalFactors === null || resultSet.experimentalFactors.length === 0) {
                    return null;
                } else {
                    return resultSet.experimentalFactors[0].id;
                }
            },

            isContinuousFactor: function (resultSet) {
                var ef = resultSet.experimentalFactors[0];
                return ef.type === 'continuous';
            },

            /**
             * Provide more details about the factor, in abbreviated format.
             *
             * @param analysis
             * @param resultSet
             * @returns {String}
             */
            getFactorNameExtra: function (analysis, resultSet) {

                if (resultSet.experimentalFactors.length > 1) {
                    return '';
                }

                var text = '';
                var ef = resultSet.experimentalFactors[0];
                var fvu = analysis.factorValuesUsed[ef.id];
                if (fvu === null || fvu === undefined) {
                    return text;
                }

                // for categorical, list the names
                var abrLen = 30;
                if (ef.type === 'categorical') {
                    for (var m = 0; m < fvu.length; m++) {
                        if (m > 0 && m < fvu.length) {
                            text = text + "&semi;&nbsp;";
                        }
                        text = text + Ext.util.Format.ellipsis(fvu[m].factorValue, abrLen, true);
                    }

                } else {
                    // for continuous, show range like "(0 - 10)" but also the descriptive name.
                    var name = ef.name;
                    var vals = [];
                    for (var m = 0; m < fvu.length; m++) {
                        vals[m] = fvu[m].factorValue;
                    }
                    if (vals.length > 1) {
                        // assume numeric, fall back on string.
                        try {
                            // numeric
                            vals.sort(function (a, b) {
                                return a - b;
                            });

                            text = ef.description + ': ' + Number(vals[0]).toPrecision(2) + "&nbsp;&ndash;&nbsp;"
                                + Number(vals[vals.length - 1]).toPrecision(2);
                        } catch (err) {
                            vals.sort(); // alpha
                            text = ef.description + ': ' + Ext.util.Format.ellipsis(vals[0]) + "&nbsp;&ndash;&nbsp;"
                                + Ext.util.Format.ellipsis(vals[vals.length - 1], abrLen, true);
                        }
                    }

                }

                return text;
            },

            /**
             * Method to draw one-piece pie chart in a canvas, with extra colour options
             *
             * @param {Object}
             *           ctx the canvas component to draw in (here, the canvas tag)
             * @param {int}
             *           x centre of pie on x axis relative to top right of ctx
             * @param {int}
             *           y centre of pie on y axis relative to top right of ctx
             * @param {int}
             *           size size of the pie chart
             * @param {String}
             *           colour colour for slice one of the pie
             * @param {int}
             *           value size of slice one in degrees
             * @param {String}
             *           outlineColour colour for the pie outline
             */
            drawOneColourMiniPie: function (ctx, x, y, size, colourOne, valueOne, outlineColour) {
                ctx.save();
                ctx.fillStyle = 'white'; // good grey: '#E0E0E0';
                ctx.moveTo(x, y);
                // draw circle
                ctx.beginPath();
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2, Math.PI * 3 / 2 + (Math.PI * 2), false);
                ctx.fill();
                // draw slice one
                ctx.fillStyle = colourOne;
                ctx.beginPath();
                ctx.moveTo(x, y);
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2, Math.PI * 3 / 2 + (Math.PI / 180) * valueOne, false);
                ctx.lineTo(x, y);
                ctx.fill();
                // draw circle outline
                ctx.beginPath();
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2, Math.PI * 3 / 2 + (Math.PI * 2), false);
                ctx.lineWidth = 0.75;
                ctx.strokeStyle = outlineColour;
                ctx.stroke();

                ctx.restore();
            },

            /**
             * Method to draw a two-colour, two-piece pie chart in a canvas (where sum of pieces can be < total)
             *
             * @param {Object}
             *           ctx the canvas component to draw in (here, the canvas tag)
             * @param {int}
             *           x centre of pie on x axis relative to top right of ctx
             * @param {int}
             *           y centre of pie on y axis relative to top right of ctx
             * @param {int}
             *           size size of the pie chart
             * @param {String}
             *           colourOne colour for slice one of the pie
             * @param {int}
             *           valueOne size of slice one in degrees
             * @param {String}
             *           colourTwo colour for slice two of the pie
             * @param {int}
             *           valueTwo size of slice two in degrees
             * @param {String}
             *           outlineColour colour for the pie outline
             */
            drawTwoColourMiniPie: function (ctx, x, y, size, colourOne, valueOne, colourTwo, valueTwo, outlineColour) {
                ctx.save();
                ctx.fillStyle = 'white'; // good grey: '#E0E0E0';
                ctx.moveTo(x, y);
                // draw circle
                ctx.beginPath();
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2, Math.PI * 3 / 2 + (Math.PI * 2), false);
                ctx.fill();
                // draw slice one
                ctx.fillStyle = colourOne;
                ctx.beginPath();
                ctx.moveTo(x, y);
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2, Math.PI * 3 / 2 + (Math.PI / 180) * valueOne, false);
                ctx.lineTo(x, y);
                ctx.fill();
                // draw slice two
                ctx.fillStyle = colourTwo;
                ctx.beginPath();
                ctx.moveTo(x, y);
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2 + (Math.PI / 180) * valueOne, Math.PI * 3 / 2 + (Math.PI / 180)
                    * valueOne + (Math.PI / 180) * valueTwo, false);
                ctx.lineTo(x, y);
                ctx.fill();
                // draw circle outline
                ctx.beginPath();
                ctx.arc(x, y, size / 2, Math.PI * 3 / 2, Math.PI * 3 / 2 + (Math.PI * 2), false);
                ctx.lineWidth = 0.75;
                ctx.strokeStyle = outlineColour;
                ctx.stroke();

                ctx.restore();
            },

            drawPieCharts: function () {
                var ctx, diffExpressed, interesting;
                for (var i = 0; i < this.contrastPercents.length; i++) {
                    var chartElement = Ext.get(this.calculateChartId(this.ee.id, i));

                    if (chartElement) {
                        ctx = chartElement.dom.getContext("2d");
                        if (this.totalProbes === null || this.totalProbes === 0 || this.contrastPercents[i] === null) {
                            this.drawOneColourMiniPie(ctx, 12, 12, 14, 'white', 0, 'grey');
                        } else {
                            up = this.contrastPercents[i].up;
                            down = this.contrastPercents[i].down;
                            diffExpressed = this.contrastPercents[i].diffExpressed;
                            interesting = false;
                            if (diffExpressed < 0.07) {
                                diffExpressed = 0.07;
                                interesting = true;
                            }
                            if (diffExpressed < 0.20) {
                                interesting = true;
                            }
                            if (interesting) {
                                this.drawOneColourMiniPie(ctx, 12, 12, 14, '#1f6568', diffExpressed * 360, 'black');
                            } else {
                                this.drawOneColourMiniPie(ctx, 12, 12, 14, 'rgb(95,158,160)', diffExpressed * 360, 'grey');
                            }
                            /*
                             * this code is for up:down pies //if percentage is less than 5%, round up to 5% so it's visible
                             * if(up<0.10){up=0.10; interesting = true}; if(down<0.10){down=0.10; interesting = true};
                             * if(interesting){ drawTwoColourMiniPie(ctx, 12, 12, 14, 'darkgrey', up*360, 'blue',
                             * down*360,'black'); }else{ drawTwoColourMiniPie(ctx, 12, 12, 14, 'lightgrey', up*360, '#7272b5',
                             * down*360,'grey'); }
                             */

                        }

                    }
                }
            },

            showPValueDistributionWindow: function (factorName, imageUrl) {
                var eeInfoTitle = "P-value distribution for "
                    + factorName
                    + " in: "
                    + "<a ext:qtip='Click for details on experiment (opens in new window)' target='_blank'  href='" + Gemma.CONTEXT_PATH + "/expressionExperiment/showExpressionExperiment.html?id="
                    + this.ee.id + "'>" + this.ee.shortName + "</a> (" + Ext.util.Format.ellipsis(this.ee.name, 35) + ")";

                new Ext.Window({
                    title: eeInfoTitle,
                    constrain: true, // Should not be modal so that other window can be opened.
                    width: 500,
                    shadow: true,
                    closeAction: 'close',
                    items: [{
                        bodyStyle: 'background-color: #EEEEEE; text-align: center; padding: 15px 60px 15px 15px;',
                        html: '<img src="' + imageUrl + '">'
                    }]
                }).show();
            }
        });

// register panel as xtype
Ext.reg('differentialExpressionAnalysesSummaryTree', Gemma.DifferentialExpressionAnalysesSummaryTree);

/**
 * fix for now, should replace visualize 'button' with ext button that calls this function, and move function inside
 * Gemma.DifferentialExpressionAnalysesSummaryTree
 */
Gemma.DifferentialExpressionAnalysesSummaryTree.visualizeDiffExpressionHandler = function(eeid, diffResultId, factorDetails, factorId) {

    var visDiffWindow = new Gemma.VisualizationWithThumbsWindow({
        thumbnails: false,
        readMethod: DEDVController.getDEDVForDiffExVisualizationByThreshold,
        title: "Top diff. ex. probes for &quot;" + factorDetails + "&quot;",
        showLegend: false,
        downloadLink: String.format(Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}&rs={1}&thresh={2}&diffex=1", eeid,
            diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD)
    });

    visDiffWindow.on('loadFailed', function () {
        visDiffWindow.destroy();
    }, this);

    visDiffWindow.show({
        params: [diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD, factorId]
    });
}