package com.example.robofast2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class Arena extends View {
	private static final int COLOR_CELL = Color.GRAY;
	private static final int SIZE = 35;
	
	private RectMap map = null;
	private int[] gridArray;
	private ArenaThread thread;
	private GestureDetector gestureDetector;
	
	public Arena(Context context) {
		super(context);
		gestureDetector = new GestureDetector(context, new GestureListener());
		map = new RectMap(gridArray, new Rectangle());
		
		thread = new ArenaThread(this);
		thread.startThread();
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		map.drawMapString(canvas);	
	}
	
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {

	    @Override
	    public boolean onDown(MotionEvent e) {
	        return true;
	    }
	    
	    
	    // event when double tap occurs
	    /*@Override
	    public boolean onDoubleTap(MotionEvent e) {
	        float x = e.getX();
	        float y = e.getY();
	        Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");
	        return true;
	    }*/
	}

	public int[] getGridArray() {
		return gridArray;
	}

	public void setGridArray(int[] gridArray) {
		this.gridArray = gridArray;
	}

	public void updateMap() {
		/*if (gridArray[2] < 2) { gridArray[2] = 2; }
		if (gridArray[3] < 2) { gridArray[3] = 2; }
		if (gridArray[2] > 14) { gridArray[2] = 14; }
		if (gridArray[3] < 19) { gridArray[3] = 19; }	
		*/
		map.setGridArray(gridArray);
	}
	
	public void turnRight() {
		//horizontal
		if (gridArray[5]==gridArray[3]) {
			if (gridArray[4] < gridArray[2]) {
				gridArray[5] -= 2;
				gridArray[4] += 2;
			} else {
				gridArray[5] += 2;
				gridArray[4] -= 2;
			}
		// vertical
		} else {
			if (gridArray[5] < gridArray[3]) {
				gridArray[5] += 2;
				gridArray[4] += 2;
			} else {
				gridArray[5] -= 2;
				gridArray[4] -= 2;
			}
		}
	}
	
	public void turnLeft() {
		// horizontal
		if (gridArray[5]==gridArray[3]) {
			if (gridArray[4] < gridArray[2]) {
				gridArray[5] += 2;
				gridArray[4] += 2;
			} else {
				gridArray[5] -= 2;
				gridArray[4] -= 2;
			}
		// vertical
		} else {
			if (gridArray[5] > gridArray[3]) {
				gridArray[5] -= 2;
				gridArray[4] += 2;
			} else {
				gridArray[5] += 2;
				gridArray[4] -= 2;
			}
		}
	}

	public void moveBackward() {
		//to be implemented
		/*if (gridArray[5]==gridArray[3]) {
			gridArray[2] = (gridArray[2] + gridArray[4]) / 2;
			gridArray[4] = (2 * gridArray[4]) - gridArray[2];
		} else {
			gridArray[3] = (gridArray[3] + gridArray[5]) / 2;
			gridArray[5] = (2 * gridArray[5]) - gridArray[3];
		}*/
	}

	public void moveForward() {
		//horizontal
		if (gridArray[5]==gridArray[3]) {
			gridArray[2] = (gridArray[2] + gridArray[4]) / 2;
			gridArray[4] = (2 * gridArray[4]) - gridArray[2];
		} else {
			gridArray[3] = (gridArray[3] + gridArray[5]) / 2;
			gridArray[5] = (2 * gridArray[5]) - gridArray[3];
		}
	}
	
	public void startPosition(int x, int y) {
		if (x < 2) {x = 2;
		} else if (x > 19) {x = 19;}
		if (y < 2) {y = 2;
		} else if (y > 14) {y = 14;}
		int offsetX = x - gridArray[2];
		int offsetY = y - gridArray[3];
		gridArray[2] = x;
		gridArray[3] = y;
		gridArray[4] += offsetX;
		gridArray[5] += offsetY;
	}
	
	public int getSize() {
		return SIZE;
	}

	public void startDirection(int x, int y) {

		if (y > gridArray[3] - 1 && y < gridArray[3] + 1) {
			if (x > gridArray[2]) {
				gridArray[5] = gridArray[3];
				gridArray[4] = gridArray[2] + 2;
			} else {
				gridArray[5] = gridArray[3];
				gridArray[4] = gridArray[2] - 2;
			}
		} else {
			if (y > gridArray[3]) {
				gridArray[4] = gridArray[2];
				gridArray[5] = gridArray[3] + 2;
			} else {
				gridArray[4] = gridArray[2];
				gridArray[5] = gridArray[3] - 2;
			}
		}
	}
	
	public void swapDirection() {
		int temp = gridArray[4];
		gridArray[4] = gridArray[5];
		gridArray[5] = temp;
	}

	public boolean setDirection(int x, int y) {
		if (y > gridArray[3] - 1 && y < gridArray[3] + 1) {
			if (x > gridArray[2] + 2) {
				gridArray[5] = gridArray[3];
				gridArray[4] = gridArray[2] + 2;
				return true;
			} else if ( x < gridArray[2] -2 ) {
				gridArray[5] = gridArray[3];
				gridArray[4] = gridArray[2] - 2;
				return true;
			}
		} else {
			if (y > gridArray[3] + 2) {
				gridArray[4] = gridArray[2];
				gridArray[5] = gridArray[3] + 2;
				return true;
			} else if (y < gridArray[3] - 2) {
				gridArray[4] = gridArray[2];
				gridArray[5] = gridArray[3] - 2;
				return true;
			}
		}
		return false;
	}

}
