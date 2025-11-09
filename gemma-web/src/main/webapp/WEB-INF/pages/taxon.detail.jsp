<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(taxon.commonName)}</title>
</head>

<div class="padded">
    <h2>${fn:escapeXml(taxon.commonName)}</h2>
    <table>
        <c:if test="${taxon.ncbiId != null}">
            <tr>
                <td class="label">NCBI ID:</td>
                <td><a href="https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=${taxon.ncbiId}"
                        target="_blank"
                        rel="noopener noreferrer">${taxon.ncbiId}</a></td>
            </tr>
        </c:if>
        <tr>
            <td class="label">Scientific name:</td>
            <td>${fn:escapeXml(taxon.scientificName)}</td>
        </tr>
        <tr>
            <td class="label">Number of experiments:</td>
            <td>${numberOfExperiments}</td>
        </tr>
    </table>
</div>