package part2.rmi.remote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;

public class DistributedServer {

    public static final String HOSTLIST = "hostlist";

    public DistributedServer(RemoteHostImpl localhost) {
        try {
            HostList hostList = new HostListImpl();
            hostList.join(localhost);
            HostList hostListStub = (HostList) UnicastRemoteObject.exportObject(hostList, 0);

            // Bind the remote object's stub in the registry
            LocateRegistry.getRegistry().rebind(HOSTLIST, hostListStub);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        HostList l = (HostList) LocateRegistry.getRegistry().lookup(HOSTLIST);
                        System.out.println("My hostlist is: "+l.getHostList().toString());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }
            },0,5000);

            System.out.println("I created a new hostlist");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public DistributedServer(String remotehost) {
        try {
            System.out.println("Reaching out to "+remotehost);
            HostList hostList = (HostList) LocateRegistry.getRegistry(remotehost, 0).lookup(HOSTLIST);
            System.out.println("I received a hostlist: "+hostList.getHostList().toString());
            RemoteHostImpl localhost = new RemoteHostImpl(hostList.getHostList().size());

            hostList.join(localhost);

            HostList newHostList = new HostListImpl(hostList);
            HostList hostListStub = (HostList) UnicastRemoteObject.exportObject(newHostList, 0);

            // Bind the remote object's stub in the registry
            LocateRegistry.getRegistry().rebind(HOSTLIST, hostListStub);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        HostList l = (HostList) LocateRegistry.getRegistry().lookup(HOSTLIST);
                        System.out.println("My hostlist is: "+l.getHostList().toString());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }
            },0,5000);


        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

}
