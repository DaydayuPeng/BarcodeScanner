package com.tugulu.scanner.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tugulu.scanner.TuguluApp
import com.tugulu.scanner.data.ApiException
import com.tugulu.scanner.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app get() = TuguluApp.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (app.session.isLoggedIn) {
            goScanner()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etBaseUrl.setText(app.session.baseUrl)
        binding.etUsername.setText(app.session.username.orEmpty())

        binding.btnLogin.setOnClickListener { doLogin() }
    }

    private fun doLogin() {
        val baseUrl = com.tugulu.scanner.data.ApiClient
            .normalizeBaseUrl(binding.etBaseUrl.text?.toString().orEmpty())
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (baseUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("请填写服务器地址、用户名和密码")
            return
        }
        binding.btnLogin.isEnabled = false
        binding.tvError.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    app.api.login(baseUrl, username, password)
                }
                val token = data.token
                if (token.isNullOrBlank()) {
                    showError("登录成功但未返回 Token")
                    return@launch
                }
                app.session.baseUrl = baseUrl
                app.session.username = username
                app.session.token = token
                app.session.realName = data.userInfo?.realName
                Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                goScanner()
            } catch (e: ApiException) {
                showError(e.message ?: "登录失败")
            } catch (e: Exception) {
                showError(e.message ?: "网络异常")
            } finally {
                binding.btnLogin.isEnabled = true
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun goScanner() {
        startActivity(Intent(this, ScannerActivity::class.java))
        finish()
    }
}
