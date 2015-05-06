package com.nexus.imdb;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovieInfo 
{
	private String myActorId ;
	private String myMovieId ;
	private String myActorName;
	private String myMovieName;
	private PathToThisMovie pathToMovie ;

    private static final Logger LOG = LoggerFactory.getLogger(MovieInfo.class);

	public MovieInfo( String actorName, String actorId, String movieName, String movieId, MovieInfo src )
	{
		myActorId = actorId ;
		myMovieId = movieId ;
		myActorName = actorName;
		myMovieName = movieName ;
		
		if ( src == null )
		{
		    pathToMovie = new PathToThisMovie( null , actorName, movieName ) ;
		}
		else
		{
			// start with empty
			pathToMovie = new PathToThisMovie() ;
			
			// append the existing path
			pathToMovie.appendPath(src.getPathToMovie());
			
			// add a path for this actor and the movie from the source
			pathToMovie.appendPath(actorName, src.getMyMovieName());

			// now add path for this actor and movie
			pathToMovie.appendPath(actorName, movieName );
		}
		
		LOG.info("MI: " + this.toString() + " , Path : ");
		pathToMovie.logThisPath("");
	}
	
	// a kind of clone
	public MovieInfo(MovieInfo mi)
	{
		this.myActorId = mi.myActorId ;
		this.myActorName = mi.myActorName ;
		this.myMovieId = mi.myMovieId ;
		this.myMovieName = mi.myMovieName ;
		this.pathToMovie = PathToThisMovie.cloneMe(mi.pathToMovie) ;

		LOG.info("CLONED MI: " + this.toString() + " , Path : ");
		pathToMovie.logThisPath("");
	}
	
	 
	// clone a LIST of MovieInfo records
    public static List<MovieInfo> cloneList(List<MovieInfo> list)
    {
    	List<MovieInfo> theClone = new ArrayList<MovieInfo>( list.size() ) ;
    	for (MovieInfo mi : list) 
    	{
    		theClone.add( new MovieInfo( mi )) ;
		}
    	return theClone ;
    }
    
	public String toString()
	{
		StringBuilder sb = new StringBuilder() ;
		sb.append( myActorName ).append(" (").append(myActorId).append(") ") ;
		sb.append("was in movie [").append(myMovieId).append("] = ").append(myMovieName) ;
		
		return sb.toString();
	}

	public String getMyActorId() {
		return myActorId;
	}

	public void setMyActorId(String myActorId) {
		this.myActorId = myActorId;
	}

	public PathToThisMovie getPathToMovie() {
		return pathToMovie;
	}

	public void setPathToMovie(PathToThisMovie pathToMovie) {
		this.pathToMovie = pathToMovie;
	}

	public String getMyMovieId() {
		return myMovieId;
	}

	public void setMyMovieId(String myMovieId) {
		this.myMovieId = myMovieId;
	}

	public String getMyActorName() {
		return myActorName;
	}

	public void setMyActorName(String myActorName) {
		this.myActorName = myActorName;
	}

	public String getMyMovieName() {
		return myMovieName;
	}

	public void setMyMovieName(String myMovieName) {
		this.myMovieName = myMovieName;
	}

}
