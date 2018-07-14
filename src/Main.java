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
        while (Peer.Shared.running) {
            final String command = Main.scanner.nextLine();
            if (command.equals("/exit")) {
                peer.stop();
                Main.scanner.close();
                break;
            } else if (command.startsWith("/sendto;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendObject(new SerializableText(command.split(";")[2], peer.Ipv4Local), command.split(";")[1]));
            } else if (command.startsWith("/sendtoall;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendToAllPeers(new SerializableText(command.split(";")[1], peer.Ipv4Local)));
            } else if (command.startsWith("/sendtoadj;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendToAdjPeers(new SerializableText(command.split(";")[1], peer.Ipv4Local)));
            } else if (command.startsWith("/sendtoallnth;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendToNAdjNode(Integer.parseInt(command.split(";")[1]), command.split(";")[2]));
            } else if (command.startsWith("/sendToRandomNode;")) {
                Peer.Shared.threadManager.submit(() -> peer.sendToRandomNode(Integer.parseInt(command.split(";")[1]), Integer.parseInt(command.split(";")[2]), command.split(";")[3]));
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
        // TODO Add sendToNthAdjNode        (1) : DONE : WORKS
        // TODO Add sendToRandomNode        (2) : DONE : WORKS
        // TODO Add startPolling            (3) : TO BE DONE : SHOULD BE EASY
    }
}
