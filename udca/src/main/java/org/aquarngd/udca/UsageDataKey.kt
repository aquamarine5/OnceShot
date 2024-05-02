package org.aquarngd.udca

class UsageDataKey(val key: String,val parentKey: List<UsageDataKey>?=null) {
    fun forEach(func:(UsageDataKey)->Unit){
        func(this)
        parentKey?.forEach {
            func(it)
        }
    }
}