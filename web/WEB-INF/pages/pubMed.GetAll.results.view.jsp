<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page
    import="edu.columbia.gemma.common.description.BibliographicReference"
    import="edu.columbia.gemma.common.description.DatabaseEntry"
    import="java.util.Calendar"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
</HEAD>
<BODY>
<%-- I know this is webflow related, but it is needed here so when clicking "New Search" 
     we return to the pubMed.Search flow, which is a webflow.
--%>
<FORM name="newSearchForm" action="flowController.htm"><input type="hidden"
    name="_flowId" value="pubMed.Search"> <input type="hidden"
    name="_eventId" value="newSearch"></FORM>
<DIV align="left">


<DIV align="left">
<h2>Search Results</h2>
</DIV>

<display:table name="bibliographicReferences" class="list" requestURI=""
    id="bibliographicReferenceList" export="true">
    <display:column sort="true" href="bibRefDetails.htm"
        paramId="pubMedId" paramProperty="pubAccession.accession"
        title="View details">
        <%="Details"%>
    </display:column>

    <display:column property="title" sort="true" titleKey="pubMed.title"
        maxWords="20" />
    <display:column property="publication" sort="true"
        titleKey="pubMed.publication" />
    <display:column property="authorList" sort="true"
        titleKey="pubMed.authors" />
    <display:column property="publicationDate" sort="true"
        titleKey="pubMed.year" />
    <display:column property="volume" sort="true"
        titleKey="pubMed.volume" />
    <display:column property="pages" sort="true" titleKey="pubMed.pages" />
    <display:setProperty name="basic.empty.showtable" value="true" />
    <display:column sort="true"
        href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=pubmed&dopt=Abstract&list_uids=ID&query_hl=3"
        paramId="list_uids" paramProperty="pubAccession.accession"
        title="Go to PubMed">
        <%="PubMed"%>
    </display:column>
</display:table>

<DIV align="right"><INPUT type="button"
    onclick="javascript:document.newSearchForm.submit()"
    value="New Search"></DIV>

</DIV>
</BODY>
</HTML>
