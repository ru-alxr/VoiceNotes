package mx.alxr.voicenotes.repository.token

import com.google.auth.oauth2.GoogleCredentials
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.reactivex.Single
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.repository.storage.ISimpleStorage
import mx.alxr.voicenotes.utils.logger.ILogger
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

const val ALIAS = "token_alias"

class TokenRepository(
    private val simpleStorage: ISimpleStorage,
    private val configRepository: IConfigRepository,
    moshi: Moshi,
    private val logger: ILogger
) : ITokenRepository {

    private val adapter: JsonAdapter<TokenContainer> by lazy { moshi.adapter(TokenContainer::class.java) }

    private val scope = Collections.singletonList("https://www.googleapis.com/auth/cloud-platform")

    override fun getToken(): Single<String> {
        return getWrapper()
            .flatMap {
                if (it.valid) Single.just(it.token)
                else getRemoteToken()
            }
    }

    private fun getRemoteToken(): Single<String> {
        return configRepository
            .getServiceCredentials()
            .map {
                val stream: InputStream = it.byteInputStream()
                val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream).createScoped(scope)
                val token = credentials.refreshAccessToken()
                val time = token!!.expirationTime
                val container = TokenContainer(
                    token = token.tokenValue,
                    time = time!!.time
                )
                simpleStorage.put(ALIAS, adapter.toJson(container))
                container.token
            }
    }

    private fun getWrapper(): Single<TokenWrapper> {
        return Single.fromCallable { getWrapperImpl() }
    }

    private fun getWrapperImpl(): TokenWrapper {
        simpleStorage.get(ALIAS, null)?.apply {
            adapter.fromJson(this)?.apply {
                val now = System.currentTimeMillis()
                val gap = TimeUnit.MINUTES.toMillis(10)
                logger
                    .with(this@TokenRepository)
                    .add("Here is token with expiration time = $time (${Date(time)})")
                    .add("I guess it is ${if (time > now + gap) "valid" else "going to expire soon"}")
                    .log()
                return TokenWrapper(token = token, valid = time > now + gap)
            }
        }
        return TokenWrapper()
    }

}

@JsonClass(generateAdapter = true)
data class TokenContainer(
    @Json(name = "token") val token: String,
    @Json(name = "time") val time: Long
)

private data class TokenWrapper(
    val token: String = "",
    val valid: Boolean = false
)