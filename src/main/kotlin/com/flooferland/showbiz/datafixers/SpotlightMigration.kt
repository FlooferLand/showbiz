package com.flooferland.showbiz.datafixers

/*
class SpotlightMigration(outputSchema: Schema) : DataFix(outputSchema, false) {
    override fun makeRule(): TypeRewriteRule {
        val chunkType = inputSchema.getType(References.CHUNK)
        return fixTypeEverywhere("Spotlight block to entity", chunkType) { t ->
            t.update(DSL.remainderFinder()) { migrate(it) }
        }
    }

    private fun migrate(chunk: Dynamic<*>): Dynamic<*> {
        TODO()
    }
}
 */