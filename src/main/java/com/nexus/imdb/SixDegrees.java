package com.nexus.imdb;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.apache.log4j.* ;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.LoggerFactory;

import com.omertron.imdbapi.ImdbApi;
import com.omertron.imdbapi.model.ImdbCast;
import com.omertron.imdbapi.model.ImdbCredit;
import com.omertron.imdbapi.model.ImdbFilmography;
import com.omertron.imdbapi.model.ImdbMovie;
import com.omertron.imdbapi.model.ImdbMovieCharacter;
import com.omertron.imdbapi.model.ImdbMovieDetails;
import com.omertron.imdbapi.model.ImdbPerson;

public class SixDegrees {

    private static final Logger LOG = LoggerFactory.getLogger(SixDegrees.class);
    
    private static Map<String, List<MovieInfo>> fromActorToMovieInfoList = new HashMap<String, List<MovieInfo>>() ;
    private static Map<String, List<MovieInfo>> toActorToMovieInfoList = new HashMap<String, List<MovieInfo>>() ;
    
    private static List<MovieInfo> recentFromMovies = new ArrayList<MovieInfo>() ;
    private static List<MovieInfo> recentToMovies = new ArrayList<MovieInfo>() ;
    
    private static String from_actorId ;
    private static String to_actorId ;
    
    private static String from_actorName ;
    private static String to_actorName;
    
    private static List<String> allFromMovieIds = new ArrayList<String>() ;
    private static List<String> allToMovieIds = new ArrayList<String>() ;

    private static List<String> allFromActorIds = new ArrayList<String>() ;
    private static List<String> allToActorIds = new ArrayList<String>() ;

    private final ImdbApi imdbApi = new ImdbApi();
    
    private static final String LOGGING_CONFIG_FILE = "log4j.xml" ;

    public static void main(String[] args)
    {
    	// is this an XML or a property file?  Easiest to check if it is XML
    	boolean isPropertyFile = ( !LOGGING_CONFIG_FILE.toLowerCase().endsWith("xml") ) ;

    	if ( isPropertyFile )
    	{
    		PropertyConfigurator.configure(LOGGING_CONFIG_FILE);
    	}
    	else
    	{
    		DOMConfigurator.configure(LOGGING_CONFIG_FILE);
    	}

    	if ( ( args.length == 0 ) || ( args.length > 2 ))
    	{
    		LOG.error("# parameters must = 1 or 2") ;
    		return ;
    	}
    	
    	from_actorName = args[0] ;
    	
    	if ( args.length == 2 )
    	{
    	    to_actorName = args[1] ;
    	}

    	SixDegrees d6 = new SixDegrees();
    }
    

    public SixDegrees() 
    {
    	// get the id for each actor
    	from_actorId = imdbApi.getSearchForActorId(from_actorName);
    	
    	if ( ( to_actorName == null ) || ( to_actorName.length() == 0 ) )
    	{
    	    LOG.info("Actor: " + from_actorName + " = " + from_actorId);
    	    addActor( from_actorId, from_actorName, true, null ) ;
    		LOG.info("Done."); 
    		return ;
    	}
    	to_actorId = imdbApi.getSearchForActorId(to_actorName);
    	
    	showTitle() ;
    	
    	LOG.info("============== STARTING ==============");
    	LOG.info("FROM: " + from_actorName + " = " + from_actorId);
    	LOG.info("TO  : " + to_actorName + " = " + to_actorId);
    	
    	// add from actor
    	LOG.info("------ FROM --------"); 
    	addActor( from_actorId, from_actorName, true, null ) ;

    	// add to actor
    	LOG.info("------ TO --------"); 
    	addActor( to_actorId, to_actorName, false, null ) ;
    	
    	// check for a match
    	boolean areWeDone = false ;
    	
    	int ktr = 0 ;
    	while ( !areWeDone )
    	{
    		// check if we are done
    		areWeDone = checkIfWeAreDone() ;
    		
    		if ( !areWeDone )
    		{
    			// do a DEEP clone of the FROM and the TO lists
    			LOG.info("CLONING: START ------------------------------------------") ;
    			List<MovieInfo> clonedFrom = MovieInfo.cloneList(recentFromMovies) ;
    			List<MovieInfo> clonedTo = MovieInfo.cloneList(recentToMovies) ;
    			LOG.info("CLONING: END   ------------------------------------------") ;

    			// clear the lists (so we can re-add to them)
    			clearRecentList();
    			
    			// now loop through the CLONED lists
    			++ktr ;

    			LOG.info("------ Loop #" + ktr + ", From=" + true + ", #Movies=" + clonedFrom.size());
    			for (MovieInfo thisMovie : clonedFrom) 
    			{
					addMovie( thisMovie, true ) ;
				}

    			LOG.info("------ Loop #" + ktr + ", From=" + false + ", #Movies=" + clonedTo.size());
    			for (MovieInfo thatMovie : clonedTo) 
    			{
					addMovie( thatMovie, false ) ;
				}
    		}
    	}
    	LOG.info("We are done.");
    }
    
