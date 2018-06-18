/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    static Scanner scanner;

    /**
     * Useful for debugging purposes.
     *
     * @param peer Peer Object.
     */
    private void handleCommandInput(Peer peer) {
        String command;
        while (Peer.Shared.running) {
            command = Main.scanner.nextLine();
            if (command.equals("/exit")) {
                System.out.println(Arrays.toString(Peer.Shared.callBackCounter.get().keySet().toArray()));
                peer.stop();
                Main.scanner.close();
            } else if (command.startsWith("/sendto;")) {
                Peer.sendObject(new SerializableText(command.split(";")[2], Peer.Ipv4Local), command.split(";")[1]);
            } else if (command.startsWith("/sendtoall;")) {
                peer.sendToAllPeers(new SerializableText(command.split(";")[1], Peer.Ipv4Local));
            } else if (command.startsWith("/sendtoadj;")) {
                peer.sendToAdjPeers(new SerializableText(command.split(";")[1], Peer.Ipv4Local));
            } else if (command.startsWith("/")) {
                System.out.println("'" + command + "' is not recognized as a valid command.");
            }
        }
    }

    public static void main(String args[]) {
        Main o = new Main();
        Peer peer = new Peer();
        peer.startPeer(new File("peer-config.config"));
        o.handleCommandInput(peer);
    }
}
