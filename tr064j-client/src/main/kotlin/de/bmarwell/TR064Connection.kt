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

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import de.bmarwell.util.Tr064SoapHelper
import de.bmarwell.util.realmAndNonce
import de.bmarwell.util.toSoapMessage
import de.bmarwell.util.toUnicodeString
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.URI
import javax.xml.soap.MessageFactory
import javax.xml.soap.SOAPElement


class TR064Connection(val params: TR064ConnectionParameters) : Closeable {

    var closed: Boolean = false

    private val messageFactory = MessageFactory.newInstance()!!

    fun getSecurityPort(): Int {
        val urn = "urn:dslforumorg:service:DeviceInfo:1"
        val method = "GetSecurityPort"

        val message = Tr064SoapHelper.createSoapMessageWithChallenge(
                msgFactory = messageFactory,
                action = "GetSecurityPort",
                urn = urn,
                params = params)

        val deviceInfo = params.uri.withPath("/upnp/control/deviceinfo")
        val request = deviceInfo.toASCIIString().httpPost().body("<?xml version=\"1.0\" encoding=\"utf-8\"?> ${message.toUnicodeString()}")
        request.headers.putAll(mapOf(
                "Content-type" to "text/xml; charset=\"utf-8\"",
                "SOAPACTION" to "$urn#$method"
        ))

        val result = request.responseString()
        val port = when (result.third) {
            is Result.Failure -> {
                val ex = result.third.component2()
                LOG.error("Problem posting.", ex)

                -1
            }
            is Result.Success -> {
                val responseMessage = result.second.toSoapMessage(messageFactory)

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

                port.toIntOrNull(10) ?: -1
            }
            else -> -1
        }

        return port
    }

    fun getPppInfo(): Map<String, String> {
        val urn = "urn:dslforum-org:service:WANPPPConnection:1"
        val method = "GetInfo"

        val message = Tr064SoapHelper.createSoapMessageWithChallenge(
                msgFactory = messageFactory,
                action = "GetInfo",
                urn = urn,
                params = params)

        val deviceInfo = params.uri.withPath("/upnp/control/wanpppconn1")
        LOG.info("Post to: [{}].", deviceInfo)
        val request = deviceInfo.toASCIIString().httpPost().body("<?xml version=\"1.0\" encoding=\"utf-8\"?> ${message.toUnicodeString()}")
        request.headers.putAll(mapOf(
                "Content-type" to "text/xml; charset=\"utf-8\"",
                "SOAPACTION" to "$urn#$method"
        ))

        val result = request.responseString()
        val msgResponse = when (result.third) {
            is Result.Success -> result.second.toSoapMessage(messageFactory)
            is Result.Failure -> {
                val ex = result.third.component2()
                LOG.error("Problem posting.", ex)
                messageFactory.createMessage();
            }
            else -> messageFactory.createMessage()
        }

        /* Post again */

        val realmAndNonce = msgResponse.realmAndNonce() ?: throw IllegalStateException()

        val authMessage = Tr064SoapHelper.createSoapMessageWithClientAuth(messageFactory,
                action = "GetInfo",
                params = params,
                nonceAndRealm = realmAndNonce,
                urn = "urn:dslforum-org:service:WANPPPConnection:1")

        val request2 = deviceInfo.toASCIIString().httpPost().body("<?xml version=\"1.0\" encoding=\"utf-8\"?> ${authMessage.toUnicodeString()}")
        request.headers.putAll(mapOf(
                "Content-type" to "text/xml; charset=\"utf-8\"",
                "SOAPACTION" to "$urn#$method"
        ))

        val result2 = request2.responseString()
        val msgResponse2 = when (result2.third) {
            is Result.Failure -> {
                val ex = result2.third.component2()
                LOG.error("Problem posting.", ex)

                messageFactory.createMessage();
            }
            is Result.Success -> result2.second.toSoapMessage(messageFactory)
            else -> messageFactory.createMessage();
        }

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
