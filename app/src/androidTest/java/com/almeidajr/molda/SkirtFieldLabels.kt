package com.almeidajr.molda

import androidx.test.platform.app.InstrumentationRegistry
import com.almeidajr.molda.skirt.domain.SkirtDraftField
import com.almeidajr.molda.skirt.ui.label
import com.almeidajr.molda.skirt.ui.summaryLabel

/** The field's form label as the app renders it. */
internal val SkirtDraftField.labelText: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(label)

/** The field's name in the measurement summary as the app renders it. */
internal val SkirtDraftField.summaryLabelText: String
    get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(summaryLabel)
