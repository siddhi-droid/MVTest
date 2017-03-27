package com.mvtest.marcinmoskala.mvtest

import rx.Subscription

class LoginPresenter(val view: LoginView) {

    val loginUseCase by lazy { LoginUseCase() }
    val validateLoginFieldsUseCase by lazy { ValidateLoginFieldsUseCase() }
    var subscriptions: List<Subscription> = emptyList()

    fun onDestroy() {
        subscriptions.forEach { it.unsubscribe() }
    }

    fun attemptLogin() {
        val (email, password) = view.email to view.password
        subscriptions += validateLoginFieldsUseCase.validateLogin(email, password)
                .smartSubscribe { if (it.correct) sendLoginRequest(email, password) else showLoginErrors(it) }
    }

    private fun sendLoginRequest(email: String, password: String) {
        loginUseCase.sendLoginRequest(email, password)
                .applySchedulers()
                .smartSubscribe(
                        onStart = { view.progressVisible = true },
                        onSuccess = { (token) -> view.informAboutLoginSuccess(token) },
                        onError = view::informAboutError,
                        onFinish = { view.progressVisible = false }
                )
    }

    private fun showLoginErrors(error: ValidateLoginFieldsUseCase.LoginErrors) {
        val (emailErrorId, passwordErrorId) = error
        view.passwordErrorId = passwordErrorId
        view.emailErrorId = emailErrorId
        when {
            passwordErrorId != null -> view.requestPasswordFocus()
            emailErrorId != null -> view.requestEmailFocus()
        }
    }
}