import com.ericsson.oss.bsim.ui.core.BsimServiceManager;
import com.ericsson.oss.bsim.domain.AddNodeData;
import com.ericsson.oss.bsim.domain.TemplateTypes;
import com.ericsson.oss.bsim.domain.NodeType;
import com.ericsson.oss.bsim.ui.service.iface.IAddNodesStatusReceiver;
import com.ericsson.oss.bsim.domain.messages.AddNodesStatus;
import com.ericsson.oss.bsim.domain.messages.ManualBindNodeStatusMessage;
import com.ericsson.oss.bsim.ui.service.iface.IManualBindNodeStatusReceiver;
import com.ericsson.oss.bsim.domain.messages.AddNodesStatusMessage;
import com.ericsson.oss.bsim.ui.service.jms.MessageHandler;
import com.ericsson.oss.bsim.domain.SecurityData;
import com.ericsson.oss.bsim.domain.NetworkConfiguration;
import com.ericsson.oss.bsim.ui.core.MimManagerHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import com.ericsson.oss.bsim.domain.DG2Domain;


class BsimAddNodeOperator {

    private AddNodeData addNodeData;

    final private List<AddNodeData> nodeDataList = new ArrayList<AddNodeData>();

    private NodeType nodeType;



    private DG2Domain dg2Domain;

    private String mimVersion;

    private String ipVersion;

    private String rnc;

    private String rbsGroup;

    private String macroBindResult;

    LinkedHashMap<String, String> nodeTemplateAttrs = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> transportTemplateAttrs = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> radioTemplateAttrs = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> siteBasicTemplateAttrs = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> siteEquipmentTemplateAttrs = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> siteInstallationTemplateAttrs = new LinkedHashMap<String, String>();

    LinkedHashMap<String, String> cabinetEquipmentTemplateAttrs = new LinkedHashMap<String, String>();

    public String createLTEAddNodeData(String nodeName, String eRbsFdn, String nodeTemplateName, String mimVersion, String ipVersion) {


        this.addNodeData = new AddNodeData(nodeName, eRbsFdn, NodeType.ERbs);
        nodeType = NodeType.ERbs;
        this.mimVersion = mimVersion;
        println"mimVersion is ==> "+mimVersion;
        this.ipVersion = ipVersion;

        println"ipVersion is ==> "+ipVersion;
        return this.addNodeData.getNodeName();
    }


    public String createDG2AddNodeData(String nodeName, String dg2Fdn, String nodeTemplateName,String domainDG2,String rnc) {


        this.addNodeData = new AddNodeData(nodeName, dg2Fdn, NodeType.DG2);
        nodeType = NodeType.DG2;
        this.dg2Domain=DG2Domain.dg2DomainTypeFromString(domainDG2);

        this.addNodeData.setDg2Domain(this.dg2Domain);
        this.rnc = rnc;
        this.addNodeData.getAifData().setNodeType(NodeType.DG2);
        println"TAF LOGGING:-> rnc is ==> "+rnc;
        return this.addNodeData.getDg2SwUpgradePackageLocation();
    }
    public String createMicroLTEAddNodeData(String nodeName, String eRbsFdn, String nodeTemplateName, String mimVersion, String ipVersion) {

        this.addNodeData = new AddNodeData(nodeName, eRbsFdn, NodeType.MicroERbs);
        nodeType = NodeType.MicroERbs;
        this.mimVersion = mimVersion;
        println"TAF LOGGING:-> mimVersion is ==> "+mimVersion;
        this.ipVersion = ipVersion;

        println"TAF LOGGING:-> ipVersion is ==> "+ipVersion;
        return this.addNodeData.getNodeName();
    }

    public String createWCDMAAddNodeData(String nodeName, String rbsFdn, String nodeTemplateName,String rnc, String rbsGroup, String mimVersion, String ipVersion) {

        this.addNodeData = new AddNodeData(nodeName, rbsFdn, NodeType.Rbs);
        nodeType = NodeType.Rbs;
        this.rnc = rnc;
        println"TAF LOGGING:-> rnc is ==> "+rnc;
        this.rbsGroup = rbsGroup;
        println"TAF LOGGING:-> rbsGroup is ==> "+rbsGroup;
        addNodeData.setNodeVersion("W16.0");
        addNodeData.setMimVersion(mimVersion);
        this.mimVersion = mimVersion;
        this.ipVersion = ipVersion;
        return this.addNodeData.getNodeName();
    }

