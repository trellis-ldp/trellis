/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.app.triplestore;

import static org.trellisldp.app.AppUtils.printBanner;

import io.dropwizard.setup.Environment;

import org.trellisldp.dropwizard.AbstractTrellisApplication;
import org.trellisldp.http.core.ServiceBundler;

/**
 * A deployable Trellis application.
 */
public class TrellisApplication extends AbstractTrellisApplication<AppConfiguration> {

    private ServiceBundler serviceBundler;

    /**
     * The main entry point.
     *
     * @param args the argument list
     * @throws Exception if something goes horribly awry
     */
    public static void main(final String[] args) throws Exception {
        new TrellisApplication().run(args);
    }

    @Override
    protected ServiceBundler getServiceBundler() {
        return serviceBundler;
    }

    @Override
    protected void initialize(final AppConfiguration config, final Environment environment) {
        super.initialize(config, environment);
        this.serviceBundler = new TrellisServiceBundler(config, environment);
        printBanner("Trellis Triplestore Application", "org/trellisldp/app/banner.txt");
    }
}
