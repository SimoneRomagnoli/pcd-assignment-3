package part2.rmi.remote;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SimpleGUIMain2 {

    public static void main(String[] args) {
        try {
            //GET REMOTE REGISTRY
            Registry registry = LocateRegistry.getRegistry(1099);

            //WATCH HOST LIST ON FIRST HOST
            HostList remoteHostList = (HostList)registry.lookup("hostlist");
            final int id = remoteHostList.getHostList().size();

            //BUILD OWN HOST
            RemoteHost localhost = new RemoteHostImpl(id);

            //UPDATE OTHER HOSTS, JOINING THEIR LOCAL HOST LIST
            for(RemoteHost host: remoteHostList.getHostList()) {
                Registry r = LocateRegistry.getRegistry(1099+host.getId());
                HostList rhl = (HostList)r.lookup("hostlist");
                rhl.join(localhost);
            }

            //CREATE LOCAL REGISTRY
            LocateRegistry.createRegistry(1099+id);

            //CREATE OWN HOST LIST
            HostList localHostList = new HostListImpl(remoteHostList);
            HostList localHostListStub = (HostList) UnicastRemoteObject.exportObject(localHostList, 0);
            LocateRegistry.getRegistry(1099+id).rebind("hostlist", localHostListStub);

            //CREATE OWN COUNTER
            Counter remoteCounter = (Counter) LocateRegistry.getRegistry(1099).lookup("countObj");
            Counter localCounter = new CounterImpl(remoteCounter.getValue());
            Counter localCounterStub = (Counter) UnicastRemoteObject.exportObject(localCounter, 0);
            LocateRegistry.getRegistry(1099+id).rebind("countObj", localCounterStub);

            Controller controller = new Controller(id, localCounter, localHostList);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