    public String createMicroWCDMAAddNodeData(String nodeName, String rbsFdn, String nodeTemplateName,String rnc, String rbsGroup, String mimVersion, String ipVersion) {

        this.addNodeData = new AddNodeData(nodeName, rbsFdn, NodeType.MicroRbs);
        nodeType = NodeType.MicroRbs;
        println"TAF LOGGING:-> In createMicroWCDMAAddNodeData in groovy"
        this.rnc = rnc;
        println"TAF LOGGING:-> rnc is ==> "+rnc;
        this.rbsGroup = rbsGroup;
        println"TAF LOGGING:-> rbsGroup is ==> "+rbsGroup;
        addNodeData.setNodeVersion("W16.0");
        addNodeData.setMimVersion(mimVersion);
        this.mimVersion = mimVersion;
        this.ipVersion = ipVersion;
        return this.addNodeData.getNodeName();
    }

    public def setAttributeForAddNodeDataObject(String key, String value){
        // set value in AddNodeData object for some specific attributes

        //GENERAL TAB ATTRIBUTES
        //Common to all Nodes

        if(key.equalsIgnoreCase("OSS Node Template")){
            addNodeData.setTemplateName(TemplateTypes.getNodeTemplateType(nodeType), value);
        }//Scrambling Code
        else if(key.equalsIgnoreCase("isUseScProfile")){
            if(value.equalsIgnoreCase("true")){
                addNodeData.setUseSCProfile(true);
            }else{
                addNodeData.setUseSCProfile(false);
            }
        }
        if(key.equalsIgnoreCase("SC Profile")){
            addNodeData.setScProfileName(value);
        }
        else if(key.equalsIgnoreCase("Plan Name")){
            addNodeData.setPlannedAreaName(value);
            String[] planList = [value];
            addNodeData.getAifData().setPlannedAreas(planList);
        }
        else if(key.equalsIgnoreCase("SubNetwork Group")){    //LTE Specific
            addNodeData.setSubNetworkGroupValue(value);
        }
        else if(key.equalsIgnoreCase("RNC Name")){  //WCDMA Specific
            println"TAF LOGGING:-> setting rnc on addNodeData bsim object ==> "+rnc;
            addNodeData.setRncName(rnc);
        }
        else if(key.equalsIgnoreCase("Group Name")){    //WCDMA Specific
            println"TAF LOGGING:-> setting rbsGroup on addNodeData bsim object ==> "+rbsGroup;
            addNodeData.setSubNetworkGroupValue(rbsGroup);
        }
        else if(key.equalsIgnoreCase("Site")){
            addNodeData.setSiteName(value);
        }

        //TRANSPORT TAB ATTRIBUTES
        else if(key.equalsIgnoreCase("Import Transport Configuration")){
            if (value.equalsIgnoreCase("true")) {
                addNodeData.setUseTransportConfiguration(true);
            }else{
                addNodeData.setUseTransportConfiguration(false);
            }
        }
        else if(key.equalsIgnoreCase("Use Transport CM Template")){
            if (value.equalsIgnoreCase("true")) {
                addNodeData.setUseTransportCmTemplateFile();
            }
        }
        else if(key.equalsIgnoreCase("Transport Template Name")){
            addNodeData.setTemplateName(TemplateTypes.getTransportTemplateType(nodeType), value);
        }
        //RADIO TAB ATTRIBUTES
        else if(key.equalsIgnoreCase("Import Radio Configuration")){
            if (value.equalsIgnoreCase("true")) {
                addNodeData.setUseRadioConfiguration(true);
            }else{
                addNodeData.setUseRadioConfiguration(false);
            }
        }
        else if(key.equalsIgnoreCase("Use Radio CM Template")){
            if (value.equalsIgnoreCase("true")) {
                addNodeData.setUseRadioTemplateFile(true);
            }
        }
        else if(key.equalsIgnoreCase("Radio Template Name")){
            addNodeData.setTemplateName(TemplateTypes.getRadioTemplateType(nodeType), value);
        }

        return "OK";
    }

