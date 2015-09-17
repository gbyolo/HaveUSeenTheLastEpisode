package it.asg.hustle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import it.asg.hustle.Info.Episode;
import it.asg.hustle.Info.Friend;
import it.asg.hustle.Info.Season;
import it.asg.hustle.Info.Show;
import it.asg.hustle.Utils.CheckConnection;
import it.asg.hustle.Utils.DBHelper;

public class ShowActivity extends AppCompatActivity {
    private ImageView posterImageView;
    private JSONObject showJSON = null;
    private JSONArray seasonsJSON = null;
    private Bitmap posterBitmap = null;
    private TextView card_description;
    public static Show show;
    private SeasonsAdapter a;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    static private ArrayList<EpisodeRecyclerAdapter> adapterList;
    static private FriendsAdapter adapter_friends;
    private ArrayList<Friend> show_friends = null;
    private ArrayList<Friend> all_friends = null;
    private boolean updateFromServer = false;

    private ArrayList<String> info;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapterList = new ArrayList<EpisodeRecyclerAdapter>();

        //caso in cui l'activity è stata stoppata o messa in pausa, ricrea i dati dai savedInstanceState
        if (savedInstanceState != null) posterBitmap = savedInstanceState.getParcelable("poster"); //ripristina l'immagine salvata poster
        if (savedInstanceState != null) {try {
            //ricrea gli oggetti java show stagioni e episodi
            showJSON = new JSONObject(savedInstanceState.getString("show"));
            } catch (JSONException e1) {
            e1.printStackTrace();
            }
            show.fillSeasonsList(seasonsJSON);
        }


