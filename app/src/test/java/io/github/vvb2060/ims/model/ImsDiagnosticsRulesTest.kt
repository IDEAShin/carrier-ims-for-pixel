package io.github.vvb2060.ims.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ImsDiagnosticsRulesTest {
    @Test
    fun registeredStateHasNoBlocker() {
        assertEquals(
            ImsRegistrationBlocker.NONE,
            ImsDiagnosticsRules.findRegistrationBlocker(
                registered = true,
                advancedCallingEnabled = false,
                volteAvailable = false,
                volteProvisioningRequired = true,
                volteProvisioned = false,
            ),
        )
    }

    @Test
    fun disabledAdvancedCallingIsReportedFirst() {
        assertEquals(
            ImsRegistrationBlocker.ADVANCED_CALLING_DISABLED,
            ImsDiagnosticsRules.findRegistrationBlocker(
                registered = false,
                advancedCallingEnabled = false,
                volteAvailable = true,
                volteProvisioningRequired = true,
                volteProvisioned = true,
            ),
        )
    }

    @Test
    fun missingCarrierCapabilityIsReported() {
        assertEquals(
            ImsRegistrationBlocker.VOLTE_UNAVAILABLE,
            ImsDiagnosticsRules.findRegistrationBlocker(
                registered = false,
                advancedCallingEnabled = true,
                volteAvailable = false,
                volteProvisioningRequired = false,
                volteProvisioned = false,
            ),
        )
    }

    @Test
    fun requiredButMissingProvisioningIsReported() {
        assertEquals(
            ImsRegistrationBlocker.VOLTE_NOT_PROVISIONED,
            ImsDiagnosticsRules.findRegistrationBlocker(
                registered = false,
                advancedCallingEnabled = true,
                volteAvailable = true,
                volteProvisioningRequired = true,
                volteProvisioned = false,
            ),
        )
    }

    @Test
    fun provisioningFlagDoesNotBlockWhenCarrierDoesNotRequireIt() {
        assertEquals(
            ImsRegistrationBlocker.UNKNOWN,
            ImsDiagnosticsRules.findRegistrationBlocker(
                registered = false,
                advancedCallingEnabled = true,
                volteAvailable = true,
                volteProvisioningRequired = false,
                volteProvisioned = false,
            ),
        )
    }
}
