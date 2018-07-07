package org.kreal.webdav.sync.authenticator

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Base class for implementing an Activity that is used to help implement an
 * AbstractAccountAuthenticator. If the AbstractAccountAuthenticator needs to use an activity
 * to handle the request then it can have the activity extend AccountAuthenticatorActivity.
 * The AbstractAccountAuthenticator passes in the response to the intent using the following:
 * <pre>
 *      intent.putExtra({@link AccountManager#KEY_ACCOUNT_AUTHENTICATOR_RESPONSE}, response);
 * </pre>
 * The activity then sets the result that is to be handed to the response via
 * {@link #setAccountAuthenticatorResult(android.os.Bundle)}.
 * This result will be sent as the result of the request when the activity finishes. If this
 * is never set or if it is set to null then error {@link AccountManager#ERROR_CODE_CANCELED}
 * will be called on the response.
 */
@SuppressLint("Registered")
open class AccountAuthenticatorAppCompatActivity : AppCompatActivity() {
    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    fun setAccountAuthenticatorResult(result: Bundle) {
        mResultBundle = result
    }

    /**
     * Retreives the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param icicle the save instance data of this Activity, may be null
     */
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        mAccountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)

        mAccountAuthenticatorResponse?.onRequestContinued()

    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    override fun finish() {
        mAccountAuthenticatorResponse?.apply {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null)
                onResult(mResultBundle)
            else onError(AccountManager.ERROR_CODE_CANCELED,
                    "canceled")
        }
        mAccountAuthenticatorResponse = null
        super.finish()
    }
}