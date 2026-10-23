package com.example.simedarbywalk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import org.json.JSONObject;
import java.util.List;

final class TownshipMapView extends View {
    interface TapListener { void tap(double x, double y); }
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    List<JSONObject> hazards;
    double[][] route;
    double[] walker;
    double[] selected;
    boolean clusters;
    TapListener listener;
    final int navy, red, border, surface, background, orange;

    TownshipMapView(Context context, List<JSONObject> hazards) {
        super(context); this.hazards = hazards;
        navy = context.getColor(R.color.brand_navy); red = context.getColor(R.color.brand_crimson);
        border = context.getColor(R.color.card_border); surface = Color.WHITE;
        background = context.getColor(R.color.app_background); orange = context.getColor(R.color.brand_orange);
        setContentDescription("Simulated township map. Tap to select a destination or report location.");
        setFocusable(true);
    }
    float px(double x) { return (float) (getWidth() * x); }
    float py(double y) { return (float) (getHeight() * y); }
    void line(Canvas c, double ax, double ay, double bx, double by, int color, float width) {
        paint.setColor(color); paint.setStrokeWidth(width); c.drawLine(px(ax), py(ay), px(bx), py(by), paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(background);
        paint.setStyle(Paint.Style.FILL);
        for (int col = 0; col < 4; col++) for (int row = 0; row < 4; row++) {
            float left = px(.04 + col * .24), top = py(.04 + row * .24);
            paint.setColor(border); c.drawRoundRect(left, top, left + px(.11), top + py(.11), 5, 5, paint);
        }
        for (double coordinate : new double[]{.18, .5, .82}) {
            line(c, coordinate, .02, coordinate, .98, border, 30);
            line(c, .02, coordinate, .98, coordinate, border, 30);
            line(c, coordinate, .02, coordinate, .98, surface, 24);
            line(c, .02, coordinate, .98, coordinate, surface, 24);
        }
        if (route != null) {
            Path path = new Path(); path.moveTo(px(route[0][0]), py(route[0][1]));
            for (int i = 1; i < route.length; i++) path.lineTo(px(route[i][0]), py(route[i][1]));
            paint.setColor(red); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(7);
            paint.setStrokeJoin(Paint.Join.ROUND); c.drawPath(path, paint); paint.setStyle(Paint.Style.FILL);
        }
        for (JSONObject h : clusters ? DemoStore.clusters(hazards) : hazards) {
            float x = px(h.optDouble("x")), y = py(h.optDouble("y"));
            paint.setColor(h.optString("type").equals("Debris") ? orange : navy);
            c.drawCircle(x, y, clusters ? 18 : 12, paint);
            paint.setColor(surface); paint.setTextSize(20); paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(clusters ? Integer.toString(h.optInt("count")) : "!", x, y + 7, paint);
        }
        if (selected != null) {
            paint.setColor(red); c.drawCircle(px(selected[0]), py(selected[1]), 16, paint);
            paint.setColor(surface); c.drawCircle(px(selected[0]), py(selected[1]), 6, paint);
        }
        if (walker != null) {
            paint.setColor(surface); c.drawCircle(px(walker[0]), py(walker[1]), 18, paint);
            paint.setColor(navy); c.drawCircle(px(walker[0]), py(walker[1]), 12, paint);
        }
        paint.setTextAlign(Paint.Align.LEFT); paint.setTextSize(25); paint.setColor(navy);
        c.drawText("CITY OF ELMINA", px(.04), py(.97), paint);
        paint.setTextSize(20); c.drawText("Community centre", px(.53), py(.14), paint);
        c.drawText("Homes", px(.04), py(.74), paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            if (listener != null) listener.tap(Math.max(.04, Math.min(.96, event.getX() / getWidth())),
                    Math.max(.04, Math.min(.96, event.getY() / getHeight())));
            return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
