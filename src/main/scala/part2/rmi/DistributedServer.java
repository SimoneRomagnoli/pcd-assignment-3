package part2.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class DistributedServer {

    public static final String HOSTLIST = "hostlist";

    public DistributedServer(Host localhost) {
        try {
            HostList hostList = new HostListImpl();
            hostList.join(localhost);
            HostList hostListStub = (HostList) UnicastRemoteObject.exportObject(hostList, 0);

            // Bind the remote object's stub in the registry
            LocateRegistry.getRegistry().rebind(HOSTLIST, hostListStub);

            System.out.println("I created a new hostlist");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public DistributedServer(String remotehost) {
        try {
            HostList hostList = (HostList) LocateRegistry.getRegistry(remotehost).lookup(HOSTLIST);
            Host localhost = new Host(hostList.getHostList().size());
            hostList.join(localhost);

            HostList newHostList = new HostListImpl(hostList);
            HostList hostListStub = (HostList) UnicastRemoteObject.exportObject(newHostList, 0);

            // Bind the remote object's stub in the registry
            LocateRegistry.getRegistry().rebind(HOSTLIST, hostListStub);

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

}
