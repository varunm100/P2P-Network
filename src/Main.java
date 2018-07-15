/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.File;
import java.util.Scanner;

public class Main {
    static Scanner scanner;

    public static void main(String args[]) {
        Main.scanner = new Scanner(System.in);

        Peer peer = new Peer();
        peer.startPeer(new File("peer-config.config"));

        while(Peer.Shared.isRunning) peer.parseStringCommand(Main.scanner.nextLine());

        // TODO Add sendToAdjNode           (1) : DONE : WORKS
        // TODO Add sendToNthAdjNode        (2) : DONE : WORKS
        // TODO Add sendToRandomNode        (3) : DONE : WORKS
        // TODO Add startPolling            (4) : DONE : WORKS
    }
}
