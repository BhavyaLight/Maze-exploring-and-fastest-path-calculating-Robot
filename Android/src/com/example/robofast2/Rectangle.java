package com.example.robofast2;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Rectangle {
	//constant define
	private static final int SIZE = 35;
	private static final int CORNER = 5;
		
	private int x;
	private int y;
	private Paint paint = null;
	
	public Rectangle() {
		super();
		paint = new Paint();
		
		paint.setAntiAlias(true);
		paint.setDither(true);
	}
	
	//draw cell with canvas, position, color
	public void drawCell(Canvas canvas, int i, int j, int color) {
		x = SIZE*i;
		y = SIZE*j;
		paint.setColor(color);
		canvas.drawRoundRect(new RectF(x-SIZE/2, y-SIZE/2, x+SIZE/2, y+SIZE/2), CORNER, CORNER, paint);
	}
	
	public void drawCell2(Canvas canvas, int color) {
		setCenterTo(this.x, this.y);
		paint.setColor(color);
		canvas.drawRoundRect(new RectF(x-SIZE/2, y-SIZE/2, x+SIZE/2, y+SIZE/2), CORNER, CORNER, paint);
	}
	
	public void setCenterTo(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

}
