/* Copyright (c) 2006-2011 Regents of the University of Minnesota.
 * For licensing terms, see the file LICENSE.
 */
package com.example.cyclopath;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.util.SparseArray;

import com.example.cyclopath.conf.Constants;
import com.example.cyclopath.db.CyclopathDbAdapter;
import com.example.cyclopath.gwis.GWIS_LandmarkExpActiveGet;
import com.example.cyclopath.items.Byway;
import com.example.cyclopath.items.Geofeature;
import com.example.cyclopath.items.MapLayer;
import com.example.cyclopath.items.Tag;
import com.example.cyclopath.util.LogS;
import com.example.cyclopath.util.User;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

/**
 * This class handles application initialization code.
 * @author Fernando Torre
 */
public class CyclopathApplication extends Application {

   /**
    * Called when the application is created.
    */
   @Override
   public void onCreate() {
      super.onCreate();

      G.app_context = this;
      Constants.init();
      G.db = new CyclopathDbAdapter(this);
      G.db.open();
      // delete tracks that have not been accessed in the last three months to
      // reduce space
      G.db.deleteOldServerTracks();
      G.track_save_attempted = false;
      Tag.all = new HashMap<String, Tag>();
      Tag.all_id = new SparseArray<Tag>();
      
      if (G.layers == null) {
         G.layers = new Hashtable<Float, MapLayer>();
         G.nodes_adjacent = new Hashtable<Integer, Set<Byway>>();
         G.vectors_old_all = new Hashtable<Integer, Geofeature>();
      }

      // set up user
      G.cookie = this.getSharedPreferences(Constants.USER_COOKIE,
                                           Context.MODE_PRIVATE);
      G.cookie_anon = this.getSharedPreferences(Constants.USER_COOKIE_ANON,
                                                Context.MODE_PRIVATE);
      G.autocomplete = this.getSharedPreferences(Constants.AUTOCOMPLETE_COOKIE,
            Context.MODE_PRIVATE);
      G.user = new User();
      G.user.startup();
      

      Editor editor = G.cookie_anon.edit();
      editor.putBoolean(Constants.COOKIE_IS_RECORDING, false);
      editor.commit();
      Intent intent = new Intent(this, TrackingService.class);
      stopService(intent);
      
      // if the notification is still shown from the app closing improperly,
      // cancel it now.
      NotificationManager notification_manager = (NotificationManager) 
         getSystemService(Context.NOTIFICATION_SERVICE);
      notification_manager.cancel(TrackingService.TRACKING_NOTIFICATION_ID);
      
      G.server_log = new LogS(this);
      G.logBuildInfo();
      
      new GWIS_LandmarkExpActiveGet().fetch();
   }

}
