<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

        <title>
            <fmt:message key="compositeSequences.title" />
        </title>

        <h2>
            <fmt:message key="compositeSequences.title" />
        </h2>

        <display:table name="compositeSequences" class="list" requestURI="" id="compositeSequenceList"
             pagesize="20">
			<display:column property="name" sortable="true" titleKey="compositeSequence.name" maxWords="20" />
			<display:column property="arrayDesign.shortName" sortable="true" title="Array Design" maxWords="20" 
			href="/Gemma/arrays/showArrayDesign.html" paramId="id" paramProperty="arrayDesign.id" />			
			<display:column property="biologicalCharacteristic.name" sortable="true" title="Biosequence" maxWords="20"
			href="/Gemma/genome/bioSequence/showBioSequence.html" paramId="id" paramProperty="biologicalCharacteristic.id" />		
			<display:column property="description" sortable="true" titleKey="compositeSequence.description" maxWords="100" />			 
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>
