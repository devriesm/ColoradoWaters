package com.coloradowaters.ui;

import android.app.Fragment;
import android.os.Bundle;

import com.coloradowaters.R;

public class MainActivity extends BaseActivity
{
	private Fragment mFragment;
	
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		if( savedInstanceState == null )
		{
			mFragment = new WaterDistrictsListFragment();
			mFragment.setArguments( intentToFragmentArguments( getIntent() ) );
			getFragmentManager().beginTransaction().add( R.id.root_container, mFragment, "single_pane" ).commit();
		}else
		{
			mFragment = getFragmentManager().findFragmentByTag( "single_pane" );
		}
    }
}
