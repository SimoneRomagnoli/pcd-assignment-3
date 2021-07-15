package part2.rmi.remote;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class CounterImpl implements Counter {
	private List<RemoteObservable> observables;
	private int value;
	
	public CounterImpl(int value){
		this.value = value;
		this.observables = new ArrayList<>();
	}
	
	public void inc(){
		value++;

		System.out.println("INC. I will update observables "+this.observables.toString());
		for(RemoteObservable o: this.observables) {
			try {
				o.update();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}
	
	public int getValue(){
		return value;
	}


	public void addObservable(RemoteObservable obs){
		this.observables.add(obs);
	}



}
