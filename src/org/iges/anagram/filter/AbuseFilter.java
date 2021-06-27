/* Copyright (C) 2000-2021 by George Mason University.
*  Authored by Joe Wielgosz and maintained by Jennifer Adams.
*  See file COPYRIGHT for more information.
*/
package org.iges.anagram.filter;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;

import org.iges.anagram.*;

/** A filter that tracks the number of hits per hour for each IP that uses this server,
 *  and blocks requests that have exceeded the IP's abuse limit.
 */
public class AbuseFilter 
    extends Filter {

    public String getFilterName() {
	return "abuse";
    }

    public void configure(Setting setting) 
	throws ConfigException {

	super.configure(setting);
	defaultHitLimit = setting.getNumAttribute("hits", 0);
	if (debug()) debug("default hit limit set to " + defaultHitLimit);
	defaultTimeout = setting.getNumAttribute("timeout", 1);
	if (debug()) debug("default timeout set to " + defaultTimeout);
    }

    protected void doFilter(ClientRequest clientRequest) 
	throws ModuleException {

	long now = System.currentTimeMillis();
	
	long hitLimit = 
	    clientRequest.getPrivilege().getNumAttribute("abuse_hits", 
							 defaultHitLimit);

	// Setting of zero means no hit limit
	if (hitLimit != 0) {
	    checkBlock(clientRequest, now, hitLimit);
	} else {
	    if (debug()) debug("no hit limit set");
	}

	next.handle(clientRequest);
    }

    protected File blockFile(String ip) {
	return server.getStore().get(this, ip);
    }

    protected HitLimiter loadBlock(String ip) {
	if (debug()) debug("checking for saved block for " + ip);
	try {
	    ObjectInputStream in = new ObjectInputStream
		(new BufferedInputStream
		    (new FileInputStream(blockFile(ip))));
	    HitLimiter returnVal = (HitLimiter)in.readObject();
	    in.close();
	    if (debug()) debug("loaded block for " + ip);
	    return returnVal;
	} catch (IOException ioe) {
	    return null;
	} catch (ClassNotFoundException cnfe) {
	    return null;
	}
	
    }

    protected void saveBlock(String ip, HitLimiter limiter) {
	if (debug()) debug("saving block for " + ip);
	try {
	    ObjectOutputStream out = new ObjectOutputStream
		(new BufferedOutputStream
		    (new FileOutputStream
			(blockFile(ip))));
	    out.writeObject(limiter);
	    out.close();
	} catch (IOException ioe) {
	    error("failed writing abuse block for " + ip + " to disk");
	}
	
    }

    protected void checkBlock(ClientRequest clientRequest, 
			      long now, 
			      long hitLimit) 
	throws ModuleException {

	HttpServletRequest request = clientRequest.getHttpRequest();
	String clientIP = request.getRemoteAddr();
	long abuseTimeout = 
	    clientRequest.getPrivilege().getNumAttribute("abuse_timeout", 
							 defaultTimeout);

	if (debug()) debug("checking whether " + clientIP + " is blocked");
	if (debug()) debug("hit limit is " + hitLimit +
			   ", timeout is " + abuseTimeout);
			   

	HitLimiter limiter;
	synchronized (ipList) {
	    limiter = (HitLimiter)ipList.get(clientIP);
	    if (limiter == null) {
		limiter = loadBlock(clientIP);
		if (limiter == null) {
		    if (debug()) debug("starting hit count for " + clientIP);
		    limiter = new HitLimiter();
		}
		ipList.put(clientIP, limiter);
	    }
	}
	
	boolean blocked = limiter.isBlocked(now, abuseTimeout);
	if (!blocked) {
	    limiter.addHit(now);
	    if (limiter.exceeded(now, hitLimit)) {
		info("limit of " + hitLimit + 
		     " hits per hour exceeded by " + clientIP + 
		     " (resolves to " + request.getRemoteHost() + ")");
		info("blocking " + clientIP + 
		     " for " + abuseTimeout + " hours");
		saveBlock(clientIP, limiter);
		blocked = true;
	    }
	}
	if (blocked) {
	    if (!blockFile(clientIP).exists()) {
		info("block file was deleted for " + clientIP + 
		     "; restarting hit count");
		ipList.remove(clientIP);
	    } else {
		throw new ModuleException(this, 
					  limiter.getMessage(abuseTimeout), 
					  "sent block message to client");
	    }
	}
	cleanIPList(now, abuseTimeout);
    }

    /** Contains HitLimiters for each IP that has been encountered recently. */
    private Map ipList = new HashMap();

    /** The last time the IP list was cleaned out. */
    private long lastCleaning;

    private static long staleTime = 24 * 3600 * 1000;

    /** Makes sure that the IP list doesn't get too big by cleaning out
     *  old entries once a day. */
    private void cleanIPList(long now, long timeoutMins) {
	if (now - lastCleaning > (staleTime)) {
	    if (debug()) debug("looking for stale hit counters");
	    Iterator it = ipList.entrySet().iterator();
	    while (it.hasNext()) {
		Map.Entry entry = (Map.Entry)it.next();
		HitLimiter limiter = (HitLimiter)entry.getValue();
		if (limiter.isStale(now, staleTime)) {
		    if (debug()) debug("removing stale hit counter for " + 
				       entry.getKey());
		    it.remove();
		}
	    }
	    lastCleaning = now;
	}
    }

    
    /** Tracks hits/hour, and switches to a blocked state if the number
     *  exceeds a given threshold.
     */
    protected class HitLimiter
	implements Serializable {

	/** Creates a hit limiter with the given limit and timeout period.
	 *  @param hitLimit Number of hits allowed in a one-hour period
	 *  @param timeout Time in hours that a block lasts once it has been
	 *  triggered.
	 */
	HitLimiter() {
	    hits = new long[countWindowMins];
	    lastTimeMins = 0;
	    blockedTimeMins = 0;
	}

	/** Time of the previous hit */
	private long lastTimeMins;
	
	/** If zero, no block is in effect.  If non-zero, indicates
	    the time at which the block went into effect.
	*/
	private long blockedTimeMins;
	
	/** Hit count for each of the previous sixty minutes. */
	private long hits[];
	
	/** Stored in between uses to avoid reallocating memory */
	private String message;
	/** Check in case the timeout changes after message is created */
	private long messageTimeout;

	public String getMessage(long timeoutMins) {
	    if (message == null || messageTimeout != timeoutMins) {
		messageTimeout = timeoutMins;
		message = "Sorry, your IP has been blocked " +
		    "because of excessive requests.\n" +
		    "Please check that any scripts you are running " + 
		    "function correctly and \n" + 
		    "are as efficient as possible.\n" +
		    "Access will be automatically restored after " +
		    (timeoutMins / 60) + " hours.\n" + 
		    "Contact the server administrator if you have questions.";
	    }
	    return message;
	}
		    
		
	/** Checks whether a block is in force */
	public boolean isBlocked(long now, long timeoutMins) {
	    long nowMins = now / (60 * 1000);
	    if (blockedTimeMins != 0) {
		return checkBlockedTime(nowMins, timeoutMins);
	    } else {
		return false;
	    }
	}
	
	/** Checks whether the current block has expired */
	private boolean checkBlockedTime(long nowMins, long timeoutMins) {
	    if ((nowMins - blockedTimeMins) > timeoutMins) {
		blockedTimeMins = 0;
		message = null;
		return false;
	    } else {
		return true;
	    }
	}
	
	/** Checks whether the hit limit has been exceeded */
	public boolean exceeded(long now, long hitLimit) {
	    long nowMins = now / (60 * 1000);
	    if (hitCount() > hitLimit) {
		blockedTimeMins = nowMins;
		return true;
	    } else {
		return false;
	    }
	}
	
	/** Indicates whether this limiter has had a hit recently. */
	public boolean isStale(long now, long staleTime) {
	    long nowMins = now / (60 * 1000);
	    return nowMins - lastTimeMins > staleTime;
	}
		
	/** Returns total number of hits in the last hour */
	private long hitCount() {
	    long count = 0;
	    for (int i = 0; i < hits.length; i++) {
		count += hits[i];
	    }
	    if (debug()) debug("cumulative hit count is " + count);
	    return count;
	}
	
	/** Registers a hit at the time given */
	public void addHit(long now) {
	    long nowMins = now / (60 * 1000);
	    updateBins(nowMins);
	    int bin = (int)(nowMins % countWindowMins);
	    hits[bin]++;
	    lastTimeMins = nowMins;
	    if (debug()) debug("adding hit. hits this minute: " + hits[bin]);
	}
	
	/** Zeroes any entries in the hits[] array that are now more than 
	 *  sixty minutes old 
	 */
	private void updateBins(long nowMins) {
	    if (nowMins == lastTimeMins || lastTimeMins == 0) {
		// Minute hasn't changed, or no hits yet
		return;
	    }
	    if (nowMins - lastTimeMins >= countWindowMins) {
		// No hit in an hour or more so reset everything
		Arrays.fill(hits, 0);
		return;
	    }
	    // Any data in bins after the last bin that was written to,
	    // through the current bin, must be from the previous time
	    // cycling through, so should now be zeroed out.  

	    // Arrays.fill will erase startBins inclusive, through endBins
	    // exclusive. So set startBin to next bin after lastTimeMins, and
	    // endBin to next bin after nowMins.

	    int startBin = (int)((lastTimeMins + 1) % countWindowMins);
	    int endBin = (int)((nowMins + 1) % countWindowMins);
	
	    // Actually, if startBin == endBin, then either nowMins == 
	    // lastTimeMins, or else nowMins >= lastTimeMins + 60. In
	    // either case, we should have already returned. But just
	    // to be safe...
	    if (startBin >= endBin) {
		Arrays.fill(hits, startBin, hits.length, 0);
		Arrays.fill(hits, 0, endBin, 0);
	    } else {
		Arrays.fill(hits, startBin, endBin, 0);
	    }
	}
    }

    /** Number of minutes a hit stays in the count, i.e. the
     *	period of time over which the hit limit applies.  Not
     *	configurable right now. */
    private int countWindowMins = 60;

    protected long defaultHitLimit;
    protected long defaultTimeout;
    
}
