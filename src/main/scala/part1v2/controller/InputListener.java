package part1v2.controller;

import java.io.File;

public interface InputListener {

	void started(File dir, File wordsFile, int limitWords);
	
	void stopped();
}
