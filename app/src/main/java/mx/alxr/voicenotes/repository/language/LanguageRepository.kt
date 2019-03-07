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
import java.util.*

/**
 * List of languages is here: https://cloud.google.com/speech-to-text/docs/languages
 */
class LanguageRepository(
    private val configRepo: IConfigRepository,
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
                    val current = Locale.getDefault().toString().replace("_", "-")
                    logger.with(this).add("LOCALE $current").log()
                    val array = JSONArray(it)
                    logger.with(this).add(it).log()
                    val languages = ArrayList<LanguageEntity>(array.length())
                    var order = 0
                    for (index in 0 until array.length()) {
                        val lang: JSONObject = array[index] as? JSONObject ?: continue
                        val code = lang.optString("code") ?: continue
                        val orderToApply: Int = if (code == current) -1 else ++order
                        val entity = lang.toLang(orderToApply) ?: continue
                        languages.add(entity)
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

    private fun JSONObject.toLang(order: Int): LanguageEntity? {
        return try {
            val code: String = getString("code")
            val name: String = getString("name")
            val nameEng: String = getString("name_en")
            logger.with(this).add("Parse  $this").log()
            val l = LanguageEntity(code = code, name = name, nameEng = nameEng, position = order)
            logger.with(this).add("Parse result is $l").log()
            l
        } catch (e: Exception) {
            logger.with(this).add("Parse language error $e").log()
            null
        }
    }

}