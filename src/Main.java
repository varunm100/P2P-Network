import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    static Scanner scanner;
    public static void main(String args[]) {
        scanner = new Scanner(System.in);

        Map<String, Integer> adjPeers = new HashMap<>();
        adjPeers.put("192.168.1.187",5000);
        Peer peer = new Peer();
        peer.startPeer(adjPeers, 5000, "192.168.1.67");

        String command;
        while(true) {
            command = Main.scanner.nextLine();
            if (command.contains("/exit")) {
                peer.cleanUp();
                Main.scanner.close();
                break;
            } else if (command.contains("/sendto;")) {
                peer.sendObject(new SerializableText(command.split(";")[2], peer.Ipv4Local), command.split(";")[1]);
            } else if (command.contains("/sendtoall;")) {
                //peer.SendDataToAllNodes(0, command.split(";")[1]);○
            } else {
                System.out.println("'" + command + "' is not recognized as a command.");
            }
        }
    }
}
