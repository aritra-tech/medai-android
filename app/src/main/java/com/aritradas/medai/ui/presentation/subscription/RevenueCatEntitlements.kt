package com.aritradas.medai.ui.presentation.subscription

import com.revenuecat.purchases.CustomerInfo

private const val PRO_ENTITLEMENT_ID = "pro"

fun CustomerInfo.hasProEntitlement(): Boolean =
    entitlements.active.containsKey(PRO_ENTITLEMENT_ID)
