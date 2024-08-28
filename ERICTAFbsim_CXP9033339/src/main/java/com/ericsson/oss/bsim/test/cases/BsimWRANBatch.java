package com.ericsson.oss.bsim.test.cases;


import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TestCase;
import com.ericsson.cifwk.taf.TorTestCaseHelper;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.oss.bsim.batch.data.model.MockWRANPicoBatch;
import com.ericsson.oss.bsim.data.model.NodeType;
import com.ericsson.oss.bsim.operators.BsimBatchOperator;
import com.ericsson.oss.bsim.operators.api.BsimDeleteNodeApiOperator;
import com.ericsson.oss.bsim.robustness.BsimPreCheckManager;
import com.ericsson.oss.bsim.test.data.BsimTestData;
import com.ericsson.oss.bsim.utils.BsimTestCaseReportHelper;

public class BsimWRANBatch extends TorTestCaseHelper implements TestCase {

    private final List<String> nodesToDeletebyBSIM = new ArrayList<String>();

    private final List<String> nodesToDeletebyARNE = new ArrayList<String>();

    private boolean nodesDeletedbyBsim = false;

    private boolean nodesDeletedbyArne = false;

    private final List<MockWRANPicoBatch> addedBatches = new ArrayList<MockWRANPicoBatch>();

    private BsimDeleteNodeApiOperator deleteNodeOperator;

    @BeforeClass
    public void prepareTheRun() {

        // assertTrue(new PreCheckUpgradePackages().checkUpgradePackages());

        setTestcase("BSIM_PREPARE_FOR_PICO_WRAN_BATCH", "Launch CEX if not running");

        setTestStep("Pre-check before running PICO WCDMA test suite");
        final BsimPreCheckManager preCheckManager = new BsimPreCheckManager(NodeType.PICO_WCDMA, new BsimTestCaseReportHelper(this));
        Assert.assertEquals(preCheckManager.doAllPreChecks(), true);

    }

    @Test(dataProvider = "newWRANPicoBatch", dataProviderClass = BsimTestData.class, groups = { "KGB" })
    public void addWRANPicoBatch(
            @TestId @Input("TC ID") final String tcId,
            final String tcTitle,
            final String tcDesc,
            final MockWRANPicoBatch mockWRANPicoBatch,
            final String bind,
            final String nodesTobind,
            final String deleteBoundNodes,
            final String batchExpectedResult,
            final String bindExpectedResult,
            final String deleteExpectedResult,
            final String isEndToEnd) throws InterruptedException {

        setTestcase(tcId, tcTitle);
        setTestInfo(tcDesc);

        final BsimBatchOperator bsimBatchOperator = new BsimBatchOperator();
        final BsimAddPicoBatchHelper addPicoBatchHelper = new BsimAddPicoBatchHelper();

        addPicoBatchHelper.doAddBatch(bsimBatchOperator, mockWRANPicoBatch);

        addedBatches.add(mockWRANPicoBatch);

        addPicoBatchHelper.checkQRCodeIsGenerated(bsimBatchOperator, mockWRANPicoBatch.getName());

        List<String> boundNodeFdns = new ArrayList<String>();
        if (bind.equalsIgnoreCase("true") && !nodesTobind.equals("")) {
            boundNodeFdns = addPicoBatchHelper.bindBatch(bsimBatchOperator, mockWRANPicoBatch, Integer.parseInt(nodesTobind), bindExpectedResult);
            if (!mockWRANPicoBatch.IsEndToEnd()) {
                nodesToDeletebyBSIM.addAll(boundNodeFdns);
            }
        }

        if (mockWRANPicoBatch.IsEndToEnd()) {

            addPicoBatchHelper.doNetsimSynchronization(mockWRANPicoBatch);
            nodesToDeletebyARNE.addAll(boundNodeFdns);
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() {

        setTestcase("BSIM_CLEANUP_WRAN_BATCH", "TAF - clean up server after tests have run");
        setTestStep("After Class ==> Deleting Nodes");
        final BsimBatchOperator bsimBatchOperator = new BsimBatchOperator();
        final BsimAddPicoBatchHelper addPicoBatchHelper = new BsimAddPicoBatchHelper();

        deleteNodeOperator = new BsimDeleteNodeApiOperator();
        for (final MockWRANPicoBatch picoBatch : addedBatches) {
            // if (picoBatch.getIsAutoPlan()) {
            // deleteNodeOperator.deletePCAwhenAuto(picoBatch);
            // }
            if (!picoBatch.getIsAutoPlan()) {
                deleteNodeOperator.deletePCA(picoBatch.getPlanName());
            }

            if (picoBatch.IsEndToEnd() && nodesDeletedbyArne == false) {
                final String nodeName = picoBatch.getNodeName();
                for (final String nodeFdnValue : picoBatch.getNodeFdnValues()) {
                    deleteNodeOperator.deleteE2EPicoNodeUsingARNE(nodeFdnValue, nodeName);
                }
                nodesDeletedbyArne = true;

            } else if (nodesDeletedbyBsim == false) {

                addPicoBatchHelper.deleteBindWRANBatchNodes(bsimBatchOperator, nodesToDeletebyBSIM, "Successful");
                nodesDeletedbyBsim = true;
            }
        }

        setTestStep("After Class ==> Deleting Batches");
        addPicoBatchHelper.deleteWranBatchesAfterTestExecution(bsimBatchOperator, addedBatches);

    }
}

