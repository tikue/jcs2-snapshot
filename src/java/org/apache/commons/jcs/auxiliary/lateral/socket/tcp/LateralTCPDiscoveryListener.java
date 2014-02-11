package org.apache.commons.jcs.auxiliary.lateral.socket.tcp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jcs.auxiliary.lateral.LateralCacheAttributes;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheNoWait;
import org.apache.commons.jcs.auxiliary.lateral.LateralCacheNoWaitFacade;
import org.apache.commons.jcs.auxiliary.lateral.socket.tcp.behavior.ITCPLateralCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICache;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs.engine.behavior.IElementSerializer;
import org.apache.commons.jcs.engine.logging.behavior.ICacheEventLogger;
import org.apache.commons.jcs.utils.discovery.DiscoveredService;
import org.apache.commons.jcs.utils.discovery.behavior.IDiscoveryListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This knows how to add and remove discovered services. It observes UDP discovery events.
 * <p>
 * We can have one listener per region, or one shared by all regions.
 */
public class LateralTCPDiscoveryListener
    implements IDiscoveryListener
{
    /** The log factory */
    private final static Log log = LogFactory.getLog( LateralTCPDiscoveryListener.class );

    /**
     * Map of no wait facades. these are used to determine which regions are locally configured to
     * use laterals.
     */
    private final Map<String, LateralCacheNoWaitFacade<? extends Serializable, ? extends Serializable>> facades =
        Collections.synchronizedMap( new HashMap<String, LateralCacheNoWaitFacade<? extends Serializable, ? extends Serializable>>() );

    /**
     * List of regions that are configured differently here than on another server. We keep track of
     * this to limit the amount of info logging.
     */
    private final Set<String> knownDifferentlyConfiguredRegions =
        Collections.synchronizedSet( new HashSet<String>() );

    /** The cache manager. */
    private final ICompositeCacheManager cacheMgr;

    /** The event logger. */
    protected ICacheEventLogger cacheEventLogger;

    /** The serializer. */
    protected IElementSerializer elementSerializer;

    /**
     * This plugs into the udp discovery system. It will receive add and remove events.
     * <p>
     * @param cacheMgr
     * @param cacheEventLogger
     * @param elementSerializer
     */
    protected LateralTCPDiscoveryListener( ICompositeCacheManager cacheMgr, ICacheEventLogger cacheEventLogger,
                                           IElementSerializer elementSerializer )
    {
        this.cacheMgr = cacheMgr;
        this.cacheEventLogger = cacheEventLogger;
        this.elementSerializer = elementSerializer;
    }

    /**
     * Adds a nowait facade under this cachename. If one already existed, it will be overridden.
     * <p>
     * This adds nowaits to a facade for the region name. If the region has no facade, then it is
     * not configured to use the lateral cache, and no facade will be created.
     * <p>
     * @param cacheName - the region name
     * @param facade - facade (for region) => multiple lateral clients.
     * @return true if the facade was not already registered.
     */
    public synchronized boolean addNoWaitFacade( String cacheName, LateralCacheNoWaitFacade<? extends Serializable, ? extends Serializable> facade )
    {
        boolean isNew = !containsNoWaitFacade( cacheName );

        // override or put anew, it doesn't matter
        facades.put( cacheName, facade );
        knownDifferentlyConfiguredRegions.remove( cacheName );

        return isNew;
    }

    /**
     * Allows us to see if the facade is present.
     * <p>
     * @param cacheName - facades are for a region
     * @return do we contain the no wait. true if so
     */
    public boolean containsNoWaitFacade( String cacheName )
    {
        return facades.containsKey( cacheName );
    }

    /**
     * Allows us to see if the facade is present and if it has the no wait.
     * <p>
     * @param cacheName - facades are for a region
     * @param noWait - is this no wait in the facade
     * @return do we contain the no wait. true if so
     */
    public <K extends Serializable, V extends Serializable> boolean containsNoWait( String cacheName, LateralCacheNoWait<K, V> noWait )
    {
        @SuppressWarnings("unchecked") // Need to cast because of common map for all facades
        LateralCacheNoWaitFacade<K, V> facade = (LateralCacheNoWaitFacade<K, V>)facades.get( noWait.getCacheName() );
        if ( facade == null )
        {
            return false;
        }

        return facade.containsNoWait( noWait );
    }

    /**
     * When a broadcast is received from the UDP Discovery receiver, for each cacheName in the
     * message, the add no wait will be called here. To add a no wait, the facade is looked up for
     * this cache name.
     * <p>
     * Each region has a facade. The facade contains a list of end points--the other tcp lateral
     * services.
     * <p>
     * @param noWait
     * @return true if we found the no wait and added it. False if the no wait was not present or it
     *         we already had it.
     */
    protected <K extends Serializable, V extends Serializable> boolean addNoWait( LateralCacheNoWait<K, V> noWait )
    {
        @SuppressWarnings("unchecked") // Need to cast because of common map for all facades
        LateralCacheNoWaitFacade<K, V> facade = (LateralCacheNoWaitFacade<K, V>)facades.get( noWait.getCacheName() );
        if ( log.isDebugEnabled() )
        {
            log.debug( "addNoWait > Got facade for " + noWait.getCacheName() + " = " + facade );
        }

        if ( facade != null )
        {
            boolean isNew = facade.addNoWait( noWait );
            if ( log.isDebugEnabled() )
            {
                log.debug( "Called addNoWait, isNew = " + isNew );
            }
            return isNew;
        }
        else
        {
            if ( !knownDifferentlyConfiguredRegions.contains( noWait.getCacheName() ) )
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "addNoWait > Different nodes are configured differently or region ["
                        + noWait.getCacheName() + "] is not yet used on this side.  " );
                }
                knownDifferentlyConfiguredRegions.add( noWait.getCacheName() );
            }
            return false;
        }
    }

    /**
     * Look up the facade for the name. If it doesn't exist, then the region is not configured for
     * use with the lateral cache. If it is present, remove the item from the no wait list.
     * <p>
     * @param noWait
     * @return true if we found the no wait and removed it. False if the no wait was not present.
     */
    protected <K extends Serializable, V extends Serializable> boolean removeNoWait( LateralCacheNoWait<K, V> noWait )
    {
        @SuppressWarnings("unchecked") // Need to cast because of common map for all facades
        LateralCacheNoWaitFacade<K, V> facade = (LateralCacheNoWaitFacade<K, V>)facades.get( noWait.getCacheName() );
        if ( log.isDebugEnabled() )
        {
            log.debug( "removeNoWait > Got facade for " + noWait.getCacheName() + " = " + facade );
        }

        if ( facade != null )
        {
            boolean removed = facade.removeNoWait( noWait );
            if ( log.isDebugEnabled() )
            {
                log.debug( "Called removeNoWait, removed " + removed );
            }
            return removed;
        }
        else
        {
            if ( !knownDifferentlyConfiguredRegions.contains( noWait.getCacheName() ) )
            {
                if ( log.isInfoEnabled() )
                {
                    log.info( "removeNoWait > Different nodes are configured differently or region ["
                        + noWait.getCacheName() + "] is not yet used on this side.  " );
                }
                knownDifferentlyConfiguredRegions.add( noWait.getCacheName() );
            }
            return false;
        }
    }

    /**
     * Creates the lateral cache if needed.
     * <p>
     * We could go to the composite cache manager and get the the cache for the region. This would
     * force a full configuration of the region. One advantage of this would be that the creation of
     * the later would go through the factory, which would add the item to the no wait list. But we
     * don't want to do this. This would force this client to have all the regions as the other.
     * This might not be desired. We don't want to send or receive for a region here that is either
     * not used or not configured to use the lateral.
     * <p>
     * Right now, I'm afraid that the region will get puts if another instance has the region
     * configured to use the lateral and our address is configured. This might be a bug, but it
     * shouldn't happen with discovery.
     * <p>
     * @param service
     */
    public void addDiscoveredService( DiscoveredService service )
    {
        // get a cache and add it to the no waits
        // the add method should not add the same.
        // we need the listener port from the original config.
        LateralTCPCacheManager lcm = findManagerForServiceEndPoint( service );

        ArrayList<String> regions = service.getCacheNames();
        if ( regions != null )
        {
            // for each region get the cache
            for (String cacheName : regions)
            {
                try
                {
                    ICache<? extends Serializable, ? extends Serializable> ic = lcm.getCache( cacheName );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Got cache, ic = " + ic );
                    }

                    // add this to the nowaits for this cachename
                    if ( ic != null )
                    {
                        addNoWait( (LateralCacheNoWait<? extends Serializable, ? extends Serializable>) ic );
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Called addNoWait for cacheName [" + cacheName + "]" );
                        }
                    }
                }
                catch ( Exception e )
                {
                    log.error( "Problem creating no wait", e );
                }
            }
        }
        else
        {
            log.warn( "No cache names found in message " + service );
        }
    }

    /**
     * Removes the lateral cache.
     * <p>
     * We need to tell the manager that this instance is bad, so it will reconnect the sender if it
     * comes back.
     * <p>
     * @param service
     */
    public void removeDiscoveredService( DiscoveredService service )
    {
        // get a cache and add it to the no waits
        // the add method should not add the same.
        // we need the listener port from the original config.
        LateralTCPCacheManager lcm = findManagerForServiceEndPoint( service );

        ArrayList<String> regions = service.getCacheNames();
        if ( regions != null )
        {
            // for each region get the cache
            for (String cacheName : regions)
            {
                try
                {
                    ICache<? extends Serializable, ? extends Serializable> ic = lcm.getCache( cacheName );

                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Got cache, ic = " + ic );
                    }

                    // remove this to the nowaits for this cachename
                    if ( ic != null )
                    {
                        removeNoWait( (LateralCacheNoWait<? extends Serializable, ? extends Serializable>) ic );
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Called removeNoWait for cacheName [" + cacheName + "]" );
                        }
                    }
                }
                catch ( Exception e )
                {
                    log.error( "Problem removing no wait", e );
                }
            }
        }
        else
        {
            log.warn( "No cache names found in message " + service );
        }
    }

    /**
     * Gets the appropriate manager.
     * <p>
     * @param service
     * @return LateralTCPCacheManager configured for that end point.
     */
    private LateralTCPCacheManager findManagerForServiceEndPoint( DiscoveredService service )
    {
        ITCPLateralCacheAttributes lca = new TCPLateralCacheAttributes();
        lca.setTransmissionType( LateralCacheAttributes.TCP );
        lca.setTcpServer( service.getServiceAddress() + ":" + service.getServicePort() );
        LateralTCPCacheManager lcm = LateralTCPCacheManager.getInstance( lca, cacheMgr, cacheEventLogger,
                                                                         elementSerializer );
        return lcm;
    }
}
