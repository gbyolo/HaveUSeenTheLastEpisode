package it.asg.hustle;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sara on 8/26/15.
 */
public class Episode{
    public String title;
    public int season;
    public int episodeNumber;
    public String episodeId;
    public String seriesID;
    public String overview;
    public String language;
    public String bmpPath;
    public Bitmap bmp;
    public JSONObject source;

    public Episode(String title)
    {
        this.title = title;
    }

    public Episode(JSONObject jo) {
        Log.d("HUSTLE", "Chiamato costruttore episode con parametro: " + jo.toString());

        try {
            if (jo.has("filename")) {
                this.bmpPath = jo.getString("filename");
            }
            this.title = jo.getString("episodename");
            if (jo.has("episodeid")) {
                this.episodeId = ""+jo.getLong("episodeid");
            }
            if (jo.has("seriesid")) {
                this.seriesID = "" + jo.getLong("seriesid");
            }
            if (jo.has("seasonnumber")) {
                this.season = jo.getInt("seasonnumber");
            }
            if (jo.has("episodenumber")) {
                this.episodeNumber = jo.getInt("episodenumber");
            }
            if (jo.has("overview")) {
                this.overview =jo.getString("overview");
            }

            Log.d("HUSTLE", "Show in creazione con lingua: " + jo.getString("language"));
            this.language = new String(jo.getString("language"));

            this.source = jo;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.bmp = null;
        Log.d("HUSTLE", "Show creato con lingua: " + this.language);
    }

    public JSONObject toJSON()
    {
        return this.source;
    }

    @Override
    public String toString() {
        return "Episode{" +
                "title='" + title + '\'' +
                ", episode id='" + episodeId + '\'' +
                ", episode #='" + episodeNumber + '\'' +
                ", season #='" + season + '\'' +
                ", series id='" + seriesID + '\'' +
                ", overview='" + overview + '\'' +
                ", language='" + language + '\'' +
                ", bmp path='" + bmpPath + '\'' +
                '}';
    }
}
