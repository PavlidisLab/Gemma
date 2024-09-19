Ext.namespace('Gemma');


Gemma.SequenceDetailsPanel = Ext
    .extend(
        Ext.Panel,
        {
            /**
             * Update the details listing.
             *
             * @memberOf Gemma.SequenceDetailsPanel
             */
            updateSequenceInfo: function (r) {

                var pan = this.sequenceDetailsGrid;
                var alignments = this.alignmentsGrid;

                this.alignmentsGrid
                    .getStore()
                    .load(
                        {
                            params: [{
                                id: r.get('compositeSequenceId')
                            }],

                            callback: function (records, options, success) {

                                pan.removeAll(true);

                                if (!success || records === null || records.length == 0) {
                                    pan.add({
                                        border: false,
                                        html: {
                                            tag: 'span',
                                            html: "No results obtained, possible error"
                                        }
                                    });
                                    return;
                                }

                                var record = null; // GeneMappingSummary
                                for (var i = 0; i < records.length; i++) {
                                    var c = records[i];
                                    if (c.get("compositeSequence") != null) {
                                        record = c;
                                        break;
                                    }
                                } // GeneMappingSummary

                                if (record == null) {
                                    pan.add({
                                        border: false,
                                        html: {
                                            tag: 'span',
                                            html: "Could not identify the element, possible error"
                                        }
                                    });
                                    return;
                                }

                                // Note this can be a dummy with no real blat result.
                                var seq = record.get("blatResult").querySequence;

                                var cs = record.get("compositeSequence");
                                var ar = cs.arrayDesign;

                                var csDesc = (cs.description != null && cs.description.length > 0) ? "<span style='font-size:smaller'>(" + cs.description
                                    + ")</span>" : "";

                                pan.add({
                                    border: false,
                                    html: {
                                        tag: 'span',
                                        html: "<strong>" + cs.name + "</strong> on " + ar.shortName + "&nbsp;" + csDesc,
                                        "ext:qtip": "Based on provider's description, genes cited may not be accurate"
                                    }
                                });

                                if (seq != null && seq.length != null) {
                                    // alignments.show();
                                    pan.add({
                                        border: false,
                                        html: {
                                            tag: 'li',
                                            html: "Length: " + seq.length + ", Type: "
                                            + (seq.type != null ? seq.type.value : "?"),
                                            "ext:qtip": "Sequence length in bases and Sequence type as classified by Gemma"
                                        }
                                    });

                                    var repeatFrac = seq.fractionRepeats ? Math.round((seq.fractionRepeats * 1000) / 10) : 0;
                                    pan.add({
                                        border: false,
                                        html: {
                                            tag: 'li',
                                            html: "Repeat-masked bases: " + repeatFrac + "%",
                                            "ext:qtip": "Percent bases masked by RepeatMasker"
                                        }
                                    });

                                    if (seq.sequence != null && seq.sequence.length > 0) {
                                        pan
                                            .add({
                                                border: false,
                                                html: {
                                                    tag: 'div',
                                                    html: seq.sequence,
                                                    cls: 'clob smaller',
                                                    style: 'word-wrap: break-word;width:500px;height:100px;padding:4px;margin:3px;font-family:monospace'
                                                }
                                            });
                                    }

                                    if (seq.sequenceDatabaseEntry != null) {
                                        pan
                                            .add({
                                                border: false,
                                                html: {
                                                    tag: 'a',
                                                    id: "ncbiLink",
                                                    target: "_blank",
                                                    title: "view at NCBI",
                                                    href: "https://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=" + encodeURIComponent(seq.sequenceDatabaseEntry.accession),
                                                    html: "View in NCBI <img src=\"" + Gemma.NCBI_ICON + "\" alt=\"NCBI icon\"/>",
                                                    "ext:qtip": "View sequence at NCBI"
                                                }
                                            });
                                    }
                                } else {
                                    // alignments.hide();
                                    pan
                                        .add({
                                            border: false,
                                            html: {
                                                tag: 'span',
                                                html: "No sequence; missing information or mapping was directly to gene without alignment"
                                            }
                                        });

                                }
                                pan.doLayout();

                            }
                        });
            },

            border: true,

            initComponent: function () {

                this.alignmentsGrid = new Gemma.GenomeAlignmentsGrid({
                    geneId: this.geneId,
                    id: 'alignment-grid',
                    height: 100,
                    width: 600
                });

                this.sequenceDetailsGrid = new Ext.Panel({
                    padding: 8,
                    items: {
                        border: false,
                        html: "Select an row to see details"
                    },
                    id: "sequence-info",
                    height: 200,
                    width: 600
                });

                Ext.apply(this, {
                    items: [this.sequenceDetailsGrid, this.alignmentsGrid]
                });

                Gemma.SequenceDetailsPanel.superclass.initComponent.call(this);
            }

        });