        //caso in cui l'activity viene generata dalla ricerca
        if(savedInstanceState == null){
            Bundle b = getIntent().getExtras();

            if (b != null) {
                String s = b.getString("show");
                try {
                    showJSON = new JSONObject(s);
                    show = new Show(showJSON);
                    doGetShowPoster(showJSON.getString("fanart"));
                    doGetInfo(show);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        //il primo adapter è per le info
        adapterList.add(new EpisodeRecyclerAdapter(getApplicationContext(), ShowActivity.this, new Season()));
        for(int i=1; i<= show.seasonNumber; i++) {
            adapterList.add(new EpisodeRecyclerAdapter(getApplicationContext(), ShowActivity.this, show.seasonsList.get(i - 1)));

        }

        setContentView(R.layout.activity_show);

        // get toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        try {
            collapsingToolbar.setTitle(showJSON.getString("seriesname"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //scarico la lista amici che vedono la serie
        show_friends = new ArrayList<Friend>();
        adapter_friends = new FriendsAdapter(show_friends);
        all_friends = getFriendList();
        downloadFriendShows(all_friends, show_friends, show.id);

        //adapter per stagioni e info
        a = new SeasonsAdapter(getSupportFragmentManager());
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        viewPager.setAdapter(a);


        TabLayout tabLayout = (TabLayout)findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        //get poster image
        posterImageView = (ImageView) findViewById(R.id.show_activity_poster);
        if(posterBitmap!=null){posterImageView.setImageBitmap(posterBitmap);}

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            Log.d("HUSTLE", "landscape mode!");
        }
        // TODO: mostra la serie nell'activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("HUSTLE", "onActivityResult di ShowActivity");
        if (requestCode == EpisodeRecyclerAdapter.EP_CHANGED){
            if (resultCode == Activity.RESULT_OK){

                Bundle b = data.getExtras();
                Boolean status = b.getBoolean("status");
                int ep_num = b.getInt("episode_num");
                int season = b.getInt("season");

                Log.d("HUSTLE", "Episodio n " + ep_num + " stagione " + season + " stato " + status);

                EpisodeRecyclerAdapter era = adapterList.get(season);
                // prende l'episodio
                Episode e = era.getEpisodes().get(ep_num-1);
                // gli cambia stato
                e.checked = status;
                // Avvisa l'adapter che i dati sono cambiati
                era.notifyDataSetChanged();
                // Ora cambia anche il json dell'episodio (per essere consistenti)
                try {
                    e.source.put("seen",status);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            } else {
                Log.d("HUSTLE", "result: "+resultCode+" OK è " + Activity.RESULT_OK);
            }
        } else {
            Log.d("HUSTLE", ""+requestCode);
        }
    }
    private void doGetShowPoster(String imageUrl) {

        AsyncTask<String, Void, Bitmap> at = new AsyncTask<String, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(String... params) {
                Bitmap bm;
                InputStream in = null;
                try {
                    in = new java.net.URL(params[0]).openStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bm = BitmapFactory.decodeStream(in);
                return bm;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                posterImageView.setImageBitmap(bitmap);
                posterBitmap = bitmap;

            }
        };
        at.execute(imageUrl);
    }

    private void doGetInfoSeason(final Show showInfo, final int seasonNumber){
        /* TODO: prende serie dal DB. Funziona già tutto, ma bisogna implementare
            che se l'utente poi fa il login su facebook si deve chiedere se si vogliono
            tenere i dati locali o quelli del server esterno
        */
        final String id = getSharedPreferences("id_facebook", Context.MODE_PRIVATE).getString("id_facebook", null);
        String name = getSharedPreferences("name_facebook", Context.MODE_PRIVATE).getString("name_facebook", null);
        final boolean logged = getSharedPreferences("logged", Context.MODE_PRIVATE).getBoolean("logged", false);

        // se c'è connessione a internet
        if (CheckConnection.isConnected(this)) {
            // se l'utente è loggato
            if (id != null && name != null && logged) {
                // prendi i dati dal server esterno e aggiorna il DB
                updateFromServer = true;
            }
        }
        // Se non c'è necessità di aggiornare dal server, prendi i dati dal DB locale se ci sono
        if (!updateFromServer) {
            JSONArray season = DBHelper.getSeasonFromDB(showInfo, seasonNumber);
            if (season != null) {
                Log.d("HUSTLE", "Trovata la stagione nel DB");
                show.seasonsList.get(seasonNumber - 1).fromJson(season);
                show.seasonsList.get(seasonNumber - 1).seasonNumber = seasonNumber;
                return;
            }
        }

        AsyncTask<String, Void, Season> st = new AsyncTask<String, Void, Season>() {
            @Override
            protected Season doInBackground(String... params) {
                ArrayList<Episode> seasonList = new ArrayList<Episode>();
                String s=null;
                JSONArray seasonJSON = null;
                // Se l'utente è loggato tramite facebook e sul server esterno, aggiunge il suo id alla richiesta
                // in modo che la risposta del server conterrà gli episodi già visti (campo "seen" del json object)
                String x = "";
                if (id != null && logged)
                    x = "&user_id="+id;
                //richiesta dati episodi della stagione
                while (seasonJSON == null) {
                    try {
                        URL url = new URL("http://hustle.altervista.org/getEpisodes.php?seriesid=" + params[0] + "&season=" + params[1] + x);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        s = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //creazione array dalla risposta
                    try {
                        seasonJSON = new JSONArray(s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                //creazione elemento java da arraylist
                int number = Integer.parseInt(params[1]);
                show.seasonsList.get(number-1).fromJson(seasonJSON);
                show.seasonsList.get(number-1).seasonNumber = number;

                return show.seasonsList.get(number-1);
            }

            @Override
            protected void onPostExecute(Season season) {
                super.onPostExecute(season);
                adapterList.get(season.seasonNumber).notifyDataSetChanged();

                // se updateFromServer è false, significa che la stagione non si trova
                // nel DB e devo aggiungerla
                if (!updateFromServer) {
                    Log.d("HUSTLE", "Aggiungo la stagione al DB");
                    DBHelper.addSeasonDB(show.seasonsList.get(season.seasonNumber - 1));
                } else {
                    Log.d("HUSTLE", "Devo aggiornare la serie dal server");
                    DBHelper.updateSeasonDB(show.seasonsList.get(season.seasonNumber - 1));
                }
            }
        };

        st.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, showInfo.id, "" + seasonNumber);
    }


    private void doGetInfo(final Show showInfo) {
        int i;
        for(i=1; i<=showInfo.seasonNumber; i++){
            doGetInfoSeason(showInfo, i);
        }
        //Log.d("HUSTLE", "creato java file di show, " + showInfo.seasonsList.get(0) + "  " + showInfo.seasonsList.get(1));
    }


    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelable("poster", posterBitmap);
        savedInstanceState.putString("show", showJSON.toString());
        seasonsJSON = show.toSeasonsJSON();

        if(seasonsJSON != null){savedInstanceState.putString("seasons", seasonsJSON.toString());}
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // sottoclasse per gestire i fragment della pagina inziale
    public static class SeasonsFragment extends Fragment {
        private static final String TAB_POSITION = "tab_position";

        public SeasonsFragment() {

        }

        public static SeasonsFragment newInstance(int tabPosition) {
            SeasonsFragment fragment = new SeasonsFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_POSITION, tabPosition);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);

            if (tabPosition == 0) {
                Log.d("HUSTLE", "Questo è il fragment con le info");
                // TODO: modifica questo fragment in modo da mostrare le info sulla serie TV
            }


            View v;

            if (tabPosition == 0){
                v = inflater.inflate(R.layout.cardview_info_scrollview, container,false);
                TextView card_description = (TextView) v.findViewById(R.id.card_description_text);
                card_description.setText(show.overview);
                RecyclerView recyclerView = (RecyclerView)v.findViewById(R.id.recyclerview_friends_card);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
                recyclerView.setAdapter(adapter_friends);
            }
            else {
                v = inflater.inflate(R.layout.fragment_episodes_view, container, false);
                RecyclerView recyclerView = (RecyclerView)v.findViewById(R.id.recyclerview);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                recyclerView.setAdapter(adapterList.get(tabPosition));
            }
            return v;
        }
    }

    //sottoclasse per l'adapter per i fragment (delle varie tab)
    class SeasonsAdapter extends FragmentStatePagerAdapter {

        private int number_of_tabs=show.seasonNumber+1;

        public SeasonsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return SeasonsFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return number_of_tabs;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            if (position == 0) {
                return getResources().getString(R.string.tab_show_info);
            }
            int i;
                // break;
            for (i=1; i<=number_of_tabs; i++) {
                if (position == i) {
                    return getResources().getString(R.string.tab_season) + " " + i;
                    // break;
                }
            }
            return "";
        }
    }


    ArrayList<Friend> getFriendList(){
        SharedPreferences options = getSharedPreferences("friend_list", Context.MODE_PRIVATE);
        String friend_list_json_string = options.getString("friend_list", null);
        ArrayList<Friend> return_list = new ArrayList<Friend>();
        if(friend_list_json_string != null){
            try {
                JSONArray friend_list_json = new JSONArray(friend_list_json_string);
                for (int i= 0; i< friend_list_json.length(); i++) {
                    return_list.add(new Friend(friend_list_json.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return return_list;
    }

    void downloadFriendShows(ArrayList<Friend> all_friends, ArrayList<Friend> friends_list_adapter, final String series_id){
//async task, prende come parametro la lista amici totale e la lista amici vuota


        AsyncTask<ArrayList<Friend>, Void, ArrayList<Friend>> friend_shows_download = new AsyncTask<ArrayList<Friend>, Void, ArrayList<Friend>>() {
            @Override
            protected ArrayList<Friend> doInBackground(ArrayList<Friend> ...params) {
                String s=null;
                JSONArray friendshowsJSON = null;
                ArrayList<Friend> all_friends = params[0];
                ArrayList<Friend> show_friends = params[1];
                Friend actual = null;
                String user_id = null;


                for (int i= 0; i < all_friends.size(); i++){
                    actual = all_friends.get(i);
                    user_id = actual.id;

                    try {
                        URL url = new URL("http://hustle.altervista.org/getSeries_bis.php?user_id_short=" + user_id + "&seriesid_short=" + series_id);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        s = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //creazione array dalla risposta
                    try {
                        friendshowsJSON= new JSONArray(s);
                        if(friendshowsJSON.length() >= 1){
                            show_friends.add(actual);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                return show_friends;

            }


            @Override
            protected void onPostExecute(ArrayList<Friend> show_friends) {
                super.onPostExecute(show_friends);

                adapter_friends.notifyDataSetChanged();

            }
        };

        friend_shows_download.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, all_friends, friends_list_adapter);
    }


}
