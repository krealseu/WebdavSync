package org.kreal.webdav.sync.syncadapter.data

data class Word(val word: String, val shortcut: String, val id: Int = 0, val locale: String? = null, val frequency: Int = 1)
