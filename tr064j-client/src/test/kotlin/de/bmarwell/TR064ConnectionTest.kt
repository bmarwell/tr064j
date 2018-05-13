package de.bmarwell

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
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

    @Test
    fun testOpenConnection() {
        val stdUri = URI.create("http://fritz.box:49000/")
        val connectionParameters = TR064ConnectionParameters(stdUri)

        val tR064Connection = TR064Connection(connectionParameters)
        tR064Connection.getPPPInfo()
        tR064Connection.close()

        assertThat(tR064Connection.isClosed(), `is`(true))
    }
}
