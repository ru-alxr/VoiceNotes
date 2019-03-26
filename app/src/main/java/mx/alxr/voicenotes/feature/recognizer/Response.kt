package mx.alxr.voicenotes.feature.recognizer

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SynchronousRecognizeResult(
    @Json(name = "results") val results: List<InnerResult>?
)

@JsonClass(generateAdapter = true)
data class Alternative(
    @Json(name = "transcript") val transcript: String,
    @Json(name = "confidence") val confidence: Double
)

data class InnerResult(@Json(name = "alternatives") val alternatives: List<Alternative>?)