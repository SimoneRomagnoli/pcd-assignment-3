package part2.rmi;

import part2.rmi.remote.DistributedServer;
import part2.rmi.remote.RemoteHostImpl;

public class DistributedPuzzle {

    public static void main(String[] args) {
        if(args.length < 1) {
            final RemoteHostImpl localhost = new RemoteHostImpl(0);
            final DistributedServer distributedServer = new DistributedServer(localhost);
            //final Client client = new Client();
        } else {
            final DistributedServer distributedServer = new DistributedServer("127.0.0.1");
        }
    }
}
