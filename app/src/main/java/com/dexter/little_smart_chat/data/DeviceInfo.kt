package com.dexter.little_smart_chat.data

object DeviceInfo {
    private var tenantId: Int = 0
    private var url: String = ""

    fun setTenantId(tenantId: Int) {
        this.tenantId = tenantId
    }
    fun getTenantId(): Int {
        return tenantId
    }

    fun getUrl(): String {
        return url
    }
    fun setUrl(url: String) {
        this.url = url
    }
}