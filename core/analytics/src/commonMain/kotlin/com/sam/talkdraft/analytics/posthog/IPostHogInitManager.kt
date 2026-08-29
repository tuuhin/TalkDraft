package com.sam.talkdraft.analytics.posthog

interface IPostHogInitManager {
    /**
     * Initializes the PostHog SDK instance with project settings (API Key, Host)
     * and global options (lifecycle tracking, autocapture)..
     */
    fun setup()

    /**
     * Opts the user out of all telemetry data collection.
     */
    fun turnOffDataCollection()

    /**
     * Opts the user back into telemetry data collection.
     */
    fun turnOnDataCollection()
}
