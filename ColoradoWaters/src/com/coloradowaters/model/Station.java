package com.coloradowaters.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class Station implements Parcelable
{
	public int div;
	public int wd;
	public String abbrev;
	public String name;
	public String dataProvider;
	public String dataProviderAbbrev;
	public double utmX;
	public double utmY;
	public LatLng latLng;
	
	
	public Station(){}
	public Station( Parcel in )
	{
		div = in.readInt();
		wd = in.readInt();
		abbrev = in.readString();
		name = in.readString();
		dataProvider = in.readString();
		dataProviderAbbrev = in.readString();
		utmX = in.readDouble();
		utmY = in.readDouble();
		latLng = in.readParcelable( LatLng.class.getClassLoader() );
	}
	
	
	@Override
	public int describeContents()
	{
		return 0;
	}
	
	
	@Override
	public void writeToParcel( Parcel dest, int flags )
	{
		dest.writeInt( div );
		dest.writeInt( wd );
		dest.writeString( abbrev );
		dest.writeString( name );
		dest.writeString( dataProvider );
		dest.writeString( dataProviderAbbrev );
		dest.writeDouble( utmX );
		dest.writeDouble( utmY );
		dest.writeParcelable( latLng, 0 );
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator()
	{
		public Station createFromParcel( Parcel in )
		{
			return new Station( in );
		}
		
		
		public Station[] newArray( int size )
		{
			return new Station[size];
		}
	};
	
}
