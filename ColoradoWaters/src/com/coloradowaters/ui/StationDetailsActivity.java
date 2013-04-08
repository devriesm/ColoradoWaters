package com.coloradowaters.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.coloradowaters.R;


public class StationDetailsActivity extends BaseActivity
{
	private Fragment mFragment;
	
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		
		getActionBar().setDisplayHomeAsUpEnabled( true );
		
		final String customTitle = getIntent().getStringExtra( Intent.EXTRA_TITLE );
		setTitle( customTitle != null ? customTitle : getTitle() );
		
		if( savedInstanceState == null )
		{
			mFragment = new StationDetailsFragment();
			mFragment.setArguments( intentToFragmentArguments( getIntent() ) );
			getFragmentManager().beginTransaction().add( R.id.root_container, mFragment, "single_pane" ).commit();
		}else
		{
			mFragment = getFragmentManager().findFragmentByTag( "single_pane" );
		}
	}
	
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		if( item.getItemId() == android.R.id.home )
		{
			Intent upIntent = new Intent( this, MainActivity.class );
			NavUtils.navigateUpTo( this, upIntent );
			return true;
		}
		return super.onOptionsItemSelected( item );
	}
	
	
	public interface OnLoaderComplete
	{
		public void onLoaderComplete( int loaderId, Object data );
	}
}
