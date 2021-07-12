package part2.rmi;

public class DistributedPuzzle {

    public static void main(String[] args) {
        if(args.length < 1) {
            final Host localhost = new Host();
            final DistributedServer distributedServer = new DistributedServer(localhost);
        } else {
            final DistributedServer distributedServer = new DistributedServer("127.0.0.1");
        }
    }
}
