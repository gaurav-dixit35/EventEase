package com.timscdr.eventease.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import androidx.exifinterface.media.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Compresses organizer-selected photos to a Firestore-safe JPEG data URI. */
public final class EventImageEncoder {
    public static final String DATA_URI_PREFIX = "data:image/jpeg;base64,";
    private static final int MAX_IMAGE_BYTES = 20 * 1024;
    private static final int MAX_BASE64_CHARS = ((MAX_IMAGE_BYTES + 2) / 3) * 4;
    private static final int[] MAX_DIMENSIONS = {960, 800, 640, 560, 480, 400, 360, 320, 280, 240, 200, 160, 128, 96};
    private static final int[] JPEG_QUALITIES = {82, 72, 62, 52, 42, 32, 24, 16, 10, 5};

    private EventImageEncoder() { }

    public static boolean needsCompression(String value) {
        if (value == null || value.isEmpty()) return false;
        return !value.startsWith(DATA_URI_PREFIX)
                || value.length() - DATA_URI_PREFIX.length() > MAX_BASE64_CHARS;
    }

    public static boolean isOversizedDataUri(String value) {
        return value != null && value.startsWith(DATA_URI_PREFIX)
                && value.length() - DATA_URI_PREFIX.length() > MAX_BASE64_CHARS;
    }

    public static String compressToDataUri(Context context, Uri source) {
        if (source == null) throw new IllegalArgumentException("An image must be selected.");
        ContentResolver resolver = context.getContentResolver();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = resolver.openInputStream(source)) {
            if (stream == null) throw new IllegalArgumentException("The selected image cannot be opened.");
            BitmapFactory.decodeStream(stream, null, bounds);
        } catch (Exception error) {
            throw new IllegalArgumentException("The selected image cannot be read.", error);
        }
        if (bounds.outWidth < 1 || bounds.outHeight < 1)
            throw new IllegalArgumentException("The selected file is not a supported image.");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSIONS[0] * 2);
        Bitmap decoded;
        try (InputStream stream = resolver.openInputStream(source)) {
            if (stream == null) throw new IllegalArgumentException("The selected image cannot be opened.");
            decoded = BitmapFactory.decodeStream(stream, null, options);
        } catch (Exception error) {
            throw new IllegalArgumentException("The selected image cannot be decoded.", error);
        }
        if (decoded == null) throw new IllegalArgumentException("The selected file is not a supported image.");

        Bitmap oriented = applyOrientation(resolver, source, decoded);
        if (oriented != decoded) decoded.recycle();
        return encodeBitmap(oriented);
    }

    /** Re-compresses older synced photos when an organizer edits an event. */
    public static String compressDataUri(String dataUri) {
        if (dataUri == null || !dataUri.startsWith(DATA_URI_PREFIX))
            throw new IllegalArgumentException("The existing event image is invalid.");
        byte[] source;
        try {
            source = Base64.decode(dataUri.substring(DATA_URI_PREFIX.length()), Base64.DEFAULT);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("The existing event image is invalid.", error);
        }
        Bitmap decoded = BitmapFactory.decodeByteArray(source, 0, source.length);
        if (decoded == null) throw new IllegalArgumentException("The existing event image cannot be decoded.");
        return encodeBitmap(decoded);
    }

    private static String encodeBitmap(Bitmap oriented) {
        byte[] bestBytes = null;
        long bestScore = -1;
        int lastEffectiveDimension = -1;
        try {
            for (int maxDimension : MAX_DIMENSIONS) {
                int effectiveDimension = Math.min(maxDimension, Math.max(oriented.getWidth(), oriented.getHeight()));
                if (effectiveDimension == lastEffectiveDimension) continue;
                lastEffectiveDimension = effectiveDimension;
                Bitmap candidate = scaleDown(oriented, maxDimension);
                try {
                    for (int quality : JPEG_QUALITIES) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        if (candidate.compress(Bitmap.CompressFormat.JPEG, quality, output)
                                && output.size() <= MAX_IMAGE_BYTES) {
                            long score = (long) candidate.getWidth() * candidate.getHeight() * quality;
                            if (score > bestScore) {
                                bestScore = score;
                                bestBytes = output.toByteArray();
                            }
                        }
                    }
                } finally {
                    if (candidate != oriented) candidate.recycle();
                }
            }
        } finally {
            oriented.recycle();
        }
        if (bestBytes == null) throw new IllegalArgumentException("The selected image could not be compressed below 20 KiB.");
        return DATA_URI_PREFIX + Base64.encodeToString(bestBytes, Base64.NO_WRAP);
    }

    private static int sampleSize(int width, int height, int target) {
        int sample = 1;
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) sample *= 2;
        while (Math.max(width / sample, height / sample) > target * 2) sample *= 2;
        return sample;
    }

    private static Bitmap applyOrientation(ContentResolver resolver, Uri source, Bitmap bitmap) {
        try (InputStream stream = resolver.openInputStream(source)) {
            if (stream == null) return bitmap;
            int orientation = new ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            float degrees = orientation == ExifInterface.ORIENTATION_ROTATE_90 ? 90f
                    : orientation == ExifInterface.ORIENTATION_ROTATE_180 ? 180f
                    : orientation == ExifInterface.ORIENTATION_ROTATE_270 ? 270f : 0f;
            if (degrees == 0f) return bitmap;
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception ignored) {
            return bitmap;
        }
    }

    private static Bitmap scaleDown(Bitmap source, int maxDimension) {
        int largest = Math.max(source.getWidth(), source.getHeight());
        if (largest <= maxDimension) return source;
        float scale = (float) maxDimension / largest;
        return Bitmap.createScaledBitmap(source, Math.round(source.getWidth() * scale),
                Math.round(source.getHeight() * scale), true);
    }
}
