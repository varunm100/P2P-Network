import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    static Scanner scanner;

    private PeerData parseConfigFile(File file) {
        Map<String, Integer> adjPeers = new HashMap<>();
        String Ipv4 = null;
        int serverPort = -1;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while((line = br.readLine())!=null) {
                if (line.startsWith("adjPeer:")) {
                    adjPeers.put(line.split(":")[1], Integer.valueOf(line.split(":")[2]));
                } else if (line.startsWith("server:")) {
                    Ipv4 = line.split(":")[1];
                    serverPort = Integer.valueOf(line.split(":")[2]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not find config file");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error while parsing config file");
            e.printStackTrace();
        }
        if (adjPeers.isEmpty() || Ipv4 == null || serverPort == -1)
            System.exit(0);
        return new PeerData(adjPeers, Ipv4, serverPort);
    }

    public static void main(String args[]) {
        Main o = new Main();
        scanner = new Scanner(System.in);

        Peer peer = new Peer();
        peer.startPeer(o.parseConfigFile(new File("peer-config.config")));

        String command;
        while(true) {
            command = Main.scanner.nextLine();
            if (command.contains("/exit")) {
                peer.cleanUp();
                Main.scanner.close();
                break;
            } else if (command.contains("/sendto;")) {
                Peer.sendObject(new SerializableText(command.split(";")[2], Peer.Ipv4Local), command.split(";")[1]);
            } else if (command.contains("/sendtoall;")) {
                peer.sendToAllPeers(new SerializableText(command.split(";")[1], Peer.Ipv4Local));
            } else {
                System.out.println("'" + command + "' is not recognized as a command.");
            }
        }
    }
}