    public def setAttributeForAddNodeDataObjectDG2(String key, String value){
        if(key.equalsIgnoreCase("OSS Node Template")){
            addNodeData.setTemplateName(TemplateTypes.getNodeTemplateType(addNodeData), value);
        }
        if(key.equalsIgnoreCase("SC Profile")){
            addNodeData.setScProfileName(value);
        }
        else if(key.equalsIgnoreCase("Plan Name")){
            addNodeData.setPlannedAreaName(value);
        }
        else if(key.equalsIgnoreCase("SubNetwork Group")){
            addNodeData.setSubNetworkGroupValue(value);
        }
        if(key.equalsIgnoreCase("Upgrade Package File Path")){
            addNodeData.setDg2SwUpgradePackageLocation(value);
        }
        else if(key.equalsIgnoreCase("Site")){
            addNodeData.setSiteName(value);
        }
        else if(key.equals("DG2 Domain")) {
            addNodeData.setDg2Domain(value);
        }
        else if(key.equalsIgnoreCase("RNC Name")){
            println"TAF LOGGING:-> setting rnc on addNodeData bsim object ==> "+rnc;
            addNodeData.setRncName(rnc);
        }
        return "OK";
    }


    public int setAttributeForNodeTemplate(String key, String value) {

        // store the attributes to nodeTemplateAttrs map
        nodeTemplateAttrs.put(key, value);
        return nodeTemplateAttrs.size();
    }

    public int setAttributeForTransportTemplate(String key, String value) {

        transportTemplateAttrs.put(key, value);
        return transportTemplateAttrs.size();
    }

    public int setAttributeForRadioTemplate(String key, String value) {

        radioTemplateAttrs.put(key, value);
        return radioTemplateAttrs.size();
    }

