package io.github.vvb2060.ims.model

enum class ImsRegistrationBlocker {
    NONE,
    ADVANCED_CALLING_DISABLED,
    VOLTE_UNAVAILABLE,
    VOLTE_NOT_PROVISIONED,
    UNKNOWN,
}

object ImsDiagnosticsRules {
    fun findRegistrationBlocker(
        registered: Boolean?,
        advancedCallingEnabled: Boolean?,
        volteAvailable: Boolean?,
        volteProvisioningRequired: Boolean?,
        volteProvisioned: Boolean?,
    ): ImsRegistrationBlocker {
        if (registered == true) return ImsRegistrationBlocker.NONE
        if (advancedCallingEnabled == false) {
            return ImsRegistrationBlocker.ADVANCED_CALLING_DISABLED
        }
        if (volteAvailable == false) return ImsRegistrationBlocker.VOLTE_UNAVAILABLE
        if (volteProvisioningRequired == true && volteProvisioned == false) {
            return ImsRegistrationBlocker.VOLTE_NOT_PROVISIONED
        }
        return ImsRegistrationBlocker.UNKNOWN
    }
}
