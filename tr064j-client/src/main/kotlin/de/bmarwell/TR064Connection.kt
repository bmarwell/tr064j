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

package de.bmarwell

import de.bmarwell.util.Tr064SoapHelper
import de.bmarwell.util.realmAndNonce
import de.bmarwell.util.toSoapMessage
import de.bmarwell.util.toUnicodeString
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.xml.soap.MessageFactory
import javax.xml.soap.SOAPElement


class TR064Connection(val params: TR064ConnectionParameters) : Closeable {

    var closed: Boolean = false

    private val messageFactory = MessageFactory.newInstance()!!

    fun getSecurityPort(): Int {
        val message = Tr064SoapHelper.createSoapMessageWithChallenge(
                msgFactory = messageFactory,
                action = "GetSecurityPort",
                urn = "urn:dslforumorg:service:DeviceInfo:1",
                params = params)

        val msg = ByteArrayOutputStream()
        message.writeTo(msg)
        val soap = String(msg.toByteArray())
        msg.close()

        val deviceInfo = params.uri.withPath("/upnp/control/deviceinfo")

        val post = khttp.post(
                url = deviceInfo.toASCIIString(),
                headers = mapOf(
                        "Content-type" to "text/xml; charset=\"utf-8\"",
                        "SOAPACTION" to "urn:dslforum-org:service:DeviceInfo:1#GetSecurityPort"
                ),
                data = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$soap",
                allowRedirects = true,
                timeout = 1000.0
        )

        LOG.info("Response: [{}], [{}]",
                post.statusCode,
                String(post.content, StandardCharsets.UTF_8))

        val responseMessage = post.toSoapMessage(messageFactory)

        val infoResponse = responseMessage.soapBody.childElements.asSequence()
                .filter { it is SOAPElement }
                .map { it as SOAPElement }
                .filter { "GetSecurityPortResponse" == it.elementName.localName }
                .firstOrNull() ?: return -1

        val port = infoResponse.childElements.asSequence()
                .filter { it is SOAPElement }
                .map { it as SOAPElement }
                .find { "NewSecurityPort" == it.localName }
                ?.textContent ?: return -1

        return port.toIntOrNull(10) ?: -1
    }

    fun getPppInfo(): Map<String, String> {
        val message = Tr064SoapHelper.createSoapMessageWithChallenge(
                msgFactory = messageFactory,
                action = "GetInfo",
                urn = "urn:dslforum-org:service:WANPPPConnection:1",
                params = params)

        val soap = message.toUnicodeString()

        val deviceInfo = params.uri.withPath("/upnp/control/wanpppconn1")

        val post = khttp.post(
                url = deviceInfo.toASCIIString(),
                headers = mapOf(
                        "Content-type" to "text/xml; charset=\"utf-8\"",
                        "SOAPACTION" to "urn:dslforum-org:service:WANPPPConnection:1#GetInfo"
                ),
                data = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$soap",
                allowRedirects = true,
                timeout = 1000.0
        )

        val msgResponse = post.toSoapMessage(messageFactory)

        LOG.info("Response: [{}], [{}]",
                post.statusCode,
                msgResponse.toUnicodeString())

        /* Post again */

        val realmAndNonce = msgResponse.realmAndNonce() ?: throw IllegalStateException()

        val authMessage = Tr064SoapHelper.createSoapMessageWithClientAuth(messageFactory,
                action = "GetInfo",
                params = params,
                nonceAndRealm = realmAndNonce,
                urn = "urn:dslforum-org:service:WANPPPConnection:1")
        val authMessageAsXml = authMessage.toUnicodeString()

        val postWithAuth = khttp.post(
                url = deviceInfo.toASCIIString(),
                headers = mapOf(
                        "Content-type" to "text/xml; charset=\"utf-8\"",
                        "SOAPACTION" to "urn:dslforum-org:service:WANPPPConnection:1#GetInfo"
                ),
                data = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$authMessageAsXml",
                allowRedirects = true,
                timeout = 1000.0
        )

        val msgResponse2 = postWithAuth.toSoapMessage(messageFactory)

        LOG.info("Response: [{}], [{}]",
                post.statusCode,
                msgResponse2.toUnicodeString())

        val infoResponse = msgResponse2.soapBody.childElements.asSequence()
                .filter { it is SOAPElement }
                .map { it as SOAPElement }
                .filter { "GetInfoResponse" == it.elementName.localName }
                .firstOrNull() ?: return mapOf()

        return infoResponse.childElements.asSequence()
                .filter { it is SOAPElement }
                .map { it as SOAPElement }
                .map { it.elementName.localName to it.textContent }
                .toMap()
    }

    override fun close() {
        closed = true
    }

    fun isClosed(): Boolean = closed

    companion object {
        val LOG = LoggerFactory.getLogger(TR064Connection::class.java)!!
    }
}

data class TR064ConnectionParameters(val uri: URI, val userId: String?, val password: String?)

fun URI.withPath(newPath: String): URI = URI(
        this.scheme,
        this.userInfo,
        this.host,
        this.port,
        newPath,
        this.query,
        this.fragment
)
