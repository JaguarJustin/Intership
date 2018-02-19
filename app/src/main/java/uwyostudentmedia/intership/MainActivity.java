package uwyostudentmedia.intership;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    WebView myWebView;

    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeLayout;
    //private TextView mFeedTitleTextView;
   // private TextView mFeedLinkTextView;
    private TextView header;
    private List<RssFeedModel> mFeedModelList;
    //private String mFeedTitle;
    //private String mFeedLink;
    private ViewFlipper vf;
    private int screen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        screen = 0;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //header = (TextView) findViewById(R.id.header);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        //mFeedTitleTextView = (TextView) findViewById(R.id.feedTitle);
        //mFeedLinkTextView = (TextView) findViewById(R.id.feedLink);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        new FetchFeedTask().execute((Void) null);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new FetchFeedTask().execute((Void) null);
            }
        });

        vf = (ViewFlipper) findViewById(R.id.viewFlipper);
        vf.showNext();
    }

    public  void perform_action(View v)
    {
        TextView myTextView = (TextView) v;

        String link = myTextView.getText().toString();

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

            return items;
        } finally {
            inputStream.close();
        }
    }


    private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

        private String urlLink;

        @Override
        protected void onPreExecute() {
            mSwipeLayout.setRefreshing(true);
            //mFeedTitle = null;
            //mFeedLink = null;
            //mFeedDescription = null;
            //mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
            //mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
            //mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
            //urlLink = mEditText.getText().toString();
            urlLink = "uwbrandingiron.com/feed";
        }

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
            mSwipeLayout.setRefreshing(false);

            if (success) {
                //mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
                //mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
                //mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
                // Fill RecyclerView
                mRecyclerView.setAdapter(new RssFeedListAdapter(mFeedModelList));
            } else {
                Toast.makeText(MainActivity.this,
                        "Enter a valid Rss feed url",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

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
                //myWebView.setWebViewClient(new WebViewClient());
                //myWebView.stopLoading();
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwbrandingiron.com");
                //myWebView.reload();
                vf.showNext();
                screen = 1;

            }
            else{
                //myWebView.setWebViewClient(new WebViewClient());
                //myWebView.stopLoading();
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwbrandingiron.com");
            }
        } else if (id == R.id.nav_design) {
            if (screen ==0) {
                //myWebView.setWebViewClient(new WebViewClient());
                // myWebView.stopLoading();
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwdynamicdesign.com");
                //myWebView.reload();
                vf.showNext();
                screen = 1;
            }
            else{
                //myWebView.setWebViewClient(new WebViewClient());
                // myWebView.stopLoading();
                myWebView.loadUrl("about:blank");
                myWebView.loadUrl("http://www.uwdynamicdesign.com");
                //myWebView.reload();
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
/*
private class MyWebViewClient extends WebViewClient {

    @Override
    public boolean should
}
*/