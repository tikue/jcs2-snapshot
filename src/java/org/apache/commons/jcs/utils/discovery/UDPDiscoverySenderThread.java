package org.apache.commons.jcs.utils.discovery;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used to periodically broadcast our location to other caches that might be listening.
 */
public class UDPDiscoverySenderThread
    implements Runnable
{
    /** The logger. */
    private final static Log log = LogFactory.getLog( UDPDiscoverySenderThread.class );

    /**
     * details of the host, port, and service being advertised to listen for TCP socket connections
     */
    private final UDPDiscoveryAttributes attributes;

    /** List of known regions. */
    private ArrayList<String> cacheNames = new ArrayList<String>();

    /**
     * @param cacheNames The cacheNames to set.
     */
    protected void setCacheNames( ArrayList<String> cacheNames )
    {
        if ( log.isInfoEnabled() )
        {
            log.info( "Resetting cacheNames = [" + cacheNames + "]" );
        }
        this.cacheNames = cacheNames;
    }

    /**
     * @return Returns the cacheNames.
     */
    protected ArrayList<String> getCacheNames()
    {
        return cacheNames;
    }

    /**
     * Constructs the sender with the port to tell others to connect to.
     * <p>
     * On construction the sender will request that the other caches let it know their addresses.
     * @param attributes host, port, etc.
     * @param cacheNames List of strings of the names of the region participating.
     */
    public UDPDiscoverySenderThread( UDPDiscoveryAttributes attributes, ArrayList<String> cacheNames )
    {
        this.attributes = attributes;

        this.cacheNames = cacheNames;

        if ( log.isDebugEnabled() )
        {
            log.debug( "Creating sender thread for discoveryAddress = [" + attributes.getUdpDiscoveryAddr()
                + "] and discoveryPort = [" + attributes.getUdpDiscoveryPort() + "] myHostName = ["
                + attributes.getServiceAddress() + "] and port = [" + attributes.getServicePort() + "]" );
        }

        UDPDiscoverySender sender = null;
        try
        {
            // move this to the run method and determine how often to call it.
            sender = new UDPDiscoverySender( attributes.getUdpDiscoveryAddr(), attributes.getUdpDiscoveryPort() );
            sender.requestBroadcast();

            if ( log.isDebugEnabled() )
            {
                log.debug( "Sent a request broadcast to the group" );
            }
        }
        catch ( Exception e )
        {
            log.error( "Problem sending a Request Broadcast", e );
        }
        finally
        {
            try
            {
                if ( sender != null )
                {
                    sender.destroy();
                }
            }
            catch ( Exception e )
            {
                log.error( "Problem closing Request Broadcast sender", e );
            }
        }
    }

    /**
     * Send a message.
     */
    public void run()
    {
        UDPDiscoverySender sender = null;
        try
        {
            // create this connection each time.
            // more robust
            sender = new UDPDiscoverySender( attributes.getUdpDiscoveryAddr(), attributes.getUdpDiscoveryPort() );

            sender.passiveBroadcast( attributes.getServiceAddress(), attributes.getServicePort(), cacheNames );

            // todo we should consider sending a request broadcast every so
            // often.

            if ( log.isDebugEnabled() )
            {
                log.debug( "Called sender to issue a passive broadcast" );
            }

        }
        catch ( Exception e )
        {
            log.error( "Problem calling the UDP Discovery Sender [" + attributes.getUdpDiscoveryAddr() + ":"
                + attributes.getUdpDiscoveryPort() + "]", e );
        }
        finally
        {
            if (sender != null)
            {
                try
                {
                    sender.destroy();
                }
                catch ( Exception e )
                {
                    log.error( "Problem closing Passive Broadcast sender", e );
                }
            }
        }
    }

    /**
     * Issues a remove broadcast to the others.
     */
    protected void shutdown()
    {
        UDPDiscoverySender sender = null;
        try
        {
            // create this connection each time.
            // more robust
            sender = new UDPDiscoverySender( attributes.getUdpDiscoveryAddr(), attributes.getUdpDiscoveryPort() );

            sender.removeBroadcast( attributes.getServiceAddress(), attributes.getServicePort(), cacheNames );

            if ( log.isDebugEnabled() )
            {
                log.debug( "Called sender to issue a remove broadcast in shudown." );
            }
        }
        catch ( Exception e )
        {
            log.error( "Problem calling the UDP Discovery Sender", e );
        }
        finally
        {
            try
            {
                if ( sender != null )
                {
                    sender.destroy();
                }
            }
            catch ( Exception e )
            {
                log.error( "Problem closing Remote Broadcast sender", e );
            }
        }
    }
}
