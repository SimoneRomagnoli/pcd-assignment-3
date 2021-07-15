package part2.rmi;


import part2.rmi.controller.Controller;
import part2.rmi.remotes.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Starter {

    public static final int REGISTRY_PORT = 1099;

    public static void main(String[] args) {
        try {
            //CREATE LOCAL REGISTRY
            LocateRegistry.createRegistry(REGISTRY_PORT);

            //CREATE A HOST LIST
            HostList hl = new HostListImpl();
            HostList hlStub = (HostList) UnicastRemoteObject.exportObject(hl,0);

            //CREATE THE MODEL
            Counter count = new CounterImpl(0);
            Counter countStub = (Counter) UnicastRemoteObject.exportObject(count, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);
            registry.rebind("countObj", countStub);
            registry.rebind("hostlist", hlStub);

            System.out.println("Objects registered.");
            Controller controller = new Controller(0, count, hl);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }
}
