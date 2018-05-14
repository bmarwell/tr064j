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

package de.bmarwell.util

import de.bmarwell.TR064ConnectionParameters
import khttp.responses.Response
import org.slf4j.LoggerFactory.getLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.namespace.QName
import javax.xml.soap.*


object Tr064SoapHelper {
    val LOG = getLogger(Tr064SoapHelper::class.java)

    private fun createSoapMessage(
            msgFactory: MessageFactory,
            action: String,
            urn: String
    ): SOAPMessage {
        val message = msgFactory.createMessage()
        val soapBody = message.soapBody

        val soapFactory = SOAPFactory.newInstance()
        val bodyName = soapFactory.createName(action, "u", urn)
        soapBody.addBodyElement(bodyName)

        // Set prefix to s
        soapBody.prefix = "s"
        message.soapHeader.prefix = "s"
        with(message.soapPart.envelope) {
            prefix = "s"
            addAttribute(javax.xml.namespace.QName("s:encodingStyle"), "http://schemas.xmlsoap.org/soap/encoding/")
            removeNamespaceDeclaration("SOAP-ENV")
            addNamespaceDeclaration("s", "http://schemas.xmlsoap.org/soap/envelope/")
        }

        return message
    }

    fun createSoapMessageWithChallenge(
            msgFactory: MessageFactory,
            action: String,
            urn: String,
            params: TR064ConnectionParameters)
            : SOAPMessage {
        val message = createSoapMessage(msgFactory, action, urn)

        message.addChallengeHeader(params.userId);

        return message
    }

    fun createSoapMessageWithClientAuth(
            msgFactory: MessageFactory,
            action: String,
            urn: String,
            params: TR064ConnectionParameters,
            nonceAndRealm: NonceAndRealm
    ): SOAPMessage {
        val message = createSoapMessage(msgFactory, action, urn)

        val auth = createAuthToken(
                realm = nonceAndRealm.realm,
                nonce = nonceAndRealm.nonce,
                password = params.password ?: "",
                uid = params.userId ?: "")
        val authData = ClientAuthData(
                nonceAndRealm = nonceAndRealm,
                auth = auth,
                userId = params.userId ?: "")

        message.addClientAuthHeader(authData)

        return message
    }

    fun createAuthToken(realm: String, uid: String, nonce: String, password: String): String {
        val secret = "$uid:$realm:$password".md5()
        return "$secret:$nonce".md5()
    }
}

fun Response.toSoapMessage(msgFactory: MessageFactory): SOAPMessage {
    val mimeHeaders = MimeHeaders().apply {
        headers.forEach { entry -> setHeader(entry.key, entry.value) }
    }

    return msgFactory.createMessage(
            mimeHeaders,
            ByteArrayInputStream(content)
    )
}

fun SOAPMessage.toUnicodeString(): String {
    val stream = ByteArrayOutputStream()
    writeTo(stream)
    stream.close()

    return String(stream.toByteArray(), StandardCharsets.UTF_8)
}

fun SOAPMessage.addChallengeHeader(uid: String?) {
    if (uid == null) {
        return;
    }

    val soapFactory = SOAPFactory.newInstance()
    val challengeHeader = soapFactory.createElement(QName("http://soap-authentication.org/digest/2001/10/", "InitChallenge", "h"))
    challengeHeader.addAttribute(QName("s:mustUnderstand"), "1")

    val userName = soapFactory.createElement(QName("UserID"))
    userName.value = uid
    challengeHeader.addChildElement(userName)

    soapHeader.addChildElement(challengeHeader)
}

fun SOAPMessage.addClientAuthHeader(clientAuthData: ClientAuthData) {
    val soapFactory = SOAPFactory.newInstance()
    val authHEader = soapFactory.createElement(QName("http://soap-authentication.org/digest/2001/10/", "ClientAuth", "h"))
    authHEader.addAttribute(QName("s:mustUnderstand"), "1")

    val userId = soapFactory.createElement(QName("UserID"))
    userId.value = clientAuthData.userId
    authHEader.addChildElement(userId)

    val nonce = soapFactory.createElement(QName("Nonce"))
    nonce.value = clientAuthData.nonce
    authHEader.addChildElement(nonce)

    val auth = soapFactory.createElement(QName("Auth"))
    auth.value = clientAuthData.auth
    authHEader.addChildElement(auth)

    val realm = soapFactory.createElement(QName("Realm"))
    realm.value = clientAuthData.realm
    authHEader.addChildElement(realm)

    soapHeader.addChildElement(authHEader)
}

fun SOAPMessage.realmAndNonce(): NonceAndRealm? {
    val childElements = soapHeader?.childElements?.asSequence()?.filter { it is SOAPElement }?.toList()
            ?: listOf()
    val challengeResp: SOAPElement? = childElements.find { it is SOAPElement } as SOAPElement?
    val nonceElement: SOAPElement? = challengeResp?.getChildElements(QName("Nonce"))?.next() as SOAPElement?
    val realmElement: SOAPElement? = challengeResp?.getChildElements(QName("Realm"))?.next() as SOAPElement?

    if (realmElement != null && nonceElement != null) {
        return NonceAndRealm(
                realm = realmElement.textContent,
                nonce = nonceElement.textContent
        )
    }

    return null
}

data class NonceAndRealm(val nonce: String, val realm: String)

data class ClientAuthData(val nonce: String, val realm: String, val auth: String, val userId: String) {
    constructor(nonceAndRealm: NonceAndRealm, auth: String, userId: String) : this(nonceAndRealm.nonce, nonceAndRealm.realm, auth, userId)
}
