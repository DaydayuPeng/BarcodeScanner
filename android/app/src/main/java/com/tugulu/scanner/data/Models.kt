package com.tugulu.scanner.data

data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?
)

data class LoginData(
    val token: String?,
    val userInfo: UserInfo?
)

data class UserInfo(
    val id: Long?,
    val realName: String?,
    val role: String?
)

data class UploadData(
    val url: String?
)

data class ScanResultData(
    val successCount: Int = 0,
    val failList: List<FailItem> = emptyList()
)

data class FailItem(
    val trackingNo: String?,
    val reason: String?
)

data class ScanRecord(
    val id: Long,
    val trackingNo: String,
    val photoPath: String?
)
