package com.example.hyundiaboysranger

/** Fictional comparison examples: compiled into the demo variant only. */
object VariantRouteProvider : RouteProvider {
    override fun candidates() = listOf(
        ComparableRoute("DEMO-ROUTE-A", 20, 5, 2, 0),
        ComparableRoute("DEMO-ROUTE-B", 35, 3, 0, 70),
    )
}
