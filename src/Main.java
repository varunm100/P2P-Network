/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.File;
import java.time.Clock;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    static Scanner scanner;

    /**
     * Used for debugging purposes.
     *
     * @param peer Peer Object.
     */
    private void handleCommandInput(Peer peer) {
        String command;
        while (Peer.Shared.running) {
            command = Main.scanner.nextLine();
            String finalCommand = command;
            if (command.equals("/exit")) {
                peer.stop();
                Main.scanner.close();
                break;
            } else if (command.startsWith("/sendto;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendObject(new SerializableText(finalCommand.split(";")[2], peer.Ipv4Local), finalCommand.split(";")[1]));
            } else if (command.startsWith("/sendtoall;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendToAllPeers(new SerializableText(finalCommand.split(";")[1], peer.Ipv4Local), LocalTime.now(Clock.systemUTC())));
            } else if (command.startsWith("/sendtoadj;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendToAdjPeers(new SerializableText(finalCommand.split(";")[1], peer.Ipv4Local)));
            } else if (command.startsWith("/startpolling")) {
                Map<String, Boolean> pollResults = new HashMap<>(peer.startPollingMessage());
                System.out.println("FINAL POLL RESULTS");
                pollResults.forEach((key,value) -> System.out.println(key + " : " + value));
                System.out.println("__________________");
            } else if (command.startsWith("/")) {
                System.out.println("'" + command + "' is not recognized as a valid command.");
            }
        }
    }

    public static void main(String args[]) {
        Main o = new Main();
        Main.scanner = new Scanner(System.in);

        Peer peer = new Peer();
        peer.startPeer(new File("peer-config.config"));

        o.handleCommandInput(peer);

        /* TODO Add polling system : DONE WITH CODE : STILL HAVE TO DEBUG */
        /* TODO Get list of active peers. */
        /* TODO Pick random peer in the network. */
    }
}
