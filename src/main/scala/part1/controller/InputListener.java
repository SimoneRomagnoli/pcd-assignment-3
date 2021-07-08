package part1.controller;

import java.io.File;

public interface InputListener {

	void started(File dir, File wordsFile, int limitWords);
	
	void stopped();
}
