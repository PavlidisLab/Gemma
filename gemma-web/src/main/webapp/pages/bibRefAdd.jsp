<%@ include file="/common/taglibs.jsp" %>

<%--@elvariable id="bibliographicReference" type="ubic.gemma.model.common.description.BibliographicReference"--%>

<head>
<title>Import ${fn:escapeXml(bibliographicReference.pubAccession.accession)}
    from ${fn:escapeXml(bibliographicReference.pubAccession.externalDatabase.name)}</title>
<meta name="description" content="${fn:escapeXml(bibliographicReference.description)}">
</head>

<div class="padded">
    <h2>Import ${fn:escapeXml(bibliographicReference.pubAccession.accession)}
        from ${fn:escapeXml(bibliographicReference.pubAccession.externalDatabase.name)}</h2>
    <p>
        This reference was obtained from PubMed; it is not in the Gemma system.
    </p>
    <security:authorize access="hasAuthority('GROUP_ADMIN')">
        <form method="POST" action="<c:url value="/bibRef/bibRefAdd.html"/>">
            <input type="hidden" name="accession"
                    value="${fn:escapeXml(bibliographicReference.pubAccession.accession)}">
            <input type="submit" value="Import">
        </form>
    </security:authorize>
</div>
