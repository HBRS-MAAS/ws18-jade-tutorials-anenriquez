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
import java.util.*;

public class BookSellerAgent extends Agent {
    // The catalogue of ebooks for sale (maps the title of a book to its price)
    private Hashtable catalogueEbooks;
    // The catalogue of paperback for sale (maps the title of a book to its price)
    private Hashtable cataloguePaperbacks;
    // The inventory of papaerbacks (maps the title of a paperback to its availability)
    private Hashtable inventoryPaperbacks;

    // Agent initialization
    protected void setup() {

		System.out.println("Seller-agent "+getAID().getName()+" is ready.");

        initializeCatalogue();

        // Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("book-selling");
		sd.setName("JADE-book-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

        // Add the behaviour serving queries from buyer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
		//addBehaviour(new shutdown());
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

    protected void initializeCatalogue(){
        // Create the catalogue_ebooks
        catalogueEbooks = new Hashtable();
        // Create the catalogue_paperbacks
        cataloguePaperbacks = new Hashtable();
        // Create the inventory of papaerbacks
        inventoryPaperbacks = new Hashtable();

        catalogueEbooks.put("Frankenstein", 3);
        catalogueEbooks.put("Dracula", 4);
        catalogueEbooks.put("Guilver's Travels", 3);
        catalogueEbooks.put("Robinson Crusoe", 4);

        cataloguePaperbacks.put("A Game of Thrones", 8);
        cataloguePaperbacks.put("A Clash of Kings", 8);
        cataloguePaperbacks.put("A Storm of Swords", 8);
        cataloguePaperbacks.put("A Feast of Crows", 8);

        inventoryPaperbacks.put("A Game of Thrones", 20);
        inventoryPaperbacks.put("A Clash of Kings", 20);
        inventoryPaperbacks.put("A Storm of Swords", 20);
        inventoryPaperbacks.put("A Feast of Crows", 20);

    }
    /**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by Book-seller agents to serve incoming requests
	   for offer from buyer agents.
	   If the requested book is in the local catalogue and there are enough copies, the seller agent replies
	   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	   sent back.
	 */
	private class OfferRequestsServer extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

                // Check if title exists in catalogueEbooks
                boolean titleInCatalogueEbooks = catalogueEbooks.containsKey(title);

                // Check if title exists in cataloguePaperbacks
                boolean titleInCataloguePaperbacks = cataloguePaperbacks.containsKey(title);

                Integer price = (Integer) 0;
                Integer quantity = (Integer) 0;

                if  (titleInCatalogueEbooks){
                    price = (Integer) catalogueEbooks.get(title);
                }
                else if (titleInCataloguePaperbacks){
                    price = (Integer) cataloguePaperbacks.get(title);
                    quantity = (Integer) inventoryPaperbacks.get(title);
                }

				if (titleInCatalogueEbooks) {
					// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}

                else if (titleInCataloguePaperbacks && quantity > 0) {
					// The requested book is available for sale. Reply with the price
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

    /**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Book-seller agents to serve incoming
	   offer acceptances (i.e. purchase orders) from buyer agents.
       If the book is in catalogueEbooks, the seller does not remove it from the catalogue because there are unlimited copies
       If the book is in cataloguePaperbacks, the seller decreases the quantity in inventory

       The seller replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				ACLMessage reply = msg.createReply();

                // Check if title exists in catalogueEbooks
                boolean titleInCatalogueEbooks = catalogueEbooks.containsKey(title);
                // Check if title exists in cataloguePaperbacks
                boolean titleInCataloguePaperbacks = cataloguePaperbacks.containsKey(title);

                if (titleInCataloguePaperbacks){
                    Integer quantity = (Integer) inventoryPaperbacks.get(title);

                    if (quantity > 0){
                        // Sell the book
                        reply.setPerformative(ACLMessage.INFORM);
    					System.out.println(title+" sold to agent "+msg.getSender().getName());
                        // Decrease the quantity in inventory
                        quantity = (Integer) inventoryPaperbacks.get(title);
                        inventoryPaperbacks.put(title, quantity-1);
                    }
                    else{
                        // The requested book has been sold to another buyer in the meanwhile .
    					reply.setPerformative(ACLMessage.FAILURE);
    					reply.setContent("not-available");
                    }
                }
                else if (titleInCatalogueEbooks){
                    // Sell the book
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(title+" sold to agent "+msg.getSender().getName());
                }

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer



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
