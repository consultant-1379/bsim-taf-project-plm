package com.ericsson.oss.bsim.test.cases;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TestCase;
import com.ericsson.cifwk.taf.TorTestCaseHelper;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.oss.bsim.batch.data.model.MockLRANPicoBatch;
import com.ericsson.oss.bsim.data.model.NodeType;
import com.ericsson.oss.bsim.operators.BsimBatchOperator;
import com.ericsson.oss.bsim.operators.api.BsimDeleteNodeApiOperator;
import com.ericsson.oss.bsim.robustness.BsimPreCheckManager;
import com.ericsson.oss.bsim.test.data.BsimTestData;
import com.ericsson.oss.bsim.utils.BsimTestCaseReportHelper;

public class BsimLRANBatch extends TorTestCaseHelper implements TestCase {

    private static Logger log;

    private final List<String> nodesToDelete = new ArrayList<String>();

    private final List<MockLRANPicoBatch> addedBatches = new ArrayList<MockLRANPicoBatch>();

    @BeforeClass
    public void prepareTheRun() {

        // assertTrue(new PreCheckUpgradePackages().checkUpgradePackages());
        log = Logger.getLogger(BsimLRANBatch.class);

        final BsimPreCheckManager preCheckManager = new BsimPreCheckManager(NodeType.PICO_LTE, new BsimTestCaseReportHelper(this));
        Assert.assertEquals(preCheckManager.doAllPreChecks(), true);

    }

    @Test(dataProvider = "newLRANPicoBatch", dataProviderClass = BsimTestData.class, groups = { "KGB" })
    public void addLRANPicoBatch(
            @TestId @Input("TC ID") final String tcId,
            final String tcTitle,
            final String tcDesc,
            final MockLRANPicoBatch mockLRANPicoBatch,
            final String bind,
            final String nodesTobind,
            final String deleteBoundNodes,
            final String batchExpectedResult,
            final String bindExpectedResult,
            final String deleteExpectedResult) throws InterruptedException {

        setTestcase(tcId, tcTitle);
        setTestInfo(tcDesc);

        final BsimBatchOperator bsimBatchOperator = new BsimBatchOperator();
        final BsimAddPicoBatchHelper bsimAddPicoBatchHelper = new BsimAddPicoBatchHelper();

        final List<String> fdnPreCheckList = new ArrayList<String>();
        final Iterator<String> iterator = mockLRANPicoBatch.getNodeFdnValues().iterator();
        String s = "";
        while (iterator.hasNext()) {
            s = iterator.next();
            if (moExist(bsimBatchOperator, s).equals("true")) {
                log.error("PreCheck: MO Exists " + s);
                fdnPreCheckList.add(s);
            }
        }

        if (!fdnPreCheckList.isEmpty()) {
            log.info("PreCheck: Attempting to delete found nodes");
            deleteBindLRANBatchNodes(bsimBatchOperator, fdnPreCheckList, deleteExpectedResult);
        }

        bsimAddPicoBatchHelper.doAddBatch(bsimBatchOperator, mockLRANPicoBatch);
        bsimAddPicoBatchHelper.checkQRCodeIsGenerated(bsimBatchOperator, mockLRANPicoBatch.getName());
        addedBatches.add(mockLRANPicoBatch);

        if (bind.equalsIgnoreCase("true") && !nodesTobind.equals("")) {
            List<String> boundNodeFdns;

            if (!mockLRANPicoBatch.getIsNoHardwareBind()) {
                boundNodeFdns = bsimAddPicoBatchHelper.bindBatch(bsimBatchOperator, mockLRANPicoBatch, Integer.parseInt(nodesTobind), bindExpectedResult);
            } else {
                boundNodeFdns = bsimAddPicoBatchHelper.executeNoHardwareBindLRANBatch(bsimBatchOperator, mockLRANPicoBatch, Integer.parseInt(nodesTobind),
                        bindExpectedResult);
            }
            nodesToDelete.addAll(boundNodeFdns);
        }

        // if (deleteBoundNodes.equalsIgnoreCase("true") && !nodesToDelete.isEmpty()) {
        // deleteBindLRANBatchNodes(bsimBatchOperator, nodesToDelete, deleteExpectedResult);
        // }

        // Clean Up
        // cleanUp(bsimBatchOperator, mockLRANPicoBatch, "true");
    }

    private String moExist(final BsimBatchOperator bsimBatchOperator, final String s) {

        return bsimBatchOperator.moExist(s);
    }

    private String deleteBindLRANBatchNodes(final BsimBatchOperator bsimBatchOperator, final List<String> nodesToDelete, final String expectedResult) {

        final String actualResult = bsimBatchOperator.deletebindLRANBatchNodes(nodesToDelete);
        Assert.assertEquals(expectedResult, actualResult);
        return actualResult;
    }

    @AfterClass(alwaysRun = true)
    private void cleanUp() {

        setTestcase("BSIM_CLEANUP_LRAN_BATCH", "TAF - clean up server after tests have run");
        setTestStep("After Class ==> Deleting Nodes");

        final BsimBatchOperator bsimBatchOperator = new BsimBatchOperator();

        if (!nodesToDelete.isEmpty()) {
            setTestStep("Deleting LRAN batch nodes via calling BSIM Service...");
            try {
                final String actualResult = deleteBindLRANBatchNodes(bsimBatchOperator, nodesToDelete, "Successful");
                Assert.assertEquals(actualResult, "Successful");
            } catch (final Exception e) {
                log.error("Error when deleting bound nodes with exception: " + e.getMessage());
            } catch (final AssertionError e) {
                // allow Cleanup to complete
            }
        } else {
            log.info("No nodes were bound during test case ==> No nodes have been deleted");
        }
        setTestStep("After Class ==> Checking Nodes have been deleted");
        Assert.assertTrue(checkBoundNodesDeletedInCs(bsimBatchOperator));
        setTestStep("After Class ==> Deleting Batches");
        deleteLranBatchesAfterTestExecution(bsimBatchOperator);
        setTestStep("After Class ==> Delete CCF");
        deleteNoHardwareCCFAfterTestExecution();
    }

    private boolean checkBoundNodesDeletedInCs(final BsimBatchOperator bsimBatchOperator) {
        return bsimBatchOperator.checkBoundNodesDeletedInCS(nodesToDelete);
    }

    private void deleteLranBatchesAfterTestExecution(final BsimBatchOperator bsimBatchOperator) {
        for (final MockLRANPicoBatch addedBatch : addedBatches) {
            try {
                log.info("Deleting Batch ==> " + addedBatch.getName());
                final String actualResult = bsimBatchOperator.deleteLRANBatch(addedBatch);
                Assert.assertEquals(actualResult, "true");
                setTestStep("Verify SMRS Account has been deleted after Clean Up");
                Assert.assertFalse(bsimBatchOperator.checkDoesSmrsAccountExistforBatch(addedBatch, false));
            } catch (final Exception e) {
                log.error("Failed to delete Batch ==> " + addedBatch.getName() + "with exception " + e.getMessage());
            } catch (final AssertionError e) {
                // allow the cleanup to complete despite AssertionError
            }
        }
    }

    /**
     * Deletes Combined Configuration File from the server after the NoHardware Bind Test has ran
     */
    private void deleteNoHardwareCCFAfterTestExecution() {

        final BsimDeleteNodeApiOperator deleteNodeOperator = new BsimDeleteNodeApiOperator();
        deleteNodeOperator.deleteCCFFile();
    }
}
