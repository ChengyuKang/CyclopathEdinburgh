/* Copyright (c) 2006-2011 Regents of the University of Minnesota.
 * For licensing terms, see the file LICENSE.
 */

package com.example.cyclopath.gwis;

import com.example.cyclopath.items.Route;

/**
 * A callback interface for handling route requests.
 * @author Fernando Torre
 */
public interface GWIS_RouteGetCallback {

   /**
    * This method is called once a route request has been processed.
    */
   public void handleGWIS_RouteGetComplete(Route r);
   
}
