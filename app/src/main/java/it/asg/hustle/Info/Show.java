package it.asg.hustle.Info;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

import it.asg.hustle.Interfaces.ThumbnailViewer;

/**
 * Created by sara on 8/26/15.
 */
public class Show implements ThumbnailViewer {
    public String title;
    public String id;
    public String overview;
    public String language;
    public String banner;
    public double rating;
    public String poster;
    public String fanart;
    public String actors;
    public String genre;
    public Bitmap bmp;
    public int seasonNumber;
    public JSONObject source;
    public ArrayList<Season> seasonsList;
    public ArrayList<Friend> friends = null;
    public Episode lastViewed = null;


    public Show(String title)
    {
        this.friends = new ArrayList<Friend>();
        this.seasonsList = new ArrayList<Season>();
        this.title = title;
    }

    public Show(JSONObject jo) {
        Log.d("HUSTLE", "Chiamato costruttore show con parametro: " + jo.toString());
        this.friends = new ArrayList<Friend>();
        try {
            if (jo.has("banner")) {
                this.banner = jo.getString("banner");
            }
            if (jo.has("rating")) {
                this.rating = jo.getDouble("rating");
            }
            if (jo.has("poster")) {
                this.poster = jo.getString("poster");
            }
            if (jo.has("genre")) {
                this.genre = jo.getString("genre");
            }
            if (jo.has("actors")) {
                this.actors = parseActors(jo.getString("actors"));
            }

            if (jo.has("fanart")) {
                this.fanart = jo.getString("fanart");
            }
            if (jo.has("seasons")) {
                this.seasonNumber = jo.getInt("seasons");
                this.seasonsList = new ArrayList<Season>(this.seasonNumber);
                for(int i=0;i<this.seasonNumber;i++){
                    this.seasonsList.add(new Season());
                }
            }
            this.title = jo.getString("seriesname");
            if (jo.has("id")) {
                this.id = ""+jo.getLong("id");
            } else if (jo.has("seriesid")) {
                this.id = "" + jo.getLong("seriesid");
            }

            if (jo.has("friends")) {
                JSONArray friendsJSON = jo.getJSONArray("friends");
                for (int i=0; i< friendsJSON.length(); i++){
                    this.friends.add(new Friend((JSONObject) friendsJSON.get(i)));
                }
            }
            if (jo.has("overview")) {
                this.overview =jo.getString("overview");
            }

            Log.d("HUSTLE", "Show in creazione con lingua: " + jo.getString("language"));
            this.language = new String(jo.getString("language"));

            this.source = jo;
            if (jo.has("last")) {
                if(jo.getJSONObject("last") != null) {
                    this.lastViewed = new Episode(jo.getJSONObject("last"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.bmp = null;
        Log.d("HUSTLE", "Show creato con lingua: " + this.language);
    }

    private String parseActors(String actors) {
        String s = actors;

        s = s.replace("|", ",");

        String a = s.substring(1,s.length()-1);

        return a;
    };



    public JSONObject toJSON()
    {
        JSONArray array = new JSONArray();
        for (int i = 0; i< this.friends.size(); i++){
            try {
                array.put(i,this.friends.get(i).toJSON());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            this.source.put("friends", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this.source;
    }

    public JSONArray toSeasonsJSON() {
        JSONArray seasonsJSON = new JSONArray();
        if(this.seasonsList != null) {
            int i;
            for (i=0;i<this.seasonsList.size();i++){
                try{
                    seasonsJSON.put(this.seasonsList.get(i).source);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return seasonsJSON;
    }

    public void fillSeasonsList(JSONArray seasonsJSON) {
        try {
            if (seasonsJSON != null) {
                int i;
                for (i = 0; i < seasonsJSON.length(); i++) {
                    this.seasonsList.set(i, new Season((JSONArray) seasonsJSON.get(i)));
                }
            }
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Show{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                ", overview='" + overview + '\'' +
                ", language='" + language + '\'' +
                ", banner='" + banner + '\'' +
                '}';
    }

    @Override
    public Bitmap getThumbnail() {
        return this.bmp;
    }

    @Override
    public void setThumbnail(Bitmap thumbnail) {
        if (thumbnail != null)
            this.bmp = thumbnail;
    }
}
