import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.bsim.domain.messages.LRANAndWRANDeleteNodesStatus;
import com.ericsson.oss.bsim.domain.messages.DeleteNodesStatusMessage;
import com.ericsson.oss.bsim.ui.core.BsimServiceManager;
import com.ericsson.oss.bsim.ui.service.iface.IDeleteNodesStatusReceiver;

/**
 * The DeleteNodesExecutor class will the deleting of nodes through BSIM
 *
 * @author ewaywal
 *
 */
public class DeleteNodesExecutor {

	private String success = "";
	
	List<String> boundNodeFdns = new ArrayList<String>();
	
	private int statusReceived = 0;
	
	public int addFdnsForNodesToBeDeleted(String fdn){
		
		boundNodeFdns.add(fdn);
		return boundNodeFdns.size();
	}

	/**
	 * Executes the deleting of nodes through BSIM
	 *
	 * @param rnc
	 * @param nodes
	 */
	public String runDeleteNodes() {

		final CountDownLatch latch = new CountDownLatch(1);

		BsimServiceManager.getInstance().getMessageHandler()
				.addDeleteNodeStatusReceiver(new IDeleteNodesStatusReceiver() {

					@Override
					public void deleteNodeStatusReceived(final DeleteNodesStatusMessage deleteNodesStatusMessage) {
								
						if (deleteNodesStatusMessage.getStatus().equals(LRANAndWRANDeleteNodesStatus.SUCCESSFUL) 
							|| deleteNodesStatusMessage.getStatus().equals(LRANAndWRANDeleteNodesStatus.UNSUCCESSFUL)) {
							
							statusReceived++;
							
							setSuccess(deleteNodesStatusMessage.getStatus().toString());
							
							if(statusReceived == boundNodeFdns.size()){
								BsimServiceManager.getInstance().getMessageHandler().removeDeleteNodeStatusReceiver(this);
							}
							
							latch.countDown();
							}
							else{
								setSuccess(deleteNodesStatusMessage.getStatus().toString());
							}
					}
				});

		if (!boundNodeFdns.isEmpty()) {
			BsimServiceManager.getInstance().getBsimService().deleteNetworkElement(boundNodeFdns);
		}
		try {
			int CountDown = boundNodeFdns.size() * 5;
			boundNodeFdns.removeAll(boundNodeFdns);
			latch.await(CountDown, TimeUnit.MINUTES);
		}
		catch (final InterruptedException e) {
			e.printStackTrace();
		}
		return success;
	}

	/**
	 * @return the success
	 */
	public String getSuccess() {

		return success;
	}

	/**
	 * @param success
	 *            the success to set
	 */			
	public void setSuccess(final String success) {

		this.success = success;
	}
}
