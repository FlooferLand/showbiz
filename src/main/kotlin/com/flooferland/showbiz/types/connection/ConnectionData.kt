package com.flooferland.showbiz.types.connection

import com.flooferland.showbiz.types.IUnsafeCompoundable

abstract class ConnectionData<T: ConnectionData<T>>(val typeId: String) : IUnsafeCompoundable {
    abstract fun tempReset()

    /**
     * Merges [other] into itself. Returns true if the merge was successful, false if self should be replaced. <br/>
     * WARNING: This can lead to a memory leak very quickly!!
     */
    open fun merge(other: T): Boolean {
        return false
    }
}