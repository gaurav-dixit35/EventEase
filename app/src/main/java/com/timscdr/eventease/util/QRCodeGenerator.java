package com.timscdr.eventease.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.EnumMap;
import java.util.Map;

/** Creates ticket QR bitmaps locally; no network service or API key is used. */
public final class QRCodeGenerator {
    private QRCodeGenerator() { }

    public static Bitmap create(String value, int size) throws WriterException {
        return create(value, size, null, Color.rgb(13, 37, 69), Color.rgb(201, 162, 39));
    }

    /** Creates a scan-friendly QR with an optional, clearly visible crest at its center. */
    public static Bitmap create(String value, int size, Bitmap logo, int foregroundColor, int accentColor)
            throws WriterException {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("QR value must not be empty.");
        if (size < 128) throw new IllegalArgumentException("QR size must be at least 128 pixels.");
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 4);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int[] row = new int[size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) row[x] = matrix.get(x, y) ? foregroundColor : Color.WHITE;
            bitmap.setPixels(row, 0, size, 0, y, size, 1);
        }
        if (logo != null) drawCenterLogo(bitmap, logo, accentColor);
        return bitmap;
    }

    private static void drawCenterLogo(Bitmap qr, Bitmap logo, int accentColor) {
        int centerSize = Math.round(qr.getWidth() * 0.235f);
        float left = (qr.getWidth() - centerSize) / 2f;
        float top = (qr.getHeight() - centerSize) / 2f;
        float radius = Math.max(8f, centerSize * 0.09f);
        RectF backing = new RectF(left, top, left + centerSize, top + centerSize);
        Canvas canvas = new Canvas(qr);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(backing, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, qr.getWidth() * 0.004f));
        paint.setColor(accentColor);
        canvas.drawRoundRect(backing, radius, radius, paint);
        int inset = Math.round(centerSize * 0.08f);
        Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, centerSize - inset * 2,
                centerSize - inset * 2, true);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(scaledLogo, left + inset, top + inset, paint);
        if (scaledLogo != logo) scaledLogo.recycle();
    }
}