    // -------------- Auto Integrate --------------
    public def setAttributeForAifOptionData(String key, String value) {

        // auto integrate switch
        if (key.equalsIgnoreCase("Auto Integrate")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setAutoIntegrate(true);
            }
            else {
                addNodeData.getAifData().setAutoIntegrate(false);
            }
        }
        // ------ aif options ------
        else if (key.equalsIgnoreCase("Unlock Cells")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setUnLockCells(true);
            }
            else {
                addNodeData.getAifData().setUnLockCells(false);
            }
        }
        else if(key.equalsIgnoreCase("Without Laptop")){
            if(value.equalsIgnoreCase("true")){
                String nodeVersion = MimManagerHelper.getInstance().getNodeVersion(addNodeData.getNodeType(), mimVersion);
                addNodeData.setNodeVersion(nodeVersion);
                addNodeData.getAifData().setWithoutLaptopIntegration(true);
            }
            else{
                addNodeData.getAifData().setWithoutLaptopIntegration(false);
            }
        }
        else if (key.equalsIgnoreCase("Upload CV after plan activation")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setUploadCVAfterPlanActivation(true);
            }
            else {
                addNodeData.getAifData().setUploadCVAfterPlanActivation(false);
            }
        }
        else if (key.equalsIgnoreCase("Upload CV after auto integration")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setUploadCVAfterAutointegration(true);
            }
            else {
                addNodeData.getAifData().setUploadCVAfterAutointegration(false);
            }
        }
        else if (key.equalsIgnoreCase("Install License")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setLicenseInstall(true);
            }
            else {
                addNodeData.getAifData().setLicenseInstall(false);
            }
        }
        else if (key.equalsIgnoreCase("Activate License")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setLicenseActivate(true);
            }
            else {
                addNodeData.getAifData().setLicenseActivate(false);
            }
        }

        else if (key.equalsIgnoreCase("Site Basic Template")) {
            addNodeData.getAifData().setUseSiteBasicTemplateFile(true);
            addNodeData.setTemplateName(TemplateTypes.getSiteBasicTemplateType(nodeType), value);
        }

        else if (key.equalsIgnoreCase("Site Equipment Template")) {
            addNodeData.getAifData().setUseSiteEquipmentTemplateFile(true);
            addNodeData.setTemplateName(TemplateTypes.getSiteEquipmentTemplateType(nodeType), value);
        }

        else if (key.equalsIgnoreCase("Site Installation Template")) {
            addNodeData.getAifData().setUseSiteInstallationTemplateFile(true);
            addNodeData.setTemplateName(TemplateTypes.getSiteInstallationTemplateType(nodeType), value);
        }

        else if (key.equalsIgnoreCase("Cabinet Equipment Template")) {
            addNodeData.getAifData().setUseCabinetEquipmentTemplateFile(true);
            addNodeData.setTemplateName(TemplateTypes.getCabinetEquipmentTemplateType(nodeType), value);
        }

        else if (key.equalsIgnoreCase("Security")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setSecurity(true);
                addNodeData.getAifData().getSecurityData().setMimVersion(mimVersion);
                addNodeData.getAifData().getSecurityData().setIpVersion(ipVersion);
            }
        }

        return "OK";
    }


    // -------------- Auto Integrate --------------
    public def setAttributeForAifOptionDataDG2(String key, String value) {
        // auto integrate switch
        if (key.equalsIgnoreCase("Auto Integrate")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setAutoIntegrate(true);
            }
            else {
                addNodeData.getAifData().setAutoIntegrate(false);
            }
        }
        // ------ aif options ------
        else if (key.equalsIgnoreCase("Unlock Cells")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setUnLockCells(true);
            }
            else {
                addNodeData.getAifData().setUnLockCells(false);
            }
        }
        else if (key.equalsIgnoreCase("Install License")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setLicenseInstall(true);
            }
            else {
                addNodeData.getAifData().setLicenseInstall(false);
            }
        }
         else if (key.equalsIgnoreCase("isUsingOssNodeProtocolFile")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setUsingOssNodeProtocolFile(true);
            }
            else {
                addNodeData.getAifData().setUsingOssNodeProtocolFile(false);
            }
        }
        else if (key.equalsIgnoreCase("Activate License")) {
            if (value.equalsIgnoreCase("true")) {
                addNodeData.getAifData().setLicenseActivate(true);
            }
            else {
                addNodeData.getAifData().setLicenseActivate(false);
            }
        }
        else if (key.equalsIgnoreCase("Site Basic File")) {
            addNodeData.getAifData().setSiteBasicFileName(value);
        }
        else if (key.equalsIgnoreCase("Site Equipment File")) {
            addNodeData.getAifData().setSiteEquipmentFileName(value);
        }
        else if (key.equalsIgnoreCase("OssNodeProtocol File")) {
            addNodeData.getAifData().setOssNodeProtocolFileName(value);
        }
        return "OK";
    }




    public int setAttributeForSiteBasicTemplate(String key, String value) {

        siteBasicTemplateAttrs.put(key, value);
        return siteBasicTemplateAttrs.size();
    }

    public int setAttributeForSiteEquipmentTemplate(String key, String value) {

        siteEquipmentTemplateAttrs.put(key, value);
        return siteEquipmentTemplateAttrs.size();
    }

    public int setAttributeForSiteInstallationTemplate(String key, String value) {

        siteInstallationTemplateAttrs.put(key, value);
        return siteInstallationTemplateAttrs.size();
    }

    public int setAttributeForCabinetEquipmentTemplate(String key, String value) {

        cabinetEquipmentTemplateAttrs.put(key, value);
        return cabinetEquipmentTemplateAttrs.size();
    }

    // -------------- Add Node --------------
    AddNodesStatus nodeStatus;

    public def addNode() {

        log("Entering addNode()");
        nodeStatus = AddNodesStatus.NOT_STARTED;

        prepareTemplates();

        // Call BSIM Service to add the node with the ready-to-use AddNodeData
        // object
        log("***Adding Node to list, see data below ******");
        log(addNodeData.toString());

        nodeDataList.add(addNodeData);
        log("Exiting addNode()");
        return nodeDataList.size();
    }

    public def addNodeDG2() {

        log("Entering addNode()");
        nodeStatus = AddNodesStatus.NOT_STARTED;

        prepareTemplatesDG2();

        // Call BSIM Service to add the node with the ready-to-use AddNodeData
        // object
        log("***Adding Node to list, see data below ******");
        log(addNodeData.toString());

        nodeDataList.add(addNodeData);
        log("Exiting addNode()");
        return nodeDataList.size();
    }


    def log(String message) {
        def now = Calendar.instance
        println "TAF LOGGING:-> $now.time: $message";
    }

    def logError(String message) {
        def now = Calendar.instance
        println "TAF LOGGING:->ERROR $now.time: $message";
    }

    public String execute(String nodesToAdd){
        log("Entering execute(String nodesToAdd)");
        int numberOfNodes = Integer.parseInt(nodesToAdd);
        log("Logging NODE DATA LIST ******** CONTAINS " + nodesToAdd + " NODE(S) TO ADD ***********")
        BsimServiceManager bsimServiceManagerInstance = BsimServiceManager.getInstance();
        log("Executing bsimServiceManagerInstance.getBsimService().addNodes(nodeDataList, addNodeData.getPlannedAreaName())" )
        bsimServiceManagerInstance.getBsimService().addNodes(nodeDataList, addNodeData.getPlannedAreaName());
        int sucessCount =0;
        int failCount =0;
        if (numberOfNodes > 1){
            final CountDownLatch latch = new CountDownLatch(1);

            final int minuteMultiplier = 2;
            int timeOutInMinutes = numberOfNodes * minuteMultiplier;

            bsimServiceManagerInstance.getMessageHandler().addNodeStatusReceiver( new IAddNodesStatusReceiver() {

                        @Override
                        public void addNodeStatusReceived( final AddNodesStatusMessage msg) {
                            nodeStatus = msg.getStatus();
                            log(msg.getFdn() + " " + nodeStatus.getStatus());
                            if (nodeStatus.getStatus().equals("Successful" )) {
                                sucessCount++;
                                if (sucessCount == numberOfNodes){
                                    log("Sucessfully added all "+numberOfNodes+ " nodes");

                                    latch.countDown();
                                }
                            }
                            else if (nodeStatus.getStatus().contains("Unsuccessful")){
                                log(nodeStatus.getFdn() + " " + nodeStatus.getStatus());
                                failCount++;
                                if (failCount ==1){
                                    logError("In fail count, error adding node check osgi logs");
                                    latch.countDown();
                                }
                            }
                        }

                    });

            try {
                // use number of nodes as minutes to count down.
                // This allows one minute per node
                latch.await(timeOutInMinutes, TimeUnit.MINUTES);
            }
            catch (final InterruptedException e) {
                e.printStackTrace();
                log(e)
            }
        }
        //make sure list is cleared
        nodeDataList.clear();
        log("Cleared nodeDataList, size is set back to " + nodeDataList.size().toString());
        log("Exiting execute(String nodesToAdd)");
        return String.valueOf(sucessCount);

    }

    //////////////////////No Hardware Bind///////////////////////////////////

    public String executeNoHardwareBindDG2(final String nodeName,final String saveAs, final String siteInstallTemplateName, final String OAMIpAddress){

        Map<String, String> siteInstallAttrs = new HashMap<String, String>();

        siteInstallAttrs.put("Integration OaM IP Address", OAMIpAddress);
        if(addNodeData.getDg2Domain()==(DG2Domain.LRAN))
        {

            siteInstallAttrs.put("FTP Service", "SubNetwork=ONRM_ROOT_MO,FtpServer=SMRSSLAVE-LRAN-nedssv4,FtpService=aiflran");
        }
        else
        {
            siteInstallAttrs.put("FTP Service", "SubNetwork=ONRM_ROOT_MO,FtpServer=SMRSSLAVE-WRAN-nedssv4,FtpService=aifwran");
        }
        executeNoHardwareBindDG2(nodeName,saveAs, siteInstallTemplateName, siteInstallAttrs);
    }




    public String executeNoHardwareBindDG2(final String nodeName,final String saveAs, final String siteInstallTemplateName, final Map<String, String> siteInstallAttrs) {

        final CountDownLatch latch = new CountDownLatch(1);


        BsimServiceManager.getInstance().getBsimService().executeNodeNoHardwareBind(nodeName,addNodeData.getNodeType(),addNodeData.getDg2Domain(), saveAs, siteInstallTemplateName, siteInstallAttrs);
        try {
            latch.await(4, TimeUnit.MINUTES);
        }
        catch (final InterruptedException e) {
            return e.printStackTrace().toString();
        }       
        return "Successful";
    }
    ///////////////////////////////////////////////////////////


    /////////////////////////////Manual Bind of RadioNode //////////////////////////////////


    public String executeBindRadioNode(final String nodeName,final String saveAs, final String siteInstallTemplateName, final String OAMIpAddress,final String serialNumber){

        Map<String, String> siteInstallAttrs = new HashMap<String, String>();

        siteInstallAttrs.put("Integration OaM IP Address", OAMIpAddress);
        if(addNodeData.getDg2Domain()==(DG2Domain.LRAN))
        {

            siteInstallAttrs.put("FTP Service", "SubNetwork=ONRM_ROOT_MO,FtpServer=SMRSSLAVE-LRAN-nedssv4,FtpService=aiflran");
        }
        else
        {
            siteInstallAttrs.put("FTP Service", "SubNetwork=ONRM_ROOT_MO,FtpServer=SMRSSLAVE-WRAN-nedssv4,FtpService=aifwran");
        }
        executeBindRadioNode(nodeName,saveAs, siteInstallTemplateName, siteInstallAttrs,serialNumber);
    }




    public String executeBindRadioNode(final String nodeName,final String saveAs, final String siteInstallTemplateName, final Map<String, String> siteInstallAttrs, final String serialNumber ) {

        final CountDownLatch latch = new CountDownLatch(1);
        
         BsimServiceManager bsimServiceManagerInstance = BsimServiceManager.getInstance();
        
        bsimServiceManagerInstance.getBsimService().executeNodeHardwareBind(nodeName,addNodeData.getNodeType(),addNodeData.getDg2Domain(), saveAs, siteInstallTemplateName, siteInstallAttrs,serialNumber);
        
        try {
            latch.await(4, TimeUnit.MINUTES);
        }
        catch (final InterruptedException e) {
            return e.printStackTrace().toString();
        }
        
         return "Successful";
    }
    ///////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////////////////////////

    public String getAddNodeStatus() {

        // return rollBackStatusMessageHandlerInstance.getAddNzodeStatus();
        return nodeStatus.getStatus();
    }

    public String getStatusMessageLog() {

        return logInfo;
    }

    private void prepareTemplates() {

        processAttributesByTemplate(TemplateTypes.getNodeTemplateType(nodeType), nodeTemplateAttrs);

        processAttributesByTemplate(TemplateTypes.getTransportTemplateType(nodeType), transportTemplateAttrs);

        processAttributesByTemplate(TemplateTypes.getRadioTemplateType(nodeType), radioTemplateAttrs);

        processAttributesByTemplate(TemplateTypes.getSiteBasicTemplateType(nodeType), siteBasicTemplateAttrs);

        processAttributesByTemplate(TemplateTypes.getSiteEquipmentTemplateType(nodeType), siteEquipmentTemplateAttrs);

        processAttributesByTemplate(TemplateTypes.getSiteInstallationTemplateType(nodeType),siteInstallationTemplateAttrs);

        processAttributesByTemplate(TemplateTypes.getCabinetEquipmentTemplateType(nodeType),cabinetEquipmentTemplateAttrs);
    }


    private void prepareTemplatesDG2() {

        processAttributesByTemplate(TemplateTypes.getNodeTemplateType(addNodeData), nodeTemplateAttrs);
    }

    private void processAttributesByTemplate(TemplateTypes templateType, Map<String, String> templateAttrs){
        List<String> templateAttrList = new LinkedList<String>();
        templateAttrList.addAll(templateAttrs.keySet());
        addNodeData.setTemplateAttributeNames(templateType, templateAttrList);
        for (Entry<String, String> ent in templateAttrs.entrySet()) {
            addNodeData.setTemplateAttribute(templateType, ent.getKey(), ent.getValue());
        }
    }

}
