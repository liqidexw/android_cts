/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.cts.verifier.tv.display;

import com.android.cts.verifier.tv.TvAppVerifierActivity;

/**
 *  Encapsulates the logic of an asynchronous test step, which displays a human instructions and a
 *  button to start the test. For synchronous steps see {@link TestStep}.
 */
public abstract class AsyncTestStep extends TestStepBase {

    public AsyncTestStep(TvAppVerifierActivity context) {
        super(context);
    }

    /**
     * Runs the test logic, when finished sets the test status by calling
     * {@link AsyncTestStep#doneWithPassingState(boolean)}.
     */
    public abstract void runTestAsync();

    @Override
    protected void onButtonClickRunTest() {
        // Disable the button, so the user can't run it twice.
        disableButton();
        runTestAsync();
    }
}