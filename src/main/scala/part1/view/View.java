package part1.view;

import part1.controller.InputListener;

import java.util.Map;

/**
 * Class representing the view part of the application.
 *
 */
public class View {

	private ViewFrame frame;

	public View(){
		frame = new ViewFrame();
	}
	
	public void addListener(InputListener l){
		frame.addListener(l);
	}

	public void display() {
        javax.swing.SwingUtilities.invokeLater(() -> {
        	frame.setVisible(true);
        });
    }
	
	public void update(final int words, final Map<String, Integer> occ) {
		frame.update(words, occ);
	}
	
	public void done() {
		frame.done();
	}

}
	
