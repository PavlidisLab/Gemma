<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<%-- Shows the results of a search for pubmed references. --%>

<head>
<title>${fn:escapeXml(bibliographicReference.pubAccession.accession)}
    from ${fn:escapeXml(bibliographicReference.pubAccession.externalDatabase.name)}</title>
<meta name="description" content="${fn:escapeXml(bibliographicReference.description)}">
</head>
<div id="detailPanel"></div>

<script type="text/javascript">
Ext.namespace( 'Gemma' );
Ext.onReady( function() {
   Ext.QuickTips.init();
   var detailPanel = new Gemma.BibliographicReference.DetailsPanel( {
      //loadBibRefId : ${bibliographicReference.id}
      //height: 400,
      //width: 400,
      renderTo : 'detailPanel'
   } );

   detailPanel.loadFromId( ${bibliographicReference.id} );
} );
</script>