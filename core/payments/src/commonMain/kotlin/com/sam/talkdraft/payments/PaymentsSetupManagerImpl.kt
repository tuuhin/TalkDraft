package com.sam.talkdraft.payments

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.sam.talkdraft.commons.AppSecretProperties
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPaymentsSetupManager::class])
internal class PaymentsSetupManagerImpl : IPaymentsSetupManager {

    override fun configure() {
        val config = PurchasesConfiguration(apiKey = AppSecretProperties.REVENUE_CAT_API_KEY)
        // marking this debug for now
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(config)
    }
}
