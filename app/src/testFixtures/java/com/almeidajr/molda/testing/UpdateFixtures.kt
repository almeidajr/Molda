package com.almeidajr.molda.testing

import com.almeidajr.molda.update.domain.ReleaseAsset
import com.almeidajr.molda.update.domain.Update

/** A beta build on offer to an older beta build, with its APK. */
val Beta2 = Update(
    version = "0.3.0-beta.2",
    page = "https://github.com/almeidajr/Molda/releases/tag/v0.3.0-beta.2",
    apk = ReleaseAsset(
        name = "molda-v0.3.0-beta.2.apk",
        downloadUrl = "https://github.com/almeidajr/Molda/releases/download/" +
            "v0.3.0-beta.2/molda-v0.3.0-beta.2.apk",
        digest = "sha256:0123",
    ),
)
