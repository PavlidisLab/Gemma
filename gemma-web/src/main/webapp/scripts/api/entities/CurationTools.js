Ext.namespace('Gemma');

/**
 *
 * Panel with curation tools for curatable objects.
 * Should be displayed in the curation tabs for ExpressionExperiment and ArrayDesign (aka. platform).
 *
 * @class Gemma.CurationTools
 * @extends Ext.Panel
 */
Gemma.CurationTools = Ext.extend(Ext.Panel, {
    curatable: null,

    border: false,
    defaultType: 'box',
    defaults: {
        border: false
    },
    padding: 10,

    initComponent: function () {
        Gemma.CurationTools.superclass.initComponent.call(this);

        this.add({
            html:
                '<div class="v-padded"><span class="bold width110">Troubled status: </span>' + this.getTroubleStatusHtml() + '</div>' +
                '<div class="v-padded"><span class="bold width110">Curation status: </span>' + this.getNeedsAttentionStatusHtml() + '</div>'

        });
    },


    /**
     * Composes the string for curation attention description
     * @returns {string} string containing the status with appropriate icon, color, date and performer.
     */
    getNeedsAttentionStatusHtml: function () {
        //Base status
        var str = this.curatable.needsAttention
            ?   '<span class="gold width130"><i class="fa fa-exclamation-circle fa-lg fa-fw"></i>Needs attention</span>'
            :   '<span class="green width130"><i class="fa fa-check-circle-o fa-lg fa-fw"></i>OK</span>';

        //Status description
        if (this.curatable.lastNeedsAttentionEvent) {
            str +=  '<div class="dark-gray v-padded">' +
                        ' <i class="fa fa-calendar"></i> ' +
                        'As of ' + this.curatable.lastNeedsAttentionEvent.date.toLocaleString() +
                        ' <i class="fa fa-user"></i> ' +
                        ' set by: ' + this.curatable.lastNeedsAttentionEvent.performer +
                        ' <i class="fa fa-pencil"></i>' +
                        ' Details: ' + this.getAuditEventNonNullDescription(this.curatable.lastNeedsAttentionEvent) +
                    '</div>';
        }
        return str;
    },

    /**
     * Composes the string for trouble status description
     * @returns {string} string containing the status with appropriate icon, color, date and performer.
     */
    getTroubleStatusHtml: function () {
        // Base status
        // This is messy because we have to detect whether we are dealing with ArrayDesign or ExpressionExperiment
        var str =       (this.curatable.actuallyTroubled !== undefined && this.curatable.actuallyTroubled === true)
                    ||  (this.curatable.actuallyTroubled === undefined && this.curatable.troubled)
            ?   '<span class="red width130"><i class="fa fa-exclamation-triangle fa-lg fa-fw"></i>Troubled</span>'
            :   '<span class="green width130"><i class="fa fa-check-circle fa-lg fa-fw"></i>Not Troubled</span>';

        if( this.curatable.actuallyTroubled !== undefined
            && this.curatable.troubled){
            str+=   '<span class="gray-red">' +
                ' <i class="fa fa-exclamation-triangle"></i> Platform troubled ' +
                '</span>';
        }

        // Status details
        if (this.curatable.lastTroubledEvent) {
            str +=  '<div class="dark-gray v-padded">' +
                        ' <i class="fa fa-calendar"></i> ' +
                        'As of ' + this.curatable.lastTroubledEvent.date.toLocaleString() +
                        ' <i class="fa fa-user"></i> ' +
                        ' set by: ' + this.curatable.lastTroubledEvent.performer +
                        ' <i class="fa fa-pencil"></i>' +
                        ' Details: ' + this.getAuditEventNonNullDescription(this.curatable.lastTroubledEvent) +
                    '</div>'
        }

        return str;
    },

    /**
     * Iterates through the events detail and note, and creates an adequate description from the found strings.
     * @param AuditEvent the event to be described.
     * @returns {string} description of the given event, or "Unspecified" if no suitable string is found.
     */
    getAuditEventNonNullDescription: function(AuditEvent){
        var str = "";
        var defaultStr = "Unspecified";

        if(AuditEvent.detail) str += AuditEvent.detail;
        if(AuditEvent.detail && AuditEvent.note) str+= " - ";
        if(AuditEvent.note) str += AuditEvent.note;

        if(str.length > 100){
            str = str.substring(0,99) + "...";
        }

        if(str.length > 0) return str;
        return defaultStr;
    }

});