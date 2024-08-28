/*------------------------------------------------------------------------
 *
 *
 *      COPYRIGHT (C)                   ERICSSON RADIO SYSTEMS AB, Sweden
 *
 *      The  copyright  to  the document(s) herein  is  the property of
 *      Ericsson Radio Systems AB, Sweden.
 *
 *      The document(s) may be used  and/or copied only with the written
 *      permission from Ericsson Radio Systems AB  or in accordance with
 *      the terms  and conditions  stipulated in the  agreement/contract
 *      under which the document(s) have been supplied.
 *
 *------------------------------------------------------------------------
 */

package com.ericsson.oss.bsim.robustness.precheck;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ericsson.oss.bsim.data.model.NodeType;
import com.ericsson.oss.bsim.getters.api.BsimApiGetter;
import com.ericsson.oss.bsim.getters.api.BsimRemoteCommandExecutor;

public class PreCheckRegisteredService implements IBsimPreChecker {

    private static Logger log = Logger.getLogger(PreCheckRegisteredService.class);

    private static BsimRemoteCommandExecutor remoteCommandExecutor = BsimApiGetter.getRemoteCommandExecutor(BsimApiGetter.getHostMaster());

    @Override
    public boolean doPreCheck(final NodeType nodeType) {

        final String cmdString = "/opt/ericsson/nms_cif_sm/bin/smtool -action OsgiFwk bundle com.ericsson.oss.bsim.server 1.0.1 | grep -i \"Registered Service\"";
        final String result = remoteCommandExecutor.simpleExec(cmdString);
        log.info("Check registered service result: " + result);
        if (result.toLowerCase().contains("no registered service")) {
            return false;
        } else {
            if (result.toLowerCase().contains("registered service")) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    @Test(groups = { "common.precheck" }, alwaysRun = true)
    public void doPreCheck() {
        Assert.assertEquals(doPreCheck(null), true);
    }

    @Override
    public String getCheckDescription() {

        return "Check registered service for bundle com.ericsson.oss.bsim.server_1.0.1...";
    }

}
