package org.odk.collect.android.utilities

import org.odk.collect.forms.FormsRepository
import org.odk.collect.forms.instances.Instance

object InstanceAutoDeleteChecker {

    /**
     * Returns whether instances of the form specified should be auto-deleted after successful
     * update.
     *
     * If the form explicitly sets the auto-delete property, then it overrides the preference.
     */
    @JvmStatic
    fun shouldInstanceBeDeleted(
        formsRepository: FormsRepository,
        isAutoDeleteEnabledInProjectSettings: Boolean,
        instance: Instance
    ): Boolean {
        formsRepository.getLatestByFormIdAndVersion(instance.formId, instance.formVersion)?.let { form ->
            return if (isAutoDeleteEnabledInProjectSettings) {
                form.autoDelete != "false"
            } else {
                form.autoDelete.toBoolean()
            }
        }

        return false
    }
}
