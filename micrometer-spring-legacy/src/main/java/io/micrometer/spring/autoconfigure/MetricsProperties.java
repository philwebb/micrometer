/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.spring.autoconfigure;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ConfigurationProperties} for configuring Micrometer-based metrics.
 *
 * @author Jon Schneider
 */
@ConfigurationProperties("management.metrics")
public class MetricsProperties {

    private Web web = new Web();
    private Summaries summaries = new Summaries();
    private Timers timers = new Timers();

    /**
     * If {@code false}, the matching meter(s) are no-op.
     */
    private Map<String, Boolean> enabled = new HashMap<>();

    /**
     * Whether or not auto-configured MeterRegistry implementations should be bound to the
     * global static registry on Metrics. For testing, set this to 'false' to maximize
     * test independence.
     */
    private boolean useGlobalRegistry = true;

    public boolean isUseGlobalRegistry() {
        return this.useGlobalRegistry;
    }

    public void setUseGlobalRegistry(boolean useGlobalRegistry) {
        this.useGlobalRegistry = useGlobalRegistry;
    }

    public Web getWeb() {
        return this.web;
    }

    public void setWeb(Web web) {
        this.web = web;
    }

    public Summaries getSummaries() {
        return summaries;
    }

    public void setSummaries(Summaries summaries) {
        this.summaries = summaries;
    }

    public Timers getTimers() {
        return timers;
    }

    public void setTimers(Timers timers) {
        this.timers = timers;
    }

    public Map<String, Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(Map<String, Boolean> enabled) {
        this.enabled = enabled;
    }

    public static class Web {

        private Client client = new Client();

        private Server server = new Server();

        public Client getClient() {
            return this.client;
        }

        public void setClient(Client client) {
            this.client = client;
        }

        public Server getServer() {
            return this.server;
        }

        public void setServer(Server server) {
            this.server = server;
        }

        public static class Client {
            /**
             * Name of the metric for sent requests.
             */
            private String requestsMetricName = "http.client.requests";

            /**
             * Maximum number of unique URI tag values allowed. After the max number of tag values is reached,
             * metrics with additional tag values are denied by filter.
             */
            private int maxUriTags = 100;

            public String getRequestsMetricName() {
                return this.requestsMetricName;
            }

            public void setRequestsMetricName(String requestsMetricName) {
                this.requestsMetricName = requestsMetricName;
            }

            public int getMaxUriTags() {
                return maxUriTags;
            }

            public void setMaxUriTags(int maxUriTags) {
                this.maxUriTags = maxUriTags;
            }
        }

        public static class Server {

            /**
             * Whether or not requests handled by Spring MVC or WebFlux should be
             * automatically timed. If the number of time series emitted grows too large
             * on account of request mapping timings, disable this and use 'Timed' on a
             * per request mapping basis as needed.
             */
            private boolean autoTimeRequests = true;

            /**
             * Name of the metric for received requests.
             */
            private String requestsMetricName = "http.server.requests";

            public boolean isAutoTimeRequests() {
                return this.autoTimeRequests;
            }

            public void setAutoTimeRequests(boolean autoTimeRequests) {
                this.autoTimeRequests = autoTimeRequests;
            }

            public String getRequestsMetricName() {
                return this.requestsMetricName;
            }

            public void setRequestsMetricName(String requestsMetricName) {
                this.requestsMetricName = requestsMetricName;
            }
        }
    }

    /**
     * Properties common to "distribution" style meters - timers and distribution summaries.
     */
    static abstract class AbstractDistributions {
        /**
         * Controls whether to publish a histogram structure for those monitoring systems that support
         * aggregable percentile calculation based on a histogram. For other systems, this has no effect.
         */
        private Map<String, Boolean> percentileHistogram = new HashMap<>();

        /**
         * The set of Micrometer-computed non-aggregable percentiles to ship to the backend. Percentiles should
         * be defined in the range of (0, 1]. For example, 0.999 represents the 99.9th percentile of the distribution.
         */
        private Map<String, double[]> percentiles = new HashMap<>();

        /**
         * Statistics emanating from a distribution like max, percentiles, and histogram counts decay over time to
         * give greater weight to recent samples (exception: histogram counts are cumulative for those systems that expect cumulative
         * histogram buckets). Samples are accumulated to such statistics in ring buffers which rotate after
         * this expiry, with a buffer length of {@link #histogramBufferLength}.
         */
        private Map<String, Duration> histogramExpiry = new HashMap<>();

