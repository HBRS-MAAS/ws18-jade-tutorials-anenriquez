package maas.tutorials;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import java.util.List;
import java.util.Vector;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent {
	// Books to buy
	private List<String> targetBooks;
	// Books bought
	private List<String> acquiredBooks;

	protected void setup() {
		// Welcome message
		System.out.println("Buyer-agent "+getAID().getName()+" is ready.");

		initializeTargetBooks();
		acquiredBooks = new Vector<>();

		// Title of the book to buy
		//targetBookTitle = "Guliver's travels";

		// if(targetBookTitle != null){
		// 	System.out.println("Trying to buy "+targetBookTitle);
		// 	// TickerBehaviour that schedules a request to the seller agent every minute
		// 	addBehaviour(new TickerBehaviour (this, 60000) {
		// 		protected void onTick(){
		// 			myAgent.addBehaviour(new RequestPerformer());
		// 		}
		// 	});
		// }
		// else {
		// 	// Make the agent terminate
		// 	System.out.println("No target book title specified");
		// 	addBehaviour(new shutdown());
		//
		// }



        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
		addBehaviour(new shutdown());
	}
	protected void initializeTargetBooks(){
		targetBooks = new Vector<>();
		targetBooks.add("Frankenstein");
		targetBooks.add("Dracula");
        targetBooks.add("Guilver's travels");
	}

	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
			    myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
			    myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
			    //LOGGER.error(e);
			}

		}
	}
}
