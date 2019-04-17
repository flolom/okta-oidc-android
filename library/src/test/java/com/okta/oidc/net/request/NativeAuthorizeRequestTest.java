/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.okta.oidc.net.request;

import com.okta.oidc.OIDCAccount;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.response.web.AuthorizeResponse;
import com.okta.oidc.util.AuthorizationException;
import com.okta.oidc.util.MockEndPoint;
import com.okta.oidc.util.MockRequestCallback;
import com.okta.oidc.util.TestValues;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.okta.oidc.util.TestValues.CUSTOM_STATE;
import static com.okta.oidc.util.TestValues.EXCHANGE_CODE;
import static com.okta.oidc.util.TestValues.SESSION_TOKEN;
import static com.okta.oidc.util.TestValues.getProviderConfiguration;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class NativeAuthorizeRequestTest {
    NativeAuthorizeRequest mRequest;
    private ExecutorService mCallbackExecutor;
    private MockEndPoint mEndPoint;

    private ProviderConfiguration mProviderConfig;

    @Rule
    public ExpectedException mExpectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        mEndPoint = new MockEndPoint();
        String url = mEndPoint.getUrl();
        OIDCAccount mAccount = TestValues.getAccountWithUrl(url);
        mProviderConfig = getProviderConfiguration(url);
        mRequest = TestValues.getNativeLogInRequest(mAccount, SESSION_TOKEN, mProviderConfig);
        mCallbackExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        mCallbackExecutor.shutdown();
        mEndPoint.shutDown();
    }

    @Test
    public void dispatchRequestSuccess() throws InterruptedException {
        mEndPoint.enqueueNativeRequestSuccess(CUSTOM_STATE);
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<AuthorizeResponse, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        assertNotNull(cb.getResult());
        assertNotNull(cb.getResult().getCode(), EXCHANGE_CODE);
    }

    @Test
    public void dispatchRequestFailure() throws InterruptedException, AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        final CountDownLatch latch = new CountDownLatch(1);
        MockRequestCallback<AuthorizeResponse, AuthorizationException> cb
                = new MockRequestCallback<>(latch);
        RequestDispatcher dispatcher = new RequestDispatcher(mCallbackExecutor);
        mRequest.dispatchRequest(dispatcher, cb);
        latch.await();
        throw cb.getException();
    }

    @Test
    public void executeRequestSuccess() throws AuthorizationException {
        mEndPoint.enqueueNativeRequestSuccess(CUSTOM_STATE);
        AuthorizeResponse result = mRequest.executeRequest();
        assertNotNull(result);
        assertNotNull(result.getCode(), EXCHANGE_CODE);
    }

    @Test
    public void executeRequestFailure() throws AuthorizationException {
        mExpectedEx.expect(AuthorizationException.class);
        mEndPoint.enqueueReturnUnauthorizedRevoked();
        mRequest.executeRequest();
    }
}