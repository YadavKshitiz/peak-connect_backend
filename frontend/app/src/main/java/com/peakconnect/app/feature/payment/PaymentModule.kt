package com.peakconnect.app.feature.payment

import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PaymentEntryPoint {
    fun paymentManager(): PaymentManager
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @Singleton
    abstract fun bindPaymentEventBus(impl: PaymentEventBusImpl): PaymentEventBus

    @Binds
    @Singleton
    abstract fun bindPaymentManager(impl: RazorpayPaymentManager): PaymentManager

    @Binds
    @Singleton
    abstract fun bindPaymentReminderScheduler(impl: NoOpPaymentReminderScheduler): PaymentReminderScheduler
}
