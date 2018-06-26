package com.ezerka.pingo.app;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.ezerka.pingo.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity
  implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

  private static final String TAG = "MapsActivity: ";
  private Toolbar mToolbar;
  private FloatingActionButton mFab;
  private DrawerLayout mDrawer;
  private ActionBarDrawerToggle mToggle;
  private NavigationView mNavigationView;
  private SupportMapFragment mSupportFragment;
  private PlaceAutocompleteFragment mPlaceFragment;

  private GoogleMap mGoogleMap;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    assignTheViews();
    assignTheLinks();
    assignTheMethods();

  }

  private void assignTheViews() {
    mToolbar = findViewById(R.id.toolbar);

    mFab = findViewById(R.id.fab);

    mDrawer = findViewById(R.id.drawer_layout);

    mNavigationView = findViewById(R.id.nav_view);

    mToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

    mSupportFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.supportMapFragment);

    mPlaceFragment = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
  }

  private void assignTheLinks() {
    mFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show();
      }
    });

  }

  private void assignTheMethods() {
    setupTheToolBar();
    setupTheMap();
    setupTheDrawer();
    setupThePlaceFragment();
  }

  private void setupTheToolBar() {
    setSupportActionBar(mToolbar);
  }

  private void setupTheMap() {
    mSupportFragment.getMapAsync(this);
  }

  private void setupTheDrawer() {
    mDrawer.addDrawerListener(mToggle);
    mToggle.syncState();

    mNavigationView.setNavigationItemSelectedListener(this);
  }


  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else{
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.maps, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      makeToast("Settings");
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    if (id == R.id.nav_camera) {
      makeToast("Camera");
      // Handle the camera action
    } else if (id == R.id.nav_gallery) {
      makeToast("Gallery");

    } else if (id == R.id.nav_slideshow) {
      makeToast("Slideshow");

    } else if (id == R.id.nav_manage) {
      makeToast("Manage");

    } else if (id == R.id.nav_share) {
      makeToast("Share");

    } else if (id == R.id.nav_send) {
      makeToast("Send");
    }

    mDrawer = findViewById(R.id.drawer_layout);
    mDrawer.closeDrawer(GravityCompat.START);
    return true;
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mGoogleMap = googleMap;

    LatLng Hyd = new LatLng(17.387140, 78.491684);

    mGoogleMap.addMarker(new MarkerOptions().position(Hyd).title("Hyderabad"));
    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(Hyd));
  }


  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {

  }

  private void setupThePlaceFragment() {
    mPlaceFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
      @Override
      public void onPlaceSelected(Place place) {
        mGoogleMap.clear();
        mGoogleMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 12.0f));

      }

      @Override
      public void onError(Status status) {
        Log.d(TAG, "Something went wrong " + status.getStatusMessage());
      }
    });
  }

  private void makeToast(String input) {
    Toast.makeText(this, input + " Clicked ", Toast.LENGTH_SHORT).show();
  }

}
