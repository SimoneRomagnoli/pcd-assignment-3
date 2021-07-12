package part1.controller;

import java.io.File;

/**
 * Interface for listening gui events.
 *
 */
public interface InputListener {

	void started(File dir, File wordsFile, int limitWords);
	
	void stopped();
}
