package part2.rmi.controller;

import part2.rmi.puzzle.SimpleGUI;
import part2.rmi.remotes.*;

import java.rmi.RemoteException;


public class Controller {

    private final Propagator propagator;
    private final SimpleGUI gui;
    private final Counter localCounter;
    private final HostList hostList;
    private final LocalObserver observer;
    private int id;

    public Controller(int id, Counter counter, HostList hl) {
        this.id = id;
        this.localCounter = counter;
        this.hostList = hl;
        this.gui = new SimpleGUI(this.id, this);
        this.observer = new LocalObserver(this);
        this.propagator = new Propagator(id, this.hostList, this.localCounter);

        try {
            this.localCounter.setPropagator(this.propagator);
            this.localCounter.setLocalObserver(this.observer);

            this.hostList.setPropagator(this.propagator);

            RemoteHost localhost = new RemoteHostImpl(id);
            this.hostList.join(localhost);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getValue() throws RemoteException {
        return this.localCounter.getValue();
    }

    public void inc() throws RemoteException {
        this.localCounter.inc();
    }

    public void update() {
        this.gui.updateVal();
    }

}
