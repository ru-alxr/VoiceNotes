package mx.alxr.voicenotes.feature.promo

import io.reactivex.Single
import mx.alxr.voicenotes.R
import mx.alxr.voicenotes.repository.config.IConfigRepository
import mx.alxr.voicenotes.repository.user.IUserRepository
import mx.alxr.voicenotes.repository.user.UserEntity
import mx.alxr.voicenotes.utils.resources.IStringResources
import okhttp3.*
import org.json.JSONObject

class Promoter(
    private val userRepository: IUserRepository,
    private val config: IConfigRepository,
    private val client: OkHttpClient,
    private val resources: IStringResources
) : IPromoter {

    override fun promoteRequest(): Single<String> {
        return userRepository
            .getUserSingle()
            .flatMap { user ->
                config.getSlackToken()
                    .flatMap { token ->
                        val url = "https://slack.com/api/chat.postMessage"
                        val builder = Request
                            .Builder()
                            .url(url)
                            .header("Content-type", "application/json")
                            .header("Authorization", "Bearer $token")
                            .method("POST", createBody(user))
                        val response: Response = client.newCall(builder.build()).execute()
                        response.body()?.close()
                        Single.just(resources.getString(R.string.promo_request_sent))
                    }
            }
    }

    private fun createBody(entity: UserEntity): RequestBody {
        val mediaType: MediaType = MediaType.parse("application/json; charset=utf-8")!!
        val content = JSONObject()
            .apply {
                put("channel", "debug")
                put(
                    "text",
                    "```Hi there! I need some extra coins! " +
                            "My name is ${entity.displayName} " +
                            "and user id is ${entity.firebaseUserId}```"
                )
                put("pretty", 1)
            }
        return RequestBody
            .create(
                mediaType,
                content.toString()
            )
    }

}