package net.rsprox.proxy.config

@Suppress("unused")
public class ProxyProperty<T>(
    public val name: String,
    public val type: PropertyType<T>,
) {
    internal companion object {
        val PROXY_PORT = ProxyProperty("proxy.port", IntProperty)
        val PROXY_HTTP_PORT = ProxyProperty("proxy.http.port", IntProperty)
        val WORLDLIST_ENDPOINT = ProxyProperty("endpoints.worldlist", StringProperty)
        val JAV_CONFIG_ENDPOINT = ProxyProperty("endpoints.javconfig", StringProperty)
        val BIND_TIMEOUT_SECONDS = ProxyProperty("bind.timeout.seconds", IntProperty)
        val WORLDLIST_REFRESH_SECONDS = ProxyProperty("worldlist.refresh.seconds", IntProperty)
        val BINARY_WRITE_INTERVAL_SECONDS = ProxyProperty("binary.write.interval.seconds", IntProperty)
    }
}
