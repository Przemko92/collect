package org.odk.collect.android.external

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.annotation.NonNull
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
import org.odk.collect.projects.ProjectsRepository
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.settings.importing.SettingsImportingResult
import javax.inject.Inject


/**
 * This class serves as a firewall for starting form filling. It should be used to do that
 * rather than [FormFillingActivity] directly as it ensures that the required data is valid.
 */
public class ProjectsProvider : ContentProvider() {

    @Inject
    lateinit var projectCreator: ProjectCreator

    @Inject
    lateinit var appConfigurationGenerator: AppConfigurationGenerator

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var projectsDeleter: ProjectDeleter

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var projectsDataService: ProjectsDataService
    override fun onCreate(): Boolean {
        return true;
    }


    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        TODO("Not supported")
    }

    override fun insert(@NonNull uri: Uri, initialValues: ContentValues?): Uri? {
        DaggerUtils.getComponent(context).inject(this)

        val projectUrl = initialValues!!["projectUrl"].toString()
        val userName = initialValues!!["userName"].toString()
        val password = initialValues!!["projectUrl"].toString()

        val settingsJson = appConfigurationGenerator.getAppConfigurationAsJsonWithServerDetails(
                projectUrl,
                userName,
                password
        )

        val result = projectCreator.createNewProject(settingsJson)
        Analytics.log(AnalyticsEvents.FROM_URI_CREATE_PROJECT)

        return when (result) {
            SettingsImportingResult.SUCCESS -> uri
            else -> null
        }
    }

    override fun delete(@NonNull uri: Uri, where: String?, whereArgs: Array<String?>?): Int {
        DaggerUtils.getComponent(context).inject(this)
        val projectId = uri.getQueryParameter("projectId")
        projectsDeleter.deleteProject(projectId!!);
        return 1;
    }

    override fun update(@NonNull uri: Uri, values: ContentValues?, where: String?, whereArgs: Array<String?>?): Int {
        DaggerUtils.getComponent(context).inject(this)

        var projectId = when (values!!.containsKey("projectId")) {
            true -> values!!["projectId"].toString()
            false -> null
        }
        val projectUrl = when (values!!.containsKey("projectUrl")) {
            true -> values!!["projectUrl"].toString()
            false -> null
        }
        val userName = when (values!!.containsKey("userName")) {
            true -> values!!["userName"].toString()
            false -> null
        }

        if (projectId == null && (projectUrl == null || userName == null)) {
            throw IllegalArgumentException("Unknown URI $uri")
        }

        when (URI_MATCHER.match(uri)) {
            SWITCH -> {
                if (projectId == null) projectId = getProjectId(projectUrl!!, userName!!)

                switchCurrentProject(projectId!!)
            }

            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        return 1;
    }

    private fun getProjectId(projectUrl: String, userName: String): String? {
        val settingsConnectionMatcher = SettingsConnectionMatcher(projectsRepository, settingsProvider)
        val settingsJson = appConfigurationGenerator.getAppConfigurationAsJsonWithServerDetails(
                projectUrl,
                userName,
                ""
        )
        return settingsConnectionMatcher.getProjectWithMatchingConnection(settingsJson); }

    private fun switchCurrentProject(uuid: String) {
        projectsDataService.setCurrentProject(uuid)
        Analytics.log(AnalyticsEvents.SWITCH_PROJECT)
    }

    override fun getType(uri: Uri): String? {
        return when (URI_MATCHER.match(uri)) {
            SWITCH -> ProjectsContract.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
    }

    companion object {
        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)
        private const val ADD = 1
        private const val SWITCH = 2

        init {
            URI_MATCHER.addURI(ProjectsContract.AUTHORITY, "projects", ADD);
            URI_MATCHER.addURI(ProjectsContract.AUTHORITY, "switch", SWITCH);
        }
    }
}