    private void showStatus( String s )
    {
    	System.out.print(s) ;
    }
   
    private void showList( MovieInfo mi , boolean inReverse )
    {
    	List<String> strings = mi.getPathToMovie().getList() ;
    	if ( strings == null )
    	{
    		System.out.println(" Strings == null");
    	}
    	else
    	{
    		if ( inReverse )
    		{
    			String[] data = (String[]) strings.toArray(new String[0]) ;
    			for (int i = data.length - 1; i >= 0; i--) 
    			{
    				System.out.println("     " + data[i] ) ;
    			}

    		}
    		else
    		{
    			for (String s : strings) 
    			{
    				System.out.println("     " + s ) ;
    			}
    		}
    	}
    }
    
    private void showTitle()
    {
    	System.out.println("");
    	System.out.println("Finding how to connect " + from_actorName + " to " + to_actorName + " (in the fewest movies/steps possible). ");
    	System.out.println("");
    	System.out.println("Status will be shown, ") ;
    	System.out.println("       with A meaning an Actor is being analyzed");
    	System.out.println("       with M meaning a Movie is being analyzed");
    	System.out.println("");
    }

    private void showAnswer( MovieInfo from, MovieInfo to)
    {
    	System.out.println("");
    	System.out.println("");
    	System.out.println("");
    	System.out.println("FROM " + from_actorName + " to " + to_actorName + " :" ) ;
    	showList( from, false ) ;
    	showList( to, true ) ;
    	System.out.println("") ;
    	
    	int numMovies = allFromMovieIds.size() + allToMovieIds.size();
    	int numActors = allFromActorIds.size() + allToActorIds.size();
    			
    	System.out.println("Analysis Details:") ;
    	System.out.println("    # of movies analyzed = " + numMovies ) ;
    	System.out.println("    # of actors analyzed = " + numActors ) ;
    	System.out.println("") ;
    }
    
