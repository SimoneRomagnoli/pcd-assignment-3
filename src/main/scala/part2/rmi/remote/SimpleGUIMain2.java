package part2.rmi.remote;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SimpleGUIMain2 {

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            HostList remoteHL = (HostList)registry.lookup("hostlist");
            final int id = remoteHL.getHostList().size();
            RemoteHost localhost = new RemoteHostImpl(id);
            for(RemoteHost host: remoteHL.getHostList()) {
                Registry r = LocateRegistry.getRegistry(1099+host.getId());
                HostList rhl = (HostList)r.lookup("hostlist");
                rhl.join(localhost);
            }

            LocateRegistry.createRegistry(1099+id);

            HostList localHostList = new HostListImpl(remoteHL);
            HostList lhlStub = (HostList) UnicastRemoteObject.exportObject(localHostList, 0);
            LocateRegistry.getRegistry(1099+id).rebind("hostlist", lhlStub);

            RemoteObservable ro = new RemoteObservableImpl(id);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
