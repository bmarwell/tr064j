package de.bmarwell.util

import org.slf4j.LoggerFactory.getLogger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.regex.Pattern


/*
 *  Copyright 2018 The tr064j contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

fun discoverTr064Router() {
    discoverTr064Router(Defaults.defaultTimeout)
}

fun discoverTr064Router(discoveryTimeout: Int) : Set<Device> {
    val devices = mutableSetOf<Device>()

    /* Create the search request */
    val msearch = StringBuilder("M-SEARCH * HTTP/1.1\n" +
            "HOST: 239.255.255.250:1900\n" +
            "MAN: \"ssdp:discover\"\n" +
            "MX: 5\n")
    msearch.append("ST: ").append(Defaults.defaultDiscoveryServiceType).append("\n")

    /* Send the request */
    val sendData = msearch.toString().toByteArray()
    val sendPacket = DatagramPacket(
            sendData, sendData.size, InetAddress.getByName("239.255.255.250"), 1900)
    val clientSocket = DatagramSocket()
    clientSocket.soTimeout = discoveryTimeout
    clientSocket.send(sendPacket)

    Defaults.LOG.info("send: [{}].", String(sendPacket.data))

    /* Receive all responses */
    val receiveData = ByteArray(1024)
    while (true) {
        try {
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            clientSocket.receive(receivePacket)
            try {
                Defaults.LOG.info("Receiving package")
                devices.add(Device.parse(receivePacket))
            } catch (e: IllegalStateException) {
                Defaults.LOG.error("Problem adding packages as Devices: ", e)
            }
        } catch (e: SocketTimeoutException) {
            break
        }

    }

    return devices.toSet()
}

object Defaults {
    const val defaultTimeout: Int = 1000
    const val defaultDiscoveryServiceType = "urn:dslforum-org:device:InternetGatewayDevice:1\n"
    val LOG = getLogger("DiscoveryHelper")!!
}

data class Device(val ip: String, val descriptionUrl: String, val server: String, val serviceType: String, val usn: String) {
    companion object
}

fun Device.Companion.parse(packet : DatagramPacket) : Device {
    val headers = mutableMapOf<String, String>()
    val pattern = Pattern.compile("(.*): (.*)")

    val lines = String(packet.data).split("\r\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

    for (line in lines) {
        val matcher = pattern.matcher(line)
        if (matcher.matches()) {
            headers[matcher.group(1).toUpperCase()] = matcher.group(2)
        }
    }

    return Device(
            ip = packet.address.hostAddress,
            descriptionUrl = headers.get("LOCATION") ?: "no description",
            server = headers.get("LOCATION") ?: throw IllegalStateException("no LOCATION"),
            serviceType = headers.get("ST") ?: throw IllegalStateException("no ST"),
            usn = headers.get("USN") ?: throw IllegalStateException("no USN")
    )
}
