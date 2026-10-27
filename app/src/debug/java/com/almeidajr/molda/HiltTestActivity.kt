package com.almeidajr.molda

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hosts the route in local screen tests, which show their own content in it. [MainActivity] calls
 * `Rive.init`, which loads native code Robolectric cannot run.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
