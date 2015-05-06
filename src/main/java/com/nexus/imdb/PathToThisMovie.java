package com.nexus.imdb;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathToThisMovie 
{
    private static final Logger LOG = LoggerFactory.getLogger(PathToThisMovie.class);
	List<String> theList ;

	public PathToThisMovie()
	{
		theList = new ArrayList<String>() ;
	}
	
	public PathToThisMovie( PathToThisMovie preface, String actorName, String movieName )
	{
		if ( preface == null )
		    theList = new ArrayList<String>() ;
		else
		    theList = cloneTheList( preface.getList()) ;
		
		// now add the actor and the movie 
		this.appendPath(actorName, movieName);
	}
	
	public void appendPath( PathToThisMovie path )
	{
		List<String> locList ;
		if ( path == null )
		    locList = new ArrayList<String>() ;
		else
		    locList = cloneTheList( path.getList()) ;
		
		theList.addAll(locList);
	}
	
	public void appendPath( String actorName, String movieName )
	{
		String merged = actorName + " in movie " + movieName ;
		theList.add( merged ) ;
	}
	
	public List<String> cloneTheList( List<String> list )
	{
		List<String> newList = new ArrayList<String>() ;
		if ( list != null )
		{
			for (String thisString : list) 
			{
				newList.add( thisString ) ;
			}
		}
		return newList ;
	}
	
	public static PathToThisMovie cloneMe( PathToThisMovie clonee )
	{
		PathToThisMovie newPath = new PathToThisMovie() ;
		if ( clonee != null )
		    newPath.theList = newPath.cloneTheList( clonee.getList() ) ;

		return newPath ;
	}
	
	public void logThisPath( String linePreface)
	{
		if ( ( theList == null ) || ( theList.isEmpty() ) )
		    LOG.info(linePreface + " is empty.") ;
		else
		{
			String[] data = (String[]) theList.toArray(new String[0]) ;
			for (int i = 0; i < data.length; i++) 
			{
				LOG.info(linePreface + "[" + i + "] = " + data[i]); 
			}
		}
	}
	
	protected List<String> getList()
	{
		return theList ;
	}
}
