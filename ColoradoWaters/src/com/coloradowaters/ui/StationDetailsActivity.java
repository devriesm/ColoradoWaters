package com.coloradowaters.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.coloradowaters.R;
import com.coloradowaters.model.ColoradoWaterSMS;
import com.coloradowaters.model.Station;
import com.coloradowaters.ui.DetailsActionBarAdapter.TabState;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MarkerOptionsCreator;


public class StationDetailsActivity extends BaseActivity implements DetailsActionBarAdapter.Listener
{
	private DetailsActionBarAdapter mActionBarAdapter;
	
	private Station mStation;
	
	private ViewPager mTabPager;
	private TabPagerAdapter mTabPagerAdapter;
	private final TabPagerListener mTabPagerListener = new TabPagerListener();
	
	private StationConditionsFragment mConditionsFragment;
	private StationChartFragment mChartFragment;
	private StationWeatherFragment mWeatherFragment;
	private StationMapFragment mMapFragment;
	
	
	private boolean mFragmentInitialized;
	
	
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		getActionBar().setDisplayHomeAsUpEnabled( true );
		
		final String customTitle = getIntent().getStringExtra( Intent.EXTRA_TITLE );
		setTitle( customTitle != null ? customTitle : getTitle() );
		
		mStation = getIntent().getExtras().getParcelable( ColoradoWaterSMS.EXTRA_STATION );
		
