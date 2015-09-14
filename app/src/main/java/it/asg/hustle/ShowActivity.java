package it.asg.hustle;

import android.app.Activity;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import it.asg.hustle.Info.Season;
import it.asg.hustle.Info.Show;

public class ShowActivity extends AppCompatActivity {
    private ImageView posterImageView;
    private JSONObject showJSON = null;
    private JSONArray seasonsJSON = null;
    private Bitmap posterBitmap = null;
    public static Show show;
    private SeasonsAdapter a;
    private ViewPager viewPager;
    static private ArrayList<EpisodeRecyclerAdapter> adapterList;
    private ArrayList<String> info;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapterList = new ArrayList<EpisodeRecyclerAdapter>();
        if (savedInstanceState != null) posterBitmap = savedInstanceState.getParcelable("poster");
        if (savedInstanceState != null) {try {
            showJSON = new JSONObject(savedInstanceState.getString("show"));
            } catch (JSONException e1) {
            e1.printStackTrace();
            }
            show.fillSeasonsList(seasonsJSON);
        }
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

        adapterList.add(new EpisodeRecyclerAdapter(new Season()));
        for(int i=1; i<= show.seasonNumber; i++) {
            adapterList.add(new EpisodeRecyclerAdapter(show.seasonsList.get(i-1)));
        }
        /*
        Season currentSeason = (show.seasonsList.get(tabPosition-1));
        for (int i = 0; i < currentSeason.episodeNumber; i++) {
            items.add(currentSeason.episodesList.get(i).title);
        }
*/
        setContentView(R.layout.activity_show);



        // get toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        try {
            collapsingToolbar.setTitle(showJSON.getString("seriesname"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        a = new SeasonsAdapter(getSupportFragmentManager());

        viewPager = (ViewPager)findViewById(R.id.viewpager);
        viewPager.setAdapter(a);


        TabLayout tabLayout = (TabLayout)findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        //get poster image
        posterImageView = (ImageView) findViewById(R.id.show_activity_poster);
        if(posterBitmap!=null){posterImageView.setImageBitmap(posterBitmap);}


        // TODO: mostra la serie nell'activity

        Log.d("HUSTLE", "Devo mostrare la serie: " + showJSON);
    }

    private void doGetShowPoster(String imageUrl) {


        AsyncTask<String, Void, Bitmap> at = new AsyncTask<String, Void, Bitmap>() {

            @Override
            protected Bitmap doInBackground(String... params) {
                Bitmap bm = null;
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
        AsyncTask<String, Void, Season> st = new AsyncTask<String, Void, Season>() {
            @Override
            protected Season doInBackground(String... params) {
                ArrayList<Episode> seasonList = new ArrayList<Episode>();
                String s=null;
                JSONArray seasonJSON = null;
                //richiesta dati episodi della stagione
                while (seasonJSON == null) {
                    try {
                        URL url = new URL("http://hustle.altervista.org/getEpisodes.php?seriesid=" + params[0] + "&season=" + params[1]);
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
            }
        };

        st.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, showInfo.id, "" + seasonNumber);
    }


    private void doGetInfo(final Show showInfo) {
        ArrayList<Season> list = showInfo.seasonsList;

        int i=0;
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
            // TODO: barra di notifica deve colorarsi con immagine poster! deve!
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);
            // TODO: modifica questo fragment in modo da mostrare le info sulla serie TV




            View v = inflater.inflate(R.layout.fragment_episodes_view, container, false);
            RecyclerView recyclerView = (RecyclerView)v.findViewById(R.id.recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            recyclerView.setAdapter( adapterList.get(tabPosition));

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
            int i=1;
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




}
