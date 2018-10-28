//Code based on the examples.bookTrading package in http://jade.tilab.com/download/jade/

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
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.List;
import java.util.Vector;
import java.util.Random;


@SuppressWarnings("serial")
public class BookBuyerAgent extends Agent {
	// catalogue of books from which the Buyer selects targetBooks
	private List<String> catalogueBooks;
	// Books to buy
	private List<String> targetBooks;
	private int nTargetBooks = 3;
	// Books bought
	private List<String> acquiredBooks;
	// The list of known seller agents
	private AID [] sellerAgents;

	protected void setup() {
		// Welcome message
		System.out.println(getAID().getLocalName()+" is ready.");

		initializeCatalogueBooks();
		initializeTargetBooks();
		acquiredBooks = new Vector<>();

		System.out.println(getAID().getName()+ " will try to buy  "+targetBooks);

		// Register the book-buying service in the yellow pages
		registerBookBuyer();

		// Add a TickerBehaviour for each targetBook
		for (String targetBook : targetBooks) {
			addBehaviour(new TickerBehaviour(this, 5000) {
				protected void onTick() {
					System.out.println(getAID().getLocalName()+"is trying to buy "+targetBook);

					// Update seller agents
					getBookSellers(myAgent);

					if(acquiredBooks.contains(targetBook)){
						System.out.println(getAID().getLocalName()+" has already bought" + targetBook);
						printAcquiredBooks();
						// Check the number of books bought so far
						checkNBoughtBooks();
						// Stop the TickerBehaviour that is trying to buy targetBook
						stop();
					}
					else{
						// Perform the request
						myAgent.addBehaviour(new RequestPerformer(targetBook));

					}
				}

			} );

		}

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
	}

	public void checkNBoughtBooks(){
		if(acquiredBooks.size() == nTargetBooks){
			System.out.println(getAID().getLocalName()+" has bought " + acquiredBooks.size() + " books");
			// Stop this agent
			doDelete();
		}
		else{
			System.out.println(getAID().getLocalName()+" has not bought " + nTargetBooks + " yet");
		}
	}

	protected void takeDown() {
        // Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	public void registerBookBuyer(){
		// Register the book-buying service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-buying");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	public void getBookSellers(Agent myAgent){
		// Update the list of seller agents
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		template.addServices(sd);
		try {
			DFAgentDescription [] result = DFService.search(myAgent, template);
			System.out.println("Found the following seller agents:");
			sellerAgents = new AID [result.length];
			for (int i = 0; i < result.length; ++i) {
				sellerAgents[i] = result[i].getName();
				System.out.println(sellerAgents[i].getName());
			}
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

	}

	public void printAcquiredBooks(){
		System.out.println("Agent"+ getAID().getLocalName()+" bought:");
		System.out.println(acquiredBooks);
	}

	public void initializeCatalogueBooks(){
		catalogueBooks = new Vector<>();
		catalogueBooks.add("Frankenstein");
		catalogueBooks.add("Dracula");
		catalogueBooks.add("A Storm of Swords");
		catalogueBooks.add("A Feast of Crows");
	}

	protected void initializeTargetBooks(){
		targetBooks = new Vector<>();
		Random rand = new Random();
		// Get a random index of the catalogueBooks until the target books has nTargetBooks
		while(targetBooks.size()< nTargetBooks){
			int randomIndex = rand.nextInt(catalogueBooks.size());
			boolean titleInTargetBooks = targetBooks.contains(catalogueBooks.get(randomIndex));
			if (!titleInTargetBooks)
				targetBooks.add(catalogueBooks.get(randomIndex));
		}

	}

	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller
	   agents the target book.
	 */
	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // The agent who provides the best offer
		private int bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;
		private String targetBook;

		public RequestPerformer(String targetBook){
			this.targetBook = targetBook;
		}

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				}
				cfp.setContent(targetBook);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer
						int price = Integer.parseInt(reply.getContent());
						if (bestSeller == null || price < bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						// We received all replies
						step = 2;
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(targetBook);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println("Agent "+getAID().getLocalName()+ " successfully purchased "+ targetBook+ " from agent "+reply.getSender().getLocalName());
						System.out.println("Bought at price = "+bestPrice);
						acquiredBooks.add(targetBook);
						//myAgent.doDelete();
					}
					else {
						System.out.println("Attempt failed: requested book already sold.");
					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}
		}

		public boolean done() {
			if (step == 2 && bestSeller == null) {
				System.out.println("Attempt failed: "+targetBook+" not available for sale");
			}
			return ((step == 2 && bestSeller == null) || step == 4);
		}
	}  // End of inner class RequestPerformer

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
