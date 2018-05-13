package de.bmarwell.util

import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import org.slf4j.LoggerFactory.getLogger

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
 
class DiscoveryHelperTest {
    companion object {
        val LOG = getLogger(DiscoveryHelperTest::class.java)!!
    }

    @Test
    fun testDiscovery() {
        val discoveredRouters = discoverTr064Router(5000)
        LOG.info("Found routers: [{}].", discoveredRouters)

        assertThat(discoveredRouters.size, `is`(1))
    }


}
