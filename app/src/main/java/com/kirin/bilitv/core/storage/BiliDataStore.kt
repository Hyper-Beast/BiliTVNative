package com.kirin.bilitv.core.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.biliDataStore by preferencesDataStore(name = "bili_settings")

