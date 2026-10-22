package com.sam.talkdraft.payments.di

import com.sam.talkdraft.payments.IPaymentsSetupManager
import com.sam.talkdraft.payments.PaymentsSetupManagerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.sam.talkdraft.payments")
class PaymentsModule {

    @Singleton
    internal fun paymentsSetupManager(): IPaymentsSetupManager = PaymentsSetupManagerImpl()
}
