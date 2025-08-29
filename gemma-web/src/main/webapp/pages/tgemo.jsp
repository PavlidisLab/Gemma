<%@ include file="/common/taglibs.jsp" %>

<title>The Gemma Ontology</title>

<div class="padded">
    <h1>The Gemma Ontology</h1>
    <p><a href="${pageContext.request.contextPath}/ont/TGEMO.OWL">Download TGEMO</a></p>
    <p>The Gemma Ontology (TGEMO) is a manually curated ontology of terms needed by Gemma.</p>
    <div class="mb-3">
        <p style="margin-bottom: 0;">Retrieve TGEMO in RDF/XML:</p>
        <pre class="mb-3">curl -L -H Accept:application/rdf+xml ${fn:escapeXml(hostUrl)}/ont/TGEMO.OWL</pre>
    </div>
    <ul class="mb-3">
        <c:forEach var="term" items="${terms}">
            <li>
                <Gemma:ontologyResource resource="${term}" />
            </li>
        </c:forEach>
    </ul>
    <p style="margin-bottom: 0"> Other ontologies used by Gemma:</p>
    <ul>
        <li><a href="${pageContext.request.contextPath}/ont/TGFVO">The Gemma Factor Value Ontology</a></li>
    </ul>
</div>