package com.coloradowaters.ui;

import com.coloradowaters.R;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;

public class DetailsActionBarAdapter
{
	private static final String EXTRA_SELECTED_TAB = "selected_tab";
	
	
	public interface Listener
	{
		/**
		 * Called when the user selects a tab.  The new tab can be obtained using
		 * {@link #getCurrentTab}.
		 */
		void onSelectedTabChanged();
	}
	
	public interface TabState
	{
		public static int CONDITIONS = 0;
		public static int CHART = 1;
		public static int MAP = 2;
		public static int WEATHER = 3;
		
		public static int COUNT = 4;
		public static int DEFAULT = CONDITIONS;
	}
	
	private final Context mContext;
	private final ActionBar mActionBar;
	private Listener mListener;
	private DetailsTabListener mTabListener;
	private int mCurrentTab = TabState.DEFAULT;
	
	
	public DetailsActionBarAdapter( Context context, Listener listener, ActionBar actionBar )
	{
		mContext = context;
		mListener = listener;
		mActionBar = actionBar;
		
		mTabListener = new DetailsTabListener();
		
		setupTabs();
	}
	
	
	private void setupTabs()
	{
		addTab( R.string.tab_current_conditions );
		addTab( R.string.tab_charts );
		addTab( R.string.tab_map );
		addTab( R.string.tab_weather );
	}
	
	
	private class DetailsTabListener implements ActionBar.TabListener
	{
		public boolean mIgnoreTabSelected;

		@Override
		public void onTabReselected( Tab tab, FragmentTransaction ft )
		{
			
		}
		
		
		@Override
		public void onTabSelected( Tab tab, FragmentTransaction ft )
		{
			if( !mIgnoreTabSelected ) 
			{
                setCurrentTab( tab.getPosition() );
            }
		}
		
		
		@Override
		public void onTabUnselected( Tab tab, FragmentTransaction ft )
		{
			
		}
	}
	
	
	public void initialize( Bundle savedState )
	{
		if( savedState != null )
		{
			mCurrentTab = savedState.getInt( EXTRA_SELECTED_TAB, TabState.DEFAULT );
		}else
		{
			mCurrentTab = TabState.DEFAULT;
		}
		
		update();
	}
	
	
	public void setListener( Listener listener )
	{
		mListener = listener;
	}
	
	
	public void addTab( int resId )
	{
		Tab tab = mActionBar.newTab();
		tab.setText( resId );
		tab.setTabListener( mTabListener );
		mActionBar.addTab( tab );
	}
	
	
	public void setCurrentTab( int tab )
	{
		setCurrentTab( tab, true );
	}
	
	
	public void setCurrentTab( int tab, boolean notifyListener )
	{
		if( tab == mCurrentTab )
			return;
			
		mCurrentTab = tab;
		
		final int actionBarSelectedNavIndex = mActionBar.getSelectedNavigationIndex();
		
		if( mCurrentTab != actionBarSelectedNavIndex )
			mActionBar.setSelectedNavigationItem( mCurrentTab );
		
		if( notifyListener && mListener != null ) mListener.onSelectedTabChanged();
	}
	
	
	public int getCurrentTab()
	{
		return mCurrentTab;
	}
	
	
	public void onSaveInstanceState( Bundle outState )
	{
        outState.putInt( EXTRA_SELECTED_TAB, mCurrentTab );
    }
	
	
	public void update()
	{
		mTabListener.mIgnoreTabSelected = false;
		mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
		mActionBar.setSelectedNavigationItem( mCurrentTab );
		//mTabListener.mIgnoreTabSelected = false;
	}
}
