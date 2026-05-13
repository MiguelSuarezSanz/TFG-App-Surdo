package com.example.mandosapp_surdo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FiguraView extends View {

    private final Paint paintLinea = new Paint();
    private final Paint paintDot = new Paint();
    private final Path path = new Path();

    private final List<float[]> puntos = new ArrayList<>();
    private float minX, maxX, minY, maxY;

    FiguraView(Context ctx) {
        super(ctx);
        limpiar();

        paintLinea.setColor(Color.parseColor("#6C63FF"));
        paintLinea.setStyle(Paint.Style.STROKE);
        paintLinea.setStrokeWidth(7f);
        paintLinea.setStrokeCap(Paint.Cap.ROUND);
        paintLinea.setStrokeJoin(Paint.Join.ROUND);
        paintLinea.setAntiAlias(true);

        paintDot.setStyle(Paint.Style.FILL);
        paintDot.setAntiAlias(true);
    }

    void agregarPunto(float yaw, float pitch) {
        puntos.add(new float[]{yaw, pitch});
        if (yaw   < minX) minX = yaw;   if (yaw   > maxX) maxX = yaw;
        if (pitch < minY) minY = pitch;  if (pitch > maxY) maxY = pitch;
        postInvalidate();
    }

    void limpiar() {
        puntos.clear();
        path.reset();
        minX = Float.MAX_VALUE;  maxX = -Float.MAX_VALUE;
        minY = Float.MAX_VALUE;  maxY = -Float.MAX_VALUE;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (puntos.size() < 2) return;

        int   w = getWidth();
        int   h = getHeight();
        float margen = 40f;
        float rangoX = (maxX == minX) ? 1 : maxX - minX;
        float rangoY = (maxY == minY) ? 1 : maxY - minY;

        path.reset();
        for (int i = 0; i < puntos.size(); i++) {
            float px = margen + ((puntos.get(i)[0] - minX) / rangoX) * (w - 2 * margen);
            float py = margen + ((puntos.get(i)[1] - minY) / rangoY) * (h - 2 * margen);
            if (i == 0) path.moveTo(px, py);
            else        path.lineTo(px, py);
        }
        canvas.drawPath(path, paintLinea);

        // Punto de inicio (blanco) y punto final (verde)
        float[] ini = puntos.get(0);
        float[] fin = puntos.get(puntos.size() - 1);
        float pxi = margen + ((ini[0] - minX) / rangoX) * (w - 2 * margen);
        float pyi = margen + ((ini[1] - minY) / rangoY) * (h - 2 * margen);
        float pxf = margen + ((fin[0] - minX) / rangoX) * (w - 2 * margen);
        float pyf = margen + ((fin[1] - minY) / rangoY) * (h - 2 * margen);

        paintDot.setColor(Color.WHITE);
        canvas.drawCircle(pxi, pyi, 12f, paintDot);
        paintDot.setColor(Color.parseColor("#43A047"));
        canvas.drawCircle(pxf, pyf, 12f, paintDot);
    }
}