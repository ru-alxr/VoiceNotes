package mx.alxr.voicenotes.repository.language

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.logger.ILogger
import org.json.JSONArray
import org.json.JSONObject
import java.lang.NullPointerException

/**
 * List of languages is here: https://cloud.google.com/speech-to-text/docs/languages
 */
class LanguageRepository(private val configRepo: IConfigRepository,
                         private val logger: ILogger,
                         db: AppDatabase
) : ILanguageRepository {

    private val mUserDAO: LanguageDAO = db.languageDataDAO()

    override fun loadAvailableLanguages(): Single<Unit> {
        return configRepo
            .getLanguages()
            .observeOn(Schedulers.io())
            .flatMap {
                try {
                    val array = JSONArray(it)
                    val languages = ArrayList<LanguageEntity>(array.length())
                    for (index in 0 until array.length()) {
                        val lang:JSONObject = array[index] as? JSONObject ?: continue
                        lang.toLang()?.apply { languages.add(this) }
                    }
                    if (languages.isEmpty()) throw NullPointerException()
                    logger.with(this).add("Insert ${languages.size} languages").log()
                    mUserDAO.insert(languages)
                } catch (e: Exception) {
                    logger.with(this).add("Parse language list error $e").log()
                    throw ProjectException(R.string.fetching_supported_languages_list_error)
                }
                Single.just(Unit)
            }
    }

    private fun JSONObject.toLang(): LanguageEntity? {
        return try {
            val code: String = getString("code")
            val name: String = getString("name")
            val nameEng: String = getString("name_en")
            LanguageEntity(code = code, name = name, nameEng = nameEng)
        } catch (e: Exception) {
            logger.with(this).add("Parse language error $e").log()
            null
        }
    }

}