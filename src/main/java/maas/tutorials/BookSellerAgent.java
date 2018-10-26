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
import java.util.*;

public class BookSellerAgent extends Agent {
    // The catalogue of ebooks for sale (maps the title of a book to its price)
    private Hashtable catalogue_ebooks;
    // The catalogue of paperback for sale (maps the title of a book to its price)
    private Hashtable catalogue_paperbacks;
    // The inventory of papaerbacks (maps the title of a paperback to its availability)
    private Hashtable inventory_paperbacks;

    // Agent initialization
    protected void setup() {

		System.out.println("Seller-agent "+getAID().getName()+" is ready.");

        initializeCatalogue();

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
		addBehaviour(new shutdown());
    }

    protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

    protected void initializeCatalogue(){
        // Create the catalogue_ebooks
        catalogue_ebooks = new Hashtable();
        // Create the catalogue_paperbacks
        catalogue_paperbacks = new Hashtable();
        // Create the inventory of papaerbacks
        inventory_paperbacks = new Hashtable();

        catalogue_ebooks.put("Frankenstein", 3.5);
        catalogue_ebooks.put("Dracula", 4.3);
        catalogue_ebooks.put("Guilver's travels", 3.8);
        catalogue_ebooks.put("Robinson Crusoe", 4.2);

        catalogue_paperbacks.put("A Game of Thrones", 8.6);
        catalogue_paperbacks.put("A Clash of Kings", 8.6);
        catalogue_paperbacks.put("A Storm of Swords", 8.6);
        catalogue_paperbacks.put("A Feast of Crows", 8.6);

        inventory_paperbacks.put("A Game of Thrones", 20);
        inventory_paperbacks.put("A Clash of Kings", 20);
        inventory_paperbacks.put("A Storm of Swords", 20);
        inventory_paperbacks.put("A Feast of Crows", 20);

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
