package model.firebase.Storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class StorageRepository {

    private final FirebaseStorage storage;

    public StorageRepository() {
        this.storage = FirebaseStorage.getInstance();
    }

    // ===== Public callbacks =====
    public interface UploadCallback {
        void onSuccess(@NonNull String downloadUrl);
        void onFailure(@NonNull Exception e);
    }

    public interface ProgressListener {
        void onProgress(long bytesTransferred, long totalBytes);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(@NonNull Exception e);
    }

    // ===== Profile photo =====

    /**
     * העלאת תמונת פרופיל כפי שהיא (ללא דחיסה), לשביל: users/{uid}/profile.jpg
     * תחזיר URL להטמעה ב־Firestore (users/{uid}.profileImageUrl).
     */
    @Nullable
    public UploadTask uploadUserProfilePhoto(@NonNull Uri fileUri,
                                             @Nullable String userId,
                                             @Nullable ProgressListener progress,
                                             @NonNull UploadCallback cb) {
        String uid = (userId != null) ? userId : FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            cb.onFailure(new IllegalStateException("Not signed in"));
            return null;
        }

        StorageReference ref = storage.getReference()
                .child("users")
                .child(uid)
                .child("profile.jpg");

        UploadTask task = ref.putFile(fileUri, new StorageMetadata.Builder()
                .setContentType("image/*")
                .build());

        if (progress != null) {
            task.addOnProgressListener(s -> progress.onProgress(s.getBytesTransferred(), s.getTotalByteCount()));
        }

        task.addOnSuccessListener(snap ->
                ref.getDownloadUrl().addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                        .addOnFailureListener(cb::onFailure)
        ).addOnFailureListener(cb::onFailure);

        return task;
    }

    /**
     * העלאת תמונת פרופיל עם דחיסה/הקטנה מקסימלית.
     * @param maxDimPx  לדוג׳ 1080 – המימד הארוך יוגבל לגודל זה (שומר יחס).
     * @param quality   איכות JPEG (0..100), מומלץ ~80.
     */
    @Nullable
    public UploadTask uploadUserProfilePhotoCompressed(@NonNull Context ctx,
                                                       @NonNull Uri fileUri,
                                                       @Nullable String userId,
                                                       int maxDimPx,
                                                       int quality,
                                                       @Nullable ProgressListener progress,
                                                       @NonNull UploadCallback cb) {
        String uid = (userId != null) ? userId : FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            cb.onFailure(new IllegalStateException("Not signed in"));
            return null;
        }

        StorageReference ref = storage.getReference()
                .child("users")
                .child(uid)
                .child("profile.jpg");

        byte[] data;
        try {
            data = decodeAndCompress(ctx, fileUri, maxDimPx, quality);
        } catch (Exception e) {
            cb.onFailure(e);
            return null;
        }

        UploadTask task = ref.putBytes(data, new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build());

        if (progress != null) {
            task.addOnProgressListener(s -> progress.onProgress(s.getBytesTransferred(), s.getTotalByteCount()));
        }

        task.addOnSuccessListener(snap ->
                ref.getDownloadUrl().addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                        .addOnFailureListener(cb::onFailure)
        ).addOnFailureListener(cb::onFailure);

        return task;
    }

    public UploadTask uploadCommunityProfileImage(
            @NonNull Context ctx,
            @NonNull String communityId,
            @NonNull Uri fileUri,
            int maxDimPx,
            int quality,
            @Nullable ProgressListener progress,
            @NonNull UploadCallback cb) {

        StorageReference ref = storage.getReference()
                .child("communities")
                .child(communityId)
                .child("profile.jpg");

        byte[] data;
        try {
            data = decodeAndCompress(ctx, fileUri, maxDimPx, quality);
        } catch (Exception e) {
            cb.onFailure(e);
            return null;
        }

        UploadTask task = ref.putBytes(data, new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build());

        task.addOnSuccessListener(snap ->
                ref.getDownloadUrl().addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                        .addOnFailureListener(cb::onFailure)
        ).addOnFailureListener(cb::onFailure);

        return task;
    }

    // ===== Feed image =====

    /**
     * העלאת תמונה לפוסט בלוח מודעות: communities/{communityId}/feed/{postId}/{uuid}.{ext}
     * מחזיר URL – אתה שומר אותו בשדה imageUrl של הפוסט ב־Firestore.
     */
    @Nullable
    public UploadTask uploadFeedImage(@NonNull Context ctx,
                                      @NonNull String communityId,
                                      @NonNull String postId,
                                      @NonNull Uri fileUri,
                                      @Nullable ProgressListener progress,
                                      @NonNull UploadCallback cb) {

        String ext = guessExtension(ctx, fileUri);
        String fileName = UUID.randomUUID() + "." + ext;

        StorageReference ref = storage.getReference()
                .child("communities")
                .child(communityId)
                .child("feed")
                .child(postId)
                .child(fileName);

        String mime = guessMime(ctx, fileUri);

        UploadTask task = ref.putFile(fileUri, new StorageMetadata.Builder()
                .setContentType(mime != null ? mime : "image/*")
                .build());

        if (progress != null) {
            task.addOnProgressListener(s -> progress.onProgress(s.getBytesTransferred(), s.getTotalByteCount()));
        }

        task.addOnSuccessListener(snap ->
                ref.getDownloadUrl().addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                        .addOnFailureListener(cb::onFailure)
        ).addOnFailureListener(cb::onFailure);

        return task;
    }

    /**
     * גרסה דחוסה להעלאת תמונה לפיד.
     */
    @Nullable
    public UploadTask uploadFeedImageCompressed(@NonNull Context ctx,
                                                @NonNull String communityId,
                                                @NonNull String postId,
                                                @NonNull Uri fileUri,
                                                int maxDimPx,
                                                int quality,
                                                @Nullable ProgressListener progress,
                                                @NonNull UploadCallback cb) {

        String fileName = UUID.randomUUID() + ".jpg"; // דוחסים ל-JPEG
        StorageReference ref = storage.getReference()
                .child("communities")
                .child(communityId)
                .child("feed")
                .child(postId)
                .child(fileName);

        byte[] data;
        try {
            data = decodeAndCompress(ctx, fileUri, maxDimPx, quality);
        } catch (Exception e) {
            cb.onFailure(e);
            return null;
        }

        UploadTask task = ref.putBytes(data, new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build());

        if (progress != null) {
            task.addOnProgressListener(s -> progress.onProgress(s.getBytesTransferred(), s.getTotalByteCount()));
        }

        task.addOnSuccessListener(snap ->
                ref.getDownloadUrl().addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                        .addOnFailureListener(cb::onFailure)
        ).addOnFailureListener(cb::onFailure);

        return task;
    }

    @Nullable
    public UploadTask uploadReportImageCompressed(
            @NonNull Context ctx,
            @NonNull String communityId,
            @NonNull String reportId,
            @NonNull Uri fileUri,
            int maxDimPx,
            int quality,
            @Nullable ProgressListener progress,
            @NonNull UploadCallback cb
    ) {
        String fileName = java.util.UUID.randomUUID() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("communities")
                .child(communityId)
                .child("reports")
                .child(reportId)
                .child(fileName);

        byte[] data;
        try {
            data = decodeAndCompress(ctx, fileUri, maxDimPx, quality);
        } catch (Exception e) {
            cb.onFailure(e);
            return null;
        }

        UploadTask task = ref.putBytes(data, new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build());

        if (progress != null) {
            task.addOnProgressListener(s -> progress.onProgress(s.getBytesTransferred(), s.getTotalByteCount()));
        }

        task.addOnSuccessListener(snap ->
                ref.getDownloadUrl().addOnSuccessListener(url -> cb.onSuccess(url.toString()))
                        .addOnFailureListener(cb::onFailure)
        ).addOnFailureListener(cb::onFailure);

        return task;
    }

    // ===== Delete helpers (אופציונלי אבל שימושי) =====

    /** מחיקה לפי downloadUrl (כששמורים לך ה־URLs). */
    public void deleteByUrl(@NonNull String downloadUrl, @NonNull SimpleCallback cb) {
        try {
            StorageReference ref = storage.getReferenceFromUrl(downloadUrl);
            ref.delete().addOnSuccessListener(v -> cb.onSuccess())
                    .addOnFailureListener(cb::onFailure);
        } catch (Exception e) {
            cb.onFailure(e);
        }
    }

    /** מחיקה לפי שביל storage (למשל "users/{uid}/profile.jpg"). */
    public void deleteByPath(@NonNull String path, @NonNull SimpleCallback cb) {
        StorageReference ref = storage.getReference().child(path);
        ref.delete().addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    // ===== Private utils =====

    @NonNull
    private static byte[] decodeAndCompress(@NonNull Context ctx, @NonNull Uri uri, int maxDim, int quality) throws Exception {
        // שלב 1: קרא ממדים בלי לטעון זיכרון מלא
        InputStream boundsIs = ctx.getContentResolver().openInputStream(uri);
        if (boundsIs == null) throw new IllegalArgumentException("Cannot open input stream");
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(boundsIs, null, bounds);
        try { boundsIs.close(); } catch (Exception ignored) {}

        int origW = bounds.outWidth;
        int origH = bounds.outHeight;
        if (origW <= 0 || origH <= 0) throw new IllegalStateException("Invalid image bounds");

        // חישוב inSampleSize
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calcInSampleSize(origW, origH, maxDim, maxDim);

        // שלב 2: Decode scaled
        InputStream imgIs = ctx.getContentResolver().openInputStream(uri);
        if (imgIs == null) throw new IllegalArgumentException("Cannot open image stream");
        Bitmap decoded = BitmapFactory.decodeStream(imgIs, null, opts);
        try { imgIs.close(); } catch (Exception ignored) {}

        if (decoded == null) throw new IllegalStateException("Failed to decode bitmap");

        // שלב 3: scale מדויק לשמירה על המימד הארוך
        int w = decoded.getWidth();
        int h = decoded.getHeight();
        float scale = Math.min(1f, (float) maxDim / Math.max(w, h));
        Bitmap scaled = (scale < 1f)
                ? Bitmap.createScaledBitmap(decoded, Math.round(w * scale), Math.round(h * scale), true)
                : decoded;

        // שלב 4: דחיסה ל-JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] data = baos.toByteArray();
        try { baos.close(); } catch (Exception ignored) {}

        if (scaled != decoded) decoded.recycle();
        scaled.recycle(); // (השורה הזו בטוחה – כבר יש לנו bytes)

        return data;
    }

    private static int calcInSampleSize(int srcW, int srcH, int reqW, int reqH) {
        int inSample = 1;
        if (srcH > reqH || srcW > reqW) {
            final int halfH = srcH / 2;
            final int halfW = srcW / 2;
            while ((halfH / inSample) >= reqH && (halfW / inSample) >= reqW) {
                inSample *= 2;
            }
        }
        return Math.max(1, inSample);
    }

    @Nullable
    private static String guessMime(@NonNull Context ctx, @NonNull Uri uri) {
        try {
            return ctx.getContentResolver().getType(uri);
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private static String guessExtension(@NonNull Context ctx, @NonNull Uri uri) {
        String mime = guessMime(ctx, uri);
        String ext = (mime != null) ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) : null;
        if (ext == null || ext.isEmpty()) ext = "jpg";
        return ext;
    }
}