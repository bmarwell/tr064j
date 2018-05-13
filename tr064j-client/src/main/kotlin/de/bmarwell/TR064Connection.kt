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

import java.io.Closeable
import java.net.URI
import javax.xml.namespace.QName
import org.slf4j.LoggerFactory
import javax.xml.soap.MessageFactory
import javax.xml.soap.SOAPFactory
import java.io.ByteArrayOutputStream
import javax.xml.soap.SOAPMessage



class TR064Connection(val params: TR064ConnectionParameters) : Closeable {

    var closed: Boolean = false

    val messageFactory = MessageFactory.newInstance()

    fun getPPPInfo() : String {
        val message = messageFactory.createMessage()
        val soapBody = message.soapBody

        val soapFactory = SOAPFactory.newInstance()
        // TODO: Insert FB PPP here
        val bodyName = soapFactory.createName("GetInfo", "u", "urn:dslforumorg:service:DeviceInfo:1")
        soapBody.addBodyElement(bodyName)

        val out = ByteArrayOutputStream()
        message.writeTo(out)
        val strMsg = String(out.toByteArray())

        log.debug("invoke: [{}].", strMsg)

        TODO("implement correctly")
    }

    override fun close() {
        closed = true
    }

    fun isClosed(): Boolean = closed

    companion object {
        val log = LoggerFactory.getLogger(TR064Connection::class.java)
    }
}

data class TR064ConnectionParameters(val uri: URI);
