package it.asg.hustle.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import it.asg.hustle.Interfaces.ThumbnailViewer;


/**
 * Created by gbyolo on 9/19/15.
 */
public class ImageDownloader {
    private Context ctx;
    private int reqWidth;
    private int reqHeight;
    private Bitmap placeHolder;
    private Resources res;

    public ImageDownloader(Context context, int reqWidth, int reqHeight) {
        this.ctx = context;
        this.reqHeight = reqHeight;
        this.reqWidth = reqWidth;
    }

    public ImageDownloader(Context context, Resources res, int holderID, int reqWidth, int reqHeight) {
        this.ctx = context;
        this.reqHeight = reqHeight;
        this.reqWidth = reqWidth;
        this.res = res;
        this.placeHolder = BitmapFactory.decodeResource(res, holderID);
    }

    public ImageDownloader(Context context, Resources res, Bitmap placeHolder, int reqWidth, int reqHeight) {
        this.ctx = context;
        this.reqHeight = reqHeight;
        this.reqWidth = reqWidth;
        this.res = res;
        this.placeHolder = placeHolder;
    }

    public boolean download(String url, ImageView imageView, ThumbnailViewer tv) {

        if (imageView == null || tv == null) {
            return false;
        }

        //Bitmap bmp = BitmapMemoryCache.getBitmapFromMemCache(url);
        Bitmap bmp = BitmapCache.get(url);
        if (bmp != null) {
            Log.d("HUSTLE", "foto in cache!");
            imageView.setImageBitmap(bmp);
            tv.setThumbnail(bmp);
            return true;
        }
        // Se la foto non è in cache e non sei connesso a internet, non scaricare
        if (CheckConnection.isConnected(ctx) == false) {
            return false;
        }
        if (cancelPotentialDownload(url, imageView)) {
            //Log.d("HUSTLE", "ImageDownloader Download OK");
            BitmapDownloader task = new BitmapDownloader(ctx, imageView, reqWidth, reqHeight, tv);
            AsyncDrawable asyncDrawable = new AsyncDrawable(res, placeHolder, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(url);
        }
        return true;
    }

    // Cancella un potenziale download inutile
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        // Prende l'AsyncTask associato all'ImageView
        BitmapDownloader bitmapDownloader = getBitmapDownloader(imageView);
        // Se è diverso da null
        if (bitmapDownloader != null) {
            // Prende l'url che deve scaricare
            String bitmapUrl = bitmapDownloader.getUrl();
            // Se è uguale a null oppure è diverso da quello che devo scaricare,
            // lo interrompo perché la View è in fase di riciclo ed è partito un nuovo
            // AsyncTask per lei, quindi quello vecchio va fermato
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                // cancella l'AsyncTask
                bitmapDownloader.cancel(true);
            } else {
                // Altrimenti, lo stesso URL è già in download, lo lascio continuare
                return false;
            }
        }
        return true;
    }

    // Prende il BitmapDownloader associato all'ImageView
    protected static BitmapDownloader getBitmapDownloader(ImageView imageView) {
        if (imageView != null) {
            // Prende la Drawable
            Drawable drawable = imageView.getDrawable();
            // Se è di tipo AsyncDrawable
            if (drawable instanceof AsyncDrawable) {
                // La casta ad AsyncDrawable
                AsyncDrawable asyncDrawable = (AsyncDrawable)drawable;
                // Ritorna la referenza dell'AsyncTask associato alla ImageView
                return asyncDrawable.getBitmapDownloader();
            }
        }
        return null;
    }
}
