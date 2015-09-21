package it.asg.hustle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;;

import it.asg.hustle.Info.Friend;
import it.asg.hustle.Info.Show;

import it.asg.hustle.Utils.BitmapCache;
import it.asg.hustle.Utils.CheckConnection;
import it.asg.hustle.Utils.DBHelper;
import it.asg.hustle.Utils.MD5;


public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;                // la toolbar
    private DrawerLayout myDrawerLayout;    // imposta NavigationDrawer
    private FloatingActionButton fab;       // FloatingActionButton
    private NavigationView navigationView;  // NavigationView (per il navigation drawer)

    private MenuItem SearchAction;
    private boolean isSearchOpened = false;
    private EditText edtSeach;

    private Display display;
    static int numOfElements = 3;          //3=default(small and normal); 4=large; 5=xlarge

    private SQLiteDatabase db = null;
    private SQLiteOpenHelper helper = null;

    private com.facebook.login.widget.ProfilePictureView profilePictureInvisible = null;
    private de.hdodenhof.circleimageview.CircleImageView circleImageView = null;
    private TextView account_name_facebook_tv = null;

    private ViewPager viewPager ;

    private boolean logged = false;
    private ArrayList<Friend> friendsList;

    private BitmapCache bitmapCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize facebook sdk
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        // apre o crea il db
        helper = new DBHelper(this);
        db = helper.getWritableDatabase();
        DBHelper.getInstance(this);
        Log.d("HUSTLE", "Aperto database con nome: " + helper.getDatabaseName());

        // Initialize bitmap cache
        bitmapCache = BitmapCache.createOrOpen(getApplicationContext());

        // imposto ActionBar sulla Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.title));

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //prendo DrawerLayout
        myDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        //click elementi su NavigationDrawer
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                myDrawerLayout.closeDrawers();
                Toast.makeText(MainActivity.this, menuItem.getTitle(), Toast.LENGTH_LONG).show();
                if (menuItem.getTitle().equals(getResources().getString(R.string.nav_item_login)) == true) {
                    // accesso facebook
                    Intent intentactivityfacebook = new Intent(MainActivity.this, FacebookActivity.class);
                    startActivity(intentactivityfacebook);

                }
                else if (menuItem.getTitle().equals(getResources().getString(R.string.nav_item_tvshow)) == true) {
                    // stai cliccando sul tasto che riporta alle tue serie tv
                    viewPager.setCurrentItem(0);
                }
                else if (menuItem.getTitle().equals(getResources().getString(R.string.nav_item_contactus)) == true) {
                    // intent per mandare email
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("application/email");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"hustle.asg@gmail.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "[HUSTLE - contact]");
                    //i.putExtra(Intent.EXTRA_TEXT, "(body)");

                    startActivity(Intent.createChooser(i, "Send Email"));

                }
                return true;

            }
        });

        //click del FAB
        fab = (FloatingActionButton) findViewById(R.id.fab_plus);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("HUSTLE", "FAB was pressed");
                // Launch activity for searching a TV show
                startActivity(new Intent(getApplicationContext(), SearchActivity.class));
            }
        });

        // prendo dimensioni del display per vedere quanti elementi della griglia posso mettere
        display = getWindowManager().getDefaultDisplay();
        MainActivity.numOfElements = getDisplayDimensions(display);

        // Crea un TvShowAdapter
        TvShowAdapter adapter = new TvShowAdapter(getSupportFragmentManager());
        // Prende il ViewPager e imposta come adapter il TvShowAdapter: in base alla tab
        // selezionata, mostra il fragment relativo
        viewPager = (ViewPager)findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        // Prende il TabLayout e imposta il ViewPager
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);
        // Imposta
        circleImageView = (de.hdodenhof.circleimageview.CircleImageView)findViewById(R.id.circleView);
        profilePictureInvisible = (com.facebook.login.widget.ProfilePictureView)findViewById(R.id.profilePictureInvisible);
        account_name_facebook_tv = (TextView) findViewById(R.id.account_name_facebook);
        //Log.d("HUSTLE", "profilePictureInvisible: " + profilePictureInvisible);
        if (CheckConnection.isConnected(this)) {
            updateLogInServer();
            updateCircleProfile();
            updateFriendList();
        }
    }

    public void updateLogInServer() {
        // Se l'utente è loggato tramite facebook, vede se è loggato sul server esterno
        // Se non è loggato sul server esterno, lo registra
        String id = getSharedPreferences("id_facebook", Context.MODE_PRIVATE).getString("id_facebook", null);
        String name = getSharedPreferences("name_facebook", Context.MODE_PRIVATE).getString("name_facebook", null);

        if (id == null || name == null) {
            Log.d("HUSTLE", "Non sei loggato su FB, quindi non puoi essere loggato sul server");
            logged = false;
            SharedPreferences o = getSharedPreferences("logged", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = o.edit();
            editor.putBoolean("logged", logged);
            editor.commit();
        }

        logged = getSharedPreferences("logged", Context.MODE_PRIVATE).getBoolean("logged", false);
        if (!logged) {
            if (id != null) {
                Log.d("HUSTLE", "Non sei registrato sul server, registrazione in corso per: id " + id + " e nome: " + name);
                logIn(id, name);
            }
        } else {
            Log.d("HUSTLE", "Sei già loggato sul server, non effettuo registrazione");
        }
    }

    public void logIn(String id, String name) {
        // AsyncTask per effettuare logIn sul server
        AsyncTask<String, Void, String> at = new AsyncTask<String, Void, String>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... params) {
                String id = params[0];
                String name = params[1];

                URL url = null;
                String s = null;
                String auth = MD5.hash(id+name);
                try {
                    String data = "id="+id+"&name="+name+"&auth="+auth;
                    Log.d("HUSTLE", "Invio richiesta di registrazione: "+data);
                    url = new URL("http://hustle.altervista.org/signup.php");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setDoOutput(true);
                    con.getOutputStream().write(data.getBytes("UTF-8"));

                    InputStream in = new BufferedInputStream(con.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    s = br.readLine();
                    Log.d("HUSTLE", "Ecco che ha risposto il server mentre mi loggo: " + s);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("HUSTLE", "returned: " + s);
                return s;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                try {
                    JSONObject jo = new JSONObject(s);
                    if (jo.getBoolean("logged")) {
                        Log.d("HUSTLE", "Ti sei loggato sul server");
                        logged = true;
                    } else {
                        Log.d("HUSTLE", "Non sei loggato sul server");
                        logged = false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                SharedPreferences o = getSharedPreferences("logged", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = o.edit();
                editor.putBoolean("logged", logged);
                editor.commit();
            }
        };

        at.execute(id, name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                updateCircleProfile();
                myDrawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.action_search:
                handleMenuSearch();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void handleMenuSearch(){
        ActionBar action = getSupportActionBar(); //get the actionbar

        if(isSearchOpened){ //test if the search is open

            action.setDisplayShowCustomEnabled(false); //disable a custom view inside the actionbar
            action.setDisplayShowTitleEnabled(true); //show the title in the action bar

            //hides the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edtSeach.getWindowToken(), 0);

            //add the search icon in the action bar
            SearchAction.setIcon(getResources().getDrawable(R.drawable.ic_search));

            isSearchOpened = false;
        } else { //open the search entry
            //
            action.setDisplayShowCustomEnabled(true); //enable it to display a
            // custom view in the action bar.
            action.setCustomView(R.layout.search_bar);//add the custom view
            action.setDisplayShowTitleEnabled(false); //hide the title

            edtSeach = (EditText)action.getCustomView().findViewById(R.id.edtSearch); //the text editor

            //this is a listener to do a search when the user clicks on search button
            edtSeach.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String searchingTitle = v.getText().toString();
                        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
                        Bundle b = new Bundle();
                        b.putString("SearchTitle", searchingTitle);
                        i.putExtras(b);
                        startActivity(i);
                        return true;
                    }
                    return false;
                }
            });

            edtSeach.requestFocus();

            //open the keyboard focused in the edtSearch
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edtSeach, InputMethodManager.SHOW_IMPLICIT);

            //add the close icon
            SearchAction.setIcon(getResources().getDrawable(R.drawable.ic_close));
            isSearchOpened = true;

        }
    }

    //
    @Override
    public void onBackPressed() {
        if(isSearchOpened) {
            handleMenuSearch();
            return;
        }
        super.onBackPressed();
    }

    // for serching button (on Toolbar)
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SearchAction = menu.findItem(R.id.action_search);
        return super.onPrepareOptionsMenu(menu);
    }

    //sottoclasse per l'adapter per i fragment e i titoli (delle varie tab)
    class TvShowAdapter extends FragmentStatePagerAdapter {
        public static final int NUMBER_OF_TABS = 3;

        public TvShowAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return TvShowFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return NUMBER_OF_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return getResources().getString(R.string.tab_myshow);
                // break;
                case 1:
                    return getResources().getString(R.string.tab_friends);
                // break;
                case 2:
                    return getResources().getString(R.string.tab_mostviewed);
                // break;
            }
            return "";
        }
    }


    // sottoclasse per gestire i fragment della pagina inziale
    public static class TvShowFragment extends Fragment {

        private static final String TAB_POSITION = "tab_position";

        // Crea un array di gridAdapter
        GridAdapter[] gridAdapter = new GridAdapter[TvShowAdapter.NUMBER_OF_TABS];
        RecyclerView recyclerView;

        // Stringa con il jsonArray delle mie serie
        private static String my_series = null;
        private JSONArray jsonArraySeries = null;

        public TvShowFragment() {
        }

        @Override
        public void onResume() {
            super.onResume();
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);

            if (tabPosition == 0) {
                Log.d("HUSTLE", "onResume fragment delle mie serie TV");
                if (CheckConnection.isConnected(getActivity())) {
                    downloadMySeries(gridAdapter[0], false);
                } else if (my_series != null)
                    showMySeries(my_series, gridAdapter[0]);
                else {
                    my_series = getActivity().getSharedPreferences("my_series", Context.MODE_PRIVATE).getString("my_series_json", null);
                    if (my_series != null)
                        showMySeries(my_series, gridAdapter[0]);
                }
            } else if (tabPosition == 1) {
                // TODO: fai resume delle serie degli amici
            }
        }

        public static TvShowFragment newInstance(int tabPosition) {
            TvShowFragment fragment = new TvShowFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_POSITION, tabPosition);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // Salva le serie viste
            Log.d("HUSTLE", "Saving state");
            outState.putString("my_series", my_series);
            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);
            Log.d("asg","tabPosition "+tabPosition);

            View v = inflater.inflate(R.layout.fragment_list_view, container, false);
            recyclerView = (RecyclerView) v.findViewById(R.id.recyclerview);
            recyclerView.setHasFixedSize(true);

            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity() , numOfElements);
            recyclerView.setLayoutManager(gridLayoutManager);

            if (tabPosition == 0){
                gridAdapter[tabPosition] = new GridAdapter(getActivity());
                recyclerView.setAdapter(gridAdapter[tabPosition]);
                // Se c'è uno stato salvato, usa quello
                if (savedInstanceState != null) {
                    my_series = savedInstanceState.getString("my_series");
                    Log.d("HUSTLE", "Ripristino mie serie da savedInstanceState");
                }
                // Se l'utente è connesso a internet scarica le sue serie TV
                if (CheckConnection.isConnected(getActivity())) {
                    Log.d("HUSTLE", "onCreate, connesso a internet, scarico le serie");
                    downloadMySeries(gridAdapter[tabPosition], true);
                } else {
                    // Altrimenti prende le mie serie TV dalle SharedPreferences e mostra quelle
                    // se my_series è diverso da null significa che ho già ripristinato lo stato
                    if (my_series == null) {
                        Log.d("HUSTLE", "Ripristino da shared preferences");
                        my_series = getActivity().getSharedPreferences("my_series", Context.MODE_PRIVATE).getString("my_series_json", null);
                    }
                    // Mostra le serie
                    if (my_series != null) {
                        showMySeries(my_series, gridAdapter[tabPosition]);
                    }
                }
            }
            if(tabPosition == 1){
                gridAdapter[tabPosition] = new GridAdapter(getActivity());
                recyclerView.setAdapter(gridAdapter[tabPosition]);
                SharedPreferences options = getActivity().getSharedPreferences("friend_list", Context.MODE_PRIVATE);
                String friend_list_json_string = options.getString("friend_list", null);
                Log.d("HUSTLE", "lista amici totale: " + friend_list_json_string);
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
                downloadFriendShows(return_list, gridAdapter[tabPosition]);

            }
            return v;
        }

        private void showMySeries(String new_series, GridAdapter gridAdapter) {
            try {
                jsonArraySeries = new JSONArray(new_series);
            } catch (JSONException e) {
                jsonArraySeries = null;
                e.printStackTrace();
            }
            if (jsonArraySeries == null)
                return;

            gridAdapter.reset();

            for(int i=0;i<jsonArraySeries.length();i++){
                try {
                    GridItem g = new GridItem();
                    JSONObject jo = jsonArraySeries.getJSONObject(i);
                    Show s = new Show(jo);

                    g.setShow(s);
                    g.setName(s.title);
                    g.setThumbnail(s.bmp);

                    gridAdapter.mItems.add(g);
                    gridAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            TvShowFragment.my_series = new_series;
            //Log.d("HUSTLE", "series: " + TvShowFragment.my_series);
        }

        public void downloadMySeries(final GridAdapter gridAdapter, final boolean oncreate) {
            // Prende id di Facebook
            final String id = getActivity().getSharedPreferences("id_facebook", Context.MODE_PRIVATE).getString("id_facebook", null);
            // Definisce un AsyncTask che scaricherà le serie viste dall'utente
            AsyncTask<String,Void,String> at = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    URL url = null;
                    String s = null;
                    try {
                        Uri builtUri = Uri.parse("http://hustle.altervista.org/getSeries.php?").
                                buildUpon().
                                appendQueryParameter("user_id", id).
                                build();
                        String u = builtUri.toString();
                        Log.d("HUSTLE", "requesting: " + u);
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
                    Log.d("HUSTLE", "returned: " + s);
                    // Salva le mie serie TV nelle SharedPreferences
                    Context c = getActivity();
                    if (c == null)
                        return s;
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("my_series", Context.MODE_PRIVATE).edit();
                    editor.putString("my_series_json", s);
                    editor.commit();

                    return s;
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    showMySeries(s, gridAdapter);
                }
            };
            // Se l'id è diverso da null e l'utente è connesso a internet, esegue l'AsyncTask
            if (id != null && CheckConnection.isConnected(getActivity())) {
                at.execute(id);
            }
        }

        public void downloadFriendShows(final ArrayList<Friend> all_friends, final GridAdapter gridAdapter){
        //async task, prende come parametro la lista amici totale

            AsyncTask<ArrayList<Friend>, Void, ArrayList<GridItem>> friend_shows_download = new AsyncTask<ArrayList<Friend>, Void, ArrayList<GridItem>>() {
                @Override
                protected ArrayList<GridItem> doInBackground(ArrayList<Friend> ...params) {
                    String s=null;
                    JSONArray friendshowsJSON = null;

                    ArrayList<Friend> all_friends = (ArrayList<Friend>) params[0];
                    Friend actual = null;
                    String user_id = null;
                    ArrayList<GridItem> listItem = new ArrayList<GridItem>();
                    for (int i= 0; i < all_friends.size(); i++){
                        actual = all_friends.get(i);
                        user_id = actual.id;
                        actual.shows.clear();
                        SharedPreferences options = getActivity().getSharedPreferences(user_id, Context.MODE_PRIVATE);
                        s = options.getString(user_id, null);
                        //lettura risposta
                        if(s==null){
                            try {
                                URL url = new URL("http://hustle.altervista.org/getSeries.php?user_id=" + user_id);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                InputStream in = new BufferedInputStream(conn.getInputStream());
                                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                                s = br.readLine();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            options = getActivity().getSharedPreferences(user_id, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = options.edit();
                            editor.putString(user_id, s);
                            editor.commit();

                        }
                        if(s!=null) {
                            try {

                                friendshowsJSON = new JSONArray(s);
                                if (friendshowsJSON.length() >= 1) {
                                    //per ogni amico legge ogni show

                                    for (int j = 0; j < friendshowsJSON.length(); j++) {
                                        Show friend_show;
                                        GridItem item = new GridItem();
                                        //crea oggetto show
                                        friend_show = new Show(friendshowsJSON.getJSONObject(j));
                                        //aggiunge alla lista personale dell'amico
                                        actual.shows.add(friend_show);

                                        //se è gia nella lista icrementa il contatore, altrimenti lo aggiunge
                                        item.setShow(friend_show);
                                        item.setName(friend_show.title);
                                        item.setThumbnail(friend_show.bmp);

                                        if (listItem.contains(item)) {
                                            Log.d("HUSTLELOG", "incremento  " +item.getName());
                                            listItem.get(listItem.indexOf(item)).addFriend();
                                        } else {
                                            item.addFriend();
                                            listItem.add(item);
                                            Log.d("HUSTLELOG", "aggiungo  " + item.getName());
                                        }
                                    }
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }


                    }

                    Collections.sort(listItem);
                    return listItem;
                }
                @Override
                protected void onPostExecute(ArrayList<GridItem> gridList) {
                    super.onPostExecute(gridList);
                    for (GridItem it : gridList) {
                        gridAdapter.mItems.add(it);
                        Log.d("HUSTLELOG", "tot  " + it.getName());
                    }
                    gridAdapter.notifyDataSetChanged();
                }
            };

            friend_shows_download.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, all_friends);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CheckConnection.isConnected(this)) {
            updateCircleProfile();
            updateLogInServer();
            updateFriendList();
        }
    }

    void updateFriendList(){
        SharedPreferences options = getSharedPreferences("id_facebook", Context.MODE_PRIVATE);
        String id = options.getString("id_facebook", null);
        if(id != null){
            new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "/me/friends",
                    null,
                    HttpMethod.GET,
                    new GraphRequest.Callback() {
                        public void onCompleted(GraphResponse response) {
                            try {
                                Log.d("HUSTLE", "amici: " + response.getJSONObject().getJSONArray("data").toString());
                                SharedPreferences options = getSharedPreferences("friend_list", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = options.edit();
                                editor.putString("friend_list", response.getJSONObject().getJSONArray("data").toString());
                                editor.commit();
                                //get friend list
                                JSONArray friend_list = response.getJSONObject().getJSONArray("data");
                                for (int i = 0; i < friend_list.length(); i++) {
                                    String id = friend_list.getJSONObject(i).getString("id");
                                }
                                friendsList = new ArrayList<Friend>();
                                //fill friend list
                                try {
                                    for (int i = 0; i < friend_list.length(); i++) {
                                        friendsList.add(new Friend(friend_list.getJSONObject(i)));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    }
            ).executeAsync();
        }
    }




    void downloadFriendPhotos(final String id){
        //non viene usata questa funzione, sdk di facebook mette a disposizione il widget che fa gia tutto

        AsyncTask<String, Void, Bitmap> profile_photo_downloader = new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {

                String id = params[0];
                //richiesta dati episodi della stagione
                try {
                    URL url = new URL("");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //creazione array dalla risposta
                try {
                    new JSONArray().get(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
                @Override
                protected void onPostExecute(Bitmap bmp) {
                    super.onPostExecute(bmp);

                }
        };

        profile_photo_downloader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
        }


    void updateCircleProfile() {
        SharedPreferences options = getSharedPreferences("id_facebook", Context.MODE_PRIVATE);
        String id = options.getString("id_facebook", null);

        String fbId = profilePictureInvisible.getProfileId();
        Log.d("HUSTLE", "Sto per aggiornare facebook, profileID: " + profilePictureInvisible.getProfileId() + ", id delle preferenze: " + id);
        profilePictureInvisible.setProfileId(id);

        Log.d("HUSTLE", "Updating circle profile");
        //ImageView profileImageView = ((ImageView)profilePictureInvisible.getChildAt(0));
        Bitmap bitmap  = ((BitmapDrawable)((ImageView)profilePictureInvisible.getChildAt(0)).getDrawable()).getBitmap();
        circleImageView.setImageBitmap(bitmap);

        //aggiornamento textview nome
        options = getSharedPreferences("name_facebook", Context.MODE_PRIVATE);
        String name = options.getString("name_facebook", null);

        if (name != null){
            account_name_facebook_tv.setText(name);
            account_name_facebook_tv.invalidate();
        }
        else{
            account_name_facebook_tv.setText(getResources().getString(R.string.account_default_name));
            account_name_facebook_tv.invalidate();
        }
    }

    public int getDisplayDimensions(Display d){
        Point size = new Point();
        d.getSize(size);
        int widthPX = size.x;
        int widthDP = pxToDp(widthPX);


        int wPX = (int) getResources().getDimension(R.dimen.grid_item_RelativeLayout_width);
        int wDP = pxToDp(wPX);
        int num = (int) Math.floor(widthPX/wPX);

        return num;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        int dp = (int) ((px/displayMetrics.density)+0.5);
        return dp;
    }

}
