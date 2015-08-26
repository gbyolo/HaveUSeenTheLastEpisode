package it.asg.hustle;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private String LOG_TAG = "HUSTLE";
    private Toolbar toolbar;
    private DrawerLayout myDrawerLayout;    //imposta NavigationDrawer
    private FloatingActionButton fab;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "Initializint Facebook SDK");
        FacebookSdk.sdkInitialize(getApplicationContext());
        Log.d(LOG_TAG, "Opening DB");
        SQLiteDatabase db = openOrCreateDatabase("test.db", MODE_PRIVATE, null);
        Log.d(LOG_TAG, "Executing initial queries");
        db.execSQL(InitialQuery.create_series_table);
        db.execSQL(InitialQuery.create_episodes_table);
        Log.d(LOG_TAG, "Initial queries executed correctly");

        setContentView(R.layout.activity_main);

        // imposto ActionBar sulla Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
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
                // Toast.makeText(MainActivity.this, menuItem.getTitle(), Toast.LENGTH_LONG).show();
                if(menuItem.getTitle().equals(getResources().getString(R.string.nav_item_login))==true){
                    // accesso facebook
                    Intent intentactivityfacebook = new Intent(MainActivity.this, FacebookActivity.class);
                    startActivity(intentactivityfacebook);

                }
                return true;
            }
        });
        //click del FAB
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("asg", "FAB was pressed");
            }
        });

        // Crea un TvShowAdapter
        TvShowAdapter adapter = new TvShowAdapter(getSupportFragmentManager());
        // Prende il ViewPager e imposta come adapter il TvShowAdapter: in base alla tab
        // selezionata, mostra il fragment relativo
        ViewPager viewPager = (ViewPager)findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
        // Prende il TabLayout e imposta il ViewPager
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

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
                myDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // sottoclasse per gestire i fragment della pagina inziale
    public static class TvShowFragment extends Fragment {
        private static final String TAB_POSITION = "tab_position";

        public TvShowFragment() {

        }

        public static TvShowFragment newInstance(int tabPosition) {
            TvShowFragment fragment = new TvShowFragment();
            Bundle args = new Bundle();
            args.putInt(TAB_POSITION, tabPosition);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args = getArguments();
            int tabPosition = args.getInt(TAB_POSITION);
            /*Log.d("asg","tabPosition "+tabPosition); */
            ArrayList<String> items = new ArrayList<String>();
            for(int i=0 ; i < 50 ; i++){
                items.add("TV-Show "+i);
            }
            View v = inflater.inflate(R.layout.fragment_list_view, container, false);
            RecyclerView recyclerView = (RecyclerView)v.findViewById(R.id.recyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(new ShowRecyclerAdapter(items));

            /*switch (tabPosition){
                case 0:

                    break;
                case 1:
                    break;
                case 2:

                    break;
            }*/
            return v;
        }
    }

    //sottoclasse per l'adapter per i fragment e i titoli (delle varie tab)
    class TvShowAdapter extends FragmentStatePagerAdapter {
        private int number_of_tabs=3;

        public TvShowAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return TvShowFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return number_of_tabs;
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

    @Override
    protected void onResume() {
        super.onResume();
        //new RequestFriendsList(getApplicationContext()).execute();
        //new MySelfRequest(getApplicationContext()).execute();

    }
}
