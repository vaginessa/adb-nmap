/*
 * adb-nmap: An ADB network device discovery and connection library
 * Copyright (C) 2017-present Arav Singhal and adb-nmap contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * The full license can be found in LICENSE.md.
 */

package net.eviltak.adbnmap.net

import net.eviltak.adbnmap.net.protocol.Protocol
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Scans networks for devices which use the specified protocol.
 *
 * @param P The type of protocol devices are expected to use.
 *
 * @property protocolFactory Creates an instance of the protocol of type [P] from the socket to be used
 * to connect to the host.
 */
class NetworkMapper<out P : Protocol<*>>(val socketConnector: SocketConnector,
                                         val protocolFactory: (Socket) -> P) {
    /**
     * Pings the host at [socketAddress] if such a host exists to check whether it supports the
     * specified protocol.
     *
     * @param socketAddress The socket address of the host to ping.
     * @return True if the host at [socketAddress] exists and supports the specified protocol.
     */
    fun ping(socketAddress: SocketAddress): Boolean {
        return socketConnector.tryConnect(socketAddress) {
            protocolFactory(it).hostUsesProtocol()
        }
    }

    /**
     * Pings all hosts in [network] and returns the address of all devices that support the
     * protocol [P].
     *
     * @param network A collection of addresses representing the network.
     * @return A [List] containing the addresses of all devices in [network] that support the
     * protocol [P].
     */
    fun getDevicesInNetwork(network: Iterable<SocketAddress>): List<SocketAddress> {
        val executor = Executors.newCachedThreadPool()
        val futures = executor.invokeAll(network.map { Callable { if (ping(it)) it else null } })

        return futures.map { it.get() }.filterNotNull()
    }
}

