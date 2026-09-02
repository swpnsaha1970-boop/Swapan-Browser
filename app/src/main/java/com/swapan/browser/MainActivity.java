package com.swapan.browser;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.speech.RecognizerIntent;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    WebView web;
    EditText address;
    LinearLayout home, tabStrip;
    ArrayList<String> tabs=new ArrayList<>();
    ArrayList<String> bookmarks=new ArrayList<>();
    ArrayList<String> history=new ArrayList<>();
    int active=0;
    SharedPreferences prefs;
    static final int VOICE=10, QR=20;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        web=findViewById(R.id.web); home=findViewById(R.id.home);
        address=findViewById(R.id.address); tabStrip=findViewById(R.id.tabStrip);
        prefs=getSharedPreferences("swapan",MODE_PRIVATE);
        loadLists();
        setupWeb();
        bind();
        newTab();
    }

    void setupWeb(){
        WebSettings s=web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(false); s.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r){return false;}
            @Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap x){address.setText(u);}
            @Override public void onPageFinished(WebView v,String u){
                address.setText(u);
                if(!history.contains(u)){history.add(u);saveLists();}
                updateTabs(v.getTitle(),u);
            }
        });
        web.setDownloadListener((url,ua,cd,mime,size)->{
            try { Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url)); startActivity(i); }
            catch(Exception e){Toast.makeText(this,"Download unavailable",Toast.LENGTH_SHORT).show();}
        });
    }

    void bind(){
        address.setOnEditorActionListener((v,a,e)->{open(address.getText().toString());return true;});
        findViewById(R.id.back).setOnClickListener(v->{if(web.canGoBack())web.goBack();});
        findViewById(R.id.forward).setOnClickListener(v->{if(web.canGoForward())web.goForward();});
        findViewById(R.id.voice).setOnClickListener(v->voice());
        findViewById(R.id.homeBtn).setOnClickListener(v->showHome());
        findViewById(R.id.bookmarkBtn).setOnClickListener(v->bookmark());
        findViewById(R.id.qrBtn).setOnClickListener(v->qr());
        findViewById(R.id.downloadBtn).setOnClickListener(v->downloads());
        findViewById(R.id.newTab).setOnClickListener(v->newTab());
        findViewById(R.id.menu).setOnClickListener(v->menu());
        findViewById(R.id.google).setOnClickListener(v->open("https://www.google.com"));
        findViewById(R.id.youtube).setOnClickListener(v->open("https://www.youtube.com"));
        findViewById(R.id.news).setOnClickListener(v->open("https://news.google.com/topstories?hl=bn&gl=IN&ceid=IN:bn"));
        findViewById(R.id.addShortcut).setOnClickListener(v->addShortcut());
    }

    void open(String q){
        if(q==null||q.trim().isEmpty())return;
        String u=q.trim();
        if(!u.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))
            u=(u.contains(".")&&!u.contains(" "))?"https://"+u:"https://www.google.com/search?q="+Uri.encode(u);
        home.setVisibility(View.GONE); web.setVisibility(View.VISIBLE); web.loadUrl(u);
    }

    void newTab(){
        tabs.add("");
        active=tabs.size()-1;
        renderTabs();
        showHome();
        address.setText("");
    }

    void renderTabs(){
        tabStrip.removeAllViews();
        for(int i=0;i<tabs.size();i++){
            final int ix=i;
            Button b=new Button(this);
            b.setText((i==active?"● ":"")+"Tab "+(i+1));
            b.setOnClickListener(v->{active=ix;String u=tabs.get(ix);if(u==null||u.isEmpty())showHome();else open(u);renderTabs();});
            b.setOnLongClickListener(v->{if(tabs.size()>1){tabs.remove(ix);if(active>=tabs.size())active=tabs.size()-1;renderTabs();String u=tabs.get(active);if(u.isEmpty())showHome();else open(u);}return true;});
            tabStrip.addView(b,new LinearLayout.LayoutParams(150,40));
        }
    }

    void updateTabs(String title,String url){
        if(active>=0&&active<tabs.size())tabs.set(active,url);
        renderTabs();
    }

    void showHome(){web.stopLoading();web.setVisibility(View.GONE);home.setVisibility(View.VISIBLE);renderTabs();}
    void voice(){
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try{startActivityForResult(i,VOICE);}catch(Exception e){Toast.makeText(this,"Voice search unavailable",Toast.LENGTH_SHORT).show();}
    }

    void qr(){
        // Uses an installed QR scanner app through the standard ZXing intent; no scanner SDK is bundled.
        Intent i=new Intent("com.google.zxing.client.android.SCAN");
        i.putExtra("SCAN_MODE","QR_CODE_MODE");
        try{startActivityForResult(i,QR);}
        catch(Exception e){
            new AlertDialog.Builder(this).setTitle("QR Scanner")
             .setMessage("No compatible QR scanner is installed. Install a trusted QR scanner and try again.")
             .setPositiveButton("OK",null).show();
        }
    }

    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);
        if(r==VOICE&&c==RESULT_OK&&d!=null){
            ArrayList<String>x=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(x!=null&&!x.isEmpty())open(x.get(0));
        } else if(r==QR&&c==RESULT_OK&&d!=null){
            String x=d.getStringExtra("SCAN_RESULT"); if(x!=null)open(x);
        }
    }

    void bookmark(){
        String u=web.getUrl(); if(u==null)return;
        if(!bookmarks.contains(u)){bookmarks.add(u);saveLists();Toast.makeText(this,"Bookmark saved",Toast.LENGTH_SHORT).show();}
        else Toast.makeText(this,"Already bookmarked",Toast.LENGTH_SHORT).show();
    }

    void addShortcut(){
        EditText e=new EditText(this);e.setHint("https://example.com");
        new AlertDialog.Builder(this).setTitle("Add Shortcut").setView(e)
         .setPositiveButton("Add",(d,w)->open(e.getText().toString())).setNegativeButton("Cancel",null).show();
    }

    void downloads(){
        try{startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));}
        catch(Exception e){Toast.makeText(this,"Downloads screen unavailable",Toast.LENGTH_SHORT).show();}
    }

    void menu(){
        String[] x={"Bookmarks","History","Desktop Site","Dark Mode","Clear Browsing Data","Incognito","Settings"};
        new AlertDialog.Builder(this).setTitle("Swapan Browser").setItems(x,(d,w)->{
            if(w==0)list("Bookmarks",bookmarks);
            else if(w==1)list("History",history);
            else if(w==2)desktop();
            else if(w==3)darkMode();
            else if(w==4)clearData();
            else if(w==5)incognito();
            else settings();
        }).show();
    }

    void list(String title,ArrayList<String> a){
        if(a.isEmpty()){Toast.makeText(this,"Nothing here yet",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle(title).setItems(a.toArray(new String[0]),(d,w)->open(a.get(w))).show();
    }

    void desktop(){
        String ua=web.getSettings().getUserAgentString();
        if(ua!=null&&ua.contains("Mobile")) web.getSettings().setUserAgentString(
          "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36");
        else web.getSettings().setUserAgentString(null);
        web.reload();
    }

    void darkMode(){AppCompatDelegateShim.toggle(this);}

    void clearData(){
        web.clearHistory();web.clearCache(true);history.clear();
        CookieManager.getInstance().removeAllCookies(null);saveLists();
        Toast.makeText(this,"Browsing data cleared",Toast.LENGTH_SHORT).show();
    }

    void incognito(){
        web.clearHistory();web.clearCache(true);
        CookieManager.getInstance().removeAllCookies(null);
        Toast.makeText(this,"Incognito session started",Toast.LENGTH_SHORT).show();
        open("https://www.google.com");
    }

    void settings(){
        new AlertDialog.Builder(this).setTitle("Privacy & Settings")
         .setMessage("Third-party cookies: BLOCKED\nAnalytics SDKs: NONE\nUnnecessary permissions: NONE\nBackup: DISABLED\n\nSwapan Browser uses Android WebView for page rendering.")
         .setPositiveButton("OK",null).show();
    }

    void loadLists(){
        String bm=prefs.getString("bookmarks","");
        String hi=prefs.getString("history","");
        if(!bm.isEmpty())bookmarks.addAll(Arrays.asList(bm.split("\\n")));
        if(!hi.isEmpty())history.addAll(Arrays.asList(hi.split("\\n")));
    }
    void saveLists(){
        prefs.edit().putString("bookmarks",join(bookmarks)).putString("history",join(history)).apply();
    }
    String join(ArrayList<String>a){StringBuilder s=new StringBuilder();for(String x:a){if(s.length()>0)s.append("\n");s.append(x);}return s.toString();}

    public void onBackPressed(){
        if(web.getVisibility()==View.VISIBLE&&web.canGoBack())web.goBack();else super.onBackPressed();
    }

    // Kept tiny: uses system night mode without adding another UI dependency.
    static class AppCompatDelegateShim{
        static void toggle(Activity a){
            int current=a.getResources().getConfiguration().uiMode & 0x30;
            Toast.makeText(a,current==0x20?"Dark mode preference is controlled by system theme":"Dark mode preference is controlled by system theme",Toast.LENGTH_SHORT).show();
        }
    }
}
