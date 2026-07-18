package com.tugulu.scanner.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val session: SessionStore) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun login(baseUrl: String, username: String, password: String): LoginData {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url(join(baseUrl, "/api/auth/login"))
            .post(body)
            .build()
        val root = execute(req)
        ensureOk(root)
        val data = root.optJSONObject("data") ?: throw ApiException(root.optString("msg", "登录失败"))
        val user = data.optJSONObject("userInfo")
        val token = data.optString("token")
        return LoginData(
            token = token.takeIf { it.isNotBlank() },
            userInfo = user?.let {
                UserInfo(
                    id = if (it.has("id")) it.optLong("id") else null,
                    realName = it.optString("realName").takeIf { n -> n.isNotBlank() },
                    role = it.optString("role").takeIf { r -> r.isNotBlank() }
                )
            }
        )
    }

    fun uploadImage(file: File): String {
        val token = session.token ?: throw ApiException("未登录")
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("image/jpeg".toMediaType())
        )
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(part)
            .build()
        val req = Request.Builder()
            .url(join(session.baseUrl, "/api/common/upload"))
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        val root = execute(req)
        ensureOk(root)
        val url = root.optJSONObject("data")?.optString("url")
        if (url.isNullOrBlank()) throw ApiException("上传成功但未返回图片地址")
        return url
    }

    fun inboundScan(trackingNos: List<String>, imageUrl: String?): ScanResultData {
        val token = session.token ?: throw ApiException("未登录")
        val list = JSONArray()
        trackingNos.forEach { no ->
            list.put(JSONObject().put("trackingNo", no))
        }
        val payload = JSONObject().put("list", list)
        if (!imageUrl.isNullOrBlank()) payload.put("imageUrl", imageUrl)
        val req = Request.Builder()
            .url(join(session.baseUrl, "/api/inbound/scan"))
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()
        val root = execute(req)
        ensureOk(root)
        val data = root.optJSONObject("data")
        val fails = mutableListOf<FailItem>()
        val failArr = data?.optJSONArray("failList")
        if (failArr != null) {
            for (i in 0 until failArr.length()) {
                val item = failArr.optJSONObject(i) ?: continue
                fails.add(
                    FailItem(
                        trackingNo = item.optString("trackingNo").takeIf { it.isNotBlank() },
                        reason = item.optString("reason").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
        return ScanResultData(
            successCount = data?.optInt("successCount", 0) ?: 0,
            failList = fails
        )
    }

    private fun execute(request: Request): JSONObject {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (text.isBlank()) {
                throw ApiException("服务器无响应 (${resp.code})")
            }
            return try {
                JSONObject(text)
            } catch (_: Exception) {
                throw ApiException("响应不是 JSON (${resp.code}): ${text.take(120)}")
            }
        }
    }

    private fun ensureOk(root: JSONObject) {
        val code = root.optInt("code", -1)
        if (code != 200) {
            throw ApiException(root.optString("msg", "请求失败 ($code)"), code)
        }
    }

    private fun join(base: String, path: String): String {
        val b = base.trim().trimEnd('/')
        if (b.isEmpty()) throw ApiException("请先填写服务器地址")
        return b + path
    }
}

class ApiException(message: String, val code: Int = 400) : IOException(message)
