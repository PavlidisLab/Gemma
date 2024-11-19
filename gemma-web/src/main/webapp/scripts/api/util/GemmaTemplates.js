Ext.namespace('Gemma.Widget', 'Gemma.Common');

Gemma.Widget.tpl = {
   ArrayDesignsNonPagingGrid : {
      rowDetails : 'Elements: <b>{designElementCount}</b><br/>'
         + 'With sequences: <b>{numProbeSequences}</b> <span style="color:grey">(Number of elements with sequences)</span><br/>'
         + 'With align: <b>{numProbeAlignments}</b> <span style="color:grey">(Number of elements with at least one genome alignment)</span><br/>'
         + 'Mapped to genes: <b>{numProbesToGenes}</b> <span style="color:grey">(Number of elements mapped to at least one gene)</span><br/>'
         + 'Unique genes: <b>{numGenes}</b> <span style="color:grey">(Number of distinct genes represented by the platform)</span><br/>'
         + ' (as of {dateCached})'
   }
};

Gemma.Common.tpl = {
   pubmedLink : {
      simple : '<a target="_blank" href="{pubmedURL}"><img ext:qtip="Go to PubMed (in new window)" ' + 'src="' + Gemma.CONTEXT_PATH + '/images/pubmed.gif" width="47" height="15" /></a>',
      complex : '<tpl if="pubAvailable==\'true\'">' + '{primaryCitationStr}' + '&nbsp; <a target="_blank" ext:qtip="Go to PubMed (in new window)"'
         + ' href="{pubmedURL}"><img src="' + Gemma.CONTEXT_PATH + '/images/pubmed.gif" ealt="PubMed" /></a>&nbsp;&nbsp' + '</tpl>' + '<tpl if="pubAvailable==\'false\'">' + 'Not Available' + '</tpl>'
   }
};