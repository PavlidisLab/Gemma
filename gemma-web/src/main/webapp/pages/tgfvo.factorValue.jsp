<%@ include file="/common/taglibs.jsp" %>

<title>FactorValue #${factorValueId}: ${fn:escapeXml(oi.label)}</title>

<div class="padded">
    <h2>The Gemma Factor Value Ontology</h2>
    <p><a href="${pageContext.request.contextPath}/ont/TGFVO">All Terms</a></p>
    <h3>FactorValue #${factorValueId}: <Gemma:ontologyResource resource="${oi}" /></h3>
    <p><b>IRI:</b> ${oi.uri}</p>
    <ul class="mb-3">
        <c:if test="${ee != null}">
            <li>belongs to <Gemma:entityLink entity="${ee}">${fn:escapeXml(ee.shortName)}</Gemma:entityLink></li>
        </c:if>
        <c:if test="${oi.instanceOf != null}">
            <li>instance of <Gemma:ontologyResource resource="${oi.instanceOf}" /></li>
        </c:if>
        <c:forEach var="relatedOi" items="${annotations}">
            <li>has annotation <Gemma:ontologyResource resource="${relatedOi}" /></li>
        </c:forEach>
        <c:forEach var="st" items="${statements}">
            <li><Gemma:ontologyResource resource="${st.subject}" />&nbsp;<Gemma:ontologyResource
                    resource="${st.predicate}" />&nbsp;<Gemma:ontologyResource resource="${st.object}" /></li>
        </c:forEach>
    </ul>
    <div class="mb-3">
        <p style="margin-bottom: 0;">Retrieve this in RDF/XML:</p>
        <pre>curl -H Accept:application/rdf+xml ${fn:escapeXml(hostUrl)}/ont/TGFVO/${factorValueId}</pre>
    </div>
    <p style="margin-bottom: 0"> Other ontologies used by Gemma:</p>
    <ul>
        <li><a href="${pageContext.request.contextPath}/ont/TGEMO">Temporary Gemma Ontology</a></li>
    </ul>
</div>
