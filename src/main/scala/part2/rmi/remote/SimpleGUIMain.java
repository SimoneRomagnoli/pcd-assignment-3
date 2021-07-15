package part2.rmi.remote;


import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SimpleGUIMain {

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);

            HostList hl = new HostListImpl();
            HostList hlStub = (HostList) UnicastRemoteObject.exportObject(hl,0);

            Counter count = new CounterImpl(0);
            Counter countStub = (Counter) UnicastRemoteObject.exportObject(count, 0);
            // Bind the remote object's stub in the registry

            Registry registry = LocateRegistry.getRegistry(1099);
            registry.rebind("countObj", countStub);
            registry.rebind("hostlist", hlStub);

            HostList remoteHL = (HostList)registry.lookup("hostlist");
            RemoteHost localhost = new RemoteHostImpl();
            remoteHL.join(localhost);

            System.out.println("Objects registered.");
            RemoteObservable ro = new RemoteObservableImpl();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }


    }
}