		initFragments( savedInstanceState );
	}
	
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		mActionBarAdapter.setListener( this );
		
		if( mTabPager != null )
			mTabPager.setOnPageChangeListener( mTabPagerListener );
	}
	
	
	@Override
	protected void onStart()
	{
		if( !mFragmentInitialized )
		{
			mFragmentInitialized = true;
			configureFragments();
		}
		super.onStart();
	}
	
	
	@Override
	protected void onDestroy()
	{
		if( mActionBarAdapter != null )
			mActionBarAdapter.setListener( null );
		
		super.onDestroy();
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mActionBarAdapter.onSaveInstanceState(outState);
		
		// Clear the listener to make sure we don't get callbacks after onSaveInstanceState,
		// in order to avoid doing fragment transactions after it.
		// TODO Figure out a better way to deal with the issue.
		mActionBarAdapter.setListener( null );
		if( mTabPager != null )
			mTabPager.setOnPageChangeListener( null );
	}
	
	
	@Override
    protected void onRestoreInstanceState( Bundle savedInstanceState )
	{
		super.onRestoreInstanceState( savedInstanceState );
    }
	
	
	@Override
	protected int getContentView()
	{
		return R.layout.activity_station_details;
	}
	
	
	private void initFragments( Bundle savedState )
	{
		final FragmentManager fragmentManager = getFragmentManager();
		
		final FragmentTransaction transaction = fragmentManager.beginTransaction();
		
		mTabPager = (ViewPager) findViewById( R.id.tab_pager );
		mTabPagerAdapter = new TabPagerAdapter();
		mTabPager.setAdapter(mTabPagerAdapter);
		mTabPager.setOnPageChangeListener( mTabPagerListener );
		
		final String CONDITIONS_TAG = "tab-pager-conditions";
		final String CHART_TAG = "tab-pager-chart";
		final String MAP_TAG = "tab-pager-map";
		final String WEATHER_TAG = "tab-pager-weather";
		
		mConditionsFragment = (StationConditionsFragment) fragmentManager.findFragmentByTag( CONDITIONS_TAG );
		mChartFragment = (StationChartFragment) fragmentManager.findFragmentByTag( CHART_TAG );
		mMapFragment = (StationMapFragment) fragmentManager.findFragmentByTag( MAP_TAG );
		mWeatherFragment = (StationWeatherFragment) fragmentManager.findFragmentByTag( WEATHER_TAG );
		
		if( mConditionsFragment == null )
		{
			GoogleMapOptions options = new GoogleMapOptions();
			options.camera( new CameraPosition( mStation.latLng, 16, 0, 0 ) );
			options.mapType( GoogleMap.MAP_TYPE_TERRAIN );
			
			mConditionsFragment = new StationConditionsFragment();
			mChartFragment = new StationChartFragment();
			mMapFragment = StationMapFragment.newInstance( options );
			mWeatherFragment = new StationWeatherFragment();
			
			mConditionsFragment.setArguments( intentToFragmentArguments( getIntent() ) );
			mChartFragment.setArguments( intentToFragmentArguments( getIntent() ) );
			
			transaction.add( R.id.tab_pager, mConditionsFragment, CONDITIONS_TAG );
			transaction.add( R.id.tab_pager, mChartFragment, CHART_TAG );
			transaction.add( R.id.tab_pager, mMapFragment, MAP_TAG );
			transaction.add( R.id.tab_pager, mWeatherFragment, WEATHER_TAG );
		}
		
		transaction.hide( mConditionsFragment );
		transaction.hide( mChartFragment );
		transaction.hide( mMapFragment );
		transaction.hide( mWeatherFragment );
		
		transaction.commitAllowingStateLoss();
		fragmentManager.executePendingTransactions();
		
		mActionBarAdapter = new DetailsActionBarAdapter( this, this, getActionBar() );
		mActionBarAdapter.initialize( savedState );
		
		invalidateOptionsMenu();
	}
	
	
	private void configureFragments()
	{
		configureMapFragment();
	}
	
	
	private void configureMapFragment()
	{
		MarkerOptions options = new MarkerOptions();
		options.title( mStation.name );
		options.snippet( mStation.dataProvider );
		options.position( mStation.latLng );
		mMapFragment.getMap().addMarker( options );
	}
	
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		if( item.getItemId() == android.R.id.home )
		{
			Intent upIntent = new Intent( this, StationsListActivity.class );
			upIntent.putExtra( ColoradoWaterSMS.EXTRA_DIVISION, mStation.div );
			upIntent.putExtra( ColoradoWaterSMS.EXTRA_WATER_DISTRICT, mStation.wd );
			NavUtils.navigateUpTo( this, upIntent );
			return true;
		}
		return super.onOptionsItemSelected( item );
	}
	
	
	public class TabPagerAdapter extends PagerAdapter
	{
		private final FragmentManager mFragmentManager;
        private FragmentTransaction mCurTransaction = null;
		private Fragment mCurrentPrimaryItem;
		
		public boolean disableSwipe = false;
		
		
		public TabPagerAdapter()
		{
			mFragmentManager = getFragmentManager();;
		}
		

		@Override
		public int getCount()
		{
			return TabState.COUNT;
		}
		
		
		@Override
		public void startUpdate( View container )
		{
			// no-op
		}
		
		
		@Override
		public boolean isViewFromObject( View view, Object object )
		{
			return ( (Fragment) object ).getView() == view;
		}
		
		
		@Override
		public int getItemPosition( Object object )
		{
			if( object == mConditionsFragment )
				return TabState.CONDITIONS;
			else if( object == mChartFragment )
				return TabState.CHART;
			else if( object == mMapFragment )
				return TabState.MAP;
			else if( object == mWeatherFragment )
				return TabState.WEATHER;
			
			return POSITION_NONE;
		}
		
		
		private Fragment getFragment( int position )
		{
			disableSwipe = false;
			if( position == TabState.CONDITIONS )
				return mConditionsFragment;
			else if( position == TabState.CHART )
				return mChartFragment;
			else if( position == TabState.MAP )
			{
				disableSwipe = true;
				return mMapFragment;
			}else if( position == TabState.WEATHER )
				return mWeatherFragment;
			
			throw new IllegalArgumentException( "no fragment for position: " + position );
		}
		
		
		public boolean getCanScroll( int position )
		{
			if( position == TabState.MAP )
			{
				return false;
			}
			
			return true;
		}
		
		
		@SuppressLint("NewApi")
		@Override
		public Object instantiateItem( View container, int position )
		{
			if( mCurTransaction == null ) 
				mCurTransaction = mFragmentManager.beginTransaction();
			
			Fragment f = getFragment( position );
			mCurTransaction.show( f );
			
			Log.e( "StationDetailsActivity.TabPagerAdapter", "SHOW FRAGMENT : " + f );
			
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 )
				f.setUserVisibleHint( f == mCurrentPrimaryItem );
			
			return f;
		}
		
		
		@Override
		public void destroyItem( View container, int position, Object object )
		{
			if( mCurTransaction == null )
				mCurTransaction = mFragmentManager.beginTransaction();
			
			mCurTransaction.hide( (Fragment) object );
		}
		
		
		@Override
		public void finishUpdate( ViewGroup container )
		{
			if( mCurTransaction != null )
			{
				mCurTransaction.commitAllowingStateLoss();
				mCurTransaction = null;
				mFragmentManager.executePendingTransactions();
			}
		}
		
		
		@SuppressLint("NewApi")
		@Override
		public void setPrimaryItem( ViewGroup container, int position, Object object )
		{
			Fragment fragment = (Fragment) object;
			if( mCurrentPrimaryItem != fragment )
			{
				if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 )
				{
					if( mCurrentPrimaryItem != null )
						mCurrentPrimaryItem.setUserVisibleHint( false );
					
					if( fragment != null )
						fragment.setUserVisibleHint( true );
				}
				
				mCurrentPrimaryItem = fragment;
			}
		}
		
		
		@Override
		public Parcelable saveState()
		{
			return null;
		}
		
		
		@Override
		public void restoreState( Parcelable state, ClassLoader loader )
		{
			// no-op
		}
	}
	
	
	private class TabPagerListener implements ViewPager.OnPageChangeListener
	{
		@Override
		public void onPageScrollStateChanged( int state )
		{
			
		}
		
		
		@Override
		public void onPageScrolled( int position, float positionOffset, int positionOffstPixels )
		{
			
		}
		
		
		@Override
		public void onPageSelected( int position )
		{
			mActionBarAdapter.setCurrentTab( position, false );
			invalidateOptionsMenu();
		}
	}
	
	
	public void onSelectedTabChanged()
	{
		updateFragmentsVisibility();
	}
	
	
	private void updateFragmentsVisibility()
	{
		int tab = mActionBarAdapter.getCurrentTab();
		
		if( mTabPager.getCurrentItem() != tab )
			mTabPager.setCurrentItem( tab, true );
	}
	
	
	public interface OnLoaderComplete
	{
		public void onLoaderComplete( int loaderId, Object data );
	}
}
