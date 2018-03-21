package uwyostudentmedia.intership;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.os.Looper;
import android.os.Handler;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    WebView myWebView;

    private static final String TAG = "MainActivity";


    private List<RssFeedModel> mFeedModelList;
    private ViewFlipper vf;
    private int screen;
    private ExpandableListAdapter listAdapter;
    private ExpandableListView expandableListView;
    private HashMap<String, List<RssFeedModel>> listDataChild =  new HashMap<String, List<RssFeedModel>>();
    private List<String> listDataHeader= new ArrayList<String>();
    private Context myContext;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //For helping me control the ViewFlipper with the webview.
        screen = 0;

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        myContext = getApplicationContext();

        myWebView = findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = myWebView.getSettings();

        //Don't set if you don't need it.
        webSettings.setJavaScriptEnabled(true);


        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(myContext.getCacheDir().getPath());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // get the listview
        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);



        new FetchFeedTask().execute((Void) null);

        vf = findViewById(R.id.viewFlipper);
        vf.showNext();

    }

    /*
	 * Preparing the list data
	 */
    private void prepareListData() {

        listDataHeader.add("Branding Iron\n Online");
        listDataHeader.add("Podcasts");
        listDataHeader.add("OneTV");
        listDataHeader.add("Frontiers");

        List<RssFeedModel> temp = new ArrayList<>();
        for(int i=0; i<10; i++){

            if(i==0){
                RssFeedModel item = new RssFeedModel("Coming Soon", "");
                temp.add(item);
            }
            else{
                RssFeedModel item = new RssFeedModel("", "");
                temp.add(item);
            }

        }

        listDataChild.put(listDataHeader.get(0), mFeedModelList); // Header, Child data
        listDataChild.put(listDataHeader.get(1), temp);
        listDataChild.put(listDataHeader.get(2), temp);
        listDataChild.put(listDataHeader.get(3), temp);

    }

    //Used to get the data link in the rss feed and to go to the specified article
    public void perform_action(View v)
    {
        TextView myTextView = (TextView) v;

        String link = myTextView.getText().toString();

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE,"link");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT,bundle);

        if(link == null)
        {
            Toast.makeText(MainActivity.this, "No link",  Toast.LENGTH_LONG).show();
        }
        else {
            //Toast.makeText(MainActivity.this, link,  Toast.LENGTH_LONG).show();

            vf.showNext();
            screen = 1;
            myWebView.loadUrl("about:blank");
            myWebView.loadUrl(link);
        }

    }



    //Takes the XML feed and parses its data.
    public List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException, IOException {
        String title = null;
        String link = null;
        String description = null;
        boolean isItem = false;
        List<RssFeedModel> items = new ArrayList<>();

        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);

            xmlPullParser.nextTag();
            while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = xmlPullParser.getEventType();

                String name = xmlPullParser.getName();
                if(name == null)
                    continue;

                if(eventType == XmlPullParser.END_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = false;
                    }
                    continue;
                }

                if (eventType == XmlPullParser.START_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = true;
                        continue;
                    }
                }

                Log.d("MainActivity", "Parsing name ==> " + name);
                String result = "";
                if (xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

                if (name.equalsIgnoreCase("title")) {
                    title = result;
                } else if (name.equalsIgnoreCase("link")) {
                    link = result;
                } else if (name.equalsIgnoreCase("description")) {
                    description = result;
                }

                if (title != null && link != null && description != null) {
                    if(isItem) {
                        RssFeedModel item = new RssFeedModel(title,link);
                        items.add(item);

                    }
                    else {
                        //mFeedTitle = title;
                        //mFeedLink = link;
                        //mFeedDescription = description;
                    }

                    title = null;
                    link = null;
                    description = null;
                    isItem = false;
                }
            }


        }catch (IOException e){throw e;}

        return items;
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

        private String urlLink;

        //Links to the website for the rss feed.
        @Override
        protected void onPreExecute() {
            //mSwipeLayout.setRefreshing(true);
            urlLink = "uwbrandingiron.com/feed";
        }

        //Keeps connection going in the background.
        @Override
        protected Boolean doInBackground(Void... voids) {
            if (TextUtils.isEmpty(urlLink))
                return false;

            try {
                if(!urlLink.startsWith("http://") && !urlLink.startsWith("https://"))
                    urlLink = "http://" + urlLink;

                URL url = new URL(urlLink);
                InputStream inputStream = url.openConnection().getInputStream();

                    mFeedModelList = parseFeed(inputStream);

                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error", e);

            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            //mSwipeLayout.setRefreshing(false);

            if (success) {
                // Fill RecyclerView

                prepareListData();
                listAdapter = new ExpandableListAdapter(myContext, listDataHeader, listDataChild);
                expandableListView.setAdapter(listAdapter);
            } else {
                Toast.makeText(MainActivity.this,
                        "Enter a valid Rss feed url",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            if(screen == 1) {
                vf.showNext();
                myWebView.loadUrl("about:blank");
                screen = 0;
            }
        }
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    */

    //Controls navigation of the navigation drawer
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            if(screen == 1) {
                vf.showNext();
                myWebView.loadUrl("about:blank");
                screen = 0;
            }
        } else if (id == R.id.nav_branding) {
            // Handle the camera action
            if(screen == 0) {
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwbrandingiron.com");
                vf.showNext();
                screen = 1;

            }
            else{
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwbrandingiron.com");
            }
        } else if (id == R.id.nav_design) {
            if (screen ==0) {
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwdynamicdesign.com");
                vf.showNext();
                screen = 1;
            }
            else{
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwdynamicdesign.com");
            }
        } else if (id == R.id.nav_media) {
            if (screen==0) {
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwdynamicmedia.com");
                vf.showNext();
                screen = 1;
            }
            else{
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwdynamicmedia.com");
            }
        } else if (id == R.id.nav_laramie) {
            if (screen==0) {
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.laramieliving.com");
                vf.showNext();
                screen = 1;
            }
            else{
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.laramieliving.com");
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
