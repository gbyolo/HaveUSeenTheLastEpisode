package it.asg.hustle;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import it.asg.hustle.Info.*;
import it.asg.hustle.Utils.CheckConnection;
import it.asg.hustle.Utils.DBHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {
    String tvShowTitle;
    RecyclerView rw;
    SearchShowRecyclerAdapter adapter;
    ArrayList<Show> shows;
    android.widget.SearchView searchv;
    String locale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        locale = Locale.getDefault().getLanguage();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        shows = new ArrayList<Show>();

        searchv = (android.widget.SearchView) findViewById(R.id.searchView);
        rw = (RecyclerView) findViewById(R.id.recyclerview);
        adapter = new SearchShowRecyclerAdapter(shows, this);
        rw.setLayoutManager(new LinearLayoutManager(SearchActivity.this));
        rw.setAdapter(adapter);
        searchv.setSubmitButtonEnabled(true);
        searchv.setIconifiedByDefault(false);
        searchv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                hideKeyboard();
                doSearch(searchv.getQuery().toString());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        Bundle b = getIntent().getExtras();

        if (b != null) {
            tvShowTitle = b.getString("SearchTitle");
            searchv.setQuery(tvShowTitle, true);
        } else {
            searchv.requestFocus();
        }
        //Log.d("HUSTLE", "SearchActivity onCreate() completed");

    }

    private void hideKeyboard() {
        searchv.clearFocus();
    }

    private void doSearch(final String tvShowTitle) {
        hideKeyboard();
        // Ogni volta che viene effettuata una nuova ricerca
        // resetta l'ArrayList
        shows = new ArrayList<Show>();
        adapter = new SearchShowRecyclerAdapter(shows, this);
        rw.setAdapter(adapter);
        if (!CheckConnection.isConnected(getApplicationContext())) {
            //Log.d("HUSTLE","NO connessione. sto scaricando serie dal db locale");
            // Effettua la ricerca nel DB locale
            JSONArray ja = DBHelper.getSeriesByNameFromDB(tvShowTitle, this.locale);
            if (ja != null) {
                handleJson(ja, false);
                return;
            }

            return;
        }


        //Log.d("HUSTLE", "Searching for serie: " + tvShowTitle);
        final ProgressDialog progDailog = new ProgressDialog(SearchActivity.this);
        final String msg_loading = getResources().getString(R.string.searching);

        // AsyncTask per prendere info su una Serie TV in base
        // al nome. Potrebbe ritornare più elementi in un JSONArray
        AsyncTask<Void, Void, String> at = new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progDailog.setMessage(msg_loading);
                progDailog.setIndeterminate(false);
                progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progDailog.setCancelable(true);
                progDailog.show();
            }

            @Override
            protected String doInBackground(Void... params) {
                URL url = null;
                String s = null;
                try {
                    Uri builtUri = Uri.parse("http://hustle.altervista.org/getSeries.php?").
                            buildUpon().
                            appendQueryParameter("seriesname", tvShowTitle).
                            appendQueryParameter("language", locale).
                            appendQueryParameter("full", null).
                            build();
                    String u = builtUri.toString();
                    //Log.d("HUSTLE", "requesting: " + u);
                    url = new URL(u);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    s = br.readLine();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Log.d("HUSTLE", "returned: " + s);
                return s;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s == null)
                    return;
                //Log.d("HUSTLE", s);
                JSONArray ja = null;
                try {
                    ja = new JSONArray(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                handleJson(ja, true);
                progDailog.dismiss();
                hideKeyboard();
                //Log.d("HUSTLE", ja.toString());
            }
        };

        at.execute();
    }
    // add è true se la stagione va aggiunta al DB, falso altrimenti
    public void handleJson(JSONArray ja, boolean add) {
        for (int i = 0; i< (ja != null ? ja.length() : 0); i++) {
            try {
                JSONObject jo = ja.getJSONObject(i);
                //Log.d("HUSTLE", "Show: " + jo.toString());
                Show s1 = new Show(jo);
                shows.add(s1);
                adapter.notifyDataSetChanged();
                if (add) {
                    //Log.d("HUSTLE", "Sto per aggiungere la serie al DB");
                    DBHelper.addSerieToDB(s1);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        return super.onOptionsItemSelected(item);
    }

}