        /**
         * Statistics emanating from a distribution like max, percentiles, and histogram counts decay over time to
         * give greater weight to recent samples (exception: histogram counts are cumulative for those systems that expect cumulative
         * histogram buckets). Samples are accumulated to such statistics in ring buffers which rotate after
         * {@link #histogramExpiry}, with this buffer length.
         */
        private Map<String, Integer> histogramBufferLength = new HashMap<>();

        public Map<String, Boolean> getPercentileHistogram() {
            return percentileHistogram;
        }

        public void setPercentileHistogram(Map<String, Boolean> percentileHistogram) {
            this.percentileHistogram = percentileHistogram;
        }

        public Map<String, double[]> getPercentiles() {
            return percentiles;
        }

        public void setPercentiles(Map<String, double[]> percentiles) {
            this.percentiles = percentiles;
        }

        public Map<String, Duration> getHistogramExpiry() {
            return histogramExpiry;
        }

        public void setHistogramExpiry(Map<String, Duration> histogramExpiry) {
            this.histogramExpiry = histogramExpiry;
        }

        public Map<String, Integer> getHistogramBufferLength() {
            return histogramBufferLength;
        }

        public void setHistogramBufferLength(Map<String, Integer> histogramBufferLength) {
            this.histogramBufferLength = histogramBufferLength;
        }
    }

    public static class Summaries extends AbstractDistributions {
        /**
         * Clamps {@link DistributionSummary} to the first percentile bucket greater than
         * or equal to the supplied value. Use this property to control the number of histogram buckets used
         * to represent a distribution.
         */
        private Map<String, Long> minimumExpectedValue = new HashMap<>();

        /**
         * Clamps {@link DistributionSummary} to the percentile buckets less than
         * or equal to the supplied value. Use this property to control the number of histogram buckets used
         * to represent a distribution.
         */
        private Map<String, Long> maximumExpectedValue = new HashMap<>();

        /**
         * Publish a counter for each SLA boundary that counts violations of the SLA.
         */
        private Map<String, long[]> sla = new HashMap<>();

        public Map<String, Long> getMinimumExpectedValue() {
            return minimumExpectedValue;
        }

        public void setMinimumExpectedValue(Map<String, Long> minimumExpectedValue) {
            this.minimumExpectedValue = minimumExpectedValue;
        }

        public Map<String, Long> getMaximumExpectedValue() {
            return maximumExpectedValue;
        }

        public void setMaximumExpectedValue(Map<String, Long> maximumExpectedValue) {
            this.maximumExpectedValue = maximumExpectedValue;
        }

        public Map<String, long[]> getSla() {
            return sla;
        }

        public void setSla(Map<String, long[]> sla) {
            this.sla = sla;
        }
    }

    public static class Timers extends AbstractDistributions {
        /**
         * Clamps {@link Timer} to the first percentile bucket greater than
         * or equal to the supplied value. Use this property to control the number of histogram buckets used
         * to represent a distribution.
         */
        Map<String, Duration> minimumExpectedValue = new HashMap<>();

        /**
         * Clamps {@link Timer} to the percentile buckets less than
         * or equal to the supplied value. Use this property to control the number of histogram buckets used
         * to represent a distribution.
         */
        Map<String, Duration> maximumExpectedValue = new HashMap<>();

        /**
         * Publish a counter for each SLA boundary that counts violations of the SLA.
         */
        Map<String, Duration[]> sla = new HashMap<>();

        public Map<String, Duration> getMinimumExpectedValue() {
            return minimumExpectedValue;
        }

        public void setMinimumExpectedValue(Map<String, Duration> minimumExpectedValue) {
            this.minimumExpectedValue = minimumExpectedValue;
        }

        public Map<String, Duration> getMaximumExpectedValue() {
            return maximumExpectedValue;
        }

        public void setMaximumExpectedValue(Map<String, Duration> maximumExpectedValue) {
            this.maximumExpectedValue = maximumExpectedValue;
        }

        public Map<String, Duration[]> getSla() {
            return sla;
        }

        public void setSla(Map<String, Duration[]> sla) {
            this.sla = sla;
        }
    }
}