    private boolean checkIfWeAreDone()
    {
    	// List<String> fromIds = getMovieIds( fromActorToMovieInfoList ) ;
    	// List<String> toIds = getMovieIds( toActorToMovieInfoList ) ;
    	
    	Iterator<Entry<String, List<MovieInfo>>> fromIterator = fromActorToMovieInfoList.entrySet().iterator();
    	Iterator<Entry<String, List<MovieInfo>>> toIterator ;
    	Map.Entry innerMapEntry ;
    	Map.Entry outerMapEntry ;
    	List<MovieInfo> outerMovieList ;
    	List<MovieInfo> innerMovieList ;
    	MovieInfo mi ;
    	while (fromIterator.hasNext()) 
    	{
    		outerMapEntry = fromIterator.next();
    		outerMovieList = (List<MovieInfo>) outerMapEntry.getValue();

    		// now loop through each of these movies (for a given actor) 
    		// and compare against ALL the To movies
    		for (MovieInfo outerMovieInfo : outerMovieList) 
    		{
    			toIterator = toActorToMovieInfoList.entrySet().iterator();
    			while (toIterator.hasNext()) 
    			{
    				innerMapEntry = toIterator.next();
    				innerMovieList = (List<MovieInfo>) innerMapEntry.getValue();

    				// now loop through each of these movies (for a given actor) 
    				for (MovieInfo innerMovieInfo : innerMovieList ) 
    				{
    					// ok, so now compare already
    					if ( innerMovieInfo.getMyMovieId().equals(outerMovieInfo.getMyMovieId()))
    					{
    						showAnswer( outerMovieInfo, innerMovieInfo ) ;
    						return true ;
    					}
    				}
    			}
    		}
    	}
    	return false ;
    }
    
    
    // actor 
    public void showFilmList( List<ImdbFilmography> list, String actorName )
    {
    	String label ;
    	List<ImdbMovieCharacter> roles ;
		ImdbMovie movie ; 
		String movieId ;
		String movieName ;

    	for (ImdbFilmography thisOne : list) 
    	{
    		label = thisOne.getToken();
    		if ( ( label.equalsIgnoreCase("Actor")) || ( label.equalsIgnoreCase("Actress")))
    		{
    			roles = thisOne.getList() ;
    			for (ImdbMovieCharacter thisRole : roles) 
    			{
					movie = thisRole.getTitle() ;
					movieId = movie.getImdbId() ;
					movieName = movie.getTitle() ;
					LOG.info(actorName + " was in movie[" + movieId + "] = " + movieName ) ;
				}
    		}
		}
    }
    
    private void process( boolean addingMovie, String actorId, MovieInfo mi, boolean isFrom)
    {
        List<MovieInfo> thisList ;
        
        // LOG.info("Adding actor " + mi.getMyActorName() + ", From = " + isFrom + ", movie = " + mi.getMyMovieName()) ;

    	if ( isFrom )
    	{
    		// if list does not exist, create an empty
    		if ( fromActorToMovieInfoList.containsKey(actorId) )
    		    thisList = fromActorToMovieInfoList.get(actorId) ;
    		else
    			thisList = new ArrayList<MovieInfo>() ;
    	}
    	else
    	{
    		// if list does not exist, create an empty
    		if ( toActorToMovieInfoList.containsKey(actorId) )
    		    thisList = toActorToMovieInfoList.get(actorId) ;
    		else
    			thisList = new ArrayList<MovieInfo>() ;
    	}
    	
    	// list is an ordered list of movieInfos
    	thisList.add( mi ) ;
    	
    	if ( isFrom )
    	{
    		fromActorToMovieInfoList.put(actorId,thisList ) ;
    		
    		/*
    		if ( addingMovie )
    		{
    			// append this movie to the All FROM movie list
    			allFromMovieIds.add( mi.getMyMovieId()) ;
    		}
    		*/
    	}
    	else
    	{
    		toActorToMovieInfoList.put(actorId,thisList ) ;

    		/*
    		if ( addingMovie )
    		{
    			// append this movie to the All TO movie list
    			allToMovieIds.add( mi.getMyMovieId()) ;
    		}
    		*/
    	}
    }

    private void clearRecentList()
    {
    	recentFromMovies = new ArrayList<MovieInfo>() ;
    	recentToMovies = new ArrayList<MovieInfo>() ;
    }

    private void addToRecentList( MovieInfo ap, boolean isFrom )
    {
    	if ( isFrom )
    	{
    		if ( recentFromMovies == null )
    			recentFromMovies = new ArrayList<MovieInfo>() ;
    		recentFromMovies.add(ap) ;
    	}
    	else
    	{
    		if ( recentToMovies == null )
    			recentToMovies = new ArrayList<MovieInfo>() ;
    		recentToMovies.add(ap) ;
    	}
    }
    
