package part2.rmi;

import part2.rmi.remote.DistributedServer;

public class DistributedPuzzleJoiner2 {

    public static void main(String[] args) {
        final DistributedServer distributedServer = new DistributedServer("192.168.0.102");
    }
}
