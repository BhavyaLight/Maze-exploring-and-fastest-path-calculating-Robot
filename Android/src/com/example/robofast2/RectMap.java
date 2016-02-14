package com.example.robofast2;

import android.graphics.Canvas;
import android.graphics.Color;

public class RectMap {
	
	private static final int LIGHT_GRAY = Color.rgb(0xEC, 0xF0, 0xF1); //ecf0f1
	private static final int GRAY = Color.rgb(0xBD, 0xC3, 0xC7);	//bdc3c7
	private static final int DARK_BLUE = Color.rgb(0x2C, 0x3E, 0x50);
	private static final int MATERIAL_GRAY = Color.rgb(0x6B, 0x7B, 0x83);  //6B7B83
	
	private static final int COLOR_CELL_BACKGROUND = LIGHT_GRAY;
	private static final int COLOR_ARENA = Color.GREEN;
	private static final int COLOR_OBSTACLE = Color.BLACK;
	private static final int ROBOT_FRONT_COLOR = Color.rgb(0xE7, 0x4C, 0x3C);	//e74c3c
	private static final int ROBOT_REAR_COLOR = Color.rgb(0x29,0x80,0xb9);
	
	private int[] gridArray;
	private Rectangle cell;
	
	public RectMap(int[] gridArray, Rectangle cell) {
		this.gridArray = gridArray;
		this.cell = cell;
	}

	public void drawMapString(Canvas canvas) {
		//String[] gString = gridString.split(" ");
		int[] gString = gridArray;
    	int height = gString[0];
    	int width = gString[1];
    	int bodyX = gString[2];
    	int bodyY = gString[3];
    	int headX = gString[4];
    	int headY = gString[5];
    	
    	for (int i=1; i <= width; i++) {
    		for (int j=1; j <= height; j++) {
    			
    			cell.drawCell(canvas,i, j, COLOR_CELL_BACKGROUND);
    		}
    	}
    	
    	for (int i=0; i<3; i++) {
    		for (int j=0; j<3; j++) {
    			cell.drawCell(canvas,bodyX-1+i, bodyY-1+j, ROBOT_REAR_COLOR);
    		}
    	}
    	if (headX>0 && headY>0) {
    		cell.drawCell(canvas,headX, headY, ROBOT_FRONT_COLOR);
    	}
    	
    	for(int arrayPos = 6; arrayPos < gString.length; arrayPos++){
    		int gridPos = arrayPos - 5;
    		if (gridPos > width*height){
    			break;
    		}
    		if (gString[arrayPos]==1){
    			int obstacleX = 0;
    			int obstacleY = 0;
    			if ((gridPos)%width==0){
    				obstacleX = width;
    				obstacleY = gridPos / width;
    			}
    			else{
    				obstacleX = gridPos % width;
    				obstacleY = gridPos / width + 1;
    			}	
    			cell.drawCell(canvas, obstacleX, obstacleY, COLOR_OBSTACLE);
    		}   		
    	}

	}
	
	public void setGridArray(int[] gridArray) {
		this.gridArray = gridArray;
	}

}
