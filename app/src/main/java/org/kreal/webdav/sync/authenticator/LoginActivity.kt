package org.kreal.webdav.sync.authenticator

import android.accounts.Account
import android.accounts.AccountManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.CallLog
import android.provider.UserDictionary
import android.text.TextUtils
import android.util.Log
import android.view.View
import de.aflx.sardine.SardineFactory
import kotlinx.android.synthetic.main.activity_login.*
import org.kreal.webdav.sync.Constants
import org.kreal.webdav.sync.R

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AccountAuthenticatorAppCompatActivity() {
    /** The Intent flag to confirm credentials.  */
    val PARAM_CONFIRM_CREDENTIALS = "confirmCredentials"

    /** The Intent extra to store password.  */
    val PARAM_PASSWORD = "password"

    /** The Intent extra to store username.  */
    val PARAM_USERNAME = "username"

    /** The Intent extra to store username.  */
    val PARAM_AUTHTOKEN_TYPE = "authtokenType"

    /** The tag used to log to adb console.  */
    private val TAG = "AuthenticatorActivity"

    private lateinit var mAccountManager: AccountManager

    /**
     * If set we are just checking that the user knows their credentials; this
     * doesn't cause the user's password or authToken to be changed on the
     * device.
     */
    private var mConfirmCredentials: Boolean = false

    /** for posting authentication attempts back to UI thread  */
    private val mHandler = Handler()
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    /** Was the original caller asking for an entirely new account?  */
    private var mRequestNewAccount = false

    private var mUsername: String? = null

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_login)
        mAccountManager = AccountManager.get(this)
        Log.i(TAG, "loading data from Intent")
        val intent = intent
        mUsername = intent.getStringExtra(PARAM_USERNAME)
        mRequestNewAccount = mUsername == null
        mConfirmCredentials = intent.getBooleanExtra(PARAM_CONFIRM_CREDENTIALS, false)
        Log.i(TAG, "    request new: $mRequestNewAccount")
        // Set up the login form.
//        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
//            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
//                attemptLogin()
//                return@OnEditorActionListener true
//            }
//            false
//        })

        email_sign_in_button.setOnClickListener { attemptLogin() }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        account.error = null
        password.error = null
        server.error = null

        // Store values at the time of the login attempt.
        val accountStr = account.text.toString()
        val passwordStr = password.text.toString()
        val pathStr = server.text.toString()

        var cancel = false
        var focusView: View? = null

        if (TextUtils.isEmpty(pathStr)) {
            server.error = getString(R.string.error_field_required)
            focusView = server
            cancel = true
        } else if (!isPathValid(pathStr)) {
            server.error = getString(R.string.error_invalid_path)
            focusView = server
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(accountStr) || !isEmailValid(accountStr)) {
            account.error = getString(R.string.error_field_required)
            focusView = account
            cancel = true
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(accountStr, passwordStr, pathStr)
            mAuthTask?.execute(null as Void?)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return email.isNotEmpty()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty()
    }

    private fun isPathValid(path: String): Boolean {
        return path.contains("^(https|http)://".toRegex())
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    @SuppressLint("StaticFieldLeak")
    inner class UserLoginTask internal constructor(private val mAccount: String, private val mPassword: String, private val mServer: String) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {
            return try {
                val sardine = SardineFactory.begin(mAccount, mPassword)
                !sardine.list(mServer).isEmpty()
            } catch (e: Exception) {
                false
            } || try {
                val sardine = SardineFactory.begin(mAccount, mPassword)
                sardine.exists(mServer)
            } catch (e: Exception) {
                false
            }
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success == true) {
                if (!mConfirmCredentials) {
                    finishLogin(mPassword)
                } else {
                    finishConfirmCredentials(true)
                }
            } else {
                password.error = getString(R.string.error_incorrect_password)
                password.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }


        /**
         * Called when response is received from the server for confirm credentials
         * request. See onAuthenticationResult(). Sets the
         * AccountAuthenticatorResult which is sent back to the caller.
         *
         * @param result the confirmCredentials result.
         */
        private fun finishConfirmCredentials(result: Boolean) {
            Log.i(TAG, "finishConfirmCredentials()")
            val account = Account(mAccount, Constants.ACCOUNT_TYPE)
            mAccountManager.setPassword(account, mPassword)
            val intent = Intent()
            intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, result)
            setAccountAuthenticatorResult(intent.extras)
            setResult(RESULT_OK, intent)
            finish()
        }

        /**
         * Called when response is received from the server for authentication
         * request. See onAuthenticationResult(). Sets the
         * AccountAuthenticatorResult which is sent back to the caller. We store the
         * authToken that's returned from the server as the 'password' for this
         * account - so we're never storing the user's actual password locally.
         *
         * @param authToken the confirmCredentials result.
         */
        private fun finishLogin(authToken: String) {

            Log.i(TAG, "finishLogin()")
            val account = Account(mAccount, Constants.ACCOUNT_TYPE)
            if (mRequestNewAccount) {
                mAccountManager.addAccountExplicitly(account, mPassword, null)
                mAccountManager.setUserData(account, Constants.ACCOUNT_SERVER, mServer)
                // Set contacts sync for this account.
                ContentResolver.setIsSyncable(account, CallLog.AUTHORITY, 1)
                ContentResolver.setIsSyncable(account, UserDictionary.AUTHORITY, 1)
                ContentResolver.setIsSyncable(account, "sms", 1)
                ContentResolver.addPeriodicSync(account, CallLog.AUTHORITY, Bundle.EMPTY, 3600 * 24)
                ContentResolver.addPeriodicSync(account, UserDictionary.AUTHORITY, Bundle.EMPTY, 3600 * 24)
                ContentResolver.addPeriodicSync(account, "sms", Bundle.EMPTY, 3600 * 24)
//                ContentResolver.addPeriodicSync(account, "org.kreal.webdav.sync.emptyprovider", Bundle.EMPTY, 3600 * 24 * 7)
                ContentResolver.setSyncAutomatically(account, "sms", true)
            } else {
                mAccountManager.setPassword(account, mPassword)
                mAccountManager.setUserData(account, Constants.ACCOUNT_SERVER, mServer)
            }
            val intent = Intent()
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccount)
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
            setAccountAuthenticatorResult(intent.extras)
            setResult(RESULT_OK, intent)
            finish()
        }

    }

    companion object {

        /**
         * A dummy authentication store containing known user names and passwords.
         * TODO: remove after connecting to a real authentication system.
         */
        private val DUMMY_CREDENTIALS = arrayOf("foo@example.com:hello", "bar@example.com:world")
    }
}
