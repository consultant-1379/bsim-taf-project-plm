package com.ericsson.oss.bsim.robustness.precheck;

import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.handlers.RemoteFileHandler;
import com.ericsson.cifwk.taf.tools.cli.CLICommandHelper;
import com.ericsson.cifwk.taf.tools.cli.Shell;
import com.ericsson.oss.bsim.data.model.NodeType;
import com.ericsson.oss.bsim.getters.api.BsimApiGetter;

/**
 * @author ejomclo
 *         <p>
 *         Class checks if SMRS is ready. /p>
 *         <p>
 *         Class not to be added to prechecks yet, waiting on clarification
 *         </p>
 */
public class PreCheckSmrsIsReady implements IBsimPreChecker {

    private static final String PASSWORD = "PIco2013!";

    private static final String SUCCESS = "Success";

    private static final int EXPECT_TIMEOUT_PERIOD = 180;

    private static final String FILE_SYSTEM_NAME = "core_osstest";

    private static final String SLAVE_SERVICE_FILENAME = "pico_slave_service.ini";

    private static final String LOCAL_FILE = new File("").getAbsolutePath().concat(
            "/src/main/resources/templates/picoSmrsSlaveTemplate/" + "pico_slave_service_template.ini");

    private static final String SMRS_CONFIG_FILENAME = new File("").getAbsolutePath().concat(
            "/src/main/resources/templates/picoSmrsSlaveTemplate/" + SLAVE_SERVICE_FILENAME);

    private static final String REMOTE_FILE = "/var/tmp/" + SLAVE_SERVICE_FILENAME;

    private static Logger log = Logger.getLogger(PreCheckSmrsIsReady.class);

    private static RemoteFileHandler masterHostFileHandler = BsimApiGetter.getMasterHostFileHandler();

    private static CLICommandHelper infraServerCLICommandHelper = BsimApiGetter.getCLICommandHelper(BsimApiGetter.getHostInfraServer());

    private static CLICommandHelper ossMasterCLICommandHelper = BsimApiGetter.getCLICommandHelper(BsimApiGetter.getHostMaster());

    private Shell shell = ossMasterCLICommandHelper.openShell();

    private Map<String, String> keyValuePairs;

    private String smrsSlaveNessIp;

    @BeforeClass
    public void setUp() {
        keyValuePairs = new TreeMap<>();
        keyValuePairs.put("SMRS_SLAVE_SERVICE_NAME", "osstest");

    }

    @Override
    public boolean doPreCheck(final NodeType nodeType) {

        return createSfsIfItDoesNotExist() && assignRightsToMaster() && assignRightsToSmrsServer() && configureSmrsSlaveService();

    }

    @Override
    @Test(groups = { "common.precheck" }, alwaysRun = true)
    public void doPreCheck() {
        assertTrue(createSfsIfItDoesNotExist() && assignRightsToMaster() && assignRightsToSmrsServer() && configureSmrsSlaveService());

    }

    private boolean createSfsIfItDoesNotExist() {

        log.info("<font color=purple><B>Start to check if SMRS is ready......</B></font>");
        if (!checkIfSfsExists()) {
            log.info("Sfs storage does not exist");
            createSfsStorage(getSysId());
            return checkIfSfsExists();

        }
        log.info("Sfs storage exists");
        return true;
    }

    private boolean assignRightsToMaster() {

        log.info("Assigning rights to master...");
        String storageIp = getStorageIp();
        String returned = executeCommandAndRespondYes(storageIp);
        return returned.contains(SUCCESS);
    }

    private boolean assignRightsToSmrsServer() {

        log.info("Assigning rights to SMRS server...");
        String smrsStorageIp = getSmrsStorageIp();
        String returned = executeCommandAndRespondYes(smrsStorageIp);
        return returned.contains(SUCCESS);
    }

    private boolean configureSmrsSlaveService() {

        log.info("Checking smrs slave service...");
        getSmrsSlaveNessIp();
        if (!checkLocalFileIsPresent()) {
            log.error("pico slave service template is not present. You need this file for test to work");
            return false;
        }
        configureFile();
        log.info("SMRS Slave service file is configured correctly\nTransferring to server...");
        if (!masterHostFileHandler.copyLocalFileToRemote(SMRS_CONFIG_FILENAME, REMOTE_FILE)) {
            log.error("Failed to transfer pico slave service template to server. ");
            return false;
        }
        shell.writeln("/opt/ericsson/nms_bismrs_mc/bin/configure_smrs.sh add slave_service -f /var/tmp/pico_slave_service.ini");
        shell.expect("What is the password for the local accounts", EXPECT_TIMEOUT_PERIOD);
        shell.writeln(PASSWORD);
        shell.expect("Please confirm the password for the local", EXPECT_TIMEOUT_PERIOD);
        shell.writeln(PASSWORD);
        return masterHostFileHandler.copyLocalFileToRemote(SMRS_CONFIG_FILENAME, REMOTE_FILE);

    }

