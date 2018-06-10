import java.io.File;
import java.util.Scanner;

public class Main {
    static Scanner scanner;

    /**
     * Handles inputted commands from user.
     * @param _peer Peer Object
     */
    private void handleCommandInput(Peer _peer) {
        String command;
        while(Peer.Shared.running) {
            command = Main.scanner.nextLine();
            if (command.equals("/exit")) {
                Main.scanner.close();
                _peer.stop();
            } else if (command.startsWith("/sendto;")) {
                Peer.sendObject(new SerializableText(command.split(";")[2], Peer.Ipv4Local), command.split(";")[1]);
            } else if (command.startsWith("/sendtoall;")) {
                _peer.sendToAllPeers(new SerializableText(command.split(";")[1], Peer.Ipv4Local));
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
