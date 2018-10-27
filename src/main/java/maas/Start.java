package maas;

import java.util.List;
import java.util.Vector;
import maas.tutorials.BookBuyerAgent;
import maas.tutorials.BookSellerAgent;

public class Start {
    public static void main(String[] args) {
        int nBookBuyers = 20;
        int nBookSellers = 3;

    	List<String> agents = new Vector<>();

        for (int i=0; i< nBookBuyers; i++){
            StringBuilder sb = new StringBuilder();
            sb.append("Buyer_");
            sb.append(Integer.toString(i));
            sb.append(":maas.tutorials.BookBuyerAgent");
            agents.add(sb.toString());
        }

        for (int i=0; i< nBookSellers; i++){
            StringBuilder sb = new StringBuilder();
            sb.append("Seller_");
            sb.append(Integer.toString(i));
            sb.append(":maas.tutorials.BookSellerAgent");
            agents.add(sb.toString());
        }

    	List<String> cmd = new Vector<>();
    	cmd.add("-agents");
    	StringBuilder sb = new StringBuilder();
    	for (String a : agents) {
    		sb.append(a);
    		sb.append(";");
    	}
    	cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }
}
