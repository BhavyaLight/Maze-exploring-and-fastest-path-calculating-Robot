package com.example.robofast2;

public class ArenaThread extends Thread {
	
	private final static int SLEEP_TIME = 1000;
	
	private boolean running = false;
	private Arena canvas = null;
	
	//constructor, assign the arena to thread
	public ArenaThread(Arena canvas) {
		super();
		this.canvas = canvas;
	}
	
	public void startThread() {
		running = true;
		super.start();
	}
	
	public void stopThread() {
		running = false;
	}
	
	public void run() {
		while (running) {
			try {
				//code to execute
				canvas.updateMap();
				canvas.postInvalidate();
				//delay every cycle
				sleep(SLEEP_TIME);
			} catch (InterruptedException ie) {
				
			}
		}
	}

}

