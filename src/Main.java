/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.File;
import java.util.Scanner;

public class Main {
    static Scanner scanner;

    /**
     * Used for debugging purposes.
     *
     * @param peer Peer Object.
     */
    private static void handleCommandInput(Peer peer) {
        String command;
        while (Peer.Shared.running) {
            command = Main.scanner.nextLine();
            if (command.equals("/exit")) {
                peer.stop();
                Main.scanner.close();
                break;
            } else if (command.startsWith("/sendto;")) {
                peer.sendObject(new SerializableText(command.split(";")[2], peer.Ipv4Local), command.split(";")[1]);
            } else if (command.startsWith("/sendtoall;")) {
                peer.sendToAllPeers(new SerializableText(command.split(";")[1], peer.Ipv4Local));
            } else if (command.startsWith("/sendtoadj;")) {
                peer.sendToAdjPeers(new SerializableText(command.split(";")[1], peer.Ipv4Local));
            } else if (command.startsWith("/sendtoallnth;")) {
                peer.sendToNAdjNode(Integer.parseInt(command.split(";")[1]), command.split(";")[2]);
            } else if (command.startsWith("/")) {
                System.out.println("'" + command + "' is not recognized as a valid command.");
            }
        }
    }

    public static void main(String args[]) {
        Main.scanner = new Scanner(System.in);

        Peer peer = new Peer();
        peer.startPeer(new File("peer-config.config"));

        handleCommandInput(peer);
        // TODO Add sendToNthAdjNode        (1) : DONE : STILL HAVE TO DEBUG
        // TODO Add sendToNode (Universal)  (2) : MIGHT NOT NEED
        // TODO Add getRandomNode           (3) : Part OF Task 1
        // TODO Add startPolling            (4)
    }
}
