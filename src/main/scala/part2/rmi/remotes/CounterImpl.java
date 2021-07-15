package part2.rmi.remotes;

import part2.rmi.controller.Propagator;

import java.rmi.RemoteException;

public class CounterImpl implements Counter {
	private Propagator propagator;
	private LocalObserver observer;
	private int value;
	
	public CounterImpl(int value){
		this.value = value;
	}
	
	public void inc(){
		value++;
		this.propagator.propagate();
		this.observer.update();
	}
	
	public int getValue(){
		return value;
	}

	@Override
	public void setPropagator(Propagator propagator) throws RemoteException {
		this.propagator = propagator;
	}

	@Override
	public void setLocalObserver(LocalObserver localObserver) throws RemoteException {
		this.observer = localObserver;
	}

	@Override
	public void remoteUpdate(int remoteValue) throws RemoteException {
		this.value = remoteValue;
		this.observer.update();
	}


}
