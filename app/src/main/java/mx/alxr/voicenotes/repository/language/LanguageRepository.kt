package mx.alxr.voicenotes.repository.language

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.db.AppDatabase
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.repository.user.UserDAO
import mx.alxr.voicenotes.utils.errors.ProjectException
import mx.alxr.voicenotes.utils.logger.ILogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

const val MAX_LANGUAGES = 1000

/**
 * List of languages is here: https://cloud.google.com/speech-to-text/docs/languages
 */
class LanguageRepository(
    private val configRepo: IConfigRepository,
    private val logger: ILogger,
    db: AppDatabase
) : ILanguageRepository {

    private val mLanguageDAO: LanguageDAO = db.languageDataDAO()
    private val mUserDAO: UserDAO = db.userDataDAO()

    override fun loadAvailableLanguages(): Single<Unit> {
        return Single
            .fromCallable {
                val count = mLanguageDAO.getCount()
                count > 0
            }
            .flatMap {
                if (it) Single.just(Unit)
                else fetchLanguages()
            }
            .observeOn(Schedulers.io())
    }

    private fun fetchLanguages(): Single<Unit> {
        return configRepo
            .getLanguages()
            .flatMap {
                try {
                    val array = JSONArray(it)
                    logger.with(this).add(it).log()
                    val languages = ArrayList<LanguageEntity>(array.length())
                    var order = 0
                    for (index in 0 until array.length()) {
                        (array[index] as? JSONObject)?.toLang()?.apply { languages.add(this) }
                    }
                    if (languages.isEmpty()) throw NullPointerException()
                    val user = mUserDAO.getUserImmediately()
                    val userLanguageCode = user?.languageCode ?: ""
                    languages.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })
                    val reorder = ArrayList<LanguageEntity>(array.length())
                    val priorityCodes = getPriorityCodes(userLanguageCode)
                    for (l in languages) {
                        val orderToApply: Int = if (priorityCodes.contains(l.code)) - MAX_LANGUAGES else ++order
                        reorder.add(l.copy(position = orderToApply))
                    }
                    logger.with(this).add("Insert ${languages.size} languages").log()
                    mLanguageDAO.insert(reorder)
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
            val l = LanguageEntity(code = code, name = name, nameEng = nameEng)
            l
        } catch (e: Exception) {
            logger.with(this).add("Parse language error $e").log()
            null
        }
    }

    private fun getPriorityCodes(userCode:String):List<String>{
        val list = ArrayList<String>()
        if (!userCode.isEmpty()) list.add(userCode)
        list.add(Locale.getDefault().toString().replace("_", "-"))
        list.add("en-US")
        list.add("es-ES")
        list.add("de-DE")
        list.add("fr-FR")
        list.add("pt-PT")
        list.add("pt-PT")
        list.add("ru-RU")
        //todo add priority languages
        return list
    }

}