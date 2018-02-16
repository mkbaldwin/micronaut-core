/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.storefront

import example.storefront.client.v1.PetClient
import example.storefront.client.v1.VendorClient
import groovy.transform.CompileStatic
import org.particleframework.context.event.ApplicationEventListener
import org.particleframework.runtime.server.event.ServerStartupEvent

import javax.inject.Singleton

/**
 * @author graemerocher
 * @since 1.0
 */
@CompileStatic
@Singleton
class Startup implements ApplicationEventListener<ServerStartupEvent> {

    final PetClient petClient
    final VendorClient vendorClient

    Startup(PetClient petClient, VendorClient vendorClient) {
        this.petClient = petClient
        this.vendorClient = vendorClient
    }

    @Override
    void onApplicationEvent(ServerStartupEvent event) {
        println petClient.list().blockingGet()
    }
}