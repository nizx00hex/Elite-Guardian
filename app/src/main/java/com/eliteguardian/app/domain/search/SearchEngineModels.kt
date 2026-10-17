package com.eliteguardian.app.domain.search

enum class SupportedSearchEngine(
    val id: String,
    val displayName: String,
    val description: String,
    val domains: List<String>
) {
    GOOGLE(
        id = "google",
        displayName = "Google Search",
        description = "Blocks google.com, google.co.* and international regional search portals",
        domains = listOf(
            "google.com",
            "www.google.com",
            "google.co.uk",
            "google.ca",
            "google.com.au",
            "google.de",
            "google.fr",
            "google.co.in",
            "google.co.jp"
        )
    ),
    BING(
        id = "bing",
        displayName = "Microsoft Bing",
        description = "Blocks bing.com, www.bing.com, and cn.bing.com",
        domains = listOf(
            "bing.com",
            "www.bing.com",
            "cn.bing.com"
        )
    ),
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        description = "Blocks duckduckgo.com and html/lite search endpoints",
        domains = listOf(
            "duckduckgo.com",
            "www.duckduckgo.com",
            "html.duckduckgo.com",
            "lite.duckduckgo.com"
        )
    ),
    YAHOO(
        id = "yahoo",
        displayName = "Yahoo Search",
        description = "Blocks search.yahoo.com and regional Yahoo search portals",
        domains = listOf(
            "search.yahoo.com",
            "yahoo.com",
            "www.yahoo.com"
        )
    ),
    YANDEX(
        id = "yandex",
        displayName = "Yandex Search",
        description = "Blocks yandex.com, yandex.ru and ya.ru",
        domains = listOf(
            "yandex.com",
            "yandex.ru",
            "ya.ru"
        )
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave Search",
        description = "Blocks search.brave.com",
        domains = listOf(
            "search.brave.com"
        )
    ),
    ECOSIA(
        id = "ecosia",
        displayName = "Ecosia",
        description = "Blocks ecosia.org and www.ecosia.org",
        domains = listOf(
            "ecosia.org",
            "www.ecosia.org"
        )
    );

    companion object {
        fun fromId(id: String): SupportedSearchEngine? = entries.find { it.id == id }
    }
}

data class SearchRestrictionState(
    val isMasterEnabled: Boolean = false,
    val enforceSafeSearch: Boolean = true,
    val restrictInFocusMode: Boolean = true,
    val activeEngineIds: Set<String> = emptySet(),
    val customDomains: List<String> = emptyList()
)

