package de.bmarwell

import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import java.net.URI

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
 
class TR064ConnectionTest {

    @get:Rule
    val rule = WireMockRule(options().dynamicPort())

    @Test
    fun testOpenConnection() {
        val stdUri = URI.create(rule.url(""))
        val connectionParameters = TR064ConnectionParameters(uri = stdUri, userId = "admin", password = "gurkensalat")

        val tR064Connection = TR064Connection(connectionParameters)
        val pppInfo = tR064Connection.getPppInfo()
        tR064Connection.close()

        assertThat(tR064Connection.isClosed(), `is`(true))
        assertThat(pppInfo["NewUserName"], `is`("aUserName"))
    }

    @Test
    fun testSecurityPort() {
        val stdUri = URI.create(rule.url(""))
        val connectionParameters = TR064ConnectionParameters(uri = stdUri, userId = "admin", password = "gurkensalat")

        val tR064Connection = TR064Connection(connectionParameters)
        val portNum = tR064Connection.getSecurityPort()
        tR064Connection.close()

        assertThat(tR064Connection.isClosed(), `is`(true))
        assertThat(portNum, `is`(49443))
    }
}
