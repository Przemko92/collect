package org.odk.collect.android.external

import android.content.Intent
import android.content.UriMatcher
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.odk.collect.analytics.Analytics
import org.odk.collect.android.activities.ActivityUtils
import org.odk.collect.android.activities.FormFillingActivity
import org.odk.collect.android.analytics.AnalyticsEvents
import org.odk.collect.android.configure.qr.AppConfigurationGenerator
import org.odk.collect.android.injection.DaggerUtils
import org.odk.collect.android.mainmenu.MainMenuActivity
import org.odk.collect.android.projects.ProjectCreator
import org.odk.collect.android.projects.ProjectDeleter
import org.odk.collect.android.projects.ProjectsDataService
import org.odk.collect.android.projects.SettingsConnectionMatcher
import org.odk.collect.androidshared.utils.Validator
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.strings.localization.getLocalizedString
import javax.inject.Inject

/**
 * This class serves as a firewall for starting form filling. It should be used to do that
 * rather than [FormFillingActivity] directly as it ensures that the required data is valid.
 */
class ProjectUriActivity : ComponentActivity() {

    @Inject
    lateinit var projectCreator: ProjectCreator

    @Inject
    lateinit var appConfigurationGenerator: AppConfigurationGenerator

    @Inject
    lateinit var projectsDataService: ProjectsDataService

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var projectsDeleter: ProjectDeleter


    private lateinit var settingsConnectionMatcher: SettingsConnectionMatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerUtils.getComponent(this).inject(this)
        settingsConnectionMatcher = SettingsConnectionMatcher(projectsRepository, settingsProvider)

        when {
            !assertValidUri() -> Unit
            !assertHasValidParameters() -> Unit
            else -> startProject()
        }
    }

    private fun assertValidUri(): Boolean {
        val isUriValid = intent.data?.let {
            return when (URI_MATCHER.match(it)) {
                PROJECTS -> true
                else -> false;
            }
        } ?: false

        return if (!isUriValid) {
            displayErrorDialog(getString(org.odk.collect.strings.R.string.unrecognized_uri))
            false
        } else {
            true
        }
    }

    private fun assertHasValidParameters(): Boolean {
        val projectUrl = intent.getStringExtra("projectUrl")!!
        val userName = intent.getStringExtra("userName")!!
        val password = intent.getStringExtra("password")!!
        return projectUrl.isNotBlank() && userName.isNotBlank() && password.isNotBlank();
    }

    private fun startProject() {
        val projectUrl = intent.getStringExtra("projectUrl")!!
        val userName = intent.getStringExtra("userName")!!
        val password = intent.getStringExtra("password")!!


        if (!Validator.isUrlValid(projectUrl)) {
            displayErrorDialog(getLocalizedString(org.odk.collect.strings.R.string.url_error));
        } else {
            val settingsJson = appConfigurationGenerator.getAppConfigurationAsJsonWithServerDetails(
                    projectUrl,
                    userName,
                    password
            )

            settingsConnectionMatcher.getProjectWithMatchingConnection(settingsJson)?.let { uuid ->
                when (intent.action) {

                    Intent.ACTION_INSERT_OR_EDIT -> switchToProject(uuid)
                    Intent.ACTION_DELETE -> deleteProject(uuid)
                    Intent.ACTION_INSERT -> switchOrAddProject(uuid, settingsJson)
                    else ->  displayErrorDialog(getLocalizedString(org.odk.collect.strings.R.string.url_error));
                }
            } ?: run {
                when (intent.action) {
                    Intent.ACTION_INSERT_OR_EDIT -> createProject(settingsJson)
                    Intent.ACTION_INSERT -> createProject(settingsJson)
                    else ->  displayErrorDialog(getLocalizedString(org.odk.collect.strings.R.string.url_error));
                }
            }
        }
    }

    private fun switchOrAddProject(uuid: String, settingsJson: String) {
        MaterialAlertDialogBuilder(this)
                .setTitle(org.odk.collect.strings.R.string.duplicate_project)
                .setMessage(org.odk.collect.strings.R.string.duplicate_project_details)
                .setPositiveButton(org.odk.collect.strings.R.string.add_duplicate_project) { _, _ -> createProject(settingsJson) }
                .setNegativeButton(org.odk.collect.strings.R.string.switch_to_existing) { _, _ ->
                    run {
                        switchToProject(uuid)
                    }
                }
                .create()
                .show()
    }

    private fun deleteProject(uuid: String) {
        projectsDeleter.deleteProject(uuid)
        Analytics.log(AnalyticsEvents.DELETE_PROJECT)
    }

    private fun createProject(settingsJson: String) {
        projectCreator.createNewProject(settingsJson)
        Analytics.log(AnalyticsEvents.FROM_URI_CREATE_PROJECT)
        ActivityUtils.startActivityAndCloseAllOthers(this, MainMenuActivity::class.java)
    }

    private fun switchToProject(uuid: String) {
        projectsDataService.setCurrentProject(uuid)
        Analytics.log(AnalyticsEvents.DUPLICATE_PROJECT_SWITCH)
        ActivityUtils.startActivityAndCloseAllOthers(this, MainMenuActivity::class.java)
    }

    private fun displayErrorDialog(message: String) {
        MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(org.odk.collect.strings.R.string.ok) { _, _ -> finish() }
                .create()
                .show()
    }

    companion object {
        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)
        private const val PROJECTS = 1

        init {
            URI_MATCHER.addURI(ProjectsContract.AUTHORITY, "projects", PROJECTS);
        }
    }
}