    private boolean checkLocalFileIsPresent() {

        log.info("Checking if local file is present...");
        return new File(LOCAL_FILE).exists();

    }

    private void getSmrsSlaveNessIp() {

        String ipDetails = getInfraServerIpDetails();
        Pattern pattern = Pattern.compile("index 2\\s+inet\\s+\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}");
        Matcher matcher = pattern.matcher(ipDetails);

        while (matcher.find()) {

            String check = matcher.group();
            String[] parts = check.split(" ");
            smrsSlaveNessIp = parts[parts.length - 1];

        }
        keyValuePairs.put("SMRS_SLAVE_NESS_IP", smrsSlaveNessIp);
    }

    /**
     * <p>
     * This method takes the default SMRS slave service file and edits it
     * </p>
     * <p>
     * Note: clarification waiting on the exact contents of file so this method might be modified to reflect this
     * </p>
     */
    private void configureFile() {

        log.info("Configuring File...");
        try (BufferedReader inputFile = new BufferedReader(new FileReader(LOCAL_FILE))) {
            PrintWriter printWriter = new PrintWriter(new File(SMRS_CONFIG_FILENAME).getAbsoluteFile());
            String s;
            start: while ((s = inputFile.readLine()) != null) {
                if (s.startsWith("#") || s.isEmpty()) {
                    printWriter.println(s);
                    continue;
                } else {
                    for (Map.Entry<String, String> entry1 : keyValuePairs.entrySet()) {
                        String key1 = entry1.getKey();
                        if (s.contains(key1)) {
                            String output = s + entry1.getValue();
                            printWriter.println(output);
                            continue start;
                        }
                    }
                    printWriter.println(s);
                }
            }
            printWriter.close();

        } catch (IOException e) {
            log.error("Unable to read pico_slave_service.ini file");
            e.printStackTrace();
        }
    }

    private String getSmrsStorageIp() {

        String nasHosts = ossMasterCLICommandHelper.simpleExec("more /etc/hosts | grep -i nas");
        String[] parts = nasHosts.split(" ");
        String ip = parts[0];
        String baseIp = ip.substring(0, ip.lastIndexOf('.'));
        String returned = getInfraServerIpDetails();
        Pattern pattern = Pattern.compile("inet " + baseIp + ".\\d{1,3}");
        Matcher matcher = pattern.matcher(returned);
        String storageIp = "";
        while (matcher.find()) {
            storageIp = matcher.group();
        }
        String[] returnedParts = storageIp.split(" ");
        return returnedParts[1].trim();
    }

    private String getInfraServerIpDetails() {
        return infraServerCLICommandHelper.simpleExec("ifconfig -a");
    }

    private String executeCommandAndRespondYes(String storageIp) {

        String cmd = "/ericsson/storage/bin/nascli add_client " + getSysId() + " " + storageIp + " rw,no_root_squash " + FILE_SYSTEM_NAME;
        executeCommand(cmd);
        return shell.expect(SUCCESS, EXPECT_TIMEOUT_PERIOD);

    }

    private void createSfsStorage(String sysId) {

        log.info("Creating SFS storage");
        String command = "/ericsson/storage/bin/nascli create_fs " + sysId + " " + "3G" + " " + "oss1_SMRS" + " " + FILE_SYSTEM_NAME;
        executeCommand(command);

    }

    private void executeCommand(String cmd) {

        shell.writeln(cmd);
        shell.expect("Do you really want to continue ", EXPECT_TIMEOUT_PERIOD);
        shell.writeln("y");

    }

    private String getStorageIp() {

        String[] parts = ossMasterCLICommandHelper.simpleExec("more /ericsson/config/cluster.ini | grep -i storipaddress").split("=");
        return parts[1].trim();
    }

    private boolean checkIfSfsExists() {

        String returned = ossMasterCLICommandHelper.simpleExec("/ericsson/storage/bin/nascli list_clients | grep -i core");
        return returned.contains(FILE_SYSTEM_NAME);
    }

    private String getSysId() {

        String[] parts = ossMasterCLICommandHelper.simpleExec("more /etc/opt/ericsson/nms_bismrs_mc/smrs_config | grep -i sysid").split("=");
        return parts[1].trim();
    }

    @Override
    public String getCheckDescription() {

        return "checking SMRS readyness...";
    }

    @AfterClass
    public void cleanUp() {
        new File(new File("").getAbsolutePath().concat("/src/main/resources/templates/picoSmrsSlaveTemplate/" + SLAVE_SERVICE_FILENAME)).delete();
    }
}