    public void addActor( String id, String name, boolean isFrom, MovieInfo sourceMi )
    {
    	showStatus("A");
    	
    	boolean addingMovie = false ; // not adding the WHOLE movie
    	
       	if ( ( isFrom ) && ( allFromActorIds.contains(id) ) )
    	{
    		// LOG.info("Already processed actor = " + name );
    		return ;
    	}

    	if ( ( !isFrom ) && ( allToActorIds.contains(id) ) )
    	{
    		// LOG.info("Already processed movie = " + name );
    		return ;
    	}

    	// get list of all movies for this actor
    	List<ImdbFilmography>  filmList = imdbApi.getActorFilmography(id) ;
    	//showFilmList( filmList , name) ;

    	// add each movie+actor association
    	String label ;
    	List<ImdbMovieCharacter> roles ;
    	ImdbMovie movie ; 
    	String movieId ;
    	String movieName ;
    	MovieInfo mi ;
    	boolean sourceIsNull = ( sourceMi == null ) ;

    	for (ImdbFilmography thisOne : filmList) 
    	{
    		label = thisOne.getToken();
    		if ( ( label.equalsIgnoreCase("Actor")) || ( label.equalsIgnoreCase("Actress")))
    		{
    			roles = thisOne.getList() ;
    			LOG.info("------  Adding movies for actor = " + name );
    			for (ImdbMovieCharacter thisRole : roles) 
    			{
    				movie = thisRole.getTitle() ;
    				String movieType = movie.getType();
    				
    				// check if it is a "movie"
    				if ( movieType.contains("feature"))
    				{
    					movieId = movie.getImdbId() ;
    					movieName = movie.getTitle() ;

    					if ( sourceIsNull )
    						mi = new MovieInfo( name, id, movieName, movieId, null ) ;
    					else
    						mi = new MovieInfo( name, id, movieName, movieId, sourceMi) ;

    					// save info for (this loop)
    					addToRecentList( mi, isFrom ) ;

    					// process this data
    					process( addingMovie, id, mi, isFrom) ;
    				}
    			}
    		}
    	}
    	
       	if ( isFrom ) 
       		allFromActorIds.add( id ) ;
       	else
       		allToActorIds.add( id ) ;
    }

    public void addMovie( MovieInfo sourceMovieInfo, boolean isFrom )
    {
    	showStatus("M");
    	boolean addingMovie = true ; // adding the WHOLE movie

    	String movieId = sourceMovieInfo.getMyMovieId();
    	String movieName = sourceMovieInfo.getMyMovieName() ;
    	
    	if ( ( isFrom ) && ( allFromMovieIds.contains(movieId) ) )
    	{
    		// LOG.info("Already processed movie = " + movieName );
    		return ;
    	}

    	if ( ( !isFrom ) && ( allToMovieIds.contains(movieId) ) )
    	{
    		// LOG.info("Already processed movie = " + movieName );
    		return ;
    	}
    	
    	// get list of all movies for this actor
    	ImdbMovieDetails  movieDetails = imdbApi.getFullDetails(movieId) ;
    	//showFilmList( filmList , name) ;

    	List<ImdbCast> castList = movieDetails.getCast() ;

    	// add each movie+actor association
    	MovieInfo mi ;
    	ImdbPerson person ;
    	String actorId ;
    	String actorName ;

    	LOG.debug("-------------       Adding " + castList.size() + " actors from movie = " + movieName ) ;
    	for (ImdbCast thisCast : castList) 
    	{
    		person = thisCast.getPerson() ;
    		actorId = person.getActorId() ;
    		actorName = person.getName() ;
    		
    		addActor(actorId, actorName, isFrom, sourceMovieInfo ) ;
    	}

    	// now update the Movies processed lists
    	if ( isFrom )
    	{
    		// append this movie to the All FROM movie list
    		allFromMovieIds.add( movieId) ;
    	}
    	else
    	{
    		// append this movie to the All TO movie list
    		allToMovieIds.add(movieId) ;
    	}
    }


}


